package com.example.akaishi.api;

/**
 * 标记接口：实现该接口的方块实体支持"挖掘/拾取数据保留"。
 * 挖掘掉落时数据写入掉落物品的 BlockEntityTag，放置方块后原版自动恢复。
 * 实现方可通过 {@link #excludedKeys()} 指定不保留的 NBT 键（如已随方块掉落的内部物品）。
 */
public interface IDataCarrier {

    /** 保存数据时需要排除的 NBT 键（默认全保留） */
    default String[] excludedKeys() {
        return new String[0];
    }
}
