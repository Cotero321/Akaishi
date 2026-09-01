package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiFusionFuelAggregatorBlockEntity;
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
 * 聚变燃料聚合器：活化成分 → 等离子体（3 种）。
 * 无方向属性；输入槽仅接纳活化成分，等离子体由等离子体管道从输出罐抽取，破坏时输入槽物品掉落。
 */
public class AkaishiFusionFuelAggregatorBlock extends AkaishiMachineBlock {

    public AkaishiFusionFuelAggregatorBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(5.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_FUSION_FUEL_AGGREGATOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_FUSION_FUEL_AGGREGATOR.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_FUSION_FUEL_AGGREGATOR.get(),
                AkaishiFusionFuelAggregatorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiFusionFuelAggregatorBlockEntity aggregator) {
                MenuRegistry.openExtendedMenu(serverPlayer, aggregator);
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
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AkaishiFusionFuelAggregatorBlockEntity aggregator) {
            Containers.dropContents(level, pos, aggregator.inputContainer());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
