package com.example.akaishi.forge.io;

import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.item.IItemPipeDevice;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 机器对外第三方物流能力提供者：凡实现 {@link IItemPipeDevice} / {@link IFluidPipeDevice}
 * 的方块实体统一挂载本能力，分别暴露 ITEM_HANDLER 与 FLUID_HANDLER（能力实例惰性创建）。
 * 方向与家族过滤全部下沉到两个 Adapter，此处不重复判断。
 */
public final class MachineCapabilityProvider implements ICapabilitySerializable<CompoundTag> {

    private final LazyOptional<IItemHandlerModifiable> item;
    private final LazyOptional<IFluidHandler> fluid;

    public MachineCapabilityProvider(BlockEntity be) {
        // 未实现对应 PipeDevice 时以空 LazyOptional 表示"无此能力"，字段永不为 null
        this.item = be instanceof IItemPipeDevice device
                ? LazyOptional.of(() -> new ForgeItemHandler(device))
                : LazyOptional.empty();
        this.fluid = be instanceof IFluidPipeDevice device
                ? LazyOptional.of(() -> new ForgeFluidDeviceHandler(device))
                : LazyOptional.empty();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return item.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluid.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return new CompoundTag(); // 容器/罐数据由 BE 自身 NBT 保存，能力无独立持久化
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    }
}
