package com.example.akaishi.block;

import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.block.entity.AkaishiLifeEnergyPipeBlockEntity;
import com.example.akaishi.block.entity.ModBlockEntities;
import com.example.akaishi.energy.EnergyPipeTier;
import com.example.akaishi.energy.LifeEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 生命能量管道：与赤能源管道同构，但传输生命能量类型。
 * 与赤能源管道物理相邻时互不连通（能量类型隔离），设备按类型匹配接入。
 */
public class AkaishiLifeEnergyPipeBlock extends AkaishiEnergyPipeBlock {

    /** 生命能量管道每 tick 传输速率 */
    private static final int LIFE_PIPE_RATE = 1000;

    public AkaishiLifeEnergyPipeBlock() {
        super(EnergyPipeTier.BASIC);
    }

    @Override
    public IEnergyType getEnergyType() {
        return LifeEnergyType.INSTANCE;
    }

    @Override
    public int getTransferRate() {
        return LIFE_PIPE_RATE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.CHISHI_LIFE_ENERGY_PIPE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        if (type != ModBlockEntities.CHISHI_LIFE_ENERGY_PIPE.get()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.CHISHI_LIFE_ENERGY_PIPE.get(), AkaishiLifeEnergyPipeBlockEntity::serverTick);
    }
}
