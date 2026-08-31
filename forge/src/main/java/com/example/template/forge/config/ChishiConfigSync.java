package com.example.template.forge.config;

import com.example.template.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Forge 配置 → common 值同步。在配置加载/重载时把 ForgeConfigSpec 的当前值
 * 写入 {@link ModConfig} 的 volatile 字段，实现热更新（改配置后无需重启）。
 */
public final class ChishiConfigSync {

    private ChishiConfigSync() {
    }

    /** 注册到 MOD 事件总线（ModConfigEvent.Loading / Reloading） */
    public static void onModConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == ChishiConfig.SPEC) {
            sync();
        }
    }

    private static void sync() {
        // 反应堆
        ModConfig.reactorEnergyPerSlot = ChishiConfig.REACTOR_ENERGY_PER_SLOT.get();
        ModConfig.reactorBaseTemp = ChishiConfig.REACTOR_BASE_TEMP.get();
        ModConfig.reactorPassiveCool = ChishiConfig.REACTOR_PASSIVE_COOL.get();
        ModConfig.reactorCoolerCool = ChishiConfig.REACTOR_COOLER_COOL.get();
        ModConfig.reactorDrainBase = ChishiConfig.REACTOR_DRAIN_BASE.get();
        ModConfig.reactorWasteRatio = ChishiConfig.REACTOR_WASTE_RATIO.get();
        ModConfig.reactorWasteCapacity = ChishiConfig.REACTOR_WASTE_CAPACITY.get();
        ModConfig.reactorTempMax = ChishiConfig.REACTOR_TEMP_MAX.get();
        ModConfig.reactorTempOptMin = ChishiConfig.REACTOR_TEMP_OPT_MIN.get();
        ModConfig.reactorTempOptMax = ChishiConfig.REACTOR_TEMP_OPT_MAX.get();
        ModConfig.reactorTempWarn = ChishiConfig.REACTOR_TEMP_WARN.get();
        ModConfig.reactorExplosionDelayTicks = ChishiConfig.REACTOR_EXPLOSION_DELAY_TICKS.get();
        // 液体管道
        ModConfig.fluidPipeRate = ChishiConfig.FLUID_PIPE_RATE.get();
        ModConfig.fluidPipeBufferCapacity = ChishiConfig.FLUID_PIPE_BUFFER_CAPACITY.get();
        // 废品口
        ModConfig.wastePortBufferCapacity = ChishiConfig.WASTE_PORT_BUFFER_CAPACITY.get();
        // 保存桶
        ModConfig.exhaustedBarrelCapacity = ChishiConfig.EXHAUSTED_BARREL_CAPACITY.get();
        // 生命活化器
        ModConfig.lifeActivatorLifeCapacity = ChishiConfig.LIFE_ACTIVATOR_LIFE_CAPACITY.get();
        ModConfig.lifeActivatorCostPerMb = ChishiConfig.LIFE_ACTIVATOR_COST_PER_MB.get();
        ModConfig.lifeActivatorInputCapacity = ChishiConfig.LIFE_ACTIVATOR_INPUT_CAPACITY.get();
        ModConfig.lifeActivatorOutputCapacity = ChishiConfig.LIFE_ACTIVATOR_OUTPUT_CAPACITY.get();
        ModConfig.lifeActivatorConvertRate = ChishiConfig.LIFE_ACTIVATOR_CONVERT_RATE.get();
        // 衰竭区域
        ModConfig.decayZoneDurationTicks = ChishiConfig.DECAY_ZONE_DURATION_TICKS.get();
        // 无线赤能源
        ModConfig.wirelessBaseLoss = ChishiConfig.WIRELESS_BASE_LOSS.get();
        ModConfig.wirelessLossPerBlock = ChishiConfig.WIRELESS_LOSS_PER_BLOCK.get();
        ModConfig.wirelessMaxLoss = ChishiConfig.WIRELESS_MAX_LOSS.get();
        ModConfig.wirelessPortTransferRate = ChishiConfig.WIRELESS_PORT_TRANSFER_RATE.get();
        ModConfig.wirelessCrossDimLoss = ChishiConfig.WIRELESS_CROSS_DIM_LOSS.get();
        ModConfig.wirelessLossReductionPerModule = ChishiConfig.WIRELESS_LOSS_REDUCTION_PER_MODULE.get();
    }
}
