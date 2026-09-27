package com.example.akaishi.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 方块实体数据保留工具：把 BE 的 NBT 写入物品的 BlockEntityTag。
 * 放置方块时原版 {@code BlockItem} 自动读取 BlockEntityTag 并调用 BE.load 恢复数据。
 */
public final class DataCarrierHelper {

    private DataCarrierHelper() {
    }

    /** 保存 BE 数据到掉落物品（排除指定 NBT 键） */
    public static void saveToItem(BlockEntity be, ItemStack stack, String... excludedKeys) {
        CompoundTag tag = be.saveWithoutMetadata();
        tag.remove("id");
        for (String key : excludedKeys) {
            tag.remove(key);
        }
        stack.getOrCreateTag().put("BlockEntityTag", tag);
    }
}
