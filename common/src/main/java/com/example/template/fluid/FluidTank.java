package com.example.template.fluid;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

/**
 * 液体槽：单种液体的容量存储（Fluid + 数量），供机器与液体管道共享。
 * 通过 Architectury 的跨平台 {@link FluidStack} 持有液体，NBT 序列化走平台实现。
 * 未指定液体或数量为 0 视为空。
 */
public class FluidTank {

    private final long capacity;
    private FluidStack stack = FluidStack.empty();

    public FluidTank(long capacity) {
        this.capacity = capacity;
    }

    public long getCapacity() {
        return capacity;
    }

    public Fluid getFluid() {
        return stack.getFluid();
    }

    public long getAmount() {
        return stack.getAmount();
    }

    public FluidStack getStack() {
        return stack;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public boolean isFull() {
        return stack.getAmount() >= capacity;
    }

    /** 注入液体，返回实际注入量（simulate=true 时不修改状态） */
    public long fill(FluidStack resource, boolean simulate) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        if (!stack.isEmpty() && !stack.isFluidEqual(resource)) {
            return 0; // 不同液体不能混装
        }
        long filled = Math.min(capacity - stack.getAmount(), resource.getAmount());
        if (filled <= 0) {
            return 0;
        }
        if (!simulate) {
            if (stack.isEmpty()) {
                stack = resource.copyWithAmount(filled);
            } else {
                stack.grow(filled);
            }
            onChanged();
        }
        return filled;
    }

    /** 按指定液体抽取，返回实际抽出的液体栈 */
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (resource == null || resource.isEmpty() || !stack.isFluidEqual(resource)) {
            return FluidStack.empty();
        }
        return drain(resource.getAmount(), simulate);
    }

    /** 按数量抽取当前液体 */
    public FluidStack drain(long amount, boolean simulate) {
        long drained = Math.min(stack.getAmount(), amount);
        if (drained <= 0) {
            return FluidStack.empty();
        }
        FluidStack out = stack.copyWithAmount(drained);
        if (!simulate) {
            stack.shrink(drained);
            if (stack.getAmount() <= 0) {
                stack = FluidStack.empty();
            }
            onChanged();
        }
        return out;
    }

    /** 直接设置液体内容（不检查容量，用于 NBT 恢复） */
    public void setStack(FluidStack stack) {
        this.stack = stack == null || stack.isEmpty() ? FluidStack.empty() : stack;
    }

    /** 内容变化回调（子类可覆盖触发方块标记保存） */
    protected void onChanged() {
    }

    public CompoundTag writeToNbt() {
        CompoundTag tag = new CompoundTag();
        FluidStackHooks.write(stack, tag);
        return tag;
    }

    public void readFromNbt(CompoundTag tag) {
        this.stack = FluidStackHooks.read(tag);
    }
}
