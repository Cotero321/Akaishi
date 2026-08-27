package com.example.template.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 赤石盔甲：基础防御/韧性由 ChishiArmorMaterial 提供（下界合金 1.25 倍）。
 * 升级属性的动态附加由 Forge 平台的 ItemAttributeModifierEvent 实现
 * （原版 Item 无带 ItemStack 的 getAttributeModifiers，无法在 common 覆写）。
 * tooltip 展示已生效升级与剩余槽位。
 */
public class ChishiArmorItem extends ArmorItem {

    public ChishiArmorItem(Type type, Properties properties) {
        super(ChishiArmorMaterial.MATERIAL, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        ChishiUpgradeHelper.ensureGear(stack);
        ChishiUpgradeHelper.appendTooltip(stack, tooltip);
    }
}
