package com.example.akaishi.api.energy;

/**
 * 按需补能标记：实现本接口的机器<b>不参与场域常态直供</b>，
 * 只在 {@link #requestsWirelessTopUp()} 返回 true 时才会被微缩矩阵终端补能。
 * <p>
 * <b>为什么需要它</b>：场域常态直供是"细水长流"（每轮每台上限 {@code PER_MACHINE_ENERGY}），
 * 适合每 tick 耗能几千~几万的一般机器；而<b>池式高耗能机器</b>（如生命提纯器每 tick 抽百万级）
 * 被这样喂等于没喂 —— 供进去立刻抽干，界面永远显示 0，玩家会误判"没有实装"。
 * 这类机器应当自述"我要不要能量"，由加工时机决定，而不是常态占着场域供给。
 * <p>
 * <b>归属</b>：本接口必须与 {@link IEnergyProvider} 同包同模块（{@code akaishi_core}）——
 * 放进 {@code common} 会造成同一包被两个模块导出，触发 JPMS 分裂包错误而无法启动。
 */
public interface IWirelessEnergyDemand {

    /**
     * 本轮是否需要场域补能。
     * <p>
     * 返回 false（如空闲、输出已满、原料不足）时终端会跳过本机，不消耗芯片能量；
     * 返回 true 时终端按<b>容量比例</b>一轮补足（见直供实现里的单轮上限）。
     */
    boolean requestsWirelessTopUp();
}
