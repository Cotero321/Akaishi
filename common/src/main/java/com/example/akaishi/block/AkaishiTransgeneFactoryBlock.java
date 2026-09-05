package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiTransgeneFactoryBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
 * 转基因工厂：以基因序列片段为"蓝图"、搭配生物质材料与生命能量固态物，
 * 培育转基因作物（凋零藤等）。需符合配方的凋零骷髅基因（纯度≥50）。
 */
public class AkaishiTransgeneFactoryBlock extends AkaishiMachineBlock {

    public AkaishiTransgeneFactoryBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_TRANSGENE_FACTORY.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_TRANSGENE_FACTORY.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_TRANSGENE_FACTORY.get(),
                AkaishiTransgeneFactoryBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiTransgeneFactoryBlockEntity factory) {
                MenuRegistry.openExtendedMenu(serverPlayer, factory);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
