package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.api.recipe.IMachineProcessKind;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiFluidProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.menu.AkaishiEnergyLiquefierMenu;
import com.example.akaishi.sound.MachineHum;
import com.example.akaishi.sound.ModSounds;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.util.LongDataSlots;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 能量液化装置方块实体（仅服务端驱动逻辑）。
 * 投入高能量材料，消耗赤能源液化出对应燃料/能量液体
 * （下界之星 → 下界至纯能量、凋零玫瑰 → 下界复合能量、各混合物/幽匿生命体 → 对应燃料）。
 * 产物存于单个通用输出罐（一次处理一种输入），由液体管道抽取，输入槽可接物品管道/漏斗。
 * 槽位：0 = 材料输入槽（只进不出）；1 = 辅料槽（配方声明 {@code catalyst} 时消耗，如生命能量固态物）。
 *
 * <p><b>配方来自数据包</b>（{@code data/akaishi/recipes/liquefying/*.json}，类型
 * {@code akaishi:liquefying}）：产物流体、辅料要求与各档赤能源成本都由配方描述，
 * 机器侧不再硬编码"输入 → 液体/成本"对照表。
 * <p><b>本机必须声明 energy</b>：液化是"能量换燃料"，配方不写能量等于凭空造燃料，
 * 故 {@code energy <= 0} 的配方本机不加工。
 * <p>本机自述工序族（{@link IMachineProcessKind}），使虚拟加工能要求"场域内真有液化机"。
 */
public class AkaishiEnergyLiquefierBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IItemPipeDevice, IDataCarrier, IUpgradeableMachine,
        IMachineProcessKind {

    public static final int INPUT_SLOT = 0;
    /** 生命能量固态物槽（末地/幽匿/巨龙燃料液化消耗） */
    public static final int SOLID_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    // Menu 同步数据槽：能量/容量/液体量/液体容量均为 long，拆高低 32 位（SimpleContainerData 仅支持 int）
    public static final int DATA_CHISHI_ENERGY = 0;
    public static final int DATA_CHISHI_ENERGY_HIGH = 1;
    public static final int DATA_CHISHI_CAPACITY = 2;
    public static final int DATA_CHISHI_CAPACITY_HIGH = 3;
    public static final int DATA_FLUID_AMOUNT = 4;
    public static final int DATA_FLUID_AMOUNT_HIGH = 5;
    public static final int DATA_FLUID_CAPACITY = 6;
    public static final int DATA_FLUID_CAPACITY_HIGH = 7;
    /** 液化进度百分比 */
    public static final int DATA_PROGRESS = 8;
    public static final int DATA_SLOTS = 9;

    /** 根据输入物品匹配液化配方；无匹配返回 null */
    @Nullable
    public static AkaishiFluidProcessRecipe recipeFor(Level level, ItemStack stack) {
        AkaishiFluidProcessRecipe recipe = AkaishiMachineRecipeIndex.find(level,
                AkaishiRecipeTypes.LIQUEFYING.get(), stack);
        // energy <= 0 视为非法配方（液化必须耗能，否则"零成本造燃料"）：机器不加工
        return recipe != null && recipe.energy() > 0L ? recipe : null;
    }

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage akaishi;
    /** 通用输出罐：存当前配方的产物液体（一次只处理一种输入） */
    private final FluidTank outputTank;
    /** 已投入的赤能源（能量池模式，满配方 cost 完成一次） */
    private long progressEnergy;
    /** 当前生效的配方（数据包提供；换配方即丢弃旧进度，防跨配方挪用能量池） */
    @Nullable
    private AkaishiFluidProcessRecipe currentRecipe;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();

    /** 运转音播放器（本机音色） */
    private final MachineHum hum = new MachineHum(ModSounds.ENERGY_LIQUEFIER_HUM, 0.4F, 1.0F);

    public AkaishiEnergyLiquefierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_LIQUEFIER.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.energyLiquefierChishiCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.outputTank = new FluidTank(ModConfig.energyLiquefierTankCapacity) {
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
        akaishi.setMaxEnergy((long) (ModConfig.energyLiquefierChishiCapacity * getEnergyCapacityMultiplier()));
        LongDataSlots.write(data, DATA_CHISHI_ENERGY, DATA_CHISHI_ENERGY_HIGH, akaishi.getEnergyStored());
        LongDataSlots.write(data, DATA_CHISHI_CAPACITY, DATA_CHISHI_CAPACITY_HIGH, akaishi.getMaxEnergy());
        LongDataSlots.write(data, DATA_FLUID_AMOUNT, DATA_FLUID_AMOUNT_HIGH, outputTank.getAmount());
        LongDataSlots.write(data, DATA_FLUID_CAPACITY, DATA_FLUID_CAPACITY_HIGH, outputTank.getCapacity());

        AkaishiFluidProcessRecipe recipe = recipeFor(level, inventory.getItem(INPUT_SLOT));
        if (recipe == null || recipe.fluidOutput() == null) {
            progressEnergy = 0;
            currentRecipe = null;
            data.set(DATA_PROGRESS, 0);
            return;
        }
        // 输入物品变化时丢弃旧进度，避免跨配方挪用能量池
        if (recipe != currentRecipe) {
            progressEnergy = 0;
            currentRecipe = recipe;
        }
        // 辅料要求：配方声明 catalyst 时必须持有足够数量（按类型校验而非仅非空，防管道/误放物品被吞）
        Ingredient catalyst = recipe.catalyst();
        ItemStack solidStack = inventory.getItem(SOLID_SLOT);
        if (catalyst != null && (!catalyst.test(solidStack) || solidStack.getCount() < recipe.inputCount())) {
            progressEnergy = 0;
            data.set(DATA_PROGRESS, 0);
            return;
        }
        // 配置 [machine] costMultiplier + 速度升级耗能倍率（封顶 4×）：单件赤能源需求与每 tick 抽取额同步放大 → 总耗放大、速度只决定快慢
        long costTotal = (long) (recipe.energy() * ModConfig.machineCostMultiplier * getEnergyCostMultiplier());
        AkaishiFluidProcessRecipe.FluidSpec output = recipe.fluidOutput();
        boolean changed = false;
        if (canAdd(outputTank, output.amount())) {
            // 机器升级：速度升级提升每 tick 抽取率（抽得快、加工更快）
            long extract = Math.min((long) (ModConfig.energyLiquefierChishiRate * getSpeedMultiplier()
                            * ModConfig.machineCostMultiplier * getEnergyCostMultiplier()),
                    akaishi.getEnergyStored());
            if (extract > 0) {
                akaishi.extractEnergy(extract, false);
                hum.tick(level, worldPosition);
                progressEnergy += extract;
                if (costTotal > 0L && progressEnergy >= costTotal) {
                    progressEnergy -= costTotal;
                    outputTank.fill(FluidStack.create(output.fluid(), output.amount()), false);
                    inventory.getItem(INPUT_SLOT).shrink(recipe.inputCount());
                    if (inventory.getItem(INPUT_SLOT).isEmpty()) {
                        inventory.setItem(INPUT_SLOT, ItemStack.EMPTY);
                    }
                    // 辅料随主料一起消耗（数量与主料一致，见 inputCount 注释）
                    if (catalyst != null) {
                        solidStack.shrink(recipe.inputCount());
                        if (solidStack.isEmpty()) {
                            inventory.setItem(SOLID_SLOT, ItemStack.EMPTY);
                        }
                    }
                }
                changed = true;
            }
        } else {
            progressEnergy = 0;
        }
        // 成本为 0（如 [machine] costMultiplier 被配成 0）时不能做除法，直接显示满载
        data.set(DATA_PROGRESS, costTotal <= 0L ? 100 : (int) (progressEnergy * 100 / costTotal));
        if (changed) {
            setChanged();
        }
    }

    /** 本机自述工序族：虚拟加工据此要求场域内确有液化机（见 {@link IMachineProcessKind}） */
    @Override
    public RecipeType<?> processKind() {
        return AkaishiRecipeTypes.LIQUEFYING.get();
    }

    /** 目标罐是否还能装入指定量液体（罐满则 false） */
    private boolean canAdd(FluidTank tank, long amount) {
        if (tank.isEmpty()) {
            return amount <= tank.getCapacity();
        }
        return tank.getAmount() + amount <= tank.getCapacity();
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
