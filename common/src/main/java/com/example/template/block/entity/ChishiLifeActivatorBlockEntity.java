package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.api.fluid.IFluidPipeDevice;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.LifeEnergyType;
import com.example.template.fluid.FluidTank;
import com.example.template.config.ModConfig;
import com.example.template.fluid.ModFluids;
import com.example.template.sound.ModSounds;
import com.example.template.fluid.MultiFluidTank;
import com.example.template.menu.ChishiLifeActivatorMenu;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 生命活化器方块实体（仅服务端驱动逻辑）。
 * 缓慢无害化衰竭燃料：废料管道将衰竭燃料注入输入罐，每 tick 至多转化 4mb
 * 为对应的"活化衰竭液体"（1:1），每 1mb 消耗 100 生命能量。
 * 活化液体为安全中间产物，由普通液体管道从输出罐抽取。
 * 混合接入设备：输入罐接废料管道家族、输出罐接普通液体管道家族（罐级家族隔离）。
 */
public class ChishiLifeActivatorBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice {

    /**
     * Menu 同步数据槽：0/1=生命能量/容量 2/3=输入量/容量 4/5=输出量/容量
     * 6/7=累计活化量（低 32 位 / 高 32 位，long 拆两槽无损同步，避免 int 溢出）
     */
    public static final int DATA_SLOTS = 8;
    public static final int DATA_LIFE_ENERGY = 0;
    public static final int DATA_LIFE_CAPACITY = 1;
    public static final int DATA_IN_AMOUNT = 2;
    public static final int DATA_IN_CAPACITY = 3;
    public static final int DATA_OUT_AMOUNT = 4;
    public static final int DATA_OUT_CAPACITY = 5;
    public static final int DATA_PROCESSED_LOW = 6;
    public static final int DATA_PROCESSED_HIGH = 7;

    /** 生命能量容量 / 每 1mb 转化成本 / 罐容量 / 转化速率均由 {@link com.example.template.config.ModConfig} 提供 */

    private final SimpleContainerData data;
    private final ChishiEnergyStorage life;
    /** 输入罐：废料专用多液体罐（仅衰竭燃料可入，7 种可混） */
    private final MultiFluidTank inTank;
    /** 输出罐：活化液体多液体罐（仅活化液体可入，7 种可混） */
    private final MultiFluidTank outTank;
    /** 累计活化量（mb，仅用于 GUI 展示无害化进度） */
    private long processed;
    /** 活化运转音播放冷却（tick） */
    private int soundCooldown;

    public ChishiLifeActivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_ACTIVATOR.get(), pos, state);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, ModConfig.lifeActivatorLifeCapacity);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiLifeActivatorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(DATA_LIFE_ENERGY, (int) life.getEnergyStored());
        data.set(DATA_LIFE_CAPACITY, (int) life.getMaxEnergy());
        data.set(DATA_IN_AMOUNT, (int) inTank.getAmount());
        data.set(DATA_IN_CAPACITY, (int) inTank.getCapacity());
        data.set(DATA_OUT_AMOUNT, (int) outTank.getAmount());
        data.set(DATA_OUT_CAPACITY, (int) outTank.getCapacity());

        Fluid fuel = inTank.getFluid();
        Fluid activated = ModFluids.activatedFuelFor(fuel);
        if (fuel == null || fuel == Fluids.EMPTY || activated == null || activated == Fluids.EMPTY) {
            return; // 无废料输入，静默等待
        }
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
        // long 拆低/高 32 位两槽同步，GUI 侧重组，避免单槽 int 溢出丢失精度
        data.set(DATA_PROCESSED_LOW, (int) processed);
        data.set(DATA_PROCESSED_HIGH, (int) (processed >>> 32));
        setChanged();
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_life_activator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ChishiLifeActivatorMenu(containerId, inventory, this);
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
