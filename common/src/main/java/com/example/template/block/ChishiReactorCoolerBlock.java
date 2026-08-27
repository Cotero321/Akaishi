package com.example.template.block;

import com.example.template.block.entity.ChishiReactorCoolerBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import com.example.template.item.ChishiHeatSinkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
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
 * 散热组件：置于反应堆内腔，仅当与燃料棒组件相邻时生效。
 * 内插 1 片散热片，品质决定散热效率；反应堆运行期间散热片消耗耐久。
 * 手持散热片右键插入，空手右键取出。
 */
public class ChishiReactorCoolerBlock extends BaseEntityBlock {

    public ChishiReactorCoolerBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(4.0F, 5.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_REACTOR_COOLER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_REACTOR_COOLER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_REACTOR_COOLER.get(),
                ChishiReactorCoolerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (level.getBlockEntity(pos) instanceof ChishiReactorCoolerBlockEntity cooler) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof ChishiHeatSinkItem) {
                // 手持散热片 → 插入散热片槽
                if (cooler.insertHeatSink(held) > 0) {
                    held.shrink(1);
                }
                return InteractionResult.sidedSuccess(false);
            }
            // 空手 → 取出散热片
            ItemStack taken = cooler.takeHeatSink();
            if (!taken.isEmpty()) {
                if (held.isEmpty()) {
                    player.setItemInHand(hand, taken);
                } else {
                    player.getInventory().placeItemBackInInventory(taken);
                }
            }
        }
        return InteractionResult.sidedSuccess(false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
