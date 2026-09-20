package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.recipe.IMachineProcessKind;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiFluidProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.menu.AkaishiFuelMixerMenu;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 燃料混合器方块实体（仅服务端驱动逻辑）。
 * 消耗赤能源，将两种燃料液体按比例调和为高阶混合燃料
 * （如 1000mb 末地混合燃料 + 1000mb 下界复合燃料 → 1500mb 高级混合燃料）。
 * 2 个通用输入罐（顺序无关，任意摆放）+ 1 个通用输出罐；配方按两罐液体组合判定，
 * 输入量不足 / 输出罐不可容纳 / 组合不匹配时停机。输入罐只可注入、输出罐只可抽取。
 *
 * <p><b>配方来自数据包</b>（{@code data/akaishi/recipes/mixing/*.json}，类型 {@code akaishi:mixing}）：
 * 两路输入流体、各自用量与输出流体都由配方描述，机器侧不再硬编码对照表。
 * <p>本机自述工序族（{@link IMachineProcessKind}），使虚拟加工能要求"场域内真有混合器"。
 */
public class AkaishiFuelMixerBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IDataCarrier, IUpgradeableMachine,
        IMachineProcessKind {

    /** Menu 同步数据槽：long 各占高低两槽 0/1=赤能量 2/3=赤容量 4/5=输入1量 6/7=输入1容量
     *  8/9=输入2量 10/11=输入2容量 12/13=输出量 14/15=输出容量 16=混合进度 */
    public static final int DATA_CHISHI_ENERGY = 0;
    public static final int DATA_CHISHI_ENERGY_HIGH = 1;
    public static final int DATA_CHISHI_CAPACITY = 2;
    public static final int DATA_CHISHI_CAPACITY_HIGH = 3;
    public static final int DATA_IN1_AMOUNT = 4;
    public static final int DATA_IN1_AMOUNT_HIGH = 5;
    public static final int DATA_IN1_CAPACITY = 6;
    public static final int DATA_IN1_CAPACITY_HIGH = 7;
    public static final int DATA_IN2_AMOUNT = 8;
    public static final int DATA_IN2_AMOUNT_HIGH = 9;
    public static final int DATA_IN2_CAPACITY = 10;
    public static final int DATA_IN2_CAPACITY_HIGH = 11;
    public static final int DATA_OUT_AMOUNT = 12;
    public static final int DATA_OUT_AMOUNT_HIGH = 13;
    public static final int DATA_OUT_CAPACITY = 14;
    public static final int DATA_OUT_CAPACITY_HIGH = 15;
    public static final int DATA_PROGRESS = 16;
    public static final int DATA_SLOTS = 17;

    /**
     * 按两个输入罐的液体组合选配方（罐顺序无关）；无匹配、输入不足或输出罐装不下则返回 null。
     * <p>数据包配方的两路流体不分先后，故两种摆放都算命中。
     */
    @Nullable
    private AkaishiFluidProcessRecipe selectRecipe() {
        Fluid in1 = in1Tank.getFluid();
        Fluid in2 = in2Tank.getFluid();
        if (in1 == null || in2 == null) {
            return null;
        }
        for (AkaishiFluidProcessRecipe candidate
                : AkaishiMachineRecipeIndex.all(level.getRecipeManager(), AkaishiRecipeTypes.MIXING.get())) {
            List<AkaishiFluidProcessRecipe.FluidSpec> ins = candidate.fluidInputs();
            AkaishiFluidProcessRecipe.FluidSpec out = candidate.fluidOutput();
            if (ins.size() != 2 || out == null) {
                continue; // 本机固定"两进一出"，形状不符的配方不适用
            }
            boolean straight = ins.get(0).fluid() == in1 && ins.get(1).fluid() == in2;
            boolean swapped = ins.get(0).fluid() == in2 && ins.get(1).fluid() == in1;
            if (!straight && !swapped) {
                continue;
            }
            long need1 = amountOf(ins, in1);
            long need2 = amountOf(ins, in2);
            if (in1Tank.getAmount() >= need1 && in2Tank.getAmount() >= need2
                    && canAdd(outTank, out.fluid(), out.amount())) {
                return candidate;
            }
        }
        return null;
    }

    /** 该流体在配方里的需求量（0 = 配方不含该流体） */
    private static long amountOf(List<AkaishiFluidProcessRecipe.FluidSpec> specs, Fluid fluid) {
        for (AkaishiFluidProcessRecipe.FluidSpec spec : specs) {
            if (spec.fluid() == fluid) {
                return spec.amount();
            }
        }
        return 0L;
    }

    private final SimpleContainerData data;
    private final AkaishiEnergyStorage akaishi;
    /** 输入罐 1/2（通用，可装任意燃料液体） */
    private final FluidTank in1Tank;
    private final FluidTank in2Tank;
    /** 输出罐（当前配方产物，通用） */
    private final FluidTank outTank;
    /** 已投入的赤能源（能量池模式，满单批混合成本完成一次） */
    private long progressEnergy;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 运转音播放器（本机音色） */
    private final MachineHum hum = new MachineHum(ModSounds.FUEL_MIXER_HUM, 0.4F, 1.0F);

    public AkaishiFuelMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUEL_MIXER.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.fuelMixerChishiCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.in1Tank = new FluidTank(ModConfig.fuelMixerTankCapacity) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.in2Tank = new FluidTank(ModConfig.fuelMixerTankCapacity) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.outTank = new FluidTank(ModConfig.fuelMixerTankCapacity) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFuelMixerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容能量缓冲（倍率变化时自动夹取）
        akaishi.setMaxEnergy((long) (ModConfig.fuelMixerChishiCapacity * getEnergyCapacityMultiplier()));
        LongDataSlots.write(data, DATA_CHISHI_ENERGY, DATA_CHISHI_ENERGY_HIGH, akaishi.getEnergyStored());
        LongDataSlots.write(data, DATA_CHISHI_CAPACITY, DATA_CHISHI_CAPACITY_HIGH, akaishi.getMaxEnergy());
        LongDataSlots.write(data, DATA_IN1_AMOUNT, DATA_IN1_AMOUNT_HIGH, in1Tank.getAmount());
        LongDataSlots.write(data, DATA_IN1_CAPACITY, DATA_IN1_CAPACITY_HIGH, in1Tank.getCapacity());
        LongDataSlots.write(data, DATA_IN2_AMOUNT, DATA_IN2_AMOUNT_HIGH, in2Tank.getAmount());
        LongDataSlots.write(data, DATA_IN2_CAPACITY, DATA_IN2_CAPACITY_HIGH, in2Tank.getCapacity());
        LongDataSlots.write(data, DATA_OUT_AMOUNT, DATA_OUT_AMOUNT_HIGH, outTank.getAmount());
        LongDataSlots.write(data, DATA_OUT_CAPACITY, DATA_OUT_CAPACITY_HIGH, outTank.getCapacity());

        AkaishiFluidProcessRecipe recipe = selectRecipe();
        // 组合不匹配 / 输入不足 / 输出罐无法容纳 → 停机等待，丢弃进度防跨配方挪用
        if (recipe == null) {
            progressEnergy = 0;
            data.set(DATA_PROGRESS, 0);
            return;
        }
        AkaishiFluidProcessRecipe.FluidSpec output = recipe.fluidOutput();
        // 机器升级：速度升级提升每 tick 抽取率（抽得快、加工更快）。
        // 配置 [machine] costMultiplier + 速度升级耗能倍率（封顶 4×）：单件赤能源需求与每 tick 抽取额同步放大 → 总耗放大、速度只决定快慢
        long costTotal = (long) (ModConfig.fuelMixerChishiCost * ModConfig.machineCostMultiplier
                * getEnergyCostMultiplier());
        long extract = Math.min((long) (ModConfig.fuelMixerChishiRate * getSpeedMultiplier()
                        * ModConfig.machineCostMultiplier * getEnergyCostMultiplier()),
                akaishi.getEnergyStored());
        if (extract > 0) {
            akaishi.extractEnergy(extract, false);
            hum.tick(level, worldPosition);
            progressEnergy += extract;
            if (costTotal > 0L && progressEnergy >= costTotal) {
                progressEnergy -= costTotal;
                in1Tank.drain(amountOf(recipe.fluidInputs(), in1Tank.getFluid()), false);
                in2Tank.drain(amountOf(recipe.fluidInputs(), in2Tank.getFluid()), false);
                outTank.fill(FluidStack.create(output.fluid(), output.amount()), false);
            }
            setChanged();
        }
        // 成本为 0（如 [machine] costMultiplier 被配成 0）时不能做除法，直接显示满载
        data.set(DATA_PROGRESS, costTotal <= 0L ? 100 : (int) (progressEnergy * 100 / costTotal));
    }

    /** 本机自述工序族：虚拟加工据此要求场域内确有混合器（见 {@link IMachineProcessKind}） */
    @Override
    public RecipeType<?> processKind() {
        return AkaishiRecipeTypes.MIXING.get();
    }

    /** 输出罐可容纳指定液体（空或同液体且余量足够） */
    private boolean canAdd(FluidTank tank, Fluid fluid, long amount) {
        if (tank.isEmpty()) {
            return amount <= tank.getCapacity();
        }
        return tank.getFluid() == fluid && tank.getAmount() + amount <= tank.getCapacity();
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_fuel_mixer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiFuelMixerMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    public SimpleContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：只接收赤能源（驱动混合），不对外输出 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return akaishi;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == AkaishiEnergyType.INSTANCE;
    }

    // ===== IFluidPipeDevice：输入罐只可注入，输出罐只可抽取 =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(in1Tank, in2Tank, outTank);
    }

    @Override
    public boolean canPipeExtract(FluidTank tank) {
        return tank == outTank;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return tank != outTank;
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("AkaishiEnergy", akaishi.getEnergyStored());
        tag.putLong("ProgressEnergy", progressEnergy);
        tag.put("In1Tank", in1Tank.writeToNbt());
        tag.put("In2Tank", in2Tank.writeToNbt());
        tag.put("OutTank", outTank.writeToNbt());
        // 机器升级槽（独立 NBT key）
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        akaishi.setEnergy(tag.getLong("AkaishiEnergy"));
        progressEnergy = tag.getLong("ProgressEnergy");
        in1Tank.readFromNbt(tag.getCompound("In1Tank"));
        in2Tank.readFromNbt(tag.getCompound("In2Tank"));
        outTank.readFromNbt(tag.getCompound("OutTank"));
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
