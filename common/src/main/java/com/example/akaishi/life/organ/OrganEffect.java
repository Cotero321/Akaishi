package com.example.akaishi.life.organ;

import com.example.akaishi.life.body.BodySlot;

import java.util.List;

/**
 * 单个生物器官的效果定义（按"具体生物 × 槽位"差异化）。
 * - attributes：覆盖槽位模板的属性集（null = 沿用模板默认属性）
 * - passives：被动技能列表（可空）
 * - special：独特机制（可空）
 * 属性基础值同样乘以品质倍率（QualityTier）。
 */
public record OrganEffect(String entityId, BodySlot slot,
                          List<OrganTemplate.AttributeBonus> attributes,
                          List<OrganPassive> passives,
                          OrganSpecial special) {
}
