package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiFusionEnergyOutputMenu;
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
 * 聚变能量输出口方块实体：聚变堆产出赤能源缓冲罐（容量 200 亿，纯发电）。
 * 控制器每 tick 灌入产出，能量管道从此抽取。右键打开能量查看界面。
 */
public class AkaishiFusionEnergyOutputBlockEntity extends BlockEntity implements IEnergyProvider, ExtendedMenuProvider, IDataCarrier {

    /** 缓冲容量：满配产出（约 6400 万/tick）下约 5 分钟 */
    public static final long BUFFER_CAPACITY = 20_000_000_000L;
    public static final int DATA_SLOTS = 4;

    private final AkaishiEnergyStorage energy;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    private BlockPos controllerPos;

    public AkaishiFusionEnergyOutputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUSION_ENERGY_OUTPUT.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, BUFFER_CAPACITY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFusionEnergyOutputBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (controllerPos != null && !(level.getBlockEntity(controllerPos) instanceof AkaishiFusionControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
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

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_fusion_energy_output");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiFusionEnergyOutputMenu(id, inv, data);
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
