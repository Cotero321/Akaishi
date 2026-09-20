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
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.fluid.MultiFluidTank;
import com.example.akaishi.menu.AkaishiLifeCentrifugeMenu;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 生命离心机方块实体（仅服务端驱动逻辑）。
 * 将活化衰竭液体分离为两类结晶：1 个对应活化结晶（主产物）+ 1 个衰竭结晶（通用副产物）。
 * 每批（配方声明的 mB）产出各 1 个；每 tick 至多分离
 * {@link ModConfig#lifeCentrifugeConvertRate} mB，每 1mb 消耗 {@link ModConfig#lifeCentrifugeCostPerMb} 赤能源。
 * 输入罐仅接纳活化燃料（普通液体管道注入），输出结晶由第三方物流/玩家从产物槽取出。
 *
 * <p><b>配方来自数据包</b>（{@code data/akaishi/recipes/centrifuging/*.json}，类型 {@code akaishi:centrifuging}）：
 * 输入是哪一种活化燃料、一批多少 mB、产出哪种结晶与副产，全部由配方描述。
 * <p><b>批量为 0 的配方视为非法</b>（机器不加工）：批量既是结算阈值也是成本基数，缺了它无意义。
 * <p>本机自述工序族（{@link IMachineProcessKind}），使虚拟加工能要求"场域内真有离心机"。
 */
public class AkaishiLifeCentrifugeBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IItemPipeDevice, IDataCarrier, IUpgradeableMachine,
        IMachineProcessKind {

    // ===== 数据槽（long 拆低/高 32 位双槽同步，避免 int 溢出）=====
    public static final int DATA_SLOTS = 11;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_ENERGY_CAPACITY = 2;
    public static final int DATA_ENERGY_CAPACITY_HIGH = 3;
    public static final int DATA_IN_AMOUNT = 4;
    public static final int DATA_IN_AMOUNT_HIGH = 5;
    public static final int DATA_IN_CAPACITY = 6;
    public static final int DATA_IN_CAPACITY_HIGH = 7;
    /** 当前批次累计分离量（mb，满"配方声明的批量"结算一次） */
    public static final int DATA_PROGRESS = 8;
    /**
     * 当前配方声明的批量（mb，0 = 当前无配方）。
     * <p>必须同步给客户端：进度条的<b>分母</b>就是它，而批量现在由数据包决定，客户端算不出来。
     */
    public static final int DATA_BATCH_MB = 9;
    public static final int DATA_BATCH_MB_HIGH = 10;

    private final SimpleContainerData data;
    private final AkaishiEnergyStorage energy;
    /** 输入罐：仅接纳活化衰竭液体（7 种可混） */
    private final MultiFluidTank inTank;
    /** 输出槽：0=活化结晶（主），1=衰竭结晶（副） */
    private final SimpleContainer output = new SimpleContainer(2);
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    private long progress;
    /** 运转音播放器（本机音色） */
    private final MachineHum hum = new MachineHum(ModSounds.LIFE_CENTRIFUGE_HUM, 0.4F, 1.0F);

    public AkaishiLifeCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_CENTRIFUGE.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.lifeCentrifugeEnergyCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inTank = new MultiFluidTank(ModConfig.lifeCentrifugeInputCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                if (resource == null || !ModFluids.isActivatedFuel(resource.getFluid())) {
                    return 0; // 仅接纳活化燃料
                }
                return super.fill(resource, simulate);
            }

            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeCentrifugeBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 动态扩容：能量升级组件生效时按倍率提升能量上限
        energy.setMaxEnergy((long) (ModConfig.lifeCentrifugeEnergyCapacity * getEnergyCapacityMultiplier()));
        LongDataSlots.write(data, DATA_ENERGY, DATA_ENERGY_HIGH, energy.getEnergyStored());
        LongDataSlots.write(data, DATA_ENERGY_CAPACITY, DATA_ENERGY_CAPACITY_HIGH, energy.getMaxEnergy());
        LongDataSlots.write(data, DATA_IN_AMOUNT, DATA_IN_AMOUNT_HIGH, inTank.getAmount());
        LongDataSlots.write(data, DATA_IN_CAPACITY, DATA_IN_CAPACITY_HIGH, inTank.getCapacity());
        data.set(DATA_PROGRESS, (int) progress);

        Fluid fluid = inTank.getFluid();
        if (fluid == null || !ModFluids.isActivatedFuel(fluid)) {
            return; // 无活化燃料，静默等待
        }
        AkaishiFluidProcessRecipe recipe = selectRecipe(fluid);
        if (recipe == null) {
            return; // 无可用配方（或输出槽装不下完整一批）→ 暂停分离
        }
        long batchMb = recipe.fluidInputs().get(0).amount();
        // 批量同步给客户端（进度条分母；数据包化后客户端算不出来）
        LongDataSlots.write(data, DATA_BATCH_MB, DATA_BATCH_MB_HIGH, batchMb);
        Item main = recipe.result().getItem();
        Item byproduct = recipe.byproduct().isEmpty() ? null : recipe.byproduct().getItem();
        // 单位成本 = costPerMb × 配置 [machine] costMultiplier × 速度升级耗能倍率（封顶 4×）（afford 按此口径限流，防超扣）
        long unitCost = (long) (ModConfig.lifeCentrifugeCostPerMb * ModConfig.machineCostMultiplier
                * getEnergyCostMultiplier());
        long afford = energy.getEnergyStored() / unitCost;
        // 速度升级：每 tick 分离量按倍率提升（消耗率随之上升，能量不足时由 afford 限流）
        long rate = (long) (Math.min(Math.min(ModConfig.lifeCentrifugeConvertRate, inTank.getAmount(fluid)), afford)
                * getSpeedMultiplier());
        if (rate <= 0) {
            return;
        }
        inTank.drain(fluid, rate, false);
        energy.extractEnergy(rate * unitCost, false);
        hum.tick(level, worldPosition);
        progress += rate;
        // 每满一批结算一次（速率低于阈值，单 tick 至多结算 1 批）
        while (progress >= batchMb) {
            progress -= batchMb;
            addOutput(0, main);
            if (byproduct != null) {
                addOutput(1, byproduct);
            }
        }
        setChanged();
    }

    /**
     * 取当前输入流体对应的配方；无匹配、批量为 0 或产出槽不够则 null。
     * <p>数据包配方按"输入流体 + 一批用量 + 主产物 + 副产"描述，机器不再硬编码对照表。
     */
    @Nullable
    private AkaishiFluidProcessRecipe selectRecipe(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        for (AkaishiFluidProcessRecipe candidate
                : AkaishiMachineRecipeIndex.all(level.getRecipeManager(), AkaishiRecipeTypes.CENTRIFUGING.get())) {
            List<AkaishiFluidProcessRecipe.FluidSpec> ins = candidate.fluidInputs();
            // 本机固定"一路进液 + 一个主产物"；批量必须 > 0（它既是结算阈值也是成本基数）
            if (ins.size() != 1 || ins.get(0).fluid() != fluid || ins.get(0).amount() <= 0L
                    || candidate.result().isEmpty()) {
                continue;
            }
            Item main = candidate.result().getItem();
            Item byproduct = candidate.byproduct().isEmpty() ? null : candidate.byproduct().getItem();
            if (canFit(0, main) && (byproduct == null || canFit(1, byproduct))) {
                return candidate;
            }
        }
        return null;
    }

    /** 本机自述工序族：虚拟加工据此要求场域内确有离心机（见 {@link IMachineProcessKind}） */
    @Override
    public RecipeType<?> processKind() {
        return AkaishiRecipeTypes.CENTRIFUGING.get();
    }

    private boolean canFit(int slot, Item item) {
        ItemStack cur = output.getItem(slot);
        return cur.isEmpty() || (cur.is(item) && cur.getCount() < cur.getMaxStackSize());
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

    public SimpleContainer outputContainer() {
        return output;
    }

    // ===== IItemPipeDevice / Container：产物槽（0 活化结晶 / 1 衰竭结晶）仅出，第三方物流可抽取 =====

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
        return new int[0]; // 无物品输入（原料为液体）
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{0, 1};
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return output.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < 2 ? output.getItem(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return index >= 0 && index < 2 ? output.removeItem(index, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return index >= 0 && index < 2 ? output.removeItemNoUpdate(index) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index >= 0 && index < 2) {
            output.setItem(index, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        output.clearContent();
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiLifeCentrifugeMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    public ContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：只接收赤能源（驱动离心），不对外输出 =====

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

    // ===== IFluidPipeDevice：单输入罐（普通液体管道可注入活化燃料） =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(inTank);
    }

    @Override
    public boolean canPipeExtract(FluidTank tank) {
        return false;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return tank == inTank;
    }

    @Override
    public boolean isWasteOnlyDevice() {
        return false; // 活化燃料为安全中间产物，接入普通液体管道家族
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
        tag.put("InTank", inTank.writeToNbt());
        tag.put("Output", output.createTag());
        tag.putLong("Progress", progress);
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        inTank.readFromNbt(tag.getCompound("InTank"));
        output.fromTag(tag.getList("Output", 10));
        progress = tag.getLong("Progress");
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
