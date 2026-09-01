package com.example.akaishi.energy;

/**
 * 便捷赤能源储存单元等级：容量越大，单 tick 充能/修复速率越高。
 * 容量设计为方块储存单元同级的 1/10~1/20，强调随身便携而非固定存储。
 */
public enum PortableCellTier {
    /** 初级：容量 5000 万，充能 50 万/tick，修复 10 耐久/tick */
    BASIC(50_000_000L, 500_000, 10),
    /** 中级：容量 2 亿，充能 100 万/tick，修复 20 耐久/tick */
    ADVANCED(200_000_000L, 1_000_000, 20),
    /** 高级：容量 10 亿，充能 400 万/tick，修复 40 耐久/tick */
    SUPER(1_000_000_000L, 4_000_000, 40);

    /** 能量容量上限 */
    public final long capacity;
    /** 单 tick 充能/输出上限（对储存单元注入而言） */
    public final int transferRate;
    /** 单 tick 可修复的赤石装备耐久点数 */
    public final int repairPerTick;

    PortableCellTier(long capacity, int transferRate, int repairPerTick) {
        this.capacity = capacity;
        this.transferRate = transferRate;
        this.repairPerTick = repairPerTick;
    }
}
