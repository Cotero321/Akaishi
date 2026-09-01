package com.example.akaishi.life.organ;

import com.example.akaishi.life.body.BodySlot;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // ===== 猫：猫眼（夜视）=====
        register("minecraft:cat", BodySlot.EYE, new OrganEffect("minecraft:cat", BodySlot.EYE,
                null, List.of(OrganPassive.NIGHT_VISION), null));

        // ===== 狼：狼爪（+攻击）=====
        register("minecraft:wolf", BodySlot.LEFT_ARM, new OrganEffect("minecraft:wolf", BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), null, null));
        register("minecraft:wolf", BodySlot.RIGHT_ARM, new OrganEffect("minecraft:wolf", BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), null, null));

        // ===== 狐狸：狐狸嘴（跳跃攻击伤害提升）=====
        register("minecraft:fox", BodySlot.VISCERA, new OrganEffect("minecraft:fox", BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.LUCK, 0.75)),
                List.of(OrganPassive.JUMP_ATTACK_BOOST), null));

        // ===== 蝙蝠：回声定位耳（侦测敌意生物高亮）=====
        register("minecraft:bat", BodySlot.VISCERA, new OrganEffect("minecraft:bat", BodySlot.VISCERA,
                null, List.of(OrganPassive.ENEMY_GLOW), null));

        // ===== 蜘蛛：蛛丝腺（攻击减速目标）=====
        register("minecraft:spider", BodySlot.VISCERA, new OrganEffect("minecraft:spider", BodySlot.VISCERA,
                null, List.of(OrganPassive.SLOW_ON_HIT), null));

        // ===== 史莱姆：黏液腺（弹性：跳跃 + 摔落免疫）=====
        register("minecraft:slime", BodySlot.LUNGS, new OrganEffect("minecraft:slime", BodySlot.LUNGS,
                null, List.of(OrganPassive.JUMP_BOOST, OrganPassive.FALL_IMMUNE), null));

        // ===== 鸡：鸡砂囊（食物恢复 +25%）=====
        register("minecraft:chicken", BodySlot.VISCERA, new OrganEffect("minecraft:chicken", BodySlot.VISCERA,
                null, List.of(OrganPassive.FOOD_BOOST), null));

        // ===== 猪：猪心（额外生命值）=====
        register("minecraft:pig", BodySlot.HEART, new OrganEffect("minecraft:pig", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 3.0)), null, null));

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

        // ===== 骷髅：骷髅骨架（骨甲 + 弹射物强化）=====
        register("minecraft:skeleton", BodySlot.VISCERA, new OrganEffect("minecraft:skeleton", BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ARMOR, 1.0)),
                List.of(OrganPassive.PROJECTILE_BOOST), null));

        // ===== 末影人：末影核心（瞬移闪避，负面：怕水）=====
        register("minecraft:enderman", BodySlot.HEART, new OrganEffect("minecraft:enderman", BodySlot.HEART,
                null, List.of(OrganPassive.TELEPORT_DODGE), OrganSpecial.ENDER_WATER_FEAR));

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

        // ===== 海龟：龟之心（水下攻击强化）=====
        register("minecraft:turtle", BodySlot.HEART, new OrganEffect("minecraft:turtle", BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 1.0)),
                List.of(OrganPassive.WATER_ATTACK_BOOST), null));

        // ===== 鳕鱼：鱼鳃（水下呼吸）=====
        register("minecraft:cod", BodySlot.LUNGS, new OrganEffect("minecraft:cod", BodySlot.LUNGS,
                null, List.of(OrganPassive.WATER_BREATHING), null));

        // ===== 山羊：山羊腿（跳跃 + 轻快）=====
        register("minecraft:goat", BodySlot.LEFT_LEG, new OrganEffect("minecraft:goat", BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015)),
                List.of(OrganPassive.JUMP_BOOST), null));
        register("minecraft:goat", BodySlot.RIGHT_LEG, new OrganEffect("minecraft:goat", BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015)),
                List.of(OrganPassive.JUMP_BOOST), null));

        // ===== 苦力怕：爆炸囊（无属性，唯一意义是触发猫×苦力怕的天敌排异）=====
        register("minecraft:creeper", BodySlot.VISCERA, new OrganEffect("minecraft:creeper", BodySlot.VISCERA,
                null, null, null));
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
}
