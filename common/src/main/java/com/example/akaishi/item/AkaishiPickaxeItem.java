package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 赤石镐：基础攻击伤害 = 下界合金镐 1.25 倍（2+4=6，下界合金 5），攻击速度 1.5（下界合金 1.2）。
 * 升级属性的动态附加由 Forge 平台的 ItemAttributeModifierEvent 实现，效率升级由挖掘速度事件实现。
 */
public class AkaishiPickaxeItem extends PickaxeItem {

    public AkaishiPickaxeItem(Properties properties) {
        super(Tiers.NETHERITE, 2, -2.5F, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        AkaishiUpgradeHelper.ensureGear(stack);
        AkaishiUpgradeHelper.appendTooltip(stack, tooltip);
    }
}
