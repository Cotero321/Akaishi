package com.example.akaishi.forge.life;

import com.example.akaishi.life.mechanical.MechanicalSpecialEffect;
import com.example.akaishi.life.mechanical.trait.TraitTier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * 内置 DNA 特殊效果的「四级曲线」实现（T5，企划 §5）。
 * <p>
 * 所有强度按<b>跨器官汇总后的有效等级</b>（1~4）取值，由 {@code AkaishiMechanicalEffectHandler} 每个效果只调用一次；
 * 有效等级 = {@code clamp(max(各器官等级) + 携带该效果的器官数 - 1, 1, 4)}。
 * <p>
 * 基调：现状 ≈ Lv2~3，Lv4 略强于现状（企划 §5）。全部数值均为<b>待调手感值</b>。
 */
public final class MechanicalDnaEffects {

    private MechanicalDnaEffects() {
    }

    private static final int MAX_LEVEL = 4;

    // ---- 四级曲线（待调手感值）----
    /** 暴击强化：每级 +3% 暴击率（Lv1~4 = 3/6/9/12%） */
    private static final float CRIT_PER_LEVEL = 0.03F;
    /** 爆炸抗性：伤害保留 80/70/60/50% */
    private static final TraitTier EXPLOSION_RETAIN = TraitTier.of(0.80F, 0.70F, 0.60F, 0.50F);
    /** 击退抗性：击退强度保留 60/45/30/15% */
    private static final TraitTier KNOCKBACK_RETAIN = TraitTier.of(0.60F, 0.45F, 0.30F, 0.15F);
    /** 火焰攻击：点燃 2/3/4/5 秒 */
    private static final TraitTier FIRE_SECONDS = TraitTier.of(2, 3, 4, 5);
    /** 火焰攻击 Lv4：火伤额外 +20% */
    private static final float FIRE_L4_BONUS = 0.20F;
    /** 低血自愈：回复间隔 tick 40/40/30/30（2s/2s/1.5s/1.5s） */
    private static final TraitTier REGEN_INTERVAL = TraitTier.of(40, 40, 30, 30);
    /** 低血自愈：触发阈值占比 30/35/35/35% */
    private static final TraitTier REGEN_THRESHOLD = TraitTier.of(0.30F, 0.35F, 0.35F, 0.35F);
    /** 低血自愈：单次回复量 1/1/1/2 */
    private static final TraitTier REGEN_AMOUNT = TraitTier.of(1, 1, 1, 2);
    /** 金装共鸣：需求金甲件数 2/2/3/4 */
    private static final TraitTier GOLD_REQUIRED = TraitTier.of(2, 2, 3, 4);
    /** 凋零攻击：等级增幅 / 时长 tick（Lv1 I2s / Lv2 I3s / Lv3 II3s / Lv4 II4s） */
    private static final int[] WITHER_AMPLIFIER = {0, 0, 1, 1};
    private static final int[] WITHER_TICKS = {40, 60, 60, 80};
    // ---- T7 Stage 2 新增曲线（待调手感值）----
    /** 摔落减免：伤害保留 70/45/20/0%（Lv4 完全免摔落） */
    private static final TraitTier FALL_RETAIN = TraitTier.of(0.70F, 0.45F, 0.20F, 0.0F);
    /** 自动拾取：半径 4/5/6/7 格，每 10 tick 一次（镜像 OrganPassive.AUTO_PICKUP） */
    private static final int AUTO_PICKUP_INTERVAL = 10;
    private static final TraitTier AUTO_PICKUP_RANGE = TraitTier.of(4, 5, 6, 7);
    /** 击退目标：推力 0.25/0.35/0.45/0.55（镜像 OrganPassive.KNOCKBACK_ON_HIT） */
    private static final TraitTier KNOCKBACK_POWER = TraitTier.of(0.25F, 0.35F, 0.45F, 0.55F);
    /** 负面时长减免：负面药水时长上限 tick 200/160/120/80（10s/8s/6s/4s） */
    private static final TraitTier DEBUFF_CAP_TICKS = TraitTier.of(200, 160, 120, 80);
    /** 纳入时长减免的负面效果集合 */
    private static final MobEffect[] NEGATIVE_EFFECTS = {
            MobEffects.POISON, MobEffects.WITHER, MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.HUNGER, MobEffects.DIG_SLOWDOWN, MobEffects.CONFUSION, MobEffects.BLINDNESS,
            MobEffects.LEVITATION};

    private static int clamp(int level) {
        return Math.max(1, Math.min(MAX_LEVEL, level));
    }

    // ==================== 连续效果（tick，取全身最强一份） ====================

    public static void applyTick(Player player, ResourceLocation effectId, int level) {
        if (MechanicalSpecialEffect.POISON_RESIST.id().equals(effectId)) {
            if (player.hasEffect(MobEffects.POISON)) {
                player.removeEffect(MobEffects.POISON);
            }
            if (level >= 3 && player.hasEffect(MobEffects.HUNGER)) {
                player.removeEffect(MobEffects.HUNGER);
            }
            if (level >= 4 && player.hasEffect(MobEffects.CONFUSION)) {
                player.removeEffect(MobEffects.CONFUSION);
            }
            return;
        }
        if (MechanicalSpecialEffect.LOW_HEALTH_REGENERATION.id().equals(effectId)) {
            int interval = Math.max(1, REGEN_INTERVAL.atInt(level));
            if (player.tickCount % interval == 0
                    && player.getHealth() < player.getMaxHealth() * REGEN_THRESHOLD.at(level)) {
                player.heal(REGEN_AMOUNT.at(level));
            }
            return;
        }
        if (MechanicalSpecialEffect.UNDERWATER_SPEED.id().equals(effectId)) {
            if (player.isInWater()) {
                applyPotion(player, MobEffects.DOLPHINS_GRACE, level >= 3 ? 1 : 0);
                if (level >= 4) {
                    applyPotion(player, MobEffects.WATER_BREATHING, 0);
                }
            }
            return;
        }
        if (MechanicalSpecialEffect.GOLD_ARMOR_BONUS.id().equals(effectId)) {
            int gold = countGoldenArmor(player);
            if (gold >= GOLD_REQUIRED.atInt(level)) {
                applyPotion(player, MobEffects.DAMAGE_RESISTANCE, level >= 4 ? 1 : 0);
            }
            return;
        }
        if (MechanicalSpecialEffect.TELEPORT_COOLDOWN.id().equals(effectId)) {
            applyTeleportCooldown(player, level);
            return;
        }
        // ---- T7 Stage 2 新增：夜视 / 水下呼吸 / 自动拾取 / 负面时长减免 ----
        if (MechanicalSpecialEffect.NIGHT_VISION.id().equals(effectId)) {
            applyPotion(player, MobEffects.NIGHT_VISION, 0);
            return;
        }
        if (MechanicalSpecialEffect.WATER_BREATHING.id().equals(effectId)) {
            applyPotion(player, MobEffects.WATER_BREATHING, 0);
            return;
        }
        if (MechanicalSpecialEffect.AUTO_PICKUP.id().equals(effectId)) {
            if (player.tickCount % AUTO_PICKUP_INTERVAL == 0) {
                pickupNearbyItems(player, AUTO_PICKUP_RANGE.atInt(level));
            }
            return;
        }
        if (MechanicalSpecialEffect.DEBUFF_RESIST.id().equals(effectId)) {
            applyDebuffCap(player, DEBUFF_CAP_TICKS.atInt(level));
        }
    }

    // ==================== 瞬时效果 ====================

    /** 受击结算前：爆炸抗性按等级保留伤害。 */
    public static float modifyIncoming(Player player, DamageSource source, float amount,
                                       ResourceLocation effectId, int level) {
        if (MechanicalSpecialEffect.EXPLOSION_RESIST.id().equals(effectId)
                && source.is(DamageTypeTags.IS_EXPLOSION)) {
            return amount * EXPLOSION_RETAIN.at(level);
        }
        // T7 Stage 2：摔落免疫（Lv4 完全免摔落）
        if (MechanicalSpecialEffect.FALL_IMMUNE.id().equals(effectId)
                && source.is(DamageTypeTags.IS_FALL)) {
            return amount * FALL_RETAIN.at(level);
        }
        return amount;
    }

    /** 命中结算前：火焰攻击 Lv4 额外 +20% 火伤。 */
    public static float modifyOutgoing(Player attacker, LivingEntity target, float amount,
                                       ResourceLocation effectId, int level) {
        if (MechanicalSpecialEffect.FIRE_ATTACK.id().equals(effectId) && level >= 4) {
            return amount * (1F + FIRE_L4_BONUS);
        }
        return amount;
    }

    /** 命中后：按等级施加火焰 / 凋零。 */
    public static void applyAttack(Player attacker, LivingEntity target,
                                   ResourceLocation effectId, int level) {
        if (MechanicalSpecialEffect.FIRE_ATTACK.id().equals(effectId) && !target.fireImmune()) {
            target.setSecondsOnFire(FIRE_SECONDS.atInt(level));
            return;
        }
        if (MechanicalSpecialEffect.WITHER_ATTACK.id().equals(effectId)) {
            int i = clamp(level) - 1;
            applyToTarget(target, MobEffects.WITHER, WITHER_AMPLIFIER[i], WITHER_TICKS[i]);
            return;
        }
        // T7 Stage 2：命中把目标沿击退方向顶开（与武器击退叠加）
        if (MechanicalSpecialEffect.KNOCKBACK_ON_HIT.id().equals(effectId)) {
            target.knockback(KNOCKBACK_POWER.at(level),
                    target.getX() - attacker.getX(), target.getZ() - attacker.getZ());
        }
    }

    /** 击退结算前：击退抗性按等级保留击退强度。 */
    public static float modifyKnockback(Player player, float strength,
                                        ResourceLocation effectId, int level) {
        if (MechanicalSpecialEffect.KNOCKBACK_RESIST.id().equals(effectId)) {
            return strength * KNOCKBACK_RETAIN.at(level);
        }
        return strength;
    }

    /** 暴击强化：该器官等级对应的暴击率加成。 */
    public static float critBonus(int level) {
        return CRIT_PER_LEVEL * clamp(level);
    }

    // ==================== 工具 ====================

    /** 瞬移冷却：Lv1 减半、Lv2+ 清零、Lv3+ 首次使用时返还 1 珍珠（以冷却百分比自节流，免额外状态）。 */
    private static void applyTeleportCooldown(Player player, int level) {
        if (!player.getCooldowns().isOnCooldown(Items.ENDER_PEARL)) {
            return;
        }
        float percent = player.getCooldowns().getCooldownPercent(Items.ENDER_PEARL, 0F);
        if (level >= 2) {
            player.getCooldowns().removeCooldown(Items.ENDER_PEARL);
            if (level >= 3 && percent > 0.9F && !player.isCreative()) {
                player.getInventory().add(new ItemStack(Items.ENDER_PEARL));
            }
        } else if (percent > 0.5F) {
            // Lv1：冷却 -50%（减半后百分比降到 0.5，下一 tick 自然不再触发）
            player.getCooldowns().removeCooldown(Items.ENDER_PEARL);
            player.getCooldowns().addCooldown(Items.ENDER_PEARL, 10);
        }
    }

    /** 穿戴金甲件数（头盔/胸甲/护腿/靴子）。 */
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

    /** 缺失时补充指定药水效果（5 秒持续；每 tick 巡检近似常驻，摘除义体后自然消退）。 */
    private static void applyPotion(Player player, MobEffect effect, int amplifier) {
        if (!player.hasEffect(effect)) {
            player.addEffect(new MobEffectInstance(effect, 100, amplifier, false, false));
        }
    }

    /** 命中附加效果：目标当前无同效果时补充（避免覆盖更强来源的等级/时长）。 */
    private static void applyToTarget(LivingEntity target, MobEffect effect, int amplifier, int durationTicks) {
        if (!target.hasEffect(effect)) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, false));
        }
    }

    /** 自动拾取：半径内无拾取延迟的掉落物直接入包（镜像 OrganPassive.AUTO_PICKUP 的拾取判定）。 */
    private static void pickupNearbyItems(Player player, int range) {
        AABB box = player.getBoundingBox().inflate(range);
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            if (item.hasPickUpDelay()) {
                continue;
            }
            if (player.getInventory().add(item.getItem())) {
                item.discard();
            }
        }
    }

    /** 负面时长减免：把在身的负面药水时长压到上限（保留增幅/粒子可见性，不叠加、不改等级）。 */
    private static void applyDebuffCap(Player player, int capTicks) {
        for (MobEffect negative : NEGATIVE_EFFECTS) {
            MobEffectInstance inst = player.getEffect(negative);
            if (inst != null && inst.getDuration() > capTicks) {
                int amplifier = inst.getAmplifier();
                boolean ambient = inst.isAmbient();
                boolean visible = inst.isVisible();
                player.removeEffect(negative);
                player.addEffect(new MobEffectInstance(negative, capTicks, amplifier, ambient, visible));
            }
        }
    }
}
