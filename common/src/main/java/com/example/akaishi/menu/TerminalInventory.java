package com.example.akaishi.menu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.akaishi.api.storage.IItemStorageUnit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 物品终端库页的聚合读视图：把全部储存单元的槽位按「物品 + NBT」合并成条目列表。
 * <p>
 * 只读聚合，不持有任何写路径（写入一律走 {@link TerminalActions}），
 * 因此不存在绕过 {@code slotIp} 账本的可能（§2.5 防线）。
 */
public final class TerminalInventory {

    private TerminalInventory() {
    }

    /**
     * 分组键：同物品且同 NBT 才算同一类。
     * <p>
     * <b>不能用 {@link ItemStack} 当 Map 键</b>：它没有内容语义的 {@code equals}/{@code hashCode}
     * （沿用引用身份），拿它做键会让<b>每个物理槽各自成条</b> —— 表现为"终端不按类合并、数字永远 ≤ 单堆上限"。
     * 这里按其内容（物品 + NBT）建键，与储存单元视图的 {@code isSameItemSameTags} 口径严格一致。
     */
    private record StackKey(Item item, CompoundTag tag) {

        private static StackKey of(ItemStack stack) {
            return new StackKey(stack.getItem(), stack.getTag());
        }

        /** 展示堆（恒 1 件，保留 NBT），供界面渲染图标与悬浮文本 */
        private ItemStack display() {
            ItemStack stack = new ItemStack(this.item);
            stack.setTag(this.tag);
            return stack;
        }
    }

    /**
     * 聚合全部单元的槽位内容。
     * <p>
     * 遍历顺序恒为「单元序 → 槽序」，且结果用 {@link LinkedHashMap} 保序，
     * 使同一次聚合的条目顺序稳定（单元列表顺序由结构扫描给出，亦稳定），
     * 避免客户端列表在刷新时跳动。
     */
    public static List<TerminalEntry> snapshot(List<IItemStorageUnit> units) {
        Map<StackKey, List<TerminalEntry.Slice>> grouped = new LinkedHashMap<>();
        for (IItemStorageUnit unit : units) {
            int slots = unit.slots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stack = unit.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                grouped.computeIfAbsent(StackKey.of(stack), k -> new ArrayList<>())
                        .add(new TerminalEntry.Slice(unit, slot, stack.getCount()));
            }
        }
        List<TerminalEntry> entries = new ArrayList<>(grouped.size());
        grouped.forEach((key, slices) -> entries.add(new TerminalEntry(key.display(), slices)));
        return entries;
    }
}
