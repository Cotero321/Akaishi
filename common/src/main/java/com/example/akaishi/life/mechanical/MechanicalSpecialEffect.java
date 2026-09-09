package com.example.akaishi.life.mechanical;

/**
 * 机械特殊效果枚举。
 * 每个DNA来源最多携带一个特殊效果。
 * 此枚举为游戏机制层，附属模组如需新增效果需在对应 handler 中实现逻辑。
 */
public enum MechanicalSpecialEffect {
    /** 无效果 */
    NONE,
    /** 暴击率提升 +5% */
    CRITICAL_BOOST,
    /** 爆炸抗性 */
    EXPLOSION_RESIST,
    /** 毒素免疫 */
    POISON_RESIST,
    /** 低血量自动回复 */
    LOW_HEALTH_REGENERATION,
    /** 攻击附带火焰 */
    FIRE_ATTACK,
    /** 水下加速 */
    UNDERWATER_SPEED,
    /** 金装备额外加成 */
    GOLD_ARMOR_BONUS,
    /** 击退抗性 */
    KNOCKBACK_RESIST,
    /** 瞬移冷却减少 */
    TELEPORT_COOLDOWN,
    /** 攻击附带凋零 */
    WITHER_ATTACK
}