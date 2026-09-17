package com.example.akaishi.menu;

import java.util.List;

import com.example.akaishi.api.storage.IItemStorageUnit;

import net.minecraft.world.item.ItemStack;

/**
 * 物品终端库页的聚合条目：同物品同 NBT 跨全部储存单元合并为一条（AE2 网格条目语义）。
 * <p>
 * {@code slices} 记录该条目在各单元各槽中的实际占用，取出时按此顺序消耗，
 * 每次取出仍走单元的 {@link IItemStorageUnit#extract} 单一写入口，账本不偏移（D9 / D10）。
 */
public final class TerminalEntry {

    /** 分片：条目落在某个单元某个槽位上的一段 */
    public record Slice(IItemStorageUnit unit, int slot, int count) {
    }

    /** 展示堆：数量恒为 1，只承载物品与 NBT（真实数量见 {@link #amount()}） */
    private final ItemStack display;
    private final long amount;
    private final List<Slice> slices;

    TerminalEntry(ItemStack display, List<Slice> slices) {
        this.display = display;
        this.slices = List.copyOf(slices);
        long total = 0L;
        for (Slice slice : this.slices) {
            total += slice.count();
        }
        this.amount = total;
    }

    public ItemStack display() {
        return this.display;
    }

    /** 聚合总量，可远超单堆上限 */
    public long amount() {
        return this.amount;
    }

    /** 只读分片视图（终端内部落账用，不对外暴露可变结构） */
    public List<Slice> slices() {
        return this.slices;
    }
}
