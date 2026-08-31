package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.fluid.IFluidPipeDevice;
import com.example.template.fluid.FluidTank;
import com.example.template.config.ModConfig;
import com.example.template.fluid.ModFluids;
import com.example.template.fluid.MultiFluidTank;
import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 废品输出口方块实体：衰竭燃料缓冲罐（仅可被管道抽取，不可注入）。
 * 反应堆控制器每 tick 将产出的衰竭燃料灌入本罐，液体管道从此抽出运走。
 * NBT 持久化缓冲罐与控制器坐标。
 */
public class ChishiReactorWastePortBlockEntity extends BlockEntity implements IFluidPipeDevice, IDataCarrier {

    /** 缓冲容量由 {@link ModConfig#wastePortBufferCapacity} 提供 */

    private final MultiFluidTank wasteTank;
    private BlockPos controllerPos;

    public ChishiReactorWastePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_REACTOR_WASTE_PORT.get(), pos, state);
        this.wasteTank = new MultiFluidTank(ModConfig.wastePortBufferCapacity) {
            @Override
            public long fill(FluidStack resource, boolean simulate) {
                // 只接受衰竭燃料
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
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiReactorWastePortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 控制器被拆时清除缓存坐标，避免悬空引用
        if (controllerPos != null && !(level.getBlockEntity(controllerPos) instanceof ChishiReactorControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
    }

    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    /** 控制器灌入衰竭燃料，返回实际接收量 */
    public long acceptWaste(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        return wasteTank.fill(stack, false);
    }

    public MultiFluidTank wasteTank() {
        return wasteTank;
    }

    // ===== IFluidPipeDevice：唯一液体罐，只可抽取（废品输出）；仅废料管道可对接 =====

    @Override
    public List<FluidTank> getFluidTanks() {
        return List.of(wasteTank);
    }

    @Override
    public boolean isWasteOnlyDevice() {
        return true;
    }

    @Override
    public boolean canPipeExtract(FluidTank tank) {
        return tank == wasteTank;
    }

    @Override
    public boolean canPipeInsert(FluidTank tank) {
        return false;
    }

    /** 挖掘保留数据：反应堆废料不保留，仅排除旧控制器关联坐标与废液罐 */
    @Override
    public String[] excludedKeys() {
        return new String[]{"WasteTank", "ControllerPos"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("WasteTank", wasteTank.writeToNbt());
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("WasteTank")) {
            wasteTank.readFromNbt(tag.getCompound("WasteTank"));
        }
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
    }
}
