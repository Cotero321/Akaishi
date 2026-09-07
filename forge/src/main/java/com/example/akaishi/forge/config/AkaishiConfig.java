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
    public static final ForgeConfigSpec.IntValue FLUID_PIPE_BUFFER_CAPACITY;

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

    // ==================== 机制开关 ====================
    public static final ForgeConfigSpec.BooleanValue DECAY_ZONE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SUNLIGHT_BURN_ENABLED;
    public static final ForgeConfigSpec.BooleanValue OVERLOAD_ENABLED;

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
    public static final ForgeConfigSpec.LongValue LIFE_ENERGY_CELL_CAPACITY;
    public static final ForgeConfigSpec.LongValue PLASMA_TANK_CAPACITY;

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
        FLUID_PIPE_BUFFER_CAPACITY = b.comment("Pipe segment buffer capacity (mb)")
                .defineInRange("bufferCapacity", 8000, 1, Integer.MAX_VALUE);
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
        DECAY_PURIFIER_RANGE = b.comment("Purification range in blocks (euclidean distance to zone center)")
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
        LIFE_ENERGY_CELL_CAPACITY = b.comment("生命能量电池：容量")
                .defineInRange("lifeEnergyCellLifeCapacity", 1_000_000L, 0L, Long.MAX_VALUE);
        PLASMA_TANK_CAPACITY = b.comment("等离子储罐：容量 (mb)")
                .defineInRange("plasmaTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
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

        SPEC = b.build();
    }

    private AkaishiConfig() {
    }
}
