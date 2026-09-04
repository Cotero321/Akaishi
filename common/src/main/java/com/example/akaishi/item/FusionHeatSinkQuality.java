package com.example.akaishi.item;

/**
 * 聚变散热片品质：决定单片散热效率（%）与耐久上限。
 * 散热片经控制器热量页 GUI 放入控制器，效率 5%~20%；生命散热片由生命灰烬合成，为最高档。
 * 消耗机制与反应堆散热片不同：运行/宕机降温期间每 100 tick（5 秒）消耗 1 点耐久。
 */
public enum FusionHeatSinkQuality {

    /** 散热 5% */
    TIER1(5, 8_000),
    /** 散热 7% */
    TIER2(7, 8_000),
    /** 散热 9% */
    TIER3(9, 8_000),
    /** 散热 12% */
    TIER4(12, 8_000),
    /** 散热 15% */
    TIER5(15, 8_000),
    /** 生命散热片：散热 20%，生命灰烬合成 */
    LIFE(20, 8_000);

    /** 散热效率（%） */
    public final int coolingPercent;
    /** 耐久上限（按每 100 tick 消耗 1 点计，8000 点 ≈ 11 小时） */
    public final int durability;

    FusionHeatSinkQuality(int coolingPercent, int durability) {
        this.coolingPercent = coolingPercent;
        this.durability = durability;
    }
}
