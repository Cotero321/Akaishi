package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiPlasmaPipeBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 等离子体管道：第三传输家族管道（与普通/废料液体管道物理隔离）。
 * 复用液体管道网络逻辑，仅传输 3 种等离子体（buffer 罐级过滤 + 家族隔离）。
 */
public class AkaishiPlasmaPipeBlock extends AkaishiFluidPipeBlock {

    @Override
    protected boolean isPlasmaPipeBlock() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_PLASMA_PIPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_PLASMA_PIPE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_PLASMA_PIPE.get(), AkaishiPlasmaPipeBlockEntity::serverTick);
    }
}
