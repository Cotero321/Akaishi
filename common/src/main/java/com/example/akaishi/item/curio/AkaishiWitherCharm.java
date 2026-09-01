package com.example.akaishi.item.curio;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 赤石凋零护符（belt 槽）：玩家处于凋零状态时消耗赤能源即时移除该效果。
 * 每次移除消耗 1000 能量（高于中毒），凋零是本模组与末地体系联动的高威胁状态。
 */
public class AkaishiWitherCharm extends AkaishiCurioItem {

    /** 容量：20 万赤能源 */
    private static final long CAPACITY = 200_000;
    /** 单次移除消耗能量 */
    private static final long COST_PER_REMOVE = 1000;

    public AkaishiWitherCharm(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public String[] curioSlots() {
        return new String[]{"belt"};
    }

    @Override
    protected String tooltipKey() {
        return "item.akaishi.curio.wither";
    }

    @Override
    public void curioTick(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        if (player.hasEffect(MobEffects.WITHER)
                && tryConsume(player, stack, COST_PER_REMOVE) >= COST_PER_REMOVE) {
            player.removeEffect(MobEffects.WITHER);
        }
    }
}
