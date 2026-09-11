package com.example.akaishi.life.mechanical;

/**
 * 机械部件的十维权重分布（九属性 + 总体倍率轴）。
 * 每项权重范围 0~5（-1 仅供 DNA 修正下压），确保"各有专攻，无法全满"。
 * 权重在三个阶段叠加：部件基础权重 + 材料点数 + DNA 修正，逐项 clamp 0~5。
 */
public final class MechanicalPartWeight {
    private final int[] values;

    /**
     * 十维构造：顺序与 {@link MechanicalProperty} 声明顺序一致。
     * 依次为 倍率 / 生命 / 攻击伤害 / 攻击速度 / 移动速度 / 护甲 / 暴击率 / 暴击伤害 / 攻击范围 / 闪避。
     *
     * @throws IllegalArgumentException 参数个数不符或任一维度超出 -1~5
     */
    public MechanicalPartWeight(int overallMultiplier, int health, int attackDamage, int attackSpeed,
                                int movementSpeed, int armor, int critChance, int critDamage,
                                int range, int dodge) {
        int[] v = {overallMultiplier, health, attackDamage, attackSpeed, movementSpeed,
                armor, critChance, critDamage, range, dodge};
        for (int i = 0; i < v.length; i++) {
            validateRange(v[i], MechanicalProperty.values()[i].name());
        }
        this.values = v;
    }

    private MechanicalPartWeight(int[] values) {
        this.values = values;
    }

    /** 权重上限（单项） */
    public static final int MAX = 5;
    /** 权重下限（单项，-1 仅供 DNA 修正） */
    public static final int MIN = -1;

    private static void validateRange(int value, String name) {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    "MechanicalProperty " + name + " must be " + MIN + "~" + MAX + ", got " + value);
        }
    }

    /** 十项权重之和 */
    public int sum() {
        int total = 0;
        for (int v : values) {
            total += v;
        }
        return total;
    }

    /** 获取指定属性的权重值 */
    public int get(MechanicalProperty prop) {
        return values[prop.ordinal()];
    }

    /** 返回替换指定属性权重后的新分布 */
    public MechanicalPartWeight with(MechanicalProperty prop, int value) {
        validateRange(value, prop.name());
        int[] copy = values.clone();
        copy[prop.ordinal()] = value;
        return new MechanicalPartWeight(copy);
    }

    /** 将两个权重分布相加（基础 + 材料，或 + DNA 修正），逐项 clamp 0~5 */
    public MechanicalPartWeight add(MechanicalPartWeight other) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = clamp(values[i] + other.values[i]);
        }
        return new MechanicalPartWeight(result);
    }

    /** 将两个权重分布相减（用于移除 DNA 修正），逐项 clamp 0~5 */
    public MechanicalPartWeight subtract(MechanicalPartWeight other) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = clamp(values[i] - other.values[i]);
        }
        return new MechanicalPartWeight(result);
    }

    private static int clamp(int value) {
        return Math.min(MAX, Math.max(0, value));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof MechanicalPartWeight other && java.util.Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "MechanicalPartWeight" + java.util.Arrays.toString(values);
    }

    /** 零权重分布（所有维度为 0） */
    public static final MechanicalPartWeight ZERO =
            new MechanicalPartWeight(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
}
