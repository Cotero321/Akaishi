package com.example.akaishi.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 封闭性衰竭管道方块实体：废料专用管道（网络家族与普通液体管道物理隔离）。
 * 直连传输：仅接纳衰竭燃料，不留内部缓冲，破坏时无泄漏。
 */
public class AkaishiExhaustedPipeBlockEntity extends AkaishiFluidPipeBlockEntity {

    public AkaishiExhaustedPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get(), pos, state);
    }

    @Override
    public boolean isWasteFamily() {
        return true;
    }
}
