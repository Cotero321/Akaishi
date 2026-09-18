package com.example.akaishi.forge.io;

import com.example.akaishi.block.entity.MiniatureTerminalBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 微缩终端的 Forge 物品能力视图（第三方物流直达：原版漏斗、MEK 管道、AE2/RS 等）。
 * <p>
 * <b>为什么不能走通用 {@link MachineCapabilityProvider}：</b>那个适配层假定机器有
 * <b>真实槽位</b>——插入按 {@code min(堆量, 最大堆)} 记账后调 {@code setItem}，
 * 抽取按"读到的槽内容"原样回报。而微缩终端是<b>虚拟槽、纯转发</b>：真正能收多少取决于
 * 终端库空间与费用闸门，抽取还要先扣取出费；费用不足时整笔拒绝（不吞不吐）。
 * 套用通用适配层会把"转发未成功"当成功（丢物）、把"未抽出"当抽出（复制）。
 * <p>
 * 因此这里只做转发：槽位方向（输入槽只插、输出槽只抽）与全部搬运语义均下沉到
 * {@link MiniatureTerminalBlockEntity} 的 Container / {@code IItemPipeDevice} 实现，
 * 能力层仅负责槽位契约与方向硬约束（与 {@code RULES §5} 同构）。
 * <p>
 * 单次上限 1 组（方块实体的 {@code getMaxStackSize}），避免一次巨量塞入绕过节流。
 */
public final class MiniatureTerminalItemHandler implements IItemHandler, ICapabilityProvider {

    private final MiniatureTerminalBlockEntity terminal;
    private final LazyOptional<IItemHandler> self = LazyOptional.of(() -> this);

    /** 输/出槽位方向表（构造时按设备声明固化，槽位集合在设备生命周期内不变） */
    private final boolean[] canInsert;
    private final boolean[] canExtract;

    public MiniatureTerminalItemHandler(MiniatureTerminalBlockEntity terminal) {
        this.terminal = terminal;
        int n = terminal.getContainerSize();
        this.canInsert = new boolean[n];
        this.canExtract = new boolean[n];
        for (int slot : terminal.getPipeInputSlots()) {
            if (slot >= 0 && slot < n) {
                canInsert[slot] = true;
            }
        }
        for (int slot : terminal.getPipeOutputSlots()) {
            if (slot >= 0 && slot < n) {
                canExtract[slot] = true;
            }
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ITEM_HANDLER ? self.cast() : LazyOptional.empty();
    }

    @Override
    public int getSlots() {
        return terminal.getContainerSize();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return inRange(slot) ? terminal.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return inRange(slot) ? terminal.getMaxStackSize() : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        // 零副作用整堆预检：空间与费用都过才承诺收下
        return inRange(slot) && canInsert[slot] && terminal.canPlaceItem(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !inRange(slot) || !canInsert[slot]) {
            return stack; // 越界/空栈/非输入槽 → 原样退回
        }
        int batch = Math.min(stack.getCount(), terminal.getMaxStackSize());
        if (batch <= 0) {
            return stack;
        }
        ItemStack probe = stack.copyWithCount(batch);
        if (!terminal.canPlaceItem(slot, probe)) {
            return stack; // 整笔拒绝：库空间或费用不足时不动账本，也不吞物
        }
        if (!simulate) {
            terminal.setItem(slot, probe);
        }
        ItemStack leftover = stack.copy();
        leftover.shrink(batch);
        return leftover;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0 || !inRange(slot) || !canExtract[slot]) {
            return ItemStack.EMPTY; // 非输出槽 → 抽不到
        }
        ItemStack preview = terminal.getItem(slot);
        if (preview.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int take = Math.min(amount, Math.min(preview.getCount(), terminal.getMaxStackSize()));
        if (simulate) {
            // 模拟态不得不乐观回报：费用闸门只在实抽时结算，但实抽返回空即代表未抽出，不会复制
            return preview.copyWithCount(take);
        }
        return terminal.removeItem(slot, take); // 费用不足/无货时返回空堆，物品与账本零改动
    }

    private boolean inRange(int slot) {
        return slot >= 0 && slot < terminal.getContainerSize();
    }
}
