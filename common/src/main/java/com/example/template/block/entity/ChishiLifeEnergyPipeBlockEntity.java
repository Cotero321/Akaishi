package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyType;
import com.example.template.energy.LifeEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命能量管道方块实体：复用赤能源管道网络逻辑，仅覆盖能量类型为生命能量。
 */
public class ChishiLifeEnergyPipeBlockEntity extends ChishiEnergyPipeBlockEntity {

    public ChishiLifeEnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_ENERGY_PIPE.get(), pos, state);
    }

    @Override
    public IEnergyType getEnergyType() {
        return LifeEnergyType.INSTANCE;
    }
}
