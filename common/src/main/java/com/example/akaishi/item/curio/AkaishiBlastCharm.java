package com.example.akaishi.item.curio;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 赤石防爆护符（body 槽）：受到爆炸伤害时消耗赤能源抵消伤害（1 点伤害耗 100 能量）。
 * 能量不足以全额抵消时按可用能量比例减免，能量耗尽后无防护效果。
 */
public class AkaishiBlastCharm extends AkaishiCurioItem {

    /** 容量：100 万赤能源 */
    private static final long CAPACITY = 1_000_000;
    /** 每点伤害消耗能量 */
    private static final long ENERGY_PER_DAMAGE = 100;

    public AkaishiBlastCharm(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public String[] curioSlots() {
        return new String[]{"body"};
    }

    @Override
    protected String tooltipKey() {
        return "item.akaishi.curio.blast";
    }

    @Override
    public float onHurt(Player player, ItemStack stack, float amount, DamageSource source) {
        if (amount <= 0 || !source.is(DamageTypes.EXPLOSION)) {
            return amount;
        }
        long needed = (long) (amount * ENERGY_PER_DAMAGE);
        long consumed = tryConsume(player, stack, needed);
        return Math.max(0, amount - consumed / (float) ENERGY_PER_DAMAGE);
    }
}
