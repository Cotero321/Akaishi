package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiPlantCultivatorBlockEntity;
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
 * 赤石植物培养机方块：消耗赤能源培养植物（种子保留，产出成熟作物）。
 */
public class AkaishiPlantCultivatorBlock extends AkaishiMachineBlock {

    public AkaishiPlantCultivatorBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_GREEN).strength(5.0F).sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_PLANT_CULTIVATOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CHISHI_PLANT_CULTIVATOR.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_PLANT_CULTIVATOR.get(),
                AkaishiPlantCultivatorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiPlantCultivatorBlockEntity be) {
                MenuRegistry.openExtendedMenu(serverPlayer, be);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 破坏时掉出输入/输出槽物品（升级槽随 NBT 保留在方块掉落物中）
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AkaishiPlantCultivatorBlockEntity be) {
            Containers.dropContents(level, pos, be.inventory());
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
