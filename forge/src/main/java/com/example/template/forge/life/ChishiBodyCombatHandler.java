package com.example.template.forge.life;

import com.example.template.life.body.IPlayerBodyState;
import com.example.template.life.body.PlayerBodyHelper;
import com.example.template.life.organ.OrganEffectResolver;
import com.example.template.life.organ.OrganPassive;
import com.example.template.life.organ.OrganSpecial;
import com.example.template.life.organ.QualityTier;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.WeakHashMap;
import java.util.Map;

/**
 * 器官战斗效果处理器（Forge 服务端）：
 * - 攻击方（移植玩家）：命中附加效果（减速/中毒/凋零）、伤害增幅（跳跃/水下/弹射物）、满级特殊弹射物
 * - 受害方（移植玩家）：火焰弱点 / 摔落免疫 / 荆棘反弹 / 瞬移闪避
 * 满级特殊弹射物（凋零骷髅头/龙息）仅在对应器官品质 IV 时触发，且不破坏方块、伤害较低。
 */
public final class ChishiBodyCombatHandler {

    public static final ChishiBodyCombatHandler INSTANCE = new ChishiBodyCombatHandler();

    /** 特殊弹射物冷却：玩家 → 上次发射 tick（4 秒） */
    private static final Map<Player, Integer> PROJECTILE_COOLDOWN = new WeakHashMap<>();
    private static final int PROJECTILE_COOLDOWN_TICKS = 80;

    private ChishiBodyCombatHandler() {
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
        event.setAmount(amount);
        // 满级特殊弹射物（凋零骷髅头/龙息）
        if (attacker.tickCount - PROJECTILE_COOLDOWN.getOrDefault(attacker, -1000) > PROJECTILE_COOLDOWN_TICKS) {
            fireMaxTierProjectile(attacker, target, state);
        }
    }

    /** 受害方：火焰弱点 / 摔落免疫 / 荆棘反弹 / 瞬移闪避 */
    private static void applyVictimEffects(LivingHurtEvent event, Player victim, Entity attacker) {
        float amount = event.getAmount();
        IPlayerBodyState state = PlayerBodyHelper.of(victim);
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
            }
            return; // 一次攻击只发射一种
        }
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
