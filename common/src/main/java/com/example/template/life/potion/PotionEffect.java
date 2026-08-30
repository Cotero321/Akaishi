package com.example.template.life.potion;

import net.minecraft.world.effect.MobEffect;

/**
 * 生物药剂效果：定义"具体生物来源"的药剂的差异化功效。
 * - compatBonus：永久药剂的适配度加成（按生物定制，默认 15）
 * - sideEffects：突破药剂的副作用池（按生物定制，未注册生物回退通用池）
 * 副作用强度仍由纯度统一控制（纯度越低越强），此处仅差异化副作用"种类"。
 */
public record PotionEffect(String entityId, int compatBonus, MobEffect[] sideEffects) {
}
