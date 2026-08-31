package com.example.template.item;

/**
 * 散热片品质：决定单片的散热效率（%）与耐久上限。
 * 高级散热片效率更高且更耐用，单片效率 1%~7%，满配 20 片终极 = 140%（散热效率无上限）。
 */
public enum HeatSinkQuality {

    /** 劣质：散热 1%，耐久最短 */
    POOR(1, 36_000),
    /** 普通：散热 2%，寿命约一个燃料周期（60 分钟） */
    NORMAL(2, 72_000),
    /** 良好：散热 3% */
    GOOD(3, 108_000),
    /** 优质：散热 4% */
    FINE(4, 144_000),
    /** 精良：散热 5%，耐久最长 */
    EXQUISITE(5, 180_000),
    /** 终极：散热 7%，最高档散热片 */
    ULTIMATE(7, 180_000);

    /** 散热效率（%），满配 20 片普通品质 = 40%，终极 = 140%（无封顶，效率越高温度压得越低） */
    public final int coolingPercent;
    /** 耐久上限（tick），仅反应堆运行燃烧时消耗 */
    public final int durability;

    HeatSinkQuality(int coolingPercent, int durability) {
        this.coolingPercent = coolingPercent;
        this.durability = durability;
    }
}
