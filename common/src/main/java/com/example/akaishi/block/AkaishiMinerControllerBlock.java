package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiMinerControllerBlockEntity;
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
 * 赤石矿机控制器（核心方块，4 级共用，等级由方块实例决定）。
 * 需按固定 9×9×5 布局搭建结构：五层共用十字框架布局，控制器位于中间层中心，
 * 底部中心柱为钻机钻头、顶部中心柱为矿机转口，四根边立柱可由设备/架构件混搭。
 * 结构成型（formed）后消耗赤能源持续挖矿，产物经转口/物品输出口输出，升级件放在升级框架中。
 */
public class AkaishiMinerControllerBlock extends AkaishiMachineBlock {

    /** 结构成型标记（由控制器写入） */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    /** 矿机等级（基础/进阶/高级/终极） */
    private final AkaishiMinerTier tier;

    public AkaishiMinerControllerBlock(AkaishiMinerTier tier) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_RED)
                .strength(4.0F)
                .sound(SoundType.METAL));
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    public AkaishiMinerTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_MINER_CONTROLLER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_MINER_CONTROLLER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_MINER_CONTROLLER.get(),
                AkaishiMinerControllerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AkaishiMinerControllerBlockEntity controller) {
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
            if (be instanceof AkaishiMinerControllerBlockEntity controller) {
                Containers.dropContents(level, pos, controller.inventory());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
