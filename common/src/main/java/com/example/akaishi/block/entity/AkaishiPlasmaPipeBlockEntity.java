package com.example.akaishi.block.entity;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.ModFluids;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 等离子体管道方块实体：复用液体管道网络逻辑，仅传输等离子体。
 * 缓冲罐拒收非等离子体液体；网络对接仅限等离子体专用罐（家族隔离）。
 */
public class AkaishiPlasmaPipeBlockEntity extends AkaishiFluidPipeBlockEntity {

    public AkaishiPlasmaPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PLASMA_PIPE.get(), pos, state);
    }

    @Override
    public boolean isPlasmaFamily() {
        return true;
    }

    @Override
    protected FluidTank createBuffer() {
        return new FluidTank(ModConfig.fluidPipeBufferCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                if (resource == null || !ModFluids.isPlasma(resource.getFluid())) {
                    return 0; // 等离子体管道仅承接等离子体
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
