package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.menu.AkaishiFuelMixerMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 燃料混合器方块实体（仅服务端驱动逻辑）。
 * 消耗赤能源，将两种燃料液体按比例调和为高阶混合燃料：
 * - 高级混合燃料：1000mb 末地混合燃料 + 1000mb 下界复合燃料 → 1500mb 高级混合燃料
 * - 终极混合燃料：50mb 末地巨龙燃料 + 50mb 至纯燃料 → 50mb 终极混合燃料
 * 2 个通用输入罐（顺序无关，任意摆放）+ 1 个通用输出罐；配方按两罐液体组合判定，
 * 输入量不足 / 输出罐不可容纳 / 组合不匹配时停机。输入罐只可注入、输出罐只可抽取。
 */
public class AkaishiFuelMixerBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IDataCarrier, IUpgradeableMachine {

    /** Menu 同步数据槽：0/1=赤能量/容量 2/3=输入1量/容量 4/5=输入2量/容量 6/7=输出量/容量 8=混合进度 */
    public static final int DATA_SLOTS = 9;
    public static final int DATA_CHISHI_ENERGY = 0;
    public static final int DATA_CHISHI_CAPACITY = 1;
    public static final int DATA_IN1_AMOUNT = 2;
    public static final int DATA_IN1_CAPACITY = 3;
    public static final int DATA_IN2_AMOUNT = 4;
    public static final int DATA_IN2_CAPACITY = 5;
    public static final int DATA_OUT_AMOUNT = 6;
    public static final int DATA_OUT_CAPACITY = 7;
    public static final int DATA_PROGRESS = 8;

    /** 每 tick 赤能源输入率 / 容量 / 单批混合成本 */
    public static final long CHISHI_RATE = 1_000_000L;
    public static final long CHISHI_CAPACITY = 100_000_000L;
    public static final long CHISHI_COST = 2_000_000L;
    /** 单个液体罐容量（mb） */
    public static final long TANK_CAPACITY = 16_000L;

    /** 混合配方：两种输入液体 1:1:1 → 一种输出液体 */
    public record Recipe(Fluid in1, long in1Amount, Fluid in2, long in2Amount, Fluid out, long outAmount) {
    }

    /** 根据两个输入罐的液体组合判定配方（罐顺序无关）；不匹配返回 null */
    public static Recipe recipeFor(Fluid f1, Fluid f2) {
        Fluid end = ModFluids.get(ModFluids.END_MIXTURE_FUEL_ID);
        Fluid compound = ModFluids.get(ModFluids.NETHER_COMPOUND_FUEL_ID);
        if ((f1 == end && f2 == compound) || (f1 == compound && f2 == end)) {
            // 高级混合：1000+1000 → 1500mb（混合增值 50%）
            return new Recipe(end, 1000L, compound, 1000L,
                    ModFluids.get(ModFluids.ADVANCED_MIXTURE_FUEL_ID), 1500L);
        }
        Fluid dragon = ModFluids.get(ModFluids.DRAGON_FUEL_ID);
        Fluid pure = ModFluids.get(ModFluids.PURE_FUEL_ID);
        if ((f1 == dragon && f2 == pure) || (f1 == pure && f2 == dragon)) {
            return new Recipe(dragon, 50L, pure, 50L,
                    ModFluids.get(ModFluids.ULTIMATE_MIXTURE_FUEL_ID), 50L);
        }
        return null;
    }

    private final SimpleContainerData data;
    private final AkaishiEnergyStorage akaishi;
    /** 输入罐 1/2（通用，可装任意燃料液体） */
    private final FluidTank in1Tank;
    private final FluidTank in2Tank;
    /** 输出罐（当前配方产物，通用） */
    private final FluidTank outTank;
    /** 已投入的赤能源（能量池模式，满 CHISHI_COST 完成一次） */
    private long progressEnergy;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();

    public AkaishiFuelMixerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUEL_MIXER.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.in1Tank = new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.in2Tank = new FluidTank(TANK_CAPACITY) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.outTank = new FluidTank(TANK_CAPACITY) {
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
        akaishi.setMaxEnergy((long) (CHISHI_CAPACITY * getEnergyCapacityMultiplier()));
        data.set(DATA_CHISHI_ENERGY, (int) akaishi.getEnergyStored());
        data.set(DATA_CHISHI_CAPACITY, (int) akaishi.getMaxEnergy());
        data.set(DATA_IN1_AMOUNT, (int) in1Tank.getAmount());
        data.set(DATA_IN1_CAPACITY, (int) in1Tank.getCapacity());
        data.set(DATA_IN2_AMOUNT, (int) in2Tank.getAmount());
        data.set(DATA_IN2_CAPACITY, (int) in2Tank.getCapacity());
        data.set(DATA_OUT_AMOUNT, (int) outTank.getAmount());
        data.set(DATA_OUT_CAPACITY, (int) outTank.getCapacity());

        Recipe recipe = recipeFor(in1Tank.getFluid(), in2Tank.getFluid());
        // 组合不匹配 / 输入不足 / 输出罐无法容纳 → 停机等待，丢弃进度防跨配方挪用
        if (recipe == null || in1Tank.getAmount() < recipe.in1Amount || in2Tank.getAmount() < recipe.in2Amount
                || !canAdd(outTank, recipe.out, recipe.outAmount)) {
            progressEnergy = 0;
            data.set(DATA_PROGRESS, 0);
            return;
        }
        // 机器升级：速度升级提升每 tick 抽取率（抽得快、加工更快）
        long extract = Math.min((long) (CHISHI_RATE * getSpeedMultiplier()), akaishi.getEnergyStored());
        if (extract > 0) {
            akaishi.extractEnergy(extract, false);
            progressEnergy += extract;
            if (progressEnergy >= CHISHI_COST) {
                progressEnergy -= CHISHI_COST;
                in1Tank.drain(recipe.in1Amount, false);
                in2Tank.drain(recipe.in2Amount, false);
                outTank.fill(FluidStack.create(recipe.out, recipe.outAmount), false);
            }
            setChanged();
        }
        data.set(DATA_PROGRESS, (int) (progressEnergy * 100 / CHISHI_COST));
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
