package com.example.akaishi.life.sample;

import com.example.akaishi.api.life.ISampleGroup;
import com.example.akaishi.api.life.SampleGroupRegistry;
import com.example.akaishi.config.ModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Shulker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 生命样本生物分组（内置实现）：决定样本的基因来源与基础纯度。
 * <p>
 * 由枚举改造为「兼容门面 + 注册表」：静态常量名保持与原枚举一致，历史调用点无需修改；
 * 底层以稳定命名空间 ID（如 {@code akaishi:warm_blooded}）登记到 {@link SampleGroupRegistry}，
 * 附属模组可注册自定义 {@link ISampleGroup} 并按 ID 与本集合共同参与采集 / 持久化 / 展示。
 * <p>
 * 内置匹配优先级不变（龙 &gt; 首领 &gt; 爆炸 &gt; 异变 &gt; 末影 &gt; 亡灵 &gt; 温血），
 * 未命中的生物再交第三方 matcher 判定；均未命中（如村民）视为无样本价值，不可采集。
 */
public final class SampleGroup implements ISampleGroup {

    // 注意：映射表须先于常量初始化，create() 依赖它们
    private static final Map<String, SampleGroup> BY_LEGACY_NAME = new ConcurrentHashMap<>();
    private static final List<SampleGroup> VALUES = new ArrayList<>();

    /** 温血生物（动物/水生） */
    public static final SampleGroup WARM_BLOODED = create("WARM_BLOODED", "warm_blooded", 45, 85, 75, 95, 0.8, 1,
            "life.akaishi.sample_group.warm_blooded",
            e -> e instanceof Animal || e instanceof AmbientCreature || e instanceof WaterAnimal);
    /** 亡灵生物（僵尸/骷髅等） */
    public static final SampleGroup UNDEAD = create("UNDEAD", "undead", 55, 70, 60, 85, 1.0, 2,
            "life.akaishi.sample_group.undead",
            e -> e.getType() == EntityType.PHANTOM || e.getType() == EntityType.ZOGLIN
                    || e.getMobType() == MobType.UNDEAD);
    /** 爆炸性生物（苦力怕） */
    public static final SampleGroup EXPLOSIVE = create("EXPLOSIVE", "explosive", 60, 55, 45, 70, 1.1, 2,
            "life.akaishi.sample_group.explosive",
            e -> e instanceof Creeper);
    /** 异变体（蜘蛛/史莱姆/烈焰人/守卫者/铁傀儡/悦灵等非自然强敌） */
    public static final SampleGroup ABERRATION = create("ABERRATION", "aberration", 65, 45, 45, 70, 1.15, 2,
            "life.akaishi.sample_group.aberration",
            e -> {
                EntityType<?> t = e.getType();
                return t == EntityType.SPIDER || t == EntityType.CAVE_SPIDER || t == EntityType.SLIME
                        || t == EntityType.MAGMA_CUBE || t == EntityType.BLAZE
                        || t == EntityType.GUARDIAN || t == EntityType.ELDER_GUARDIAN || t == EntityType.IRON_GOLEM
                        || t == EntityType.SNOW_GOLEM || t == EntityType.ALLAY
                        || t == EntityType.GHAST || t == EntityType.VINDICATOR
                        // 下界系（1.16+ 火狱生态）：猪灵/蛮兵、疣猪兽、女巫
                        || t == EntityType.PIGLIN || t == EntityType.PIGLIN_BRUTE
                        || t == EntityType.HOGLIN || t == EntityType.WITCH;
            });
    /** 末影生物（末影人/末影螨/潜影贝） */
    public static final SampleGroup ENDER = create("ENDER", "ender", 70, 40, 35, 60, 1.3, 3,
            "life.akaishi.sample_group.ender",
            e -> e instanceof EnderMan || e instanceof Endermite || e instanceof Shulker);
    /** 首领级（凋灵/循声守卫）：Boss 基因，仅次龙族 */
    public static final SampleGroup BOSS = create("BOSS", "boss", 85, 20, 30, 55, 1.5, 4,
            "life.akaishi.sample_group.boss",
            e -> e.getType() == EntityType.WITHER || e.getType() == EntityType.WARDEN);
    /** 龙族（末影龙，顶级样本） */
    public static final SampleGroup DRAGON = create("DRAGON", "dragon", 90, 25, 25, 50, 1.6, 4,
            "life.akaishi.sample_group.dragon",
            e -> e instanceof EnderDragon);

    /** 内置匹配优先级（与旧枚举 if 链一致：龙 > 首领 > 爆炸 > 异变 > 末影 > 亡灵 > 温血） */
    private static final List<SampleGroup> PRIORITY =
            List.of(DRAGON, BOSS, EXPLOSIVE, ABERRATION, ENDER, UNDEAD, WARM_BLOODED);

    static {
        // 内置分组注入公共注册表：附属模组可按 ID 查询 / 扩展 / 展示
        for (SampleGroup group : VALUES) {
            SampleGroupRegistry.override(group);
        }
    }

    /** 原枚举名（历史 NBT / 调试兼容） */
    private final String legacyName;
    /** 稳定命名空间 ID（NBT / 网络共用） */
    private final ResourceLocation id;
    /** 基础纯度（0-100），采集时附加随机波动 */
    private final int basePurity;
    /** 基础采集成功率（0-100），生物血量越低加成越高，封顶 95 */
    private final int baseCollectRate;
    /** 器官适配度区间下限（越强生物越难契合，原生器官固定 100） */
    private final int compatMin;
    /** 器官适配度区间上限 */
    private final int compatMax;
    /** 排斥系数：基因强度 → 身体负担倍数（温血 0.8 最契合，龙 1.6 最难契合） */
    private final double rejectionFactor;
    /** 品质档位（1=I ~ 4=IV） */
    private final int qualityTier;
    /** 显示名翻译键 */
    private final String nameKey;
    /** 采集匹配谓词 */
    private final Predicate<LivingEntity> matcher;
    /** 配置覆盖下标（内置声明序）：扩展分组为 -1，不参与内置配置覆盖 */
    private final int configIndex;

    private SampleGroup(String legacyName, ResourceLocation id, int basePurity, int baseCollectRate,
                        int compatMin, int compatMax, double rejectionFactor, int qualityTier,
                        String nameKey, Predicate<LivingEntity> matcher, int configIndex) {
        this.legacyName = legacyName;
        this.id = id;
        this.basePurity = basePurity;
        this.baseCollectRate = baseCollectRate;
        this.compatMin = compatMin;
        this.compatMax = compatMax;
        this.rejectionFactor = rejectionFactor;
        this.qualityTier = qualityTier;
        this.nameKey = nameKey;
        this.matcher = matcher;
        this.configIndex = configIndex;
    }

    private static SampleGroup create(String legacyName, String path, int basePurity, int baseCollectRate,
                                      int compatMin, int compatMax, double rejectionFactor, int qualityTier,
                                      String nameKey, Predicate<LivingEntity> matcher) {
        ResourceLocation parsed = ResourceLocation.tryParse("akaishi:" + path);
        if (parsed == null) {
            throw new IllegalStateException("Invalid built-in sample group id: " + path);
        }
        SampleGroup group = new SampleGroup(legacyName, parsed, basePurity, baseCollectRate, compatMin, compatMax,
                rejectionFactor, qualityTier, nameKey, matcher, VALUES.size());
        BY_LEGACY_NAME.put(legacyName, group);
        VALUES.add(group);
        return group;
    }

    // ==================== 契约 ====================

    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public int getBasePurity() {
        return basePurity;
    }

    @Override
    public int getBaseCollectRate() {
        return baseCollectRate;
    }

    @Override
    public int getCompatMin() {
        return compatMin;
    }

    @Override
    public int getCompatMax() {
        return compatMax;
    }

    @Override
    public String getNameKey() {
        return nameKey;
    }

    @Override
    public int getQualityTier() {
        return qualityTier;
    }

    @Override
    public Predicate<LivingEntity> matcher() {
        return matcher;
    }

    /** 排斥系数（基因强度 → 身体负担倍数，值越低越契合）——
     *  可由配置 akaishi-common.toml [sample_groups] 列表覆盖（按内置声明序，0 = 用内置默认） */
    @Override
    public double getRejectionFactor() {
        double[] arr = ModConfig.groupRejectionFactor;
        int i = configIndex;
        return i >= 0 && i < arr.length && arr[i] > 0 ? arr[i] : rejectionFactor;
    }

    /** 原枚举名（NBT / 调试兼容） */
    public String name() {
        return legacyName;
    }

    public ResourceLocation id() {
        return id;
    }

    // ==================== 查询 ====================

    /** 全部内置分组（不可变，顺序与声明一致） */
    public static List<SampleGroup> values() {
        return Collections.unmodifiableList(VALUES);
    }

    /** 旧枚举名 → 内置分组；未知返回 null */
    public static SampleGroup byLegacyName(String legacyName) {
        return legacyName == null ? null : BY_LEGACY_NAME.get(legacyName.toUpperCase(Locale.ROOT));
    }

    /**
     * 按 id 反查分组（NBT / 网络反序列化用）。
     * 完整 ID 精确命中 → 裸 path 回退 {@code akaishi:} 命名空间 → 旧枚举名回退；未知返回 null。
     */
    public static ISampleGroup byId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String raw = id.trim();
        ResourceLocation parsed = ResourceLocation.tryParse(raw);
        if (parsed != null) {
            ISampleGroup hit = SampleGroupRegistry.get(parsed);
            if (hit != null) {
                return hit;
            }
        }
        int colon = raw.indexOf(':');
        String path = colon >= 0 ? raw.substring(colon + 1) : raw;
        ISampleGroup fallback = SampleGroupRegistry.get("akaishi:" + path.toLowerCase(Locale.ROOT));
        if (fallback != null) {
            return fallback;
        }
        return BY_LEGACY_NAME.get(path.toUpperCase(Locale.ROOT));
    }

    /** 匹配实体所属分组：先按内置优先级，再交第三方 matcher；无样本价值返回 null */
    public static ISampleGroup of(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        for (SampleGroup group : PRIORITY) {
            if (group.matcher.test(entity)) {
                return group;
            }
        }
        for (ISampleGroup group : SampleGroupRegistry.getAll()) {
            if (group instanceof SampleGroup) {
                continue; // 内置分组已在优先级链中判定
            }
            Predicate<LivingEntity> matcher = group.matcher();
            if (matcher != null && matcher.test(entity)) {
                return group;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
