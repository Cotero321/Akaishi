package com.example.akaishi.life.organ;

import com.example.akaishi.life.sample.SampleGroup;

/**
 * 器官品质等级：由基因来源分组决定（温血 I → 龙族 IV）。
 * - 属性加成 = 模板基础值 × 品质倍率（每级 +25%，I=1.25 ~ IV=2.0）
 * - 基础排斥值：移植器官时一次性产生，此后按品质间隔随时间缓慢增长。
 *   数值为"降档曲线"（12/24/36/48，级差 12）：升级提升收益的同时排斥起点平稳增长，
 *   避免线性 15/30/45/60 让 III→IV 的排斥跃升吃掉升品收益。
 */
public enum QualityTier {

    I(1.25, 12, 45),
    II(1.5, 24, 30),
    III(1.75, 36, 20),
    IV(2.0, 48, 20);

    /** 属性加成倍率 */
    private final double multiplier;
    /** 移植时的基础排斥值 */
    private final int baseRejection;
    /** 排斥缓慢增长间隔（秒）：品质越高排斥涨得越快（III/IV 同级最快，封底 20 秒一跳） */
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
            case UNDEAD, EXPLOSIVE, ABERRATION -> II;
            case ENDER -> III;
            case BOSS, DRAGON -> IV;
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
