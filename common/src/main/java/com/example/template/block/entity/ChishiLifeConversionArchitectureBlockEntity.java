package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.energy.LifeEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命转换架构方块实体：已停用为纯材料（生命转换矩阵取代），
 * 结构恒不成型、不再提供界面，仅保留能量存储（NBT 持久化）供合成材料使用。
 */
public class ChishiLifeConversionArchitectureBlockEntity extends BlockEntity implements IEnergyProvider {

    /** 成型后每 tick 转换次数（保留常量供旧数据解读） */
    public static final int CONVERSIONS_PER_TICK = 45;
    /** 单次转换消耗的赤能源量 */
    public static final long CONVERSION_COST = ChishiLifeAggregationConverterBlockEntity.CONVERSION_COST;
    /** 单次转换产出的生命能量量 */
    public static final long CONVERSION_OUTPUT = ChishiLifeAggregationConverterBlockEntity.CONVERSION_OUTPUT;
    /** 中心赤能源缓冲容量 */
    public static final long CHISHI_CAPACITY = 500_000_000L;
    /** 中心生命能量存储容量 */
    public static final long LIFE_CAPACITY = 5000L;

    private final ChishiEnergyStorage chishi;
    private final ChishiEnergyStorage life;

    public ChishiLifeConversionArchitectureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_CONVERSION_ARCHITECTURE.get(), pos, state);
        this.chishi = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
    }

    /** 结构校验：恒不成型（新式转换矩阵由 {@link ChishiLifeMatrixControllerBlockEntity} 接管） */
    public boolean isStructureValid() {
        return false;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return chishi;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        if (type == ChishiEnergyType.INSTANCE) {
            return chishi;
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
        return type == ChishiEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("ChishiEnergy", chishi.getEnergyStored());
        tag.putLong("LifeEnergy", life.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        chishi.setEnergy(tag.getLong("ChishiEnergy"));
        life.setEnergy(tag.getLong("LifeEnergy"));
    }
}
