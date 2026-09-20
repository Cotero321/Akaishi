package com.example.akaishi.craft;

import com.example.akaishi.api.energy.IEnergyType;

/**
 * 虚拟加工的机器能量来源：矩阵场域内「按类型匹配的芯片能量池」。
 *
 * <p><b>为什么不走物品终端</b>：物品终端的赤能源缓冲只负责结算存取手续费（默认 1M 量级，
 * 加上缓冲扩展组件至多 9M），而机器能耗（聚合器/提纯机一笔可达 10M）必须在能量芯片的大池子里付 ——
 * 赤能源扣赤能源芯片、生命能量扣生命能量芯片，与"能量都在各自终端里"的口径一致。
 *
 * <p><b>为什么单独一个接口</b>：{@code VirtualCraftTask} 只依赖"能查余额、能原子扣款"这两件事，
 * 不必知道矩阵的芯片结构（DIP）。
 */
public interface ICraftEnergyPool {

    /** 该类能量在池里的可动用总量（开工前预检用） */
    long availableEnergy(IEnergyType type);

    /**
     * 扣除能量；<b>不足时整笔拒绝，不做部分扣</b>。
     * <p>调用方必须先用 {@link #availableEnergy} 预检 —— 本接口没有退能能力，
     * 与 {@code IItemTerminalHost#tryChargeFee} 同一套"预检后再扣"的纪律。
     *
     * @return true 表示已扣除（返回 false 时池子零改动）
     */
    boolean consumeEnergy(IEnergyType type, long amount);
}
