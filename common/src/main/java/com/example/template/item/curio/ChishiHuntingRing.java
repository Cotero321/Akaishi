package com.example.template.item.curio;

import com.example.template.item.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 赤石狩猎指环（ring 槽）：玩家击杀生物时有 15% 概率获得 1 个赤石晶（直接进入背包）。
 * 无能量消耗，与"击杀掉落赤石精华"的剑能力形成互补（掉落物不同）。
 */
public class ChishiHuntingRing extends ChishiCurioItem {

    /** 容量：10 万赤能源（备用，当前效果不耗能） */
    private static final long CAPACITY = 100_000;
    /** 掉落概率（百分比） */
    private static final int DROP_CHANCE_PERCENT = 15;

    public ChishiHuntingRing(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public String[] curioSlots() {
        return new String[]{"ring"};
    }

    @Override
    protected String tooltipKey() {
        return "item.template_mod.curio.hunting";
    }

    @Override
    public void onKill(Player player, ItemStack stack, LivingEntity target) {
        if (!player.level().isClientSide && player.getRandom().nextInt(100) < DROP_CHANCE_PERCENT) {
            player.getInventory().add(new ItemStack(ModItems.chishiCrystal.get()));
        }
    }
}
