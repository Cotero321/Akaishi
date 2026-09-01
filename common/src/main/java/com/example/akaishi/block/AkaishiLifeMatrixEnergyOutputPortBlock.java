package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifeMatrixEnergyOutputPortBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
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
 * 生命转换矩阵能量输出口：生命能量输出端口（仅管道抽取，无手动界面）。
 * 结构成型后每 tick 从控制器拉取生命能量缓存，供生命能量管道/第三方物流抽取。
 */
public class AkaishiLifeMatrixEnergyOutputPortBlock extends AkaishiMachineBlock {

    public AkaishiLifeMatrixEnergyOutputPortBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT.get(),
                AkaishiLifeMatrixEnergyOutputPortBlockEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
