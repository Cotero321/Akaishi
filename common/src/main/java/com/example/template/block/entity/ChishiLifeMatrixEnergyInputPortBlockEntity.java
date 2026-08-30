package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.block.ChishiLifeMatrixControllerBlock;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命转换矩阵能量输入口：赤能源输入缓冲（纯汇，仅管道供能，无手动界面）。
 * 结构成型后每 tick 将缓冲赤能源注入控制器，支持能量管道/第三方物流系统供能。
 */
public class ChishiLifeMatrixEnergyInputPortBlockEntity extends BlockEntity implements IEnergyProvider, IDataCarrier {

    /** 缓冲容量：100M，覆盖单次 10M 的转换消耗落差 */
    public static final long BUFFER_CAPACITY = 100_000_000L;

    private final ChishiEnergyStorage energy;
    private BlockPos controllerPos;

    public ChishiLifeMatrixEnergyInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_MATRIX_ENERGY_INPUT.get(), pos, state);
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, BUFFER_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiLifeMatrixEnergyInputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 控制器被拆/结构解散时清除缓存坐标，避免悬空引用
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof ChishiLifeMatrixControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        ChishiLifeMatrixControllerBlockEntity controller = at instanceof ChishiLifeMatrixControllerBlockEntity c ? c : null;
        if (controller == null || !controller.getBlockState().getValue(ChishiLifeMatrixControllerBlock.FORMED)) {
            return;
        }
        // 将缓冲赤能源注入控制器，每次最多转控制器剩余容量
        long stored = energy.getEnergyStored();
        if (stored <= 0) {
            return;
        }
        long free = controller.chishiStorage().getMaxEnergy() - controller.chishiStorage().getEnergyStored();
        long pushed = energy.extractEnergy(Math.min(stored, free), false);
        if (pushed > 0) {
            controller.chishiStorage().addEnergy(pushed, false);
            controller.setChanged();
            setChanged();
        }
    }

    public ChishiEnergyStorage energy() {
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
        return type == ChishiEnergyType.INSTANCE ? energy : null;
    }

    /** 纯汇：只允许注入赤能源 */
    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == ChishiEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return false;
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
