package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiAdvancedPurifierBlockEntity;
import com.example.akaishi.block.entity.AkaishiPurifierBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 赤石提纯器：消耗赤石能量将粗制赤石块提纯为赤石精华。
 * 自带 GUI（能量条/进度条/燃料槽），服务端持有方块实体驱动逻辑。
 * 被 26 个高级提纯构建方块环绕时（formed=true）作为"提纯矩阵"中心，以 30 倍效率集中提纯。
 */
public class AkaishiPurifierBlock extends AkaishiMachineBlock {

    /** 是否为提纯矩阵中心（成型标记，由自身结构校验写入） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public AkaishiPurifierBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(3.5F)
                .sound(SoundType.METAL));
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
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
        return createTickerHelper(type, ModBlockEntities.CHISHI_PURIFIER.get(), AkaishiPurifierBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiPurifierBlockEntity purifier) {
                MenuRegistry.openExtendedMenu(serverPlayer, purifier);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide) {
            // 中心方块放置：通知周围外壳重新检测成型状态（自身结构校验由首次 tick 完成）
            AkaishiAdvancedPurifierBlockEntity.notifyNearbyShells(level, pos);
        }
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
            if (be instanceof AkaishiPurifierBlockEntity purifier) {
                Containers.dropContents(level, pos, purifier.inventory());
            }
            // 中心方块被移除：周围外壳不再成型，通知其重新检测
            if (!level.isClientSide) {
                AkaishiAdvancedPurifierBlockEntity.notifyNearbyShells(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
