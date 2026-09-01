package com.example.akaishi.life.organ;

import com.example.akaishi.life.body.BodySlot;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 器官模板注册表：9 个躯体槽位各一个基础模板（仅属性，特殊效果见 OrganEffectRegistry）。
 * 设计要点：
 * - 眼：远程输出
 * - 心：最大生命
 * - 肺：护甲韧性
 * - 内体：幸运（消化/内脏类器官通用落点）
 * - 肾：攻击
 * - 双臂：攻击 + 击退抗性（纯属性）
 * - 双腿：移速（左右独立，可同时生效）
 */
public final class OrganRegistry {

    private static final Map<BodySlot, OrganTemplate> TEMPLATES = new EnumMap<>(BodySlot.class);

    static {
        // 器官（5）
        register(new OrganTemplate(BodySlot.EYE,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.5))));
        register(new OrganTemplate(BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 2.0))));
        register(new OrganTemplate(BodySlot.LUNGS,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 0.5))));
        register(new OrganTemplate(BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.LUCK, 0.5))));
        register(new OrganTemplate(BodySlot.KIDNEYS,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.5))));
        // 肢体（4，左右独立）
        register(new OrganTemplate(BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05))));
        register(new OrganTemplate(BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05))));
        register(new OrganTemplate(BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015))));
        register(new OrganTemplate(BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015))));
    }

    private OrganRegistry() {
    }

    private static void register(OrganTemplate template) {
        TEMPLATES.put(template.slot(), template);
    }

    /** 查询槽位模板（无则返回 null） */
    public static OrganTemplate get(BodySlot slot) {
        return TEMPLATES.get(slot);
    }
}
