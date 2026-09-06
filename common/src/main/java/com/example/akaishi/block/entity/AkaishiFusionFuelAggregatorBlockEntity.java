package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiFusionFuelAggregatorMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import dev.architectury.fluid.FluidStack;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚变燃料聚合器方块实体（仅服务端驱动逻辑）。
 * 将活化成分聚合成等离子体：1 个活化成分 → 1000mb 对应等离子体。
 * 活化成分按燃料来源归入三种等离子体罐：
 * - 混合离子体：世界基础 / 高级混合 / 终极混合
 * - 下界离子体：下界复合 / 至纯
 * - 末地离子体：末地混合 / 末地巨龙
 * 输出罐为等离子体专用罐（仅等离子体管道可对接抽取）；换料清零进度防跨类错配。
 */
public class AkaishiFusionFuelAggregatorBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    // ===== 数据槽 =====
    public static final int DATA_SLOTS = 9;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_PLASMA0_AMOUNT = 3;
    public static final int DATA_PLASMA0_CAPACITY = 4;
    public static final int DATA_PLASMA1_AMOUNT = 5;
    public static final int DATA_PLASMA1_CAPACITY = 6;
    public static final int DATA_PLASMA2_AMOUNT = 7;
    public static final int DATA_PLASMA2_CAPACITY = 8;

    private final SimpleContainerData data;
    private final AkaishiEnergyStorage energy;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 输入槽（0=活化成分，7 种任一） */
    private final SimpleContainer input;
    /** 等离子体输出罐（0=混合，1=下界，2=末地；仅本类流体可入） */
    private final List<FluidTank> plasmaTanks = new ArrayList<>(3);
    private int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;
    /** 当前加工的等离子体种类（跨类换料清零进度） */
    private Fluid currentPlasma;

    public AkaishiFusionFuelAggregatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUSION_FUEL_AGGREGATOR.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.aggregatorEnergyCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.input = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiFusionFuelAggregatorBlockEntity.this.setChanged();
            }
        };
        this.plasmaTanks.add(plasmaTank(0, ModFluids.get(ModFluids.MIXED_PLASMA_ID)));
        this.plasmaTanks.add(plasmaTank(1, ModFluids.get(ModFluids.NETHER_PLASMA_ID)));
        this.plasmaTanks.add(plasmaTank(2, ModFluids.get(ModFluids.END_PLASMA_ID)));
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    /** 等离子体专用罐：仅接纳本罐对应流体（防混罐） */
    private FluidTank plasmaTank(int idx, Fluid fluid) {
        return new FluidTank(ModConfig.aggregatorPlasmaCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                if (resource == null || resource.getFluid() != fluid) {
                    return 0;
                }
                return super.fill(resource, simulate);
            }

            @Override
            protected void onChanged() {
                setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFusionFuelAggregatorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容能量缓冲（倍率变化时自动夹取）
        energy.setMaxEnergy((long) (ModConfig.aggregatorEnergyCapacity * getEnergyCapacityMultiplier()));
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_ENERGY_CAPACITY, (int) energy.getMaxEnergy());
        data.set(DATA_PROGRESS, progress);
        for (int i = 0; i < plasmaTanks.size(); i++) {
            FluidTank tank = plasmaTanks.get(i);
            data.set(DATA_PLASMA0_AMOUNT + i * 2, (int) tank.getAmount());
            data.set(DATA_PLASMA0_CAPACITY + i * 2, (int) tank.getCapacity());
        }

        ItemStack inputStack = input.getItem(0);
        Fluid target = plasmaFor(inputStack);
        if (target == null) {
            // 无有效输入 → 清零进度
            progress = 0;
            speedAccum = 0;
            currentPlasma = null;
            return;
        }
        // 换料防御：等离子体种类变化 → 清零进度
        if (target != currentPlasma) {
            progress = 0;
            speedAccum = 0;
            currentPlasma = target;
        }
        // tick 前检查能量足够才扣费
        if (energy.getEnergyStored() < ModConfig.aggregatorCostPerCraft) {
            return;
        }
        FluidTank tank = plasmaTanks.get(indexOf(target));
        // 输出罐放不下本次产出 → 暂停等待管道抽走
        if (tank.getAmount() + ModConfig.aggregatorProducePerCraft > tank.getCapacity()) {
            return;
        }
        // 机器升级：速度升级提升每 tick 加工进度（+1 → 每级 +12.5%，8 级 2 倍速；小数余量累积避免截断）
        speedAccum += getSpeedMultiplier();
        int delta = (int) speedAccum;
        if (delta > 0) {
            speedAccum -= delta;
            progress += delta;
        }
        if (progress >= ModConfig.aggregatorProcessTicks) {
            progress = 0;
            inputStack.shrink(1);
            energy.extractEnergy(ModConfig.aggregatorCostPerCraft, false);
            tank.fill(FluidStack.create(target, ModConfig.aggregatorProducePerCraft), false);
        }
        setChanged();
    }

    /** 活化成分 → 对应等离子体；非活化成分返回 null */
    private static Fluid plasmaFor(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.activatedSculkComponent.get()
                || item == ModItems.activatedAdvancedMixtureComponent.get()
                || item == ModItems.activatedUltimateMixtureComponent.get()) {
            return ModFluids.get(ModFluids.MIXED_PLASMA_ID);
        }
        if (item == ModItems.activatedNetherCompoundComponent.get()
                || item == ModItems.activatedPureComponent.get()) {
            return ModFluids.get(ModFluids.NETHER_PLASMA_ID);
        }
        if (item == ModItems.activatedEndMixtureComponent.get()
                || item == ModItems.activatedDragonComponent.get()) {
            return ModFluids.get(ModFluids.END_PLASMA_ID);
        }
        return null;
    }

    /** 是否可放入输入槽（仅 7 种活化成分） */
    public static boolean isActivatedComponent(ItemStack stack) {
        return !stack.isEmpty() && plasmaFor(stack) != null;
    }

    /** 等离子体 → 罐索引（0=混合 1=下界 2=末地） */
    private static int indexOf(Fluid plasma) {
        if (plasma == ModFluids.get(ModFluids.MIXED_PLASMA_ID)) {
            return 0;
        }
        if (plasma == ModFluids.get(ModFluids.NETHER_PLASMA_ID)) {
            return 1;
        }
        return 2;
    }

    public SimpleContainer inputContainer() {
        return input;
    }

    // ===== IItemPipeDevice / Container：输入槽（0）供第三方物流投入活化成分，仅入不可抽 =====

    @Override
    public boolean canPipeInput() {
        return IFluidPipeDevice.super.canPipeInput() || IItemPipeDevice.super.canPipeInput();
    }

    @Override
    public boolean canPipeOutput() {
        return IFluidPipeDevice.super.canPipeOutput() || IItemPipeDevice.super.canPipeOutput();
    }

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{0};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[0]; // 产物为等离子体液体（走 FLUID_HANDLER），无物品输出
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? input.getItem(0) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return index == 0 ? input.removeItem(0, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return index == 0 ? input.removeItemNoUpdate(0) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0) {
            input.setItem(0, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        input.clearContent();
    }

    public List<FluidTank> plasmaTanks() {
        return plasmaTanks;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_fusion_fuel_aggregator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiFusionFuelAggregatorMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    public ContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：只接收赤能源（驱动聚合），不对外输出 =====

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

    // ===== IFluidPipeDevice：3 个等离子体输出罐（仅等离子体管道可抽取，不可灌回） =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return plasmaTanks;
    }

    @Override
    public boolean isPlasmaTank(FluidTank tank) {
        return true;
    }

    @Override
    public boolean canPipeExtract(FluidTank tank) {
        return true;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return false;
    }

    // ===== NBT =====

    @Override
    public String[] excludedKeys() {
        return new String[]{"Plasma"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.put("Input", input.createTag());
        CompoundTag plasma = new CompoundTag();
        for (int i = 0; i < plasmaTanks.size(); i++) {
            plasma.putLong("Tank" + i, plasmaTanks.get(i).getAmount());
        }
        tag.put("Plasma", plasma);
        // 机器升级槽（独立 NBT key）
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        progress = tag.getInt("Progress");
        input.fromTag(tag.getList("Input", 10));
        CompoundTag plasma = tag.getCompound("Plasma");
        for (int i = 0; i < plasmaTanks.size(); i++) {
            long amount = plasma.getLong("Tank" + i);
            FluidTank tank = plasmaTanks.get(i);
            tank.setStack(amount > 0 ? FluidStack.create(tank.getFluid(), amount) : FluidStack.empty());
        }
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
