package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.item.AkaishiFuelCellItem;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiFuelCannerMenu;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 燃料装罐机方块实体（仅服务端驱动逻辑）。
 * 将输入罐中的液体燃料灌入空/半满燃料罐，灌满 10L（10000mb）后送出到输出槽。
 * 不消耗赤能源（纯物理灌装）；液体经液体管道注入通用输入罐（只进不出）。
 * 槽位：0 = 空/半满燃料罐（只进），1 = 满燃料罐成品（只出）。
 */
public class AkaishiFuelCannerBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IFluidPipeDevice, IItemPipeDevice, IDataCarrier {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    /** Menu 同步数据槽：0/1=输入液体量/容量 */
    public static final int DATA_SLOTS = 2;
    public static final int DATA_FLUID_AMOUNT = 0;
    public static final int DATA_FLUID_CAPACITY = 1;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final FluidTank liquidTank;

    public AkaishiFuelCannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUEL_CANNER.get(), pos, state);
        this.liquidTank = new FluidTank(ModConfig.fuelCannerTankCapacity) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiFuelCannerBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFuelCannerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(DATA_FLUID_AMOUNT, (int) liquidTank.getAmount());
        data.set(DATA_FLUID_CAPACITY, (int) liquidTank.getCapacity());

        ItemStack in = inventory.getItem(INPUT_SLOT);
        // 输入槽必须为燃料罐，输出槽必须空（成品满罐不可堆叠，仅占 1 格）
        if (in.isEmpty() || !(in.getItem() instanceof AkaishiFuelCellItem)) {
            return;
        }
        if (!inventory.getItem(OUTPUT_SLOT).isEmpty()) {
            return;
        }
        if (liquidTank.isEmpty()) {
            return;
        }

        Fluid tankFluid = liquidTank.getFluid();
        Fluid cellFluid = AkaishiFuelCellItem.getFluid(in);
        // 罐中已有其他液体时不可混装
        if (cellFluid != null && cellFluid != tankFluid) {
            return;
        }

        int current = AkaishiFuelCellItem.getAmount(in);
        int remaining = AkaishiFuelCellItem.CAPACITY - current;
        if (remaining <= 0) {
            return;
        }
        long toFill = Math.min(ModConfig.fuelCannerFillRate, Math.min(liquidTank.getAmount(), remaining));
        if (toFill <= 0) {
            return;
        }
        liquidTank.drain(toFill, false);
        AkaishiFuelCellItem.setFluid(in, tankFluid, current + (int) toFill);

        if (AkaishiFuelCellItem.getAmount(in) >= AkaishiFuelCellItem.CAPACITY) {
            // 灌满：成品 1 个送入输出槽，输入槽减 1（空罐堆叠逐个消耗）
            ItemStack result = in.copy();
            result.setCount(1);
            inventory.setItem(OUTPUT_SLOT, result);
            in.shrink(1);
            if (in.isEmpty()) {
                inventory.setItem(INPUT_SLOT, ItemStack.EMPTY);
            }
        }
        setChanged();
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_fuel_canner");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiFuelCannerMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        // 当前输入液体注册名（空串 = 无液体），客户端据此显示燃料名称
        Fluid fluid = liquidTank.getFluid();
        buf.writeUtf(fluid == null || fluid.isSame(net.minecraft.world.level.material.Fluids.EMPTY)
                ? "" : net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).toString());
    }

    // ===== 容器委托（管道/漏斗访问） =====

    public SimpleContainer inventory() {
        return inventory;
    }

    public SimpleContainerData data() {
        return data;
    }

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

    // ===== IItemPipeDevice：输入空罐 / 输出满罐 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    // 两接口均有同名默认方法，需显式合并（与液化装置一致）
    @Override
    public boolean canPipeInput() {
        return IFluidPipeDevice.super.canPipeInput() || IItemPipeDevice.super.canPipeInput();
    }

    @Override
    public boolean canPipeOutput() {
        return IFluidPipeDevice.super.canPipeOutput() || IItemPipeDevice.super.canPipeOutput();
    }

    // ===== IFluidPipeDevice：液体罐只可注入（防管道反向抽取液体） =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(liquidTank);
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
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("LiquidTank", liquidTank.writeToNbt());
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        liquidTank.readFromNbt(tag.getCompound("LiquidTank"));
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    /** 仅供调试工具读取（透明罐内容） */
    public FluidTank getLiquidTank() {
        return liquidTank;
    }
}
