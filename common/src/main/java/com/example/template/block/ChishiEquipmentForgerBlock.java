package com.example.template.block;

import com.example.template.block.entity.ChishiEquipmentForgerBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
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
 * 赤石装备打造器：消耗赤能源 + 赤石锭，将下界合金装备重铸为赤石装备（基础属性 1.25 倍，初始 4 槽）。
 */
public class ChishiEquipmentForgerBlock extends BaseEntityBlock {

    public ChishiEquipmentForgerBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_RED).strength(5.0F, 6.0F).sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_EQUIPMENT_FORGER.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_EQUIPMENT_FORGER.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_EQUIPMENT_FORGER.get(), ChishiEquipmentForgerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof ChishiEquipmentForgerBlockEntity be) {
                MenuRegistry.openExtendedMenu(serverPlayer, be);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
