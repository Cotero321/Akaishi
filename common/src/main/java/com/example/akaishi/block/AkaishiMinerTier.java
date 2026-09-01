package com.example.akaishi.block;

/**
 * 赤石矿机等级参数（4 级，由核心方块实例决定等级）。
 * 等级影响：挖矿速率倍率、能量容量、稀有矿物权重倍率。
 */
public enum AkaishiMinerTier {

    /** 基础：1.0 倍速率，1M 能量 */
    BASIC(1.0, 1_000_000L, 1.0, "basic"),
    /** 进阶：1.5 倍速率，5M 能量 */
    ADVANCED(1.5, 5_000_000L, 1.5, "advanced"),
    /** 高级：2.0 倍速率，20M 能量 */
    SUPER(2.0, 20_000_000L, 2.0, "super"),
    /** 终极：3.0 倍速率，50M 能量 */
    ULTIMATE(3.0, 50_000_000L, 3.0, "ultimate");

    /** 挖矿进度推进速率倍率 */
    public final double rateMultiplier;
    /** 基础能量容量（储能升级可再提升） */
    public final long maxEnergy;
    /** 稀有矿物（钻石/绿宝石/下界合金碎片）权重倍率 */
    public final double rareMultiplier;
    /** 语言键后缀 */
    public final String suffix;

    AkaishiMinerTier(double rateMultiplier, long maxEnergy, double rareMultiplier, String suffix) {
        this.rateMultiplier = rateMultiplier;
        this.maxEnergy = maxEnergy;
        this.rareMultiplier = rareMultiplier;
        this.suffix = suffix;
    }
}
