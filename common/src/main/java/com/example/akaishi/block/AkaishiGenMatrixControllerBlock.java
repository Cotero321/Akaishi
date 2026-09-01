package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiGenMatrixControllerBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
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
 * 发生器矩阵控制器：类反应堆式矩阵主方块（低级/高级共用，等级由方块实例决定）。
 * 结构成型（formed）后以对应倍率集中产能，右键打开控制界面。
 */
public class AkaishiGenMatrixControllerBlock extends AkaishiMachineBlock {

    /** 结构成型标记（由控制器写入） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    /** 矩阵等级（低级 3×3×3 / 高级 5×5×5） */
    private final AkaishiGenMatrixTier tier;

    public AkaishiGenMatrixControllerBlock(AkaishiGenMatrixTier tier) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(4.0F)
                .sound(SoundType.METAL));
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    public AkaishiGenMatrixTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_GEN_MATRIX_CONTROLLER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_GEN_MATRIX_CONTROLLER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_GEN_MATRIX_CONTROLLER.get(),
                AkaishiGenMatrixControllerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiGenMatrixControllerBlockEntity controller) {
                MenuRegistry.openExtendedMenu(serverPlayer, controller);
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
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiGenMatrixControllerBlockEntity controller) {
                Containers.dropContents(level, pos, controller.inventory());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
