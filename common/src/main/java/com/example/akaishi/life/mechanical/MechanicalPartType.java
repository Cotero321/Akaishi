package com.example.akaishi.life.mechanical;

/**
 * 机械器官的四个部件类型。
 * 每个机械器官/义体由四个部件组合而成，各部件主导不同属性维度。
 */
public enum MechanicalPartType {
    /** 核心：决定基础性能与输出强度 */
    CORE,
    /** 模块：提供功能特化与差异化能力 */
    MODULE,
    /** 外壳：提供结构防护与稳定性 */
    SHELL,
    /** 散热：管理热量与能耗效率 */
    COOLING
}