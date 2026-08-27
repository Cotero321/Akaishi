package com.example.template.block;

import com.example.template.block.entity.ChishiEnergyLiquefierBlockEntity;
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
 * 能量液化装置：反应堆燃料体系的第一台机器。
 * 消耗赤能源（输入率 1M/t），将下界之星 / 凋零玫瑰液化成
 * 下界至纯能量 / 下界复合能量（液体），供能量加工器与液体管道使用。
 */
public class ChishiEnergyLiquefierBlock extends BaseEntityBlock {

    public ChishiEnergyLiquefierBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(5.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ENERGY_LIQUEFIER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ENERGY_LIQUEFIER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ENERGY_LIQUEFIER.get(),
                ChishiEnergyLiquefierBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof ChishiEnergyLiquefierBlockEntity liquefier) {
                MenuRegistry.openExtendedMenu(serverPlayer, liquefier);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
