package com.example.template.api.energy;

/**
 * 机器能量存储接口：承载某种能量类型的当前值与容量。
 * 与 {@link IEnergyType}（类型描述）解耦，一个方块/设备可持有任意能量类型的存储实例。
 */
public interface IEnergyStorage {

    /** 本存储持有的能量类型 */
    IEnergyType getType();

    /** 当前已存储能量 */
    long getEnergyStored();

    /** 最大可存储能量 */
    long getMaxEnergy();

    /**
     * 存入能量，返回实际存入量。
     *
     * @param amount   尝试存入量
     * @param simulate true 时仅模拟不写入
     */
    long addEnergy(long amount, boolean simulate);

    /**
     * 取出能量，返回实际取出量。
     *
     * @param amount   尝试取出量
     * @param simulate true 时仅模拟不写入
     */
    long extractEnergy(long amount, boolean simulate);

    /**
     * 直接设置当前能量值（钳制到 [0, 容量]）。
     * 默认抛异常：仅用于测试注入/存档恢复等特殊场景，生产存储一般通过 add/extract 变更。
     */
    default void setEnergy(long amount) {
        throw new UnsupportedOperationException("setEnergy not supported by " + getClass().getSimpleName());
    }
}
