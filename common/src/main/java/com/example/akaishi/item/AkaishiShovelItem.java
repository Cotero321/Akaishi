package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 赤石铲：基础攻击伤害 = 下界合金铲 1.25 倍（4+4=8，下界合金 6.5），攻击速度 1.25（下界合金 1.0）。
 * 升级属性的动态附加由 Forge 平台的 ItemAttributeModifierEvent 实现。
 */
public class AkaishiShovelItem extends ShovelItem {

    public AkaishiShovelItem(Properties properties) {
        super(Tiers.NETHERITE, 4.0F, -2.75F, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        AkaishiUpgradeHelper.ensureGear(stack);
        AkaishiUpgradeHelper.appendTooltip(stack, tooltip);
    }
}
