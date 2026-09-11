package com.example.akaishi.life.mechanical;

/**
 * 机械器官的十维权重体系（九属性 + 一总体倍率轴）。
 * 每个部件、材料、DNA 都在这十个维度上拥有权重分布。
 */
public enum MechanicalProperty {
    /** 总体倍率：倍化其余九项属性的最终值，范围 1.0×~2.0×+（不受整合度折扣） */
    OVERALL_MULTIPLIER,
    /** 生命值：额外最大生命值加成 */
    HEALTH,
    /** 攻击伤害：近战攻击力加成 */
    ATTACK_DAMAGE,
    /** 攻击速度：攻击频率加成 */
    ATTACK_SPEED,
    /** 移动速度：移动速度加成 */
    MOVEMENT_SPEED,
    /** 护甲：护甲值加成 */
    ARMOR,
    /** 暴击率：暴击概率加成 */
    CRIT_CHANCE,
    /** 暴击伤害：暴击倍率加成 */
    CRIT_DAMAGE,
    /** 攻击范围：交互/攻击距离加成 */
    RANGE,
    /** 闪避：闪避概率加成 */
    DODGE;

    /** 权重槽总数（含总体倍率轴） */
    public static final int COUNT = values().length;

    /** 悬浮文本本地化键（部件权重与成品属性共用同一套标签） */
    public String tooltipKey() {
        return switch (this) {
            case OVERALL_MULTIPLIER -> "tooltip.akaishi.mechanical.om";
            case HEALTH -> "tooltip.akaishi.mechanical.hp";
            case ATTACK_DAMAGE -> "tooltip.akaishi.mechanical.ad";
            case ATTACK_SPEED -> "tooltip.akaishi.mechanical.as";
            case MOVEMENT_SPEED -> "tooltip.akaishi.mechanical.ms";
            case ARMOR -> "tooltip.akaishi.mechanical.armor";
            case CRIT_CHANCE -> "tooltip.akaishi.mechanical.crit";
            case CRIT_DAMAGE -> "tooltip.akaishi.mechanical.crit_damage";
            case RANGE -> "tooltip.akaishi.mechanical.range";
            case DODGE -> "tooltip.akaishi.mechanical.dodge";
        };
    }
}
