package com.example.akaishi.api.storage;

/**
 * 物品终端赤能源接入口契约（D13 多口并联）。
 * <p>
 * 口自带缓冲，由外部能量管道注入；终端在发生存入 / 取出结算时**主动**抽取各口能量
 * （{@code absorbEnergyFromPorts()}），口本身不向终端推送 —— 单一能量入口路径，
 * 避免「每 tick 推送 + 操作时汇聚」并存导致同一份能量被计两次。
 */
public interface IItemTerminalEnergyPort {

    /** 口内当前可用能量（赤能源） */
    long availableEnergy();

    /**
     * 抽取口内能量（真实写入）。
     *
     * @param amount 尝试抽取量
     * @return 实际抽出量
     */
    long drainEnergy(long amount);
}
