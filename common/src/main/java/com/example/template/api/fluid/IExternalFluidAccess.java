package com.example.template.api.fluid;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * 外部液体访问桥：液体管道访问非模组方块（MEK 储罐、原版方块等）的液体能力。
 * common 模块无法直接接触 Forge 的 capability 系统，因此由 Forge 平台实现
 * {@link IExternalFluidAccess} 并在初始化时注入 {@link FluidBridge#INSTANCE}。
 * 管道 tick 中：模组设备走 {@link IFluidPipeDevice}，其余方块走本桥。
 */
public interface IExternalFluidAccess {

    /** 从指定方向访问邻居方块的液体罐（无液体能力时返回 null） */
    @Nullable
    ExternalFluidTank getTank(Level level, BlockPos pos, Direction side);

    /** 外部液体罐视图：只读状态 + 注入/抽取（平台实现内部转换为 capability 操作） */
    interface ExternalFluidTank {
        Fluid getFluid();

        long getAmount();

        long getCapacity();

        /** 注入液体，返回实际注入量 */
        long fill(FluidStack resource, boolean simulate);

        /** 抽取液体，返回实际抽出的液体栈 */
        FluidStack drain(long amount, boolean simulate);
    }

    /** 平台桥注册点：Forge 初始化时设置，管道据此访问第三方液体 */
    final class FluidBridge {
        /** volatile 保证跨线程可见；平台初始化时仅注入一次，运行期不再变更 */
        @Nullable
        public static volatile IExternalFluidAccess INSTANCE;

        private FluidBridge() {
        }
    }
}