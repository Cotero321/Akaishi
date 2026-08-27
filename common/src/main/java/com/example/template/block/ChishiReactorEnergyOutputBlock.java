package com.example.template.block;

import com.example.template.block.entity.ChishiReactorEnergyOutputBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * 能量输出口：反应堆外壳上的赤能源输出口（可多个，数据共享）。
 * 控制器每 tick 将产生的赤能源分发到全部能量输出口缓冲，管道可从中抽取。
 * 纯发电：不允许反向充能。
 */
public class ChishiReactorEnergyOutputBlock extends BaseEntityBlock {

    public ChishiReactorEnergyOutputBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_REACTOR_ENERGY_OUTPUT.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_REACTOR_ENERGY_OUTPUT.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_REACTOR_ENERGY_OUTPUT.get(),
                ChishiReactorEnergyOutputBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
