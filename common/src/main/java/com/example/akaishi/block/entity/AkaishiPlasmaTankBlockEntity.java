package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.fluid.FluidTank;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.menu.AkaishiFluidTankMenu;
import com.example.akaishi.menu.ModMenus;
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
 * 等离子体燃料储罐方块实体：单个等离子体专用罐。
 * 罐层拒收非等离子体液体（fill 覆写），管道对接仅限等离子体管道（isPlasmaTank=true）。
 * 数据槽：0=液体量 1=容量（供界面显示，复用液体储罐界面）。
 */
public class AkaishiPlasmaTankBlockEntity extends BlockEntity implements ExtendedMenuProvider, IFluidPipeDevice, IDataCarrier {

    public static final int DATA_SLOTS = 2;
    public static final int DATA_AMOUNT = 0;
    public static final int DATA_CAPACITY = 1;

    /** 容量：16000mb（与基础液体储罐同级） */
    private static final long CAPACITY = 16_000L;

    private final FluidTank tank;
    private final SimpleContainerData data;

    public AkaishiPlasmaTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PLASMA_TANK.get(), pos, state);
        this.tank = new FluidTank(CAPACITY) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                if (resource == null || !ModFluids.isPlasma(resource.getFluid())) {
                    return 0; // 仅接纳等离子体，其余液体拒收
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPlasmaTankBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(DATA_AMOUNT, (int) tank.getAmount());
        data.set(DATA_CAPACITY, (int) tank.getCapacity());
    }

    public ContainerData data() {
        return data;
    }

    // ===== IFluidPipeDevice：唯一等离子体专用罐，可注可抽 =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(tank);
    }

    @Override
    public boolean isPlasmaTank(FluidTank tank) {
        return tank == this.tank;
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
        return Component.translatable("block.akaishi.akaishi_plasma_tank");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiFluidTankMenu(ModMenus.CHISHI_PLASMA_TANK.get(), id, inv, data);
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
