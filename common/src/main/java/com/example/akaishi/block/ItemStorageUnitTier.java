package com.example.akaishi.block;

/**
 * 物品储存单元等阶：等阶只决定 IP 容量（可容纳的占用量上限），槽位数各阶一致。
 * <p>
 * 容量按 ×8 递进（设计 D6）：基础 1e6 / 进阶 8e6 / 超级 64e6 IP。
 * 与 {@link com.example.akaishi.energy.EnergyCellTier} 同构，但量纲独立（IP 非赤能源）。
 */
public enum ItemStorageUnitTier {

    /** 基础：100 万 IP（≈ 1.4 箱钻石） */
    BASIC(1_000_000L),
    /** 进阶：800 万 IP */
    ADVANCED(8_000_000L),
    /** 超级：6400 万 IP */
    SUPER(64_000_000L);

    /** IP 占用容量上限 */
    public final long ipCapacity;

    ItemStorageUnitTier(long ipCapacity) {
        this.ipCapacity = ipCapacity;
    }
}
