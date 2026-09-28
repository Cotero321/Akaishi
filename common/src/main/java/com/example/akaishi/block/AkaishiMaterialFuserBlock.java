package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiMaterialFuserBlockEntity;
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
 * 材料融合器方块：两种原料 + 赤能源 → 一件机械材料（单方块形态，无朝向属性）。
 * <p>与打粉机等单槽机器同一范式：破坏时掉出输入/输出槽物品，升级槽随 NBT 保留在掉落物里。
 */
public class AkaishiMaterialFuserBlock extends AkaishiMachineBlock {

    public AkaishiMaterialFuserBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_GRAY)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_MATERIAL_FUSER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHISHI_MATERIAL_FUSER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_MATERIAL_FUSER.get(),
                AkaishiMaterialFuserBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiMaterialFuserBlockEntity be) {
                MenuRegistry.openExtendedMenu(serverPlayer, be);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 破坏时掉出输入/输出槽物品（升级槽随 NBT 保留在方块掉落物中）
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AkaishiMaterialFuserBlockEntity be) {
            Containers.dropContents(level, pos, be.inventory());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
