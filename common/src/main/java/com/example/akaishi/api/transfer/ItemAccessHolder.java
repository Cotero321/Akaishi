package com.example.akaishi.api.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * 物品访问实现的装载点（依赖倒置：common 只依赖 {@link IItemAccess}）。
 * <p>
 * 默认实现是原版 {@link Container} 兜底，因此**不装任何加载器能力也能跑**（箱子/漏斗/多数简单机器）；
 * 各加载器在自身初始化里用 {@link #install(IItemAccess)} 装上更强的实现（forge：物品能力）。
 */
public final class ItemAccessHolder {

    private static volatile IItemAccess access = new ContainerItemAccess();

    private ItemAccessHolder() {
    }

    public static IItemAccess get() {
        return access;
    }

    /** 加载器注册实现（各加载器 init 调用一次；传 null 忽略） */
    public static void install(IItemAccess impl) {
        if (impl != null) {
            access = impl;
        }
    }

    /** 兜底实现：只认原版 Container（无方向语义，side 参数忽略） */
    private static final class ContainerItemAccess implements IItemAccess {

        @Override
        public ItemStack insert(Level level, BlockPos pos, Direction side, ItemStack stack) {
            Container container = containerAt(level, pos);
            if (container == null || stack.isEmpty()) {
                return stack;
            }
            ItemStack remaining = stack.copy();
            int size = container.getContainerSize();
            // 先并入同类堆，再找空位（与原版漏斗一致）
            for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
                for (int slot = 0; slot < size && !remaining.isEmpty(); slot++) {
                    if (!container.canPlaceItem(slot, remaining)) {
                        continue;
                    }
                    ItemStack existing = container.getItem(slot);
                    if (pass == 0) {
                        if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, remaining)) {
                            continue;
                        }
                        int room = Math.min(container.getMaxStackSize(), remaining.getMaxStackSize())
                                - existing.getCount();
                        if (room <= 0) {
                            continue;
                        }
                        int move = Math.min(room, remaining.getCount());
                        existing.grow(move);
                        remaining.shrink(move);
                        container.setChanged();
                    } else if (existing.isEmpty()) {
                        int move = Math.min(Math.min(container.getMaxStackSize(), remaining.getMaxStackSize()),
                                remaining.getCount());
                        ItemStack placed = remaining.copy();
                        placed.setCount(move);
                        container.setItem(slot, placed);
                        remaining.shrink(move);
                        container.setChanged();
                    }
                }
            }
            return remaining;
        }

        @Override
        public ItemStack extract(Level level, BlockPos pos, Direction side, int maxCount,
                Predicate<ItemStack> filter) {
            Container container = containerAt(level, pos);
            if (container == null || maxCount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack result = ItemStack.EMPTY;
            int size = container.getContainerSize();
            for (int slot = 0; slot < size && result.getCount() < maxCount; slot++) {
                ItemStack existing = container.getItem(slot);
                if (existing.isEmpty() || (filter != null && !filter.test(existing))) {
                    continue;
                }
                // 只聚同类堆：不同物品不混在一笔里，避免调用方回退时错位
                if (!result.isEmpty() && !ItemStack.isSameItemSameTags(result, existing)) {
                    continue;
                }
                int take = Math.min(maxCount - result.getCount(), existing.getCount());
                if (take <= 0) {
                    continue;
                }
                if (result.isEmpty()) {
                    result = existing.copy();
                    result.setCount(take);
                } else {
                    result.grow(take);
                }
                existing.shrink(take);
                if (existing.isEmpty()) {
                    container.setItem(slot, ItemStack.EMPTY);
                }
                container.setChanged();
            }
            return result;
        }

        @Override
        public int acceptable(Level level, BlockPos pos, Direction side, ItemStack stack) {
            Container container = containerAt(level, pos);
            if (container == null || stack.isEmpty()) {
                return 0;
            }
            int size = container.getContainerSize();
            int room = 0;
            for (int slot = 0; slot < size && room < stack.getCount(); slot++) {
                if (!container.canPlaceItem(slot, stack)) {
                    continue;
                }
                ItemStack existing = container.getItem(slot);
                int slotMax = Math.min(container.getMaxStackSize(), stack.getMaxStackSize());
                if (existing.isEmpty()) {
                    room += slotMax;
                } else if (ItemStack.isSameItemSameTags(existing, stack)) {
                    room += Math.max(0, Math.min(slotMax, existing.getMaxStackSize()) - existing.getCount());
                }
            }
            // 纯查询：不 setItem、不 setChanged，也不会改到 existing（上面只读计数）
            return Math.min(room, stack.getCount());
        }

        @Override
        public boolean hasItemCapability(Level level, BlockPos pos, Direction side) {
            // 兜底实现只认原版 Container；纯查询，不改动任何状态
            return containerAt(level, pos) != null;
        }

        private static Container containerAt(Level level, BlockPos pos) {
            if (level == null || pos == null || !level.isLoaded(pos)) {
                return null;
            }
            return level.getBlockEntity(pos) instanceof Container container ? container : null;
        }
    }
}
