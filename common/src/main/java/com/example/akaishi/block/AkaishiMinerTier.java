package com.example.akaishi.block;

/**
 * 赤石矿机等级参数（4 级，由核心方块实例决定等级）。
 * 等级影响：挖矿速率倍率、能量容量、稀有矿物权重倍率、升级模块生效上限。
 */
public enum AkaishiMinerTier {

    /** 基础：1.0 倍速率，1M 能量，速度升级上限 8（储能上限 2） */
    BASIC(1.0, 1_000_000L, 1.0, 8, "basic"),
    /** 进阶：1.5 倍速率，5M 能量，速度升级上限 16（储能上限 4） */
    ADVANCED(1.5, 5_000_000L, 1.5, 16, "advanced"),
    /** 高级：2.0 倍速率，20M 能量，速度升级上限 24（储能上限 6） */
    SUPER(2.0, 20_000_000L, 2.0, 24, "super"),
    /** 终极：3.0 倍速率，50M 能量，速度升级上限 32（储能上限 8） */
    ULTIMATE(3.0, 50_000_000L, 3.0, 32, "ultimate");

    /** 挖矿进度推进速率倍率 */
    public final double rateMultiplier;
    /** 基础能量容量（储能升级可再提升） */
    public final long maxEnergy;
    /** 稀有矿物（钻石/绿宝石/下界合金碎片）权重倍率 */
    public final double rareMultiplier;
    /** 该等级可生效的速度升级（效率框架）模块数量上限 */
    public final int maxSpeedUpgrades;
    /** 语言键后缀 */
    public final String suffix;

    AkaishiMinerTier(double rateMultiplier, long maxEnergy, double rareMultiplier,
                     int maxSpeedUpgrades, String suffix) {
        this.rateMultiplier = rateMultiplier;
        this.maxEnergy = maxEnergy;
        this.rareMultiplier = rareMultiplier;
        this.maxSpeedUpgrades = maxSpeedUpgrades;
        this.suffix = suffix;
    }
}
