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
import com.example.akaishi.menu.AkaishiEnergyLiquefierMenu;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * 能量液化装置方块实体（仅服务端驱动逻辑）。
 * 投入高能量材料，消耗赤能源液化出对应燃料/能量液体：
 * - 下界之星 → 下界至纯能量（高级档，1000mb/个，耗 50M 赤能源）
 * - 凋零玫瑰 → 下界复合能量（低级档，400mb/个，耗 5M 赤能源）
 * - 末地混合物 → 末地混合燃料（中级档，500mb/个，耗 10M 赤能源）
 * - 幽匿生命体 → 幽匿生命燃料（最低档，100mb/个，耗 10M 赤能源）
 * - 巨龙混合物 → 末地巨龙燃料（高级档，500mb/个，耗 50M 赤能源）
 * 产物存于单个通用输出罐（一次处理一种输入），由液体管道抽取，输入槽可接物品管道/漏斗。
 * 槽位：0 = 材料输入槽（下界之星/凋零玫瑰/各混合物，只进不出）；1 = 生命能量固态物槽
 * （末地/幽匿/巨龙燃料液化时消耗 1 个固态物，下界能量液化无需固态物）。
 */
public class AkaishiEnergyLiquefierBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    /** 每 tick 赤能源输入率 */
    public static final long CHISHI_RATE = 1_000_000L;
    /** 赤能源缓冲容量（够 2 次下界之星液化积累） */
    public static final long CHISHI_CAPACITY = 100_000_000L;
    /** 单个产物液体罐容量（mb） */
    public static final long TANK_CAPACITY = 16_000L;

    public static final int INPUT_SLOT = 0;
    /** 生命能量固态物槽（末地/幽匿/巨龙燃料液化消耗） */
    public static final int SOLID_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    /** Menu 同步数据槽：0/1=赤能量/赤容量 2/3=输出液体量/容量 4=液化进度百分比 */
    public static final int DATA_SLOTS = 5;
    public static final int DATA_CHISHI_ENERGY = 0;
    public static final int DATA_CHISHI_CAPACITY = 1;
    public static final int DATA_FLUID_AMOUNT = 2;
    public static final int DATA_FLUID_CAPACITY = 3;
    public static final int DATA_PROGRESS = 4;

    /** 液化配方定义：输入物品 → 产物液体；needsSolid 标记是否需消耗生命能量固态物 */
    public record Recipe(ItemStack input, long cost, long amount, Fluid output, boolean needsSolid) {
    }

    /** 根据输入物品匹配液化配方；无匹配返回 null */
    public static Recipe recipeFor(ItemStack stack) {
        if (stack.is(Items.NETHER_STAR)) {
            // 高级档：1 颗星 → 1000mb 至纯能量（处理器浓缩为 500mb 至纯燃料）
            return new Recipe(new ItemStack(Items.NETHER_STAR), 50_000_000L, 1000L,
                    ModFluids.get(ModFluids.NETHER_PURE_ENERGY_ID), false);
        }
        if (stack.is(Items.WITHER_ROSE)) {
            // 低级档：1 朵玫瑰 → 400mb 复合能量（大幅缩减产出，需积攒多朵加工）
            return new Recipe(new ItemStack(Items.WITHER_ROSE), 5_000_000L, 400L,
                    ModFluids.get(ModFluids.NETHER_COMPOUND_ENERGY_ID), false);
        }
        if (stack.is(ModItems.endMixture.get())) {
            // 中级档：浓缩燃料（1 固态物 → 500mb 末地混合燃料）
            return new Recipe(new ItemStack(ModItems.endMixture.get()), 10_000_000L, 500L,
                    ModFluids.get(ModFluids.END_MIXTURE_FUEL_ID), true);
        }
        if (stack.is(ModItems.dragonMixture.get())) {
            // 高级档：浓缩燃料（1 固态物 → 500mb，利用率 7 约可燃烧 42 分钟，与合成成本匹配）
            return new Recipe(new ItemStack(ModItems.dragonMixture.get()), 50_000_000L, 500L,
                    ModFluids.get(ModFluids.DRAGON_FUEL_ID), true);
        }
        if (stack.is(ModItems.sculkLifeform.get())) {
            // 最低级档：1 个幽匿生命体 → 100mb（大幅缩减产出，利用率 3 约可燃烧 4 分钟）
            return new Recipe(new ItemStack(ModItems.sculkLifeform.get()), 10_000_000L, 100L,
                    ModFluids.get(ModFluids.SCULK_LIFE_FUEL_ID), true);
        }
        return null;
    }

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage akaishi;
    /** 通用输出罐：存当前配方的产物液体（一次只处理一种输入） */
    private final FluidTank outputTank;
    /** 已投入的赤能源（能量池模式，满配方 cost 完成一次） */
    private long progressEnergy;
    /** 当前输入物品的注册名，用于物品变化时重置进度 */
    private String lastItemKey = "";
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();

    public AkaishiEnergyLiquefierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_LIQUEFIER.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.outputTank = new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiEnergyLiquefierBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiEnergyLiquefierBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容能量缓冲（倍率变化时自动夹取）
        akaishi.setMaxEnergy((long) (CHISHI_CAPACITY * getEnergyCapacityMultiplier()));
        data.set(DATA_CHISHI_ENERGY, (int) akaishi.getEnergyStored());
        data.set(DATA_CHISHI_CAPACITY, (int) akaishi.getMaxEnergy());
        data.set(DATA_FLUID_AMOUNT, (int) outputTank.getAmount());
        data.set(DATA_FLUID_CAPACITY, (int) outputTank.getCapacity());

        Recipe recipe = recipeFor(inventory.getItem(INPUT_SLOT));
        if (recipe == null) {
            progressEnergy = 0;
            lastItemKey = "";
            data.set(DATA_PROGRESS, 0);
            return;
        }
        // 输入物品变化时丢弃旧进度，避免跨配方挪用能量池
        String key = recipeKey(inventory.getItem(INPUT_SLOT));
        if (!key.equals(lastItemKey)) {
            progressEnergy = 0;
            lastItemKey = key;
        }
        // 需要固态物的配方必须持有生命能量固态物（按类型校验而非仅非空，防止管道/误放物品被吞），否则停机等待
        if (recipe.needsSolid && !inventory.getItem(SOLID_SLOT).is(ModItems.akaishiLifeEssenceSolid.get())) {
            progressEnergy = 0;
            data.set(DATA_PROGRESS, 0);
            return;
        }
        // 目标罐：通用输出罐（产物与罐中异常液体不一致时 fill 会拒绝，安全）
        boolean changed = false;
        if (canAdd(outputTank, recipe.amount)) {
            // 机器升级：速度升级提升每 tick 抽取率（抽得快、加工更快）
            long extract = Math.min((long) (CHISHI_RATE * getSpeedMultiplier()), akaishi.getEnergyStored());
            if (extract > 0) {
                akaishi.extractEnergy(extract, false);
                progressEnergy += extract;
                if (progressEnergy >= recipe.cost) {
                    progressEnergy -= recipe.cost;
                    outputTank.fill(FluidStack.create(recipe.output, recipe.amount), false);
                    inventory.getItem(INPUT_SLOT).shrink(1);
                    if (inventory.getItem(INPUT_SLOT).isEmpty()) {
                        inventory.setItem(INPUT_SLOT, ItemStack.EMPTY);
                    }
                    // 末地/幽匿/巨龙燃料液化消耗 1 个生命能量固态物
                    if (recipe.needsSolid) {
                        inventory.getItem(SOLID_SLOT).shrink(1);
                        if (inventory.getItem(SOLID_SLOT).isEmpty()) {
                            inventory.setItem(SOLID_SLOT, ItemStack.EMPTY);
                        }
                    }
                }
                changed = true;
            }
        } else {
            progressEnergy = 0;
        }
        data.set(DATA_PROGRESS, (int) (progressEnergy * 100 / recipe.cost));
        if (changed) {
            setChanged();
        }
    }

    /** 目标罐是否还能装入指定量液体（罐满则 false） */
    private boolean canAdd(FluidTank tank, long amount) {
        if (tank.isEmpty()) {
            return amount <= tank.getCapacity();
        }
        return tank.getAmount() + amount <= tank.getCapacity();
    }

    private static String recipeKey(ItemStack stack) {
        ResourceLocation id = stack.getItem().arch$registryName();
        return id == null ? "" : id.toString();
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== IItemPipeDevice：材料 + 固态物双输入槽 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_SLOT, SOLID_SLOT};
    }

    // ===== IFluidPipeDevice：产物液体罐只可抽取（防管道灌入错误液体） =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(outputTank);
    }

    @Override
    public boolean canPipeExtract(FluidTank tank) {
        return true;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return false;
    }

    // 消除 IFluidPipeDevice 与 IItemPipeDevice 同名默认方法冲突：
    // 液体只可抽取、物品只可输入，两者合并为"可入物品 或 可抽液体"判断
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
        return Component.translatable("block.akaishi.akaishi_energy_liquefier");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiEnergyLiquefierMenu(id, inv, this);
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
        tag.put("OutputTank", outputTank.writeToNbt());
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
        outputTank.readFromNbt(tag.getCompound("OutputTank"));
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
