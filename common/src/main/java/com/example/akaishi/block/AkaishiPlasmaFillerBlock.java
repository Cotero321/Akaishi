package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiPlasmaFillerBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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
 * 离子体填装器：将等离子体灌入聚变反应棒，产出等离子体燃料棒（3 种）。
 * 无方向属性；等离子体由等离子体管道注入输入罐，产物从 GUI 取出，破坏时槽内物品掉落。
 */
public class AkaishiPlasmaFillerBlock extends AkaishiMachineBlock {

    public AkaishiPlasmaFillerBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(5.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_PLASMA_FILLER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_PLASMA_FILLER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_PLASMA_FILLER.get(),
                AkaishiPlasmaFillerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiPlasmaFillerBlockEntity filler) {
                MenuRegistry.openExtendedMenu(serverPlayer, filler);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AkaishiPlasmaFillerBlockEntity filler) {
            Containers.dropContents(level, pos, filler.rodsContainer());
            Containers.dropContents(level, pos, filler.outputContainer());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
