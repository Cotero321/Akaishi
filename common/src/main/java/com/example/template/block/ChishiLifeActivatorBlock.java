package com.example.template.block;

import com.example.template.block.entity.ChishiLifeActivatorBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import com.example.template.config.ModConfig;
import com.example.template.decay.DecayZoneManager;
import com.example.template.sound.ModSounds;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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
 * 生命活化器：消耗生命能量缓慢无害化衰竭燃料。
 * 废料管道注入衰竭燃料（输入罐），普通液体管道抽取活化衰竭液体（输出罐）。
 * 破坏时若输入罐内残留衰竭燃料，触发衰竭区域（等级按液量占比 1-3）。
 */
public class ChishiLifeActivatorBlock extends ChishiMachineBlock {

    public ChishiLifeActivatorBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_GREEN)
                .strength(5.0F)
                .sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_ACTIVATOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_ACTIVATOR.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_ACTIVATOR.get(),
                ChishiLifeActivatorBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof ChishiLifeActivatorBlockEntity activator) {
                MenuRegistry.openExtendedMenu(serverPlayer, activator);
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
        // 输入罐内残留衰竭燃料被破坏 → 泄漏触发衰竭区域（暂存液量占比越高，衰变等级越强 1-3 级）
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof ChishiLifeActivatorBlockEntity activator) {
            long amount = activator.getWasteAmount();
            if (amount > 0) {
                int amp = 1 + (int) (3 * amount / (double) ModConfig.lifeActivatorInputCapacity);
                DecayZoneManager.createZone(serverLevel, pos, Math.min(3, amp), false);
                // 泄漏警示音效
                level.playSound(null, pos, ModSounds.DECAY_LEAK.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
