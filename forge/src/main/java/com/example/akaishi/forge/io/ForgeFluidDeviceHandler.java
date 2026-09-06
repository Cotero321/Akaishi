package com.example.akaishi.forge.io;

import com.example.akaishi.api.fluid.IFluidPipeDevice;
import com.example.akaishi.fluid.FluidTank;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 液体管道设备（{@link IFluidPipeDevice}）的 Forge 第三方液体能力视图。
 * <p>
 * 把设备声明的罐级方向原样映射为 FLUID_HANDLER：
 * - 注入只落到「可注入罐」（canPipeInsert），产物罐/仅输出设备拒绝被灌；
 * - 抽取只来自「可抽取罐」（canPipeExtract），原料罐/仅输入设备抽不到；
 * - 家族隔离：废料 / 等离子体专用罐（isWasteTank / isPlasmaTank）不向第三方开放——
 *   第三方管道无法表达家族语义，开放会把普通液体灌进废料罐造成污染；
 *   保存桶、废品口等废料专用设备因此整体不出现在第三方能力中（仍可走自家废料管道）。
 */
public final class ForgeFluidDeviceHandler implements IFluidHandler {

    private final IFluidPipeDevice device;
    private final List<FluidTank> tanks;

    public ForgeFluidDeviceHandler(IFluidPipeDevice device) {
        this.device = device;
        this.tanks = device.getFluidTanks();
    }

    private boolean isOpen(int index) {
        if (index < 0 || index >= tanks.size()) {
            return false;
        }
        FluidTank tank = tanks.get(index);
        return !device.isWasteTank(tank) && !device.isPlasmaTank(tank);
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        if (!isOpen(tank)) {
            return FluidStack.EMPTY;
        }
        return FluidStackHooksForge.toForge(tanks.get(tank).getStack());
    }

    @Override
    public int getTankCapacity(int tank) {
        if (!isOpen(tank)) {
            return 0;
        }
        return (int) Math.min(tanks.get(tank).getCapacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return isOpen(tank) && stack != null && !stack.isEmpty();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        long filled = 0;
        dev.architectury.fluid.FluidStack work = FluidStackHooksForge.fromForge(resource).copy();
        for (int i = 0; i < tanks.size() && !work.isEmpty(); i++) {
            if (!isOpen(i) || !device.canPipeInsert(tanks.get(i))) {
                continue; // 家族隔离 + 方向：仅普通家族的可注入罐可收液
            }
            long f = tanks.get(i).fill(work, action.simulate());
            if (f > 0) {
                work.shrink(f);
                filled += f;
            }
        }
        return (int) filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        for (int i = 0; i < tanks.size(); i++) {
            FluidTank tank = tanks.get(i);
            if (!isOpen(i) || !device.canPipeExtract(tank) || tank.isEmpty()) {
                continue;
            }
            return FluidStackHooksForge.toForge(
                    tank.drain(FluidStackHooksForge.fromForge(resource), action.simulate()));
        }
        return FluidStack.EMPTY;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        for (int i = 0; i < tanks.size(); i++) {
            FluidTank tank = tanks.get(i);
            if (!isOpen(i) || !device.canPipeExtract(tank) || tank.isEmpty()) {
                continue;
            }
            return FluidStackHooksForge.toForge(tank.drain(maxDrain, action.simulate()));
        }
        return FluidStack.EMPTY;
    }
}
