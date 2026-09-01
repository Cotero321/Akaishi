package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiReactorControllerBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.decay.DecayZoneManager;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 反应堆控制器：反应堆多方块结构主方块，必须作为外壳的一部分放置。
 * 持有全部反应堆状态（燃料槽/温度/废品/NBT 持久化）。
 * 仅控制器可打开界面，且仅在结构成型（{@link #FORMED}）后允许打开。
 * 服务端 tick 驱动结构扫描与燃烧结算。
 */
public class AkaishiReactorControllerBlock extends AkaishiMachineBlock {

    /** 结构成型标记：成型时启用燃烧逻辑并切换外观 */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public AkaishiReactorControllerBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(6.0F, 8.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_REACTOR_CONTROLLER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_REACTOR_CONTROLLER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_REACTOR_CONTROLLER.get(),
                AkaishiReactorControllerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 仅控制器可开 GUI（外壳/燃料棒/核心等部件不可）；未成型时也可打开以查看成型状态
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AkaishiReactorControllerBlockEntity controller
                && player instanceof ServerPlayer serverPlayer) {
            MenuRegistry.openExtendedMenu(serverPlayer, controller);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 满热量时被挖掘 → 触发普通衰竭区域（燃料泄漏）；爆炸清空结构（exploded 已置位）不重复触发
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof AkaishiReactorControllerBlockEntity controller
                && controller.isAtFullHeat() && !controller.exploded()) {
            DecayZoneManager.createZone(serverLevel, pos, 0, false);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
