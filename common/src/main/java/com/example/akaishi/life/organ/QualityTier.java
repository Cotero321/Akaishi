package com.example.akaishi.life.organ;

import com.example.akaishi.life.sample.SampleGroup;

/**
 * 器官品质等级：由基因来源分组决定（温血 I → 龙族 IV）。
 * - 属性加成 = 模板基础值 × 品质倍率（每级 +25%，I=1.25 ~ IV=2.0）
 * - 基础排斥值：移植器官时一次性产生，此后按品质间隔随时间缓慢增长
 */
public enum QualityTier {

    I(1.25, 15, 45),
    II(1.5, 30, 30),
    III(1.75, 45, 20),
    IV(2.0, 60, 15);

    /** 属性加成倍率 */
    private final double multiplier;
    /** 移植时的基础排斥值 */
    private final int baseRejection;
    /** 排斥缓慢增长间隔（秒）：品质越高排斥涨得越快 */
    private final int growthIntervalSeconds;

    QualityTier(double multiplier, int baseRejection, int growthIntervalSeconds) {
        this.multiplier = multiplier;
        this.baseRejection = baseRejection;
        this.growthIntervalSeconds = growthIntervalSeconds;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public int getBaseRejection() {
        return baseRejection;
    }

    public int getGrowthIntervalSeconds() {
        return growthIntervalSeconds;
    }

    /** 由基因来源分组映射品质等级 */
    public static QualityTier of(SampleGroup source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case WARM_BLOODED -> I;
            case UNDEAD, EXPLOSIVE -> II;
            case ENDER -> III;
            case DRAGON -> IV;
        };
    }

    /** 下一级品质（IV 已是最高级，返回 null） */
    public QualityTier next() {
        return switch (this) {
            case I -> II;
            case II -> III;
            case III -> IV;
            case IV -> null;
        };
    }
}
