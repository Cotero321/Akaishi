package com.example.akaishi.block.entity;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.block.CreativeEnergySourceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 创造模式能量源方块实体：无限能量输出（extract 始终成功），不可注入。
 * 不能直接实现 IEnergyStorage（其 getType() 与 BlockEntity.getType() 冲突），
 * 因此内部持有无限源存储对象，通过 IEnergyProvider 对外暴露。
 */
public class CreativeEnergySourceBlockEntity extends BlockEntity implements IEnergyProvider {

    /** 无限源存储（按方块类型决定能量类型） */
    private final InfiniteSourceStorage storage;

    public CreativeEnergySourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_ENERGY_SOURCE.get(), pos, state);
        IEnergyType type = ((CreativeEnergySourceBlock) state.getBlock()).energyType;
        this.storage = new InfiniteSourceStorage(type);
    }

    // ===== IEnergyProvider：纯源节点 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return storage;
    }

    @Override
    public boolean canOutputEnergy() {
        return true;
    }

    @Override
    public boolean canInputEnergy() {
        return false; // 只出不进，避免被管道反向注能
    }

    /**
     * 无限能量源存储：存量恒为最大、extract 永远成功、add 无效。
     */
    private static class InfiniteSourceStorage implements IEnergyStorage {

        private final IEnergyType type;

        InfiniteSourceStorage(IEnergyType type) {
            this.type = type;
        }

        @Override
        public IEnergyType getType() {
            return type;
        }

        @Override
        public long getEnergyStored() {
            return Long.MAX_VALUE;
        }

        @Override
        public long getMaxEnergy() {
            return Long.MAX_VALUE;
        }

        @Override
        public long addEnergy(long amount, boolean simulate) {
            return 0; // 不可注入
        }

        @Override
        public long extractEnergy(long amount, boolean simulate) {
            return amount; // 无限输出
        }
    }
}
