package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 赤石盔甲：基础防御/韧性由 AkaishiArmorMaterial 提供（下界合金 1.25 倍）。
 * 升级属性的动态附加由 Forge 平台的 ItemAttributeModifierEvent 实现
 * （原版 Item 无带 ItemStack 的 getAttributeModifiers，无法在 common 覆写）。
 * 穿戴纹理由 ArmorMaterial#getName() 返回的命名空间（akaishi:akaishi）
 * 由 Forge 默认 getArmorTexture 自动定位，无需在 common 覆写 Forge 专属接口。
 * tooltip 展示已生效升级与剩余槽位。
 */
public class AkaishiArmorItem extends ArmorItem {

    public AkaishiArmorItem(Type type, Properties properties) {
        super(AkaishiArmorMaterial.MATERIAL, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        AkaishiUpgradeHelper.ensureGear(stack);
        AkaishiUpgradeHelper.appendTooltip(stack, tooltip);
    }
}
