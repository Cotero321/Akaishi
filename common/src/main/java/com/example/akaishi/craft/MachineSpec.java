package com.example.akaishi.craft;

/**
 * 一台已入网机台的升级配置（虚拟加工据此核算能耗与耗时）。
 *
 * <p>只存"件数"，倍率换算一律放在 {@link MachineProcessEnergy} —— 那里同时持有全局配置
 * （{@code [machine] workSpeed} / {@code costMultiplier}），倍率口径必须只有一处。
 *
 * @param speedCount  速度升级件数（0~8）
 * @param energyCount 能量升级件数（0~8；只抬容量，不影响虚拟加工的成本与时间）
 */
public record MachineSpec(int speedCount, int energyCount) {

    /** 无升级机台：外部无场域信息（列表预筛等）时的基准 */
    public static final MachineSpec BASE = new MachineSpec(0, 0);

    public static MachineSpec of(int speedCount, int energyCount) {
        return new MachineSpec(Math.max(0, speedCount), Math.max(0, energyCount));
    }
}
