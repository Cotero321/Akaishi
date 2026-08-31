package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.fluid.IFluidPipeDevice;
import com.example.template.fluid.FluidTank;
import com.example.template.config.ModConfig;
import com.example.template.fluid.ModFluids;
import com.example.template.fluid.MultiFluidTank;
import com.example.template.menu.ChishiExhaustedBarrelMenu;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 衰竭保存桶方块实体：专储衰竭燃料（容量 1000L，带 GUI 液位显示）。
 * 仅接受衰竭燃料（7 种），管道可注可抽，NBT 持久化液体。
 * 数据槽：0=总液体量 1=容量（供界面显示）。
 */
public class ChishiExhaustedBarrelBlockEntity extends BlockEntity implements ExtendedMenuProvider, IFluidPipeDevice, IDataCarrier {

    public static final int DATA_SLOTS = 2;
    public static final int DATA_AMOUNT = 0;
    public static final int DATA_CAPACITY = 1;

    private final MultiFluidTank tank;
    private final SimpleContainerData data;

    public ChishiExhaustedBarrelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_EXHAUSTED_BARREL.get(), pos, state);
        this.tank = new MultiFluidTank(ModConfig.exhaustedBarrelCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                // 专用储液：只接受衰竭燃料
                if (resource == null || !ModFluids.isExhaustedFuel(resource.getFluid())) {
                    return 0;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiExhaustedBarrelBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(DATA_AMOUNT, (int) tank.getAmount());
        data.set(DATA_CAPACITY, (int) tank.getCapacity());
    }

    public ContainerData data() {
        return data;
    }

    public MultiFluidTank tank() {
        return tank;
    }

    // ===== IFluidPipeDevice：唯一液体罐，可注可抽 =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(tank);
    }

    @Override
    public boolean isWasteOnlyDevice() {
        return true;
    }

    @Override
    public boolean canPipeExtract(FluidTank tank) {
        return tank == this.tank;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return tank == this.tank;
    }

    // ===== 菜单 / 序列化 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_exhausted_barrel");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiExhaustedBarrelMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Tank", tank.writeToNbt());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Tank")) {
            tank.readFromNbt(tag.getCompound("Tank"));
        }
    }
}
