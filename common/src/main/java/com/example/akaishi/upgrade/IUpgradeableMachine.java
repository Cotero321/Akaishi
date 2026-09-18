package com.example.akaishi.upgrade;

import com.example.akaishi.wireless.WirelessFieldManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * 可升级机器标记接口：用电器实现后自动获得机器升级组件加成。
 * 接入方需在 tick 中应用 {@link #getSpeedMultiplier()}（progress 推进）、
 * {@link #getEnergyCostMultiplier()}（耗能放大，封顶 4×）与
 * {@link #getEnergyCapacityMultiplier()}（能量存储容量）。
 */
public interface IUpgradeableMachine {

    /** 机器升级槽（速度格 / 能量格 / 无线接收格，各一格；速度与能量单格堆叠 8 封顶） */
    MachineUpgradeSlots getUpgradeSlots();

    /** 加工速度倍率（默认接口实现，直接读升级槽） */
    default float getSpeedMultiplier() {
        return getUpgradeSlots().getSpeedMultiplier();
    }

    /** 耗能倍率：速度升级抬高耗能，封顶 4×（每 tick 耗电功率 / 单次加工耗能） */
    default float getEnergyCostMultiplier() {
        return getUpgradeSlots().getEnergyCostMultiplier();
    }

    /** 能量容量倍率（默认接口实现，直接读升级槽） */
    default float getEnergyCapacityMultiplier() {
        return getUpgradeSlots().getEnergyCapacityMultiplier();
    }

    /** 是否已装入无线接收升级（无线调度的准入条件之一） */
    default boolean hasWirelessReceiver() {
        return getUpgradeSlots().hasWirelessReceiver();
    }

    /**
     * 是否「已装接收升级 且 身处无线场域」——即能否被微缩矩阵终端调度。
     * <p>
     * 机器侧主动自查（见设计记忆 §15.2 口径 4）：场域半径可达 3 区块（49 区块），
     * 由终端逐 tick 反向推送不可行；调度方与机器方读的是同一份
     * {@link WirelessFieldManager} 判定，不会出现「一边说在场、一边说不在」。
     *
     * @param level 机器所在的服务端世界
     * @param pos   机器坐标（实现方传自己的 {@code getBlockPos()}）
     */
    default boolean isWirelessReady(ServerLevel level, BlockPos pos) {
        return hasWirelessReceiver() && WirelessFieldManager.inField(level, pos);
    }
}
