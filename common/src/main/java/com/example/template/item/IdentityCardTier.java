package com.example.template.item;

/**
 * 终端身份卡等级：当前仅基础卡，预留未来扩展。
 * 等级只影响口传输速率倍率（区块加载由终端内腔组件对已认证口统一生效）。
 */
public enum IdentityCardTier {

    BASIC(0, 1),
    ADVANCED(1, 2),
    ELITE(2, 4),
    ULTIMATE(3, 8);

    private final int id;
    /** 相对基础速率的传输倍率 */
    private final int rateMultiplier;

    IdentityCardTier(int id, int rateMultiplier) {
        this.id = id;
        this.rateMultiplier = rateMultiplier;
    }

    public int id() {
        return id;
    }

    public int rateMultiplier() {
        return rateMultiplier;
    }

    /** 由序数索引取等级（NBT 存储用），越界回退基础级 */
    public static IdentityCardTier byId(int id) {
        for (IdentityCardTier t : values()) {
            if (t.id == id) {
                return t;
            }
        }
        return BASIC;
    }
}
