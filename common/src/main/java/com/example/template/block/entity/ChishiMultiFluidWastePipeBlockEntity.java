package com.example.template.block.entity;

import com.example.template.config.ModConfig;
import com.example.template.fluid.FluidTank;
import com.example.template.fluid.ModFluids;
import com.example.template.fluid.MultiFluidTank;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 多流体废料管道方块实体：废料专用管道（网络家族与普通液体管道物理隔离）。
 * 内部多液体缓冲：仅接纳衰竭燃料，7 种废料可同时驻留/传输（MultiFluidTank），
 * 适合混合废料的多路线输送。破坏时若缓冲内含废料会触发衰竭区域（见方块 onRemove）。
 */
public class ChishiMultiFluidWastePipeBlockEntity extends ChishiFluidPipeBlockEntity {

    public ChishiMultiFluidWastePipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MULTI_FLUID_WASTE_PIPE.get(), pos, state);
    }

    @Override
    public boolean isWasteFamily() {
        return true;
    }

    /** 多流体：MultiFluidTank 缓冲，多种废料混驻，仅接纳衰竭燃料 */
    @Override
    protected FluidTank createBuffer() {
        return new MultiFluidTank(ModConfig.fluidPipeBufferCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                if (resource == null || !ModFluids.isExhaustedFuel(resource.getFluid())) {
                    return 0;
                }
                return super.fill(resource, simulate);
            }

            @Override
            protected void onChanged() {
                setChanged();
            }
        };
    }
}
