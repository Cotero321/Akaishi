package com.example.akaishi.life.mechanical;

/**
 * 机械器官的五维属性体系。
 * 每个部件、材料、DNA都在这五个维度上拥有权重分布。
 */
public enum MechanicalProperty {
    /** 总体倍率：倍化其他四项属性的最终值，范围 1.0×~2.0×+ */
    OVERALL_MULTIPLIER,
    /** 生命值：额外最大生命值加成 */
    HEALTH,
    /** 攻击伤害：近战攻击力加成 */
    ATTACK_DAMAGE,
    /** 攻击速度：攻击频率加成 */
    ATTACK_SPEED,
    /** 移动速度：移动速度加成 */
    MOVEMENT_SPEED
}