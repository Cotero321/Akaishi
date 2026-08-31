package com.example.template.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge 原生配置文件（common.toml）。所有可调数值经 ForgeConfigSpec 定义，
 * 通过 {@link ChishiConfigSync} 在加载/重载时同步到 common 的 {@link com.example.template.config.ModConfig}。
 * Forge 自动生成原生配置界面（Mods 列表 → Config）。
 */
public final class ChishiConfig {

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

    // ==================== 衰竭区域 ====================
    public static final ForgeConfigSpec.LongValue DECAY_ZONE_DURATION_TICKS;

    // ==================== 无线赤能源 ====================
    public static final ForgeConfigSpec.DoubleValue WIRELESS_BASE_LOSS;
    public static final ForgeConfigSpec.DoubleValue WIRELESS_LOSS_PER_BLOCK;
    public static final ForgeConfigSpec.DoubleValue WIRELESS_MAX_LOSS;
    public static final ForgeConfigSpec.LongValue WIRELESS_PORT_TRANSFER_RATE;
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

        b.push("decay_zone");
        DECAY_ZONE_DURATION_TICKS = b.comment("Decay zone duration (ticks, default 30 hours)")
                .defineInRange("durationTicks", 30L * 60 * 60 * 20, 1L, Long.MAX_VALUE);
        b.pop();

        b.push("wireless");
        WIRELESS_BASE_LOSS = b.comment("Base transfer loss ratio per port transfer")
                .defineInRange("baseLoss", 0.05, 0.0, 1.0);
        WIRELESS_LOSS_PER_BLOCK = b.comment("Extra loss ratio per block of distance")
                .defineInRange("lossPerBlock", 0.001, 0.0, 0.1);
        WIRELESS_MAX_LOSS = b.comment("Loss ratio cap")
                .defineInRange("maxLoss", 0.5, 0.0, 0.99);
        WIRELESS_PORT_TRANSFER_RATE = b.comment("Base transfer per port per tick (no upgrades)")
                .defineInRange("portTransferRate", 4096L, 1L, Long.MAX_VALUE);
        WIRELESS_CROSS_DIM_LOSS = b.comment("Fixed loss ratio for cross-dimension transfer (requires dim bridge)")
                .defineInRange("crossDimLoss", 0.25, 0.0, 0.99);
        WIRELESS_LOSS_REDUCTION_PER_MODULE = b.comment("Loss reduction per input/output loss suppressor module (0.05 = -5%, stackable, capped at 90%)")
                .defineInRange("lossReductionPerModule", 0.05, 0.0, 0.9);
        b.pop();

        SPEC = b.build();
    }

    private ChishiConfig() {
    }
}
