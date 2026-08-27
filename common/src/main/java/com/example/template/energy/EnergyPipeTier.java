package com.example.template.energy;

/**
 * 赤能源管道等级：定义各等级管道的每 tick 传输速率。
 * 参考 Mekanism 通用电缆四级制（基础/高级/精英/终极），每级 10 倍速率。
 * 基础管道已足以带动组合发生机等高产设备，更高级管道支撑大规模电网。
 */
public enum EnergyPipeTier {
    /** 基础：满足提纯器、发生机、组合发生机等常规设备 */
    BASIC(50000),
    /** 高级：支撑多台组合发生机的骨干网络 */
    ADVANCED(500000),
    /** 精英：大规模电网的主干管道 */
    ELITE(5000000),
    /** 终极：海量能量瞬间输送 */
    ULTIMATE(50000000);

    public final int transferRate;

    EnergyPipeTier(int transferRate) {
        this.transferRate = transferRate;
    }
}
