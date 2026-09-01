package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.decay.DecayZoneManager;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiDecayPurifierMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 衰变净化塔方块实体：每 tick 消耗赤能源，削减范围内同维度衰竭区域的剩余时间。
 * <p>
 * 升级加成：速度升级提升净化速度（每级 +12.5%），能量升级扩容缓冲（每级 +50%）。
 * 净化速度为浮点累加（speedAccum），避免 (int) 截断导致 1~7 级速度升级无效。
 * 无区域在范围内时待机不耗能。
 * GUI 数据槽：0=能量 1=容量 2=净化中标志 3=范围内区域数。
 */
public class AkaishiDecayPurifierBlockEntity extends BlockEntity
        implements IUpgradeableMachine, IEnergyProvider, ExtendedMenuProvider, IDataCarrier {

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_WORKING = 2;
    public static final int DATA_ZONE_COUNT = 3;
    public static final int DATA_SLOTS = 4;

    private final AkaishiEnergyStorage energy;
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    /** 净化量浮点余量累加器（防速度倍率截断精度丢失） */
    private float speedAccum;
    private boolean working;

    public AkaishiDecayPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_DECAY_PURIFIER.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.decayPurifierEnergyCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiDecayPurifierBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // 能量升级动态扩容（容量倍率变化实时生效）
        energy.setMaxEnergy((long) (ModConfig.decayPurifierEnergyCapacity * getEnergyCapacityMultiplier()));
        int zoneCount = DecayZoneManager.countZonesInRange(serverLevel, worldPosition, ModConfig.decayPurifierRange);
        long stored = energy.getEnergyStored();
        long max = energy.getMaxEnergy();

        // 待机：无区域可净化或能量不足
        if (zoneCount <= 0 || stored < ModConfig.decayPurifierCostPerTick) {
            working = false;
            speedAccum = 0;
            syncData(zoneCount);
            return;
        }
        working = true;
        energy.extractEnergy(ModConfig.decayPurifierCostPerTick, false);
        // 净化速度 = 基础 × 速度倍率；余量累加防截断，避免 1~7 级速度升级全部无效
        speedAccum += ModConfig.decayPurifierTicksPerTick * getSpeedMultiplier();
        long ticks = (long) speedAccum;
        if (ticks >= 1) {
            speedAccum -= ticks;
            DecayZoneManager.purifyZones(serverLevel, worldPosition, ModConfig.decayPurifierRange, ticks);
        }
        syncData(zoneCount);
        setChanged();
    }

    private void syncData(int zoneCount) {
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_CAPACITY, (int) energy.getMaxEnergy());
        data.set(DATA_WORKING, working ? 1 : 0);
        data.set(DATA_ZONE_COUNT, zoneCount);
    }

    // ===== 能量（仅赤石能量，纯消耗型） =====

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

    // ===== 机器升级 =====

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }

    public ContainerData data() {
        return data;
    }

    // ===== 菜单 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_decay_purifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiDecayPurifierMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== NBT 持久化 =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
    }
}
