package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;
import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.api.fluid.IFluidPipeDevice;
import com.example.template.config.ModConfig;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.fluid.FluidTank;
import com.example.template.fluid.ModFluids;
import com.example.template.fluid.MultiFluidTank;
import com.example.template.item.ModItems;
import com.example.template.menu.ChishiLifeCentrifugeMenu;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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

import java.util.List;

/**
 * 生命离心机方块实体（仅服务端驱动逻辑）。
 * 将活化衰竭液体分离为两类结晶：1 个对应活化结晶（主产物）+ 1 个衰竭结晶（通用副产物）。
 * 每 100mb 活化燃料产出各 1 个；每 tick 至多分离 8mb，每 1mb 消耗 50 赤能源。
 * 输入罐仅接纳活化燃料（普通液体管道注入），物品管道不可接入，输出物由玩家从 GUI 取出。
 */
public class ChishiLifeCentrifugeBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IFluidPipeDevice, IDataCarrier {

    /** 每批产出所需的活化燃料量（mb） */
    public static final long BATCH_MB = 100L;

    // ===== 数据槽 =====
    public static final int DATA_SLOTS = 5;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_IN_AMOUNT = 2;
    public static final int DATA_IN_CAPACITY = 3;
    /** 当前批次累计分离量（mb，满 BATCH_MB 结算一次） */
    public static final int DATA_PROGRESS = 4;

    private final SimpleContainerData data;
    private final ChishiEnergyStorage energy;
    /** 输入罐：仅接纳活化衰竭液体（7 种可混） */
    private final MultiFluidTank inTank;
    /** 输出槽：0=活化结晶（主），1=衰竭结晶（副） */
    private final SimpleContainer output = new SimpleContainer(2);
    private long progress;

    public ChishiLifeCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_CENTRIFUGE.get(), pos, state);
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, ModConfig.lifeCentrifugeEnergyCapacity);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiLifeCentrifugeBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_ENERGY_CAPACITY, (int) energy.getMaxEnergy());
        data.set(DATA_IN_AMOUNT, (int) inTank.getAmount());
        data.set(DATA_IN_CAPACITY, (int) inTank.getCapacity());
        data.set(DATA_PROGRESS, (int) progress);

        Fluid fluid = inTank.getFluid();
        if (fluid == null || !ModFluids.isActivatedFuel(fluid)) {
            return; // 无活化燃料，静默等待
        }
        Item main = crystalFor(fluid);
        if (main == null || !canFit(0, main) || !canFit(1, ModItems.exhaustedCrystal.get())) {
            return; // 输出槽不可容纳完整一批，暂停分离
        }
        long afford = energy.getEnergyStored() / ModConfig.lifeCentrifugeCostPerMb;
        long rate = Math.min(Math.min(ModConfig.lifeCentrifugeConvertRate, inTank.getAmount(fluid)), afford);
        if (rate <= 0) {
            return;
        }
        inTank.drain(fluid, rate, false);
        energy.extractEnergy(rate * ModConfig.lifeCentrifugeCostPerMb, false);
        progress += rate;
        // 每满一批结算一次（速率低于阈值，单 tick 至多结算 1 批）
        while (progress >= BATCH_MB) {
            progress -= BATCH_MB;
            addOutput(0, main);
            addOutput(1, ModItems.exhaustedCrystal.get());
        }
        setChanged();
    }

    /** 活化燃料 → 对应活化结晶；非七种活化燃料返回 null */
    private static Item crystalFor(Fluid fluid) {
        if (fluid == ModFluids.get(ModFluids.ACTIVATED_EXHAUSTED_SCULK_FUEL_ID)) {
            return ModItems.activatedSculkCrystal.get();
        }
        if (fluid == ModFluids.get(ModFluids.ACTIVATED_EXHAUSTED_NETHER_COMPOUND_FUEL_ID)) {
            return ModItems.activatedNetherCompoundCrystal.get();
        }
        if (fluid == ModFluids.get(ModFluids.ACTIVATED_EXHAUSTED_END_MIXTURE_FUEL_ID)) {
            return ModItems.activatedEndMixtureCrystal.get();
        }
        if (fluid == ModFluids.get(ModFluids.ACTIVATED_EXHAUSTED_ADVANCED_MIXTURE_FUEL_ID)) {
            return ModItems.activatedAdvancedMixtureCrystal.get();
        }
        if (fluid == ModFluids.get(ModFluids.ACTIVATED_EXHAUSTED_PURE_FUEL_ID)) {
            return ModItems.activatedPureCrystal.get();
        }
        if (fluid == ModFluids.get(ModFluids.ACTIVATED_EXHAUSTED_DRAGON_FUEL_ID)) {
            return ModItems.activatedDragonCrystal.get();
        }
        if (fluid == ModFluids.get(ModFluids.ACTIVATED_EXHAUSTED_ULTIMATE_MIXTURE_FUEL_ID)) {
            return ModItems.activatedUltimateMixtureCrystal.get();
        }
        return null;
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

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_life_centrifuge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ChishiLifeCentrifugeMenu(containerId, inventory, this);
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
        return type == ChishiEnergyType.INSTANCE;
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
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        inTank.readFromNbt(tag.getCompound("InTank"));
        output.fromTag(tag.getList("Output", 10));
        progress = tag.getLong("Progress");
    }
}
