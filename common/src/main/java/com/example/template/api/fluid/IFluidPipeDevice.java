package com.example.template.api.fluid;

import com.example.template.fluid.FluidTank;

import java.util.List;

/**
 * 液体管道设备接口：持有液体罐、可被液体管道对接的方块实体实现此接口。
 * 液体管道扫描相邻方块时通过本接口统一获取液体罐，实现发送/接收与链式转发。
 * 设备通过 {@link #canPipeExtract}/{@link #canPipeInsert} 精确控制每个罐的流向，
 * 防止原料罐被抽空、产物罐被灌回（类似 IItemPipeDevice 的槽位控制）。
 */
public interface IFluidPipeDevice {

    /** 该设备暴露给液体管道的全部液体罐（顺序固定，与 GUI 显示一致） */
    List<FluidTank> getFluidTanks();

    /** 液体管道是否可从该罐抽取液体（产物罐应为 true，原料罐应为 false） */
    default boolean canPipeExtract(FluidTank tank) {
        return true;
    }

    /** 液体管道是否可向该罐注入液体（原料罐应为 true，产物罐应为 false） */
    default boolean canPipeInsert(FluidTank tank) {
        return true;
    }

    /** 是否存在可被管道注入的罐 */
    default boolean canPipeInput() {
        for (FluidTank tank : getFluidTanks()) {
            if (canPipeInsert(tank) && !tank.isFull()) {
                return true;
            }
        }
        return false;
    }

    /** 是否存在可被管道抽取的罐 */
    default boolean canPipeOutput() {
        for (FluidTank tank : getFluidTanks()) {
            if (canPipeExtract(tank) && !tank.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
