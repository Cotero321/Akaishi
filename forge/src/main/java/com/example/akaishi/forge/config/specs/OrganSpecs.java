package com.example.akaishi.forge.config.specs;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 配置域：器官品质曲线、来源组、纯度、排斥、血清、突变词条、培养机升级。
 * 仅由 {@link com.example.akaishi.forge.config.AkaishiConfig} 的 static 初始化按固定顺序调用；
 * 键名/注释/默认值/范围与拆分前逐字一致，调用顺序即 common.toml 分节顺序。
 */
public final class OrganSpecs {

    // ==================== 器官·品质曲线 ====================
    /** 品质 I~IV 属性加成倍率（下标 = 品质序数，下同） */
    public static ForgeConfigSpec.ConfigValue<List<? extends Double>> ORGAN_TIER_MULTIPLIER;
    /** 品质 I~IV 移植基础排斥 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> ORGAN_TIER_BASE_REJECTION;
    /** 品质 I~IV 排斥增长间隔（秒） */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> ORGAN_TIER_GROWTH_INTERVAL;

    // ==================== 基因来源组排斥系数 ====================
    /** 温血/亡灵/爆炸/异变/末影/Boss/龙 七组排斥系数 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Double>> GROUP_REJECTION_FACTOR;

    // ==================== 纯度联动 ====================
    public static ForgeConfigSpec.DoubleValue PURITY_REJECTION_CAP;
    public static ForgeConfigSpec.DoubleValue PURITY_COMPAT_WEIGHT;

    // ==================== 排斥·标尺与阈值 ====================
    public static ForgeConfigSpec.IntValue MAX_REJECTION;
    public static ForgeConfigSpec.IntValue REJECTION_WARNING;
    public static ForgeConfigSpec.IntValue REJECTION_POISON;
    public static ForgeConfigSpec.IntValue COMPAT_SEVERE_THRESHOLD;
    public static ForgeConfigSpec.IntValue SLOT_DEBUFF_CLEAN_THRESHOLD;
    public static ForgeConfigSpec.IntValue SLOT_DEBUFF_SEVERE_THRESHOLD;
    public static ForgeConfigSpec.IntValue GROWTH_INTERVAL_MIN_TICKS;
    public static ForgeConfigSpec.IntValue CONFLICT_PUNISH_INTERVAL_TICKS;
    public static ForgeConfigSpec.DoubleValue CONFLICT_PUNISH_DAMAGE;
    public static ForgeConfigSpec.IntValue OVERLOAD_LIGHT;
    public static ForgeConfigSpec.IntValue OVERLOAD_HEAVY;

    // ==================== 排异中和剂（血清） ====================
    public static ForgeConfigSpec.IntValue SERUM_WASH_REDUCE;
    public static ForgeConfigSpec.IntValue SERUM_WASH_LIMIT;
    public static ForgeConfigSpec.IntValue SERUM_COOLDOWN_TICKS;

    // ==================== 突变词条 ====================
    public static ForgeConfigSpec.DoubleValue TRAIT_BENIGN_RATIO;
    public static ForgeConfigSpec.IntValue TRAIT_RARITY_HIGH_THRESHOLD;
    public static ForgeConfigSpec.IntValue TRAIT_RARITY_MID_THRESHOLD;

    // ==================== 培养机·品质升级 ====================
    /** I→II / II→III / III→IV 成功率（百分比） */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_UPGRADE_SUCCESS;
    /** 三段升级生命能量消耗 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_UPGRADE_ENERGY;
    /** 三段升级固态物消耗 */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_UPGRADE_SOLID;
    /** 三段升级耗时（tick） */
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_UPGRADE_TICKS;
    /** 升级成功额外适配加成 */
    public static ForgeConfigSpec.IntValue CULTIVATOR_UPGRADE_COMPAT_BONUS;

    private OrganSpecs() {
    }

    /** 按拆分前的定义顺序在共享 Builder 上构建本域各节（仅由 AkaishiConfig 调用） */
    public static void build(ForgeConfigSpec.Builder b) {
        // ==================== 器官·品质曲线 ====================
        // 下标 = 品质序数（0=I，1=II，2=III，3=IV）；0 或缺失条目回退到内置默认
        b.push("organ_quality");
        ORGAN_TIER_MULTIPLIER = b.comment("属性倍率，下标 = 品质 [I, II, III, IV]；0 = 用内置默认")
                .defineList("multiplier", List.of(1.25, 1.5, 1.75, 2.0),
                        (Object o) -> o instanceof Number n && n.doubleValue() >= 0);
        ORGAN_TIER_BASE_REJECTION = b.comment("移植时基础排斥，下标 = 品质 [I, II, III, IV]；0 = 用内置默认")
                .defineList("baseRejection", List.of(12, 24, 36, 48),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        ORGAN_TIER_GROWTH_INTERVAL = b.comment("排斥增长间隔（秒），下标 = 品质 [I, II, III, IV]；0 = 用内置默认")
                .defineList("growthInterval", List.of(45, 30, 20, 20),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        b.pop();

        // ==================== 基因来源组排斥系数 ====================
        b.push("sample_groups");
        GROUP_REJECTION_FACTOR = b.comment("各来源组排斥系数，下标 = SampleGroup 序数 "
                        + "[WARM_BLOODED 温血, UNDEAD 不死, EXPLOSIVE 爆炸, ABERRATION 异变, ENDER 末影, BOSS 首领, DRAGON 龙]；0 = 用内置默认")
                .defineList("rejectionFactor", List.of(0.8, 1.0, 1.1, 1.15, 1.3, 1.5, 1.6),
                        (Object o) -> o instanceof Number n && n.doubleValue() >= 0);
        b.pop();

        // ==================== 纯度联动 ====================
        b.push("purity");
        PURITY_REJECTION_CAP = b.comment("器官纯度对排斥的最大削减比例（0.5 = 纯度 100 时排斥减半）；0 = 用内置默认")
                .defineInRange("rejectionCap", 0.5, 0.0, 1.0);
        PURITY_COMPAT_WEIGHT = b.comment("纯度对兼容度偏置的最大权重（0.3 = 纯度 100 时权重 30%）；0 = 用内置默认")
                .defineInRange("compatWeight", 0.3, 0.0, 1.0);
        b.pop();

        // ==================== 排斥·标尺与阈值 ====================
        b.push("rejection");
        MAX_REJECTION = b.comment("单槽位排斥上限，达到即器官失效；0 = 用内置 100")
                .defineInRange("maxRejection", 100, 0, 1000);
        REJECTION_WARNING = b.comment("排斥警告阈值：达到后开始随机施加中毒/虚弱")
                .defineInRange("warningThreshold", 60, 0, 1000);
        REJECTION_POISON = b.comment("排斥中毒阈值")
                .defineInRange("poisonThreshold", 80, 0, 1000);
        COMPAT_SEVERE_THRESHOLD = b.comment("有效适配低于此值时排斥增速翻倍")
                .defineInRange("compatSevereThreshold", 60, 0, 100);
        SLOT_DEBUFF_CLEAN_THRESHOLD = b.comment("有效适配 ≥ 此值的槽位不受部位减益")
                .defineInRange("slotDebuffCleanThreshold", 70, 0, 100);
        SLOT_DEBUFF_SEVERE_THRESHOLD = b.comment("有效适配低于此值时部位减益升为 II 级")
                .defineInRange("slotDebuffSevereThreshold", 45, 0, 100);
        GROWTH_INTERVAL_MIN_TICKS = b.comment("排斥增长间隔下限 (tick，每点 15 秒折算)")
                .defineInRange("growthIntervalMinTicks", 300, 1, Integer.MAX_VALUE);
        CONFLICT_PUNISH_INTERVAL_TICKS = b.comment("天敌组合冲突惩罚间隔 (tick)")
                .defineInRange("conflictPunishInterval", 100, 1, Integer.MAX_VALUE);
        CONFLICT_PUNISH_DAMAGE = b.comment("天敌冲突每次自伤（爆炸伤害来源）")
                .defineInRange("conflictPunishDamage", 5.0, 0.0, 100.0);
        OVERLOAD_LIGHT = b.comment("总排斥 ≥ 此值触发躯体超载 I（缓慢 I）")
                .defineInRange("overloadLight", 320, 0, Integer.MAX_VALUE);
        OVERLOAD_HEAVY = b.comment("总排斥 ≥ 此值触发躯体超载 II（缓慢 II + 虚弱）")
                .defineInRange("overloadHeavy", 450, 0, Integer.MAX_VALUE);
        b.pop();

        // ==================== 排异中和剂（血清） ====================
        b.push("serum");
        SERUM_WASH_REDUCE = b.comment("每次饮用对可洗涤器官减少的排斥值")
                .defineInRange("washReduce", 12, 1, 100);
        SERUM_WASH_LIMIT = b.comment("每次移植后每器官洗涤次数上限（重新移植重置）")
                .defineInRange("washLimit", 6, 1, 64);
        SERUM_COOLDOWN_TICKS = b.comment("饮用冷却 (tick)")
                .defineInRange("cooldownTicks", 300, 1, Integer.MAX_VALUE);
        b.pop();

        // ==================== 突变词条 ====================
        b.push("trait");
        TRAIT_BENIGN_RATIO = b.comment("先抽良性词条池的概率（0.7 = 70% 良性 / 30% 双刃）")
                .defineInRange("benignRatio", 0.7, 0.0, 1.0);
        TRAIT_RARITY_HIGH_THRESHOLD = b.comment("III 级稀有词条的纯度阈值（默认 85）")
                .defineInRange("rarityHighThreshold", 85, 1, 100);
        TRAIT_RARITY_MID_THRESHOLD = b.comment("II 级稀有词条的纯度阈值（默认 60）")
                .defineInRange("rarityMidThreshold", 60, 1, 100);
        b.pop();

        // ==================== 培养机·品质升级 ====================
        // 三段：I→II / II→III / III→IV；0 或缺失条目回退到内置默认
        b.push("cultivator_upgrade");
        CULTIVATOR_UPGRADE_SUCCESS = b.comment("各阶段升级成功率 (%) [I→II, II→III, III→IV]；0 = 用内置默认")
                .defineList("successRate", List.of(85, 75, 65),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_UPGRADE_ENERGY = b.comment("各阶段升级生命能量消耗 [I→II, II→III, III→IV]；0 = 用内置默认")
                .defineList("energyCost", List.of(20_000, 80_000, 300_000),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_UPGRADE_SOLID = b.comment("各阶段升级固态生命精华消耗 [I→II, II→III, III→IV]；0 = 用内置默认")
                .defineList("solidCost", List.of(1, 4, 16),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_UPGRADE_TICKS = b.comment("各阶段升级耗时 (tick) [I→II, II→III, III→IV]；0 = 用内置默认")
                .defineList("processTicks", List.of(600, 1200, 2400),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_UPGRADE_COMPAT_BONUS = b.comment("升级成功获得的适配加成；0 = 用内置 +8")
                .defineInRange("compatBonus", 8, 0, 100);
        b.pop();
    }
}
