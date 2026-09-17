package com.example.akaishi.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import com.example.akaishi.config.ModConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * 掉落来源索引：离线模拟抽取全部战利品表，得到「物品 → 来源分」。
 *
 * <p>扫描分帧推进（每 tick 若干张表），避免服务端启动 / 数据包重载时卡顿；
 * 扫描完成前掉落项按 0 计，只少不多，不会产生虚高估值。
 *
 * <p>产出的解析器经 {@link AkaishiValueService#setLootResolver} 注入内核，
 * 只参与统一存储库的排序 / 统计 / 筛选，不做任何经济兑换。
 */
public final class LootValueIndex {

    /** 每张表的模拟抽取次数：64 次在概率精度与耗时之间取平衡 */
    private static final int SCAN_ROLLS = 64;
    /** 每 tick 处理的表数：全量扫描摊到多 tick，不阻塞服务端主线程 */
    private static final int TABLES_PER_TICK = 6;

    /** 非实体来源（宝箱 / 方块 / 钓鱼等）的基础难度 */
    private static final double BASE_WORLD_SOURCE = 8.0;
    /** 实体表匹配不到实体时的兜底基础难度 */
    private static final double BASE_ENTITY_FALLBACK = 12.0;
    /** 家畜惩罚：可无限繁殖的动物不应因「能刷」而被高估 */
    private static final double FARM_PENALTY = 18.0;
    private static final double ENTITY_BASE_CAP = 80.0;

    /** 稀有度权重：命中概率每降一个数量级 +20 分 */
    private static final double RARITY_WEIGHT = 20.0;
    /** 数量权重：单次产出越多略加价 */
    private static final double COUNT_WEIGHT = 3.0;
    /** 来源广度权重：来源表越多略加价 */
    private static final double SOURCE_WEIGHT = 5.0;
    private static final double MIN_PROBABILITY = 0.001;
    private static final double MAX_COUNT_BONUS = 10.0;

    private static final String BLOCK_TABLE_PREFIX = "blocks/";

    private static final ToDoubleFunction<Item> NO_LOOT = item -> 0.0;

    /** 单物品在本次扫描中的来源累积 */
    private static final class Accum {
        private double best;
        private int sources;

        private void merge(double score) {
            if (score > best) {
                best = score;
            }
            sources++;
        }
    }

    /** 已发布的结果解析器：整块替换，读侧无需加锁 */
    private static volatile ToDoubleFunction<Item> resolver = NO_LOOT;
    /** 待重建标记：数据包重载 / 配置变更后置位，下个服务端 tick 开始扫描 */
    private static volatile boolean dirty = true;

    // 以下中间状态仅在服务端线程读写（tick 与数据包重载都跑在服务端线程）
    private static Map<Item, Accum> accums = new HashMap<>();
    private static List<ResourceLocation> queue = new ArrayList<>();
    private static Map<ResourceLocation, EntityType<?>> entityByLootTable = Map.of();
    private static int cursor;
    private static boolean scanning;

    private LootValueIndex() {
    }

    /** 当前掉落来源解析器；未就绪时返回全 0 */
    public static ToDoubleFunction<Item> resolver() {
        return resolver;
    }

    /** 标记索引过期（数据包重载 / 配置变更）；进行中的扫描会被作废重来 */
    public static void markDirty() {
        scanning = false;
        dirty = true;
    }

    /** 服务端停止 / 世界卸载：丢弃索引与中间状态，避免跨存档残留旧价 */
    public static void clear() {
        resolver = NO_LOOT;
        accums = new HashMap<>();
        queue = new ArrayList<>();
        entityByLootTable = Map.of();
        cursor = 0;
        scanning = false;
        dirty = false;
        AkaishiValueService.instance().setLootResolver(null);
    }

    /** 服务端每 tick 推进扫描；扫完即发布解析器并失效估值快照 */
    public static void tick(MinecraftServer server) {
        if (!dirty || server == null) {
            return;
        }
        if (!ModConfig.valueLootEnabled) {
            // 关闭掉落估值：不扫描，直接清空解析器
            finish(NO_LOOT);
            return;
        }
        ServerLevel level = server.overworld();
        if (level == null) {
            return;
        }
        if (!scanning) {
            begin(server);
        }
        int processed = 0;
        while (cursor < queue.size() && processed < TABLES_PER_TICK) {
            scanOne(level, queue.get(cursor++));
            processed++;
        }
        if (cursor >= queue.size()) {
            publish();
        }
    }

    /** 准备一轮扫描：清空累积、建立实体掉落表反查索引、取出待扫表清单 */
    private static void begin(MinecraftServer server) {
        scanning = true;
        cursor = 0;
        accums = new HashMap<>();
        entityByLootTable = buildEntityIndex();
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation location : server.getLootData().getKeys(LootDataType.TABLE)) {
            if (location != null) {
                ids.add(location);
            }
        }
        queue = ids;
    }

    /** 实体类型 → 默认战利品表 反查索引（用于由表反推实体难度） */
    private static Map<ResourceLocation, EntityType<?>> buildEntityIndex() {
        Map<ResourceLocation, EntityType<?>> index = new HashMap<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            try {
                ResourceLocation table = type.getDefaultLootTable();
                if (table != null) {
                    index.putIfAbsent(table, type);
                }
            } catch (RuntimeException ignored) {
                // 个别模组实体取默认掉落表可能抛错，跳过即可
            }
        }
        return index;
    }

    /** 模拟抽取单张战利品表，把每个产出物品的来源分并入累积表 */
    private static void scanOne(ServerLevel level, ResourceLocation id) {
        LootTable table = level.getServer().getLootData().getLootTable(id);
        if (table == null || table == LootTable.EMPTY) {
            return;
        }
        LootContextParamSet paramSet = table.getParamSet();
        if (paramSet == null || paramSet == LootContextParamSets.EMPTY) {
            return;
        }
        EntityType<?> sourceType = entityByLootTable.get(id);
        Entity probe = createProbe(level, sourceType, paramSet);
        BlockState state = blockStateFor(id, paramSet);

        LootParams params;
        try {
            params = buildParams(level, paramSet, probe, state);
        } catch (RuntimeException e) {
            // 该表依赖无法构造的上下文（如需真实玩家），跳过而非中断整轮扫描
            return;
        }

        Map<Item, int[]> tally = new HashMap<>();
        Set<Item> seen = new HashSet<>();
        for (int roll = 0; roll < SCAN_ROLLS; roll++) {
            seen.clear();
            List<ItemStack> drops;
            try {
                drops = table.getRandomItems(params);
            } catch (RuntimeException e) {
                return;
            }
            for (ItemStack stack : drops) {
                if (stack.isEmpty() || stack.is(Items.BARRIER)) {
                    continue;
                }
                int[] entry = tally.computeIfAbsent(stack.getItem(), key -> new int[2]);
                entry[1] += stack.getCount();
                if (seen.add(stack.getItem())) {
                    entry[0]++;
                }
            }
        }
        if (tally.isEmpty()) {
            return;
        }

        double base = baseDifficulty(probe, sourceType);
        for (Map.Entry<Item, int[]> entry : tally.entrySet()) {
            int hits = entry.getValue()[0];
            int total = entry.getValue()[1];
            if (hits <= 0) {
                continue;
            }
            double probability = hits / (double) SCAN_ROLLS;
            double averageCount = total / (double) hits;
            double score = base
                    + RARITY_WEIGHT * Math.max(0.0, -Math.log10(Math.max(probability, MIN_PROBABILITY)))
                    + Math.min(MAX_COUNT_BONUS, COUNT_WEIGHT * log2(averageCount + 1.0));
            accums.computeIfAbsent(entry.getKey(), key -> new Accum()).merge(score);
        }
    }

    /** 由实体属性折算基础难度；非实体来源用世界来源基础分 */
    private static double baseDifficulty(Entity probe, EntityType<?> type) {
        if (probe == null) {
            return type == null ? BASE_WORLD_SOURCE : BASE_ENTITY_FALLBACK;
        }
        if (!(probe instanceof LivingEntity living)) {
            return BASE_ENTITY_FALLBACK;
        }
        AttributeMap attributes = living.getAttributes();
        double health = baseAttribute(attributes, Attributes.MAX_HEALTH);
        double attack = baseAttribute(attributes, Attributes.ATTACK_DAMAGE);
        double armor = baseAttribute(attributes, Attributes.ARMOR)
                + baseAttribute(attributes, Attributes.ARMOR_TOUGHNESS);
        double base = Math.min(ENTITY_BASE_CAP, health * 0.6 + attack * 2.0 + armor * 1.5);
        if (living instanceof Animal) {
            base -= FARM_PENALTY;
        }
        return Math.max(0.0, base);
    }

    private static double baseAttribute(AttributeMap attributes, Attribute attribute) {
        return attributes.hasAttribute(attribute) ? attributes.getBaseValue(attribute) : 0.0;
    }

    /** 构造探针实体（用于满足实体表的 THIS_ENTITY 等必需参数）；不可用时返回 null */
    private static Entity createProbe(ServerLevel level, EntityType<?> type, LootContextParamSet paramSet) {
        if (type == null || !paramSet.isAllowed(LootContextParams.THIS_ENTITY)) {
            return null;
        }
        try {
            return type.create(level);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 方块掉落表：按 id 反推方块并取其默认状态，推不出时用空气兜底 */
    private static BlockState blockStateFor(ResourceLocation id, LootContextParamSet paramSet) {
        if (!paramSet.isAllowed(LootContextParams.BLOCK_STATE)) {
            return null;
        }
        String path = id.getPath();
        if (path.startsWith(BLOCK_TABLE_PREFIX)) {
            ResourceLocation blockId = new ResourceLocation(id.getNamespace(),
                    path.substring(BLOCK_TABLE_PREFIX.length()));
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            if (block != null && block != Blocks.AIR) {
                return block.defaultBlockState();
            }
        }
        return Blocks.AIR.defaultBlockState();
    }

    /** 按参数集允许范围填装上下文，避免出现「参数不允许」异常 */
    private static LootParams buildParams(ServerLevel level, LootContextParamSet paramSet,
                                          Entity probe, BlockState state) {
        LootParams.Builder builder = new LootParams.Builder(level);
        putIfAllowed(builder, paramSet, LootContextParams.ORIGIN, Vec3.atCenterOf(BlockPos.ZERO));
        putIfAllowed(builder, paramSet, LootContextParams.TOOL, ItemStack.EMPTY);
        if (state != null) {
            putIfAllowed(builder, paramSet, LootContextParams.BLOCK_STATE, state);
        }
        if (probe != null) {
            putIfAllowed(builder, paramSet, LootContextParams.THIS_ENTITY, probe);
            putIfAllowed(builder, paramSet, LootContextParams.KILLER_ENTITY, probe);
            putIfAllowed(builder, paramSet, LootContextParams.DIRECT_KILLER_ENTITY, probe);
            putIfAllowed(builder, paramSet, LootContextParams.DAMAGE_SOURCE, level.damageSources().generic());
        }
        return builder.create(paramSet);
    }

    private static <T> void putIfAllowed(LootParams.Builder builder, LootContextParamSet paramSet,
                                         LootContextParam<T> param, T value) {
        if (paramSet.isAllowed(param)) {
            builder.withParameter(param, value);
        }
    }

    /** 汇总累积表：最高来源分 + 来源广度加成 */
    private static void publish() {
        Map<Item, Double> values = new HashMap<>(accums.size() * 2);
        for (Map.Entry<Item, Accum> entry : accums.entrySet()) {
            Accum accum = entry.getValue();
            values.put(entry.getKey(),
                    accum.best + SOURCE_WEIGHT * log2(accum.sources + 1.0));
        }
        Map<Item, Double> frozen = Map.copyOf(values);
        finish(item -> frozen.getOrDefault(item, 0.0));
    }

    /** 发布解析器并复位扫描状态 */
    private static void finish(ToDoubleFunction<Item> result) {
        resolver = result;
        AkaishiValueService.instance().setLootResolver(result);
        ValueCache.invalidate();
        accums = new HashMap<>();
        queue = new ArrayList<>();
        entityByLootTable = Map.of();
        cursor = 0;
        scanning = false;
        dirty = false;
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2.0);
    }
}
