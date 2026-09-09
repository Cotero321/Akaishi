package com.example.akaishi.life.mechanical;

/**
 * 机械部件的五维权重分布，五项和为固定值。
 * 每项权重范围 0~5，确保"各有专攻，无法全满"。
 * 权重值在不同阶段叠加：基础权重 + 材料点数 + DNA修正。
 */
public record MechanicalPartWeight(
        int overallMultiplier,  // 总体倍率权重 0~5
        int health,             // 生命值权重 0~5
        int attackDamage,       // 攻击伤害权重 0~5
        int attackSpeed,        // 攻击速度权重 0~5
        int movementSpeed       // 移动速度权重 0~5
) {
    /**
     * 验证权重值是否合法（每项 -1~5；-1 仅供 DNA 修正使用，权重表本身仍取值 0~5）。
     *
     * @throws IllegalArgumentException 如果有任何维度超出范围
     */
    public MechanicalPartWeight {
        validateRange(overallMultiplier, "overallMultiplier");
        validateRange(health, "health");
        validateRange(attackDamage, "attackDamage");
        validateRange(attackSpeed, "attackSpeed");
        validateRange(movementSpeed, "movementSpeed");
    }

    private static void validateRange(int value, String name) {
        if (value < -1 || value > 5) {
            throw new IllegalArgumentException(
                    "MechanicalProperty " + name + " must be -1~5, got " + value);
        }
    }

    /** 五项权重之和 */
    public int sum() {
        return overallMultiplier + health + attackDamage + attackSpeed + movementSpeed;
    }

    /** 获取指定属性的权重值 */
    public int get(MechanicalProperty prop) {
        return switch (prop) {
            case OVERALL_MULTIPLIER -> overallMultiplier;
            case HEALTH -> health;
            case ATTACK_DAMAGE -> attackDamage;
            case ATTACK_SPEED -> attackSpeed;
            case MOVEMENT_SPEED -> movementSpeed;
        };
    }

    /** 将两个权重分布相加（材料 + 基础，或基础 + DNA修正） */
    public MechanicalPartWeight add(MechanicalPartWeight other) {
        return new MechanicalPartWeight(
                clampAdd(this.overallMultiplier, other.overallMultiplier),
                clampAdd(this.health, other.health),
                clampAdd(this.attackDamage, other.attackDamage),
                clampAdd(this.attackSpeed, other.attackSpeed),
                clampAdd(this.movementSpeed, other.movementSpeed)
        );
    }

    /** 将两个权重分布相减（用于移除DNA修正） */
    public MechanicalPartWeight subtract(MechanicalPartWeight other) {
        return new MechanicalPartWeight(
                clampSub(this.overallMultiplier, other.overallMultiplier),
                clampSub(this.health, other.health),
                clampSub(this.attackDamage, other.attackDamage),
                clampSub(this.attackSpeed, other.attackSpeed),
                clampSub(this.movementSpeed, other.movementSpeed)
        );
    }

    private static int clampAdd(int a, int b) {
        return Math.min(5, Math.max(0, a + b));
    }

    private static int clampSub(int a, int b) {
        return Math.min(5, Math.max(0, a - b));
    }

    /** 零权重分布（所有维度为 0） */
    public static final MechanicalPartWeight ZERO = new MechanicalPartWeight(0, 0, 0, 0, 0);
}