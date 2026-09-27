package com.example.akaishi.forge.config.specs;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 配置域：禁断四件（生命之触/幼崽之心/母神之印/孕育之环/套装/侵蚀/祭坛）。
 * 仅由 {@link com.example.akaishi.forge.config.AkaishiConfig} 的 static 初始化按固定顺序调用；
 * 键名/注释/默认值/范围与拆分前逐字一致，调用顺序即 common.toml 分节顺序。
 */
public final class CurioSpecs {

    // ---- 禁断四件 · 1 生命之触 ----
    public static ForgeConfigSpec.BooleanValue LIFE_TOUCH_ENABLED;
    public static ForgeConfigSpec.DoubleValue LIFE_TOUCH_REACH_BONUS;
    public static ForgeConfigSpec.DoubleValue LIFE_TOUCH_DOUBLE_STRIKE_CHANCE;
    public static ForgeConfigSpec.DoubleValue LIFE_TOUCH_SELF_HURT_CHANCE;
    public static ForgeConfigSpec.DoubleValue LIFE_TOUCH_SELF_HURT_DAMAGE;
    public static ForgeConfigSpec.DoubleValue LIFE_TOUCH_HUNGER_COST_CHANCE;
    public static ForgeConfigSpec.IntValue LIFE_TOUCH_HUNGER_COST_AMOUNT;
    public static ForgeConfigSpec.DoubleValue LIFE_TOUCH_HUNGER_RESTORE_CHANCE;
    public static ForgeConfigSpec.IntValue LIFE_TOUCH_HUNGER_RESTORE_AMOUNT;
    public static ForgeConfigSpec.IntValue LIFE_TOUCH_HIT_CACHE_TICKS;

    // ---- 禁断四件 · 2 幼崽之心 ----
    public static ForgeConfigSpec.BooleanValue CUB_HEART_ENABLED;
    public static ForgeConfigSpec.DoubleValue CUB_HEART_ABSORPTION_RATIO;
    public static ForgeConfigSpec.IntValue CUB_HEART_EFFECT_INTERVAL;
    public static ForgeConfigSpec.DoubleValue CUB_HEART_SLOW_HASTE_CHANCE;
    public static ForgeConfigSpec.DoubleValue CUB_HEART_SLOW_HASTE_AMPLITUDE;
    public static ForgeConfigSpec.IntValue CUB_HEART_SLOW_HASTE_TICKS;
    public static ForgeConfigSpec.DoubleValue CUB_HEART_DAMAGE_SHIFT_CHANCE;
    public static ForgeConfigSpec.DoubleValue CUB_HEART_DAMAGE_SHIFT_AMOUNT;
    public static ForgeConfigSpec.IntValue CUB_HEART_DAMAGE_SHIFT_TICKS;
    public static ForgeConfigSpec.DoubleValue CUB_HEART_UNNAMEABLE_CHANCE;
    public static ForgeConfigSpec.IntValue CUB_HEART_UNNAMEABLE_AMPLIFIER;
    public static ForgeConfigSpec.IntValue CUB_HEART_UNNAMEABLE_TICKS;
    public static ForgeConfigSpec.DoubleValue CUB_HEART_EXCITEMENT_ATTACK_SPEED;
    public static ForgeConfigSpec.IntValue CUB_HEART_EXCITEMENT_TICKS;

    // ---- 禁断四件 · 3 母神之印 ----
    public static ForgeConfigSpec.BooleanValue MOTHER_SEAL_ENABLED;
    public static ForgeConfigSpec.DoubleValue MOTHER_SEAL_DAMAGE_BONUS;
    public static ForgeConfigSpec.DoubleValue MOTHER_SEAL_HEALTH_BONUS;
    public static ForgeConfigSpec.DoubleValue MOTHER_SEAL_SPEED_BONUS;
    public static ForgeConfigSpec.DoubleValue MOTHER_SEAL_ATTACK_SPEED_BONUS;
    public static ForgeConfigSpec.IntValue MOTHER_SEAL_SELF_UNNAMEABLE_PERIOD;
    public static ForgeConfigSpec.IntValue MOTHER_SEAL_SELF_UNNAMEABLE_TICKS;
    public static ForgeConfigSpec.IntValue MOTHER_SEAL_SELF_UNNAMEABLE_AMPLIFIER;
    public static ForgeConfigSpec.IntValue MOTHER_SEAL_VEGETARIAN_HUNGER_COST;
    public static ForgeConfigSpec.IntValue MOTHER_SEAL_VEGETARIAN_NAUSEA_TICKS;
    public static ForgeConfigSpec.DoubleValue MOTHER_SEAL_RESIST_FACTOR;

    // ---- 禁断四件 · 4 孕育之环 ----
    public static ForgeConfigSpec.BooleanValue FERTILITY_RING_ENABLED;
    public static ForgeConfigSpec.DoubleValue FERTILITY_RING_HEAL_CHANCE;
    public static ForgeConfigSpec.DoubleValue FERTILITY_RING_HEAL_AMOUNT;
    public static ForgeConfigSpec.IntValue FERTILITY_RING_HEAL_HUNGER_COST;
    public static ForgeConfigSpec.DoubleValue FERTILITY_RING_ATTACK_SPEED_BONUS;
    public static ForgeConfigSpec.IntValue FERTILITY_RING_ATTACK_SPEED_TICKS;
    public static ForgeConfigSpec.DoubleValue FERTILITY_RING_MEAT_HEAL_AMOUNT;
    public static ForgeConfigSpec.BooleanValue FERTILITY_RING_SATIATED_MEAT_ONLY;
    public static ForgeConfigSpec.DoubleValue FERTILITY_RING_STARVE_MULTIPLIER;
    public static ForgeConfigSpec.BooleanValue FERTILITY_RING_STARVE_LETHAL;

    // ---- 禁断四件 · 5 套装 ----
    public static ForgeConfigSpec.IntValue SET_ATTACK_COUNT_REQUIRED;
    public static ForgeConfigSpec.IntValue SET_ATTACK_COUNT_HUNGER_RESTORE;
    public static ForgeConfigSpec.IntValue SET_UNNAMEABLE_LEVEL_BONUS;
    public static ForgeConfigSpec.BooleanValue SET_SUPPRESS_DISTORTION;
    public static ForgeConfigSpec.DoubleValue SET_DAMAGE_REDUCTION;
    public static ForgeConfigSpec.DoubleValue SET_UNNAMEABLE_CRIT_CHANCE;
    public static ForgeConfigSpec.DoubleValue SET_UNNAMEABLE_CRIT_DAMAGE;
    public static ForgeConfigSpec.DoubleValue SET_NEAR_DEATH_HEAL_PERCENT;
    public static ForgeConfigSpec.IntValue SET_NEAR_DEATH_UNNAMEABLE_TICKS;
    public static ForgeConfigSpec.IntValue SET_NEAR_DEATH_COOLDOWN_TICKS;

    // ---- 禁断四件 · 6 侵蚀 ----
    public static ForgeConfigSpec.BooleanValue EROSION_ENABLED;
    public static ForgeConfigSpec.IntValue EROSION_DURATION_MINUTES;
    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> EROSION_NOTICE_THRESHOLDS;
    public static ForgeConfigSpec.IntValue EROSION_NOTICE_INTERVAL_MINUTES;
    public static ForgeConfigSpec.DoubleValue EROSION_STAT_REROLL_PERCENT;
    public static ForgeConfigSpec.IntValue EROSION_NBT_FLUSH_TICKS;
    public static ForgeConfigSpec.BooleanValue EROSION_SCREEN_FLASH_ENABLED;

    // ---- 禁断四件 · 7 仪式与吸取 ----
    public static ForgeConfigSpec.BooleanValue ALTAR_DRAIN_ENABLED;
    public static ForgeConfigSpec.IntValue ALTAR_DRAIN_RADIUS;
    public static ForgeConfigSpec.IntValue ALTAR_DRAIN_INTERVAL_TICKS;
    public static ForgeConfigSpec.DoubleValue ALTAR_DRAIN_HEALTH_PERCENT;
    public static ForgeConfigSpec.LongValue ALTAR_DRAIN_ENERGY_PER_HP;
    public static ForgeConfigSpec.IntValue ALTAR_DRAIN_MAX_TARGETS;
    public static ForgeConfigSpec.LongValue ALTAR_DRAIN_MAX_ENERGY_PER_TARGET;
    public static ForgeConfigSpec.BooleanValue ALTAR_DRAIN_EXEMPT_CREATIVE;
    public static ForgeConfigSpec.IntValue ALTAR_NEW_RECIPE_TIER_REQUIRED;
    public static ForgeConfigSpec.LongValue ALTAR_NEW_RECIPE_PROGRESS_MAX;
    public static ForgeConfigSpec.LongValue ALTAR_LEGACY_PROGRESS_MAX;

    private CurioSpecs() {
    }

    /** 按拆分前的定义顺序在共享 Builder 上构建本域各节（仅由 AkaishiConfig 调用） */
    public static void build(ForgeConfigSpec.Builder b) {
        // ==================== 禁断四件 ====================
        b.push("curio");

        // ---- 1 生命之触 ----
        b.push("lifeTouch");
        LIFE_TOUCH_ENABLED = b.comment("生命之触：是否生效")
                .define("enabled", true);
        LIFE_TOUCH_REACH_BONUS = b.comment("生命之触：实体与方块交互距离加成（格）")
                .defineInRange("reachBonus", 2.0, 0.0, 8.0);
        LIFE_TOUCH_DOUBLE_STRIKE_CHANCE = b.comment("生命之触：连打（复刻上一击伤害）概率")
                .defineInRange("doubleStrikeChance", 0.25, 0.0, 1.0);
        LIFE_TOUCH_SELF_HURT_CHANCE = b.comment("生命之触：命中后自身受真实伤害概率")
                .defineInRange("selfHurtChance", 0.1, 0.0, 1.0);
        LIFE_TOUCH_SELF_HURT_DAMAGE = b.comment("生命之触：自身真实伤害点数（无视护甲，可致死）")
                .defineInRange("selfHurtDamage", 1.0, 0.0, 20.0);
        LIFE_TOUCH_HUNGER_COST_CHANCE = b.comment("生命之触：命中后扣饱食概率")
                .defineInRange("hungerCostChance", 0.2, 0.0, 1.0);
        LIFE_TOUCH_HUNGER_COST_AMOUNT = b.comment("生命之触：命中后扣饱食格数")
                .defineInRange("hungerCostAmount", 1, 0, 20);
        LIFE_TOUCH_HUNGER_RESTORE_CHANCE = b.comment("生命之触：命中后回饱食概率")
                .defineInRange("hungerRestoreChance", 0.3333, 0.0, 1.0);
        LIFE_TOUCH_HUNGER_RESTORE_AMOUNT = b.comment("生命之触：命中后回饱食格数")
                .defineInRange("hungerRestoreAmount", 1, 0, 20);
        LIFE_TOUCH_HIT_CACHE_TICKS = b.comment("生命之触：「上一击伤害」缓存时效 (tick)，超时后连打不再复刻")
                .defineInRange("hitCacheTicks", 100, 0, 1200);
        b.pop();

        // ---- 2 幼崽之心 ----
        b.push("cubHeart");
        CUB_HEART_ENABLED = b.comment("幼崽之心：是否生效")
                .define("enabled", true);
        CUB_HEART_ABSORPTION_RATIO = b.comment("幼崽之心：造成伤害转吸收（黄心）比例，累加不封顶")
                .defineInRange("absorptionRatio", 0.1, 0.0, 1.0);
        CUB_HEART_EFFECT_INTERVAL = b.comment("幼崽之心：效果刷新节流 (tick)，期间重复命中不重复结算")
                .defineInRange("effectInterval", 20, 1, 200);
        CUB_HEART_SLOW_HASTE_CHANCE = b.comment("幼崽之心：减速对方 / 加速自身 概率")
                .defineInRange("slowHasteChance", 0.1, 0.0, 1.0);
        CUB_HEART_SLOW_HASTE_AMPLITUDE = b.comment("幼崽之心：移速变动幅度（0.10 = 10%）")
                .defineInRange("slowHasteAmplitude", 0.10, 0.0, 1.0);
        CUB_HEART_SLOW_HASTE_TICKS = b.comment("幼崽之心：移速变动持续 (tick)，命中时只刷新时长")
                .defineInRange("slowHasteTicks", 600, 0, 600);
        CUB_HEART_DAMAGE_SHIFT_CHANCE = b.comment("幼崽之心：自身加伤 / 对方减伤 概率")
                .defineInRange("damageShiftChance", 0.05, 0.0, 1.0);
        CUB_HEART_DAMAGE_SHIFT_AMOUNT = b.comment("幼崽之心：伤害变动点数")
                .defineInRange("damageShiftAmount", 1.0, 0.0, 20.0);
        CUB_HEART_DAMAGE_SHIFT_TICKS = b.comment("幼崽之心：伤害变动持续 (tick)")
                .defineInRange("damageShiftTicks", 600, 0, 600);
        CUB_HEART_UNNAMEABLE_CHANCE = b.comment("幼崽之心：触发效果时自施不可名状概率")
                .defineInRange("unnameableChance", 0.1, 0.0, 1.0);
        CUB_HEART_UNNAMEABLE_AMPLIFIER = b.comment("幼崽之心：自施不可名状等级（1 = II 级）")
                .defineInRange("unnameableAmplifier", 1, 0, 4);
        CUB_HEART_UNNAMEABLE_TICKS = b.comment("幼崽之心：自施不可名状持续 (tick)")
                .defineInRange("unnameableTicks", 400, 0, 6000);
        CUB_HEART_EXCITEMENT_ATTACK_SPEED = b.comment("幼崽之心：「亢奋」攻速加成（0.20 = 20%）")
                .defineInRange("excitementAttackSpeed", 0.20, 0.0, 1.0);
        CUB_HEART_EXCITEMENT_TICKS = b.comment("幼崽之心：「亢奋」持续 (tick)，每次有效命中刷新")
                .defineInRange("excitementTicks", 100, 0, 600);
        b.pop();

        // ---- 3 母神之印 ----
        b.push("motherSeal");
        MOTHER_SEAL_ENABLED = b.comment("母神之印：是否生效")
                .define("enabled", true);
        MOTHER_SEAL_DAMAGE_BONUS = b.comment("母神之印：不可名状 I 级的伤害加成（II 级 ×1.5，III 级及以上 ×2.0）")
                .defineInRange("damageBonus", 2.0, 0.0, 20.0);
        MOTHER_SEAL_HEALTH_BONUS = b.comment("母神之印：不可名状 I 级的最大生命加成")
                .defineInRange("healthBonus", 4.0, 0.0, 40.0);
        MOTHER_SEAL_SPEED_BONUS = b.comment("母神之印：不可名状 I 级的移速加成（0.10 = 10%）")
                .defineInRange("speedBonus", 0.10, 0.0, 1.0);
        MOTHER_SEAL_ATTACK_SPEED_BONUS = b.comment("母神之印：不可名状 I 级的攻速加成（0.10 = 10%）")
                .defineInRange("attackSpeedBonus", 0.10, 0.0, 1.0);
        MOTHER_SEAL_SELF_UNNAMEABLE_PERIOD = b.comment("母神之印：自施不可名状周期 (tick)，0 = 关闭自施")
                .defineInRange("selfUnnameablePeriod", 1200, 0, 24000);
        MOTHER_SEAL_SELF_UNNAMEABLE_TICKS = b.comment("母神之印：自施不可名状持续 (tick)")
                .defineInRange("selfUnnameableTicks", 200, 0, 6000);
        MOTHER_SEAL_SELF_UNNAMEABLE_AMPLIFIER = b.comment("母神之印：自施不可名状等级（1 = II 级）")
                .defineInRange("selfUnnameableAmplifier", 1, 0, 4);
        MOTHER_SEAL_VEGETARIAN_HUNGER_COST = b.comment("母神之印：吃素食倒扣饱食格数")
                .defineInRange("vegetarianHungerCost", 1, 0, 20);
        MOTHER_SEAL_VEGETARIAN_NAUSEA_TICKS = b.comment("母神之印：吃素食恶心时长 (tick)")
                .defineInRange("vegetarianNauseaTicks", 100, 0, 1200);
        MOTHER_SEAL_RESIST_FACTOR = b.comment("母神之印：毒 / 火 / 凋零 / 爆炸伤害倍率（0.5 = 减半）")
                .defineInRange("resistFactor", 0.5, 0.0, 1.0);
        b.pop();

        // ---- 4 孕育之环 ----
        b.push("fertilityRing");
        FERTILITY_RING_ENABLED = b.comment("孕育之环：是否生效")
                .define("enabled", true);
        FERTILITY_RING_HEAL_CHANCE = b.comment("孕育之环：受伤回血概率")
                .defineInRange("healChance", 0.2, 0.0, 1.0);
        FERTILITY_RING_HEAL_AMOUNT = b.comment("孕育之环：受伤回血点数")
                .defineInRange("healAmount", 2.0, 0.0, 20.0);
        FERTILITY_RING_HEAL_HUNGER_COST = b.comment("孕育之环：回血同时扣饱食格数")
                .defineInRange("healHungerCost", 1, 0, 20);
        FERTILITY_RING_ATTACK_SPEED_BONUS = b.comment("孕育之环：触发后攻速加成（0.20 = 20%）")
                .defineInRange("attackSpeedBonus", 0.20, 0.0, 1.0);
        FERTILITY_RING_ATTACK_SPEED_TICKS = b.comment("孕育之环：攻速加成持续 (tick)")
                .defineInRange("attackSpeedTicks", 100, 0, 600);
        FERTILITY_RING_MEAT_HEAL_AMOUNT = b.comment("孕育之环：吃肉回血点数")
                .defineInRange("meatHealAmount", 3.0, 0.0, 20.0);
        FERTILITY_RING_SATIATED_MEAT_ONLY = b.comment("孕育之环：满饱食时仅允许进食肉类")
                .define("satiatedMeatOnly", true);
        FERTILITY_RING_STARVE_MULTIPLIER = b.comment("孕育之环：饥饿伤害倍率（最终伤害 = 原值 × 倍率）")
                .defineInRange("starveMultiplier", 3.0, 1.0, 20.0);
        FERTILITY_RING_STARVE_LETHAL = b.comment("孕育之环：饥饿伤害可致死（false = 最低留 1 点血）")
                .define("starveLethal", true);
        b.pop();

        // ---- 5 套装 ----
        b.push("set");
        SET_ATTACK_COUNT_REQUIRED = b.comment("套装：触发回饱食所需的有效攻击次数（持久化，不掉线衰减）")
                .defineInRange("attackCountRequired", 10, 1, 100);
        SET_ATTACK_COUNT_HUNGER_RESTORE = b.comment("套装：达标后回饱食格数")
                .defineInRange("attackCountHungerRestore", 1, 0, 20);
        SET_UNNAMEABLE_LEVEL_BONUS = b.comment("套装：不可名状等级加成（对所有施加来源统一生效）")
                .defineInRange("unnameableLevelBonus", 1, 0, 4);
        SET_SUPPRESS_DISTORTION = b.comment("套装：屏蔽不可名状的视野扭曲表现")
                .define("suppressDistortion", true);
        SET_DAMAGE_REDUCTION = b.comment("套装：受到的伤害减免（0.30 = 30%，与四类减半叠乘）")
                .defineInRange("damageReduction", 0.30, 0.0, 1.0);
        SET_UNNAMEABLE_CRIT_CHANCE = b.comment("套装：持不可名状时的暴击率加成（0.20 = +20%）")
                .defineInRange("unnameableCritChance", 0.20, 0.0, 1.0);
        SET_UNNAMEABLE_CRIT_DAMAGE = b.comment("套装：持不可名状时的暴击伤害加成（0.30 = 暴击倍率 +0.3）")
                .defineInRange("unnameableCritDamage", 0.30, 0.0, 100.0);
        SET_NEAR_DEATH_HEAL_PERCENT = b.comment("套装：濒死免死时恢复的最大生命占比（0.60 = 60%）")
                .defineInRange("nearDeathHealPercent", 0.60, 0.0, 1.0);
        SET_NEAR_DEATH_UNNAMEABLE_TICKS = b.comment("套装：濒死免死时获得的不可名状时长（tick，600 = 30 秒）")
                .defineInRange("nearDeathUnnameableTicks", 600, 0, 72000);
        SET_NEAR_DEATH_COOLDOWN_TICKS = b.comment("套装：濒死免死的冷却（tick，9600 = 480 秒）")
                .defineInRange("nearDeathCooldownTicks", 9600, 0, 72000);
        b.pop();

        // ---- 6 侵蚀 ----
        b.push("erosion");
        EROSION_ENABLED = b.comment("侵蚀：佩戴期间进度是否累加（总开关）")
                .define("enabled", true);
        EROSION_DURATION_MINUTES = b.comment("侵蚀：佩戴累计跑满所需分钟数（0 = 使用内置默认 480）")
                .defineInRange("durationMinutes", 480, 0, 10080);
        EROSION_NOTICE_THRESHOLDS = b.comment("侵蚀：必报节点百分比 [25, 50, 75, 100]（1~100）")
                .defineList("noticeThresholds", List.of(25, 50, 75, 100),
                        (Object o) -> o instanceof Number n && n.intValue() >= 1 && n.intValue() <= 100);
        EROSION_NOTICE_INTERVAL_MINUTES = b.comment("侵蚀：兜底提示间隔（分钟），非节点也按此周期播报")
                .defineInRange("noticeIntervalMinutes", 30, 1, 480);
        EROSION_STAT_REROLL_PERCENT = b.comment("侵蚀：乱码时数值重抽幅度（±%，0.25 = ±25%，保底不低于 1）")
                .defineInRange("statRerollPercent", 0.25, 0.0, 1.0);
        EROSION_NBT_FLUSH_TICKS = b.comment("侵蚀：进度落盘间隔 (tick)，摘下 / 死亡 / 下线时强制落盘")
                .defineInRange("nbtFlushTicks", 100, 20, 1200);
        EROSION_SCREEN_FLASH_ENABLED = b.comment("侵蚀：跑满时屏幕边缘泛红提示")
                .define("screenFlashEnabled", true);
        b.pop();

        // ---- 7 仪式与吸取 ----
        b.push("altar");
        ALTAR_DRAIN_ENABLED = b.comment("仪式吸取：配方齐备时是否自动吸取周边生物")
                .define("drainEnabled", true);
        ALTAR_DRAIN_RADIUS = b.comment("仪式吸取：吸取半径（格）")
                .defineInRange("drainRadius", 64, 4, 128);
        ALTAR_DRAIN_INTERVAL_TICKS = b.comment("仪式吸取：吸取间隔 (tick)")
                .defineInRange("drainIntervalTicks", 20, 1, 200);
        ALTAR_DRAIN_HEALTH_PERCENT = b.comment("仪式吸取：单次抽取的最大生命比例（0.10 = 10%）")
                .defineInRange("drainHealthPercent", 0.10, 0.0, 1.0);
        ALTAR_DRAIN_ENERGY_PER_HP = b.comment("仪式吸取：每 1 点血折算的生命能量")
                .defineInRange("drainEnergyPerHp", 1000L, 1L, 100_000L);
        ALTAR_DRAIN_MAX_TARGETS = b.comment("仪式吸取：单次最多吸取目标数（超出按距离优先）")
                .defineInRange("drainMaxTargets", 16, 1, 128);
        ALTAR_DRAIN_MAX_ENERGY_PER_TARGET = b.comment("仪式吸取：单只单次贡献上限（0 = 不限制）")
                .defineInRange("drainMaxEnergyPerTarget", 5000L, 0L, 1_000_000L);
        ALTAR_DRAIN_EXEMPT_CREATIVE = b.comment("仪式吸取：豁免创造模式玩家")
                .define("drainExemptCreative", true);
        ALTAR_NEW_RECIPE_TIER_REQUIRED = b.comment("四件饰品新仪式：所需的祭坛等级（生命结构台扫描等级）")
                .defineInRange("newRecipeTierRequired", 3, 1, 3);
        ALTAR_NEW_RECIPE_PROGRESS_MAX = b.comment("四件饰品新仪式：蓄能阈值（祭品不齐时进度冻结不归零）")
                .defineInRange("newRecipeProgressMax", 800_000L, 1L, Long.MAX_VALUE);
        ALTAR_LEGACY_PROGRESS_MAX = b.comment("旧生命融合锭仪式：蓄能阈值")
                .defineInRange("legacyProgressMax", 80_000L, 1L, Long.MAX_VALUE);
        b.pop();

        b.pop();
    }
}
