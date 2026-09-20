package com.example.akaishi.boss.agaitolos.skill;

import com.example.akaishi.boss.agaitolos.AgaitolosCombat;
import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 一阶段普通攻击：挥舞武器，造成凋零 III（1s）并追加 4 点真实伤害；
 * 若目标在命中<b>前</b>已带凋零，则真实伤害翻倍。
 * <p>
 * 结算顺序不可调换：先记录"是否已凋零"，再施加新凋零——否则新效果会先落地，判定恒为 true。
 */
public final class AgaitolosMeleeSkill {

    /** 凋零持续时长（tick）：1s。P8 转配置项 */
    public static final int WITHER_DURATION_TICKS = 20;

    /** 凋零等级放大器：2 = 凋零 III（amplifier 从 0 起算）。P8 转配置项 */
    public static final int WITHER_AMPLIFIER = 2;

    /** 真实伤害基础值。P8 转配置项 */
    public static final float TRUE_DAMAGE = 4.0F;

    /** 目标已带凋零时的真实伤害倍率。P8 转配置项 */
    public static final float TRUE_DAMAGE_MULTIPLIER_WHEN_WITHERED = 2.0F;

    private AgaitolosMeleeSkill() {
    }

    /**
     * 执行一次普攻（仅服务端有效，由 {@link AgaitolosEntity#doHurtTarget} 调用）。
     *
     * @return 是否真正打出这一击（目标非生物或物理段未命中时为 false）
     */
    public static boolean perform(AgaitolosEntity boss, Entity target) {
        if (!(target instanceof LivingEntity living)) {
            return false;
        }
        // 必须在施加新凋零之前判定，否则永远为 true（新效果已落地）
        boolean alreadyWithered = living.hasEffect(MobEffects.WITHER);

        // 物理段：单走原版 Mob#doHurtTarget（攻击力 / 附魔 / 击退等原版结算）
        if (!boss.doHurtTargetPhysical(target)) {
            return false;
        }

        // 凋零段：施加者记为本 BOSS，保证击杀归属正确
        living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION_TICKS, WITHER_AMPLIFIER), boss);

        // 真实段：无视护甲/抗性/附魔，且因 bypasses_cooldown 不会被物理段的无敌帧吞掉。
        // 必须带 causingEntity = BOSS（弹体传 null）：否则 source.getEntity() 为 null，
        // 原版只会取"无攻击者"的死亡消息，death.attack.akaishi.true_damage.player 这条双语键永远用不到，
        // 且击杀归属丢失。
        float amount = TRUE_DAMAGE * (alreadyWithered ? TRUE_DAMAGE_MULTIPLIER_WHEN_WITHERED : 1.0F);
        living.hurt(AgaitolosCombat.trueDamage(boss.level(), null, boss), amount);
        return true;
    }
}
