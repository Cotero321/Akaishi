package com.example.akaishi.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 凋零骨片（凋零藤产物）：满格摆法兑换出的凋零骷髅骨片，9 片可在工作台拼成 1 个凋零骷髅头。
 * <p>刻意做成"碎片"而不是直接给头颅：头颅是通往凋零 Boss 的关键物，需要多轮收获累积，
 * 而不是一次兑换就拿到。
 */
public class AkaishiWitherBoneShardItem extends Item {

    public AkaishiWitherBoneShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.akaishi.akaishi_wither_bone_shard.desc"));
    }
}
