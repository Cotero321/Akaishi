package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiPurifierItemInputPortBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * 提纯矩阵物品输入口：提纯原料（粗制赤石块/赤石水晶块）输入端口（仅管道/漏斗，无手动界面）。
 * 结构成型后自动将缓冲槽内的原料分配到控制器输入槽。
 */
public class AkaishiPurifierItemInputPortBlock extends AkaishiMachineBlock {

    public AkaishiPurifierItemInputPortBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_PURIFIER_ITEM_INPUT.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_PURIFIER_ITEM_INPUT.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_PURIFIER_ITEM_INPUT.get(),
                AkaishiPurifierItemInputPortBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPurifierItemInputPortBlockEntity port) {
                Containers.dropContents(level, pos, port.buffer());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
