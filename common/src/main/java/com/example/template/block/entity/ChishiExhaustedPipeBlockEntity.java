package com.example.template.block.entity;

import com.example.template.config.ModConfig;
import com.example.template.fluid.FluidTank;
import com.example.template.fluid.ModFluids;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 封闭性衰竭管道方块实体：废料专用管道（网络家族与普通液体管道物理隔离）。
 * 内部单液体缓冲：仅接纳衰竭燃料，同一时刻只能驻留一种废料（换种需先抽空），
 * 适合单一路线废料输送。破坏时若缓冲内含废料会触发衰竭区域（见方块 onRemove）。
 */
public class ChishiExhaustedPipeBlockEntity extends ChishiFluidPipeBlockEntity {

    public ChishiExhaustedPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get(), pos, state);
    }

    @Override
    public boolean isWasteFamily() {
        return true;
    }

    /** 封闭性：单液体缓冲，仅接纳衰竭燃料 */
    @Override
    protected FluidTank createBuffer() {
        return new FluidTank(ModConfig.fluidPipeBufferCapacity) {
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
