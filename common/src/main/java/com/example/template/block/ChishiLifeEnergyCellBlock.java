package com.example.template.block;

import com.example.template.block.entity.ChishiLifeEnergyCellBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 生命能量储存器：大容量生命能量存储方块，可被生命能量管道接入充放。
 * 单方块电池，无多方块结构。
 */
public class ChishiLifeEnergyCellBlock extends BaseEntityBlock {

    public ChishiLifeEnergyCellBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_ENERGY_CELL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_ENERGY_CELL.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_ENERGY_CELL.get(), ChishiLifeEnergyCellBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 右键打开生命能量储存器界面
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof ChishiLifeEnergyCellBlockEntity cell) {
                MenuRegistry.openExtendedMenu(serverPlayer, cell);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
