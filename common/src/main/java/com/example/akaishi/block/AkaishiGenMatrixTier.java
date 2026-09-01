package com.example.akaishi.block;

/**
 * 发生器矩阵等级参数：低级 3×3×3（45 倍，沿用组合结构）/ 高级 5×5×5（200 倍，沿用超级架构）。
 * 燃料总产能系数沿用旧结构（低级 ×9/8，高级 ×9/4）。
 */
public enum AkaishiGenMatrixTier {

    /** 低级：3×3×3，26 个外壳位，45 倍速率 */
    BASIC(1, 45, 3375, 5_000_000L, 9, 8, "basic"),
    /** 高级：5×5×5，124 个外壳位，200 倍速率 */
    ADVANCED(2, 200, 15000, 50_000_000L, 9, 4, "advanced");

    /** 结构半径（中心到边缘的距离，3×3×3=1，5×5×5=2） */
    public final int radius;
    /** 显示用产能倍率 */
    public final int multiply;
    /** 每 tick 燃烧速度（赤能源/t） */
    public final int generateRate;
    /** 最大能量存储 */
    public final long maxEnergy;
    /** 燃料总产能系数（分子/分母） */
    public final int fuelNum;
    public final int fuelDen;
    /** 语言键后缀 */
    public final String suffix;

    AkaishiGenMatrixTier(int radius, int multiply, int generateRate, long maxEnergy, int fuelNum, int fuelDen, String suffix) {
        this.radius = radius;
        this.multiply = multiply;
        this.generateRate = generateRate;
        this.maxEnergy = maxEnergy;
        this.fuelNum = fuelNum;
        this.fuelDen = fuelDen;
        this.suffix = suffix;
    }
}
