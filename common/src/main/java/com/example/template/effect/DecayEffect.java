package com.example.template.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 衰变效果：衰竭区域的核心减益。
 * 伤害不由效果自身结算（避免每 tick 刷新导致的伤害过频），
 * 由 {@link com.example.template.decay.DecayZoneManager} 按区域统一节奏施加魔法伤害：
 * 区域内实体每 tick 刷新 1 秒效果，区域每 20 tick 对实体造成 2×(等级+1) 点魔法伤害。
 * 亡灵免疫：施加对象在区域查询时已被过滤（{@code Mob.isInvertedHealAndHarm()}）。
 * 1.20.1 效果图标无需代码绑定：客户端按注册 ID 自动加载 textures/mob_effect/decay.png（18×18）。
 */
public class DecayEffect extends MobEffect {

    public DecayEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A8A4A);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false; // 伤害由衰竭区域统一施加
    }
}
