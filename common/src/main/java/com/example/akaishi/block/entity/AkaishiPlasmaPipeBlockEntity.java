package com.example.akaishi.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 等离子体管道方块实体：复用液体管道网络逻辑，仅传输等离子体。
 * 直连传输：不承接非等离子体液体，网络对接仅限等离子体专用罐（家族隔离）。
 */
public class AkaishiPlasmaPipeBlockEntity extends AkaishiFluidPipeBlockEntity {

    public AkaishiPlasmaPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PLASMA_PIPE.get(), pos, state);
    }

    @Override
    public boolean isPlasmaFamily() {
        return true;
    }
}
