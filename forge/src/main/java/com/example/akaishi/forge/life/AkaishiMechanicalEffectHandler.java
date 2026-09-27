package com.example.akaishi.forge.life;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffectHandler;
import com.example.akaishi.api.mechanical.MechanicalEffectHandlerRegistry;
import com.example.akaishi.api.mechanical.trait.IMechanicalTraitHandler;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitHandlerRegistry;
import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.mechanical.MechanicalSpecialEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机械义体效果处理器（Forge 服务端，平衡收敛：汇总后单次回调）。
 * <p>
 * <b>分发模型</b>：先经 {@link MechanicalAggregation} 对每个「特性 ID / DNA 效果 ID」跨器官汇总出
 * <b>唯一有效等级</b>，再<b>每种条目只回调 handler 一次</b>（参数即有效等级）。
 * 不再逐器官调用，跨器官叠加被 {@code clamp(max + 器官数 - 1, 1, maxLevel)} 封顶，避免线性失控。
 * <ul>
 *   <li>材料特性：{@link MechanicalTraitHandlerRegistry} 按特性 ID 取处理器，每个特性一次钩子调用；</li>
 *   <li>DNA 效果：{@link MechanicalEffectHandlerRegistry} 按效果 ID 取处理器，每个效果一次调用；</li>
 *   <li>内置效果曲线见 {@link MechanicalDnaEffects}；</li>
 *   <li>装备变更（onEquip / onUnequip）按「跨器官有效等级集合」的差集触发。</li>
 * </ul>
 * 暴击强化由 {@link #critBonus(IPlayerBodyState)} 供战斗处理器求和，避免二次暴击判定。
 */
public final class AkaishiMechanicalEffectHandler {

    public static final AkaishiMechanicalEffectHandler INSTANCE = new AkaishiMechanicalEffectHandler();

    /** 每玩家「上次见到的器官」快照，用于 onEquip / onUnequip 差分（登出时清理）。 */
    private static final Map<UUID, EnumMap<BodySlot, ItemStack>> LAST_ORGANS = new ConcurrentHashMap<>();

    private AkaishiMechanicalEffectHandler() {
    }

    // ==================== 暴击强化 ====================

    /** 暴击强化加成：按跨器官有效等级单次取值，与器官暴击属性求和后统一受配置上限裁剪。 */
    public static float critBonus(IPlayerBodyState state) {
        ResourceLocation critId = MechanicalSpecialEffect.CRITICAL_BOOST.id();
        Integer level = MechanicalAggregation.effectLevels(MechanicalAggregation.organs(state)).get(critId);
        return level == null ? 0F : MechanicalDnaEffects.critBonus(level);
    }

    // ==================== tick 类 ====================

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (player.isDeadOrDying()) {
            return;
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return;
        }
        List<ItemStack> organs = MechanicalAggregation.organs(state);
        handleEquipDiff(player, state, organs);

        dispatchTraitTick(player, MechanicalAggregation.traitLevels(organs));
        dispatchEffectTick(player, MechanicalAggregation.effectLevels(organs));
    }

    /** 特性 tick 分发：每种特性只回调一次，参数 = 跨器官有效等级。 */
    public static void dispatchTraitTick(Player player, Map<String, Integer> levels) {
        levels.forEach((id, level) -> {
            IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(id);
            if (handler != null) {
                handler.onPlayerTick(player, level);
            }
        });
    }

    /** DNA 效果 tick 分发：每个效果只回调一次（第三方钩子 + 内置曲线各一次）。 */
    private static void dispatchEffectTick(Player player, Map<ResourceLocation, Integer> levels) {
        levels.forEach((id, level) -> {
            IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(id);
            if (handler != null) {
                handler.onPlayerTick(player, level);
            }
            MechanicalDnaEffects.applyTick(player, id, level);
        });
    }

    // ==================== 受击 / 命中 / 击杀类 ====================

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        // 受害方：材料特性 / DNA 受击修正 + 第三方入伤修正（每条目一次）
        if (event.getEntity() instanceof Player victim) {
            IPlayerBodyState state = PlayerBodyHelper.of(victim);
            if (state != null) {
                List<ItemStack> organs = MechanicalAggregation.organs(state);
                float amount = event.getAmount();
                for (Map.Entry<String, Integer> entry : MechanicalAggregation.traitLevels(organs).entrySet()) {
                    IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(entry.getKey());
                    if (handler != null) {
                        amount = handler.modifyIncomingDamage(victim, event.getSource(), amount, entry.getValue());
                    }
                }
                for (Map.Entry<ResourceLocation, Integer> entry
                        : MechanicalAggregation.effectLevels(organs).entrySet()) {
                    amount = MechanicalDnaEffects.modifyIncoming(victim, event.getSource(), amount,
                            entry.getKey(), entry.getValue());
                    IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(entry.getKey());
                    if (handler != null) {
                        amount = handler.modifyIncomingDamage(victim, event.getSource(), amount, entry.getValue());
                    }
                }
                event.setAmount(amount);
            }
        }
        // 攻击方：材料特性命中钩子 / DNA 火焰凋零 + 第三方命中钩子（每条目一次）
        if (event.getSource().getEntity() instanceof Player attacker) {
            IPlayerBodyState state = PlayerBodyHelper.of(attacker);
            if (state == null) {
                return;
            }
            LivingEntity target = event.getEntity();
            List<ItemStack> organs = MechanicalAggregation.organs(state);
            float amount = event.getAmount();
            for (Map.Entry<String, Integer> entry : MechanicalAggregation.traitLevels(organs).entrySet()) {
                IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(entry.getKey());
                if (handler != null) {
                    amount = handler.modifyOutgoingDamage(attacker, target, amount, entry.getValue());
                    handler.onAttack(attacker, target, entry.getValue());
                }
            }
            for (Map.Entry<ResourceLocation, Integer> entry
                    : MechanicalAggregation.effectLevels(organs).entrySet()) {
                MechanicalDnaEffects.applyAttack(attacker, target, entry.getKey(), entry.getValue());
                amount = MechanicalDnaEffects.modifyOutgoing(attacker, target, amount,
                        entry.getKey(), entry.getValue());
                IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(entry.getKey());
                if (handler != null) {
                    amount = handler.modifyOutgoingDamage(attacker, target, amount, entry.getValue());
                    handler.onAttack(attacker, target, entry.getValue());
                }
            }
            event.setAmount(amount);
        }
    }

    /** 击退抗性：按 DNA 效果有效等级保留击退强度（与原版击退抗性属性叠加结算）+ 第三方击退修正。 */
    @SubscribeEvent
    public void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return;
        }
        float strength = event.getStrength();
        for (Map.Entry<ResourceLocation, Integer> entry
                : MechanicalAggregation.effectLevels(MechanicalAggregation.organs(state)).entrySet()) {
            strength = MechanicalDnaEffects.modifyKnockback(player, strength, entry.getKey(), entry.getValue());
            IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(entry.getKey());
            if (handler != null) {
                strength = handler.modifyKnockback(player, strength, entry.getValue());
            }
        }
        event.setStrength(strength);
    }

    /** 击杀钩子：每种材料特性按有效等级回调 onKill 一次；DNA 效果同样按有效等级回调一次。 */
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return;
        }
        LivingEntity victim = event.getEntity();
        List<ItemStack> organs = MechanicalAggregation.organs(state);
        for (Map.Entry<String, Integer> entry : MechanicalAggregation.traitLevels(organs).entrySet()) {
            IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(entry.getKey());
            if (handler != null) {
                handler.onKill(player, victim, entry.getValue());
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : MechanicalAggregation.effectLevels(organs).entrySet()) {
            IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(entry.getKey());
            if (handler != null) {
                handler.onKill(player, victim, entry.getValue());
            }
        }
    }

    // ==================== 装备差分 ====================

    /**
     * 与上次快照差分：按「跨器官有效等级集合」的差集触发 onEquip / onUnequip
     * （等级变化时先 onUnequip 旧等级、再 onEquip 新等级；不再是逐器官触发）。
     * 材料特性与 DNA 效果各差一遍（DNA 侧供挂载临时属性修饰符的效果在失效时清理）。
     */
    private static void handleEquipDiff(Player player, IPlayerBodyState state, List<ItemStack> organs) {
        EnumMap<BodySlot, ItemStack> previous = LAST_ORGANS.computeIfAbsent(
                player.getUUID(), k -> new EnumMap<>(BodySlot.class));
        boolean changed = false;
        for (BodySlot slot : BodySlot.values()) {
            ItemStack now = state.getOrgan(slot);
            ItemStack old = previous.get(slot);
            boolean same = old == null ? now.isEmpty() : ItemStack.matches(now, old);
            if (!same) {
                changed = true;
                break;
            }
        }
        if (!changed) {
            return;
        }
        List<ItemStack> previousOrgans = previousOrgans(previous);
        Map<String, Integer> before = MechanicalAggregation.traitLevels(previousOrgans);
        Map<String, Integer> after = MechanicalAggregation.traitLevels(organs);
        Set<String> ids = new LinkedHashSet<>(before.keySet());
        ids.addAll(after.keySet());
        for (String id : ids) {
            Integer old = before.get(id);
            Integer now = after.get(id);
            if (Objects.equals(old, now)) {
                continue;
            }
            IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(id);
            if (handler == null) {
                continue;
            }
            if (old != null) {
                handler.onUnequip(player, old);
            }
            if (now != null) {
                handler.onEquip(player, now);
            }
        }
        Map<ResourceLocation, Integer> beforeEffects = MechanicalAggregation.effectLevels(previousOrgans);
        Map<ResourceLocation, Integer> afterEffects = MechanicalAggregation.effectLevels(organs);
        Set<ResourceLocation> effectIds = new LinkedHashSet<>(beforeEffects.keySet());
        effectIds.addAll(afterEffects.keySet());
        for (ResourceLocation id : effectIds) {
            Integer old = beforeEffects.get(id);
            Integer now = afterEffects.get(id);
            if (Objects.equals(old, now)) {
                continue;
            }
            IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(id);
            if (handler == null) {
                continue;
            }
            if (old != null) {
                handler.onUnequip(player, old);
            }
            if (now != null) {
                handler.onEquip(player, now);
            }
        }
        for (BodySlot slot : BodySlot.values()) {
            ItemStack now = state.getOrgan(slot);
            if (now.isEmpty()) {
                previous.remove(slot);
            } else {
                previous.put(slot, now.copy());
            }
        }
    }

    /** 快照中的机械器官（过滤非机械器官，供"上次有效等级"复用同一汇总入口）。 */
    private static List<ItemStack> previousOrgans(EnumMap<BodySlot, ItemStack> previous) {
        List<ItemStack> organs = new java.util.ArrayList<>(previous.size());
        for (ItemStack stack : previous.values()) {
            if (stack.getItem() instanceof MechanicalOrganItem) {
                organs.add(stack);
            }
        }
        return organs;
    }

    /** 玩家登出：丢弃器官快照，避免长期钉住 UUID。 */
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_ORGANS.remove(event.getEntity().getUUID());
    }
}
