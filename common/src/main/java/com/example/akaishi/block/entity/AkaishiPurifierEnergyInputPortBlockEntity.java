package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiPurifierMatrixControllerBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 提纯矩阵能量输入口：赤能源输入缓冲（纯汇，仅管道供能，无手动界面）。
 * 结构成型后每 tick 将缓冲能量注入控制器，支持能量管道/第三方物流系统供能。
 */
public class AkaishiPurifierEnergyInputPortBlockEntity extends BlockEntity implements IEnergyProvider, IDataCarrier {

    private final AkaishiEnergyStorage energy;
    private BlockPos controllerPos;

    public AkaishiPurifierEnergyInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PURIFIER_ENERGY_INPUT.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.purifierEnergyInputPortBufferCapacity);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPurifierEnergyInputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof AkaishiPurifierMatrixControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        AkaishiPurifierMatrixControllerBlockEntity controller = at instanceof AkaishiPurifierMatrixControllerBlockEntity c ? c : null;
        if (controller == null || !controller.getBlockState().getValue(AkaishiPurifierMatrixControllerBlock.FORMED)) {
            return;
        }
        // 将缓冲能量注入控制器，每次最多转控制器剩余容量
        long stored = energy.getEnergyStored();
        if (stored <= 0) {
            return;
        }
        long free = controller.energy().getMaxEnergy() - controller.energy().getEnergyStored();
        long pushed = energy.extractEnergy(Math.min(stored, free), false);
        if (pushed > 0) {
            controller.energy().addEnergy(pushed, false);
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

    /** 纯汇：只允许注入 */
    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
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
