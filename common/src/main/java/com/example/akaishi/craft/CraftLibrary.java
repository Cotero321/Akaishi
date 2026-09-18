package com.example.akaishi.craft;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.menu.TerminalEntry;
import com.example.akaishi.menu.TerminalInventory;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 终端库的批量读写工具（虚拟加工与后续派发共用）。
 * <p>
 * <b>只经 {@link IItemStorageUnit} 的单一写入口</b>（{@code insert} / {@code extract}），
 * 因此不会绕过 {@code slotIp} 账本；读用 {@link TerminalInventory#snapshot} 的聚合结果
 * （含 {@code slices} 定位到「哪个单元哪个槽」）。
 * <p>
 * <b>失败纪律</b>：任何塞不下的余量一律落地成掉落物（{@code Containers.dropItemStack}），
 * 绝不"吞掉"或"复制" —— 与 {@code ItemPortTransfer} 同一条铁律。
 */
public final class CraftLibrary {

    private CraftLibrary() {
    }

    /** 库里某物品的可用量（按物品匹配；配方树的叶子是物品级需求，不区分 NBT） */
    public static long count(List<IItemStorageUnit> units, Item item) {
        return countIn(TerminalInventory.snapshot(units), item);
    }

    /**
     * 库快照聚合为「物品 → 总量」：批量判定（一次核几百条材料清单）用。
     * <p>
     * 逐条调用 {@link #hasAll} 会每条重建一次快照，在批量路径上是 O(条目 × 槽数)；
     * 先聚合一次再查表即可。
     */
    public static java.util.Map<Item, Long> aggregate(List<TerminalEntry> snapshot) {
        java.util.Map<Item, Long> stock = new java.util.HashMap<>();
        for (TerminalEntry entry : snapshot) {
            stock.merge(entry.display().getItem(), entry.amount(), Long::sum);
        }
        return stock;
    }

    /**
     * 库快照聚合（物品 → 总量）：<b>库存口径的唯一入口</b>。
     * <p>
     * 规划（逐级先扣库存）、"够不够"判定、开工复核必须用同一份库存，
     * 否则会出现"界面说能做、点下去说不够"的两套口径。
     */
    public static java.util.Map<Item, Long> stock(List<IItemStorageUnit> units) {
        return aggregate(TerminalInventory.snapshot(units));
    }

    /**
     * 按预先聚合好的库存判定材料齐备（与 {@link #hasAll(List, List)} 同一语义，仅省去重复取快照）。
     * <p>
     * <b>同名先累加</b>：{@code Plan.consumables()} = 现采材料 + 直接从库存吃掉的，
     * 同一物品可能两边各出现一条（只够一部分时），逐条比对会漏判（实际缺 5 个，却因为"另一条只缺 2 个"放行）。
     */
    public static boolean hasAll(java.util.Map<Item, Long> stock, List<VirtualCraftPlanner.Leaf> leaves) {
        if (leaves.isEmpty()) {
            return true;
        }
        java.util.Map<Item, Long> need = new java.util.HashMap<>();
        for (VirtualCraftPlanner.Leaf leaf : leaves) {
            need.merge(leaf.item(), leaf.count(), Long::sum);
        }
        for (java.util.Map.Entry<Item, Long> entry : need.entrySet()) {
            if (stock.getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 材料齐备判定（按叶子清单逐项核对）。
     * <p>
     * <b>只取一次库快照</b>：快照要遍历全部单元全部槽，逐叶重建在深链（上百个叶子）下是
     * O(叶子 × 槽数) 的重复计算。同名条目先合并，故共用一份快照等价。
     */
    public static boolean hasAll(List<IItemStorageUnit> units, List<VirtualCraftPlanner.Leaf> leaves) {
        if (leaves.isEmpty()) {
            return true;
        }
        java.util.Map<Item, Long> need = new java.util.HashMap<>();
        for (VirtualCraftPlanner.Leaf leaf : leaves) {
            need.merge(leaf.item(), leaf.count(), Long::sum);
        }
        List<TerminalEntry> snapshot = TerminalInventory.snapshot(units);
        for (java.util.Map.Entry<Item, Long> entry : need.entrySet()) {
            if (countIn(snapshot, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 按叶子清单一次性扣料；同样只取一次快照。
     *
     * @return 实际扣下的堆（保留原 NBT，供失败时回填）
     */
    public static List<ItemStack> takeAll(List<IItemStorageUnit> units, List<VirtualCraftPlanner.Leaf> leaves) {
        List<TerminalEntry> snapshot = TerminalInventory.snapshot(units);
        List<ItemStack> taken = new ArrayList<>();
        for (VirtualCraftPlanner.Leaf leaf : leaves) {
            taken.addAll(takeFrom(snapshot, leaf.item(), leaf.count()));
        }
        return taken;
    }

    /** 按聚合分片顺序扣料；返回实际扣下的堆（保留原 NBT，供失败时回填） */
    public static List<ItemStack> take(List<IItemStorageUnit> units, Item item, long amount) {
        return takeFrom(TerminalInventory.snapshot(units), item, amount);
    }

    private static long countIn(List<TerminalEntry> snapshot, Item item) {
        long total = 0L;
        for (TerminalEntry entry : snapshot) {
            if (entry.display().is(item)) {
                total += entry.amount();
            }
        }
        return total;
    }

    private static List<ItemStack> takeFrom(List<TerminalEntry> snapshot, Item item, long amount) {
        List<ItemStack> taken = new ArrayList<>();
        long left = amount;
        for (TerminalEntry entry : snapshot) {
            if (left <= 0L) {
                break;
            }
            if (!entry.display().is(item)) {
                continue;
            }
            for (TerminalEntry.Slice slice : entry.slices()) {
                if (left <= 0L) {
                    break;
                }
                int want = (int) Math.min(left, slice.count());
                ItemStack got = slice.unit().extract(slice.slot(), want);
                if (!got.isEmpty()) {
                    taken.add(got);
                    left -= got.getCount();
                }
            }
        }
        return taken;
    }

    /**
     * 整堆存入，返回未塞下的余量。
     * <p>
     * {@code insert} 是<b>整笔</b>语义（不塞部分），故先按 {@link IItemStorageUnit#acceptable} 夹量，
     * 否则"余量大于单槽可容纳"时会整笔被拒。
     */
    public static ItemStack insertAll(List<IItemStorageUnit> units, ItemStack stack) {
        ItemStack left = stack.copy();
        for (IItemStorageUnit unit : units) {
            if (left.isEmpty()) {
                break;
            }
            int accepted = Math.min(unit.acceptable(left), left.getCount());
            if (accepted <= 0) {
                continue;
            }
            int moved = unit.insert(left.copyWithCount(accepted));
            if (moved > 0) {
                left.shrink(moved);
            }
        }
        return left;
    }

    /** 回填多堆：能进库的进库，进不去的落地（物品守恒） */
    public static void refund(ServerLevel level, BlockPos pos, List<IItemStorageUnit> units,
            List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            ItemStack leftover = insertAll(units, stack);
            if (!leftover.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, leftover);
            }
        }
    }
}
