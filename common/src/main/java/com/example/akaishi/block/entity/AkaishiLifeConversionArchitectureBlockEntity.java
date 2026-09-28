package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.LifeEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命转换架构方块实体：已停用为纯材料（生命转换矩阵取代），
 * 结构恒不成型、不再提供界面，仅保留能量存储（NBT 持久化）供合成材料使用。
 */
public class AkaishiLifeConversionArchitectureBlockEntity extends BlockEntity
        implements IEnergyProvider, IDataCarrier {

    private final AkaishiEnergyStorage akaishi;
    private final AkaishiEnergyStorage life;

    public AkaishiLifeConversionArchitectureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_CONVERSION_ARCHITECTURE.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.lifeConversionChishiCapacity);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, ModConfig.lifeConversionLifeCapacity);
    }

    /** 结构校验：恒不成型（新式转换矩阵由 {@link AkaishiLifeMatrixControllerBlockEntity} 接管）。
     *  本实体未注册 ticker、无 tickServer，故不存在每 tick 空跑，无需早退。 */
    public boolean isStructureValid() {
        return false;
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

    @Override
    public String[] excludedKeys() {
        // 方块 onRemove 不单独掉落任何容器（已停用为纯材料，仅能量数据），故无需排除
        return new String[0];
    }
}
