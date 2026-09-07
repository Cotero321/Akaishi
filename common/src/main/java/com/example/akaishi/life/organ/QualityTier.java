package com.example.akaishi.life.organ;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.life.sample.SampleGroup;

/**
 * 器官品质等级：由基因来源分组决定（温血 I → 龙族 IV）。
 * - 属性加成 = 模板基础值 × 品质倍率（每级 +25%，I=1.25 ~ IV=2.0）
 * - 基础排斥值：移植器官时一次性产生，此后按品质间隔随时间缓慢增长。
 *   数值为"降档曲线"（12/24/36/48，级差 12）：升级提升收益的同时排斥起点平稳增长，
 *   避免线性 15/30/45/60 让 III→IV 的排斥跃升吃掉升品收益。
 * 三档数值均可被配置覆盖（akaishi-common.toml [organ_quality] 列表按品质序数填写，
 * 0 或缺失条目回退到下方内置默认）。
 */
public enum QualityTier {

    I(1.25, 12, 45),
    II(1.5, 24, 30),
    III(1.75, 36, 20),
    IV(2.0, 48, 20);

    /** 属性加成倍率（内置默认，可被配置覆盖） */
    private final double multiplier;
    /** 移植时的基础排斥值（内置默认） */
    private final int baseRejection;
    /** 排斥缓慢增长间隔（秒）（内置默认） */
    private final int growthIntervalSeconds;

    QualityTier(double multiplier, int baseRejection, int growthIntervalSeconds) {
        this.multiplier = multiplier;
        this.baseRejection = baseRejection;
        this.growthIntervalSeconds = growthIntervalSeconds;
    }

    public double getMultiplier() {
        return overrideDouble(ModConfig.organTierMultiplier, multiplier);
    }

    public int getBaseRejection() {
        return overrideInt(ModConfig.organTierBaseRejection, baseRejection);
    }

    public int getGrowthIntervalSeconds() {
        return overrideInt(ModConfig.organTierGrowthInterval, growthIntervalSeconds);
    }

    /** 品质配置 override（数组按品质序数，0 = 未配置 → 用内置默认；防配置缺条目越界） */
    private double overrideDouble(double[] arr, double def) {
        int i = ordinal();
        return i < arr.length && arr[i] > 0 ? arr[i] : def;
    }

    private int overrideInt(int[] arr, int def) {
        int i = ordinal();
        return i < arr.length && arr[i] > 0 ? arr[i] : def;
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
