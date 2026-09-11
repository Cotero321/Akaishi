package com.example.akaishi.forge.life;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffect;
import com.example.akaishi.api.mechanical.IMechanicalDnaEffectHandler;
import com.example.akaishi.api.mechanical.MechanicalEffectHandlerRegistry;
import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.mechanical.MechanicalSpecialEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 机械义体特殊效果处理器（Forge 服务端）。
 * 机械成品的效果由 DNA 授予并持久化在 NBT，本类负责平台侧实际消费，避免"看得到用不到"：
 * - tick 类：毒素免疫 / 低血自愈 / 水下加速 / 金装共鸣 / 瞬移冷却减少
 * - 受击类：爆炸抗性 / 击退抗性
 * - 命中类：火焰攻击 / 凋零攻击
 * - 第三方扩展：每个效果可通过 {@link MechanicalEffectHandlerRegistry} 挂接执行钩子，
 *   本类统一按效果 ID 分发，无需为附属模组硬编码分支。
 * - 暴击强化由 {@link #critBonus(IPlayerBodyState)} 供战斗处理器求和，避免二次暴击判定
 */
public final class AkaishiMechanicalEffectHandler {

    public static final AkaishiMechanicalEffectHandler INSTANCE = new AkaishiMechanicalEffectHandler();

    /** 暴击强化：每条来源 +5% 暴击率 */
    private static final float CRIT_BONUS_PER_SOURCE = 0.05F;
    /** 爆炸抗性：爆炸伤害保留 60%（减免 40%） */
    private static final float EXPLOSION_RESIST_FACTOR = 0.6F;
    /** 击退抗性：击退强度保留 40% */
    private static final float KNOCKBACK_RESIST_FACTOR = 0.4F;
    /** 低血自愈触发线（最大生命占比） */
    private static final float LOW_HEALTH_THRESHOLD = 0.3F;

    // 内置效果稳定 ID（引用兼容门面，保证与历史 NBT / 注册表一致）
    private static final ResourceLocation ID_CRITICAL_BOOST = MechanicalSpecialEffect.CRITICAL_BOOST.id();
    private static final ResourceLocation ID_POISON_RESIST = MechanicalSpecialEffect.POISON_RESIST.id();
    private static final ResourceLocation ID_LOW_HEALTH_REGEN = MechanicalSpecialEffect.LOW_HEALTH_REGENERATION.id();
    private static final ResourceLocation ID_UNDERWATER_SPEED = MechanicalSpecialEffect.UNDERWATER_SPEED.id();
    private static final ResourceLocation ID_GOLD_ARMOR_BONUS = MechanicalSpecialEffect.GOLD_ARMOR_BONUS.id();
    private static final ResourceLocation ID_TELEPORT_COOLDOWN = MechanicalSpecialEffect.TELEPORT_COOLDOWN.id();
    private static final ResourceLocation ID_EXPLOSION_RESIST = MechanicalSpecialEffect.EXPLOSION_RESIST.id();
    private static final ResourceLocation ID_FIRE_ATTACK = MechanicalSpecialEffect.FIRE_ATTACK.id();
    private static final ResourceLocation ID_WITHER_ATTACK = MechanicalSpecialEffect.WITHER_ATTACK.id();
    private static final ResourceLocation ID_KNOCKBACK_RESIST = MechanicalSpecialEffect.KNOCKBACK_RESIST.id();

    private AkaishiMechanicalEffectHandler() {
    }

    // ==================== 效果收集 ====================

    /**
     * 收集玩家当前全部机械义体特殊效果 → 携带来源数（同一槽位内去重，跨槽位累加）。
     * 采用普通 Map（效果已去枚举），内置与附属效果一视同仁。
     */
    public static Map<ResourceLocation, Integer> collectSources(IPlayerBodyState state) {
        Map<ResourceLocation, Integer> sources = new HashMap<>();
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (!(organ.getItem() instanceof MechanicalOrganItem)) {
                continue;
            }
            Set<ResourceLocation> inSlot = new HashSet<>();
            for (IMechanicalDnaEffect effect : MechanicalOrganItem.getEffects(organ)) {
                ResourceLocation id = effect.id();
                if (id != null && inSlot.add(id)) {
                    sources.merge(id, 1, Integer::sum);
                }
            }
        }
        return sources;
    }

    /** 暴击强化加成（与器官暴击属性求和后统一受配置上限裁剪） */
    public static float critBonus(IPlayerBodyState state) {
        return CRIT_BONUS_PER_SOURCE * collectSources(state).getOrDefault(ID_CRITICAL_BOOST, 0);
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
        Map<ResourceLocation, Integer> sources = collectSources(state);
        if (sources.isEmpty()) {
            return;
        }
        // 毒素免疫：清除中毒状态，中毒伤害随之归零
        if (sources.containsKey(ID_POISON_RESIST) && player.hasEffect(MobEffects.POISON)) {
            player.removeEffect(MobEffects.POISON);
        }
        // 低血自愈：生命低于 30% 时每 2 秒回复 1 点（不越线，与再生类被动独立）
        if (sources.containsKey(ID_LOW_HEALTH_REGEN)
                && player.tickCount % 40 == 0
                && player.getHealth() < player.getMaxHealth() * LOW_HEALTH_THRESHOLD) {
            player.heal(1.0F);
        }
        // 水下加速：浸水时持续海豚的恩惠
        if (sources.containsKey(ID_UNDERWATER_SPEED) && player.isInWater()) {
            applyPotion(player, MobEffects.DOLPHINS_GRACE, 0);
        }
        // 金装共鸣：穿戴金甲 2 件起抗性提升 I，集齐 4 件提升至 II
        if (sources.containsKey(ID_GOLD_ARMOR_BONUS)) {
            int gold = countGoldenArmor(player);
            if (gold >= 2) {
                applyPotion(player, MobEffects.DAMAGE_RESISTANCE, gold >= 4 ? 1 : 0);
            }
        }
        // 瞬移冷却减少：末影珍珠冷却清零（原版 1 秒等待被义体消除）
        if (sources.containsKey(ID_TELEPORT_COOLDOWN)
                && player.getCooldowns().isOnCooldown(Items.ENDER_PEARL)) {
            player.getCooldowns().removeCooldown(Items.ENDER_PEARL);
        }
        // 第三方扩展：按效果 ID 分发 tick 钩子
        if (!MechanicalEffectHandlerRegistry.isEmpty()) {
            for (Map.Entry<ResourceLocation, Integer> entry : sources.entrySet()) {
                IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(entry.getKey());
                if (handler != null) {
                    handler.onPlayerTick(player, entry.getValue());
                }
            }
        }
    }

    // ==================== 受击 / 命中类 ====================

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        // 受害方：爆炸抗性（与器官 BLAST_RESIST 独立结算）+ 第三方入伤修正
        if (event.getEntity() instanceof Player victim) {
            IPlayerBodyState state = PlayerBodyHelper.of(victim);
            if (state != null) {
                Map<ResourceLocation, Integer> sources = collectSources(state);
                float amount = event.getAmount();
                if (event.getSource().is(DamageTypeTags.IS_EXPLOSION) && sources.containsKey(ID_EXPLOSION_RESIST)) {
                    amount *= EXPLOSION_RESIST_FACTOR;
                }
                if (!MechanicalEffectHandlerRegistry.isEmpty()) {
                    for (Map.Entry<ResourceLocation, Integer> entry : sources.entrySet()) {
                        IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(entry.getKey());
                        if (handler != null) {
                            amount = handler.modifyIncomingDamage(victim, event.getSource(), amount, entry.getValue());
                        }
                    }
                }
                event.setAmount(amount);
            }
        }
        // 攻击方：命中附加火焰 / 凋零 + 第三方命中钩子
        if (event.getSource().getEntity() instanceof Player attacker) {
            IPlayerBodyState state = PlayerBodyHelper.of(attacker);
            if (state == null) {
                return;
            }
            Map<ResourceLocation, Integer> sources = collectSources(state);
            LivingEntity target = event.getEntity();
            if (sources.containsKey(ID_FIRE_ATTACK) && !target.fireImmune()) {
                target.setSecondsOnFire(4);
            }
            if (sources.containsKey(ID_WITHER_ATTACK)) {
                applyToTarget(target, MobEffects.WITHER, 0, 100); // 5 秒
            }
            if (!MechanicalEffectHandlerRegistry.isEmpty()) {
                for (Map.Entry<ResourceLocation, Integer> entry : sources.entrySet()) {
                    IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(entry.getKey());
                    if (handler != null) {
                        handler.onAttack(attacker, target, entry.getValue());
                    }
                }
            }
        }
    }

    /** 击退抗性：受击击退强度按比例保留（与原版击退抗性属性叠加结算）+ 第三方击退修正 */
    @SubscribeEvent
    public void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return;
        }
        Map<ResourceLocation, Integer> sources = collectSources(state);
        float strength = event.getStrength();
        if (sources.containsKey(ID_KNOCKBACK_RESIST)) {
            strength *= KNOCKBACK_RESIST_FACTOR;
        }
        if (!MechanicalEffectHandlerRegistry.isEmpty()) {
            for (Map.Entry<ResourceLocation, Integer> entry : sources.entrySet()) {
                IMechanicalDnaEffectHandler handler = MechanicalEffectHandlerRegistry.get(entry.getKey());
                if (handler != null) {
                    strength = handler.modifyKnockback(player, strength, entry.getValue());
                }
            }
        }
        event.setStrength(strength);
    }

    // ==================== 工具 ====================

    /** 穿戴金甲件数（头盔/胸甲/护腿/靴子） */
    private static int countGoldenArmor(Player player) {
        int n = 0;
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.is(Items.GOLDEN_HELMET) || armor.is(Items.GOLDEN_CHESTPLATE)
                    || armor.is(Items.GOLDEN_LEGGINGS) || armor.is(Items.GOLDEN_BOOTS)) {
                n++;
            }
        }
        return n;
    }

    /** 缺失时补充指定药水效果（5 秒持续；每 tick 巡检近似常驻，摘除义体后自然消退） */
    private static void applyPotion(Player player, MobEffect effect, int amplifier) {
        if (!player.hasEffect(effect)) {
            player.addEffect(new MobEffectInstance(effect, 100, amplifier, false, false));
        }
    }

    /** 命中附加效果：目标当前无同效果时补充（避免覆盖更强来源的等级/时长） */
    private static void applyToTarget(LivingEntity target, MobEffect effect, int amplifier, int durationTicks) {
        if (!target.hasEffect(effect)) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, false));
        }
    }
}
