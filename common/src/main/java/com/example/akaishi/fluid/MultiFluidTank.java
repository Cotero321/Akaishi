package com.example.akaishi.fluid;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多液体罐：共享总容量的分类型液体存储（Map&lt;液体, 数量&gt;），继承 {@link FluidTank} 以兼容
 * {@link com.example.akaishi.api.fluid.IFluidPipeDevice} 的"按罐交互"（管道无需感知液体类型）。
 * {@link #fill} 按液体类型合并、{@link #drain(long, boolean)} 抽当前第一种非空液体，
 * {@link #getAmount()} 为总量。NBT 序列化走 {@link FluidStackHooks}（每类型一条）。
 */
public class MultiFluidTank extends FluidTank {

    /** 液体 → 数量（LinkedHashMap 保序，抽取顺序确定；方块实体主线程单线程访问） */
    private final Map<Fluid, Long> contents = new LinkedHashMap<>();

    public MultiFluidTank(long capacity) {
        super(capacity);
    }

    @Override
    public long getAmount() {
        long total = 0;
        for (long v : contents.values()) {
            total += v;
        }
        return total;
    }

    @Override
    public Fluid getFluid() {
        return contents.isEmpty() ? null : contents.keySet().iterator().next();
    }

    /** 指定液体的数量（mb） */
    public long getAmount(Fluid fluid) {
        return fluid == null ? 0 : contents.getOrDefault(fluid, 0L);
    }

    /** 液体种类数 */
    public int typeCount() {
        return contents.size();
    }

    /** 液体类型快照（遍历时安全，不受后续 drain 影响） */
    public List<Fluid> fluidTypes() {
        return new ArrayList<>(contents.keySet());
    }

    @Override
    public boolean isEmpty() {
        return contents.isEmpty();
    }

    @Override
    public boolean isFull() {
        return getAmount() >= getCapacity();
    }

    /** 注入液体（按类型合并），返回实际注入量（simulate=true 不修改状态） */
    @Override
    public long fill(FluidStack resource, boolean simulate) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        long filled = Math.min(getCapacity() - getAmount(), resource.getAmount());
        if (filled <= 0) {
            return 0;
        }
        if (!simulate) {
            contents.merge(resource.getFluid(), filled, Long::sum);
            onChanged();
        }
        return filled;
    }

    /** 按指定液体与数量抽取 */
    public FluidStack drain(Fluid fluid, long amount, boolean simulate) {
        if (fluid == null || amount <= 0) {
            return FluidStack.empty();
        }
        long drained = Math.min(contents.getOrDefault(fluid, 0L), amount);
        if (drained <= 0) {
            return FluidStack.empty();
        }
        FluidStack out = FluidStack.create(fluid, drained);
        if (!simulate) {
            removeAmount(fluid, drained);
            onChanged();
        }
        return out;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (resource == null || resource.isEmpty()) {
            return FluidStack.empty();
        }
        return drain(resource.getFluid(), resource.getAmount(), simulate);
    }

    /** 按数量抽取当前第一种非空液体（液体管道用，返回单一液体栈） */
    @Override
    public FluidStack drain(long amount, boolean simulate) {
        if (amount <= 0 || contents.isEmpty()) {
            return FluidStack.empty();
        }
        return drain(contents.keySet().iterator().next(), amount, simulate);
    }

    @Override
    public FluidStack getStack() {
        if (contents.isEmpty()) {
            return FluidStack.empty();
        }
        Map.Entry<Fluid, Long> first = contents.entrySet().iterator().next();
        return FluidStack.create(first.getKey(), first.getValue());
    }

    @Override
    public void setStack(FluidStack stack) {
        contents.clear();
        if (stack != null && !stack.isEmpty()) {
            contents.put(stack.getFluid(), stack.getAmount());
        }
    }

    /** 从存储中扣除数量并清理空项 */
    private void removeAmount(Fluid fluid, long amount) {
        long left = contents.getOrDefault(fluid, 0L) - amount;
        if (left <= 0) {
            contents.remove(fluid);
        } else {
            contents.put(fluid, left);
        }
    }

    @Override
    public CompoundTag writeToNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<Fluid, Long> e : contents.entrySet()) {
            CompoundTag t = new CompoundTag();
            FluidStackHooks.write(FluidStack.create(e.getKey(), e.getValue()), t);
            list.add(t);
        }
        tag.put("Contents", list);
        return tag;
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        contents.clear();
        ListTag list = tag.getList("Contents", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            FluidStack s = FluidStackHooks.read(list.getCompound(i));
            if (!s.isEmpty()) {
                contents.put(s.getFluid(), s.getAmount());
            }
        }
    }
}
