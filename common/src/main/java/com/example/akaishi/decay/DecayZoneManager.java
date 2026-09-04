package com.example.akaishi.decay;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.effect.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 衰竭区域管理器：跨重启持久化所有活跃衰竭区域（30 小时）并驱动每 tick 结算。
 * 存于主世界存档（SavedData），区域记录自身维度；服务端每 tick 由 Architectury
 * {@code TickEvent.SERVER_LEVEL_POST} 驱动，仅处理与当前维度匹配的区域。
 * <p>
 * 每 tick 结算：
 * 1. 减益：区域内非亡灵实体刷新 1 秒衰变效果；每 20 tick 施加 2×(等级+1) 魔法伤害
 * 2. 环境转化：随机采样球内方块，草/菌丝/灰化土/苔藓/耕地 → 砂土，树叶/植物 → 摧毁
 * 3. 生物转化（周期）：骷髅→凋零骷髅、马→僵尸马/骷髅马、村民→僵尸村民
 */
public final class DecayZoneManager extends SavedData {

    /** 存档键名 */
    public static final String DATA_NAME = "akaishi_decay_zones";
    /** 半径 5 区块 = 80 格 */
    public static final int RADIUS_BLOCKS = 5 * 16;
    /** 每 tick 环境采样次数（分批转化防止卡顿） */
    private static final int ENV_SAMPLES_PER_TICK = 256;
    /** 生物转化判定周期（tick） */
    private static final int CONVERT_INTERVAL = 40;
    /** 区域剩余时间落盘间隔（tick）：衰减/净化递减需周期保存，防重启后剩余时间回滚 */
    private static final int SAVE_INTERVAL = 600;

    /** 环境替换映射：表层生息方块 → 砂土 */
    private static final Map<net.minecraft.world.level.block.Block, net.minecraft.world.level.block.Block> REPLACE_MAP = Map.of(
            Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT,
            Blocks.MYCELIUM, Blocks.COARSE_DIRT,
            Blocks.PODZOL, Blocks.COARSE_DIRT,
            Blocks.MOSS_BLOCK, Blocks.COARSE_DIRT,
            Blocks.FARMLAND, Blocks.COARSE_DIRT);

    /** 活跃区域（并发安全，服务端单线程 tick 时仅读） */
    private final List<DecayZone> zones = new CopyOnWriteArrayList<>();

    public DecayZoneManager() {
    }

    /** 1.20.1 SavedData 工厂：从存档 NBT 还原区域（由 computeIfAbsent 在磁盘有数据时调用） */
    public DecayZoneManager(CompoundTag tag) {
        this();
        loadZones(tag);
    }

    /** 获取主世界数据存储中的管理器（懒加载） */
    public static DecayZoneManager get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage()
                .computeIfAbsent(DecayZoneManager::new, DecayZoneManager::new, DATA_NAME);
    }

    /**
     * 在指定维度创建衰竭区域。
     *
     * @param amplifier 衰变效果等级（I 级=0；衰竭桶按液量 1-3；爆炸=衰竭五 4）
     * @param explosion 是否爆炸触发（仅决定语义，效果等级由 amplifier 表达）
     */
    public static void createZone(ServerLevel level, BlockPos center, int amplifier, boolean explosion) {
        DecayZoneManager mgr = get(level);
        mgr.zones.add(new DecayZone(center, RADIUS_BLOCKS, amplifier,
                level.dimension().location().toString(), ModConfig.decayZoneDurationTicks));
        mgr.setDirty();
    }

    /**
     * 统计指定位置范围内、同维度的衰竭区域数量（净化塔 GUI 展示用）。
     * 距离按区域中心到塔心的欧氏距离判定，范围含边界。
     */
    public static int countZonesInRange(ServerLevel level, BlockPos center, int range) {
        DecayZoneManager mgr = get(level);
        String dim = level.dimension().location().toString();
        int n = 0;
        for (DecayZone zone : mgr.zones) {
            if (zone.dimension().equals(dim) && inRange(center, zone.center(), range)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 净化指定位置范围内、同维度的衰竭区域：每个区域剩余时间削减 {@code ticks}。
     * 净除（剩余时间归零）的区域立即移除，不等下一 tick 的自然到期判定。
     *
     * @return 净除的区域数量
     */
    public static int purifyZones(ServerLevel level, BlockPos center, int range, long ticks) {
        if (ticks <= 0) {
            return 0;
        }
        DecayZoneManager mgr = get(level);
        String dim = level.dimension().location().toString();
        int removed = 0;
        boolean changed = false;
        for (DecayZone zone : mgr.zones) {
            if (!zone.dimension().equals(dim) || !inRange(center, zone.center(), range)) {
                continue;
            }
            if (zone.purify(ticks)) {
                mgr.zones.remove(zone); // CopyOnWriteArrayList 迭代时删除安全
                removed++;
            }
            changed = true;
        }
        if (changed) {
            mgr.setDirty();
        }
        return removed;
    }

    /** 塔心到区域中心的欧氏距离是否 ≤ 范围（平方比较避免开方） */
    private static boolean inRange(BlockPos from, BlockPos to, int range) {
        long dx = from.getX() - to.getX();
        long dy = from.getY() - to.getY();
        long dz = from.getZ() - to.getZ();
        return dx * dx + dy * dy + dz * dz <= (long) range * range;
    }

    /** 服务端每 tick 调用（由 Architectury TickEvent.SERVER_LEVEL_POST 驱动，每个维度各调一次） */
    public static void serverTick(ServerLevel level) {
        DecayZoneManager mgr = get(level);
        if (mgr.zones.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<DecayZone> it = mgr.zones.iterator();
        while (it.hasNext()) {
            DecayZone zone = it.next();
            if (!zone.dimension().equals(level.dimension().location().toString())) {
                continue;
            }
            if (zone.tickDown()) {
                it.remove();
                mgr.setDirty();
                continue;
            }
            mgr.tickZone(level, zone, now);
        }
        // 剩余时间（自然衰减/净化削减）非创建/移除事件不落盘，此处周期标记一次保存，
        // 使区域内剩余时间随世界 autosave 持久化（否则重启后回滚到上次事件保存值）
        if (!mgr.zones.isEmpty() && now % SAVE_INTERVAL == 0) {
            mgr.setDirty();
        }
    }

    private void tickZone(ServerLevel level, DecayZone zone, long now) {
        AABB box = new AABB(zone.center()).inflate(zone.radius());
        int amp = zone.amplifier();
        // 1) 减益：刷新 1 秒衰变效果（亡灵已过滤）
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, DecayZoneManager::isAffected)) {
            e.addEffect(new MobEffectInstance(ModEffects.DECAY.get(), 20, amp, false, true));
        }
        // 每秒魔法伤害（区域统一节奏，避免效果刷新导致的伤害过频）
        if (now % 20 == 0) {
            float dmg = zone.damagePerSecond();
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, DecayZoneManager::isAffected)) {
                e.hurt(level.damageSources().magic(), dmg);
            }
        }
        // 2) 环境转化（每 tick 一批，30 小时内必然覆盖全区域）
        for (int i = 0; i < ENV_SAMPLES_PER_TICK; i++) {
            sampleDecayBlock(level, zone);
        }
        // 3) 生物转化（周期性判定）
        if (now % CONVERT_INTERVAL == 0) {
            convertEntities(level, box);
        }
    }

    /** 非亡灵实体才受衰变影响（骷髅等亡灵免疫） */
    private static boolean isAffected(LivingEntity e) {
        return !(e instanceof Mob mob && mob.isInvertedHealAndHarm());
    }

    /** 球内随机采样一个位置并转化方块（空气/未加载区块直接跳过） */
    private static void sampleDecayBlock(ServerLevel level, DecayZone zone) {
        BlockPos c = zone.center();
        int r = zone.radius();
        int dx = level.random.nextInt(r * 2 + 1) - r;
        int dy = level.random.nextInt(r * 2 + 1) - r;
        int dz = level.random.nextInt(r * 2 + 1) - r;
        if (dx * dx + dy * dy + dz * dz > r * r) {
            return; // 球形裁剪
        }
        BlockPos p = c.offset(dx, dy, dz);
        BlockState state = level.getBlockState(p);
        if (state.isAir()) {
            return;
        }
        var replaced = REPLACE_MAP.get(state.getBlock());
        if (replaced != null) {
            level.setBlock(p, replaced.defaultBlockState(), 3);
            return;
        }
        if (state.getBlock() instanceof BushBlock || state.getBlock() instanceof LeavesBlock
                || state.getBlock() instanceof VineBlock) {
            level.removeBlock(p, false); // 植物/树叶完全摧毁（不掉落）
        }
    }

    /** 生物转化：骷髅→凋零骷髅（小概率）、马→僵尸马/骷髅马、村民→僵尸村民（必定） */
    private static void convertEntities(ServerLevel level, AABB box) {
        for (LivingEntity e : new ArrayList<>(level.getEntitiesOfClass(Mob.class, box))) {
            if (e instanceof WitherSkeleton) {
                continue; // 已转化完成
            }
            if (e instanceof Skeleton sk) {
                if (level.random.nextFloat() < 0.02f) {
                    sk.convertTo(EntityType.WITHER_SKELETON, true);
                }
            } else if (e instanceof Horse horse) {
                if (level.random.nextFloat() < 0.05f) {
                    boolean zombie = level.random.nextBoolean();
                    if (zombie) {
                        horse.convertTo(EntityType.ZOMBIE_HORSE, true);
                    } else {
                        horse.convertTo(EntityType.SKELETON_HORSE, true);
                    }
                }
            } else if (e instanceof Villager villager) {
                villager.convertTo(EntityType.ZOMBIE_VILLAGER, true);
            }
        }
    }

    // ===== SavedData 持久化（1.20.1：加载走构造工厂，保存实现抽象方法 save） =====

    /** 从存档 NBT 还原区域（构造工厂调用） */
    private void loadZones(CompoundTag tag) {
        zones.clear();
        CompoundTag list = tag.getCompound("Zones");
        for (String key : list.getAllKeys()) {
            zones.add(DecayZone.fromNbt(list.getCompound(key)));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag list = new CompoundTag();
        for (int i = 0; i < zones.size(); i++) {
            list.put(String.valueOf(i), zones.get(i).toNbt());
        }
        tag.put("Zones", list);
        return tag;
    }
}
