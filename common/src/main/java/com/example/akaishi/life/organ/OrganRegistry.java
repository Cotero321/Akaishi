package com.example.akaishi.life.organ;

import com.example.akaishi.combat.ModCombatAttributes;
import com.example.akaishi.life.body.BodySlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 器官模板注册表：9 个躯体槽位各一个基础模板（仅属性，特殊效果见 OrganEffectRegistry）。
 * 设计要点：
 * - 眼：暴击率（精准瞄准，输出靠暴击而非裸攻）
 * - 心：最大生命
 * - 肺：无属性（呼吸/滑翔等功能槽，属性由生物特色覆盖承载）
 * - 内体：幸运 + 护甲韧性（防御与消化/内脏类器官通用落点）
 * - 肾：攻击
 * - 双臂：攻击 + 击退抗性 + 暴击伤害（重击破甲）
 * - 双腿：移速 + 闪避（身法位移，左右独立，可同时生效）
 * 属性↔槽位合法性矩阵以本类为唯一真源，其它域只允许在合法槽位上做覆盖，不得越界。
 */
public final class OrganRegistry {

    private static final Map<BodySlot, OrganTemplate> TEMPLATES = new EnumMap<>(BodySlot.class);

    static {
        // 器官（5）
        register(new OrganTemplate(BodySlot.EYE,
                List.of(new OrganTemplate.AttributeBonus(ModCombatAttributes.CRIT_CHANCE.get(), 0.03))));
        register(new OrganTemplate(BodySlot.HEART,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 2.0))));
        // 肺为功能槽：无基础属性，特色属性由生物覆盖承载（维持"呼吸/滑翔"语义纯净）
        register(new OrganTemplate(BodySlot.LUNGS, List.of()));
        register(new OrganTemplate(BodySlot.VISCERA,
                List.of(new OrganTemplate.AttributeBonus(Attributes.LUCK, 0.5),
                        new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 0.5))));
        register(new OrganTemplate(BodySlot.KIDNEYS,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.5))));
        // 肢体（4，左右独立）
        register(new OrganTemplate(BodySlot.LEFT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05),
                        new OrganTemplate.AttributeBonus(ModCombatAttributes.CRIT_DAMAGE.get(), 0.10))));
        register(new OrganTemplate(BodySlot.RIGHT_ARM,
                List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.75),
                        new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05),
                        new OrganTemplate.AttributeBonus(ModCombatAttributes.CRIT_DAMAGE.get(), 0.10))));
        register(new OrganTemplate(BodySlot.LEFT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015),
                        new OrganTemplate.AttributeBonus(ModCombatAttributes.DODGE_CHANCE.get(), 0.02))));
        register(new OrganTemplate(BodySlot.RIGHT_LEG,
                List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015),
                        new OrganTemplate.AttributeBonus(ModCombatAttributes.DODGE_CHANCE.get(), 0.02))));
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

    /**
     * 矩阵判定：属性是否允许出现在该槽位。正负值同受约束——惩罚代价也必须落在该槽位的合法轴上，
     * 不存在"全身可拖累"的例外（代价要表达，就迁到它真正归属的槽位或折入该槽位主属性）。
     * 未列入矩阵的属性不作约束；左右肢互为等价。
     */
    public static boolean allows(BodySlot slot, Attribute attribute) {
        if (slot == null || attribute == null) {
            return true;
        }
        if (attribute == ModCombatAttributes.CRIT_CHANCE.get()) {
            return slot == BodySlot.EYE;
        }
        if (attribute == ModCombatAttributes.CRIT_DAMAGE.get()) {
            return isArm(slot);
        }
        if (attribute == ModCombatAttributes.DODGE_CHANCE.get()) {
            return isLeg(slot);
        }
        if (attribute == Attributes.MAX_HEALTH) {
            return slot == BodySlot.HEART;
        }
        if (attribute == Attributes.LUCK) {
            return slot == BodySlot.VISCERA;
        }
        if (attribute == Attributes.MOVEMENT_SPEED) {
            return isLeg(slot);
        }
        if (attribute == Attributes.ARMOR) {
            return slot == BodySlot.VISCERA;
        }
        if (attribute == Attributes.ARMOR_TOUGHNESS) {
            return slot == BodySlot.VISCERA;
        }
        if (attribute == Attributes.ATTACK_DAMAGE) {
            return slot == BodySlot.KIDNEYS || isArm(slot);
        }
        if (attribute == Attributes.ATTACK_SPEED) {
            return isArm(slot);
        }
        if (attribute == Attributes.KNOCKBACK_RESISTANCE) {
            // 击退抗性=抗击退的体魄，仅肢体（臂/腿）承载，内体不可给
            return isArm(slot) || isLeg(slot);
        }
        // 未列入矩阵的属性一律放行。肺槽（LUNGS）刻意不落入任何已列属性分支：
        // 肺为纯被动机能（呼吸/鳃类词条），属性增益由词条 SLOT_ONLY 与模板自行约束
        return true;
    }

    private static boolean isArm(BodySlot slot) {
        return slot == BodySlot.LEFT_ARM || slot == BodySlot.RIGHT_ARM;
    }

    private static boolean isLeg(BodySlot slot) {
        return slot == BodySlot.LEFT_LEG || slot == BodySlot.RIGHT_LEG;
    }
}
