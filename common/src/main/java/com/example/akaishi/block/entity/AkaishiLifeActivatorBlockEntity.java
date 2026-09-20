package com.example.akaishi.block.entity;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.recipe.IMachineProcessKind;
import com.example.akaishi.craft.recipe.AkaishiFluidProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.sound.ModSounds;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.fluid.MultiFluidTank;
import com.example.akaishi.menu.AkaishiLifeActivatorMenu;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 生命活化器方块实体（仅服务端驱动逻辑）。
 * 缓慢无害化衰竭燃料：废料管道将衰竭燃料注入输入罐，每 tick 至多转化
 * {@link ModConfig#lifeActivatorConvertRate} mb 为对应的"活化衰竭液体"（1:1），
 * 每 1mb 消耗 {@link ModConfig#lifeActivatorCostPerMb} 生命能量。
 * 活化液体为安全中间产物，由普通液体管道从输出罐抽取。
 * 混合接入设备：输入罐接废料管道家族、输出罐接普通液体管道家族（罐级家族隔离）。
 *
 * <p><b>配方来自数据包</b>（{@code data/akaishi/recipes/activating/*.json}，类型 {@code akaishi:activating}）：
 * 哪种衰竭燃料转成哪种活化液体由配方声明，机器侧不再硬编码 7 分支对照表。
 * <p><b>转化是 1:1 连续流</b>，没有"批"的概念：配方里的 {@code amount} 只作<b>计价单位</b>
 * （{@code MachineProcessEnergy} 按"每 amount mB 耗多少生命能量"报价），实际转化量每 tick 由速率决定。
 * <p>本机自述工序族（{@link IMachineProcessKind}）。
 */
public class AkaishiLifeActivatorBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IMachineProcessKind {

    /**
     * Menu 同步数据槽：每个 long 拆低/高 32 位两槽无损同步，避免 int 溢出。
     * 0/1=生命能量 2/3=生命容量 4/5=输入量 6/7=输入容量
     * 8/9=输出量 10/11=输出容量 12/13=累计活化量
     */
    public static final int DATA_SLOTS = 14;
    public static final int DATA_LIFE_ENERGY = 0;
    public static final int DATA_LIFE_ENERGY_HIGH = 1;
    public static final int DATA_LIFE_CAPACITY = 2;
    public static final int DATA_LIFE_CAPACITY_HIGH = 3;
    public static final int DATA_IN_AMOUNT = 4;
    public static final int DATA_IN_AMOUNT_HIGH = 5;
    public static final int DATA_IN_CAPACITY = 6;
    public static final int DATA_IN_CAPACITY_HIGH = 7;
    public static final int DATA_OUT_AMOUNT = 8;
    public static final int DATA_OUT_AMOUNT_HIGH = 9;
    public static final int DATA_OUT_CAPACITY = 10;
    public static final int DATA_OUT_CAPACITY_HIGH = 11;
    public static final int DATA_PROCESSED_LOW = 12;
    public static final int DATA_PROCESSED_HIGH = 13;

    /** 生命能量容量 / 每 1mb 转化成本 / 罐容量 / 转化速率均由 {@link com.example.akaishi.config.ModConfig} 提供 */

    private final SimpleContainerData data;
    private final AkaishiEnergyStorage life;
    /** 输入罐：废料专用多液体罐（仅衰竭燃料可入，7 种可混） */
    private final MultiFluidTank inTank;
    /** 输出罐：活化液体多液体罐（仅活化液体可入，7 种可混） */
    private final MultiFluidTank outTank;
    /** 累计活化量（mb，仅用于 GUI 展示无害化进度） */
    private long processed;
    /** 活化运转音播放冷却（tick） */
    private int soundCooldown;

    public AkaishiLifeActivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_ACTIVATOR.get(), pos, state);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, ModConfig.lifeActivatorLifeCapacity);
        this.inTank = new MultiFluidTank(ModConfig.lifeActivatorInputCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                if (resource == null || !ModFluids.isExhaustedFuel(resource.getFluid())) {
                    return 0; // 仅接纳衰竭燃料
                }
                return super.fill(resource, simulate);
            }

            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.outTank = new MultiFluidTank(ModConfig.lifeActivatorOutputCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                if (resource == null || !ModFluids.isActivatedFuel(resource.getFluid())) {
                    return 0; // 仅接纳活化液体，防止普通管道误灌
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeActivatorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        LongDataSlots.write(data, DATA_LIFE_ENERGY, DATA_LIFE_ENERGY_HIGH, life.getEnergyStored());
        LongDataSlots.write(data, DATA_LIFE_CAPACITY, DATA_LIFE_CAPACITY_HIGH, life.getMaxEnergy());
        LongDataSlots.write(data, DATA_IN_AMOUNT, DATA_IN_AMOUNT_HIGH, inTank.getAmount());
        LongDataSlots.write(data, DATA_IN_CAPACITY, DATA_IN_CAPACITY_HIGH, inTank.getCapacity());
        LongDataSlots.write(data, DATA_OUT_AMOUNT, DATA_OUT_AMOUNT_HIGH, outTank.getAmount());
        LongDataSlots.write(data, DATA_OUT_CAPACITY, DATA_OUT_CAPACITY_HIGH, outTank.getCapacity());

        Fluid fuel = inTank.getFluid();
        AkaishiFluidProcessRecipe recipe = selectRecipe(fuel);
        if (recipe == null) {
            return; // 无废料输入（或该燃料没有配方），静默等待
        }
        Fluid activated = recipe.fluidOutput().fluid();
        // 实际转化量 = min(速率, 该废料存量, 输出余量, 生命能量可支持量)
        long outRoom = outTank.getCapacity() - outTank.getAmount();
        long afford = life.getEnergyStored() / ModConfig.lifeActivatorCostPerMb;
        long amount = Math.min(Math.min(ModConfig.lifeActivatorConvertRate, inTank.getAmount(fuel)), Math.min(outRoom, afford));
        if (amount <= 0) {
            return;
        }
        inTank.drain(fuel, amount, false);
        outTank.fill(FluidStack.create(activated, amount), false);
        life.extractEnergy(amount * ModConfig.lifeActivatorCostPerMb, false);
        processed += amount;
        // 活化运转声（循环）：每 15 tick 重播短音
        if (--soundCooldown <= 0) {
            level.playSound(null, worldPosition, ModSounds.ACTIVATOR_BUBBLE.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
            soundCooldown = 15;
        }
        // 累计活化量拆低/高 16 位段同步，GUI 侧经 LongDataSlots 重组
        LongDataSlots.write(data, DATA_PROCESSED_LOW, DATA_PROCESSED_HIGH, processed);
        setChanged();
    }

    /** 取输入衰竭燃料对应的配方；无匹配或配方没声明流体产物则 null */
    @Nullable
    private AkaishiFluidProcessRecipe selectRecipe(@Nullable Fluid fuel) {
        if (fuel == null || fuel == Fluids.EMPTY) {
            return null;
        }
        for (AkaishiFluidProcessRecipe candidate
                : AkaishiMachineRecipeIndex.all(level.getRecipeManager(), AkaishiRecipeTypes.ACTIVATING.get())) {
            List<AkaishiFluidProcessRecipe.FluidSpec> ins = candidate.fluidInputs();
            if (ins.size() == 1 && ins.get(0).fluid() == fuel && candidate.fluidOutput() != null) {
                return candidate;
            }
        }
        return null;
    }

    /** 本机自述工序族：虚拟加工据此要求场域内确有活化器（见 {@link IMachineProcessKind}） */
    @Override
    public RecipeType<?> processKind() {
        return AkaishiRecipeTypes.ACTIVATING.get();
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_activator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiLifeActivatorMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    public SimpleContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：只接收生命能量（驱动活化），不对外输出 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return life;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    // ===== IFluidPipeDevice：混合接入——输入罐接废料管道，输出罐接普通管道 =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(inTank, outTank);
    }

    @Override
    public boolean canPipeExtract(FluidTank tank) {
        return tank == outTank;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return tank == inTank;
    }

    @Override
    public boolean acceptsBothFluidFamilies() {
        return true; // 废料进、活化液出，两族管道均可接入
    }

    @Override
    public boolean isWasteTank(FluidTank tank) {
        return tank == inTank; // 仅输入罐属废料家族
    }

    /** 输入罐内暂存的衰竭燃料总量（mb）：破坏泄漏按此占比定级 */
    public long getWasteAmount() {
        return inTank.getAmount();
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("LifeEnergy", life.getEnergyStored());
        tag.put("InTank", inTank.writeToNbt());
        tag.put("OutTank", outTank.writeToNbt());
        tag.putLong("Processed", processed);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        life.setEnergy(tag.getLong("LifeEnergy"));
        inTank.readFromNbt(tag.getCompound("InTank"));
        outTank.readFromNbt(tag.getCompound("OutTank"));
        processed = tag.getLong("Processed");
    }
}
