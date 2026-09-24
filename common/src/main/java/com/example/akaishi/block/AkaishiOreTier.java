package com.example.akaishi.block;

/**
 * 赤石矿簇浓度等级。
 * 当前只保留中浓度一档（低浓度/完美/无暇已随方块一并移除，恢复时在此加回常量即可）。
 */
public enum AkaishiOreTier {
    MEDIUM("medium", 2, 10, 6);

    /** id 后缀，如 akaishi_ore_medium */
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
