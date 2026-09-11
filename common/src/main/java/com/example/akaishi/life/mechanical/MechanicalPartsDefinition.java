package com.example.akaishi.life.mechanical;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 机械部件基础权重定义：每种器官 × 每个部件类型的十维权重分布。
 * 设计约束（见 {@link MechanicalAttributeMatrix}）：
 * - 每个器官的四个部件属性点合计 = 该槽位预算（全身 71 点）；
 * - 四部件按 CORE 40% / MODULE 30% / SHELL 20% / COOLING 10% 分摊；
 * - 只在该槽位允许的属性轴上分点，越界轴恒为 0；
 * - 总体倍率轴独立于预算，按部件职责给定（核心/散热主导）。
 * 这是"器官倾向"的最底层数据，材料点数和 DNA 修正在此基础上叠加。
 */
public final class MechanicalPartsDefinition {

    private MechanicalPartsDefinition() {}

    // ---- 内部器官 ----
    /** 义眼 — 核心：感知输出（暴击率/暴击伤害/生命） */
    private static final MechanicalPartWeight EYE_CORE = w(1, 1, 0, 0, 0, 0, 1, 1, 0, 0);
    /** 义眼 — 模块：功能特化（暴击双修） */
    private static final MechanicalPartWeight EYE_MODULE = w(2, 0, 0, 0, 0, 0, 1, 1, 0, 0);
    /** 义眼 — 外壳：防护稳定 */
    private static final MechanicalPartWeight EYE_SHELL = w(1, 1, 0, 0, 0, 0, 0, 0, 0, 0);
    /** 义眼 — 散热：散热续航（攻击速度） */
    private static final MechanicalPartWeight EYE_COOLING = w(3, 0, 0, 1, 0, 0, 0, 0, 0, 0);

    /** 心脏 — 核心：能源输出（生命/攻击） */
    private static final MechanicalPartWeight HEART_CORE = w(4, 2, 1, 1, 0, 0, 0, 0, 0, 0);
    /** 心脏 — 模块：循环调节 */
    private static final MechanicalPartWeight HEART_MODULE = w(3, 1, 1, 1, 0, 0, 0, 0, 0, 0);
    /** 心脏 — 外壳：稳压保护 */
    private static final MechanicalPartWeight HEART_SHELL = w(1, 1, 0, 0, 0, 1, 0, 0, 0, 0);
    /** 心脏 — 散热：冷却回路（移速） */
    private static final MechanicalPartWeight HEART_COOLING = w(4, 0, 0, 0, 1, 0, 0, 0, 0, 0);

    /** 肺 — 核心：过滤适应（生命/护甲） */
    private static final MechanicalPartWeight LUNG_CORE = w(3, 2, 0, 0, 0, 1, 0, 0, 0, 0);
    /** 肺 — 模块：增压功能（攻速/移速） */
    private static final MechanicalPartWeight LUNG_MODULE = w(2, 0, 0, 1, 1, 0, 0, 0, 0, 0);
    /** 肺 — 外壳：气密防护 */
    private static final MechanicalPartWeight LUNG_SHELL = w(1, 0, 0, 0, 0, 1, 0, 0, 0, 0);
    /** 肺 — 散热：热交换 */
    private static final MechanicalPartWeight LUNG_COOLING = w(4, 0, 0, 0, 1, 0, 0, 0, 0, 0);

    /** 内脏 — 核心：代谢转化（攻击/生命/护甲） */
    private static final MechanicalPartWeight VISCERA_CORE = w(2, 1, 2, 0, 0, 1, 0, 0, 0, 0);
    /** 内脏 — 模块：转化特化 */
    private static final MechanicalPartWeight VISCERA_MODULE = w(2, 1, 1, 1, 0, 0, 0, 0, 0, 0);
    /** 内脏 — 外壳：缓冲保护 */
    private static final MechanicalPartWeight VISCERA_SHELL = w(1, 0, 0, 0, 0, 1, 0, 0, 0, 0);
    /** 内脏 — 散热：热管理 */
    private static final MechanicalPartWeight VISCERA_COOLING = w(4, 0, 0, 0, 1, 0, 0, 0, 0, 0);

    /** 肾脏 — 核心：净化处理（攻击/攻速） */
    private static final MechanicalPartWeight KIDNEY_CORE = w(3, 0, 2, 1, 0, 0, 0, 0, 0, 0);
    /** 肾脏 — 模块：循环处理（攻速/移速） */
    private static final MechanicalPartWeight KIDNEY_MODULE = w(2, 0, 0, 1, 1, 0, 0, 0, 0, 0);
    /** 肾脏 — 外壳：耐蚀防护 */
    private static final MechanicalPartWeight KIDNEY_SHELL = w(1, 1, 0, 0, 0, 1, 0, 0, 0, 0);
    /** 肾脏 — 散热：冷却结构 */
    private static final MechanicalPartWeight KIDNEY_COOLING = w(4, 0, 0, 0, 1, 0, 0, 0, 0, 0);

    // ---- 四肢 ----
    /** 手臂 — 核心：动力输出（攻击伤害/暴击伤害） */
    private static final MechanicalPartWeight ARM_CORE = w(1, 0, 2, 0, 0, 0, 0, 1, 0, 0);
    /** 手臂 — 模块：功能模块（攻速/攻击范围） */
    private static final MechanicalPartWeight ARM_MODULE = w(2, 0, 0, 1, 0, 0, 0, 0, 1, 0);
    /** 手臂 — 外壳：骨架防护（生命/暴击率） */
    private static final MechanicalPartWeight ARM_SHELL = w(2, 1, 0, 0, 0, 0, 1, 0, 0, 0);
    /** 手臂 — 散热：散热排热（护甲） */
    private static final MechanicalPartWeight ARM_COOLING = w(4, 0, 0, 0, 0, 1, 0, 0, 0, 0);

    /** 腿 — 核心：驱动输出（移速/闪避） */
    private static final MechanicalPartWeight LEG_CORE = w(2, 0, 0, 0, 2, 0, 0, 0, 0, 1);
    /** 腿 — 模块：稳定机动（移速/生命） */
    private static final MechanicalPartWeight LEG_MODULE = w(1, 1, 0, 0, 1, 0, 0, 0, 0, 0);
    /** 腿 — 外壳：骨架防护（护甲） */
    private static final MechanicalPartWeight LEG_SHELL = w(2, 0, 0, 0, 0, 1, 0, 0, 0, 0);
    /** 腿 — 散热：散热结构（闪避） */
    private static final MechanicalPartWeight LEG_COOLING = w(3, 0, 0, 0, 0, 0, 0, 0, 0, 1);

    // ---- 按器官类型索引的映射 ----
    private static final Map<MechanicalOrganType, Map<MechanicalPartType, MechanicalPartWeight>> DEFINITIONS;

    static {
        Map<MechanicalOrganType, Map<MechanicalPartType, MechanicalPartWeight>> map = new EnumMap<>(MechanicalOrganType.class);

        map.put(MechanicalOrganType.EYE, partMap(MechanicalOrganType.EYE, EYE_CORE, EYE_MODULE, EYE_SHELL, EYE_COOLING));
        map.put(MechanicalOrganType.HEART, partMap(MechanicalOrganType.HEART, HEART_CORE, HEART_MODULE, HEART_SHELL, HEART_COOLING));
        map.put(MechanicalOrganType.LUNG, partMap(MechanicalOrganType.LUNG, LUNG_CORE, LUNG_MODULE, LUNG_SHELL, LUNG_COOLING));
        map.put(MechanicalOrganType.VISCERA, partMap(MechanicalOrganType.VISCERA, VISCERA_CORE, VISCERA_MODULE, VISCERA_SHELL, VISCERA_COOLING));
        map.put(MechanicalOrganType.KIDNEY, partMap(MechanicalOrganType.KIDNEY, KIDNEY_CORE, KIDNEY_MODULE, KIDNEY_SHELL, KIDNEY_COOLING));
        // 左/右臂共用权重视图
        map.put(MechanicalOrganType.LEFT_ARM, partMap(MechanicalOrganType.LEFT_ARM, ARM_CORE, ARM_MODULE, ARM_SHELL, ARM_COOLING));
        map.put(MechanicalOrganType.RIGHT_ARM, partMap(MechanicalOrganType.RIGHT_ARM, ARM_CORE, ARM_MODULE, ARM_SHELL, ARM_COOLING));
        map.put(MechanicalOrganType.LEFT_LEG, partMap(MechanicalOrganType.LEFT_LEG, LEG_CORE, LEG_MODULE, LEG_SHELL, LEG_COOLING));
        map.put(MechanicalOrganType.RIGHT_LEG, partMap(MechanicalOrganType.RIGHT_LEG, LEG_CORE, LEG_MODULE, LEG_SHELL, LEG_COOLING));

        DEFINITIONS = Collections.unmodifiableMap(map);
    }

    /** 获取指定器官 × 部件的基础权重，不存在则返回 ZERO */
    public static MechanicalPartWeight getBaseWeight(MechanicalOrganType organ, MechanicalPartType part) {
        Map<MechanicalPartType, MechanicalPartWeight> parts = DEFINITIONS.get(organ);
        if (parts == null) return MechanicalPartWeight.ZERO;
        return parts.getOrDefault(part, MechanicalPartWeight.ZERO);
    }

    /** 构建某个器官的四个部件权重映射（并校验预算与矩阵正交） */
    private static Map<MechanicalPartType, MechanicalPartWeight> partMap(
            MechanicalOrganType organ,
            MechanicalPartWeight core, MechanicalPartWeight module,
            MechanicalPartWeight shell, MechanicalPartWeight cooling) {
        Map<MechanicalPartType, MechanicalPartWeight> map = new EnumMap<>(MechanicalPartType.class);
        map.put(MechanicalPartType.CORE, core);
        map.put(MechanicalPartType.MODULE, module);
        map.put(MechanicalPartType.SHELL, shell);
        map.put(MechanicalPartType.COOLING, cooling);
        validate(organ, map);
        return Collections.unmodifiableMap(map);
    }

    /** 快速失败：属性点必须与槽位预算一致，且不得落在矩阵之外的轴上 */
    private static void validate(MechanicalOrganType organ, Map<MechanicalPartType, MechanicalPartWeight> parts) {
        int attrSum = 0;
        for (MechanicalPartWeight weight : parts.values()) {
            for (MechanicalProperty prop : MechanicalProperty.values()) {
                int value = weight.get(prop);
                if (prop == MechanicalProperty.OVERALL_MULTIPLIER) {
                    continue;
                }
                if (value != 0 && !MechanicalAttributeMatrix.allows(organ, prop)) {
                    throw new IllegalStateException(
                            "Part weight violates matrix: " + organ + " -> " + prop);
                }
                attrSum += value;
            }
        }
        if (attrSum != MechanicalAttributeMatrix.budget(organ)) {
            throw new IllegalStateException(
                    "Part budget mismatch for " + organ + ": " + attrSum
                            + " != " + MechanicalAttributeMatrix.budget(organ));
        }
    }

    /** 创建十维权重的快捷方法：om / 生命 / 攻击伤害 / 攻击速度 / 移速 / 护甲 / 暴击率 / 暴击伤害 / 攻击范围 / 闪避 */
    private static MechanicalPartWeight w(int om, int hp, int ad, int as, int ms,
                                         int armor, int crit, int critDamage, int range, int dodge) {
        return new MechanicalPartWeight(om, hp, ad, as, ms, armor, crit, critDamage, range, dodge);
    }
}
