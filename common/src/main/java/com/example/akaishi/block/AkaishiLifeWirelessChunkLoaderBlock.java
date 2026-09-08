package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifeWirelessChunkLoaderBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/** 生命无线终端区块加载构架。 */
public class AkaishiLifeWirelessChunkLoaderBlock extends Block implements EntityBlock {

    public AkaishiLifeWirelessChunkLoaderBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_WIRELESS_CHUNK_LOADER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHISHI_LIFE_WIRELESS_CHUNK_LOADER.get()) {
            return null;
        }
        return (currentLevel, pos, currentState, blockEntity) ->
                AkaishiLifeWirelessChunkLoaderBlockEntity.serverTick(currentLevel, pos, currentState,
                        (AkaishiLifeWirelessChunkLoaderBlockEntity) blockEntity);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof AkaishiLifeWirelessChunkLoaderBlockEntity blockEntity) {
            blockEntity.releaseChunkLoad();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
