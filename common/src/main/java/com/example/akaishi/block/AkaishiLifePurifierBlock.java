package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiLifePurifierBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 生命能量提纯器：生命能量体系的固化设备。
 * 消耗赤能源（输入率 1M/t）驱动，每 1000 生命能量 + 10M 赤能源
 * 提纯固化出 1 个生命能量固态物（物品）。
 */
public class AkaishiLifePurifierBlock extends BaseEntityBlock {

    public AkaishiLifePurifierBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GREEN)
                .strength(5.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_PURIFIER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_PURIFIER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_PURIFIER.get(),
                AkaishiLifePurifierBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiLifePurifierBlockEntity purifier) {
                MenuRegistry.openExtendedMenu(serverPlayer, purifier);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
