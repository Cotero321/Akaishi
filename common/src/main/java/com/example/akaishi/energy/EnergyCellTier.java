package com.example.akaishi.energy;

/**
 * 赤能源储存单元等级：等级越高容量与单 tick 传输速率越大。
 * 高级、超级单元可通过合成由低一级单元升级而来。
 * 容量从基础起每级放大：基础 5 亿 → 高级 20 亿 → 超级 200 亿（×10000 后，long 存储）。
 */
public enum EnergyCellTier {
    /** 基础：容量 5 亿（原 5 万 ×10000），单 tick 传输 100 万 */
    BASIC(500_000_000L, 1_000_000),
    /** 高级：容量 20 亿（原 20 万 ×10000），单 tick 传输 500 万 */
    ADVANCED(2_000_000_000L, 5_000_000),
    /** 超级：容量 200 亿，单 tick 传输 5000 万 */
    SUPER(20_000_000_000L, 50_000_000);

    /** 能量容量上限 */
    public final long capacity;
    /** 单 tick 对外传输上限 */
    public final int transferRate;

    EnergyCellTier(long capacity, int transferRate) {
        this.capacity = capacity;
        this.transferRate = transferRate;
    }
}
