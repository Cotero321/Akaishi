package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.IMinerPortDevice;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiMinerControllerBlock;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiMinerEnergyInputMenu;
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
 * 矿机能量输入口方块实体：安装在矿机边立柱上的赤能源输入设备。
 * 能量管道把赤能源充入本地缓冲（10M），结构成型后每 tick 限量转发给控制器；
 * 无物品槽，破坏方块时能量随挖掘数据保留（IDataCarrier）。
 */
public class AkaishiMinerEnergyInputBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IMinerPortDevice, IEnergyProvider, IDataCarrier {

    /** 能量缓冲容量（与矿机转口一致） */
    public static final long BUFFER_CAPACITY = 10_000_000L;

    public static final int DATA_ENERGY = 0, DATA_CAPACITY = 1, DATA_FORMED = 2;

    private final AkaishiEnergyStorage energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, BUFFER_CAPACITY);
    private final SimpleContainerData data = new SimpleContainerData(3);
    private BlockPos controllerPos;

    public AkaishiMinerEnergyInputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINER_ENERGY_INPUT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiMinerEnergyInputBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_CAPACITY, (int) energy.getMaxEnergy());
        // 控制器被拆/结构解散时清除关联坐标，避免悬空引用
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof AkaishiMinerControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        AkaishiMinerControllerBlockEntity controller = at instanceof AkaishiMinerControllerBlockEntity c ? c : null;
        boolean formed = controller != null
                && controller.getBlockState().getValue(AkaishiMinerControllerBlock.FORMED);
        data.set(DATA_FORMED, formed ? 1 : 0);
        if (!formed) {
            return;
        }
        // 能量缓冲 → 控制器（每 tick 满速转发，仅受控制器剩余容量限制）
        long free = controller.getEnergyCapacity() - controller.getEnergyStored();
        long pushed = energy.extractEnergy(free, false);
        if (pushed > 0) {
            controller.addEnergy(pushed);
            controller.setChanged();
            setChanged();
        }
    }

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    // ===== IEnergyProvider：仅输入（能量管道注入赤能源），不可向外输出 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_miner_energy_input");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiMinerEnergyInputMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    /** 挖掘保留数据：排除旧控制器关联坐标（能量由 BlockEntityTag 恢复） */
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
