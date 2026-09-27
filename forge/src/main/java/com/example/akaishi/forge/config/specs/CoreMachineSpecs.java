package com.example.akaishi.forge.config.specs;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 配置域：反应堆、基础物流与赤石早期机器。
 * 仅由 {@link com.example.akaishi.forge.config.AkaishiConfig} 的 static 初始化按固定顺序调用；
 * 键名/注释/默认值/范围与拆分前逐字一致，调用顺序即 common.toml 分节顺序。
 */
public final class CoreMachineSpecs {

    // ==================== 反应堆 ====================
    public static ForgeConfigSpec.LongValue REACTOR_ENERGY_PER_SLOT;
    public static ForgeConfigSpec.IntValue REACTOR_BASE_TEMP;
    public static ForgeConfigSpec.DoubleValue REACTOR_PASSIVE_COOL;
    public static ForgeConfigSpec.DoubleValue REACTOR_COOLER_COOL;
    public static ForgeConfigSpec.DoubleValue REACTOR_DRAIN_BASE;
    public static ForgeConfigSpec.DoubleValue REACTOR_WASTE_RATIO;
    public static ForgeConfigSpec.IntValue REACTOR_WASTE_CAPACITY;
    public static ForgeConfigSpec.IntValue REACTOR_TEMP_MAX;
    public static ForgeConfigSpec.IntValue REACTOR_TEMP_OPT_MIN;
    public static ForgeConfigSpec.IntValue REACTOR_TEMP_OPT_MAX;
    public static ForgeConfigSpec.IntValue REACTOR_TEMP_WARN;
    public static ForgeConfigSpec.IntValue REACTOR_EXPLOSION_DELAY_TICKS;

    // ==================== 液体管道 ====================
    public static ForgeConfigSpec.IntValue FLUID_PIPE_RATE;

    // ==================== 废品口 ====================
    public static ForgeConfigSpec.IntValue WASTE_PORT_BUFFER_CAPACITY;

    // ==================== 衰竭保存桶 ====================
    public static ForgeConfigSpec.LongValue EXHAUSTED_BARREL_CAPACITY;

    // ==================== 生命活化器 ====================
    public static ForgeConfigSpec.LongValue LIFE_ACTIVATOR_LIFE_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_ACTIVATOR_COST_PER_MB;
    public static ForgeConfigSpec.LongValue LIFE_ACTIVATOR_INPUT_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_ACTIVATOR_OUTPUT_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_ACTIVATOR_CONVERT_RATE;
    public static ForgeConfigSpec.LongValue LIFE_CENTRIFUGE_ENERGY_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_CENTRIFUGE_INPUT_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_CENTRIFUGE_CONVERT_RATE;
    public static ForgeConfigSpec.LongValue LIFE_CENTRIFUGE_COST_PER_MB;
    public static ForgeConfigSpec.LongValue RECONSTRUCTOR_ENERGY_CAPACITY;
    public static ForgeConfigSpec.LongValue RECONSTRUCTOR_COST_PER_CRYSTAL;

    // ==================== 聚变燃料聚合器 ====================
    public static ForgeConfigSpec.LongValue AGGREGATOR_ENERGY_CAPACITY;
    public static ForgeConfigSpec.LongValue AGGREGATOR_COST_PER_CRAFT;
    public static ForgeConfigSpec.IntValue AGGREGATOR_PROCESS_TICKS;
    public static ForgeConfigSpec.LongValue AGGREGATOR_PLASMA_CAPACITY;
    public static ForgeConfigSpec.LongValue AGGREGATOR_PRODUCE_PER_CRAFT;

    // ==================== 离子体填装器 ====================
    public static ForgeConfigSpec.LongValue FILLER_PLASMA_CAPACITY;
    public static ForgeConfigSpec.LongValue FILLER_PLASMA_PER_ROD;
    public static ForgeConfigSpec.IntValue FILLER_PROCESS_TICKS;

    // ==================== 赤石植物培养机 ====================
    public static ForgeConfigSpec.LongValue PLANT_CULTIVATOR_ENERGY_CAPACITY;
    public static ForgeConfigSpec.IntValue PLANT_CULTIVATOR_TICKS;
    public static ForgeConfigSpec.LongValue PLANT_CULTIVATOR_COST_PER_TICK;

    // ==================== 赤石压缩机 ====================
    public static ForgeConfigSpec.LongValue COMPRESSOR_ENERGY_CAPACITY;
    public static ForgeConfigSpec.IntValue COMPRESSOR_TICKS;
    public static ForgeConfigSpec.LongValue COMPRESSOR_COST_PER_TICK;

    // ==================== 赤石打粉机 ====================
    public static ForgeConfigSpec.LongValue PULVERIZER_ENERGY_CAPACITY;
    public static ForgeConfigSpec.IntValue PULVERIZER_TICKS;
    public static ForgeConfigSpec.LongValue PULVERIZER_COST_PER_TICK;

    // ==================== 赤石变化器 ====================
    public static ForgeConfigSpec.LongValue TRANSFORMER_ENERGY_CAPACITY;
    public static ForgeConfigSpec.IntValue TRANSFORMER_TICKS;
    public static ForgeConfigSpec.LongValue TRANSFORMER_COST_PER_TICK;

    // ==================== 赤石矿机 ====================
    public static ForgeConfigSpec.IntValue MINER_TICKS_BASE;
    public static ForgeConfigSpec.LongValue MINER_COST_PER_TICK_BASE;
    public static ForgeConfigSpec.IntValue MINER_PRECISE_FORTUNE_DIVISOR;
    public static ForgeConfigSpec.IntValue MINER_EXTRA_ORE_WEIGHT;

    private CoreMachineSpecs() {
    }

    /** 按拆分前的定义顺序在共享 Builder 上构建本域各节（仅由 AkaishiConfig 调用） */
    public static void build(ForgeConfigSpec.Builder b) {
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
    }
}
