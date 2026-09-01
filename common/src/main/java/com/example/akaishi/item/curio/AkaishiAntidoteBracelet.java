package com.example.akaishi.item.curio;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 赤石净化手镯（bracelet 槽）：玩家处于中毒状态时消耗赤能源即时移除该效果。
 * 每次移除消耗 500 能量；能量不足时效果保留，下 tick 继续尝试。
 */
public class AkaishiAntidoteBracelet extends AkaishiCurioItem {

    /** 容量：20 万赤能源 */
    private static final long CAPACITY = 200_000;
    /** 单次移除消耗能量 */
    private static final long COST_PER_REMOVE = 500;

    public AkaishiAntidoteBracelet(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public String[] curioSlots() {
        return new String[]{"bracelet"};
    }

    @Override
    protected String tooltipKey() {
        return "item.akaishi.curio.antidote";
    }

    @Override
    public void curioTick(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        if (player.hasEffect(MobEffects.POISON)
                && tryConsume(player, stack, COST_PER_REMOVE) >= COST_PER_REMOVE) {
            player.removeEffect(MobEffects.POISON);
        }
    }
}
