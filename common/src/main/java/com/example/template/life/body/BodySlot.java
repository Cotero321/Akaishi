package com.example.template.life.body;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * 玩家躯体槽位：5 器官 + 4 肢体，共 9 个位置。
 * 每个槽位可移植一个器官物品；摘除原部件会损失生命值（extractDamage，无视护甲）。
 * 槽位排斥值（rejection）由基因系统驱动，越高负面效果越强。
 * 生命权重：心脏槽占 20%，其余各占 10%（空槽按权重扣减生命上限）。
 */
public enum BodySlot {

    // ===== 器官（5）=====
    EYE("eye", BodyPartType.ORGAN, 6.0f, 0.10, MobEffects.BLINDNESS, "body.template_mod.slot.eye"),
    HEART("heart", BodyPartType.ORGAN, 8.0f, 0.20, MobEffects.WEAKNESS, "body.template_mod.slot.heart"),
    LUNGS("lungs", BodyPartType.ORGAN, 8.0f, 0.10, MobEffects.WEAKNESS, "body.template_mod.slot.lungs"),
    // 内体：消化/内脏类器官的通用落点（牛胃、牛肝等）
    VISCERA("viscera", BodyPartType.ORGAN, 6.0f, 0.10, MobEffects.HUNGER, "body.template_mod.slot.viscera"),
    KIDNEYS("kidneys", BodyPartType.ORGAN, 6.0f, 0.10, MobEffects.POISON, "body.template_mod.slot.kidneys"),

    // ===== 肢体（4）=====
    LEFT_ARM("left_arm", BodyPartType.LIMB, 4.0f, 0.10, MobEffects.DIG_SLOWDOWN, "body.template_mod.slot.left_arm"),
    RIGHT_ARM("right_arm", BodyPartType.LIMB, 4.0f, 0.10, MobEffects.DIG_SLOWDOWN, "body.template_mod.slot.right_arm"),
    LEFT_LEG("left_leg", BodyPartType.LIMB, 4.0f, 0.10, MobEffects.MOVEMENT_SLOWDOWN, "body.template_mod.slot.left_leg"),
    RIGHT_LEG("right_leg", BodyPartType.LIMB, 4.0f, 0.10, MobEffects.MOVEMENT_SLOWDOWN, "body.template_mod.slot.right_leg");

    /** 注册 id（NBT/网络/物品 NBT 共用） */
    private final String id;
    private final BodyPartType type;
    /** 摘除该部位造成的生命值损失 */
    private final float extractDamage;
    /** 生命权重（0-1）：空槽按此比例扣减生命上限（心脏为两倍权重 0.2） */
    private final double lifeWeight;
    /** 移植低适配度器官时的部位 debuff */
    private final MobEffect debuff;
    /** 显示名翻译键 */
    private final String nameKey;

    BodySlot(String id, BodyPartType type, float extractDamage, double lifeWeight, MobEffect debuff, String nameKey) {
        this.id = id;
        this.type = type;
        this.extractDamage = extractDamage;
        this.lifeWeight = lifeWeight;
        this.debuff = debuff;
        this.nameKey = nameKey;
    }

    public String getId() {
        return id;
    }

    public BodyPartType getType() {
        return type;
    }

    public boolean isOrgan() {
        return type == BodyPartType.ORGAN;
    }

    public float getExtractDamage() {
        return extractDamage;
    }

    public double getLifeWeight() {
        return lifeWeight;
    }

    public MobEffect getDebuff() {
        return debuff;
    }

    public String getNameKey() {
        return nameKey;
    }

    /** 按 id 反查槽位（NBT 反序列化用） */
    public static BodySlot byId(String id) {
        for (BodySlot slot : values()) {
            if (slot.id.equals(id)) {
                return slot;
            }
        }
        return null;
    }
}
