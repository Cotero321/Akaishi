package com.example.akaishi.energy;

/**
 * 生命能量储存器等级：等级越高容量越大。
 * 镜像赤能源储存单元的分级比例（基础 ×4 → 高级 ×10 → 超级）。
 * 基础级容量与旧单方块默认配置一致（100 万），保证旧存档平衡不变。
 */
public enum LifeEnergyCellTier {
    /** 基础：容量 100 万（对应旧"生命能量储存器"默认容量） */
    BASIC(1_000_000L),
    /** 高级：容量 400 万 */
    ADVANCED(4_000_000L),
    /** 超级：容量 4000 万 */
    SUPER(40_000_000L);

    /** 生命能量容量上限 */
    public final long capacity;

    LifeEnergyCellTier(long capacity) {
        this.capacity = capacity;
    }
}
