package com.example.template.menu;

import java.util.Locale;

/**
 * GUI 能量数值格式化工具：大数值缩写显示（T/B/M/K）。
 * 由多个机器界面共用，避免各 Screen 重复实现。
 */
public final class EnergyFormat {

    private EnergyFormat() {
    }

    /** >=1T 万亿，>=1B 十亿，>=1M 百万，>=1K 千，否则原样输出 */
    public static String format(long v) {
        if (v >= 1_000_000_000_000L) {
            return trim(v / 1.0e12) + "T";
        }
        if (v >= 1_000_000_000L) {
            return trim(v / 1.0e9) + "B";
        }
        if (v >= 1_000_000L) {
            return trim(v / 1.0e6) + "M";
        }
        if (v >= 1_000L) {
            return trim(v / 1.0e3) + "K";
        }
        return String.valueOf(v);
    }

    /** 保留 1 位小数，整数时去掉小数部分（2.0 → 2） */
    private static String trim(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }
}
