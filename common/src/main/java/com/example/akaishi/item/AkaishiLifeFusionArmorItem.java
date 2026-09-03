package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 生命融合护甲：基础防御/韧性由 AkaishiLifeFusionArmorMaterial 提供（赤石装备 2 倍）。
 * 保留赤石装备的升级 NBT（AkaishiGear），tooltip 展示已生效升级与剩余槽位。
 */
public class AkaishiLifeFusionArmorItem extends ArmorItem {

    public AkaishiLifeFusionArmorItem(Type type, Properties properties) {
        super(AkaishiLifeFusionArmorMaterial.MATERIAL, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        AkaishiUpgradeHelper.ensureGear(stack);
        AkaishiUpgradeHelper.appendTooltip(stack, tooltip);
    }
}
