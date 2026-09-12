package com.example.akaishi.energy;

/**
 * 生命能量管道等级：定义各等级管道每 tick 的生命能量传输速率。
 * 对齐物品管道四级制（基础/中级/高级/超级），每级 4 倍速率。
 * 与赤能源管道相互独立（能量类型隔离，数值不可混用），故单独定义生命域数值。
 */
public enum LifeEnergyPipeTier {
    /** 基础：1000/tick（与旧单等级管道一致，保证旧存档平衡不变） */
    BASIC(1000, EnergyPipeTier.BASIC),
    /** 中级：4000/tick */
    ADVANCED(4000, EnergyPipeTier.ADVANCED),
    /** 高级：16000/tick */
    ELITE(16000, EnergyPipeTier.ELITE),
    /** 超级：64000/tick */
    ULTIMATE(64000, EnergyPipeTier.ULTIMATE);

    /** 每 tick 生命能量传输速率 */
    public final int transferRate;
    /** 对应的管道等级标识（供父类构造，保持继承的 getTier 语义正确） */
    public final EnergyPipeTier level;

    LifeEnergyPipeTier(int transferRate, EnergyPipeTier level) {
        this.transferRate = transferRate;
        this.level = level;
    }
}
