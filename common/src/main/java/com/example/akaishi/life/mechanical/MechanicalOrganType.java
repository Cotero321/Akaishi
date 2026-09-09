package com.example.akaishi.life.mechanical;

/**
 * 可机械化改造的器官/肢体类型。
 * 内部五器官（眼、心脏、肺、内脏、肾脏）+ 四肢（左/右臂、左/右腿）。
 */
public enum MechanicalOrganType {
    /** 机械义眼 — 感知、扫描、视觉辅助 */
    EYE,
    /** 赤能心脏 — 生命维持、能源核心 */
    HEART,
    /** 过滤肺 — 环境适应、呼吸抗性 */
    LUNG,
    /** 代谢内脏 — 药剂、转化、新陈代谢 */
    VISCERA,
    /** 净化肾脏 — 毒素处理、废热排出 */
    KIDNEY,
    /** 机械义肢（左臂）— 攻击、挖掘 */
    LEFT_ARM,
    /** 机械义肢（右臂）— 攻击、挖掘 */
    RIGHT_ARM,
    /** 机械义肢（左腿）— 移动、跳跃 */
    LEFT_LEG,
    /** 机械义肢（右腿）— 移动、跳跃 */
    RIGHT_LEG
}