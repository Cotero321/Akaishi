package com.example.akaishi.life.sample;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 生命胚胎：八份生命精华与一枚鸡蛋凝聚出的"生命之始"。
 * 尚无具体形态（无 NBT 铭刻），供母神祭坛献祭，并为后续培育玩法预留扩展位。
 */
public class AkaishiLifeEmbryoItem extends Item {

    public AkaishiLifeEmbryoItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.akaishi.life_embryo.desc"));
    }
}
