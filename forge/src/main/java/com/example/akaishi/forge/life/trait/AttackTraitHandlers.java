package com.example.akaishi.forge.life.trait;

import com.example.akaishi.api.mechanical.trait.IMechanicalTraitHandler;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitHandlerRegistry;
import com.example.akaishi.life.mechanical.trait.MechanicalTraits;
import com.example.akaishi.life.mechanical.trait.TraitTier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 攻击向材料特性处理器（6 个）。
 * <p>
 * 全部数值均为<b>待调手感值</b>；等级由分发层按「该器官内的等级」逐器官传入（M2）。
 */
final class AttackTraitHandlers {

    private AttackTraitHandlers() {
    }

    static void register() {
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.ARMOR_PIERCE, new ArmorPierce());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.EXECUTIONER, new Executioner());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.COMBO_DRIVER, new ComboDriver());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.HEMOSIPHON, new Hemosiphon());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.KINETIC_SURGE, new KineticSurge());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.REACTIVE_PLATING, new ReactivePlating());
    }

    /**
     * 破甲刃：目标有护甲时伤害 +10%/15%/20%/30%。
     * <p><b>近似实现</b>：无"无视护甲"直接入口，等价折算为对带甲目标的增伤（口径见报告存疑）。
     */
    private static final class ArmorPierce implements IMechanicalTraitHandler {
        private static final TraitTier BONUS = TraitTier.of(0.10F, 0.15F, 0.20F, 0.30F);

        @Override
        public float modifyOutgoingDamage(Player attacker, LivingEntity target, float amount, int level) {
            if (target.getArmorValue() <= 0) {
                return amount;
            }
            return amount * (1F + BONUS.at(level));
        }
    }

    /** 处决程序：目标生命低于 20%/22%/25%/30% 时伤害 +15%/20%/25%/35%。（待调手感值） */
    private static final class Executioner implements IMechanicalTraitHandler {
        private static final TraitTier THRESHOLD = TraitTier.of(0.20F, 0.22F, 0.25F, 0.30F);
        private static final TraitTier BONUS = TraitTier.of(0.15F, 0.20F, 0.25F, 0.35F);

        @Override
        public float modifyOutgoingDamage(Player attacker, LivingEntity target, float amount, int level) {
            if (target.getHealth() / target.getMaxHealth() < THRESHOLD.at(level)) {
                return amount * (1F + BONUS.at(level));
            }
            return amount;
        }
    }

    /**
     * 连击驱动：每次命中累叠，每层 +3%/4%/5%/6%，层数上限 3/4/5/6。
     * <p>连击窗口与"同 tick 只计一次"（多器官共用状态）见 {@link MechanicalTraitRuntime}。（待调手感值）
     */
    private static final class ComboDriver implements IMechanicalTraitHandler {
        private static final TraitTier PER_STACK = TraitTier.of(0.03F, 0.04F, 0.05F, 0.06F);
        private static final TraitTier MAX_STACKS = TraitTier.of(3, 4, 5, 6);
        private static final long WINDOW_TICKS = 60;

        @Override
        public float modifyOutgoingDamage(Player attacker, LivingEntity target, float amount, int level) {
            MechanicalTraitRuntime.State st = MechanicalTraitRuntime.of(attacker.getUUID());
            long now = attacker.tickCount;
            if (now - st.comboLastTick > WINDOW_TICKS) {
                st.comboHits = 0;
            }
            if (st.comboLastTick != now) {
                st.comboHits = Math.min(st.comboHits + 1, MAX_STACKS.atInt(level));
                st.comboLastTick = now;
            }
            return amount * (1F + st.comboHits * PER_STACK.at(level));
        }
    }

    /** 血液虹吸：命中时按伤害 3%/5%/7%/9% 回血，单次上限 3 点。（待调手感值） */
    private static final class Hemosiphon implements IMechanicalTraitHandler {
        private static final TraitTier RATIO = TraitTier.of(0.03F, 0.05F, 0.07F, 0.09F);
        private static final float CAP = 3.0F;

        @Override
        public float modifyOutgoingDamage(Player attacker, LivingEntity target, float amount, int level) {
            float heal = Math.min(amount * RATIO.at(level), CAP);
            if (heal > 0F && attacker.getHealth() < attacker.getMaxHealth()) {
                attacker.heal(heal);
            }
            return amount;
        }
    }

    /**
     * 动能释放：冲刺状态下"首击"伤害 +10%/15%/20%/25%；Lv4 命中附带击退。
     * <p>"首击"= 距上次命中超过 {@link #FIRST_HIT_WINDOW} tick。（待调手感值）
     */
    private static final class KineticSurge implements IMechanicalTraitHandler {
        private static final TraitTier BONUS = TraitTier.of(0.10F, 0.15F, 0.20F, 0.25F);
        private static final long FIRST_HIT_WINDOW = 20;
        private static final double KNOCKBACK_STRENGTH = 0.4D;

        @Override
        public float modifyOutgoingDamage(Player attacker, LivingEntity target, float amount, int level) {
            MechanicalTraitRuntime.State st = MechanicalTraitRuntime.of(attacker.getUUID());
            boolean firstHit = attacker.tickCount - st.lastAttackTick > FIRST_HIT_WINDOW;
            if (attacker.isSprinting() && firstHit) {
                return amount * (1F + BONUS.at(level));
            }
            return amount;
        }

        @Override
        public void onAttack(Player attacker, LivingEntity target, int level) {
            MechanicalTraitRuntime.State st = MechanicalTraitRuntime.of(attacker.getUUID());
            st.lastAttackTick = attacker.tickCount;
            if (level >= 4) {
                target.knockback(KNOCKBACK_STRENGTH, target.getX() - attacker.getX(), target.getZ() - attacker.getZ());
            }
        }
    }

    /** 反应装甲：受击时反弹 8%/12%/16%/20% 伤害给来源（荆棘伤害）。（待调手感值） */
    private static final class ReactivePlating implements IMechanicalTraitHandler {
        private static final TraitTier RATIO = TraitTier.of(0.08F, 0.12F, 0.16F, 0.20F);

        @Override
        public float modifyIncomingDamage(Player player, DamageSource source, float amount, int level) {
            Entity attacker = source.getEntity();
            if (attacker instanceof LivingEntity living && attacker != player) {
                living.hurt(player.damageSources().thorns(player), amount * RATIO.at(level));
            }
            return amount;
        }
    }
}
