package com.example.template.life.organ;

import com.example.template.life.body.BodySlot;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.List;

/**
 * 器官槽位模板：定义该槽位器官的基础属性加成。
 * 属性加成 = base × 品质倍率；特殊效果不再绑定槽位，
 * 由"器官来源基因 × 槽位"组合差异化定义（见 OrganEffectRegistry）。
 */
public record OrganTemplate(BodySlot slot, List<AttributeBonus> bonuses) {

    /** 单项属性加成（基础值，最终值乘以品质倍率） */
    public record AttributeBonus(Attribute attribute, double base) {
    }
}
