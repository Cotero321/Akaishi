package com.example.akaishi.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

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

        SPEC = b.build();
    }

    private AkaishiConfig() {
    }
}
