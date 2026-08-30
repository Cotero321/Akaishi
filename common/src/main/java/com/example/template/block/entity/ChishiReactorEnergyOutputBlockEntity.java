package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.menu.ChishiReactorEnergyOutputMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 能量输出口方块实体：反应堆产出的赤能源缓冲罐（纯发电，管道只能抽取）。
 * 控制器每 tick 将赤能源灌入本罐，液体管道/能量网络从此抽取。
 * 右键打开能量查看界面（能量/容量经 4 个 int 数据槽同步）。
 */
public class ChishiReactorEnergyOutputBlockEntity extends BlockEntity implements IEnergyProvider, ExtendedMenuProvider, IDataCarrier {

    /** 缓冲容量：可容纳长时间满负荷产出（满产 100M/tick 下约 50 秒） */
    public static final long BUFFER_CAPACITY = 5_000_000_000L;
    /** 数据槽：0/1=能量低/高位，2/3=容量低/高位（long 拆分同步） */
    public static final int DATA_SLOTS = 4;

    private final ChishiEnergyStorage energy;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
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
        // 同步能量/容量到 GUI（long 拆 4 个 int 槽，Menu 侧重组）
        long stored = energy.getEnergyStored();
        long max = energy.getMaxEnergy();
        data.set(0, (int) stored);
        data.set(1, (int) (stored >>> 32));
        data.set(2, (int) max);
        data.set(3, (int) (max >>> 32));
    }

    public ContainerData data() {
        return data;
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

    // ===== ExtendedMenuProvider：右键打开能量查看界面 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_reactor_energy_output");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiReactorEnergyOutputMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
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
