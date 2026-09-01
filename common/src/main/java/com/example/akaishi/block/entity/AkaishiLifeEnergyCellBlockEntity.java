package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.menu.AkaishiLifeConverterMenu;
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
 * 生命能量储存器方块实体：纯生命能量存储（双向缓冲，可充可放）。
 * 复用生命转换菜单/界面（数据槽 2/3 = 生命能量/容量），赤槽恒为 0。
 */
public class AkaishiLifeEnergyCellBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, IDataCarrier {

    /** 存储容量 */
    public static final long LIFE_CAPACITY = 1_000_000L;

    private final AkaishiEnergyStorage energy;
    /** 数据缓存：0/1=赤槽（恒 0，供菜单占位），2=生命能量，3=生命容量，4=结构状态（恒 0） */
    private final SimpleContainerData data;

    public AkaishiLifeEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_ENERGY_CELL.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.data = new SimpleContainerData(5);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeEnergyCellBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(2, (int) energy.getEnergyStored());
        data.set(3, (int) energy.getMaxEnergy());
    }

    public ContainerData data() {
        return data;
    }

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        // 只暴露生命能量存储（赤管道通过类型匹配自动跳过本方块）
        return type == LifeEnergyType.INSTANCE ? energy : null;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_energy_cell");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifeConverterMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("LifeEnergy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("LifeEnergy"));
    }
}
