package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.block.ChishiGenMatrixControllerBlock;
import com.example.template.block.ChishiGenMatrixTier;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 发生器矩阵能量输出口：赤能源输出缓冲（纯发电，仅管道抽取，无手动界面）。
 * 结构成型后每 tick 从控制器拉取产出能量缓存，能量管道/第三方物流从此抽取。
 */
public class ChishiGenEnergyOutputPortBlockEntity extends BlockEntity implements IEnergyProvider, IDataCarrier {

    /** 缓冲容量：固定 100M，覆盖低级（5M）/高级（50M）两档产出缓存 */
    public static final long BUFFER_CAPACITY = 100_000_000L;

    private final ChishiEnergyStorage energy;
    private BlockPos controllerPos;

    public ChishiGenEnergyOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_GEN_ENERGY_OUTPUT.get(), pos, state);
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, BUFFER_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiGenEnergyOutputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 控制器被拆/结构解散时清除缓存坐标，避免悬空引用
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof ChishiGenMatrixControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        ChishiGenMatrixControllerBlockEntity controller = at instanceof ChishiGenMatrixControllerBlockEntity c ? c : null;
        if (controller == null || !controller.getBlockState().getValue(ChishiGenMatrixControllerBlock.FORMED)) {
            return;
        }
        // 每 tick 按当前实际产出速率拉取（含升级倍率），避免一次性清空控制器能量
        long rate = controller.tier().generateRate
                * (long) Math.ceil(ChishiGenMatrixControllerBlockEntity.getBoostMultiplier(controller.getUpgradeCount()));
        long take = Math.min(rate, controller.energy().getEnergyStored());
        long free = energy.getMaxEnergy() - energy.getEnergyStored();
        long pulled = controller.energy().extractEnergy(Math.min(take, free), false);
        if (pulled > 0) {
            energy.addEnergy(pulled, false);
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

    /** 纯发电：只允许抽取 */
    @Override
    public boolean canOutputEnergy() {
        return true;
    }

    @Override
    public boolean canInputEnergy() {
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
