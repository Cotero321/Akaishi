package com.example.akaishi.api.mechanical;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 机械 DNA 效果的运行时执行钩子。
 * <p>
 * 内置效果的实际逻辑由平台侧处理器实现；附属模组若要让自定义效果真正生效，
 * 需为效果实现本接口并在 {@link MechanicalEffectHandlerRegistry} 注册。
 * 未注册处理器的效果只展示、不生效（不会报错）。
 * <p>
 * 所有回调均运行在服务端，且仅在玩家装备了带该效果的机械器官时触发；
 * {@code sources} 为当前携带该效果的器官数量（≥1），供强度叠加使用。
 */
public interface IMechanicalDnaEffectHandler {

    /** 玩家每 tick 末回调（可做常驻 buff / 周期恢复）。 */
    default void onPlayerTick(Player player, int sources) {
    }

    /** 玩家命中目标后回调（可附加药水、点燃等）。 */
    default void onAttack(Player attacker, LivingEntity target, int sources) {
    }

    /** 玩家受击结算前回调，返回修正后的伤害值；默认原样返回。 */
    default float modifyIncomingDamage(Player player, DamageSource source, float amount, int sources) {
        return amount;
    }

    /** 玩家被击退结算前回调，返回修正后的击退强度；默认原样返回。 */
    default float modifyKnockback(Player player, float strength, int sources) {
        return strength;
    }
}
