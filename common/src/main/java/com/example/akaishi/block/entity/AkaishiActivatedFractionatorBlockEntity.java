package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiActivatedFractionatorMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 活化分馏器方块实体（仅服务端驱动逻辑）。
 * 将活化结晶深度拆分：1 个活化结晶 → 1 个对应活化成分（主）+ 1 个衰竭结晶（副）。
 * 每次加工耗时 {@link ModConfig#fractionatorProcessTicks} tick、消耗赤能源一次结清；
 * 输入仅接纳 7 种活化结晶，输出槽只读（防止杂物卡死机器）。
 * 换料/取空输入槽会清零进度，防止跨配方错配白嫖半程进度。
 */
public class AkaishiActivatedFractionatorBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IDataCarrier, IUpgradeableMachine {

    // ===== 数据槽 =====
    public static final int DATA_SLOTS = 3;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;

    private final SimpleContainerData data;
    private final AkaishiEnergyStorage energy;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 输入槽（0=活化结晶，7 种任一） */
    private final SimpleContainer input;
    /** 输出槽（0=活化成分，1=衰竭结晶） */
    private final SimpleContainer output = new SimpleContainer(2);
    /** 当前加工进度（tick，满 {@link ModConfig#fractionatorProcessTicks} 结算一次） */
    private int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;
    /** 当前输入的活化结晶种类（跨配方错配防御：换料清零进度） */
    private Item currentInput;

    public AkaishiActivatedFractionatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ACTIVATED_FRACTIONATOR.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.fractionatorEnergyCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.input = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiActivatedFractionatorBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiActivatedFractionatorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容能量缓冲（倍率变化时自动夹取）
        energy.setMaxEnergy((long) (ModConfig.fractionatorEnergyCapacity * getEnergyCapacityMultiplier()));
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_ENERGY_CAPACITY, (int) energy.getMaxEnergy());
        data.set(DATA_PROGRESS, progress);

        ItemStack inputStack = input.getItem(0);
        Item component = componentFor(inputStack);
        if (component == null) {
            // 无有效输入 → 清零进度（换料/取空均在此兜底）
            progress = 0;
            speedAccum = 0;
            currentInput = null;
            return;
        }
        // 换料防御：成分种类变化 → 清零进度，防止跨配方白嫖半程
        if (component != currentInput) {
            progress = 0;
            speedAccum = 0;
            currentInput = component;
        }
        // tick 前检查能量足够才扣费（能量不足 → 暂停，进度保持）
        if (energy.getEnergyStored() < ModConfig.fractionatorCostPerCraft) {
            return;
        }
        // 产物槽不可容纳（加工中满仓）→ 暂停等待腾出
        if (progress < ModConfig.fractionatorProcessTicks && !canFit(component)) {
            return;
        }
        // 机器升级：速度升级提升每 tick 加工进度（+1 → 每级 +12.5%，8 级 2 倍速；小数余量累积避免截断）
        speedAccum += getSpeedMultiplier();
        int delta = (int) speedAccum;
        if (delta > 0) {
            speedAccum -= delta;
            progress += delta;
        }
        if (progress >= ModConfig.fractionatorProcessTicks) {
            if (canFit(component)) {
                progress = 0;
                inputStack.shrink(1);
                energy.extractEnergy(ModConfig.fractionatorCostPerCraft, false);
                addOutput(0, component);
                addOutput(1, ModItems.exhaustedCrystal.get());
            }
            // 满进度但产物槽满 → 保持满值，等待腾出后下 tick 结算
        }
        setChanged();
    }

    /** 活化结晶 → 对应活化成分；非七种活化结晶返回 null */
    private static Item componentFor(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.activatedSculkCrystal.get()) {
            return ModItems.activatedSculkComponent.get();
        }
        if (item == ModItems.activatedNetherCompoundCrystal.get()) {
            return ModItems.activatedNetherCompoundComponent.get();
        }
        if (item == ModItems.activatedEndMixtureCrystal.get()) {
            return ModItems.activatedEndMixtureComponent.get();
        }
        if (item == ModItems.activatedAdvancedMixtureCrystal.get()) {
            return ModItems.activatedAdvancedMixtureComponent.get();
        }
        if (item == ModItems.activatedPureCrystal.get()) {
            return ModItems.activatedPureComponent.get();
        }
        if (item == ModItems.activatedDragonCrystal.get()) {
            return ModItems.activatedDragonComponent.get();
        }
        if (item == ModItems.activatedUltimateMixtureCrystal.get()) {
            return ModItems.activatedUltimateMixtureComponent.get();
        }
        return null;
    }

    /** 是否可放入输入槽（仅 7 种活化结晶） */
    public static boolean isActivatedCrystal(ItemStack stack) {
        return !stack.isEmpty() && componentFor(stack) != null;
    }

    private boolean canFit(int slot, Item item) {
        ItemStack cur = output.getItem(slot);
        return cur.isEmpty() || (cur.is(item) && cur.getCount() < cur.getMaxStackSize());
    }

    private boolean canFit(Item component) {
        return canFit(0, component) && canFit(1, ModItems.exhaustedCrystal.get());
    }

    private void addOutput(int slot, Item item) {
        ItemStack cur = output.getItem(slot);
        if (cur.isEmpty()) {
            output.setItem(slot, new ItemStack(item));
        } else if (cur.is(item)) {
            cur.grow(1);
            output.setItem(slot, cur);
        }
    }

    public SimpleContainer inputContainer() {
        return input;
    }

    public SimpleContainer outputContainer() {
        return output;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_activated_fractionator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiActivatedFractionatorMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    public ContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：只接收赤能源（驱动分馏），不对外输出 =====

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

    // ===== NBT =====

    @Override
    public String[] excludedKeys() {
        return new String[]{"Output"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.put("Input", input.createTag());
        tag.put("Output", output.createTag());
        // 机器升级槽（独立 NBT key）
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        progress = tag.getInt("Progress");
        input.fromTag(tag.getList("Input", 10));
        output.fromTag(tag.getList("Output", 10));
        // 机器升级槽恢复（旧档无该 key 时保持空）
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
