package com.example.akaishi.life.mechanical;

import java.util.List;

/**
 * 机械器官/义体的聚合最终属性。
 * 由组装加工台在四个部件的基础上计算得出。
 * 包含：聚合五维值、总体倍率系数、触发的协同加成。
 *
 * 整合度折扣：通过 {@link #applyIntegration(double)} 获取整合后的实际生效值。
 * 0% 整合度 = 20% 生效，100% 整合度 = 100% 生效。
 */
public class MechanicalAssembledStats {

    private final MechanicalOrganType organType;
    private final double overallMultiplier;   // 总体倍率系数 1.0~2.0+
    private final double health;              // 最终生命值加成（满整合）
    private final double attackDamage;        // 最终攻击伤害加成（满整合）
    private final double attackSpeed;         // 最终攻击速度加成（满整合）
    private final double movementSpeed;       // 最终移动速度加成（满整合）
    private final List<String> synergyBonuses; // 已触发的协同加成描述键
    private final List<MechanicalSpecialEffect> effects; // 激活的特殊效果

    public MechanicalAssembledStats(MechanicalOrganType organType,
                                    double overallMultiplier, double health,
                                    double attackDamage, double attackSpeed,
                                    double movementSpeed,
                                    List<String> synergyBonuses,
                                    List<MechanicalSpecialEffect> effects) {
        this.organType = organType;
        this.overallMultiplier = overallMultiplier;
        this.health = health;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.movementSpeed = movementSpeed;
        this.synergyBonuses = synergyBonuses;
        this.effects = effects;
    }

    public MechanicalOrganType organType() { return organType; }
    public double overallMultiplier() { return overallMultiplier; }
    public double health() { return health; }
    public double attackDamage() { return attackDamage; }
    public double attackSpeed() { return attackSpeed; }
    public double movementSpeed() { return movementSpeed; }
    public List<String> synergyBonuses() { return synergyBonuses; }
    public List<MechanicalSpecialEffect> effects() { return effects; }

    /** 获取指定属性的满整合值 */
    public double get(MechanicalProperty prop) {
        return switch (prop) {
            case OVERALL_MULTIPLIER -> overallMultiplier;
            case HEALTH -> health;
            case ATTACK_DAMAGE -> attackDamage;
            case ATTACK_SPEED -> attackSpeed;
            case MOVEMENT_SPEED -> movementSpeed;
        };
    }

    /**
     * 应用整合度折扣，返回整合后的实际生效值副本。
     * 整合度决定机械器官的实际生效比例，与生物排异不同，整合度只涨不降。
     *
     * @param integrationMultiplier 整合度系数，0.2~1.0（由 MechanicalIntegrationService 提供）
     * @return 整合后的实际生效属性
     */
    public MechanicalAssembledStats applyIntegration(double integrationMultiplier) {
        return new MechanicalAssembledStats(
                organType,
                overallMultiplier, // 总体倍率本身不受整合度影响（它是乘算基础）
                health * integrationMultiplier,
                attackDamage * integrationMultiplier,
                attackSpeed * integrationMultiplier,
                movementSpeed * integrationMultiplier,
                synergyBonuses,
                effects
        );
    }

    /**
     * 获取整合后的实际属性值。
     * 总体倍率不受整合度影响。
     *
     * @param prop 属性类型
     * @param integrationMultiplier 整合度系数 0.2~1.0
     * @return 整合后的实际值
     */
    public double getEffective(MechanicalProperty prop, double integrationMultiplier) {
        if (prop == MechanicalProperty.OVERALL_MULTIPLIER) {
            return overallMultiplier;
        }
        return get(prop) * integrationMultiplier;
    }
}