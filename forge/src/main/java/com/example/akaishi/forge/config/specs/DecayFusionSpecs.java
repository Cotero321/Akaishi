package com.example.akaishi.forge.config.specs;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 配置域：衰竭区域、衰变净化塔、聚变堆、无线赤能源。
 * 仅由 {@link com.example.akaishi.forge.config.AkaishiConfig} 的 static 初始化按固定顺序调用；
 * 键名/注释/默认值/范围与拆分前逐字一致，调用顺序即 common.toml 分节顺序。
 */
public final class DecayFusionSpecs {

    // ==================== 衰竭区域 ====================
    public static ForgeConfigSpec.LongValue DECAY_ZONE_DURATION_TICKS;
    public static ForgeConfigSpec.IntValue DECAY_ZONE_SAMPLES_PER_TICK;

    // ==================== 衰变净化塔 ====================
    public static ForgeConfigSpec.LongValue DECAY_PURIFIER_ENERGY_CAPACITY;
    public static ForgeConfigSpec.IntValue DECAY_PURIFIER_RANGE;
    public static ForgeConfigSpec.LongValue DECAY_PURIFIER_COST_PER_TICK;
    public static ForgeConfigSpec.LongValue DECAY_PURIFIER_TICKS_PER_TICK;

    // ==================== 聚变堆 ====================
    public static ForgeConfigSpec.DoubleValue FUSION_EFFICIENCY_GROWTH;
    public static ForgeConfigSpec.DoubleValue FUSION_COOLER_FRAME_BONUS;
    public static ForgeConfigSpec.LongValue FUSION_COOLING_PER_PERCENT;
    public static ForgeConfigSpec.IntValue FUSION_BASE_TEMP;
    public static ForgeConfigSpec.IntValue FUSION_TEMP_MAX;
    public static ForgeConfigSpec.IntValue FUSION_TEMP_TRIP;
    public static ForgeConfigSpec.IntValue FUSION_TEMP_OPT_MIN;
    public static ForgeConfigSpec.IntValue FUSION_TEMP_OPT_MAX;
    public static ForgeConfigSpec.IntValue FUSION_TEMP_RESUME;
    public static ForgeConfigSpec.IntValue FUSION_TEMP_STEP;
    public static ForgeConfigSpec.IntValue FUSION_COOLER_DURABILITY_INTERVAL;
    public static ForgeConfigSpec.LongValue FUSION_ASH_PER_ENERGY;
    public static ForgeConfigSpec.LongValue FUSION_ROD_ENERGY;

    // ==================== 无线赤能源 ====================
    public static ForgeConfigSpec.DoubleValue WIRELESS_BASE_LOSS;
    public static ForgeConfigSpec.DoubleValue WIRELESS_LOSS_PER_BLOCK;
    public static ForgeConfigSpec.DoubleValue WIRELESS_MAX_LOSS;
    public static ForgeConfigSpec.DoubleValue WIRELESS_CROSS_DIM_LOSS;
    public static ForgeConfigSpec.DoubleValue WIRELESS_LOSS_REDUCTION_PER_MODULE;
    /** 场域屏障可见性：false = 所有人可见（默认），true = 仅归属者与同队可见 */
    public static ForgeConfigSpec.BooleanValue WIRELESS_FIELD_OWNER_ONLY;

    private DecayFusionSpecs() {
    }

    /** 按拆分前的定义顺序在共享 Builder 上构建本域各节（仅由 AkaishiConfig 调用） */
    public static void build(ForgeConfigSpec.Builder b) {
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
        WIRELESS_FIELD_OWNER_ONLY = b.comment("场域屏障可见性：false = 所有人可见（默认，起环境提示作用），"
                        + "true = 仅终端归属者与同队玩家可见")
                .define("fieldBarrierOwnerOnly", false);
        b.pop();
    }
}
