package com.example.akaishi.forge.life;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffectHandler;
import com.example.akaishi.api.mechanical.MechanicalEffectHandlerRegistry;
import com.example.akaishi.api.mechanical.trait.IMechanicalTraitHandler;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitHandlerRegistry;
import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalLevels;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机械义体效果处理器（Forge 服务端，T4 逐器官分发版）。
 * <p>
 * <b>分发模型（M2：各器官各自生效）</b>：遍历玩家每个槽位上的机械器官，对每个器官分别算出
 * 「材料特性 → 该器官内的等级」与「DNA 来源 → 该器官内的等级」，并<b>逐个器官</b>按各自 level 回调，
 * 不存在跨器官等级合并（企划 §2.1b）。
 * <ul>
 *   <li>材料特性：{@link MechanicalTraitHandlerRegistry} 按特性 ID 取处理器，逐个器官调用 7 个钩子；
 *       未注册处理器的特性只不生效、不报错；</li>
 *   <li>DNA 效果：{@link MechanicalEffectHandlerRegistry} 按效果 ID 取处理器，参数语义已由"来源数"
 *       升级为"该器官内的 DNA 等级(1~4)"；内置 10 个效果的曲线见 {@link MechanicalDnaEffects}；</li>
 *   <li>连续型 DNA 效果（免疫 / 自愈 / 水下 / 金装 / 瞬移）取全身最强一份调用一次，
 *       瞬时型（伤害 / 火焰 / 凋零 / 击退）逐器官叠加；</li>
 *   <li>装备变更（onEquip / onUnequip）由每 tick 与上次器官快照差分检测。</li>
 * </ul>
 * 暴击强化由 {@link #critBonus(IPlayerBodyState)} 供战斗处理器求和，避免二次暴击判定。
 */
public final class AkaishiMechanicalEffectHandler {

    public static final AkaishiMechanicalEffectHandler INSTANCE = new AkaishiMechanicalEffectHandler();

    /** 每玩家「上次见到的器官」快照，用于 onEquip / onUnequip 差分（登出时清理）。 */
    private static final Map<UUID, EnumMap<BodySlot, ItemStack>> LAST_ORGANS = new ConcurrentHashMap<>();

    private AkaishiMechanicalEffectHandler() {
    }

    // ==================== 逐器官数据 ====================

    /** 收集玩家所有机械器官（按槽位顺序）。 */
    private static List<ItemStack> mechanicalOrgans(IPlayerBodyState state) {
        List<ItemStack> organs = new ArrayList<>(BodySlot.values().length);
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (organ.getItem() instanceof MechanicalOrganItem) {
                organs.add(organ);
            }
        }
        return organs;
    }

    /**
     * 某器官的「DNA 效果等级表」：把逐部件 DNA 来源的等级汇总到其授予的效果上
     * （同一效果由多个来源授予时取最高等级）。空 Map 表示该器官无有效效果。
     */
    private static Map<ResourceLocation, Integer> effectLevels(ItemStack organ) {
        Map<String, Integer> dnaLevels = MechanicalLevels.dnaLevels(organ);
        if (dnaLevels.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Integer> levels = new HashMap<>(dnaLevels.size());
        for (Map.Entry<String, Integer> entry : dnaLevels.entrySet()) {
            MechanicalDnaProfile profile = MechanicalDnaProfile.get(entry.getKey());
            if (profile == null || MechanicalSpecialEffect.isNone(profile.effect())) {
                continue;
            }
            ResourceLocation id = profile.effect().id();
            if (id != null) {
                levels.merge(id, entry.getValue(), Math::max);
            }
        }
        return levels;
    }

    /** 暴击强化加成：逐器官累加（M2），与器官暴击属性求和后统一受配置上限裁剪。 */
    public static float critBonus(IPlayerBodyState state) {
        ResourceLocation critId = MechanicalSpecialEffect.CRITICAL_BOOST.id();
        float bonus = 0F;
        for (ItemStack organ : mechanicalOrgans(state)) {
            Integer level = effectLevels(organ).get(critId);
            if (level != null) {
                bonus += MechanicalDnaEffects.critBonus(level);
            }
        }
        return bonus;
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
        // 装备差分 → onEquip / onUnequip（属性型特性亦借此清理瞬时修饰符）
        handleEquipDiff(player, state);

        // 逐器官分发：材料特性（7 钩子之 onPlayerTick）与 DNA 效果（第三方按器官等级回调）
        Map<ResourceLocation, Integer> tickMax = new HashMap<>();
        for (ItemStack organ : mechanicalOrgans(state)) {
            for (Map.Entry<String, Integer> trait : MechanicalLevels.traitLevels(organ).entrySet()) {
                IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(trait.getKey());
                if (handler != null) {
                    handler.onPlayerTick(player, trait.getValue());
                }
            }
            for (Map.Entry<ResourceLocation, Integer> effect : effectLevels(organ).entrySet()) {
                tickMax.merge(effect.getKey(), effect.getValue(), Math::max);
                IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(effect.getKey());
                if (handler != null) {
                    handler.onPlayerTick(player, effect.getValue());
                }
            }
        }
        // 内置连续效果：取全身最强一份
        tickMax.forEach((id, level) -> MechanicalDnaEffects.applyTick(player, id, level));
    }

    // ==================== 受击 / 命中 / 击杀类 ====================

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        // 受害方：材料特性 / DNA 受击修正 + 第三方入伤修正（逐器官）
        if (event.getEntity() instanceof Player victim) {
            IPlayerBodyState state = PlayerBodyHelper.of(victim);
            if (state != null) {
                float amount = event.getAmount();
                for (ItemStack organ : mechanicalOrgans(state)) {
                    for (Map.Entry<String, Integer> trait : MechanicalLevels.traitLevels(organ).entrySet()) {
                        IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(trait.getKey());
                        if (handler != null) {
                            amount = handler.modifyIncomingDamage(victim, event.getSource(), amount, trait.getValue());
                        }
                    }
                    for (Map.Entry<ResourceLocation, Integer> effect : effectLevels(organ).entrySet()) {
                        amount = MechanicalDnaEffects.modifyIncoming(victim, event.getSource(), amount,
                                effect.getKey(), effect.getValue());
                        IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(effect.getKey());
                        if (handler != null) {
                            amount = handler.modifyIncomingDamage(victim, event.getSource(), amount, effect.getValue());
                        }
                    }
                }
                event.setAmount(amount);
            }
        }
        // 攻击方：材料特性命中钩子 / DNA 火焰凋零 + 第三方命中钩子（逐器官）
        if (event.getSource().getEntity() instanceof Player attacker) {
            IPlayerBodyState state = PlayerBodyHelper.of(attacker);
            if (state == null) {
                return;
            }
            LivingEntity target = event.getEntity();
            float amount = event.getAmount();
            for (ItemStack organ : mechanicalOrgans(state)) {
                for (Map.Entry<String, Integer> trait : MechanicalLevels.traitLevels(organ).entrySet()) {
                    IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(trait.getKey());
                    if (handler != null) {
                        amount = handler.modifyOutgoingDamage(attacker, target, amount, trait.getValue());
                        handler.onAttack(attacker, target, trait.getValue());
                    }
                }
                for (Map.Entry<ResourceLocation, Integer> effect : effectLevels(organ).entrySet()) {
                    MechanicalDnaEffects.applyAttack(attacker, target, effect.getKey(), effect.getValue());
                    amount = MechanicalDnaEffects.modifyOutgoing(attacker, target, amount,
                            effect.getKey(), effect.getValue());
                    IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(effect.getKey());
                    if (handler != null) {
                        handler.onAttack(attacker, target, effect.getValue());
                    }
                }
            }
            event.setAmount(amount);
        }
    }

    /** 击退抗性：逐器官按 DNA 等级保留击退强度（与原版击退抗性属性叠加结算）+ 第三方击退修正。 */
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
        for (ItemStack organ : mechanicalOrgans(state)) {
            for (Map.Entry<ResourceLocation, Integer> effect : effectLevels(organ).entrySet()) {
                strength = MechanicalDnaEffects.modifyKnockback(player, strength, effect.getKey(), effect.getValue());
                IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(effect.getKey());
                if (handler != null) {
                    strength = handler.modifyKnockback(player, strength, effect.getValue());
                }
            }
        }
        event.setStrength(strength);
    }

    /** 击杀钩子：逐器官回调材料特性的 onKill（DNA 效果无此钩子）。 */
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
        for (ItemStack organ : mechanicalOrgans(state)) {
            for (Map.Entry<String, Integer> trait : MechanicalLevels.traitLevels(organ).entrySet()) {
                IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(trait.getKey());
                if (handler != null) {
                    handler.onKill(player, victim, trait.getValue());
                }
            }
        }
    }

    // ==================== 装备差分 ====================

    /** 与上次快照差分：新增 / 变更的器官触发 onEquip，移除 / 变更前的器官触发 onUnequip。 */
    private static void handleEquipDiff(Player player, IPlayerBodyState state) {
        EnumMap<BodySlot, ItemStack> previous = LAST_ORGANS.computeIfAbsent(
                player.getUUID(), k -> new EnumMap<>(BodySlot.class));
        for (BodySlot slot : BodySlot.values()) {
            ItemStack now = state.getOrgan(slot);
            ItemStack old = previous.get(slot);
            if (old != null && ItemStack.matches(now, old)) {
                continue;
            }
            if (old != null && old.getItem() instanceof MechanicalOrganItem) {
                dispatchEquipChange(player, old, false);
            }
            if (now.getItem() instanceof MechanicalOrganItem) {
                dispatchEquipChange(player, now, true);
            }
            if (now.isEmpty()) {
                previous.remove(slot);
            } else {
                previous.put(slot, now.copy());
            }
        }
    }

    private static void dispatchEquipChange(Player player, ItemStack organ, boolean equip) {
        for (Map.Entry<String, Integer> trait : MechanicalLevels.traitLevels(organ).entrySet()) {
            IMechanicalTraitHandler handler = MechanicalTraitHandlerRegistry.get(trait.getKey());
            if (handler == null) {
                continue;
            }
            if (equip) {
                handler.onEquip(player, trait.getValue());
            } else {
                handler.onUnequip(player, trait.getValue());
            }
        }
    }

    /** 玩家登出：丢弃器官快照，避免长期钉住 UUID。 */
    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_ORGANS.remove(event.getEntity().getUUID());
    }
}
