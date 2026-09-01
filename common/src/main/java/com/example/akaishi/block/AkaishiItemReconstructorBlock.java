package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiItemReconstructorBlockEntity;
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
 * 物品重构仪：以衰竭结晶为代价嬗变物品（骷髅头/金苹果/紫水晶等升级为高阶物品）。
 * 无方向属性；3 物品槽（原料/结晶/产物），破坏时全部掉落。
 */
public class AkaishiItemReconstructorBlock extends AkaishiMachineBlock {

    public AkaishiItemReconstructorBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_ITEM_RECONSTRUCTOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_ITEM_RECONSTRUCTOR.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_ITEM_RECONSTRUCTOR.get(),
                AkaishiItemReconstructorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiItemReconstructorBlockEntity reconstructor) {
                MenuRegistry.openExtendedMenu(serverPlayer, reconstructor);
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AkaishiItemReconstructorBlockEntity reconstructor) {
            // 槽位物品（原料/结晶/产物）全部掉落
            Containers.dropContents(level, pos, reconstructor.inventory());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
