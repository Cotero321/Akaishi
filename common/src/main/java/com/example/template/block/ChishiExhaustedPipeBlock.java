package com.example.template.block;

import com.example.template.block.entity.ChishiExhaustedPipeBlockEntity;
import com.example.template.block.entity.ModBlockEntities;
import com.example.template.decay.DecayZoneManager;
import com.example.template.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 封闭性衰竭管道：仅限传输衰竭燃料的专用管道（单缓冲，一次一种废料）。
 * 与普通液体管道网络物理隔离，仅对接废品口/衰竭保存桶等废料设备。
 * 破坏时若缓冲内含废料，泄漏触发衰竭区域（等级按液量 1-3）。
 */
public class ChishiExhaustedPipeBlock extends ChishiFluidPipeBlock {

    public ChishiExhaustedPipeBlock() {
        super();
    }

    @Override
    protected boolean isWastePipeBlock() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_EXHAUSTED_PIPE.get(), ChishiExhaustedPipeBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 管道内残留废料被破坏 → 泄漏触发衰竭区域（液量占比越高，衰变等级越强 1-3 级）
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof ChishiExhaustedPipeBlockEntity pipe) {
            long amount = pipe.buffer().getAmount();
            if (amount > 0) {
                int amp = 1 + (int) (3 * amount / (double) pipe.buffer().getCapacity());
                DecayZoneManager.createZone(serverLevel, pos, Math.min(3, amp), false);
                // 泄漏警示音效
                level.playSound(null, pos, ModSounds.DECAY_LEAK.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
