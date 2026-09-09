package com.example.akaishi.life.mechanical;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 机械部件基础权重定义：每种器官 × 每个部件类型的五项权重分布。
 * 每部件五项总和 = 10，各部件主导不同属性维度。
 * 这是"器官倾向"的最底层数据，材料点数和DNA修正在此基础上叠加。
 */
public final class MechanicalPartsDefinition {

    private MechanicalPartsDefinition() {}

    // ---- 内部器官 ----
    /** 义眼 — 核心：感知输出 */
    private static final MechanicalPartWeight EYE_CORE = w(1, 1, 3, 3, 2);
    /** 义眼 — 模块：功能特化 */
    private static final MechanicalPartWeight EYE_MODULE = w(2, 0, 2, 3, 3);
    /** 义眼 — 外壳：防护稳定 */
    private static final MechanicalPartWeight EYE_SHELL = w(1, 4, 1, 1, 3);
    /** 义眼 — 散热：散热续航 */
    private static final MechanicalPartWeight EYE_COOLING = w(3, 1, 1, 1, 4);

    /** 心脏 — 核心：能源输出 */
    private static final MechanicalPartWeight HEART_CORE = w(4, 4, 1, 1, 0);
    /** 心脏 — 模块：循环调节 */
    private static final MechanicalPartWeight HEART_MODULE = w(3, 3, 2, 2, 0);
    /** 心脏 — 外壳：稳压保护 */
    private static final MechanicalPartWeight HEART_SHELL = w(1, 5, 1, 1, 2);
    /** 心脏 — 散热：冷却回路 */
    private static final MechanicalPartWeight HEART_COOLING = w(4, 2, 1, 1, 2);

    /** 肺 — 核心：过滤适应 */
    private static final MechanicalPartWeight LUNG_CORE = w(3, 3, 0, 1, 3);
    /** 肺 — 模块：增压功能 */
    private static final MechanicalPartWeight LUNG_MODULE = w(2, 2, 1, 1, 4);
    /** 肺 — 外壳：气密防护 */
    private static final MechanicalPartWeight LUNG_SHELL = w(1, 4, 0, 1, 4);
    /** 肺 — 散热：热交换 */
    private static final MechanicalPartWeight LUNG_COOLING = w(4, 2, 0, 1, 3);

    /** 内脏 — 核心：代谢转化 */
    private static final MechanicalPartWeight VISCERA_CORE = w(2, 4, 2, 2, 0);
    /** 内脏 — 模块：转化特化 */
    private static final MechanicalPartWeight VISCERA_MODULE = w(2, 3, 3, 2, 0);
    /** 内脏 — 外壳：缓冲保护 */
    private static final MechanicalPartWeight VISCERA_SHELL = w(1, 5, 1, 1, 2);
    /** 内脏 — 散热：热管理 */
    private static final MechanicalPartWeight VISCERA_COOLING = w(4, 2, 1, 1, 2);

    /** 肾脏 — 核心：净化处理 */
    private static final MechanicalPartWeight KIDNEY_CORE = w(3, 3, 1, 1, 2);
    /** 肾脏 — 模块：循环处理 */
    private static final MechanicalPartWeight KIDNEY_MODULE = w(2, 2, 2, 2, 2);
    /** 肾脏 — 外壳：耐蚀防护 */
    private static final MechanicalPartWeight KIDNEY_SHELL = w(1, 5, 0, 0, 4);
    /** 肾脏 — 散热：冷却结构 */
    private static final MechanicalPartWeight KIDNEY_COOLING = w(4, 1, 1, 1, 3);

    // ---- 四肢 ----
    /** 手臂 — 核心：动力输出 */
    private static final MechanicalPartWeight ARM_CORE = w(1, 0, 5, 3, 1);
    /** 手臂 — 模块：功能模块 */
    private static final MechanicalPartWeight ARM_MODULE = w(2, 1, 4, 3, 0);
    /** 手臂 — 外壳：骨架防护 */
    private static final MechanicalPartWeight ARM_SHELL = w(2, 4, 2, 1, 1);
    /** 手臂 — 散热：散热排热 */
    private static final MechanicalPartWeight ARM_COOLING = w(4, 1, 1, 2, 2);

    /** 腿 — 核心：驱动输出（移速上限 5，溢出点转总体倍率） */
    private static final MechanicalPartWeight LEG_CORE = w(2, 1, 1, 1, 5);
    /** 腿 — 模块：稳定机动 */
    private static final MechanicalPartWeight LEG_MODULE = w(1, 1, 1, 2, 5);
    /** 腿 — 外壳：骨架防护 */
    private static final MechanicalPartWeight LEG_SHELL = w(2, 4, 0, 1, 3);
    /** 腿 — 散热：散热结构 */
    private static final MechanicalPartWeight LEG_COOLING = w(3, 1, 0, 1, 5);

    // ---- 按器官类型索引的映射 ----
    private static final Map<MechanicalOrganType, Map<MechanicalPartType, MechanicalPartWeight>> DEFINITIONS;

    static {
        Map<MechanicalOrganType, Map<MechanicalPartType, MechanicalPartWeight>> map = new EnumMap<>(MechanicalOrganType.class);

        map.put(MechanicalOrganType.EYE, partMap(EYE_CORE, EYE_MODULE, EYE_SHELL, EYE_COOLING));
        map.put(MechanicalOrganType.HEART, partMap(HEART_CORE, HEART_MODULE, HEART_SHELL, HEART_COOLING));
        map.put(MechanicalOrganType.LUNG, partMap(LUNG_CORE, LUNG_MODULE, LUNG_SHELL, LUNG_COOLING));
        map.put(MechanicalOrganType.VISCERA, partMap(VISCERA_CORE, VISCERA_MODULE, VISCERA_SHELL, VISCERA_COOLING));
        map.put(MechanicalOrganType.KIDNEY, partMap(KIDNEY_CORE, KIDNEY_MODULE, KIDNEY_SHELL, KIDNEY_COOLING));
        // 左/右臂共用权重视图
        map.put(MechanicalOrganType.LEFT_ARM, partMap(ARM_CORE, ARM_MODULE, ARM_SHELL, ARM_COOLING));
        map.put(MechanicalOrganType.RIGHT_ARM, partMap(ARM_CORE, ARM_MODULE, ARM_SHELL, ARM_COOLING));
        map.put(MechanicalOrganType.LEFT_LEG, partMap(LEG_CORE, LEG_MODULE, LEG_SHELL, LEG_COOLING));
        map.put(MechanicalOrganType.RIGHT_LEG, partMap(LEG_CORE, LEG_MODULE, LEG_SHELL, LEG_COOLING));

        DEFINITIONS = Collections.unmodifiableMap(map);
    }

    /** 获取指定器官 × 部件的基础权重，不存在则返回 ZERO */
    public static MechanicalPartWeight getBaseWeight(MechanicalOrganType organ, MechanicalPartType part) {
        Map<MechanicalPartType, MechanicalPartWeight> parts = DEFINITIONS.get(organ);
        if (parts == null) return MechanicalPartWeight.ZERO;
        return parts.getOrDefault(part, MechanicalPartWeight.ZERO);
    }

    /** 构建某个器官的四个部件权重映射 */
    private static Map<MechanicalPartType, MechanicalPartWeight> partMap(
            MechanicalPartWeight core, MechanicalPartWeight module,
            MechanicalPartWeight shell, MechanicalPartWeight cooling) {
        Map<MechanicalPartType, MechanicalPartWeight> map = new EnumMap<>(MechanicalPartType.class);
        map.put(MechanicalPartType.CORE, core);
        map.put(MechanicalPartType.MODULE, module);
        map.put(MechanicalPartType.SHELL, shell);
        map.put(MechanicalPartType.COOLING, cooling);
        return Collections.unmodifiableMap(map);
    }

    /** 创建五项权重的快捷方法 */
    private static MechanicalPartWeight w(int om, int h, int ad, int as, int ms) {
        return new MechanicalPartWeight(om, h, ad, as, ms);
    }
}