package com.example.akaishi.upgrade;

/**
 * 可升级机器标记接口：用电器实现后自动获得机器升级组件加成。
 * 接入方需在 tick 中应用 {@link #getSpeedMultiplier()}（progress 推进）与
 * {@link #getEnergyCapacityMultiplier()}（能量存储容量）。
 */
public interface IUpgradeableMachine {

    /** 机器升级槽（速度格/能量格，各一格、单格堆叠 8 封顶） */
    MachineUpgradeSlots getUpgradeSlots();

    /** 加工速度倍率（默认接口实现，直接读升级槽） */
    default float getSpeedMultiplier() {
        return getUpgradeSlots().getSpeedMultiplier();
    }

    /** 能量容量倍率（默认接口实现，直接读升级槽） */
    default float getEnergyCapacityMultiplier() {
        return getUpgradeSlots().getEnergyCapacityMultiplier();
    }
}
