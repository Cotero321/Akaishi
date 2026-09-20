package com.example.akaishi.forge.transfer;

import com.example.akaishi.api.transfer.IItemAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.function.Predicate;

/**
 * forge 侧物品访问实现：优先取 Forge 物品能力（覆盖漏斗、MEK 管道、AE2/RS 等），
 * 无能力但实现原版 {@link Container} 的方块退回 {@link InvWrapper} 包装。
 * <p>
 * 由 {@code AkaishiModForge} 在 init 时经 {@code ItemAccessHolder.install(...)} 装上，
 * 从而让 common 的搬运引擎（{@code ItemPortTransfer}）在 forge 上拿到完整兼容性。
 */
public final class ForgeItemAccess implements IItemAccess {

    @Override
    public ItemStack insert(Level level, BlockPos pos, Direction side, ItemStack stack) {
        IItemHandler handler = handlerAt(level, pos, side);
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            // 契约：insertItem 不修改传入堆，返回塞不下的余量
            remaining = handler.insertItem(slot, remaining, false);
        }
        return remaining;
    }

    @Override
    public ItemStack extract(Level level, BlockPos pos, Direction side, int maxCount,
            Predicate<ItemStack> filter) {
        IItemHandler handler = handlerAt(level, pos, side);
        if (handler == null || maxCount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        for (int slot = 0; slot < handler.getSlots() && result.getCount() < maxCount; slot++) {
            ItemStack probe = handler.getStackInSlot(slot);
            if (probe.isEmpty() || (filter != null && !filter.test(probe))) {
                continue;
            }
            // 只聚同类堆：不同物品不混在一笔里，便于调用方按原样回退
            if (!result.isEmpty() && !ItemStack.isSameItemSameTags(result, probe)) {
                continue;
            }
            ItemStack taken = handler.extractItem(slot, maxCount - result.getCount(), false);
            if (taken.isEmpty()) {
                continue;
            }
            if (result.isEmpty()) {
                result = taken.copy();
            } else {
                result.grow(taken.getCount());
            }
        }
        return result;
    }

    @Override
    public int acceptable(Level level, BlockPos pos, Direction side, ItemStack stack) {
        IItemHandler handler = handlerAt(level, pos, side);
        if (handler == null || stack.isEmpty()) {
            return 0;
        }
        // simulate=true：能力契约保证不改变容器状态，可安全用于"据实计费"的预检
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, true);
        }
        return stack.getCount() - remaining.getCount();
    }

    @Override
    public boolean hasItemCapability(Level level, BlockPos pos, Direction side) {
        // 只看"有没有容器"：满仓的机器也必须判为存在，故不能复用 acceptable（那时返回 0）
        return handlerAt(level, pos, side) != null;
    }

    /** 取面朝侧的能力；无能力时用 {@link InvWrapper} 兜底（只读语义仍走原版容器） */
    private static IItemHandler handlerAt(Level level, BlockPos pos, Direction side) {
        if (level == null || pos == null || !level.isLoaded(pos)) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
        if (handler != null) {
            return handler;
        }
        return be instanceof Container container ? new InvWrapper(container) : null;
    }
}
