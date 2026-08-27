package com.example.template.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 赤石斧：基础攻击伤害 = 下界合金斧 1.25 倍（7+4+1=12，下界合金 10），攻击速度 1.25（下界合金 1.0）。
 * 升级属性的动态附加由 Forge 平台的 ItemAttributeModifierEvent 实现。
 */
public class ChishiAxeItem extends AxeItem {

    public ChishiAxeItem(Properties properties) {
        super(Tiers.NETHERITE, 7.0F, -2.75F, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        ChishiUpgradeHelper.ensureGear(stack);
        ChishiUpgradeHelper.appendTooltip(stack, tooltip);
    }
}
