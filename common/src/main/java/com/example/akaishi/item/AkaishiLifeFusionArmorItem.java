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
 * 套装效果说明（每件/全套/条件）由 AkaishiLifeFusionTooltip 追加；
 * 实时穿戴状态（已穿件数/激活情况）由 forge 端 ItemTooltipEvent 注入。
 */
public class AkaishiLifeFusionArmorItem extends ArmorItem {

    public AkaishiLifeFusionArmorItem(Type type, Properties properties) {
        super(AkaishiLifeFusionArmorMaterial.MATERIAL, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        AkaishiUpgradeHelper.ensureGear(stack);
        AkaishiUpgradeHelper.appendTooltip(stack, tooltip);
        // 套装效果说明为静态文案，不依赖玩家，始终渲染
        AkaishiLifeFusionTooltip.appendEffectLines(tooltip);
    }
}
