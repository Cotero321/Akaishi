package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 单输入单输出处理机器抽象基类（赤石植物培养机/压缩机/打粉机/变化器共用）。
 * 统一实现：配方表（物品→物品）、进度推进（速度升级倍率，浮点余量防截断）、
 * 每 tick 能量消耗、升级槽、数据槽同步与 NBT 持久化。
 * 子类仅需提供配方表、能量容量/耗时/能耗与菜单构造。
 */
public abstract class AkaishiSingleSlotMachineBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IDataCarrier, IUpgradeableMachine {

    /** 加工配方：输入物品 → 输出物品（inputCount 消耗量、outputCount 产量） */
    public record MachineRecipe(Item input, int inputCount, Item output, int outputCount) {
    }

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    // ===== 数据槽（Menu 同步）=====
    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_REQUIRED = 3;
    public static final int DATA_SLOTS = 4;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage energy;
    protected final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 当前配方对应输入物品（更换时重置进度，防跨配方错配） */
    private Item currentInput;
    protected int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;

    protected AkaishiSingleSlotMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, baseCapacity());
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiSingleSlotMachineBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    /** 配方表：输入物品 → 配方（子类硬编码） */
    protected abstract Map<Item, MachineRecipe> recipes();

    /** 能量缓冲基础容量（能量升级按倍率扩容） */
    protected abstract long baseCapacity();

    /** 单次加工基础耗时（tick，速度升级缩短） */
    protected abstract int ticks();

    /** 每 tick 消耗的赤能源 */
    protected abstract long energyPerTick();

    /** 加工是否消耗输入物品（植物培养机种子保留 → 覆写 false） */
    protected boolean consumesInput() {
        return true;
    }

    /** 子类创建具体菜单（供 createMenu 委托） */
    protected abstract AbstractContainerMenu createMenuInstance(int id, Inventory inv);

    /** 服务端 tick 入口（由子类 Block 的 getTicker 绑定） */
    protected void tickServer() {
        // 能量升级动态扩容（倍率变化实时生效，容量缩小时自动夹取）
        energy.setMaxEnergy((long) (baseCapacity() * getEnergyCapacityMultiplier()));
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_CAPACITY, (int) energy.getMaxEnergy());

        ItemStack inputStack = inventory.getItem(SLOT_INPUT);
        Item input = inputStack.getItem();
        MachineRecipe recipe = recipes().get(input);
        // 更换原料 → 重置进度（防止跨配方错配）
        if (currentInput == null) {
            currentInput = input;
        } else if (input != currentInput) {
            progress = 0;
            speedAccum = 0;
            currentInput = input;
        }
        data.set(DATA_REQUIRED, recipe == null ? 0 : ticks());
        data.set(DATA_PROGRESS, progress);

        // 无配方 / 输入不足 / 输出不可容纳 / 能量不足 → 待机
        int have = consumesInput() ? inputStack.getCount() : 1;
        if (recipe == null || inputStack.isEmpty() || have < recipe.inputCount()
                || !canFitOutput(recipe.output()) || energy.getEnergyStored() < energyPerTick()) {
            return;
        }
        // 推进：每 tick 扣能量，进度按速度倍率累加（小数余量防截断）
        energy.extractEnergy(energyPerTick(), false);
        speedAccum += getSpeedMultiplier();
        int delta = (int) speedAccum;
        if (delta > 0) {
            speedAccum -= delta;
            progress += delta;
        }
        if (progress >= ticks()) {
            progress = 0;
            speedAccum = 0;
            if (consumesInput()) {
                inputStack.shrink(recipe.inputCount());
            }
            addOutput(recipe.output(), recipe.outputCount());
        }
        setChanged();
    }

    private boolean canFitOutput(Item output) {
        ItemStack cur = inventory.getItem(SLOT_OUTPUT);
        return cur.isEmpty() || (cur.is(output) && cur.getCount() < cur.getMaxStackSize());
    }

    private void addOutput(Item output, int count) {
        ItemStack cur = inventory.getItem(SLOT_OUTPUT);
        if (cur.isEmpty()) {
            inventory.setItem(SLOT_OUTPUT, new ItemStack(output, count));
        } else if (cur.is(output)) {
            cur.grow(count);
            inventory.setItem(SLOT_OUTPUT, cur);
        }
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block." + nameKey());
    }

    /** 方块翻译 key（block.akaishi.<id>） */
    protected abstract String nameKey();

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return createMenuInstance(id, inv);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== IEnergyProvider：只接收赤能源（纯消耗型）=====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == AkaishiEnergyType.INSTANCE;
    }

    // ===== IDataCarrier：物品随方块掉落，其余 NBT（能量/进度/升级）保留在掉落物 =====

    @Override
    public String[] excludedKeys() {
        return new String[]{"Inventory"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.put("Inventory", inventory.createTag());
        tag.putInt("Progress", progress);
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        inventory.fromTag(tag.getList("Inventory", 10));
        progress = tag.getInt("Progress");
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        // 重载校验：无配方或进度超上限（跨配方错配）→ 清零
        MachineRecipe recipe = recipes().get(inventory.getItem(SLOT_INPUT).getItem());
        if (recipe == null || progress >= ticks()) {
            progress = 0;
            speedAccum = 0;
        }
        currentInput = null;
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
