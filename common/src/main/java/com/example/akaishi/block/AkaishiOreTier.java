package com.example.akaishi.block;

/**
 * 赤石矿簇浓度等级。
 * 浓度越高：掉落赤石晶越多、矿脉越小但越稀有。
 */
public enum AkaishiOreTier {
    LOW("low", 1, 20, 9),
    MEDIUM("medium", 2, 10, 6),
    PERFECT("perfect", 3, 4, 4),
    FLAWLESS("flawless", 4, 1, 2);

    /** id 后缀，如 akaishi_ore_low */
    private final String suffix;
    /** 开采固定掉落数量（不受时运/幸运影响） */
    private final int dropCount;
    /** 每个区块生成尝试次数 */
    private final int veinCount;
    /** 矿簇最大体积 */
    private final int veinSize;

    AkaishiOreTier(String suffix, int dropCount, int veinCount, int veinSize) {
        this.suffix = suffix;
        this.dropCount = dropCount;
        this.veinCount = veinCount;
        this.veinSize = veinSize;
    }

    public String suffix() {
        return suffix;
    }

    public int dropCount() {
        return dropCount;
    }

    public int veinCount() {
        return veinCount;
    }

    public int veinSize() {
        return veinSize;
    }
}
