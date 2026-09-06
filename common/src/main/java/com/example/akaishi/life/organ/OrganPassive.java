package com.example.akaishi.life.organ;

/**
 * 器官被动技能类型（全部常驻/被动触发，无需玩家操作）。
 * 强度两维：
 * - 数量维度：跨器官来源数叠加（由生效层 count 聚合，≥2 升级）；
 * - 品质维度：携带该被动的最高来源器官品质抬升数值上限（由生效层按 QualityTier 档位逐级增强）。
 * negative = 负面（代价型）被动：tooltip 红字警示，用于诅咒/高风险器官多元化。
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
    FIRE_WEAKNESS("fire_weakness", true),
    /** 水下攻击伤害 +50% */
    WATER_ATTACK_BOOST("water_attack_boost"),
    /** 弹射物伤害 +25% */
    PROJECTILE_BOOST("projectile_boost"),
    /** 近战攻击距离提升（每臂 +0.75 格，长臂生物） */
    LONG_REACH("long_reach"),
    /** 受击概率喷墨隐身（25%，4 秒，鱿鱼墨雾脱身） */
    INK_CLOUD("ink_cloud"),
    /** 滑翔：空中缓降（鹦鹉羽肺等飞行生物） */
    GLIDE("glide"),
    /** 排毒代谢：每 5 秒清除自身中毒（肾脏系） */
    ANTIDOTE("antidote"),
    /** 命中附加挖掘疲劳（10 秒 I 级，克制采矿——远古守卫者激光眼） */
    FATIGUE_ON_HIT("fatigue_on_hit"),
    /** 寒髓代谢：免疫冰冻并清除缓慢（雪傀儡肾脏） */
    ANTIFREEZE("antifreeze"),
    /** 冲撞兽击：命中把目标向后顶开（疣猪兽兽性冲撞，与武器击退叠加） */
    KNOCKBACK_ON_HIT("knockback_on_hit"),
    /** 灼热点燃：命中使目标着火 3 秒（岩浆怪熔核） */
    IGNITE_ON_HIT("ignite_on_hit"),
    /** 爆破体质：受到的爆炸伤害减免（苦力怕硫磺代谢——自家爆炸反噬免疫） */
    BLAST_RESIST("blast_resist"),
    /** 女巫酿造：每 12 秒随机调一杯药水给自己灌下（运动/防护/恢复随机池，20 秒持续） */
    WITCH_BREW("witch_brew"),

    // ===== 负面被动（诅咒系——高回报器官的代价，tooltip 红字警示）=====
    /** 阳光灼晒：白天处于天空直射下会燃烧（亡灵速腿/幻翼肺的代价，抗火药水/火焰免疫可豁免） */
    SUNLIGHT_BURN("sunlight_burn", true),
    /** 脆骨体质：受到的近战/弹射伤害加深（凋灵骷髅臂——玻璃大炮） */
    VULNERABLE("vulnerable", true),
    /** 高代谢：饥饿消耗加速（狂怒副肾等强攻速器官的代价） */
    RAPID_EXHAUSTION("rapid_exhaustion", true);

    private final String id;
    /** 是否为负面（代价型）被动：tooltip 以红字警示 */
    private final boolean negative;

    OrganPassive(String id) {
        this(id, false);
    }

    OrganPassive(String id, boolean negative) {
        this.id = id;
        this.negative = negative;
    }

    public String getId() {
        return id;
    }

    public boolean isNegative() {
        return negative;
    }
}
