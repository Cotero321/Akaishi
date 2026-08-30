package com.example.template.life.organ;

/**
 * 器官被动技能类型（全部常驻/被动触发，无需玩家操作）。
 * 参数均为固定设计值，未来如需差异化可在 OrganEffect 中扩展。
 */
public enum OrganPassive {

    /** 常驻跳跃提升（等级 II） */
    JUMP_BOOST("jump_boost"),
    /** 常驻夜视 */
    NIGHT_VISION("night_vision"),
    /** 摔落伤害免疫 */
    FALL_IMMUNE("fall_immune"),
    /** 侦测 24 格内敌意生物并高亮 */
    ENEMY_GLOW("enemy_glow"),
    /** 攻击命中时减速目标（缓慢 II，5 秒） */
    SLOW_ON_HIT("slow_on_hit"),
    /** 跳跃攻击伤害提升（+60%） */
    JUMP_ATTACK_BOOST("jump_attack_boost"),
    /** 食物恢复效果 +25% */
    FOOD_BOOST("food_boost"),
    /** 自动拾取周围掉落物（5 格） */
    AUTO_PICKUP("auto_pickup"),
    /** 免疫移动减速（蛛网/黏液等） */
    SLOW_IMMUNE("slow_immune"),
    /** 游泳加速 */
    SWIM_BOOST("swim_boost"),
    /** 自身发光（洞穴探路） */
    GLOW("glow"),
    /** 水下呼吸 */
    WATER_BREATHING("water_breathing"),
    /** 攻击命中附加中毒（II，3 秒） */
    POISON_ON_HIT("poison_on_hit"),
    /** 缓慢再生（每 2 秒 1 点） */
    REGEN("regen"),
    /** 受击概率瞬移闪避 */
    TELEPORT_DODGE("teleport_dodge"),
    /** 火焰/岩浆免疫 */
    FIRE_IMMUNE("fire_immune"),
    /** 反弹近战伤害（30%） */
    THORNS("thorns"),
    /** 攻击命中附加凋零（II，3 秒） */
    WITHER_ON_HIT("wither_on_hit"),
    /** 火焰伤害 +50%（负面） */
    FIRE_WEAKNESS("fire_weakness"),
    /** 水下攻击伤害 +50% */
    WATER_ATTACK_BOOST("water_attack_boost"),
    /** 弹射物伤害 +25% */
    PROJECTILE_BOOST("projectile_boost");

    private final String id;

    OrganPassive(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
