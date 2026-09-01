package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.block.AkaishiLifeMatrixControllerBlock;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命转换矩阵能量输出口：生命能量输出缓冲（纯发，仅管道抽取，无手动界面）。
 * 结构成型后每 tick 从控制器拉取生命能量缓存，供生命能量管道/第三方物流抽取。
 */
public class AkaishiLifeMatrixEnergyOutputPortBlockEntity extends BlockEntity implements IEnergyProvider, IDataCarrier {

    /** 缓冲容量：与控制器生命能量容量一致 */
    public static final long BUFFER_CAPACITY = 5000L;

    private final AkaishiEnergyStorage energy;
    private BlockPos controllerPos;

    public AkaishiLifeMatrixEnergyOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, BUFFER_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeMatrixEnergyOutputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 控制器被拆/结构解散时清除缓存坐标，避免悬空引用
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof AkaishiLifeMatrixControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        AkaishiLifeMatrixControllerBlockEntity controller = at instanceof AkaishiLifeMatrixControllerBlockEntity c ? c : null;
        if (controller == null || !controller.getBlockState().getValue(AkaishiLifeMatrixControllerBlock.FORMED)) {
            return;
        }
        // 从控制器拉取生命能量到缓冲，每 tick 按控制器当前速率（45 次 × 10 = 450）拉取
        long rate = AkaishiLifeMatrixControllerBlockEntity.CONVERSIONS_PER_TICK
                * AkaishiLifeMatrixControllerBlockEntity.CONVERSION_OUTPUT;
        long take = Math.min(rate, controller.lifeStorage().getEnergyStored());
        long free = energy.getMaxEnergy() - energy.getEnergyStored();
        long pulled = controller.lifeStorage().extractEnergy(Math.min(take, free), false);
        if (pulled > 0) {
            energy.addEnergy(pulled, false);
            controller.setChanged();
            setChanged();
        }
    }

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE ? energy : null;
    }

    /** 纯发：只允许抽取生命能量 */
    @Override
    public boolean canOutputEnergy() {
        return true;
    }

    @Override
    public boolean canInputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return false;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    /** 挖掘保留数据：排除旧控制器关联坐标，放置后重新扫描结构 */
    @Override
    public String[] excludedKeys() {
        return new String[]{"ControllerPos"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
    }
}
