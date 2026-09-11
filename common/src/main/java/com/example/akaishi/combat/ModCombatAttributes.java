package com.example.akaishi.combat;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * 模组底层战斗属性：暴击率 / 暴击伤害 / 闪避。
 * 三个概念不隶属于「机械义体」或「生命工程」任一数值线，故独立成注册表，
 * 两条线都通过标准属性修饰符挂载（各线只负责加值，判定逻辑统一在本层消费）。
 * 注意：注册只完成"存在"，玩家身上生效还需平台侧把属性挂到 {@code EntityType.PLAYER}。
 */
public final class ModCombatAttributes {

    /** 属性 descriptionId 前缀，同时是 lang key 前缀（器官 tooltip 直接 translatable(getDescriptionId)） */
    public static final String LANG_PREFIX = "attribute.name." + AkaishiMod.MOD_ID + ".";

    /** 暴击率：普通攻击触发暴击的概率（0~1，1 = 必定暴击） */
    public static RegistrySupplier<Attribute> CRIT_CHANCE;
    /** 暴击伤害：暴击时追加的伤害倍率（0.5 = 默认倍率 ×1.5，与原版跳劈暴击一致） */
    public static RegistrySupplier<Attribute> CRIT_DAMAGE;
    /** 闪避：受物理伤害时完全免除该次伤害的概率（0~1） */
    public static RegistrySupplier<Attribute> DODGE_CHANCE;

    private ModCombatAttributes() {
    }

    /** 由 {@link AkaishiMod#init()} 在 mod 事件总线内调用（注册表冻结前） */
    public static void register() {
        Registrar<Attribute> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ATTRIBUTE);
        CRIT_CHANCE = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "crit_chance"),
                () -> new RangedAttribute(LANG_PREFIX + "crit_chance", 0.0, 0.0, 1.0).setSyncable(true));
        CRIT_DAMAGE = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "crit_damage"),
                () -> new RangedAttribute(LANG_PREFIX + "crit_damage", 0.5, 0.0, 100.0).setSyncable(true));
        DODGE_CHANCE = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "dodge_chance"),
                () -> new RangedAttribute(LANG_PREFIX + "dodge_chance", 0.0, 0.0, 1.0).setSyncable(true));
    }
}
