package com.example.akaishi.life.organ;

import com.example.akaishi.life.body.BodySlot;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 生物特色器官效果注册表（增量式）："具体生物 × 槽位" → 效果定义。
 * 设计原则：
 * - 每个生物只注册有特色的器官，没有默认全量值（不同生物可用器官不同）
 * - 属性覆盖槽位模板（null 表示沿用模板），被动/特殊效果是特色来源
 * - 新生物逐个补充注册即可，无需改动其他代码
 */
public final class OrganEffectRegistry {

    /** entityId(minecraft:cow) → 槽位 → 效果 */
    private static final Map<String, Map<BodySlot, OrganEffect>> EFFECTS = new HashMap<>();

    static {
        // ===== 牛：牛心（+生命）、牛胃（吃小麦/禁肉）、牛腿（+攻击-速度）=====
        register("minecraft:cow", BodySlot.HEART, new OrganEffect("minecraft:cow", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 3.0)), null, null));
        register("minecraft:cow", BodySlot.VISCERA, new OrganEffect("minecraft:cow", BodySlot.VISCERA,
                null, null, OrganSpecial.COW_STOMACH));
        register("minecraft:cow", BodySlot.LEFT_LEG, new OrganEffect("minecraft:cow", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.02)), null, null));
        register("minecraft:cow", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:cow", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.02)), null, null));

        // ===== 兔：兔腿（跳跃提升 + 摔落减免）=====
        register("minecraft:rabbit", BodySlot.LEFT_LEG, new OrganEffect("minecraft:rabbit", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)),
                List.of(OrganPassive.JUMP_BOOST, OrganPassive.FALL_IMMUNE), null));
        register("minecraft:rabbit", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:rabbit", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)),
                List.of(OrganPassive.JUMP_BOOST, OrganPassive.FALL_IMMUNE), null));

        // ===== 猫：猫眼（夜视）+ 猫爪（攻速，敏捷刺客）=====
        register("minecraft:cat", BodySlot.EYE, new OrganEffect("minecraft:cat", BodySlot.EYE,
                null, List.of(OrganPassive.NIGHT_VISION), null));
        register("minecraft:cat", BodySlot.LEFT_ARM, new OrganEffect("minecraft:cat", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.5)), null, null));
        register("minecraft:cat", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:cat", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.5)), null, null));

        // ===== 狼：狼爪（+攻击，撕咬连击——群猎咬合快，攻速加成区别于钝击系）=====
        register("minecraft:wolf", BodySlot.LEFT_ARM, new OrganEffect("minecraft:wolf", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.15)), null, null));
        register("minecraft:wolf", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:wolf", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.15)), null, null));

        // ===== 狐狸：狐狸嘴（跳跃攻击伤害提升）+ 狐狸腿（轻灵）=====
        register("minecraft:fox", BodySlot.VISCERA, new OrganEffect("minecraft:fox", BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.LUCK, 0.75)),
                List.of(OrganPassive.JUMP_ATTACK_BOOST), null));
        register("minecraft:fox", BodySlot.LEFT_LEG, new OrganEffect("minecraft:fox", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)), null, null));
        register("minecraft:fox", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:fox", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)), null, null));

        // ===== 蝙蝠：回声定位耳（侦测敌意生物高亮）=====
        register("minecraft:bat", BodySlot.VISCERA, new OrganEffect("minecraft:bat", BodySlot.VISCERA,
                null, List.of(OrganPassive.ENEMY_GLOW), null));

        // ===== 蜘蛛：蛛丝腺（攻击减速目标）=====
        register("minecraft:spider", BodySlot.VISCERA, new OrganEffect("minecraft:spider", BodySlot.VISCERA,
                null, List.of(OrganPassive.SLOW_ON_HIT), null));

        // ===== 史莱姆：黏液腺（弹性：跳跃 + 摔落免疫）=====
        register("minecraft:slime", BodySlot.LUNGS, new OrganEffect("minecraft:slime", BodySlot.LUNGS,
                null, List.of(OrganPassive.JUMP_BOOST, OrganPassive.FALL_IMMUNE), null));

        // ===== 鸡：鸡砂囊（食物恢复 +25%）+ 鸡腿（轻落，摔落减免）=====
        register("minecraft:chicken", BodySlot.VISCERA, new OrganEffect("minecraft:chicken", BodySlot.VISCERA,
                null, List.of(OrganPassive.FOOD_BOOST), null));
        register("minecraft:chicken", BodySlot.LEFT_LEG, new OrganEffect("minecraft:chicken", BodySlot.LEFT_LEG,
                null, List.of(OrganPassive.FALL_IMMUNE), null));
        register("minecraft:chicken", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:chicken", BodySlot.RIGHT_LEG,
                null, List.of(OrganPassive.FALL_IMMUNE), null));

        // ===== 猪：猪心（额外生命值）+ 猪腿（短腿低重心——稳腿阶梯 III 档，与羊驼/驴/僵尸马排开）=====
        register("minecraft:pig", BodySlot.HEART, new OrganEffect("minecraft:pig", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 3.0)), null, null));
        register("minecraft:pig", BodySlot.LEFT_LEG, new OrganEffect("minecraft:pig", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.06)), null, null));
        register("minecraft:pig", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:pig", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.06)), null, null));

        // ===== 羊：羊毛绒（免疫减速）=====
        register("minecraft:sheep", BodySlot.VISCERA, new OrganEffect("minecraft:sheep", BodySlot.VISCERA,
                null, List.of(OrganPassive.SLOW_IMMUNE), null));

        // ===== 马：马腿（高移速）=====
        register("minecraft:horse", BodySlot.LEFT_LEG, new OrganEffect("minecraft:horse", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.03)), null, null));
        register("minecraft:horse", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:horse", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.03)), null, null));

        // ===== 铁傀儡：傀儡核心（重装，负面：笨重缓慢）=====
        register("minecraft:iron_golem", BodySlot.HEART, new OrganEffect("minecraft:iron_golem", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 4.0),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR, 2.0),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.1),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.03),
                        new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, -1.0)), null, null));
        // ===== 铁傀儡：巨铁臂（重拳长挥——原版铁傀儡攻击判定全村最大，双臂各攻 +1.0 + 攻击距离 +0.75）=====
        register("minecraft:iron_golem", BodySlot.LEFT_ARM, new OrganEffect("minecraft:iron_golem", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)),
                List.of(OrganPassive.LONG_REACH), null));
        register("minecraft:iron_golem", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:iron_golem", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)),
                List.of(OrganPassive.LONG_REACH), null));

        // ===== 海豚：海豚鳍（游泳加速）=====
        register("minecraft:dolphin", BodySlot.LUNGS, new OrganEffect("minecraft:dolphin", BodySlot.LUNGS,
                null, List.of(OrganPassive.SWIM_BOOST), null));

        // ===== 发光鱿鱼：发光腺（自身发光）=====
        register("minecraft:glow_squid", BodySlot.VISCERA, new OrganEffect("minecraft:glow_squid", BodySlot.VISCERA,
                null, List.of(OrganPassive.GLOW), null));

        // ===== 北极熊：熊掌（重击，负面：笨重）=====
        register("minecraft:polar_bear", BodySlot.LEFT_ARM, new OrganEffect("minecraft:polar_bear", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.5),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.01)), null, null));
        register("minecraft:polar_bear", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:polar_bear", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.5),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.01)), null, null));

        // ===== 悦灵：悦灵之心（自动拾取掉落物）=====
        register("minecraft:allay", BodySlot.HEART, new OrganEffect("minecraft:allay", BodySlot.HEART,
                null, List.of(OrganPassive.AUTO_PICKUP), null));

        // ===== 蜜蜂：蜂刺腺（攻击中毒）=====
        register("minecraft:bee", BodySlot.VISCERA, new OrganEffect("minecraft:bee", BodySlot.VISCERA,
                null, List.of(OrganPassive.POISON_ON_HIT), null));

        // ===== 僵尸：僵尸心脏（力量，负面：怕火）=====
        register("minecraft:zombie", BodySlot.HEART, new OrganEffect("minecraft:zombie", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0)),
                List.of(OrganPassive.REGEN, OrganPassive.FIRE_WEAKNESS), null));

        // ===== 骷髅：骷髅骨架（骨甲 + 弹射物强化）+ 枯骨双臂（亡灵最低基础臂——轻灵快剑）=====
        register("minecraft:skeleton", BodySlot.VISCERA, new OrganEffect("minecraft:skeleton", BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ARMOR, 1.0)),
                List.of(OrganPassive.PROJECTILE_BOOST), null));
        register("minecraft:skeleton", BodySlot.LEFT_ARM, new OrganEffect("minecraft:skeleton", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.5)), null, null));
        register("minecraft:skeleton", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:skeleton", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.5)), null, null));

        // ===== 末影人：末影核心（瞬移闪避，负面：怕水）+ 末影臂（长臂挥击）=====
        register("minecraft:enderman", BodySlot.HEART, new OrganEffect("minecraft:enderman", BodySlot.HEART,
                null, List.of(OrganPassive.TELEPORT_DODGE), OrganSpecial.ENDER_WATER_FEAR));
        register("minecraft:enderman", BodySlot.LEFT_ARM, new OrganEffect("minecraft:enderman", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)),
                List.of(OrganPassive.LONG_REACH), null));
        register("minecraft:enderman", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:enderman", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)),
                List.of(OrganPassive.LONG_REACH), null));

        // ===== 烈焰人：火焰核心（火焰免疫）=====
        register("minecraft:blaze", BodySlot.HEART, new OrganEffect("minecraft:blaze", BodySlot.HEART,
                null, List.of(OrganPassive.FIRE_IMMUNE), null));

        // ===== 守卫者：荆棘脊（反弹近战伤害）=====
        register("minecraft:guardian", BodySlot.VISCERA, new OrganEffect("minecraft:guardian", BodySlot.VISCERA,
                null, List.of(OrganPassive.THORNS), null));

        // ===== 凋灵：凋灵核心（攻击凋零 + 再生，满级出凋零骷髅头）=====
        register("minecraft:wither", BodySlot.HEART, new OrganEffect("minecraft:wither", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 4.0),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR, 2.0)),
                List.of(OrganPassive.WITHER_ON_HIT, OrganPassive.REGEN), OrganSpecial.WITHER_SKULL));

        // ===== 末影龙：龙之心（终局顶级，满级出龙息）=====
        register("minecraft:ender_dragon", BodySlot.HEART, new OrganEffect("minecraft:ender_dragon", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 6.0),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR, 3.0),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 2.0)),
                List.of(OrganPassive.REGEN, OrganPassive.FIRE_IMMUNE), OrganSpecial.DRAGON_BREATH));

        // ===== 青蛙：蛙皮（两栖：水下呼吸 + 跳跃）=====
        register("minecraft:frog", BodySlot.LUNGS, new OrganEffect("minecraft:frog", BodySlot.LUNGS,
                null, List.of(OrganPassive.WATER_BREATHING, OrganPassive.JUMP_BOOST), null));

        // ===== 海龟：龟之心（水下攻击强化）+ 龟甲（护甲，坚壳）=====
        register("minecraft:turtle", BodySlot.HEART, new OrganEffect("minecraft:turtle", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 1.0)),
                List.of(OrganPassive.WATER_ATTACK_BOOST), null));
        register("minecraft:turtle", BodySlot.VISCERA, new OrganEffect("minecraft:turtle", BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ARMOR, 1.0)), null, null));

        // ===== 鳕鱼：鱼鳃（水下呼吸）=====
        register("minecraft:cod", BodySlot.LUNGS, new OrganEffect("minecraft:cod", BodySlot.LUNGS,
                null, List.of(OrganPassive.WATER_BREATHING), null));

        // ===== 山羊：山羊腿（跳跃 + 轻快——跳跃腿阶梯 I 档，迈过猪速档的岩羊蹿跳）=====
        register("minecraft:goat", BodySlot.LEFT_LEG, new OrganEffect("minecraft:goat", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)),
                List.of(OrganPassive.JUMP_BOOST), null));
        register("minecraft:goat", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:goat", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)),
                List.of(OrganPassive.JUMP_BOOST), null));

        // ===== 苦力怕：爆炸囊（无属性，唯一意义是触发猫×苦力怕的天敌排异）=====
        register("minecraft:creeper", BodySlot.VISCERA, new OrganEffect("minecraft:creeper", BodySlot.VISCERA,
                null, null, null));

        // ===== 熊猫：熊掌拍击（+攻击，笨重憨厚移速慢）=====
        register("minecraft:panda", BodySlot.LEFT_ARM, new OrganEffect("minecraft:panda", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.1),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.02)), null, null));
        register("minecraft:panda", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:panda", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.1),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.02)), null, null));

        // ===== 豹猫：轻灵猫腿（高移速 + 跳跃提升）=====
        register("minecraft:ocelot", BodySlot.LEFT_LEG, new OrganEffect("minecraft:ocelot", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.025)),
                List.of(OrganPassive.JUMP_BOOST), null));
        register("minecraft:ocelot", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:ocelot", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.025)),
                List.of(OrganPassive.JUMP_BOOST), null));

        // ===== 羊驼：稳如驼峰（移速 + 击退抗性，久战不倒）=====
        register("minecraft:llama", BodySlot.LEFT_LEG, new OrganEffect("minecraft:llama", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), null, null));
        register("minecraft:llama", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:llama", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), null, null));

        // ===== 溺尸：水鬼之臂（+攻击，水下战斗强化）=====
        register("minecraft:drowned", BodySlot.LEFT_ARM, new OrganEffect("minecraft:drowned", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75)),
                List.of(OrganPassive.WATER_ATTACK_BOOST), null));
        register("minecraft:drowned", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:drowned", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75)),
                List.of(OrganPassive.WATER_ATTACK_BOOST), null));

        // ===== 凋灵骷髅：凋零骨臂（+攻击，命中附加凋零；负面：脆骨受创——玻璃大炮）=====
        register("minecraft:wither_skeleton", BodySlot.LEFT_ARM, new OrganEffect("minecraft:wither_skeleton", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0)),
                List.of(OrganPassive.WITHER_ON_HIT, OrganPassive.VULNERABLE), null));
        register("minecraft:wither_skeleton", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:wither_skeleton", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0)),
                List.of(OrganPassive.WITHER_ON_HIT, OrganPassive.VULNERABLE), null));

        // ===== 驴：驮兽腿（耐推不倒，稳健）=====
        register("minecraft:donkey", BodySlot.LEFT_LEG, new OrganEffect("minecraft:donkey", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.01),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.08)), null, null));
        register("minecraft:donkey", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:donkey", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.01),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.08)), null, null));

        // ===== 尸壳：烈日枯臂（+攻击，皮糙护甲）=====
        register("minecraft:husk", BodySlot.LEFT_ARM, new OrganEffect("minecraft:husk", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR, 0.5)), null, null));
        register("minecraft:husk", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:husk", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR, 0.5)), null, null));

        // ===== 流浪者：冰箭腿（命中减速目标，同其箭矢）=====
        register("minecraft:stray", BodySlot.LEFT_LEG, new OrganEffect("minecraft:stray", BodySlot.LEFT_LEG,
                null, List.of(OrganPassive.SLOW_ON_HIT), null));
        register("minecraft:stray", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:stray", BodySlot.RIGHT_LEG,
                null, List.of(OrganPassive.SLOW_ON_HIT), null));

        // ===== 骆驼：沙漠高腿（高移速 + 击退抗，沙漠坐骑）=====
        register("minecraft:camel", BodySlot.LEFT_LEG, new OrganEffect("minecraft:camel", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.025),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), null, null));
        register("minecraft:camel", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:camel", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.025),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), null, null));

        // ===== 蝾螈：再生腮肺（水下呼吸 + 缓慢再生，如其断肢再生）=====
        register("minecraft:axolotl", BodySlot.LUNGS, new OrganEffect("minecraft:axolotl", BodySlot.LUNGS,
                null, List.of(OrganPassive.WATER_BREATHING, OrganPassive.REGEN), null));

        // ===== 骷髅马：亡灵快腿（高移速，凋零骑士坐骑）=====
        register("minecraft:skeleton_horse", BodySlot.LEFT_LEG, new OrganEffect("minecraft:skeleton_horse", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.03)), null, null));
        register("minecraft:skeleton_horse", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:skeleton_horse", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.03)), null, null));

        // ===== 僵尸马：不屈腐腿（低移速 + 高击退抗，顽强不倒）=====
        register("minecraft:zombie_horse", BodySlot.LEFT_LEG, new OrganEffect("minecraft:zombie_horse", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.1)), null, null));
        register("minecraft:zombie_horse", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:zombie_horse", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.1)), null, null));

        // ===== 潜影贝：浮空石壳（高击退抗 + 护甲，稳如磐石）=====
        register("minecraft:shulker", BodySlot.VISCERA, new OrganEffect("minecraft:shulker", BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.15),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR, 0.5)), null, null));

        // ===== 河豚：毒刺皮（荆棘反伤，近战者自食其果）=====
        register("minecraft:pufferfish", BodySlot.VISCERA, new OrganEffect("minecraft:pufferfish", BodySlot.VISCERA,
                null, List.of(OrganPassive.THORNS), null));

        // ===== 鱿鱼：墨囊（受击喷墨隐身，脱身保命）+ 长须腕（触腕抽打——双臂纯攻击距离，水族触腕移植）=====
        register("minecraft:squid", BodySlot.VISCERA, new OrganEffect("minecraft:squid", BodySlot.VISCERA,
                null, List.of(OrganPassive.INK_CLOUD), null));
        register("minecraft:squid", BodySlot.LEFT_ARM, new OrganEffect("minecraft:squid", BodySlot.LEFT_ARM,
                List.of(), List.of(OrganPassive.LONG_REACH), null));
        register("minecraft:squid", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:squid", BodySlot.RIGHT_ARM,
                List.of(), List.of(OrganPassive.LONG_REACH), null));

        // ===== 鹦鹉：滑翔羽肺（空中缓降）=====
        register("minecraft:parrot", BodySlot.LUNGS, new OrganEffect("minecraft:parrot", BodySlot.LUNGS,
                null, List.of(OrganPassive.GLIDE), null));

        // ===== 洞穴蜘蛛：剧毒眼（命中中毒——蜘蛛眼本为剧毒材料）=====
        register("minecraft:cave_spider", BodySlot.EYE, new OrganEffect("minecraft:cave_spider", BodySlot.EYE,
                null, List.of(OrganPassive.POISON_ON_HIT), null));

        // ===== 僵尸村民：奸商眼光（幸运 +0.5，交易寻获好运）=====
        register("minecraft:zombie_villager", BodySlot.EYE, new OrganEffect("minecraft:zombie_villager", BodySlot.EYE,
                List.of(new OrganTemplate.AttributeBonus(Attributes.LUCK, 0.5)), null, null));

        // ===== 僵尸猪灵：秽土排毒肾（每 5 秒清除中毒——肾脏系开张）+ 秽金双臂（下界剑士的标配臂）=====
        register("minecraft:zombified_piglin", BodySlot.KIDNEYS, new OrganEffect("minecraft:zombified_piglin", BodySlot.KIDNEYS,
                null, List.of(OrganPassive.ANTIDOTE), null));
        register("minecraft:zombified_piglin", BodySlot.LEFT_ARM, new OrganEffect("minecraft:zombified_piglin", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75)), null, null));
        register("minecraft:zombified_piglin", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:zombified_piglin", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75)), null, null));

        // ===== 嗅探兽：寻宝嗅觉（幸运 +0.8，远古寻宝血脉）=====
        register("minecraft:sniffer", BodySlot.VISCERA, new OrganEffect("minecraft:sniffer", BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.LUCK, 0.8)), null, null));

        // ===== 幻翼：夜航滑翔（空中缓降——夜行猎手；负面：阳光灼晒——夜行者见光即燃）=====
        register("minecraft:phantom", BodySlot.LUNGS, new OrganEffect("minecraft:phantom", BodySlot.LUNGS,
                null, List.of(OrganPassive.GLIDE, OrganPassive.SUNLIGHT_BURN), null));

        // ===== 末影螨：末影空间感知（受击瞬移闪避——眼槽瞬移流）=====
        register("minecraft:endermite", BodySlot.EYE, new OrganEffect("minecraft:endermite", BodySlot.EYE,
                null, List.of(OrganPassive.TELEPORT_DODGE), null));

        // ===== 远古守卫者：激光聚焦（命中附加挖掘疲劳——克制采矿）=====
        register("minecraft:elder_guardian", BodySlot.EYE, new OrganEffect("minecraft:elder_guardian", BodySlot.EYE,
                null, List.of(OrganPassive.FATIGUE_ON_HIT), null));

        // ===== 雪傀儡：寒髓代谢（免疫冰冻并清除缓慢——肾脏系第二条）=====
        register("minecraft:snow_golem", BodySlot.KIDNEYS, new OrganEffect("minecraft:snow_golem", BodySlot.KIDNEYS,
                null, List.of(OrganPassive.ANTIFREEZE), null));

        // ===== 循声守卫：震怒之心（厚血坚甲 + 声呐侦测敌意 + 满级直线音爆）=====
        register("minecraft:warden", BodySlot.HEART, new OrganEffect("minecraft:warden", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 4.0),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR, 1.0)),
                List.of(OrganPassive.ENEMY_GLOW), OrganSpecial.SONIC_BOOM));

        // ===== 诅咒器官（高风险高回报）=====
        // 恶魂：怨灵之怒（攻击 +2.0 全库最高，代价火焰伤害 +50%——怒引火上身）
        register("minecraft:ghast", BodySlot.VISCERA, new OrganEffect("minecraft:ghast", BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 2.0)),
                List.of(OrganPassive.FIRE_WEAKNESS), null));

        // 僵尸疣猪兽：狂暴之心（生命 +6，代价移速 -12%/击退抗 -20%——鲁莽失衡之躯）
        register("minecraft:zoglin", BodySlot.HEART, new OrganEffect("minecraft:zoglin", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 6.0),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.012),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, -0.2)),
                null, null));

        // 卫道士：狂怒臂（攻击 +1.5 全臂最高，代价生命 -1/臂——狂怒消耗生命）
        register("minecraft:vindicator", BodySlot.LEFT_ARM, new OrganEffect("minecraft:vindicator", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.5),
                        new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, -1.0)),
                null, null));
        register("minecraft:vindicator", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:vindicator", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.5),
                        new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, -1.0)),
                null, null));

        // ===== 下界系（异变族，2026 基因完善化）=====
        // 猪灵：贪婪金瞳（眼槽——寻金血脉带来好运，恰逢眼槽冷门）
        register("minecraft:piglin", BodySlot.EYE, new OrganEffect("minecraft:piglin", BodySlot.EYE,
                List.of(new OrganTemplate.AttributeBonus(Attributes.LUCK, 0.75)), null, null));
        // 猪灵蛮兵：金斧蛮臂（双臂最高纯攻击加成，金甲护体加击退抗）
        register("minecraft:piglin_brute", BodySlot.LEFT_ARM, new OrganEffect("minecraft:piglin_brute", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.25),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), null, null));
        register("minecraft:piglin_brute", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:piglin_brute", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.25),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), null, null));
        // 疣猪兽：冲撞兽心（攻击+速度——命中的一瞬把目标顶开，呼应蓄力冲撞）
        register("minecraft:hoglin", BodySlot.HEART, new OrganEffect("minecraft:hoglin", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)),
                List.of(OrganPassive.KNOCKBACK_ON_HIT), null));
        // 岩浆怪：炽热熔肺（熔岩在体内循环如肺——命中点燃而非免疫火焰，与烈焰人心错开）
        register("minecraft:magma_cube", BodySlot.LUNGS, new OrganEffect("minecraft:magma_cube", BodySlot.LUNGS,
                null, List.of(OrganPassive.IGNITE_ON_HIT), null));
        // 女巫：炼药内脏（女巫酿造——常年煮药练就随手调药，周期性随机获得一杯正面药水）
        register("minecraft:witch", BodySlot.VISCERA, new OrganEffect("minecraft:witch", BodySlot.VISCERA,
                null, List.of(OrganPassive.WITCH_BREW), null));

        // ===== 已有生物补冷门槽（肾系 2026 完善化）=====
        // 苦力怕：硫磺代谢腺（肾——爆炸囊同源器官的代谢端；免疫自家爆炸反噬 30%）
        register("minecraft:creeper", BodySlot.KIDNEYS, new OrganEffect("minecraft:creeper", BodySlot.KIDNEYS,
                null, List.of(OrganPassive.BLAST_RESIST), null));

        // ===== 肾脏补全（内分泌/滤排系——变异生理才有差异化代谢，哺乳同质不硬塞）=====
        // 疣猪兽：狂暴激素肾（发狂冲动由肾上激素驱动——攻击速度 +0.35，无代价内分泌增益）
        register("minecraft:hoglin", BodySlot.KIDNEYS, new OrganEffect("minecraft:hoglin", BodySlot.KIDNEYS,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.35)), null, null));
        // 僵尸疣猪兽：尸变狂暴肾（尸化激素劣化——攻速 +0.3 但躯壳衰败扣 1 生命）
        register("minecraft:zoglin", BodySlot.KIDNEYS, new OrganEffect("minecraft:zoglin", BodySlot.KIDNEYS,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.3),
                        new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, -1.0)), null, null));
        // 卫道士：狂怒副肾（副肾素催动斧狂连斩——攻速 +0.4，代价：高代谢饥饿加剧——狂怒以食量为薪）
        register("minecraft:vindicator", BodySlot.KIDNEYS, new OrganEffect("minecraft:vindicator", BodySlot.KIDNEYS,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.4)),
                List.of(OrganPassive.RAPID_EXHAUSTION), null));
        // 骆驼：稳态代谢肾（沙漠储水——体液恒定不受粘滞减速拖累，激活 SLOW_IMMUNE 首宿主）
        register("minecraft:camel", BodySlot.KIDNEYS, new OrganEffect("minecraft:camel", BodySlot.KIDNEYS,
                null, List.of(OrganPassive.SLOW_IMMUNE), null));
        // 守卫者：高压滤压肾（深海鱼以肾调节渗透压对抗深海高压——水下攻击 +50%）
        register("minecraft:guardian", BodySlot.KIDNEYS, new OrganEffect("minecraft:guardian", BodySlot.KIDNEYS,
                null, List.of(OrganPassive.WATER_ATTACK_BOOST), null));

        // ===== 眼睛补全（视觉/感知系——眼为"视界"，生理差异集中在猎手/元素/亡灵三类）=====
        // 蜘蛛：八目夜视（节肢八单眼夜猎——黑处见如白昼，填补温血猫之外的夜视第二宿主）
        register("minecraft:spider", BodySlot.EYE, new OrganEffect("minecraft:spider", BodySlot.EYE,
                null, List.of(OrganPassive.NIGHT_VISION), null));
        // 恶魂：怨魂瞄眼（本体即浮游巨眼+火球手——投射物伤害加成挂其瞄准之眼）
        register("minecraft:ghast", BodySlot.EYE, new OrganEffect("minecraft:ghast", BodySlot.EYE,
                null, List.of(OrganPassive.PROJECTILE_BOOST), null));
        // 烈焰人：热像之眼（烈焰热感知——敌意生物热源高亮显形，常驻索敌）
        register("minecraft:blaze", BodySlot.EYE, new OrganEffect("minecraft:blaze", BodySlot.EYE,
                null, List.of(OrganPassive.ENEMY_GLOW), null));
        // 流浪者：霜瞳冻视（冰系亡灵射手——目之所击命中使目标减速，冰川瞳冻住猎物）
        register("minecraft:stray", BodySlot.EYE, new OrganEffect("minecraft:stray", BodySlot.EYE,
                null, List.of(OrganPassive.SLOW_ON_HIT), null));
    }

    private OrganEffectRegistry() {
    }

    /** 注册（槽位 map 懒创建，ConcurrentHashMap 语义由调用时机保证——仅静态初始化） */
    private static void register(String entityId, BodySlot slot, OrganEffect effect) {
        EFFECTS.computeIfAbsent(entityId, k -> new EnumMap<>(BodySlot.class)).put(slot, effect);
    }

    /** 查询生物 × 槽位的特色效果（未注册返回 null，此时沿用槽位模板） */
    public static OrganEffect get(String entityId, BodySlot slot) {
        if (entityId == null || entityId.isEmpty()) {
            return null;
        }
        Map<BodySlot, OrganEffect> map = EFFECTS.get(entityId);
        return map != null ? map.get(slot) : null;
    }

    /** 该生物可解析出器官的槽位列表（结构台可选目标；未注册任何器官返回空列表） */
    public static List<BodySlot> availableSlots(String entityId) {
        Map<BodySlot, OrganEffect> map = entityId == null ? null : EFFECTS.get(entityId);
        return map == null ? List.of() : List.copyOf(map.keySet());
    }

    /** 已注册的（生物×槽位）效果总数（数据驱动规模观测点） */
    public static int entryCount() {
        int total = 0;
        for (Map<BodySlot, OrganEffect> map : EFFECTS.values()) {
            total += map.size();
        }
        return total;
    }

    /** 已注册特色器官的生物数量 */
    public static int entityCount() {
        return EFFECTS.size();
    }

    /** 已注册器官的来源生物 id 集合（一致性测试/诊断用） */
    public static Set<String> registeredSources() {
        return Set.copyOf(EFFECTS.keySet());
    }
}
