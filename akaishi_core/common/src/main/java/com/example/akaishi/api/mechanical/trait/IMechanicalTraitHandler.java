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
 * <b>调用模型（平衡收敛：单器官计数取最大 + 单次回调）</b>：分发层先取跨器官最高等级作为「有效等级」
 * （单器官等级 = clamp(该材料在本器官 4 部件中的出现次数, 1, maxLevel)；跨器官取各器官等级的最大值），
 * 再<b>每个特性只回调一次</b>；不再逐器官调用、也不存在按器官的多次叠加。
 * <p>
 * 所有回调均运行在服务端，且仅在玩家装备了带该特性的机械器官时触发。
 *
 * @param level 跨器官有效等级（各器官单器官等级的最大值，1 ~ {@link IMechanicalTrait#maxLevel()}）
 */
public interface IMechanicalTraitHandler {

    /** 该特性的跨器官有效等级出现或变化时回调一次（不再是逐器官回调）。 */
    default void onEquip(Player player, int level) {
    }

    /** 该特性的跨器官有效等级失效或变化时回调一次（不再是逐器官回调）。 */
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
