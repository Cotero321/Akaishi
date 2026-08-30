package com.example.template.item.curio;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 赤石防火吊坠（necklace 槽）：持续消耗赤能源维持玩家防火效果（I 级，无粒子）。
 * 每 tick 消耗 20 能量，能量充足时持续刷新 2 秒防火；能量耗尽后效果自然消退。
 */
public class ChishiFireNecklace extends ChishiCurioItem {

    /** 容量：100 万赤能源 */
    private static final long CAPACITY = 1_000_000;
    /** 每 tick 消耗能量 */
    private static final long COST_PER_TICK = 20;

    public ChishiFireNecklace(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public String[] curioSlots() {
        return new String[]{"necklace"};
    }

    @Override
    protected String tooltipKey() {
        return "item.template_mod.curio.fire";
    }

    @Override
    public void curioTick(Level level, Player player, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        if (!player.hasEffect(MobEffects.FIRE_RESISTANCE)
                && tryConsume(player, stack, COST_PER_TICK) >= COST_PER_TICK) {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
        }
    }
}
