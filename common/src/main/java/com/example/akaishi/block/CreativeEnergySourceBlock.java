package com.example.akaishi.block;

import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.block.entity.CreativeEnergySourceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 创造模式能量源方块：放置后即为无限能量源，供测试能量网络使用。
 * 通过构造参数区分能量类型（赤能源 / 生命能量），方块实体按方块类型返回对应存储。
 */
public class CreativeEnergySourceBlock extends Block implements EntityBlock {

    /** 本方块提供的能量类型 */
    public final IEnergyType energyType;

    public CreativeEnergySourceBlock(IEnergyType energyType) {
        super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                .lightLevel(state -> 10));
        this.energyType = energyType;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEnergySourceBlockEntity(pos, state);
    }
}
