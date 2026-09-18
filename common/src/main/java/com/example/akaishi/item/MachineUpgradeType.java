package com.example.akaishi.item;

/**
 * 机器升级组件类型。
 * 每台用电器有 3 个升级槽（速度格 / 能量格 / 无线接收格），槽位 mayPlace 互斥，
 * 同类型组件单格最多堆叠 8 个，堆叠数即升级等级（无线接收升级只需 1 个）。
 * <p>
 * <b>枚举序数 = 槽位序数</b>（{@code MachineUpgradeSlots} 的 SLOT_* 常量与之对位），不可调整顺序。
 */
public enum MachineUpgradeType {
    /** 速度升级：每个 +100% 加工速度，8 个封顶 8× */
    SPEED,
    /** 能量升级：每个 +50% 能量容量，8 个封顶 +400%（×5） */
    ENERGY,
    /** 无线接收升级：装 1 个即让该机器加入无线场域，可被微缩矩阵终端调度 */
    WIRELESS
}
