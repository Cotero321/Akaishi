package com.example.akaishi.api.mechanical.trait;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 机械材料特性的运行时执行钩子。
 * <p>
 * 内置特性的实际逻辑由本体侧处理器实现；附属模组若要让自定义特性真正生效，
 * 需为特性实现本接口并在 {@link MechanicalTraitHandlerRegistry} 注册。
 * 未注册处理器的特性只展示、不生效（不会报错）。
 * <p>
 * <b>调用模型（M2：各器官各自生效）</b>：所有回调都按「器官」逐个触发 ——
 * 玩家身上每个携带该特性的器官，都会以自己的 {@code level} 调用一次回调；
 * 因此多个低级器官会各自叠加调用（不存在跨器官的等级合并）。
 * <p>
 * 所有回调均运行在服务端，且仅在玩家装备了带该特性的机械器官时触发。
 *
 * @param level 该器官内本特性的等级（1 ~ {@link IMechanicalTrait#maxLevel()}）
 */
public interface IMechanicalTraitHandler {

    /** 器官被装上时回调（每个带该特性的器官一次）。 */
    default void onEquip(Player player, int level) {
    }

    /** 器官被卸下时回调（每个带该特性的器官一次）。 */
    default void onUnequip(Player player, int level) {
    }

    /** 玩家每 tick 末回调（可做常驻 buff / 周期恢复）。 */
    default void onPlayerTick(Player player, int level) {
    }

    /** 玩家命中目标后回调（可附加药水、点燃等）。 */
    default void onAttack(Player attacker, LivingEntity target, int level) {
    }

    /** 玩家造成伤害结算前回调，返回修正后的伤害值；默认原样返回。 */
    default float modifyOutgoingDamage(Player attacker, LivingEntity target, float amount, int level) {
        return amount;
    }

    /** 玩家受击结算前回调，返回修正后的伤害值；默认原样返回。 */
    default float modifyIncomingDamage(Player player, DamageSource source, float amount, int level) {
        return amount;
    }

    /** 玩家击杀目标后回调。 */
    default void onKill(Player player, LivingEntity victim, int level) {
    }
}
