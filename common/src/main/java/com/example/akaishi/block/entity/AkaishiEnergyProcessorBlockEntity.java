package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiEnergyProcessorMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * 能量加工器方块实体（仅服务端驱动逻辑）。
 * 消耗赤能源（输入率 1M/t）驱动：生命固态物 + 下界能量液体 → 反应堆燃料。
 * - 1 固态物 + 1000mb 下界复合能量 → 1000mb 下界复合燃料
 * - 1 固态物 + 100mb 下界至纯能量 → 50mb 至纯燃料（至纯燃料更浓缩，产量减半）
 * 四个液体罐：至纯/复合能量输入罐（管道注入）+ 至纯/复合燃料输出罐（管道抽取）。
 * 槽位：0 = 输入槽（生命固态物，只进不出）。
 */
public class AkaishiEnergyProcessorBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    /** 每 tick 赤能源输入率 */
    public static final long CHISHI_RATE = 1_000_000L;
    /** 赤能源缓冲容量（够 4 次加工积累） */
    public static final long CHISHI_CAPACITY = 20_000_000L;
    /** 单个液体罐容量（mb） */
    public static final long TANK_CAPACITY = 16_000L;
    /** 每次加工消耗赤能源 */
    public static final long CHISHI_COST = 5_000_000L;
    /** 复合加工：1 固态物 + 1000mb 复合能量 → 1000mb 复合燃料 */
    public static final long COMPOUND_AMOUNT = 1000L;
    /** 至纯加工：1 固态物 + 100mb 至纯能量 → 75mb 至纯燃料（轻度浓缩，减轻固态物负担） */
    public static final long PURE_INPUT_AMOUNT = 100L;
    public static final long PURE_OUTPUT_AMOUNT = 75L;

    public static final int INPUT_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    /** Menu 同步数据槽：0/1=赤能量/赤容量 2/3=至纯能量入量/容量 4/5=复合能量入量/容量
     *  6/7=至纯燃料出量/容量 8/9=复合燃料出量/容量 10=加工进度百分比 */
    public static final int DATA_SLOTS = 11;
    public static final int DATA_CHISHI_ENERGY = 0;
    public static final int DATA_CHISHI_CAPACITY = 1;
    public static final int DATA_PURE_IN_AMOUNT = 2;
    public static final int DATA_PURE_IN_CAPACITY = 3;
    public static final int DATA_COMPOUND_IN_AMOUNT = 4;
    public static final int DATA_COMPOUND_IN_CAPACITY = 5;
    public static final int DATA_PURE_OUT_AMOUNT = 6;
    public static final int DATA_PURE_OUT_CAPACITY = 7;
    public static final int DATA_COMPOUND_OUT_AMOUNT = 8;
    public static final int DATA_COMPOUND_OUT_CAPACITY = 9;
    public static final int DATA_PROGRESS = 10;

    /** 加工配方：输入液体+固态物 → 输出液体（输入/输出量分离，可支持浓缩产出） */
    public record Recipe(Fluid inputFluid, Fluid outputFluid, long inputAmount, long outputAmount) {
    }

    /** 复合配方优先：量大、成本低，避免与至纯配方抢占进度 */
    public static Recipe compoundRecipe() {
        return new Recipe(ModFluids.get(ModFluids.NETHER_COMPOUND_ENERGY_ID),
                ModFluids.get(ModFluids.NETHER_COMPOUND_FUEL_ID), COMPOUND_AMOUNT, COMPOUND_AMOUNT);
    }

    public static Recipe pureRecipe() {
        return new Recipe(ModFluids.get(ModFluids.NETHER_PURE_ENERGY_ID),
                ModFluids.get(ModFluids.PURE_FUEL_ID), PURE_INPUT_AMOUNT, PURE_OUTPUT_AMOUNT);
    }

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage akaishi;
    private final FluidTank pureInTank;
    private final FluidTank compoundInTank;
    private final FluidTank pureOutTank;
    private final FluidTank compoundOutTank;
    /** 已投入的赤能源（能量池模式，满 CHISHI_COST 完成一次加工） */
    private long progressEnergy;
    /** 当前配方标识（""=无，compound/pure），配方变化时重置进度 */
    private String lastRecipeKey = "";
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();

    public AkaishiEnergyProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_PROCESSOR.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.pureInTank = tank();
        this.compoundInTank = tank();
        this.pureOutTank = tank();
        this.compoundOutTank = tank();
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiEnergyProcessorBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    private FluidTank tank() {
        return new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiEnergyProcessorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容能量缓冲（倍率变化时自动夹取）
        akaishi.setMaxEnergy((long) (CHISHI_CAPACITY * getEnergyCapacityMultiplier()));
        data.set(DATA_CHISHI_ENERGY, (int) akaishi.getEnergyStored());
        data.set(DATA_CHISHI_CAPACITY, (int) akaishi.getMaxEnergy());
        data.set(DATA_PURE_IN_AMOUNT, (int) pureInTank.getAmount());
        data.set(DATA_PURE_IN_CAPACITY, (int) pureInTank.getCapacity());
        data.set(DATA_COMPOUND_IN_AMOUNT, (int) compoundInTank.getAmount());
        data.set(DATA_COMPOUND_IN_CAPACITY, (int) compoundInTank.getCapacity());
        data.set(DATA_PURE_OUT_AMOUNT, (int) pureOutTank.getAmount());
        data.set(DATA_PURE_OUT_CAPACITY, (int) pureOutTank.getCapacity());
        data.set(DATA_COMPOUND_OUT_AMOUNT, (int) compoundOutTank.getAmount());
        data.set(DATA_COMPOUND_OUT_CAPACITY, (int) compoundOutTank.getCapacity());

        ItemStack input = inventory.getItem(INPUT_SLOT);
        Recipe recipe = null;
        String key = "";
        if (!input.isEmpty() && input.is(ModItems.akaishiLifeEssenceSolid.get())) {
            // 复合配方优先（量大成本低）
            Recipe compound = compoundRecipe();
            Recipe pure = pureRecipe();
            if (canProcess(compoundInTank, compoundOutTank, compound)) {
                recipe = compound;
                key = "compound";
            } else if (canProcess(pureInTank, pureOutTank, pure)) {
                recipe = pure;
                key = "pure";
            }
        }
        if (recipe == null) {
            progressEnergy = 0;
            lastRecipeKey = "";
            data.set(DATA_PROGRESS, 0);
            return;
        }
        // 配方切换时丢弃旧进度，避免跨配方挪用能量池
        if (!key.equals(lastRecipeKey)) {
            progressEnergy = 0;
            lastRecipeKey = key;
        }
        FluidTank inputTank = recipe.inputFluid == ModFluids.get(ModFluids.NETHER_PURE_ENERGY_ID) ? pureInTank : compoundInTank;
        FluidTank outputTank = recipe.outputFluid == ModFluids.get(ModFluids.PURE_FUEL_ID) ? pureOutTank : compoundOutTank;
        boolean changed = false;
        if (canProcess(inputTank, outputTank, recipe)) {
            // 机器升级：速度升级提升每 tick 抽取率（抽得快、加工更快）
            long extract = Math.min((long) (CHISHI_RATE * getSpeedMultiplier()), akaishi.getEnergyStored());
            if (extract > 0) {
                akaishi.extractEnergy(extract, false);
                progressEnergy += extract;
                if (progressEnergy >= CHISHI_COST) {
                    progressEnergy -= CHISHI_COST;
                    // 原子完成：消耗固态物 + 输入液体，产出燃料液体
                    inputTank.drain(recipe.inputAmount, false);
                    outputTank.fill(FluidStack.create(recipe.outputFluid, recipe.outputAmount), false);
                    input.shrink(1);
                    if (input.isEmpty()) {
                        inventory.setItem(INPUT_SLOT, ItemStack.EMPTY);
                    }
                }
                changed = true;
            }
        } else {
            progressEnergy = 0;
        }
        data.set(DATA_PROGRESS, (int) (progressEnergy * 100 / CHISHI_COST));
        if (changed) {
            setChanged();
        }
    }

    /** 输入罐液体充足且输出罐能装下时，配方可执行 */
    private boolean canProcess(FluidTank inputTank, FluidTank outputTank, Recipe recipe) {
        if (inputTank.getFluid() != recipe.inputFluid || inputTank.getAmount() < recipe.inputAmount) {
            return false;
        }
        if (outputTank.isEmpty()) {
            return recipe.outputAmount <= outputTank.getCapacity();
        }
        return outputTank.getFluid() == recipe.outputFluid
                && outputTank.getAmount() + recipe.outputAmount <= outputTank.getCapacity();
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== IItemPipeDevice：仅输入槽（生命固态物），管道可插入 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    // ===== IFluidPipeDevice：输入罐可注、输出罐可抽 =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(pureInTank, compoundInTank, pureOutTank, compoundOutTank);
    }

    @Override
    public boolean canPipeExtract(FluidTank tank) {
        return tank == pureOutTank || tank == compoundOutTank;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return tank == pureInTank || tank == compoundInTank;
    }

    // 消除 IFluidPipeDevice 与 IItemPipeDevice 同名默认方法冲突
    @Override
    public boolean canPipeInput() {
        return IFluidPipeDevice.super.canPipeInput() || IItemPipeDevice.super.canPipeInput();
    }

    @Override
    public boolean canPipeOutput() {
        return IFluidPipeDevice.super.canPipeOutput() || IItemPipeDevice.super.canPipeOutput();
    }

    // ===== Container 委托：供漏斗 / 物品管道读写输入槽 =====

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return inventory.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return inventory.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return inventory.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        inventory.setItem(index, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // ===== IEnergyProvider：仅赤能源输入（驱动），不外输 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return akaishi;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == AkaishiEnergyType.INSTANCE;
    }

    // ===== 菜单 / 序列化 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_energy_processor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiEnergyProcessorMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("AkaishiEnergy", akaishi.getEnergyStored());
        tag.putLong("ProgressEnergy", progressEnergy);
        tag.put("PureInTank", pureInTank.writeToNbt());
        tag.put("CompoundInTank", compoundInTank.writeToNbt());
        tag.put("PureOutTank", pureOutTank.writeToNbt());
        tag.put("CompoundOutTank", compoundOutTank.writeToNbt());
        net.minecraft.core.NonNullList<ItemStack> items =
                net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, inventory.getItem(i));
        }
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items);
        // 机器升级槽（独立 NBT key，避免与输入槽 "Items" 冲突）
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        akaishi.setEnergy(tag.getLong("AkaishiEnergy"));
        progressEnergy = tag.getLong("ProgressEnergy");
        pureInTank.readFromNbt(tag.getCompound("PureInTank"));
        compoundInTank.readFromNbt(tag.getCompound("CompoundInTank"));
        pureOutTank.readFromNbt(tag.getCompound("PureOutTank"));
        compoundOutTank.readFromNbt(tag.getCompound("CompoundOutTank"));
        net.minecraft.core.NonNullList<ItemStack> items =
                net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
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
