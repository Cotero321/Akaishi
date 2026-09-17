package com.example.akaishi.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 多流体废料管道方块实体：废料专用管道（网络家族与普通液体管道物理隔离）。
 * 直连传输：仅接纳衰竭燃料，多种废料可各自直连输送（无内部缓冲），破坏时无泄漏。
 */
public class AkaishiMultiFluidWastePipeBlockEntity extends AkaishiFluidPipeBlockEntity {

    public AkaishiMultiFluidWastePipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MULTI_FLUID_WASTE_PIPE.get(), pos, state);
    }

    @Override
    public boolean isWasteFamily() {
        return true;
    }
}
