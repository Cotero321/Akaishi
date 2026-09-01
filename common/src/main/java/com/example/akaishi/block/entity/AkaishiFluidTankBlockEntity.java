package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.block.AkaishiFluidTankBlock;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.FluidTankTier;
import com.example.akaishi.menu.AkaishiFluidTankMenu;
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
 * 液体储罐方块实体：单液体罐存储（双向：管道可注可抽）。
 * 容量由方块等级 {@link FluidTankTier} 决定，NBT 持久化液体。
 * 数据槽：0=液体量 1=容量（供界面显示）。
 */
public class AkaishiFluidTankBlockEntity extends BlockEntity implements ExtendedMenuProvider, IFluidPipeDevice, IDataCarrier {

    public static final int DATA_SLOTS = 2;
    public static final int DATA_AMOUNT = 0;
    public static final int DATA_CAPACITY = 1;

    private final FluidTankTier tier;
    private final FluidTank tank;
    private final SimpleContainerData data;

    public AkaishiFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FLUID_TANK.get(), pos, state);
        this.tier = ((AkaishiFluidTankBlock) state.getBlock()).getTier();
        this.tank = new FluidTank(tier.capacity) {
            @Override
            protected void onChanged() {
                setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFluidTankBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(DATA_AMOUNT, (int) tank.getAmount());
        data.set(DATA_CAPACITY, (int) tank.getCapacity());
    }

    public ContainerData data() {
        return data;
    }

    // ===== IFluidPipeDevice：唯一液体罐，可注可抽 =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(tank);
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
        return Component.translatable("block.akaishi." + switch (tier) {
            case BASIC -> "akaishi_fluid_tank_basic";
            case ADVANCED -> "akaishi_fluid_tank_advanced";
            case SUPER -> "akaishi_fluid_tank_super";
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiFluidTankMenu(id, inv, data);
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
        tank.readFromNbt(tag.getCompound("Tank"));
    }
}
