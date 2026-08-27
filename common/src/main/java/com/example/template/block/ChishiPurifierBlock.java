package com.example.template.block;

import com.example.template.block.entity.ChishiPurifierBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 赤石提纯器：消耗赤石能量将粗制赤石块提纯为赤石精华。
 * 自带 GUI（能量条/进度条/燃料槽），服务端持有方块实体驱动逻辑。
 */
public class ChishiPurifierBlock extends BaseEntityBlock {

    public ChishiPurifierBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_PURIFIER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // 逻辑只在服务端运行，客户端仅负责渲染 GUI 显示数据
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_PURIFIER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_PURIFIER.get(), ChishiPurifierBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiPurifierBlockEntity purifier) {
                MenuRegistry.openExtendedMenu(serverPlayer, purifier);
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
        // 方块被替换/破坏时，将内部物品倒出，避免玩家物品丢失
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiPurifierBlockEntity purifier) {
                Containers.dropContents(level, pos, purifier.inventory());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
