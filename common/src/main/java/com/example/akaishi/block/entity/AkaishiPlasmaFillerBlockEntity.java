package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiPlasmaFillerMenu;
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
 * 离子体填装器方块实体（仅服务端驱动逻辑）。
 * 将等离子体灌入聚变反应棒，产出等离子体燃料棒（3 种）：
 * 每根反应棒 + 1000mb 对应等离子体 → 1 根对应燃料棒。
 * 输入罐为等离子体专用罐（仅等离子体管道可注入）；输出槽只读。
 * 加工罐选择：优先量最多的可用罐（单一/混合注入均自然支持）。
 */
public class AkaishiPlasmaFillerBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IFluidPipeDevice, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    // ===== 数据槽 =====
    public static final int DATA_SLOTS = 7;
    public static final int DATA_PLASMA0_AMOUNT = 0;
    public static final int DATA_PLASMA0_CAPACITY = 1;
    public static final int DATA_PLASMA1_AMOUNT = 2;
    public static final int DATA_PLASMA1_CAPACITY = 3;
    public static final int DATA_PLASMA2_AMOUNT = 4;
    public static final int DATA_PLASMA2_CAPACITY = 5;
    public static final int DATA_PROGRESS = 6;

    private final SimpleContainerData data;
    /** 等离子体输入罐（0=混合，1=下界，2=末地；仅本类流体可入） */
    private final List<FluidTank> plasmaTanks = new ArrayList<>(3);
    /** 反应棒槽（0） */
    private final SimpleContainer rods;
    /** 燃料棒输出槽（0=混合 1=下界 2=末地，只读） */
    private final SimpleContainer output = new SimpleContainer(3);
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    private int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;
    /** 当前加工罐索引（-1 无加工） */
    private int currentIdx = -1;

    public AkaishiPlasmaFillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PLASMA_FILLER.get(), pos, state);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.rods = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiPlasmaFillerBlockEntity.this.setChanged();
            }
        };
        this.plasmaTanks.add(plasmaTank(0, ModFluids.get(ModFluids.MIXED_PLASMA_ID)));
        this.plasmaTanks.add(plasmaTank(1, ModFluids.get(ModFluids.NETHER_PLASMA_ID)));
        this.plasmaTanks.add(plasmaTank(2, ModFluids.get(ModFluids.END_PLASMA_ID)));
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    /** 等离子体专用罐：仅接纳本罐对应流体（防混罐） */
    private FluidTank plasmaTank(int idx, Fluid fluid) {
        return new FluidTank(ModConfig.fillerPlasmaCapacity) {
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPlasmaFillerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 能量升级：动态提升等离子体罐容量（等离子体为该机加工资源，容量升级提升储备上限）
        for (int i = 0; i < plasmaTanks.size(); i++) {
            plasmaTanks.get(i).setCapacity((long) (ModConfig.fillerPlasmaCapacity * getEnergyCapacityMultiplier()));
        }
        data.set(DATA_PROGRESS, progress);
        for (int i = 0; i < plasmaTanks.size(); i++) {
            FluidTank tank = plasmaTanks.get(i);
            data.set(DATA_PLASMA0_AMOUNT + i * 2, (int) tank.getAmount());
            data.set(DATA_PLASMA0_CAPACITY + i * 2, (int) tank.getCapacity());
        }

        // 已在加工：校验当前罐条件仍满足，否则清零
        if (currentIdx >= 0 && !canCraft(currentIdx)) {
            progress = 0;
            speedAccum = 0;
            currentIdx = -1;
        }
        // 选择目标罐：优先量最多的可用罐（多罐可同时供料时按量自然分配）
        if (currentIdx < 0) {
            long best = 0;
            for (int i = 0; i < plasmaTanks.size(); i++) {
                if (canCraft(i)) {
                    long amount = plasmaTanks.get(i).getAmount();
                    if (amount > best) {
                        best = amount;
                        currentIdx = i;
                    }
                }
            }
        }
        if (currentIdx < 0) {
            return;
        }
        // 速度升级：每 tick 进度按倍率累加（小数余量累积，升级后单批耗时缩短）
        speedAccum += getSpeedMultiplier();
        int delta = (int) speedAccum;
        if (delta > 0) {
            speedAccum -= delta;
            progress += delta;
        }
        if (progress >= ModConfig.fillerProcessTicks) {
            progress = 0;
            FluidTank tank = plasmaTanks.get(currentIdx);
            tank.drain(ModConfig.fillerPlasmaPerRod, false);
            rods.getItem(0).shrink(1);
            addOutput(currentIdx);
            int done = currentIdx;
            currentIdx = -1;
            if (canCraft(done)) {
                currentIdx = done; // 同类材料充足时连续加工同罐
            }
        }
        setChanged();
    }

    /** 是否可用指定罐加工：罐量充足 + 反应棒充足 + 输出槽可放 */
    private boolean canCraft(int idx) {
        FluidTank tank = plasmaTanks.get(idx);
        if (tank.getAmount() < ModConfig.fillerPlasmaPerRod) {
            return false;
        }
        ItemStack rod = rods.getItem(0);
        if (rod.isEmpty() || !rod.is(ModItems.fusionRod.get())) {
            return false;
        }
        ItemStack cur = output.getItem(idx);
        Item rodItem = rodItemFor(idx);
        return cur.isEmpty() || (cur.is(rodItem) && cur.getCount() < cur.getMaxStackSize());
    }

    /** 罐索引 → 对应燃料棒物品 */
    private static Item rodItemFor(int idx) {
        if (idx == 0) {
            return ModItems.mixedPlasmaRod.get();
        }
        if (idx == 1) {
            return ModItems.netherPlasmaRod.get();
        }
        return ModItems.endPlasmaRod.get();
    }

    private void addOutput(int idx) {
        ItemStack cur = output.getItem(idx);
        Item rodItem = rodItemFor(idx);
        if (cur.isEmpty()) {
            output.setItem(idx, new ItemStack(rodItem));
        } else if (cur.is(rodItem)) {
            cur.grow(1);
            output.setItem(idx, cur);
        }
    }

    public List<FluidTank> plasmaTanks() {
        return plasmaTanks;
    }

    public SimpleContainer rodsContainer() {
        return rods;
    }

    public SimpleContainer outputContainer() {
        return output;
    }

    // ===== IItemPipeDevice / Container：组合视图（0=反应棒入，1~3=燃料棒产物仅出），供第三方物流 =====

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
        return new int[]{0}; // 反应棒槽可插
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{1, 2, 3}; // 3 格产物只读，仅可抽取
    }

    @Override
    public int getContainerSize() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        return rods.isEmpty() && output.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        if (index == 0) {
            return rods.getItem(0);
        }
        return index >= 1 && index <= 3 ? output.getItem(index - 1) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index == 0) {
            return rods.removeItem(0, count);
        }
        return index >= 1 && index <= 3 ? output.removeItem(index - 1, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index == 0) {
            return rods.removeItemNoUpdate(0);
        }
        return index >= 1 && index <= 3 ? output.removeItemNoUpdate(index - 1) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0) {
            rods.setItem(0, stack);
        } else if (index >= 1 && index <= 3) {
            output.setItem(index - 1, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        rods.clearContent();
        output.clearContent();
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_plasma_filler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiPlasmaFillerMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    public ContainerData data() {
        return data;
    }

    // ===== IFluidPipeDevice：3 个等离子体输入罐（仅等离子体管道可注入，不可抽取） =====

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
        return false;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return true;
    }

    // ===== NBT =====

    @Override
    public String[] excludedKeys() {
        return new String[]{"Plasma", "Output"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
        tag.putInt("CurrentIdx", currentIdx);
        tag.put("Rods", rods.createTag());
        tag.put("Output", output.createTag());
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
        CompoundTag plasma = new CompoundTag();
        for (int i = 0; i < plasmaTanks.size(); i++) {
            plasma.putLong("Tank" + i, plasmaTanks.get(i).getAmount());
        }
        tag.put("Plasma", plasma);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("Progress");
        currentIdx = tag.getInt("CurrentIdx");
        rods.fromTag(tag.getList("Rods", 10));
        output.fromTag(tag.getList("Output", 10));
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        CompoundTag plasma = tag.getCompound("Plasma");
        for (int i = 0; i < plasmaTanks.size(); i++) {
            long amount = plasma.getLong("Tank" + i);
            FluidTank tank = plasmaTanks.get(i);
            tank.setStack(amount > 0 ? FluidStack.create(tank.getFluid(), amount) : FluidStack.empty());
        }
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
