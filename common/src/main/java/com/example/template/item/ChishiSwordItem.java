package com.example.template.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 赤石剑：基础攻击伤害 = 下界合金剑 1.25 倍（4+7=11，下界合金 9）。
 * 升级属性的动态附加由 Forge 平台的 ItemAttributeModifierEvent 实现。
 * tooltip 展示已生效升级与剩余槽位。
 */
public class ChishiSwordItem extends SwordItem {

    public ChishiSwordItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        ChishiUpgradeHelper.ensureGear(stack);
        ChishiUpgradeHelper.appendTooltip(stack, tooltip);
    }
}
