package com.example.akaishi.item;

/**
 * 机器升级组件类型。
 * 每台用电器有 2 个升级槽（速度格/能量格），槽位 mayPlace 互斥，
 * 同类型组件单格最多堆叠 8 个，堆叠数即升级等级。
 */
public enum MachineUpgradeType {
    /** 速度升级：每个 +12.5% 加工速度，8 个封顶 +100% */
    SPEED,
    /** 能量升级：每个 +50% 能量容量，8 个封顶 +400%（×5） */
    ENERGY
}
