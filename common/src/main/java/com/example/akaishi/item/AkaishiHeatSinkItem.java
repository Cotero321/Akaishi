package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 散热片：插入散热组件槽位的消耗品。
 * 品质决定散热效率（%）与耐久；只有反应堆成型且燃烧时消耗耐久，耐久归零后破碎消失。
 */
public class AkaishiHeatSinkItem extends Item {

    private final HeatSinkQuality quality;

    public AkaishiHeatSinkItem(HeatSinkQuality quality) {
        // 耐久只随反应堆运行自然消耗（无修复材料，铁砧无法修复）
        super(new Item.Properties().durability(quality.durability));
        this.quality = quality;
    }

    public HeatSinkQuality getQuality() {
        return quality;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.heat_sink.efficiency", quality.coolingPercent));
        tooltip.add(Component.translatable("gui.akaishi.heat_sink.durability", quality.durability));
    }
}
