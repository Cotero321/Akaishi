package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiMultiFluidWastePipeBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 多流体废料管道：仅限传输衰竭燃料的专用管道（多种废料可各自直连输送）。
 * 与普通液体管道网络物理隔离，仅对接废品口/衰竭保存桶等废料设备。
 * 直连传输，管道不驻留液体，破坏时无泄漏。
 */
public class AkaishiMultiFluidWastePipeBlock extends AkaishiFluidPipeBlock {

    public AkaishiMultiFluidWastePipeBlock() {
        super();
    }

    @Override
    protected boolean isWastePipeBlock() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_MULTI_FLUID_WASTE_PIPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_MULTI_FLUID_WASTE_PIPE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_MULTI_FLUID_WASTE_PIPE.get(),
                AkaishiMultiFluidWastePipeBlockEntity::serverTick);
    }
}
