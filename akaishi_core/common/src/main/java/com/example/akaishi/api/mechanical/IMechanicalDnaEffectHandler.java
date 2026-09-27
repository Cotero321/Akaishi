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
 * 所有回调均运行在服务端，且仅在玩家装备了带该效果的机械器官时触发。
 * <p>
 * <b>参数语义（平衡收敛）</b>：{@code level} = <b>跨器官有效等级</b>（1~4）——
 * 单器官等级 = clamp(该 DNA 在本器官 4 部件中的出现次数, 1, 4)，跨器官取各器官等级的最大值；
 * 分发层<b>每个效果只回调一次</b>（不再逐器官调用、不跨器官累计等级）。
 */
public interface IMechanicalDnaEffectHandler {

    /** 玩家每 tick 末回调（可做常驻 buff / 周期恢复）。{@code level} = 跨器官有效等级（1~4）。 */
    default void onPlayerTick(Player player, int level) {
    }

    /** 玩家命中目标后回调（可附加药水、点燃等）。{@code level} = 跨器官有效等级（1~4）。 */
    default void onAttack(Player attacker, LivingEntity target, int level) {
    }

    /** 玩家命中结算前回调，返回修正后的伤害值；默认原样返回。{@code level} = 跨器官有效等级（1~4）。 */
    default float modifyOutgoingDamage(Player attacker, LivingEntity target, float amount, int level) {
        return amount;
    }

    /** 玩家击杀目标后回调（可做击杀回血等）。{@code level} = 跨器官有效等级（1~4）。 */
    default void onKill(Player player, LivingEntity victim, int level) {
    }

    /** 玩家受击结算前回调，返回修正后的伤害值；默认原样返回。{@code level} = 跨器官有效等级（1~4）。 */
    default float modifyIncomingDamage(Player player, DamageSource source, float amount, int level) {
        return amount;
    }

    /** 玩家被击退结算前回调，返回修正后的击退强度；默认原样返回。{@code level} = 跨器官有效等级（1~4）。 */
    default float modifyKnockback(Player player, float strength, int level) {
        return strength;
    }

    /** 该效果的跨器官有效等级生效时回调（用于挂载临时属性修饰符）。{@code level} = 跨器官有效等级（1~4）。 */
    default void onEquip(Player player, int level) {
    }

    /** 该效果的跨器官有效等级失效时回调（用于清理临时属性修饰符）。{@code level} = 失效前的有效等级（1~4）。 */
    default void onUnequip(Player player, int level) {
    }
}
