package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiExhaustedPipeBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 封闭性衰竭管道：仅限传输衰竭燃料的专用管道。
 * 与普通液体管道网络物理隔离，仅对接废品口/衰竭保存桶等废料设备。
 * 直连传输，管道不驻留液体，破坏时无泄漏。
 */
public class AkaishiExhaustedPipeBlock extends AkaishiFluidPipeBlock {

    public AkaishiExhaustedPipeBlock() {
        super();
    }

    @Override
    protected boolean isWastePipeBlock() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get(), AkaishiExhaustedPipeBlockEntity::serverTick);
    }
}
