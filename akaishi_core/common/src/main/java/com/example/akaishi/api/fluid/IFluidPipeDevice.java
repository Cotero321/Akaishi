package com.example.akaishi.api.fluid;

import com.example.akaishi.fluid.FluidTank;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 液体管道设备接口：持有液体罐、可被液体管道对接的方块实体实现此接口。
 * 液体管道扫描相邻方块时通过本接口统一获取液体罐，实现发送/接收与链式转发。
 * 设备通过 {@link #canPipeExtract}/{@link #canPipeInsert} 精确控制每个罐的流向，
 * 防止原料罐被抽空、产物罐被灌回（类似 IItemPipeDevice 的槽位控制）。
 * 废料专用设备（废品口/保存桶）通过 {@link #isWasteOnlyDevice()} 声明，仅废料管道可对接。
 */
public interface IFluidPipeDevice {

    /** 该设备暴露给液体管道的全部液体罐（顺序固定，与 GUI 显示一致）；实现不得返回 null */
    @NotNull
    List<FluidTank> getFluidTanks();

    /** 是否废料专用设备：true 时仅"废料管道家族"可对接，普通液体管道不可接入 */
    default boolean isWasteOnlyDevice() {
        return false;
    }

    /**
     * 是否混合接入设备：废料管道与普通液体管道均可连接，罐级家族由 {@link #isWasteTank} 区分。
     * 如生命活化器：废料罐接废料管道（进）、产物罐接普通管道（出）。
     */
    default boolean acceptsBothFluidFamilies() {
        return false;
    }

    /** 指定罐是否废料专用罐（仅废料管道可注入/抽取）；默认与整体 {@link #isWasteOnlyDevice()} 一致。
     *  混合接入设备应覆写：废料罐返回 true、普通罐返回 false。 */
    default boolean isWasteTank(FluidTank tank) {
        return isWasteOnlyDevice();
    }

    /**
     * 指定罐是否等离子体专用罐（仅等离子体管道可注入/抽取；与废料/普通家族互斥隔离）。
     * 等离子体设备（聚变燃料聚合器输出罐、离子体填装器输入罐）应覆写返回 true。
     */
    default boolean isPlasmaTank(FluidTank tank) {
        return false;
    }

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
