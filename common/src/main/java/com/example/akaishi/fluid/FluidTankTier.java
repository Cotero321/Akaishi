package com.example.akaishi.fluid;

/**
 * 液体储罐等级：等级越高容量越大。
 * 高级、超级储罐可通过合成由低一级储罐升级而来。
 */
public enum FluidTankTier {
    /** 基础：容量 16000mb */
    BASIC(16_000L),
    /** 高级：容量 64000mb */
    ADVANCED(64_000L),
    /** 超级：容量 256000mb */
    SUPER(256_000L);

    /** 液体容量上限（mb） */
    public final long capacity;

    FluidTankTier(long capacity) {
        this.capacity = capacity;
    }
}
