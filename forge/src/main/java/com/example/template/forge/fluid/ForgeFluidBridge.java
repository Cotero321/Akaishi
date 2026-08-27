package com.example.template.forge.fluid;

import com.example.template.api.fluid.IExternalFluidAccess;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * 外部液体访问桥（Forge 实现）：让液体管道对接非模组方块的液体能力。
 * common 无法直接接触 Forge capability 系统，因此本类在平台入口初始化时注入
 * {@link IExternalFluidAccess.FluidBridge#INSTANCE}。
 * 通过查询 ForgeCapabilities.FLUID_HANDLER 对接 MEK 储罐/机械管道、原版桶槽等
 * 一切标准 Forge 液体方块，实现"液体管道 ↔ MEK 管道/储罐"互通。
 */
public final class ForgeFluidBridge implements IExternalFluidAccess {

    /** 在 Forge 平台入口调用，注入桥实例（服务端/客户端各执行一次，读取侧已做客户端防护） */
    public static void init() {
        IExternalFluidAccess.FluidBridge.INSTANCE = new ForgeFluidBridge();
    }

    private ForgeFluidBridge() {
    }

    @Override
    public ExternalFluidTank getTank(Level level, BlockPos pos, Direction side) {
        if (level == null || pos == null || level.isClientSide) {
            return null; // capability 数据仅服务端可靠
        }
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        java.util.Optional<IFluidHandler> handler =
                be.getCapability(ForgeCapabilities.FLUID_HANDLER, side).resolve();
        return handler.map(HandlerView::new).filter(h -> h.getCapacity() > 0).orElse(null);
    }

    /**
     * 把多槽 IFluidHandler 聚合成"单一罐"视图：管道网络以统一视角对接外部方块。
     * 容量/液量为各槽求和，注入按槽位依次分配，抽取时优先抽第一个含液槽。
     */
    private record HandlerView(IFluidHandler handler) implements ExternalFluidTank {

        @Override
        public Fluid getFluid() {
            for (int i = 0; i < handler.getTanks(); i++) {
                net.minecraftforge.fluids.FluidStack stack = handler.getFluidInTank(i);
                if (!stack.isEmpty()) {
                    return stack.getFluid();
                }
            }
            return Fluids.EMPTY;
        }

        @Override
        public long getAmount() {
            long total = 0;
            for (int i = 0; i < handler.getTanks(); i++) {
                total += handler.getFluidInTank(i).getAmount();
            }
            return total;
        }

        @Override
        public long getCapacity() {
            long total = 0;
            for (int i = 0; i < handler.getTanks(); i++) {
                total += handler.getTankCapacity(i);
            }
            return total;
        }

        @Override
        public long fill(FluidStack resource, boolean simulate) {
            if (resource == null || resource.isEmpty()) {
                return 0;
            }
            net.minecraftforge.fluids.FluidStack work = FluidStackHooksForge.toForge(resource).copy();
            long remaining = resource.getAmount();
            IFluidHandler.FluidAction action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
            for (int i = 0; i < handler.getTanks() && remaining > 0; i++) {
                int filled = handler.fill(work, action);
                if (filled > 0) {
                    work.shrink(filled);
                    remaining -= filled;
                }
            }
            return resource.getAmount() - remaining;
        }

        @Override
        public FluidStack drain(long amount, boolean simulate) {
            if (amount <= 0) {
                return FluidStack.empty();
            }
            IFluidHandler.FluidAction action = simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE;
            net.minecraftforge.fluids.FluidStack drained = handler.drain((int) Math.min(amount, Integer.MAX_VALUE), action);
            return FluidStackHooksForge.fromForge(drained);
        }
    }
}
