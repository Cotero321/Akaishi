package com.example.akaishi.forge.fluid;

import com.example.akaishi.block.entity.AkaishiFluidPipeBlockEntity;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 液体管道的 Forge 液体能力视图：暴露内部缓冲罐，供 MEK 等外部管道直接注入/抽取。
 * 罐级家族过滤由缓冲罐自身 fill 覆写继承（普通拒收废料/等离子体，废料/等离子体管道仅收本族）。
 */
public final class ForgeFluidHandler implements IFluidHandler {

    private final AkaishiFluidPipeBlockEntity pipe;

    public ForgeFluidHandler(AkaishiFluidPipeBlockEntity pipe) {
        this.pipe = pipe;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return FluidStackHooksForge.toForge(pipe.buffer().getStack());
    }

    @Override
    public int getTankCapacity(int tank) {
        return (int) Math.min(pipe.buffer().getCapacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return stack != null && !stack.isEmpty();
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        long filled = pipe.buffer().fill(FluidStackHooksForge.fromForge(resource), action.simulate());
        return (int) filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return FluidStackHooksForge.toForge(
                pipe.buffer().drain(FluidStackHooksForge.fromForge(resource), action.simulate()));
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        return FluidStackHooksForge.toForge(pipe.buffer().drain(maxDrain, action.simulate()));
    }
}
