package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 聚变散热片：通过聚变控制器热量页 GUI 放入控制器散热片容器（结构内散热框架决定可用槽数）的消耗品。
 * 品质决定散热效率（%）与耐久；聚变堆运行或过热宕机降温期间每 100 tick 消耗 1 点耐久，
 * 耐久归零后破碎消失。
 */
public class AkaishiFusionHeatSinkItem extends Item {

    private final FusionHeatSinkQuality quality;

    public AkaishiFusionHeatSinkItem(FusionHeatSinkQuality quality) {
        super(new Item.Properties().durability(quality.durability));
        this.quality = quality;
    }

    public FusionHeatSinkQuality getQuality() {
        return quality;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.fusion_heat_sink.efficiency", quality.coolingPercent));
        tooltip.add(Component.translatable("gui.akaishi.fusion_heat_sink.durability", quality.durability));
    }
}
