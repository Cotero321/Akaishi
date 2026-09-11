package com.example.akaishi.life.mechanical;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 机械义体「属性↔槽位」合法性矩阵（与生物器官的 OrganRegistry 完全独立）。
 * 机械器官不沿用生物器官的排布，可承载维度由槽位专精决定：
 * <ul>
 *   <li>义眼：暴击率 / 暴击伤害 / 生命 / 攻击速度 —— 瞄准输出</li>
 *   <li>心脏：生命 / 护甲 / 攻击伤害 / 攻击速度 / 移速 —— 全身动力</li>
 *   <li>肺：生命 / 护甲 / 攻击速度 / 移速 —— 环境适应</li>
 *   <li>内脏：生命 / 护甲 / 攻击伤害 / 攻击速度 / 移速 —— 代谢转化</li>
 *   <li>肾脏：生命 / 护甲 / 攻击伤害 / 攻击速度 / 移速 —— 净化循环</li>
 *   <li>臂：攻击伤害 / 攻击速度 / 攻击范围 / 暴击率 / 暴击伤害 / 生命 / 护甲 —— 重击与精准</li>
 *   <li>腿：移速 / 生命 / 护甲 / 闪避 —— 身法与承重</li>
 * </ul>
 * 总体倍率轴（{@link MechanicalProperty#OVERALL_MULTIPLIER}）不受矩阵约束，所有槽位皆可承载。
 */
public final class MechanicalAttributeMatrix {

    private MechanicalAttributeMatrix() {}

    private static final MechanicalProperty[] ARM_AXES = {
            MechanicalProperty.ATTACK_DAMAGE, MechanicalProperty.ATTACK_SPEED,
            MechanicalProperty.RANGE, MechanicalProperty.CRIT_CHANCE, MechanicalProperty.CRIT_DAMAGE,
            MechanicalProperty.HEALTH, MechanicalProperty.ARMOR
    };

    private static final MechanicalProperty[] LEG_AXES = {
            MechanicalProperty.MOVEMENT_SPEED, MechanicalProperty.HEALTH,
            MechanicalProperty.ARMOR, MechanicalProperty.DODGE
    };

    /** 槽位 → 允许承载的属性集合 */
    private static final Map<MechanicalOrganType, Set<MechanicalProperty>> MATRIX =
            new EnumMap<>(MechanicalOrganType.class);

    /** 槽位 → 该器官四个部件的属性预算总点数（全身合计 71 点） */
    private static final Map<MechanicalOrganType, Integer> BUDGET =
            new EnumMap<>(MechanicalOrganType.class);

    static {
        // 内在器官
        define(MechanicalOrganType.EYE, 7,
                MechanicalProperty.CRIT_CHANCE, MechanicalProperty.CRIT_DAMAGE,
                MechanicalProperty.HEALTH, MechanicalProperty.ATTACK_SPEED);
        define(MechanicalOrganType.HEART, 10,
                MechanicalProperty.HEALTH, MechanicalProperty.ARMOR,
                MechanicalProperty.ATTACK_DAMAGE, MechanicalProperty.ATTACK_SPEED,
                MechanicalProperty.MOVEMENT_SPEED);
        define(MechanicalOrganType.LUNG, 7,
                MechanicalProperty.HEALTH, MechanicalProperty.ARMOR,
                MechanicalProperty.ATTACK_SPEED, MechanicalProperty.MOVEMENT_SPEED);
        define(MechanicalOrganType.VISCERA, 9,
                MechanicalProperty.HEALTH, MechanicalProperty.ARMOR,
                MechanicalProperty.ATTACK_DAMAGE, MechanicalProperty.ATTACK_SPEED,
                MechanicalProperty.MOVEMENT_SPEED);
        define(MechanicalOrganType.KIDNEY, 8,
                MechanicalProperty.HEALTH, MechanicalProperty.ARMOR,
                MechanicalProperty.ATTACK_DAMAGE, MechanicalProperty.ATTACK_SPEED,
                MechanicalProperty.MOVEMENT_SPEED);
        // 四肢（左右同构）
        define(MechanicalOrganType.LEFT_ARM, 8, ARM_AXES);
        define(MechanicalOrganType.RIGHT_ARM, 8, ARM_AXES);
        define(MechanicalOrganType.LEFT_LEG, 7, LEG_AXES);
        define(MechanicalOrganType.RIGHT_LEG, 7, LEG_AXES);
    }

    private static void define(MechanicalOrganType organ, int budget, MechanicalProperty... allowed) {
        Set<MechanicalProperty> set = EnumSet.noneOf(MechanicalProperty.class);
        for (MechanicalProperty prop : allowed) {
            set.add(prop);
        }
        MATRIX.put(organ, set);
        BUDGET.put(organ, budget);
    }

    /** 该槽位是否允许承载该属性（总体倍率轴恒为 true） */
    public static boolean allows(MechanicalOrganType organ, MechanicalProperty prop) {
        if (organ == null || prop == null) {
            return true;
        }
        if (prop == MechanicalProperty.OVERALL_MULTIPLIER) {
            return true;
        }
        Set<MechanicalProperty> allowed = MATRIX.get(organ);
        return allowed == null || allowed.contains(prop);
    }

    /** 该槽位的属性预算总点数（不含总体倍率轴） */
    public static int budget(MechanicalOrganType organ) {
        return BUDGET.getOrDefault(organ, 0);
    }
}
