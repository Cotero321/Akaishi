package com.example.akaishi.block.entity;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.LifeEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命聚合转换器方块实体：已停用为纯材料（生命转换矩阵取代），
 * 不再提供界面、不再独立转换能量，仅保留能量存储（NBT 持久化）供合成材料使用。
 */
public class AkaishiLifeAggregationConverterBlockEntity extends BlockEntity implements IEnergyProvider {

    /** 单次聚合消耗的赤能源量（保留常量供旧数据解读） */
    public static final long CONVERSION_COST = 10_000_000L;
    /** 单次聚合产出的生命能量量 */
    public static final long CONVERSION_OUTPUT = 10L;
    /** 自身赤能源缓冲容量 */
    public static final long CHISHI_CAPACITY = 100_000_000L;
    /** 自身生命能量缓冲容量 */
    public static final long LIFE_CAPACITY = 100L;

    private final AkaishiEnergyStorage akaishi;
    private final AkaishiEnergyStorage life;

    public AkaishiLifeAggregationConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_AGGREGATION_CONVERTER.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return akaishi;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        if (type == AkaishiEnergyType.INSTANCE) {
            return akaishi;
        }
        if (type == LifeEnergyType.INSTANCE) {
            return life;
        }
        return null;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        // 赤能源只进（原料），生命能量只出不进
        return type == AkaishiEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("AkaishiEnergy", akaishi.getEnergyStored());
        tag.putLong("LifeEnergy", life.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        akaishi.setEnergy(tag.getLong("AkaishiEnergy"));
        life.setEnergy(tag.getLong("LifeEnergy"));
    }
}
