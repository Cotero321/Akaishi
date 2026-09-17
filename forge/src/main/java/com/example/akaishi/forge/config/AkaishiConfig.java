package com.example.akaishi.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * Forge 原生配置文件（common.toml）。所有可调数值经 ForgeConfigSpec 定义，
 * 通过 {@link AkaishiConfigSync} 在加载/重载时同步到 common 的 {@link com.example.akaishi.config.ModConfig}。
 * Forge 自动生成原生配置界面（Mods 列表 → Config）。
 */
public final class AkaishiConfig {

    public static final ForgeConfigSpec SPEC;

    // ==================== 反应堆 ====================
    public static final ForgeConfigSpec.LongValue REACTOR_ENERGY_PER_SLOT;
    public static final ForgeConfigSpec.IntValue REACTOR_BASE_TEMP;
    public static final ForgeConfigSpec.DoubleValue REACTOR_PASSIVE_COOL;
    public static final ForgeConfigSpec.DoubleValue REACTOR_COOLER_COOL;
    public static final ForgeConfigSpec.DoubleValue REACTOR_DRAIN_BASE;
    public static final ForgeConfigSpec.DoubleValue REACTOR_WASTE_RATIO;
    public static final ForgeConfigSpec.IntValue REACTOR_WASTE_CAPACITY;
    public static final ForgeConfigSpec.IntValue REACTOR_TEMP_MAX;
    public static final ForgeConfigSpec.IntValue REACTOR_TEMP_OPT_MIN;
    public static final ForgeConfigSpec.IntValue REACTOR_TEMP_OPT_MAX;
    public static final ForgeConfigSpec.IntValue REACTOR_TEMP_WARN;
    public static final ForgeConfigSpec.IntValue REACTOR_EXPLOSION_DELAY_TICKS;

    // ==================== 液体管道 ====================
    public static final ForgeConfigSpec.IntValue FLUID_PIPE_RATE;

    // ==================== 废品口 ====================
    public static final ForgeConfigSpec.IntValue WASTE_PORT_BUFFER_CAPACITY;

    // ==================== 衰竭保存桶 ====================
    public static final ForgeConfigSpec.LongValue EXHAUSTED_BARREL_CAPACITY;

    // ==================== 生命活化器 ====================
    public static final ForgeConfigSpec.LongValue LIFE_ACTIVATOR_LIFE_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_ACTIVATOR_COST_PER_MB;
    public static final ForgeConfigSpec.LongValue LIFE_ACTIVATOR_INPUT_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_ACTIVATOR_OUTPUT_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_ACTIVATOR_CONVERT_RATE;
    public static final ForgeConfigSpec.LongValue LIFE_CENTRIFUGE_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_CENTRIFUGE_INPUT_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_CENTRIFUGE_CONVERT_RATE;
    public static final ForgeConfigSpec.LongValue LIFE_CENTRIFUGE_COST_PER_MB;
    public static final ForgeConfigSpec.LongValue RECONSTRUCTOR_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.LongValue RECONSTRUCTOR_COST_PER_CRYSTAL;

    // ==================== 聚变燃料聚合器 ====================
    public static final ForgeConfigSpec.LongValue AGGREGATOR_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.LongValue AGGREGATOR_COST_PER_CRAFT;
    public static final ForgeConfigSpec.IntValue AGGREGATOR_PROCESS_TICKS;
    public static final ForgeConfigSpec.LongValue AGGREGATOR_PLASMA_CAPACITY;
    public static final ForgeConfigSpec.LongValue AGGREGATOR_PRODUCE_PER_CRAFT;

    // ==================== 离子体填装器 ====================
    public static final ForgeConfigSpec.LongValue FILLER_PLASMA_CAPACITY;
    public static final ForgeConfigSpec.LongValue FILLER_PLASMA_PER_ROD;
    public static final ForgeConfigSpec.IntValue FILLER_PROCESS_TICKS;

    // ==================== 赤石植物培养机 ====================
    public static final ForgeConfigSpec.LongValue PLANT_CULTIVATOR_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.IntValue PLANT_CULTIVATOR_TICKS;
    public static final ForgeConfigSpec.LongValue PLANT_CULTIVATOR_COST_PER_TICK;

    // ==================== 赤石压缩机 ====================
    public static final ForgeConfigSpec.LongValue COMPRESSOR_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.IntValue COMPRESSOR_TICKS;
    public static final ForgeConfigSpec.LongValue COMPRESSOR_COST_PER_TICK;

    // ==================== 赤石打粉机 ====================
    public static final ForgeConfigSpec.LongValue PULVERIZER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.IntValue PULVERIZER_TICKS;
    public static final ForgeConfigSpec.LongValue PULVERIZER_COST_PER_TICK;

    // ==================== 赤石变化器 ====================
    public static final ForgeConfigSpec.LongValue TRANSFORMER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.IntValue TRANSFORMER_TICKS;
    public static final ForgeConfigSpec.LongValue TRANSFORMER_COST_PER_TICK;

    // ==================== 赤石矿机 ====================
    public static final ForgeConfigSpec.IntValue MINER_TICKS_BASE;
    public static final ForgeConfigSpec.LongValue MINER_COST_PER_TICK_BASE;
    public static final ForgeConfigSpec.IntValue MINER_PRECISE_FORTUNE_DIVISOR;
    public static final ForgeConfigSpec.IntValue MINER_EXTRA_ORE_WEIGHT;

    // ==================== 衰竭区域 ====================
    public static final ForgeConfigSpec.LongValue DECAY_ZONE_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue DECAY_ZONE_SAMPLES_PER_TICK;

    // ==================== 衰变净化塔 ====================
    public static final ForgeConfigSpec.LongValue DECAY_PURIFIER_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.IntValue DECAY_PURIFIER_RANGE;
    public static final ForgeConfigSpec.LongValue DECAY_PURIFIER_COST_PER_TICK;
    public static final ForgeConfigSpec.LongValue DECAY_PURIFIER_TICKS_PER_TICK;

    // ==================== 聚变堆 ====================
    public static final ForgeConfigSpec.DoubleValue FUSION_EFFICIENCY_GROWTH;
    public static final ForgeConfigSpec.DoubleValue FUSION_COOLER_FRAME_BONUS;
    public static final ForgeConfigSpec.LongValue FUSION_COOLING_PER_PERCENT;
    public static final ForgeConfigSpec.IntValue FUSION_BASE_TEMP;
    public static final ForgeConfigSpec.IntValue FUSION_TEMP_MAX;
    public static final ForgeConfigSpec.IntValue FUSION_TEMP_TRIP;
    public static final ForgeConfigSpec.IntValue FUSION_TEMP_OPT_MIN;
    public static final ForgeConfigSpec.IntValue FUSION_TEMP_OPT_MAX;
    public static final ForgeConfigSpec.IntValue FUSION_TEMP_RESUME;
    public static final ForgeConfigSpec.IntValue FUSION_TEMP_STEP;
    public static final ForgeConfigSpec.IntValue FUSION_COOLER_DURABILITY_INTERVAL;
    public static final ForgeConfigSpec.LongValue FUSION_ASH_PER_ENERGY;
    public static final ForgeConfigSpec.LongValue FUSION_ROD_ENERGY;

    // ==================== 无线赤能源 ====================
    public static final ForgeConfigSpec.DoubleValue WIRELESS_BASE_LOSS;
    public static final ForgeConfigSpec.DoubleValue WIRELESS_LOSS_PER_BLOCK;
    public static final ForgeConfigSpec.DoubleValue WIRELESS_MAX_LOSS;
    public static final ForgeConfigSpec.DoubleValue WIRELESS_CROSS_DIM_LOSS;
    public static final ForgeConfigSpec.DoubleValue WIRELESS_LOSS_REDUCTION_PER_MODULE;

    // ==================== 器官·品质曲线 ====================
    /** 品质 I~IV 属性加成倍率（下标 = 品质序数，下同） */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Double>> ORGAN_TIER_MULTIPLIER;
    /** 品质 I~IV 移植基础排斥 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> ORGAN_TIER_BASE_REJECTION;
    /** 品质 I~IV 排斥增长间隔（秒） */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> ORGAN_TIER_GROWTH_INTERVAL;

    // ==================== 基因来源组排斥系数 ====================
    /** 温血/亡灵/爆炸/异变/末影/Boss/龙 七组排斥系数 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Double>> GROUP_REJECTION_FACTOR;

    // ==================== 纯度联动 ====================
    public static final ForgeConfigSpec.DoubleValue PURITY_REJECTION_CAP;
    public static final ForgeConfigSpec.DoubleValue PURITY_COMPAT_WEIGHT;

    // ==================== 排斥·标尺与阈值 ====================
    public static final ForgeConfigSpec.IntValue MAX_REJECTION;
    public static final ForgeConfigSpec.IntValue REJECTION_WARNING;
    public static final ForgeConfigSpec.IntValue REJECTION_POISON;
    public static final ForgeConfigSpec.IntValue COMPAT_SEVERE_THRESHOLD;
    public static final ForgeConfigSpec.IntValue SLOT_DEBUFF_CLEAN_THRESHOLD;
    public static final ForgeConfigSpec.IntValue SLOT_DEBUFF_SEVERE_THRESHOLD;
    public static final ForgeConfigSpec.IntValue GROWTH_INTERVAL_MIN_TICKS;
    public static final ForgeConfigSpec.IntValue CONFLICT_PUNISH_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue CONFLICT_PUNISH_DAMAGE;
    public static final ForgeConfigSpec.IntValue OVERLOAD_LIGHT;
    public static final ForgeConfigSpec.IntValue OVERLOAD_HEAVY;

    // ==================== 排异中和剂（血清） ====================
    public static final ForgeConfigSpec.IntValue SERUM_WASH_REDUCE;
    public static final ForgeConfigSpec.IntValue SERUM_WASH_LIMIT;
    public static final ForgeConfigSpec.IntValue SERUM_COOLDOWN_TICKS;

    // ==================== 突变词条 ====================
    public static final ForgeConfigSpec.DoubleValue TRAIT_BENIGN_RATIO;
    public static final ForgeConfigSpec.IntValue TRAIT_RARITY_HIGH_THRESHOLD;
    public static final ForgeConfigSpec.IntValue TRAIT_RARITY_MID_THRESHOLD;

    // ==================== 培养机·品质升级 ====================
    /** I→II / II→III / III→IV 成功率（百分比） */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_UPGRADE_SUCCESS;
    /** 三段升级生命能量消耗 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_UPGRADE_ENERGY;
    /** 三段升级固态物消耗 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_UPGRADE_SOLID;
    /** 三段升级耗时（tick） */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_UPGRADE_TICKS;
    /** 升级成功额外适配加成 */
    public static final ForgeConfigSpec.IntValue CULTIVATOR_UPGRADE_COMPAT_BONUS;

    // ==================== 机器全局倍率 ====================
    public static final ForgeConfigSpec.DoubleValue MACHINE_WORK_SPEED;
    public static final ForgeConfigSpec.DoubleValue MACHINE_COST_MULTIPLIER;

    // ==================== 机械改造机器 ====================
    /** 机械三机赤能源缓冲容量（共用） */
    public static final ForgeConfigSpec.LongValue MECH_CHISHI_CAPACITY;
    /** 机械三机生命能量缓冲容量（共用） */
    public static final ForgeConfigSpec.LongValue MECH_LIFE_CAPACITY;
    /** 模板制造厂：塑形一次赤能源消耗 */
    public static final ForgeConfigSpec.LongValue MECH_TEMPLATE_CHISHI_COST;
    /** 模板制造厂：塑形一次生命能量消耗 */
    public static final ForgeConfigSpec.LongValue MECH_TEMPLATE_LIFE_COST;
    /** 模板制造厂：塑形一次耗时（tick） */
    public static final ForgeConfigSpec.IntValue MECH_TEMPLATE_TICKS;
    /** 加工厂：器官基价赤能（下标=器官序数 0~8），0=用内置默认 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Long>> MECH_PROCESS_CHISHI_BASE;
    /** 加工厂：器官基价生命能（下标同上），0=用内置默认 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Long>> MECH_PROCESS_LIFE_BASE;
    /** 加工厂：器官耗时基价 tick（下标同上），0=用内置默认 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> MECH_PROCESS_TICKS_BASE;
    /** 加工厂：部件系数百分数（下标=部件序数 0~3），0=用内置默认 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> MECH_PROCESS_PART_FACTOR;
    /** 加工厂：材料消耗份数（下标=部件序数 0~3），0=用内置默认 */
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> MECH_PROCESS_MATERIAL_COUNT;
    /** 组装台：组装一次赤能源消耗（固定） */
    public static final ForgeConfigSpec.LongValue MECH_ASSEMBLY_CHISHI_COST;
    /** 组装台：组装一次生命能量消耗（固定） */
    public static final ForgeConfigSpec.LongValue MECH_ASSEMBLY_LIFE_COST;
    /** 组装台：组装一次耗时（tick） */
    public static final ForgeConfigSpec.IntValue MECH_ASSEMBLY_TICKS;

    // ==================== 机械义体属性换算 ====================
    /** 机械义体：生命值权重 → 生命上限换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_HEALTH_SCALE;
    /** 机械义体：攻击伤害权重 → 攻击伤害换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_ATTACK_SCALE;
    /** 机械义体：攻击速度权重 → 攻击速度换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_ATTACK_SPEED_SCALE;
    /** 机械义体：移动速度权重 → 移动速度换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_MOVEMENT_SPEED_SCALE;
    /** 机械义体：护甲权重 → 护甲值换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_ARMOR_SCALE;
    /** 机械义体：暴击率权重 → 暴击率换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_CRIT_CHANCE_SCALE;
    /** 机械义体：暴击伤害权重 → 暴击伤害换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_CRIT_DAMAGE_SCALE;
    /** 机械义体：攻击范围权重 → 攻击距离换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_RANGE_SCALE;
    /** 机械义体：闪避权重 → 闪避率换算倍率 */
    public static final ForgeConfigSpec.DoubleValue MECH_BODY_DODGE_SCALE;

    // ==================== 基因属性权重 ====================
    /** 基因属性权重：生物专精属性轴的最大加成比例（最强轴 ×(1+k)） */
    public static final ForgeConfigSpec.DoubleValue GENE_WEIGHT_STRENGTH;

    // ==================== 底层战斗（暴击/闪避） ====================
    /** 底层战斗：暴击总开关 */
    public static final ForgeConfigSpec.BooleanValue COMBAT_CRIT_ENABLED;
    /** 底层战斗：闪避总开关 */
    public static final ForgeConfigSpec.BooleanValue COMBAT_DODGE_ENABLED;
    /** 底层战斗：暴击率上限 */
    public static final ForgeConfigSpec.DoubleValue COMBAT_CRIT_CHANCE_CAP;
    /** 底层战斗：暴击伤害上限（追加倍率口径） */
    public static final ForgeConfigSpec.DoubleValue COMBAT_CRIT_DAMAGE_CAP;
    /** 底层战斗：闪避上限 */
    public static final ForgeConfigSpec.DoubleValue COMBAT_DODGE_CHANCE_CAP;

    // ==================== 机制开关 ====================
    public static final ForgeConfigSpec.BooleanValue DECAY_ZONE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SUNLIGHT_BURN_ENABLED;
    public static final ForgeConfigSpec.BooleanValue OVERLOAD_ENABLED;

    // ---- 赤石饰品扩展槽 ----
    public static final ForgeConfigSpec.BooleanValue CURIO_SLOT_UNLOCK_REQUIRED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CURIO_SLOT_UNLOCK_THRESHOLDS;

    // ---- 禁断四件 · 1 生命之触 ----
    public static final ForgeConfigSpec.BooleanValue LIFE_TOUCH_ENABLED;
    public static final ForgeConfigSpec.DoubleValue LIFE_TOUCH_REACH_BONUS;
    public static final ForgeConfigSpec.DoubleValue LIFE_TOUCH_DOUBLE_STRIKE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue LIFE_TOUCH_SELF_HURT_CHANCE;
    public static final ForgeConfigSpec.DoubleValue LIFE_TOUCH_SELF_HURT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue LIFE_TOUCH_HUNGER_COST_CHANCE;
    public static final ForgeConfigSpec.IntValue LIFE_TOUCH_HUNGER_COST_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue LIFE_TOUCH_HUNGER_RESTORE_CHANCE;
    public static final ForgeConfigSpec.IntValue LIFE_TOUCH_HUNGER_RESTORE_AMOUNT;
    public static final ForgeConfigSpec.IntValue LIFE_TOUCH_HIT_CACHE_TICKS;

    // ---- 禁断四件 · 2 幼崽之心 ----
    public static final ForgeConfigSpec.BooleanValue CUB_HEART_ENABLED;
    public static final ForgeConfigSpec.DoubleValue CUB_HEART_ABSORPTION_RATIO;
    public static final ForgeConfigSpec.IntValue CUB_HEART_EFFECT_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue CUB_HEART_SLOW_HASTE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CUB_HEART_SLOW_HASTE_AMPLITUDE;
    public static final ForgeConfigSpec.IntValue CUB_HEART_SLOW_HASTE_TICKS;
    public static final ForgeConfigSpec.DoubleValue CUB_HEART_DAMAGE_SHIFT_CHANCE;
    public static final ForgeConfigSpec.DoubleValue CUB_HEART_DAMAGE_SHIFT_AMOUNT;
    public static final ForgeConfigSpec.IntValue CUB_HEART_DAMAGE_SHIFT_TICKS;
    public static final ForgeConfigSpec.DoubleValue CUB_HEART_UNNAMEABLE_CHANCE;
    public static final ForgeConfigSpec.IntValue CUB_HEART_UNNAMEABLE_AMPLIFIER;
    public static final ForgeConfigSpec.IntValue CUB_HEART_UNNAMEABLE_TICKS;
    public static final ForgeConfigSpec.DoubleValue CUB_HEART_EXCITEMENT_ATTACK_SPEED;
    public static final ForgeConfigSpec.IntValue CUB_HEART_EXCITEMENT_TICKS;

    // ---- 禁断四件 · 3 母神之印 ----
    public static final ForgeConfigSpec.BooleanValue MOTHER_SEAL_ENABLED;
    public static final ForgeConfigSpec.DoubleValue MOTHER_SEAL_DAMAGE_BONUS;
    public static final ForgeConfigSpec.DoubleValue MOTHER_SEAL_HEALTH_BONUS;
    public static final ForgeConfigSpec.DoubleValue MOTHER_SEAL_SPEED_BONUS;
    public static final ForgeConfigSpec.DoubleValue MOTHER_SEAL_ATTACK_SPEED_BONUS;
    public static final ForgeConfigSpec.IntValue MOTHER_SEAL_SELF_UNNAMEABLE_PERIOD;
    public static final ForgeConfigSpec.IntValue MOTHER_SEAL_SELF_UNNAMEABLE_TICKS;
    public static final ForgeConfigSpec.IntValue MOTHER_SEAL_SELF_UNNAMEABLE_AMPLIFIER;
    public static final ForgeConfigSpec.IntValue MOTHER_SEAL_VEGETARIAN_HUNGER_COST;
    public static final ForgeConfigSpec.IntValue MOTHER_SEAL_VEGETARIAN_NAUSEA_TICKS;
    public static final ForgeConfigSpec.DoubleValue MOTHER_SEAL_RESIST_FACTOR;

    // ---- 禁断四件 · 4 孕育之环 ----
    public static final ForgeConfigSpec.BooleanValue FERTILITY_RING_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FERTILITY_RING_HEAL_CHANCE;
    public static final ForgeConfigSpec.DoubleValue FERTILITY_RING_HEAL_AMOUNT;
    public static final ForgeConfigSpec.IntValue FERTILITY_RING_HEAL_HUNGER_COST;
    public static final ForgeConfigSpec.DoubleValue FERTILITY_RING_ATTACK_SPEED_BONUS;
    public static final ForgeConfigSpec.IntValue FERTILITY_RING_ATTACK_SPEED_TICKS;
    public static final ForgeConfigSpec.DoubleValue FERTILITY_RING_MEAT_HEAL_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue FERTILITY_RING_SATIATED_MEAT_ONLY;
    public static final ForgeConfigSpec.DoubleValue FERTILITY_RING_STARVE_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue FERTILITY_RING_STARVE_LETHAL;

    // ---- 禁断四件 · 5 套装 ----
    public static final ForgeConfigSpec.IntValue SET_ATTACK_COUNT_REQUIRED;
    public static final ForgeConfigSpec.IntValue SET_ATTACK_COUNT_HUNGER_RESTORE;
    public static final ForgeConfigSpec.IntValue SET_UNNAMEABLE_LEVEL_BONUS;
    public static final ForgeConfigSpec.BooleanValue SET_SUPPRESS_DISTORTION;
    public static final ForgeConfigSpec.DoubleValue SET_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SET_UNNAMEABLE_CRIT_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SET_UNNAMEABLE_CRIT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SET_NEAR_DEATH_HEAL_PERCENT;
    public static final ForgeConfigSpec.IntValue SET_NEAR_DEATH_UNNAMEABLE_TICKS;
    public static final ForgeConfigSpec.IntValue SET_NEAR_DEATH_COOLDOWN_TICKS;

    // ---- 禁断四件 · 6 侵蚀 ----
    public static final ForgeConfigSpec.BooleanValue EROSION_ENABLED;
    public static final ForgeConfigSpec.IntValue EROSION_DURATION_MINUTES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> EROSION_NOTICE_THRESHOLDS;
    public static final ForgeConfigSpec.IntValue EROSION_NOTICE_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.DoubleValue EROSION_STAT_REROLL_PERCENT;
    public static final ForgeConfigSpec.IntValue EROSION_NBT_FLUSH_TICKS;
    public static final ForgeConfigSpec.BooleanValue EROSION_SCREEN_FLASH_ENABLED;

    // ---- 禁断四件 · 7 仪式与吸取 ----
    public static final ForgeConfigSpec.BooleanValue ALTAR_DRAIN_ENABLED;
    public static final ForgeConfigSpec.IntValue ALTAR_DRAIN_RADIUS;
    public static final ForgeConfigSpec.IntValue ALTAR_DRAIN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue ALTAR_DRAIN_HEALTH_PERCENT;
    public static final ForgeConfigSpec.LongValue ALTAR_DRAIN_ENERGY_PER_HP;
    public static final ForgeConfigSpec.IntValue ALTAR_DRAIN_MAX_TARGETS;
    public static final ForgeConfigSpec.LongValue ALTAR_DRAIN_MAX_ENERGY_PER_TARGET;
    public static final ForgeConfigSpec.BooleanValue ALTAR_DRAIN_EXEMPT_CREATIVE;
    public static final ForgeConfigSpec.IntValue ALTAR_NEW_RECIPE_TIER_REQUIRED;
    public static final ForgeConfigSpec.LongValue ALTAR_NEW_RECIPE_PROGRESS_MAX;
    public static final ForgeConfigSpec.LongValue ALTAR_LEGACY_PROGRESS_MAX;

    // ---- 生命研究机器 ----
    public static final ForgeConfigSpec.LongValue GENE_ANALYZER_LIFE_COST;
    public static final ForgeConfigSpec.LongValue GENE_ANALYZER_LIFE_CAPACITY;
    public static final ForgeConfigSpec.IntValue GENE_ANALYZER_PROCESS_TICKS;
    public static final ForgeConfigSpec.DoubleValue GENE_ANALYZER_MIN_SUCCESS;
    public static final ForgeConfigSpec.DoubleValue GENE_ANALYZER_MAX_SUCCESS;
    public static final ForgeConfigSpec.LongValue LIFE_STRUCT_LIFE_COST;
    public static final ForgeConfigSpec.IntValue LIFE_STRUCT_SOLID_COST;
    public static final ForgeConfigSpec.LongValue LIFE_STRUCT_LIFE_CAPACITY;
    public static final ForgeConfigSpec.IntValue LIFE_STRUCT_PROCESS_TICKS;
    public static final ForgeConfigSpec.LongValue LIFE_BREEDER_LIFE_COST;
    public static final ForgeConfigSpec.IntValue LIFE_BREEDER_CRYSTAL_COST;
    public static final ForgeConfigSpec.LongValue LIFE_BREEDER_LIFE_CAPACITY;
    public static final ForgeConfigSpec.IntValue LIFE_BREEDER_PROCESS_TICKS;
    public static final ForgeConfigSpec.DoubleValue LIFE_BREEDER_MIN_SUCCESS;
    public static final ForgeConfigSpec.DoubleValue LIFE_BREEDER_MAX_SUCCESS;
    public static final ForgeConfigSpec.LongValue TRAIT_REFORGER_LIFE_COST;
    public static final ForgeConfigSpec.LongValue TRAIT_REFORGER_LIFE_CAPACITY;
    public static final ForgeConfigSpec.IntValue TRAIT_REFORGER_PROCESS_TICKS;
    public static final ForgeConfigSpec.IntValue TRAIT_REFORGER_CRYSTAL_PER_RARITY;
    public static final ForgeConfigSpec.LongValue TRANSGENE_FACTORY_LIFE_COST;
    public static final ForgeConfigSpec.LongValue TRANSGENE_FACTORY_LIFE_CAPACITY;
    public static final ForgeConfigSpec.IntValue TRANSGENE_FACTORY_PROCESS_TICKS;
    public static final ForgeConfigSpec.IntValue SURGERY_IMPLANT_SOLID_COST;
    public static final ForgeConfigSpec.LongValue SURGERY_IMPLANT_LIFE_COST;
    public static final ForgeConfigSpec.IntValue SURGERY_EXTRACT_SOLID_COST;
    public static final ForgeConfigSpec.LongValue SURGERY_EXTRACT_LIFE_COST;
    public static final ForgeConfigSpec.LongValue SURGERY_LIFE_CAPACITY;
    public static final ForgeConfigSpec.IntValue SURGERY_PROCESS_TICKS;
    public static final ForgeConfigSpec.LongValue ORGAN_VAULT_LIFE_CAPACITY;
    public static final ForgeConfigSpec.LongValue ORGAN_VAULT_KEEP_COST;
    public static final ForgeConfigSpec.LongValue POTION_TABLE_LIFE_CAPACITY;

    // ---- 能量机器 ----
    public static final ForgeConfigSpec.LongValue ENERGY_PROCESSOR_CHISHI_RATE;
    public static final ForgeConfigSpec.LongValue ENERGY_PROCESSOR_CHISHI_CAPACITY;
    public static final ForgeConfigSpec.LongValue ENERGY_PROCESSOR_TANK_CAPACITY;
    public static final ForgeConfigSpec.LongValue ENERGY_PROCESSOR_CHISHI_COST;
    public static final ForgeConfigSpec.LongValue ENERGY_LIQUEFIER_CHISHI_RATE;
    public static final ForgeConfigSpec.LongValue ENERGY_LIQUEFIER_CHISHI_CAPACITY;
    public static final ForgeConfigSpec.LongValue ENERGY_LIQUEFIER_TANK_CAPACITY;
    public static final ForgeConfigSpec.LongValue FUEL_MIXER_CHISHI_RATE;
    public static final ForgeConfigSpec.LongValue FUEL_MIXER_CHISHI_CAPACITY;
    public static final ForgeConfigSpec.LongValue FUEL_MIXER_CHISHI_COST;
    public static final ForgeConfigSpec.LongValue FUEL_MIXER_TANK_CAPACITY;
    public static final ForgeConfigSpec.LongValue FUEL_CANNER_TANK_CAPACITY;
    public static final ForgeConfigSpec.LongValue FUEL_CANNER_FILL_RATE;
    public static final ForgeConfigSpec.LongValue ENERGY_AGGREGATOR_PER_INGOT;
    public static final ForgeConfigSpec.LongValue ENERGY_AGGREGATOR_PER_GEODE;
    public static final ForgeConfigSpec.LongValue ENERGY_AGGREGATOR_CAPACITY;
    public static final ForgeConfigSpec.IntValue ENERGY_GENERATOR_RATE;
    public static final ForgeConfigSpec.IntValue ENERGY_ASSEMBLY_RATE;
    public static final ForgeConfigSpec.IntValue SUPER_GENERATOR_CORE_RATE;
    public static final ForgeConfigSpec.LongValue ENERGY_CELL_BASE_CAPACITY;
    public static final ForgeConfigSpec.LongValue UPGRADE_STATION_PER_UPGRADE;
    public static final ForgeConfigSpec.LongValue UPGRADE_STATION_CAPACITY;
    public static final ForgeConfigSpec.LongValue EQUIPMENT_FORGER_PER_FORGE;
    public static final ForgeConfigSpec.LongValue EQUIPMENT_FORGER_CAPACITY;

    // ---- 净化与矩阵 ----
    public static final ForgeConfigSpec.IntValue PURIFIER_ENERGY_PER_TICK;
    public static final ForgeConfigSpec.IntValue PURIFIER_BURN_RATE;
    public static final ForgeConfigSpec.LongValue PURIFIER_TOTAL_COST;
    public static final ForgeConfigSpec.LongValue PURIFIER_RATE_FORMED;
    public static final ForgeConfigSpec.LongValue PURIFIER_MATRIX_TOTAL_COST;
    public static final ForgeConfigSpec.LongValue PURIFIER_MATRIX_RATE_FORMED;
    public static final ForgeConfigSpec.LongValue LIFE_PURIFIER_CHISHI_RATE;
    public static final ForgeConfigSpec.LongValue LIFE_PURIFIER_TOTAL_COST;
    public static final ForgeConfigSpec.LongValue LIFE_PURIFIER_LIFE_COST;
    public static final ForgeConfigSpec.LongValue LIFE_PURIFIER_CHISHI_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_PURIFIER_LIFE_CAPACITY;
    public static final ForgeConfigSpec.IntValue LIFE_MATRIX_CONVERSIONS_PER_TICK;
    public static final ForgeConfigSpec.LongValue LIFE_MATRIX_CONVERSION_COST;
    public static final ForgeConfigSpec.LongValue LIFE_MATRIX_CHISHI_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_MATRIX_LIFE_CAPACITY;
    public static final ForgeConfigSpec.IntValue LIFE_CONVERSION_PER_TICK;
    public static final ForgeConfigSpec.LongValue LIFE_CONVERSION_CHISHI_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_CONVERSION_LIFE_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_AGGREGATION_COST;
    public static final ForgeConfigSpec.LongValue LIFE_AGGREGATION_OUTPUT;
    public static final ForgeConfigSpec.LongValue LIFE_AGGREGATION_CHISHI_CAPACITY;
    public static final ForgeConfigSpec.LongValue LIFE_AGGREGATION_LIFE_CAPACITY;

    // ---- 端口与电池缓冲 ----
    public static final ForgeConfigSpec.LongValue LIFE_MATRIX_INPUT_PORT_BUFFER;
    public static final ForgeConfigSpec.LongValue LIFE_MATRIX_OUTPUT_PORT_BUFFER;
    public static final ForgeConfigSpec.LongValue PURIFIER_INPUT_PORT_BUFFER;
    public static final ForgeConfigSpec.LongValue MINER_PORT_BUFFER;
    public static final ForgeConfigSpec.LongValue MINER_ENERGY_INPUT_BUFFER;
    public static final ForgeConfigSpec.LongValue WIRELESS_INPUT_PORT_BUFFER;
    public static final ForgeConfigSpec.LongValue WIRELESS_OUTPUT_PORT_BUFFER;
    public static final ForgeConfigSpec.LongValue GEN_ENERGY_OUTPUT_BUFFER;
    public static final ForgeConfigSpec.LongValue FUSION_ENERGY_OUTPUT_BUFFER;
    public static final ForgeConfigSpec.LongValue REACTOR_ENERGY_OUTPUT_BUFFER;
    public static final ForgeConfigSpec.LongValue LIFE_ENERGY_CELL_SERIALIZER_CAPACITY;
    public static final ForgeConfigSpec.LongValue PLASMA_TANK_CAPACITY;
    public static final ForgeConfigSpec.LongValue ITEM_TERMINAL_ENERGY_BUFFER;
    public static final ForgeConfigSpec.LongValue ITEM_TERMINAL_ENERGY_PORT_BUFFER;

    // ---- 培养机提纯与分馏机 ----
    public static final ForgeConfigSpec.LongValue CULTIVATOR_LIFE_CAPACITY;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_PURIFY_SUCCESS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Long>> CULTIVATOR_PURIFY_ENERGY;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_PURIFY_SOLID;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> CULTIVATOR_PURIFY_TICKS;
    public static final ForgeConfigSpec.IntValue CULTIVATOR_PURIFY_GAIN;
    public static final ForgeConfigSpec.LongValue FRACTIONATOR_ENERGY_CAPACITY;
    public static final ForgeConfigSpec.LongValue FRACTIONATOR_COST_PER_CRAFT;
    public static final ForgeConfigSpec.IntValue FRACTIONATOR_PROCESS_TICKS;

    // ---- 价值分（统一存储库定价内核） ----
    public static final ForgeConfigSpec.DoubleValue VALUE_COST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue VALUE_INGREDIENT_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue VALUE_MAGIC_BONUS;
    public static final ForgeConfigSpec.DoubleValue VALUE_INGREDIENT_CAP;
    public static final ForgeConfigSpec.IntValue VALUE_ITERATIONS;
    public static final ForgeConfigSpec.BooleanValue VALUE_LOOT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue VALUE_LOOT_AUTO_APPLY;
    public static final ForgeConfigSpec.DoubleValue VALUE_LOOT_CAP;
    public static final ForgeConfigSpec.BooleanValue VALUE_FLUID_ENABLED;
    public static final ForgeConfigSpec.DoubleValue VALUE_FLUID_PER_MB_CAP;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_OVERRIDES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_FLUID_VALUES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_TAG_VALUES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_TIER_BONUS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_KEYWORD_EXCLUSIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALUE_LOOT_BLACKLIST;
    public static final ForgeConfigSpec.IntValue UNIFIED_VAULT_ROWS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("reactor");
        REACTOR_ENERGY_PER_SLOT = b.comment("Max Chi Energy output per fuel slot per tick (utilization 10)")
                .defineInRange("energyPerSlot", 1_500_000L, 1L, Long.MAX_VALUE);
        REACTOR_BASE_TEMP = b.comment("Base reactor temperature (no fuel heat)")
                .defineInRange("baseTemp", 300, 0, Integer.MAX_VALUE);
        REACTOR_PASSIVE_COOL = b.comment("Passive cooling coefficient (fixed 0.2)")
                .defineInRange("passiveCool", 0.2, 0.0, 1.0);
        REACTOR_COOLER_COOL = b.comment("Heat sink cooling coefficient (0.7 x sink efficiency)")
                .defineInRange("coolerCool", 0.7, 0.0, 10.0);
        REACTOR_DRAIN_BASE = b.comment("Fuel consumed per slot per tick (mb)")
                .defineInRange("drainBase", 1.0 / 50.0, 0.0001, 1000.0);
        REACTOR_WASTE_RATIO = b.comment("Fuel consumed -> waste produced ratio")
                .defineInRange("wasteRatio", 0.2, 0.0, 10.0);
        REACTOR_WASTE_CAPACITY = b.comment("Controller waste buffer capacity (mb)")
                .defineInRange("wasteCapacity", 64_000, 1, Integer.MAX_VALUE);
        REACTOR_TEMP_MAX = b.comment("Max temperature: at this value the explosion countdown starts")
                .defineInRange("tempMax", 1000, 1, Integer.MAX_VALUE);
        REACTOR_TEMP_OPT_MIN = b.comment("Optimal yield temperature range lower bound")
                .defineInRange("tempOptMin", 400, 0, Integer.MAX_VALUE);
        REACTOR_TEMP_OPT_MAX = b.comment("Optimal yield temperature range upper bound")
                .defineInRange("tempOptMax", 700, 0, Integer.MAX_VALUE);
        REACTOR_TEMP_WARN = b.comment("High temperature warning threshold")
                .defineInRange("tempWarn", 850, 0, Integer.MAX_VALUE);
        REACTOR_EXPLOSION_DELAY_TICKS = b.comment("Delay from max temperature to explosion (ticks, 10s)")
                .defineInRange("explosionDelayTicks", 200, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("fluid_pipe");
        FLUID_PIPE_RATE = b.comment("Max transfer per pipe segment per tick (mb)")
                .defineInRange("rate", 4000, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("waste_port");
        WASTE_PORT_BUFFER_CAPACITY = b.comment("Waste port waste buffer capacity (mb)")
                .defineInRange("bufferCapacity", 64_000, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("exhausted_barrel");
        EXHAUSTED_BARREL_CAPACITY = b.comment("Exhausted fuel barrel capacity (mb)")
                .defineInRange("capacity", 1_000_000L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("life_activator");
        LIFE_ACTIVATOR_LIFE_CAPACITY = b.comment("Life energy storage capacity")
                .defineInRange("lifeCapacity", 200_000L, 1L, Long.MAX_VALUE);
        LIFE_ACTIVATOR_COST_PER_MB = b.comment("Life energy consumed per mb converted")
                .defineInRange("costPerMb", 100L, 1L, Long.MAX_VALUE);
        LIFE_ACTIVATOR_INPUT_CAPACITY = b.comment("Input tank (waste) capacity (mb)")
                .defineInRange("inputCapacity", 8_000L, 1L, Long.MAX_VALUE);
        LIFE_ACTIVATOR_OUTPUT_CAPACITY = b.comment("Output tank (activated liquid) capacity (mb)")
                .defineInRange("outputCapacity", 16_000L, 1L, Long.MAX_VALUE);
        LIFE_ACTIVATOR_CONVERT_RATE = b.comment("Max conversion per tick (mb)")
                .defineInRange("convertRate", 4L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("life_centrifuge");
        LIFE_CENTRIFUGE_ENERGY_CAPACITY = b.comment("Akaishi energy storage capacity")
                .defineInRange("energyCapacity", 100_000L, 1L, Long.MAX_VALUE);
        LIFE_CENTRIFUGE_INPUT_CAPACITY = b.comment("Input tank (activated liquid) capacity (mb)")
                .defineInRange("inputCapacity", 64_000L, 1L, Long.MAX_VALUE);
        LIFE_CENTRIFUGE_CONVERT_RATE = b.comment("Max separation per tick (mb)")
                .defineInRange("convertRate", 8L, 1L, Long.MAX_VALUE);
        LIFE_CENTRIFUGE_COST_PER_MB = b.comment("Akaishi energy consumed per mb separated")
                .defineInRange("costPerMb", 50L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("item_reconstructor");
        RECONSTRUCTOR_ENERGY_CAPACITY = b.comment("Akaishi energy storage capacity")
                .defineInRange("energyCapacity", 100_000L, 1L, Long.MAX_VALUE);
        RECONSTRUCTOR_COST_PER_CRYSTAL = b.comment("Akaishi energy consumed per exhausted crystal")
                .defineInRange("costPerCrystal", 50L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("fusion_fuel_aggregator");
        AGGREGATOR_ENERGY_CAPACITY = b.comment("Akaishi energy storage capacity")
                .defineInRange("energyCapacity", 100_000L, 1L, Long.MAX_VALUE);
        AGGREGATOR_COST_PER_CRAFT = b.comment("Akaishi energy consumed per activated component")
                .defineInRange("costPerCraft", 2_000L, 1L, Long.MAX_VALUE);
        AGGREGATOR_PROCESS_TICKS = b.comment("Ticks to aggregate one component into plasma")
                .defineInRange("processTicks", 100, 1, Integer.MAX_VALUE);
        AGGREGATOR_PLASMA_CAPACITY = b.comment("Per-plasma output tank capacity (mb)")
                .defineInRange("plasmaCapacity", 8_000L, 1L, Long.MAX_VALUE);
        AGGREGATOR_PRODUCE_PER_CRAFT = b.comment("Plasma produced per component (mb)")
                .defineInRange("producePerCraft", 1_000L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("plasma_filler");
        FILLER_PLASMA_CAPACITY = b.comment("Per-plasma input tank capacity (mb)")
                .defineInRange("plasmaCapacity", 8_000L, 1L, Long.MAX_VALUE);
        FILLER_PLASMA_PER_ROD = b.comment("Plasma consumed per fusion rod (mb)")
                .defineInRange("plasmaPerRod", 1_000L, 1L, Long.MAX_VALUE);
        FILLER_PROCESS_TICKS = b.comment("Ticks to fill one rod into a plasma rod")
                .defineInRange("processTicks", 100, 1, Integer.MAX_VALUE);
        b.pop();

        b.push("plant_cultivator");
        PLANT_CULTIVATOR_ENERGY_CAPACITY = b.comment("Akaishi energy buffer capacity")
                .defineInRange("energyCapacity", 100_000L, 1L, Long.MAX_VALUE);
        PLANT_CULTIVATOR_TICKS = b.comment("Ticks to cultivate one crop (seed is not consumed)")
                .defineInRange("ticks", 200, 1, Integer.MAX_VALUE);
        PLANT_CULTIVATOR_COST_PER_TICK = b.comment("Akaishi energy consumed per tick while cultivating")
                .defineInRange("costPerTick", 10L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("compressor");
        COMPRESSOR_ENERGY_CAPACITY = b.comment("Akaishi energy buffer capacity")
                .defineInRange("energyCapacity", 100_000L, 1L, Long.MAX_VALUE);
        COMPRESSOR_TICKS = b.comment("Ticks to compress once")
                .defineInRange("ticks", 100, 1, Integer.MAX_VALUE);
        COMPRESSOR_COST_PER_TICK = b.comment("Akaishi energy consumed per tick while compressing")
                .defineInRange("costPerTick", 15L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("pulverizer");
        PULVERIZER_ENERGY_CAPACITY = b.comment("Akaishi energy buffer capacity")
                .defineInRange("energyCapacity", 100_000L, 1L, Long.MAX_VALUE);
        PULVERIZER_TICKS = b.comment("Ticks to pulverize once")
                .defineInRange("ticks", 100, 1, Integer.MAX_VALUE);
        PULVERIZER_COST_PER_TICK = b.comment("Akaishi energy consumed per tick while pulverizing")
                .defineInRange("costPerTick", 15L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("transformer");
        TRANSFORMER_ENERGY_CAPACITY = b.comment("Akaishi energy buffer capacity")
                .defineInRange("energyCapacity", 100_000L, 1L, Long.MAX_VALUE);
        TRANSFORMER_TICKS = b.comment("Ticks to transform once")
                .defineInRange("ticks", 100, 1, Integer.MAX_VALUE);
        TRANSFORMER_COST_PER_TICK = b.comment("Akaishi energy consumed per tick while transforming")
                .defineInRange("costPerTick", 15L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("miner");
        MINER_TICKS_BASE = b.comment("Base ticks for one mining cycle (tier multiplier speeds it up)")
                .defineInRange("ticksBase", 200, 1, Integer.MAX_VALUE);
        MINER_COST_PER_TICK_BASE = b.comment("Base akaishi energy consumed per tick while mining")
                .defineInRange("costPerTickBase", 2000L, 1L, Long.MAX_VALUE);
        MINER_PRECISE_FORTUNE_DIVISOR = b.comment("Precise mode fortune divisor: fortune upgrades take effect at 1/N "
                        + "(effective fortune = fortuneCount / N, rounded down). 3 = fortune works at 1/3 in precise mode")
                .defineInRange("preciseFortuneDivisor", 3, 1, 8);
        MINER_EXTRA_ORE_WEIGHT = b.comment("Loot weight for extra minerals registered via the #akaishi:miner/minerals "
                        + "item tag (0 = disable tag extension, mine only the default ten ores)")
                .defineInRange("extraOreWeight", 1, 0, 1000);
        b.pop();

        b.push("decay_zone");
        DECAY_ZONE_DURATION_TICKS = b.comment("Decay zone duration (ticks, default 30 hours)")
                .defineInRange("durationTicks", 30L * 60 * 60 * 20, 1L, Long.MAX_VALUE);
        DECAY_ZONE_SAMPLES_PER_TICK = b.comment("Block conversion samples per zone per tick (256 = old speed; 8192 = 32x, default). "
                        + "Capped at 65536 (256x old speed) to keep chunk sampling cost sane.")
                .defineInRange("samplesPerTick", 8192, 256, 65536);
        b.pop();

        b.push("decay_purifier");
        DECAY_PURIFIER_ENERGY_CAPACITY = b.comment("Akaishi energy buffer capacity")
                .defineInRange("energyCapacity", 1_000_000L, 1L, Long.MAX_VALUE);
        DECAY_PURIFIER_RANGE = b.comment("Purification range in blocks: a zone is always reached when the tower "
                        + "stands inside it (horizontal distance <= zone radius); otherwise the tower reaches "
                        + "zones whose center is within this euclidean distance")
                .defineInRange("range", 80, 1, Integer.MAX_VALUE);
        DECAY_PURIFIER_COST_PER_TICK = b.comment("Akaishi energy consumed per tick while purifying")
                .defineInRange("costPerTick", 2_000L, 1L, Long.MAX_VALUE);
        DECAY_PURIFIER_TICKS_PER_TICK = b.comment("Decay zone remaining ticks reduced per tick (10 = 10x faster)")
                .defineInRange("ticksPerTick", 10L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("fusion_reactor");
        FUSION_EFFICIENCY_GROWTH = b.comment("Yield/heat multiplier per efficiency frame (1.15 -> x5.35 at 12 frames)")
                .defineInRange("efficiencyGrowth", 1.15, 1.0, 10.0);
        FUSION_COOLER_FRAME_BONUS = b.comment("Total cooling multiplier per cooler frame (0.1 -> x2 at 10 frames)")
                .defineInRange("coolerFrameBonus", 0.1, 0.0, 10.0);
        FUSION_COOLING_PER_PERCENT = b.comment("Temperature offset per 1% cooling efficiency (M)")
                .defineInRange("coolingPerPercent", 2_000_000L, 1L, Long.MAX_VALUE);
        FUSION_BASE_TEMP = b.comment("Base temperature with no fuel heat (M)")
                .defineInRange("baseTemp", 50_000_000, 0, Integer.MAX_VALUE);
        FUSION_TEMP_MAX = b.comment("Physical temperature cap / yield falloff anchor (M); overheat shutdown happens at tempTrip")
                .defineInRange("tempMax", 160_000_000, 1, Integer.MAX_VALUE);
        FUSION_TEMP_TRIP = b.comment("Overheat shutdown threshold: burning stops once temperature reaches this value (M, below tempMax)")
                .defineInRange("tempTrip", 159_000_000, 1, Integer.MAX_VALUE);
        FUSION_TEMP_OPT_MIN = b.comment("Optimal yield temperature range lower bound (M)")
                .defineInRange("tempOptMin", 100_000_000, 0, Integer.MAX_VALUE);
        FUSION_TEMP_OPT_MAX = b.comment("Optimal yield temperature range upper bound (M)")
                .defineInRange("tempOptMax", 130_000_000, 0, Integer.MAX_VALUE);
        FUSION_TEMP_RESUME = b.comment("Shutdown recovers when temperature drops to half of max (M)")
                .defineInRange("tempResume", 80_000_000, 0, Integer.MAX_VALUE);
        FUSION_TEMP_STEP = b.comment("Max temperature change per tick (M, smooths transitions)")
                .defineInRange("tempStep", 2_000_000, 1, Integer.MAX_VALUE);
        FUSION_COOLER_DURABILITY_INTERVAL = b.comment("Heat sink durability ticks per 1 point (100 = 5s)")
                .defineInRange("coolerDurabilityInterval", 100, 1, Integer.MAX_VALUE);
        FUSION_ASH_PER_ENERGY = b.comment("Energy consumed per life ash produced")
                .defineInRange("ashPerEnergy", 100_000_000_000L, 1L, Long.MAX_VALUE);
        FUSION_ROD_ENERGY = b.comment("Total fusion energy stored in one fuel rod")
                .defineInRange("rodEnergy", 6_000_000_000_000L, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("wireless");
        WIRELESS_BASE_LOSS = b.comment("Base transfer loss ratio per port transfer")
                .defineInRange("baseLoss", 0.05, 0.0, 1.0);
        WIRELESS_LOSS_PER_BLOCK = b.comment("Extra loss ratio per block of distance")
                .defineInRange("lossPerBlock", 0.001, 0.0, 0.1);
        WIRELESS_MAX_LOSS = b.comment("Loss ratio cap")
                .defineInRange("maxLoss", 0.5, 0.0, 0.99);
        WIRELESS_CROSS_DIM_LOSS = b.comment("Fixed loss ratio for cross-dimension transfer (requires dim bridge)")
                .defineInRange("crossDimLoss", 0.25, 0.0, 0.99);
        WIRELESS_LOSS_REDUCTION_PER_MODULE = b.comment("Loss reduction per input/output loss suppressor module (0.05 = -5%, stackable, capped at 90%)")
                .defineInRange("lossReductionPerModule", 0.05, 0.0, 0.9);
        b.pop();

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

        // ==================== 机械改造机器 ====================
        b.push("mechanical_machines");
        MECH_CHISHI_CAPACITY = b.comment("机械三机赤能源缓冲容量（共用）")
                .defineInRange("chishiCapacity", 100_000L, 0L, Long.MAX_VALUE);
        MECH_LIFE_CAPACITY = b.comment("机械三机生命能量缓冲容量（共用）")
                .defineInRange("lifeCapacity", 20_000L, 0L, Long.MAX_VALUE);
        MECH_TEMPLATE_CHISHI_COST = b.comment("模板制造厂：塑形一次赤能源消耗")
                .defineInRange("templateChishiCost", 1_000L, 0L, Long.MAX_VALUE);
        MECH_TEMPLATE_LIFE_COST = b.comment("模板制造厂：塑形一次生命能量消耗")
                .defineInRange("templateLifeCost", 1_000L, 0L, Long.MAX_VALUE);
        MECH_TEMPLATE_TICKS = b.comment("模板制造厂：塑形一次耗时 (tick)")
                .defineInRange("templateTicks", 60, 1, Integer.MAX_VALUE);
        MECH_PROCESS_CHISHI_BASE = b.comment("加工厂：器官基价赤能（下标=器官序数 0~8：眼/心/肺/内脏/肾/左臂/右臂/左腿/右腿）；0 = 用内置默认")
                .defineList("processChishiBase", List.of(2_000L, 4_000L, 2_000L, 4_000L, 2_000L, 3_000L, 3_000L, 3_000L, 3_000L),
                        (Object o) -> o instanceof Number n && n.longValue() >= 0);
        MECH_PROCESS_LIFE_BASE = b.comment("加工厂：器官基价生命能（下标同上）；0 = 用内置默认")
                .defineList("processLifeBase", List.of(2_000L, 4_000L, 2_000L, 4_000L, 2_000L, 2_000L, 2_000L, 2_000L, 2_000L),
                        (Object o) -> o instanceof Number n && n.longValue() >= 0);
        MECH_PROCESS_TICKS_BASE = b.comment("加工厂：器官耗时基价 tick（下标同上）；0 = 用内置默认")
                .defineList("processTicksBase", List.of(80, 160, 80, 160, 80, 120, 120, 120, 120),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        MECH_PROCESS_PART_FACTOR = b.comment("加工厂：部件系数百分数（下标=部件序数 0~3：核心/模块/外壳/散热；核心 120=×1.2、模块 100、外壳 90、散热 110）；0 = 用内置默认")
                .defineList("processPartFactor", List.of(120, 100, 90, 110),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        MECH_PROCESS_MATERIAL_COUNT = b.comment("加工厂：材料消耗份数（下标=部件序数 0~3：核心 3/模块 2/外壳 4/散热 2，与材料等级无关）；0 = 用内置默认")
                .defineList("processMaterialCount", List.of(3, 2, 4, 2),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        MECH_ASSEMBLY_CHISHI_COST = b.comment("组装台：组装一次赤能源消耗（固定，与器官无关）")
                .defineInRange("assemblyChishiCost", 2_000L, 0L, Long.MAX_VALUE);
        MECH_ASSEMBLY_LIFE_COST = b.comment("组装台：组装一次生命能量消耗（固定）")
                .defineInRange("assemblyLifeCost", 2_000L, 0L, Long.MAX_VALUE);
        MECH_ASSEMBLY_TICKS = b.comment("组装台：组装一次耗时 (tick)")
                .defineInRange("assemblyTicks", 80, 1, Integer.MAX_VALUE);
        b.pop();

        // ==================== 机械义体属性换算 ====================
        b.push("mechanical_body");
        MECH_BODY_HEALTH_SCALE = b.comment("机械义体：生命值权重 → 生命上限换算倍率（实际加成 = 聚合权重 × 整合度折扣 × 总体倍率 × 该倍率）；0 = 用内置 0.03")
                .defineInRange("healthScale", 0.03, 0.0, 1000.0);
        MECH_BODY_ATTACK_SCALE = b.comment("机械义体：攻击伤害权重 → 攻击伤害换算倍率；0 = 用内置 0.03")
                .defineInRange("attackScale", 0.03, 0.0, 1000.0);
        MECH_BODY_ATTACK_SPEED_SCALE = b.comment("机械义体：攻击速度权重 → 攻击速度换算倍率；0 = 用内置 0.01")
                .defineInRange("attackSpeedScale", 0.01, 0.0, 1000.0);
        MECH_BODY_MOVEMENT_SPEED_SCALE = b.comment("机械义体：移动速度权重 → 移动速度换算倍率；0 = 用内置 0.01")
                .defineInRange("movementSpeedScale", 0.01, 0.0, 1000.0);
        MECH_BODY_ARMOR_SCALE = b.comment("机械义体：护甲权重 → 护甲值换算倍率；0 = 用内置 0.02")
                .defineInRange("armorScale", 0.02, 0.0, 1000.0);
        MECH_BODY_CRIT_CHANCE_SCALE = b.comment("机械义体：暴击率权重 → 暴击率换算倍率（最终受暴击率上限约束）；0 = 用内置 0.1")
                .defineInRange("critChanceScale", 0.1, 0.0, 1000.0);
        MECH_BODY_CRIT_DAMAGE_SCALE = b.comment("机械义体：暴击伤害权重 → 暴击伤害换算倍率（追加倍率口径，最终受暴击伤害上限约束）；0 = 用内置 0.2")
                .defineInRange("critDamageScale", 0.2, 0.0, 1000.0);
        MECH_BODY_RANGE_SCALE = b.comment("机械义体：攻击范围权重 → 攻击距离换算倍率（单位：格）；0 = 用内置 0.02")
                .defineInRange("rangeScale", 0.02, 0.0, 1000.0);
        MECH_BODY_DODGE_SCALE = b.comment("机械义体：闪避权重 → 闪避率换算倍率（最终受闪避上限约束）；0 = 用内置 0.1")
                .defineInRange("dodgeScale", 0.1, 0.0, 1000.0);
        b.pop();

        // ==================== 基因属性权重 ====================
        b.push("gene_weight");
        GENE_WEIGHT_STRENGTH = b.comment("基因属性权重：生物专精轴最大加成比例（最强轴 ×(1+k)，其余按强度占比递减）；0 = 用内置 0.25")
                .defineInRange("strength", 0.25, 0.0, 2.0);
        b.pop();

        // ==================== 底层战斗（暴击/闪避） ====================
        b.push("combat");
        COMBAT_CRIT_ENABLED = b.comment("底层战斗：暴击总开关；false 时暴击率/暴击伤害不参与结算")
                .define("critEnabled", true);
        COMBAT_DODGE_ENABLED = b.comment("底层战斗：闪避总开关；false 时闪避不参与结算")
                .define("dodgeEnabled", true);
        COMBAT_CRIT_CHANCE_CAP = b.comment("底层战斗：暴击率上限（0~1）；0 = 用内置 1.0")
                .defineInRange("critChanceCap", 1.0, 0.0, 1.0);
        COMBAT_CRIT_DAMAGE_CAP = b.comment("底层战斗：暴击伤害上限（追加倍率，0.5 = 最终 ×1.5）；0 = 用内置 5.0")
                .defineInRange("critDamageCap", 5.0, 0.0, 100.0);
        COMBAT_DODGE_CHANCE_CAP = b.comment("底层战斗：闪避上限（0~1）；0 = 用内置 0.8")
                .defineInRange("dodgeChanceCap", 0.8, 0.0, 1.0);
        b.pop();

        // ==================== 机器全局倍率 ====================
        b.push("machine");
        MACHINE_WORK_SPEED = b.comment("全部可升级加工机器的全局速度倍率（与机器自身速度升级叠乘；1.0 = 不变）")
                .defineInRange("workSpeed", 1.0, 0.05, 10.0);
        MACHINE_COST_MULTIPLIER = b.comment("持续耗能机器的全局运行能耗倍率（仅放大运行扣费，活化器/重铸仪等固定工艺费不变；1.0 = 不变）")
                .defineInRange("costMultiplier", 1.0, 0.05, 10.0);
        b.pop();

        // ==================== 机制开关 ====================
        b.push("toggles");
        DECAY_ZONE_ENABLED = b.comment("是否生成衰竭区域（管道/桶/反应堆泄漏等）")
                .define("decayZone", true);
        SUNLIGHT_BURN_ENABLED = b.comment("日光自燃负面被动是否生效（骷髅腿 / 幻翼肺）")
                .define("sunlightBurn", true);
        OVERLOAD_ENABLED = b.comment("躯体超载减益（按总排斥结算）是否生效")
                .define("overload", true);
        b.pop();

        // ==================== 赤石饰品扩展槽 ====================
        b.push("curio_slots");
        CURIO_SLOT_UNLOCK_REQUIRED = b.comment("赤石饰品扩展槽是否需要进度解锁（false = 四个扩展槽始终开启）")
                .define("unlockRequired", true);
        CURIO_SLOT_UNLOCK_THRESHOLDS = b.comment("四个扩展槽各自所需的赤石进度节点数 [槽1, 槽2, 槽3, 槽4]；0 = 该槽无条件开启（默认全 0，四个槽登录即全开）")
                .defineList("unlockThresholds", List.of(0, 0, 0, 0),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        b.pop();

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

        // ==================== 生命研究机器 ====================
        b.push("life_machines");
        GENE_ANALYZER_LIFE_COST = b.comment("基因分析仪：解构一次消耗的生命能量")
                .defineInRange("geneAnalyzerLifeCost", 5_000L, 0L, Long.MAX_VALUE);
        GENE_ANALYZER_LIFE_CAPACITY = b.comment("基因分析仪：生命能量缓冲容量")
                .defineInRange("geneAnalyzerLifeCapacity", 10_000L, 0L, Long.MAX_VALUE);
        GENE_ANALYZER_PROCESS_TICKS = b.comment("基因分析仪：解构耗时 (tick)")
                .defineInRange("geneAnalyzerProcessTicks", 100, 1, Integer.MAX_VALUE);
        GENE_ANALYZER_MIN_SUCCESS = b.comment("基因分析仪：最低成功率（纯度 25）")
                .defineInRange("geneAnalyzerMinSuccessRate", 0.70, 0.0, 1.0);
        GENE_ANALYZER_MAX_SUCCESS = b.comment("基因分析仪：最高成功率（纯度 100）")
                .defineInRange("geneAnalyzerMaxSuccessRate", 0.95, 0.0, 1.0);
        LIFE_STRUCT_LIFE_COST = b.comment("生命结构台：构造一次消耗的生命能量")
                .defineInRange("lifeStructLifeCost", 80_000L, 0L, Long.MAX_VALUE);
        LIFE_STRUCT_SOLID_COST = b.comment("生命结构台：构造一次消耗的固态生命精华")
                .defineInRange("lifeStructSolidCost", 5, 0, 64);
        LIFE_STRUCT_LIFE_CAPACITY = b.comment("生命结构台：生命能量缓冲容量")
                .defineInRange("lifeStructLifeCapacity", 160_000L, 0L, Long.MAX_VALUE);
        LIFE_STRUCT_PROCESS_TICKS = b.comment("生命结构台：构造耗时 (tick)")
                .defineInRange("lifeStructProcessTicks", 120, 1, Integer.MAX_VALUE);
        LIFE_BREEDER_LIFE_COST = b.comment("生命培育器：培育一次消耗的生命能量")
                .defineInRange("lifeBreederLifeCost", 60_000L, 0L, Long.MAX_VALUE);
        LIFE_BREEDER_CRYSTAL_COST = b.comment("生命培育器：培育一次消耗的赤水晶")
                .defineInRange("lifeBreederCrystalCost", 2, 0, 64);
        LIFE_BREEDER_LIFE_CAPACITY = b.comment("生命培育器：生命能量缓冲容量")
                .defineInRange("lifeBreederLifeCapacity", 120_000L, 0L, Long.MAX_VALUE);
        LIFE_BREEDER_PROCESS_TICKS = b.comment("生命培育器：培育耗时 (tick)")
                .defineInRange("lifeBreederProcessTicks", 1000, 1, Integer.MAX_VALUE);
        LIFE_BREEDER_MIN_SUCCESS = b.comment("生命培育器：最低成功率")
                .defineInRange("lifeBreederMinSuccessRate", 0.35, 0.0, 1.0);
        LIFE_BREEDER_MAX_SUCCESS = b.comment("生命培育器：最高成功率")
                .defineInRange("lifeBreederMaxSuccessRate", 0.70, 0.0, 1.0);
        TRAIT_REFORGER_LIFE_COST = b.comment("词条重铸仪：重铸一次消耗的生命能量")
                .defineInRange("traitReforgerLifeCost", 120_000L, 0L, Long.MAX_VALUE);
        TRAIT_REFORGER_LIFE_CAPACITY = b.comment("词条重铸仪：生命能量缓冲容量")
                .defineInRange("traitReforgerLifeCapacity", 240_000L, 0L, Long.MAX_VALUE);
        TRAIT_REFORGER_PROCESS_TICKS = b.comment("词条重铸仪：重铸耗时 (tick)")
                .defineInRange("traitReforgerProcessTicks", 600, 1, Integer.MAX_VALUE);
        TRAIT_REFORGER_CRYSTAL_PER_RARITY = b.comment("词条重铸仪：每级稀有度消耗的赤水晶")
                .defineInRange("traitReforgerCrystalPerRarity", 2, 0, 64);
        TRANSGENE_FACTORY_LIFE_COST = b.comment("转基因工厂：加工一次消耗的生命能量")
                .defineInRange("transgeneFactoryLifeCost", 5_000L, 0L, Long.MAX_VALUE);
        TRANSGENE_FACTORY_LIFE_CAPACITY = b.comment("转基因工厂：生命能量缓冲容量")
                .defineInRange("transgeneFactoryLifeCapacity", 10_000L, 0L, Long.MAX_VALUE);
        TRANSGENE_FACTORY_PROCESS_TICKS = b.comment("转基因工厂：加工耗时 (tick)")
                .defineInRange("transgeneFactoryProcessTicks", 100, 1, Integer.MAX_VALUE);
        SURGERY_IMPLANT_SOLID_COST = b.comment("手术仓：移植消耗的固态生命精华")
                .defineInRange("surgeryImplantSolidCost", 3, 0, 64);
        SURGERY_IMPLANT_LIFE_COST = b.comment("手术仓：移植消耗的生命能量")
                .defineInRange("surgeryImplantLifeCost", 20_000L, 0L, Long.MAX_VALUE);
        SURGERY_EXTRACT_SOLID_COST = b.comment("手术仓：摘除消耗的固态生命精华")
                .defineInRange("surgeryExtractSolidCost", 1, 0, 64);
        SURGERY_EXTRACT_LIFE_COST = b.comment("手术仓：摘除消耗的生命能量")
                .defineInRange("surgeryExtractLifeCost", 5_000L, 0L, Long.MAX_VALUE);
        SURGERY_LIFE_CAPACITY = b.comment("手术仓：生命能量缓冲容量")
                .defineInRange("surgeryLifeCapacity", 100_000L, 0L, Long.MAX_VALUE);
        SURGERY_PROCESS_TICKS = b.comment("手术仓：单次手术耗时 (tick)")
                .defineInRange("surgeryProcessTicks", 80, 1, Integer.MAX_VALUE);
        ORGAN_VAULT_LIFE_CAPACITY = b.comment("器官储藏库：生命能量缓冲容量")
                .defineInRange("organVaultLifeCapacity", 100_000L, 0L, Long.MAX_VALUE);
        ORGAN_VAULT_KEEP_COST = b.comment("器官储藏库：每 tick 保育消耗（有器官时）")
                .defineInRange("organVaultKeepCostPerTick", 1L, 0L, Long.MAX_VALUE);
        POTION_TABLE_LIFE_CAPACITY = b.comment("药剂台：生命能量缓冲容量")
                .defineInRange("potionTableLifeCapacity", 100_000L, 0L, Long.MAX_VALUE);
        b.pop();

        // ==================== 能量机器 ====================
        b.push("energy_machines");
        ENERGY_PROCESSOR_CHISHI_RATE = b.comment("能量加工机：每 tick 抽取赤能源上限")
                .defineInRange("energyProcessorChishiRate", 1_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_PROCESSOR_CHISHI_CAPACITY = b.comment("能量加工机：赤能源池容量")
                .defineInRange("energyProcessorChishiCapacity", 20_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_PROCESSOR_TANK_CAPACITY = b.comment("能量加工机：各液体罐容量 (mb)")
                .defineInRange("energyProcessorTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        ENERGY_PROCESSOR_CHISHI_COST = b.comment("能量加工机：每次加工消耗的赤能源")
                .defineInRange("energyProcessorChishiCost", 5_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_LIQUEFIER_CHISHI_RATE = b.comment("能量液化器：每 tick 抽取赤能源上限")
                .defineInRange("energyLiquefierChishiRate", 1_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_LIQUEFIER_CHISHI_CAPACITY = b.comment("能量液化器：赤能源池容量")
                .defineInRange("energyLiquefierChishiCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_LIQUEFIER_TANK_CAPACITY = b.comment("能量液化器：液体罐容量 (mb)")
                .defineInRange("energyLiquefierTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        FUEL_MIXER_CHISHI_RATE = b.comment("燃料混合器：每 tick 抽取赤能源上限")
                .defineInRange("fuelMixerChishiRate", 1_000_000L, 0L, Long.MAX_VALUE);
        FUEL_MIXER_CHISHI_CAPACITY = b.comment("燃料混合器：赤能源池容量")
                .defineInRange("fuelMixerChishiCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        FUEL_MIXER_CHISHI_COST = b.comment("燃料混合器：每次混合消耗的赤能源")
                .defineInRange("fuelMixerChishiCost", 2_000_000L, 0L, Long.MAX_VALUE);
        FUEL_MIXER_TANK_CAPACITY = b.comment("燃料混合器：液体罐容量 (mb)")
                .defineInRange("fuelMixerTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        FUEL_CANNER_TANK_CAPACITY = b.comment("燃料灌装机：液体罐容量 (mb)")
                .defineInRange("fuelCannerTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        FUEL_CANNER_FILL_RATE = b.comment("燃料灌装机：每 tick 灌装量 (mb)")
                .defineInRange("fuelCannerFillRate", 1_000L, 0L, Long.MAX_VALUE);
        ENERGY_AGGREGATOR_PER_INGOT = b.comment("能量聚合器：每颗赤石粉聚合消耗的赤能源")
                .defineInRange("energyAggregatorEnergyPerIngot", 10_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_AGGREGATOR_PER_GEODE = b.comment("能量聚合器：晶洞升级一次消耗的赤能源")
                .defineInRange("energyAggregatorEnergyPerGeodeUpgrade", 10_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_AGGREGATOR_CAPACITY = b.comment("能量聚合器：赤能源存储容量")
                .defineInRange("energyAggregatorEnergyCapacity", 200_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_GENERATOR_RATE = b.comment("能量发电机：每 tick 发电量")
                .defineInRange("energyGeneratorGenerateRate", 75, 0, Integer.MAX_VALUE);
        ENERGY_ASSEMBLY_RATE = b.comment("能量组装机：每 tick 发电量")
                .defineInRange("energyAssemblyGenerateRate", 3375, 0, Integer.MAX_VALUE);
        SUPER_GENERATOR_CORE_RATE = b.comment("超级发电机核心：每 tick 发电量")
                .defineInRange("superGeneratorCoreGenerateRate", 15_000, 0, Integer.MAX_VALUE);
        ENERGY_CELL_BASE_CAPACITY = b.comment("能量池：基础容量")
                .defineInRange("energyCellSerializerBaseCapacity", 1_000_000_000L, 0L, Long.MAX_VALUE);
        UPGRADE_STATION_PER_UPGRADE = b.comment("升级工作台：每次升级消耗的赤能源")
                .defineInRange("upgradeStationEnergyPerUpgrade", 20_000_000L, 0L, Long.MAX_VALUE);
        UPGRADE_STATION_CAPACITY = b.comment("升级工作台：赤能源存储容量")
                .defineInRange("upgradeStationEnergyCapacity", 40_000_000L, 0L, Long.MAX_VALUE);
        EQUIPMENT_FORGER_PER_FORGE = b.comment("装备锻造台：每次锻造消耗的赤能源")
                .defineInRange("equipmentForgerEnergyPerForge", 50_000_000L, 0L, Long.MAX_VALUE);
        EQUIPMENT_FORGER_CAPACITY = b.comment("装备锻造台：赤能源存储容量")
                .defineInRange("equipmentForgerEnergyCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        b.pop();

        // ==================== 净化与矩阵 ====================
        b.push("purifier_matrix");
        PURIFIER_ENERGY_PER_TICK = b.comment("净化塔：每 tick 消耗的赤能源")
                .defineInRange("purifierEnergyPerTick", 5, 0, Integer.MAX_VALUE);
        PURIFIER_BURN_RATE = b.comment("净化塔：燃料燃烧速率（每点产能 tick 数）")
                .defineInRange("purifierBurnRate", 10, 1, Integer.MAX_VALUE);
        PURIFIER_TOTAL_COST = b.comment("净化塔：单次提纯消耗（生命能量）")
                .defineInRange("purifierTotalCost", 500L, 0L, Long.MAX_VALUE);
        PURIFIER_RATE_FORMED = b.comment("净化塔：成型后每 tick 提纯量")
                .defineInRange("purifierRateFormed", 150L, 0L, Long.MAX_VALUE);
        PURIFIER_MATRIX_TOTAL_COST = b.comment("净化矩阵：单次提纯消耗（生命能量）")
                .defineInRange("purifierMatrixTotalCost", 500L, 0L, Long.MAX_VALUE);
        PURIFIER_MATRIX_RATE_FORMED = b.comment("净化矩阵：成型后每 tick 提纯量")
                .defineInRange("purifierMatrixRateFormed", 150L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_CHISHI_RATE = b.comment("生命净化机：每 tick 抽取赤能源上限")
                .defineInRange("lifePurifierChishiRate", 1_000_000L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_TOTAL_COST = b.comment("生命净化机：单次固化消耗的赤能源")
                .defineInRange("lifePurifierTotalCost", 10_000_000L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_LIFE_COST = b.comment("生命净化机：单次固化消耗的生命能量")
                .defineInRange("lifePurifierLifeCost", 1_000L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_CHISHI_CAPACITY = b.comment("生命净化机：赤能源池容量")
                .defineInRange("lifePurifierChishiCapacity", 20_000_000L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_LIFE_CAPACITY = b.comment("生命净化机：生命能量缓冲容量")
                .defineInRange("lifePurifierLifeCapacity", 5_000L, 0L, Long.MAX_VALUE);
        LIFE_MATRIX_CONVERSIONS_PER_TICK = b.comment("生命矩阵：每 tick 转化次数")
                .defineInRange("lifeMatrixConversionsPerTick", 45, 0, Integer.MAX_VALUE);
        LIFE_MATRIX_CONVERSION_COST = b.comment("生命矩阵：单次转化消耗的赤能源")
                .defineInRange("lifeMatrixConversionCost", 10_000_000L, 0L, Long.MAX_VALUE);
        LIFE_MATRIX_CHISHI_CAPACITY = b.comment("生命矩阵：赤能源池容量")
                .defineInRange("lifeMatrixChishiCapacity", 500_000_000L, 0L, Long.MAX_VALUE);
        LIFE_MATRIX_LIFE_CAPACITY = b.comment("生命矩阵：生命能量缓冲容量")
                .defineInRange("lifeMatrixLifeCapacity", 5_000L, 0L, Long.MAX_VALUE);
        LIFE_CONVERSION_PER_TICK = b.comment("生命转化架构【外接】：每 tick 转化次数")
                .defineInRange("lifeConversionConversionsPerTick", 45, 0, Integer.MAX_VALUE);
        LIFE_CONVERSION_CHISHI_CAPACITY = b.comment("生命转化架构【外接】：赤能源池容量")
                .defineInRange("lifeConversionChishiCapacity", 500_000_000L, 0L, Long.MAX_VALUE);
        LIFE_CONVERSION_LIFE_CAPACITY = b.comment("生命转化架构【外接】：生命能量缓冲容量")
                .defineInRange("lifeConversionLifeCapacity", 5_000L, 0L, Long.MAX_VALUE);
        LIFE_AGGREGATION_COST = b.comment("生命聚合转化器：单次转化消耗的赤能源")
                .defineInRange("lifeAggregationConversionCost", 10_000_000L, 0L, Long.MAX_VALUE);
        LIFE_AGGREGATION_OUTPUT = b.comment("生命聚合转化器：单次转化产出的生命能量")
                .defineInRange("lifeAggregationConversionOutput", 10L, 0L, Long.MAX_VALUE);
        LIFE_AGGREGATION_CHISHI_CAPACITY = b.comment("生命聚合转化器：赤能源池容量")
                .defineInRange("lifeAggregationChishiCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        LIFE_AGGREGATION_LIFE_CAPACITY = b.comment("生命聚合转化器：生命能量缓冲容量")
                .defineInRange("lifeAggregationLifeCapacity", 100L, 0L, Long.MAX_VALUE);
        b.pop();

        // ==================== 端口与电池缓冲 ====================
        b.push("buffers");
        LIFE_MATRIX_INPUT_PORT_BUFFER = b.comment("生命矩阵能量输入口缓冲容量")
                .defineInRange("lifeMatrixInputPortBufferCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        LIFE_MATRIX_OUTPUT_PORT_BUFFER = b.comment("生命矩阵能量输出口缓冲容量")
                .defineInRange("lifeMatrixOutputPortBufferCapacity", 5_000L, 0L, Long.MAX_VALUE);
        PURIFIER_INPUT_PORT_BUFFER = b.comment("净化矩阵能量输入口缓冲容量")
                .defineInRange("purifierEnergyInputPortBufferCapacity", 1_000_000L, 0L, Long.MAX_VALUE);
        MINER_PORT_BUFFER = b.comment("矿机能量端口缓冲容量")
                .defineInRange("minerPortBufferCapacity", 10_000_000L, 0L, Long.MAX_VALUE);
        MINER_ENERGY_INPUT_BUFFER = b.comment("矿机能量输入口缓冲容量")
                .defineInRange("minerEnergyInputBufferCapacity", 10_000_000L, 0L, Long.MAX_VALUE);
        WIRELESS_INPUT_PORT_BUFFER = b.comment("无线能量输入口缓冲容量")
                .defineInRange("wirelessInputPortBufferCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        WIRELESS_OUTPUT_PORT_BUFFER = b.comment("无线能量输出口缓冲容量")
                .defineInRange("wirelessOutputPortBufferCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        GEN_ENERGY_OUTPUT_BUFFER = b.comment("生命矩阵结构【外接】能量输出口缓冲容量")
                .defineInRange("genEnergyOutputPortBufferCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        FUSION_ENERGY_OUTPUT_BUFFER = b.comment("聚变能量输出口缓冲容量")
                .defineInRange("fusionEnergyOutputBufferCapacity", 20_000_000_000L, 0L, Long.MAX_VALUE);
        REACTOR_ENERGY_OUTPUT_BUFFER = b.comment("反应堆能量输出口缓冲容量")
                .defineInRange("reactorEnergyOutputBufferCapacity", 5_000_000_000L, 0L, Long.MAX_VALUE);
        LIFE_ENERGY_CELL_SERIALIZER_CAPACITY = b.comment("生命储存串联器：自身基础容量（成型后总容量 = 该值 + 26 台外壳储存器容量之和；容量分级见各档储存器）")
                .defineInRange("lifeEnergyCellSerializerBaseCapacity", 2_000_000L, 0L, Long.MAX_VALUE);
        PLASMA_TANK_CAPACITY = b.comment("等离子储罐：容量 (mb)")
                .defineInRange("plasmaTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        ITEM_TERMINAL_ENERGY_BUFFER = b.comment("物品终端：赤能源缓冲容量（一次性存取费用的费用池）",
                        "须 ≥ 允许的最大单笔费用；单笔 IP 上限 = floor(该值 × 4 / 费率系数)，费率系数为存入 1.005 / 取出 1.0025",
                        "默认 1000000 赤能源 ⇒ 单笔最多约 398 万 IP（约 5.6 箱钻石），可一次存一箱")
                .defineInRange("itemTerminalEnergyBuffer", 1_000_000L, 1L, Long.MAX_VALUE);
        ITEM_TERMINAL_ENERGY_PORT_BUFFER = b.comment("物品终端赤能源接入口：单口自身缓冲容量",
                        "接入口由赤能源管道注入并暂存，终端在存取结算时主动汇聚抽取（多口并联，各口独立蓄能）",
                        "该值只影响单口可暂存量，不影响单笔费用上限（后者由 itemTerminalEnergyBuffer 决定）")
                .defineInRange("itemTerminalEnergyPortBufferCapacity", 1_000_000L, 1L, Long.MAX_VALUE);
        b.pop();

        // ==================== 培养机提纯与分馏机 ====================
        // 提纯表下标 = 纯度区间序数 [0, 25, 50, 75]；0 或缺失条目回退到内置默认
        b.push("cultivator_fractionator");
        CULTIVATOR_LIFE_CAPACITY = b.comment("培养机：生命能量缓冲容量")
                .defineInRange("cultivatorLifeCapacity", 500_000L, 0L, Long.MAX_VALUE);
        CULTIVATOR_PURIFY_SUCCESS = b.comment("提纯成功率（%）按纯度区间 [0, 25, 50, 75]；0 = 用内置默认")
                .defineList("cultivatorPurifySuccess", List.of(90, 80, 70, 60),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_PURIFY_ENERGY = b.comment("提纯生命能量消耗按纯度区间 [0, 25, 50, 75]；0 = 用内置默认")
                .defineList("cultivatorPurifyEnergy", List.of(10_000L, 20_000L, 40_000L, 80_000L),
                        (Object o) -> o instanceof Number n && n.longValue() >= 0);
        CULTIVATOR_PURIFY_SOLID = b.comment("提纯固态生命精华消耗按纯度区间 [0, 25, 50, 75]；0 = 用内置默认")
                .defineList("cultivatorPurifySolid", List.of(1, 2, 4, 8),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_PURIFY_TICKS = b.comment("提纯耗时 (tick) 按纯度区间 [0, 25, 50, 75]；0 = 用内置默认")
                .defineList("cultivatorPurifyTicks", List.of(300, 600, 1200, 2400),
                        (Object o) -> o instanceof Number n && n.intValue() >= 0);
        CULTIVATOR_PURIFY_GAIN = b.comment("培养机：单次提纯增加的纯度")
                .defineInRange("cultivatorPurifyGain", 10, 0, 100);
        FRACTIONATOR_ENERGY_CAPACITY = b.comment("活化分馏机：赤能源存储容量")
                .defineInRange("fractionatorEnergyCapacity", 100_000L, 0L, Long.MAX_VALUE);
        FRACTIONATOR_COST_PER_CRAFT = b.comment("活化分馏机：每次分馏消耗的赤能源")
                .defineInRange("fractionatorCostPerCraft", 2_000L, 0L, Long.MAX_VALUE);
        FRACTIONATOR_PROCESS_TICKS = b.comment("活化分馏机：每次分馏耗时 (tick)")
                .defineInRange("fractionatorProcessTicks", 100, 1, Integer.MAX_VALUE);
        b.pop();

        // ==================== 价值分（统一存储库定价内核） ====================
        // 说明：所有"分"都是相对价值单位，仅用于统一存储库的排序/统计/筛选，不做经济兑换。
        b.push("value");
        VALUE_COST_MULTIPLIER = b.comment("最终造价倍率；0 = 用内置默认 10")
                .defineInRange("costMultiplier", 10.0, 0.0, 10_000.0);
        VALUE_INGREDIENT_WEIGHT = b.comment("原料价值项权重；0 = 用内置默认 0.75")
                .defineInRange("ingredientWeight", 0.75, 0.0, 100.0);
        VALUE_MAGIC_BONUS = b.comment("魔法/功能类物品加成（卷轴/符文/法术书等）；0 = 用内置默认 40")
                .defineInRange("magicBonus", 40.0, 0.0, 10_000.0);
        VALUE_INGREDIENT_CAP = b.comment("单项原料价值封顶（防止一组配方把价炸飞）；0 = 用内置默认 80")
                .defineInRange("ingredientCap", 80.0, 0.0, 100_000.0);
        VALUE_ITERATIONS = b.comment("不动点迭代遍数（合成链传播层数）；0 = 用内置默认 4")
                .defineInRange("iterations", 4, 0, 32);
        VALUE_LOOT_ENABLED = b.comment("掉落来源估值开关（离线扫描战利品表）")
                .define("lootEnabled", true);
        VALUE_LOOT_AUTO_APPLY = b.comment("掉落来源项是否自动生效；false 时仅在索引中给出建议值，不加价")
                .define("lootAutoApply", false);
        VALUE_LOOT_CAP = b.comment("掉落来源项封顶；0 = 用内置默认 60")
                .defineInRange("lootCap", 60.0, 0.0, 100_000.0);
        VALUE_FLUID_ENABLED = b.comment("流体估值开关（桶代理 + 流标签表）")
                .define("fluidEnabled", true);
        VALUE_FLUID_PER_MB_CAP = b.comment("流体每 mB 价值上限；0 = 用内置默认 0.5（即单桶 500 分）")
                .defineInRange("fluidPerMbCap", 0.5, 0.0, 10_000.0);
        VALUE_OVERRIDES = b.comment("手动指定价值表，三级优先级：精确 id → 通配符 → #tag；格式 id=分值")
                .defineList("overrides", List.of(),
                        (Object o) -> o instanceof String s && s.contains("="));
        VALUE_FLUID_VALUES = b.comment("流体价值表，格式 流体id=每桶分值（支持 * 通配）；"
                + "优先于内置无桶推导与桶装折算，用于给不注册桶的流体单独定价")
                .defineList("fluidValues", List.of(),
                        (Object o) -> o instanceof String s && s.contains("="));
        VALUE_TAG_VALUES = b.comment("标签价值表，格式 #tag=分值（叠加计算，有总分上限）")
                .defineList("tagValues", List.of(),
                        (Object o) -> o instanceof String s && s.startsWith("#") && s.contains("="));
        VALUE_TIER_BONUS = b.comment("品质词加成表，格式 词=分值（对物品 id 词边界匹配，命中多个取最高）")
                .defineList("tierBonus", List.of(),
                        (Object o) -> o instanceof String s && s.contains("="));
        VALUE_KEYWORD_EXCLUSIONS = b.comment("关键词豁免表：id / 通配符（*）；命中则跳过品质词与魔法关键词加成，"
                + "空列表 = 用内置默认（豁免本 mod 的等级后缀 ultimate 与 Curios 槽位名 charm）")
                .defineList("keywordExclusions", List.of(),
                        (Object o) -> o instanceof String s && !s.isBlank());
        VALUE_LOOT_BLACKLIST = b.comment("掉落来源项黑名单：命名空间 / id / 通配符（*）")
                .defineList("lootBlacklist", List.of(),
                        (Object o) -> o instanceof String s && !s.isBlank());
        UNIFIED_VAULT_ROWS = b.comment("统一存储库每页行数（1 行 = 9 格）；0 = 用内置默认 6")
                .defineInRange("unifiedVaultRows", 6, 0, 26);
        b.pop();

        SPEC = b.build();
    }

    private AkaishiConfig() {
    }
}
