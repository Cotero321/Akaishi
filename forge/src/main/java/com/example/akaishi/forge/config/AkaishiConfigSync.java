package com.example.akaishi.forge.config;

import com.example.akaishi.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Forge 配置 → common 值同步。在配置加载/重载时把 ForgeConfigSpec 的当前值
 * 写入 {@link ModConfig} 的 volatile 字段，实现热更新（改配置后无需重启）。
 */
public final class AkaishiConfigSync {

    private AkaishiConfigSync() {
    }

    /** 注册到 MOD 事件总线（ModConfigEvent.Loading / Reloading） */
    public static void onModConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == AkaishiConfig.SPEC) {
            sync();
        }
    }

    private static void sync() {
        // 反应堆
        ModConfig.reactorEnergyPerSlot = AkaishiConfig.REACTOR_ENERGY_PER_SLOT.get();
        ModConfig.reactorBaseTemp = AkaishiConfig.REACTOR_BASE_TEMP.get();
        ModConfig.reactorPassiveCool = AkaishiConfig.REACTOR_PASSIVE_COOL.get();
        ModConfig.reactorCoolerCool = AkaishiConfig.REACTOR_COOLER_COOL.get();
        ModConfig.reactorDrainBase = AkaishiConfig.REACTOR_DRAIN_BASE.get();
        ModConfig.reactorWasteRatio = AkaishiConfig.REACTOR_WASTE_RATIO.get();
        ModConfig.reactorWasteCapacity = AkaishiConfig.REACTOR_WASTE_CAPACITY.get();
        ModConfig.reactorTempMax = AkaishiConfig.REACTOR_TEMP_MAX.get();
        ModConfig.reactorTempOptMin = AkaishiConfig.REACTOR_TEMP_OPT_MIN.get();
        ModConfig.reactorTempOptMax = AkaishiConfig.REACTOR_TEMP_OPT_MAX.get();
        ModConfig.reactorTempWarn = AkaishiConfig.REACTOR_TEMP_WARN.get();
        ModConfig.reactorExplosionDelayTicks = AkaishiConfig.REACTOR_EXPLOSION_DELAY_TICKS.get();
        // 液体管道
        ModConfig.fluidPipeRate = AkaishiConfig.FLUID_PIPE_RATE.get();
        ModConfig.fluidPipeBufferCapacity = AkaishiConfig.FLUID_PIPE_BUFFER_CAPACITY.get();
        // 废品口
        ModConfig.wastePortBufferCapacity = AkaishiConfig.WASTE_PORT_BUFFER_CAPACITY.get();
        // 保存桶
        ModConfig.exhaustedBarrelCapacity = AkaishiConfig.EXHAUSTED_BARREL_CAPACITY.get();
        // 生命活化器
        ModConfig.lifeActivatorLifeCapacity = AkaishiConfig.LIFE_ACTIVATOR_LIFE_CAPACITY.get();
        ModConfig.lifeActivatorCostPerMb = AkaishiConfig.LIFE_ACTIVATOR_COST_PER_MB.get();
        ModConfig.lifeActivatorInputCapacity = AkaishiConfig.LIFE_ACTIVATOR_INPUT_CAPACITY.get();
        ModConfig.lifeActivatorOutputCapacity = AkaishiConfig.LIFE_ACTIVATOR_OUTPUT_CAPACITY.get();
        ModConfig.lifeActivatorConvertRate = AkaishiConfig.LIFE_ACTIVATOR_CONVERT_RATE.get();
        // 生命离心机
        ModConfig.lifeCentrifugeEnergyCapacity = AkaishiConfig.LIFE_CENTRIFUGE_ENERGY_CAPACITY.get();
        ModConfig.lifeCentrifugeInputCapacity = AkaishiConfig.LIFE_CENTRIFUGE_INPUT_CAPACITY.get();
        ModConfig.lifeCentrifugeConvertRate = AkaishiConfig.LIFE_CENTRIFUGE_CONVERT_RATE.get();
        ModConfig.lifeCentrifugeCostPerMb = AkaishiConfig.LIFE_CENTRIFUGE_COST_PER_MB.get();
        // 物品重构仪
        ModConfig.reconstructorEnergyCapacity = AkaishiConfig.RECONSTRUCTOR_ENERGY_CAPACITY.get();
        ModConfig.reconstructorCostPerCrystal = AkaishiConfig.RECONSTRUCTOR_COST_PER_CRYSTAL.get();
        // 聚变燃料聚合器
        ModConfig.aggregatorEnergyCapacity = AkaishiConfig.AGGREGATOR_ENERGY_CAPACITY.get();
        ModConfig.aggregatorCostPerCraft = AkaishiConfig.AGGREGATOR_COST_PER_CRAFT.get();
        ModConfig.aggregatorProcessTicks = AkaishiConfig.AGGREGATOR_PROCESS_TICKS.get();
        ModConfig.aggregatorPlasmaCapacity = AkaishiConfig.AGGREGATOR_PLASMA_CAPACITY.get();
        ModConfig.aggregatorProducePerCraft = AkaishiConfig.AGGREGATOR_PRODUCE_PER_CRAFT.get();
        // 离子体填装器
        ModConfig.fillerPlasmaCapacity = AkaishiConfig.FILLER_PLASMA_CAPACITY.get();
        ModConfig.fillerPlasmaPerRod = AkaishiConfig.FILLER_PLASMA_PER_ROD.get();
        ModConfig.fillerProcessTicks = AkaishiConfig.FILLER_PROCESS_TICKS.get();

        ModConfig.plantCultivatorEnergyCapacity = AkaishiConfig.PLANT_CULTIVATOR_ENERGY_CAPACITY.get();
        ModConfig.plantCultivatorTicks = AkaishiConfig.PLANT_CULTIVATOR_TICKS.get();
        ModConfig.plantCultivatorCostPerTick = AkaishiConfig.PLANT_CULTIVATOR_COST_PER_TICK.get();
        ModConfig.compressorEnergyCapacity = AkaishiConfig.COMPRESSOR_ENERGY_CAPACITY.get();
        ModConfig.compressorTicks = AkaishiConfig.COMPRESSOR_TICKS.get();
        ModConfig.compressorCostPerTick = AkaishiConfig.COMPRESSOR_COST_PER_TICK.get();
        ModConfig.pulverizerEnergyCapacity = AkaishiConfig.PULVERIZER_ENERGY_CAPACITY.get();
        ModConfig.pulverizerTicks = AkaishiConfig.PULVERIZER_TICKS.get();
        ModConfig.pulverizerCostPerTick = AkaishiConfig.PULVERIZER_COST_PER_TICK.get();
        ModConfig.transformerEnergyCapacity = AkaishiConfig.TRANSFORMER_ENERGY_CAPACITY.get();
        ModConfig.transformerTicks = AkaishiConfig.TRANSFORMER_TICKS.get();
        ModConfig.transformerCostPerTick = AkaishiConfig.TRANSFORMER_COST_PER_TICK.get();
        ModConfig.minerTicksBase = AkaishiConfig.MINER_TICKS_BASE.get();
        ModConfig.minerCostPerTickBase = AkaishiConfig.MINER_COST_PER_TICK_BASE.get();

        ModConfig.decayZoneDurationTicks = AkaishiConfig.DECAY_ZONE_DURATION_TICKS.get();
        // 衰变净化塔
        ModConfig.decayPurifierEnergyCapacity = AkaishiConfig.DECAY_PURIFIER_ENERGY_CAPACITY.get();
        ModConfig.decayPurifierRange = AkaishiConfig.DECAY_PURIFIER_RANGE.get();
        ModConfig.decayPurifierCostPerTick = AkaishiConfig.DECAY_PURIFIER_COST_PER_TICK.get();
        ModConfig.decayPurifierTicksPerTick = AkaishiConfig.DECAY_PURIFIER_TICKS_PER_TICK.get();
        // 聚变堆
        ModConfig.fusionEfficiencyGrowth = AkaishiConfig.FUSION_EFFICIENCY_GROWTH.get();
        ModConfig.fusionCoolerFrameBonus = AkaishiConfig.FUSION_COOLER_FRAME_BONUS.get();
        ModConfig.fusionCoolingPerPercent = AkaishiConfig.FUSION_COOLING_PER_PERCENT.get();
        ModConfig.fusionBaseTemp = AkaishiConfig.FUSION_BASE_TEMP.get();
        ModConfig.fusionTempMax = AkaishiConfig.FUSION_TEMP_MAX.get();
        ModConfig.fusionTempOptMin = AkaishiConfig.FUSION_TEMP_OPT_MIN.get();
        ModConfig.fusionTempOptMax = AkaishiConfig.FUSION_TEMP_OPT_MAX.get();
        ModConfig.fusionTempResume = AkaishiConfig.FUSION_TEMP_RESUME.get();
        ModConfig.fusionTempStep = AkaishiConfig.FUSION_TEMP_STEP.get();
        ModConfig.fusionCoolerDurabilityInterval = AkaishiConfig.FUSION_COOLER_DURABILITY_INTERVAL.get();
        ModConfig.fusionAshPerEnergy = AkaishiConfig.FUSION_ASH_PER_ENERGY.get();
        ModConfig.fusionRodEnergy = AkaishiConfig.FUSION_ROD_ENERGY.get();
        // 无线赤能源
        ModConfig.wirelessBaseLoss = AkaishiConfig.WIRELESS_BASE_LOSS.get();
        ModConfig.wirelessLossPerBlock = AkaishiConfig.WIRELESS_LOSS_PER_BLOCK.get();
        ModConfig.wirelessMaxLoss = AkaishiConfig.WIRELESS_MAX_LOSS.get();
        ModConfig.wirelessPortTransferRate = AkaishiConfig.WIRELESS_PORT_TRANSFER_RATE.get();
        ModConfig.wirelessCrossDimLoss = AkaishiConfig.WIRELESS_CROSS_DIM_LOSS.get();
        ModConfig.wirelessLossReductionPerModule = AkaishiConfig.WIRELESS_LOSS_REDUCTION_PER_MODULE.get();
        ModConfig.wirelessChunkTaxPerChunk = AkaishiConfig.WIRELESS_CHUNK_TAX_PER_CHUNK.get();
    }
}
