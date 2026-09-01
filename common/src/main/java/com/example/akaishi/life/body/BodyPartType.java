package com.example.akaishi.life.body;

/**
 * 躯体部位类型：器官（内脏）或肢体。
 * 决定移植/摘除的代价档位与后续基因效果的作用域。
 */
public enum BodyPartType {

    /** 器官：眼/心/肺/肝/肾，摘除代价更高 */
    ORGAN,
    /** 肢体：左右臂/左右腿，摘除代价较低 */
    LIMB
}
