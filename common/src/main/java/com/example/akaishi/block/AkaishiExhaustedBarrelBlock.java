package com.example.akaishi.block;

import com.example.akaishi.block.entity.AkaishiExhaustedBarrelBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.decay.DecayZoneManager;
import com.example.akaishi.sound.ModSounds;
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
 * 衰竭保存桶：仅容纳衰竭的生命燃料的专用储液桶（独立单方块，非多方块部件）。
 * 液体管道可从废品输出口抽取后注入本桶；右键打开液位界面。
 */
public class AkaishiExhaustedBarrelBlock extends AkaishiMachineBlock {

    public AkaishiExhaustedBarrelBlock() {
        super(Properties.of()
                .mapColor(MapColor.PLANT)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_EXHAUSTED_BARREL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_EXHAUSTED_BARREL.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_EXHAUSTED_BARREL.get(),
                AkaishiExhaustedBarrelBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof AkaishiExhaustedBarrelBlockEntity barrel) {
                MenuRegistry.openExtendedMenu(serverPlayer, barrel);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 桶内含有衰竭燃料时被破坏 → 触发衰竭区域（液体占比越高，衰变等级越强 1-3 级）
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof AkaishiExhaustedBarrelBlockEntity barrel) {
            long amount = barrel.tank().getAmount();
            if (amount > 0) {
                int amp = 1 + (int) (3 * amount / (double) barrel.tank().getCapacity());
                DecayZoneManager.createZone(serverLevel, pos, Math.min(3, amp), false);
                // 泄漏警示音效（就地播放，警示附近玩家）
                level.playSound(null, pos, ModSounds.DECAY_LEAK.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
