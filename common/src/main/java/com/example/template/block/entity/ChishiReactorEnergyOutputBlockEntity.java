package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量输出口方块实体：反应堆产出的赤能源缓冲罐（纯发电，管道只能抽取）。
 * 控制器每 tick 将赤能源灌入本罐，液体管道/能量网络从此抽取。
 */
public class ChishiReactorEnergyOutputBlockEntity extends BlockEntity implements IEnergyProvider {

    /** 缓冲容量：可容纳数秒满负荷产出 */
    public static final long BUFFER_CAPACITY = 50_000_000L;

    private final ChishiEnergyStorage energy;
    private BlockPos controllerPos;

    public ChishiReactorEnergyOutputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_REACTOR_ENERGY_OUTPUT.get(), pos, state);
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, BUFFER_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiReactorEnergyOutputBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 控制器被拆时清除缓存坐标，避免悬空引用
        if (controllerPos != null && !(level.getBlockEntity(controllerPos) instanceof ChishiReactorControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
    }

    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    /** 控制器灌入产出能量 */
    public void receiveEnergy(long amount) {
        energy.addEnergy(amount, false);
    }

    public ChishiEnergyStorage energy() {
        return energy;
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
