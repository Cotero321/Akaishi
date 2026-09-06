package com.example.akaishi.forge.io;

import com.example.akaishi.api.item.IItemPipeDevice;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

/**
 * 物品管道设备（{@link IItemPipeDevice}）的 Forge 第三方物品能力视图。
 * <p>
 * 把「输入槽可插、输出槽可抽」的方向语义原样映射为 ITEM_HANDLER，槽索引与设备 Container 槽一一对应：
 * - 仅输出机器（矿机物品输出口/转口等）：全部槽只可抽取，第三方插入一律原样退回；
 * - 仅输入机器：全部槽只可插入，第三方抽取一律返回空栈；
 * - 双通设备（如培养器输入=产出槽）按设备声明同时允许插与抽。
 * 严禁在适配层放宽方向限制——与 GUI / 自家物品管道行为保持一致。
 */
public final class ForgeItemHandler implements IItemHandlerModifiable {

    private final Container container;
    private final boolean[] canInsert;
    private final boolean[] canExtract;

    public ForgeItemHandler(IItemPipeDevice device) {
        this.container = device;
        int n = device.getContainerSize();
        this.canInsert = new boolean[n];
        this.canExtract = new boolean[n];
        for (int s : device.getPipeInputSlots()) {
            if (s >= 0 && s < n) {
                canInsert[s] = true;
            }
        }
        for (int s : device.getPipeOutputSlots()) {
            if (s >= 0 && s < n) {
                canExtract[s] = true;
            }
        }
    }

    private boolean inRange(int slot) {
        return slot >= 0 && slot < container.getContainerSize();
    }

    @Override
    public int getSlots() {
        return container.getContainerSize();
    }

    @Override
    public int getSlotLimit(int slot) {
        if (!inRange(slot)) {
            return 0;
        }
        ItemStack s = container.getItem(slot);
        return s.isEmpty() ? 64 : s.getMaxStackSize();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return inRange(slot) ? container.getItem(slot) : ItemStack.EMPTY;
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !inRange(slot) || !canInsert[slot]) {
            return stack; // 越界/空栈/非输入槽（仅输出机器）→ 原样退回
        }
        ItemStack existing = container.getItem(slot);
        ItemStack work = stack.copy();
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(existing, work)) {
                return stack; // 槽内是其它物品：不覆盖，交给抽取侧先清空（与自家管道一致）
            }
            int space = existing.getMaxStackSize() - existing.getCount();
            if (space <= 0) {
                return stack;
            }
            int add = Math.min(space, work.getCount());
            if (!simulate) {
                existing.grow(add);
                container.setChanged();
            }
            work.shrink(add);
        } else {
            int put = Math.min(work.getCount(), work.getMaxStackSize());
            if (!simulate) {
                container.setItem(slot, work.copyWithCount(put));
            }
            work.shrink(put);
        }
        return work;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !inRange(slot) || !canExtract[slot]) {
            return ItemStack.EMPTY; // 非输出槽（仅输入机器）→ 抽不到
        }
        ItemStack existing = container.getItem(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, existing.getCount());
        if (!simulate) {
            container.removeItem(slot, take);
        }
        return existing.copyWithCount(take);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (!inRange(slot) || !canInsert[slot]) {
            return;
        }
        container.setItem(slot, stack);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return inRange(slot) && canInsert[slot];
    }
}
