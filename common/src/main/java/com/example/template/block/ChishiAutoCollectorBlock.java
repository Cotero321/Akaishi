package com.example.template.block;

import com.example.template.block.entity.ChishiAutoCollectorBlockEntity;
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
import net.minecraft.world.level.block.Block;
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
 * 自动收集器：每 tick 消耗赤能源，按等级效率自动收获范围内成熟的水晶簇
 * 并将其产物（赤石精华）存入内部 27 槽容器（箱子一样，支持漏斗 / AE2 / MEK 物流管道）。
 * 4 级（初/中/高/终）共用本方块类与方块实体，等级参数由构造器传入。
 */
public class ChishiAutoCollectorBlock extends BaseEntityBlock {

    /** 收集器等级：收集范围边长（与催化器分级一致）/ 每方块收集耗时 tick / 每 tick 耗能（2 的 1-4 次方） */
    public enum CollectorTier {
        BASIC(3, 30, 2),
        MEDIUM(5, 15, 4),
        ADVANCED(9, 7, 8),
        ULTIMATE(16, 2, 16);

        /** 作用范围边长（收集器为中心） */
        public final int range;
        /** 收集一个方块所需 tick（越低越快） */
        public final int workTicks;
        /** 每 tick 消耗赤能源 */
        public final int energyCost;

        CollectorTier(int range, int workTicks, int energyCost) {
            this.range = range;
            this.workTicks = workTicks;
            this.energyCost = energyCost;
        }
    }

    private final CollectorTier tier;

    /** 当前等级（供方块实体读取，等级由方块本身决定） */
    public CollectorTier tier() {
        return tier;
    }

    public ChishiAutoCollectorBlock(CollectorTier tier) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(3.5F)
                .sound(SoundType.METAL));
        this.tier = tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_AUTO_COLLECTOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHISHI_AUTO_COLLECTOR.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_AUTO_COLLECTOR.get(), ChishiAutoCollectorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiAutoCollectorBlockEntity collector) {
                MenuRegistry.openExtendedMenu(serverPlayer, collector);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChishiAutoCollectorBlockEntity collector) {
                Containers.dropContents(level, pos, collector.inventory());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
