package com.example.akaishi.forge.life;

import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.organ.OrganEffectResolver;
import com.example.akaishi.life.organ.OrganPassive;
import com.example.akaishi.life.organ.OrganSpecial;
import com.example.akaishi.life.organ.QualityTier;
import com.example.akaishi.life.sample.SampleGroup;
import com.example.akaishi.item.AkaishiLifeFusionSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.WeakHashMap;
import java.util.Map;

/**
 * 器官战斗效果处理器（Forge 服务端）：
 * - 攻击方（移植玩家）：命中附加效果（减速/中毒/凋零）、伤害增幅（跳跃/水下/弹射物）、满级特殊弹射物
 * - 受害方（移植玩家）：火焰弱点 / 摔落免疫 / 荆棘反弹 / 瞬移闪避 / 能量盾（仅超出防护的实际伤害耗能抵消）
 * 满级特殊弹射物（凋零骷髅头/龙息）仅在对应器官品质 IV 时触发，且不破坏方块、伤害较低。
 */
public final class AkaishiBodyCombatHandler {

    public static final AkaishiBodyCombatHandler INSTANCE = new AkaishiBodyCombatHandler();

    /** 特殊弹射物冷却：玩家 → 上次发射 tick（4 秒） */
    private static final Map<Player, Integer> PROJECTILE_COOLDOWN = new WeakHashMap<>();
    private static final int PROJECTILE_COOLDOWN_TICKS = 80;

    /** 音爆单次伤害（原版监守者音爆量级，无视护甲） */
    private static final float SONIC_BOOM_DAMAGE = 10.0F;
    /** 音爆最大射程（格）与命中判定半径（格） */
    private static final double SONIC_BOOM_RANGE = 24.0;
    private static final double SONIC_BOOM_RADIUS = 1.0;

    private AkaishiBodyCombatHandler() {
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        // ===== 攻击方效果（玩家移植器官）=====
        if (attacker instanceof Player ap) {
            IPlayerBodyState state = PlayerBodyHelper.of(ap);
            if (state != null) {
                applyAttackerEffects(event, ap, state);
            }
        }
        // ===== 受害方效果（玩家移植器官）=====
        if (event.getEntity() instanceof Player victim) {
            IPlayerBodyState state = PlayerBodyHelper.of(victim);
            if (state != null) {
                applyVictimEffects(event, victim, attacker);
            }
        }
    }

    /** 攻击方：命中附加 + 伤害增幅 + 满级特殊弹射物 */
    private static void applyAttackerEffects(LivingHurtEvent event, Player attacker, IPlayerBodyState state) {
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        if (OrganEffectResolver.hasPassive(state, OrganPassive.SLOW_ON_HIT)) {
            applyToTarget(target, MobEffects.MOVEMENT_SLOWDOWN, 1, 100);
        }
        if (OrganEffectResolver.hasPassive(state, OrganPassive.POISON_ON_HIT)) {
            applyToTarget(target, MobEffects.POISON, 1, 60);
        }
        if (OrganEffectResolver.hasPassive(state, OrganPassive.WITHER_ON_HIT)) {
            applyToTarget(target, MobEffects.WITHER, 1, 60);
        }
        if (OrganEffectResolver.hasPassive(state, OrganPassive.FATIGUE_ON_HIT)) {
            applyToTarget(target, MobEffects.DIG_SLOWDOWN, 1, 200);
        }
        // 伤害增幅
        if (OrganEffectResolver.hasPassive(state, OrganPassive.JUMP_ATTACK_BOOST) && !attacker.onGround()) {
            amount *= 1.6F; // 跳跃攻击 +60%
        }
        if (OrganEffectResolver.hasPassive(state, OrganPassive.WATER_ATTACK_BOOST) && attacker.isInWater()) {
            amount *= 1.5F; // 水下攻击 +50%
        }
        if (OrganEffectResolver.hasPassive(state, OrganPassive.PROJECTILE_BOOST) && event.getSource().isIndirect()) {
            amount *= 1.25F; // 弹射物伤害 +25%
        }
        // 生态套装·纯亡灵：夜间近战增伤（小共鸣 +10% / 大共鸣 +15%）
        OrganEffectResolver.Synergy synergy = OrganEffectResolver.synergyOf(state, attacker.level());
        if (synergy.group() == SampleGroup.UNDEAD && attacker.level().isNight()) {
            amount *= synergy.isMajor() ? 1.15F : 1.10F;
        }
        event.setAmount(amount);
        // 满级特殊弹射物（凋零骷髅头/龙息）；生命融合套装 + BOSS/龙肢体时强化（冷却减半，弹射更频繁）
        boolean enhanced = isBossDragonEnhanced(attacker);
        int cooldown = enhanced ? PROJECTILE_COOLDOWN_TICKS / 2 : PROJECTILE_COOLDOWN_TICKS;
        if (attacker.tickCount - PROJECTILE_COOLDOWN.getOrDefault(attacker, -1000) > cooldown) {
            fireMaxTierProjectile(attacker, target, state);
        }
    }

    /** 受害方：火焰弱点 / 摔落免疫 / 荆棘反弹 / 瞬移闪避 */
    private static void applyVictimEffects(LivingHurtEvent event, Player victim, Entity attacker) {
        float amount = event.getAmount();
        IPlayerBodyState state = PlayerBodyHelper.of(victim);
        // 火焰免疫（烈焰之心/末影龙之心）：直接吞掉火焰/岩浆类伤害；着火状态的清除由被动 tick 负责
        if (OrganEffectResolver.hasPassive(state, OrganPassive.FIRE_IMMUNE)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }
        if (OrganEffectResolver.hasPassive(state, OrganPassive.FIRE_WEAKNESS)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            amount *= 1.5F; // 怕火：火焰伤害 +50%
        }
        if (OrganEffectResolver.hasPassive(state, OrganPassive.FALL_IMMUNE)
                && event.getSource().is(DamageTypeTags.IS_FALL)) {
            amount = 0.0F; // 摔落免疫
        }
        event.setAmount(amount);
        // 荆棘反弹：近战伤害 30% 返还攻击者
        if (OrganEffectResolver.hasPassive(state, OrganPassive.THORNS) && attacker instanceof LivingEntity la) {
            la.hurt(victim.damageSources().thorns(victim), event.getAmount() * 0.3F);
        }
        // 瞬移闪避：受击 20% 概率瞬移（不减免伤害，只躲后续）
        if (OrganEffectResolver.hasPassive(state, OrganPassive.TELEPORT_DODGE)
                && victim.getRandom().nextFloat() < 0.2F) {
            teleportRandomly(victim);
        }
        // 墨雾脱身：受击 25% 概率喷墨隐身 4 秒（鱿鱼墨囊）
        if (OrganEffectResolver.hasPassive(state, OrganPassive.INK_CLOUD)
                && victim.getRandom().nextFloat() < 0.25F) {
            victim.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false));
            victim.level().addParticle(ParticleTypes.SQUID_INK,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                    0.0, 0.0, 0.0);
        }
        // 生态套装（3 件小共鸣 / 6 件大共鸣）：
        OrganEffectResolver.Synergy synergy = OrganEffectResolver.synergyOf(state, victim.level());
        // 纯异变：受击狂怒——概率获得速度（小：25% 速度 I 2 秒 / 大：30% 速度 II 3 秒）
        if (synergy.group() == SampleGroup.ABERRATION
                && victim.getRandom().nextFloat() < (synergy.isMajor() ? 0.30F : 0.25F)) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,
                    synergy.isMajor() ? 60 : 40, synergy.isMajor() ? 1 : 0, false, false));
        }
        // 纯末影：受击概率瞬移脱身（小共鸣 15% / 大共鸣 25%）
        if (synergy.group() == SampleGroup.ENDER
                && victim.getRandom().nextFloat() < (synergy.isMajor() ? 0.25F : 0.15F)) {
            teleportRandomly(victim);
        }
    }

    /**
     * 生命融合套装能量护盾：只在护甲/附魔/吸收心结算后仍会造成实际扣血的伤害
     * 上消耗能量抵消（100 能量抵消 1 伤害）——防护挡得住的伤害不耗能。
     * 能量足够则完全抵消并取消事件；不足则按比例抵扣后返回剩余伤害。
     */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof Player victim) {
            event.setAmount(applyEnergyShield(event, victim, event.getAmount()));
        }
    }

    private static float applyEnergyShield(LivingDamageEvent event, Player victim, float amount) {
        if (amount <= 0.0F || !AkaishiLifeFusionSet.isFullSet(victim)) {
            return amount;
        }
        long need = (long) Math.ceil(amount * AkaishiLifeFusionSet.ENERGY_PER_DAMAGE);
        long drained = AkaishiLifeFusionSet.drainEnergy(victim, need);
        if (drained <= 0) {
            return amount;
        }
        float negated = drained / (float) AkaishiLifeFusionSet.ENERGY_PER_DAMAGE;
        if (negated >= amount) {
            event.setCanceled(true);
            return 0.0F;
        }
        return amount - negated;
    }

    /** 命中附加效果：目标当前无同效果时补充 */
    private static void applyToTarget(LivingEntity target, MobEffect effect, int amplifier, int durationTicks) {
        if (!target.hasEffect(effect)) {
            target.addEffect(new MobEffectInstance(effect, durationTicks, amplifier, false, false));
        }
    }

    /** 满级特殊弹射物：器官品质 IV 且对应特殊效果时发射（不破坏方块，伤害较低） */
    private static void fireMaxTierProjectile(Player attacker, LivingEntity target, IPlayerBodyState state) {
        for (OrganEffectResolver.ActiveOrgan organ : OrganEffectResolver.collect(state)) {
            if (organ.effect() == null || organ.tier() != QualityTier.IV) {
                continue;
            }
            OrganSpecial special = organ.effect().special();
            if (special == null) {
                continue;
            }
            Level level = attacker.level();
            Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0)
                    .subtract(attacker.position().add(0, attacker.getEyeHeight(), 0)).normalize();
            if (special == OrganSpecial.WITHER_SKULL) {
                WitherSkull skull = new WitherSkull(level, attacker, dir.x, dir.y, dir.z);
                skull.setDangerous(false); // 不破坏方块
                skull.setPos(attacker.getX(), attacker.getEyeY(), attacker.getZ());
                level.addFreshEntity(skull);
                PROJECTILE_COOLDOWN.put(attacker, attacker.tickCount);
            } else if (special == OrganSpecial.DRAGON_BREATH) {
                DragonFireball ball = new DragonFireball(level, attacker, dir.x, dir.y, dir.z);
                ball.setPos(attacker.getX(), attacker.getEyeY(), attacker.getZ());
                level.addFreshEntity(ball);
                PROJECTILE_COOLDOWN.put(attacker, attacker.tickCount);
            } else if (special == OrganSpecial.SONIC_BOOM) {
                fireSonicBoom(attacker, target);
                PROJECTILE_COOLDOWN.put(attacker, attacker.tickCount);
            }
            return; // 一次攻击只触发一种特殊效果
        }
    }

    /** 生命融合套装是否强化 BOSS/龙肢体被动：穿齐 4 件且移植了 BOSS/龙族来源器官 */
    private static boolean isBossDragonEnhanced(Player player) {
        if (!AkaishiLifeFusionSet.isFullSet(player)) {
            return false;
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        return state != null && AkaishiLifeFusionSet.hasBossOrDragonOrgan(player, state);
    }

    /**
     * 音爆：沿玩家视线直线瞬间释放，命中圆柱（半径 1 格）内投影 [0, 射程] 的全部生物。
     * 音爆伤害无视护甲（原版 sonic_boom 伤害类型），单次 10 点，4 秒冷却；粒子 + 咆哮音效表现。
     */
    private static void fireSonicBoom(Player attacker, LivingEntity target) {
        Level level = attacker.level();
        Vec3 start = attacker.position().add(0.0, attacker.getEyeHeight(), 0.0);
        Vec3 toTarget = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(start);
        double range = Math.min(SONIC_BOOM_RANGE, toTarget.length());
        Vec3 dir = toTarget.normalize();
        // 视线圆柱筛选：投影距离 [0, range] 且到轴线垂距在命中半径内（含目标与其后方的穿透目标）
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                attacker.getBoundingBox().expandTowards(dir.scale(range)).inflate(SONIC_BOOM_RADIUS),
                e -> e.isAlive() && e != attacker)) {
            Vec3 to = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0).subtract(start);
            double proj = to.dot(dir);
            if (proj <= 0.0 || proj > range) {
                continue;
            }
            double hitRadius = SONIC_BOOM_RADIUS + victim.getBbWidth() * 0.5;
            if (to.subtract(dir.scale(proj)).lengthSqr() > hitRadius * hitRadius) {
                continue;
            }
            victim.hurt(level.damageSources().sonicBoom(attacker), SONIC_BOOM_DAMAGE);
        }
        // 音爆波前粒子（客户端播放方向性粒子）+ 咆哮音效
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, start.x, start.y, start.z,
                    1, dir.x, dir.y, dir.z, 0.0);
        }
        level.playSound(null, start.x, start.y, start.z,
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);
    }

    /** 随机瞬移：尝试 16 次找无碰撞且不浸水的落点 */
    private static boolean teleportRandomly(Player player) {
        for (int i = 0; i < 16; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 12.0;
            double y = player.getY() + player.getRandom().nextInt(7) - 3;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 12.0;
            if (player.level().noCollision(player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ()))) {
                player.teleportTo(x, y, z);
                return true;
            }
        }
        return false;
    }
}
