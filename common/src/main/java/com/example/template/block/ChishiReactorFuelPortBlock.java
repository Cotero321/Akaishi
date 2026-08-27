package com.example.template.block;

import com.example.template.block.entity.ChishiReactorFuelPortBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import com.example.template.item.ChishiFuelCellItem;
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
 * 燃料投放口：反应堆外壳上的燃料输入口（可多个，数据共享）。
 * 支持管道插入与手动投料：手持燃料罐右键投入缓冲槽，空手右键取出一罐（空罐）。
 * 缓冲槽中的燃料罐由投放口自动分配到控制器的空燃料槽。
 */
public class ChishiReactorFuelPortBlock extends BaseEntityBlock {

    public ChishiReactorFuelPortBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_REACTOR_FUEL_PORT.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_REACTOR_FUEL_PORT.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_REACTOR_FUEL_PORT.get(),
                ChishiReactorFuelPortBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        if (level.getBlockEntity(pos) instanceof ChishiReactorFuelPortBlockEntity port) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof ChishiFuelCellItem) {
                // 手持燃料罐 → 投入缓冲槽
                int inserted = port.insertCell(held);
                if (inserted > 0) {
                    held.shrink(inserted);
                }
                return InteractionResult.sidedSuccess(false);
            }
            // 空手 → 取出一罐（优先空罐，方便回收）
            ItemStack taken = port.takeCell();
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
