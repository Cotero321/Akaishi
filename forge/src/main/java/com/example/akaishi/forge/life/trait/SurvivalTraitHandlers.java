package com.example.akaishi.forge.life.trait;

import com.example.akaishi.api.mechanical.trait.IMechanicalTraitHandler;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitHandlerRegistry;
import com.example.akaishi.life.mechanical.trait.MechanicalTraits;
import com.example.akaishi.life.mechanical.trait.TraitTier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 生存向材料特性处理器（6 个中的 5 个；{@code debuff_ward} 需平台事件，见
 * {@link AkaishiMechanicalTraitEvents}）。
 * <p>
 * 全部数值均为<b>待调手感值</b>；等级由分发层传入「跨器官汇总后的<b>有效等级</b>」，
 * 每个特性每次钩子只回调一次（不再逐器官累加）。
 */
final class SurvivalTraitHandlers {

    private SurvivalTraitHandlers() {
    }

    static void register() {
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.REINFORCED_FRAME, new ReinforcedFrame());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.VITAL_RESERVE, new VitalReserve());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.SELF_REPAIR, new SelfRepair());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.ABLATION_ARMOR, new AblationArmor());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.SHOCK_ABSORB, new ShockAbsorb());
    }

    /** 强化骨架：受击 -1/-1/-2/-2（按有效等级单次应用）；Lv4 额外 5% 概率完全免伤（每次受击只掷一次）。（待调手感值） */
    private static final class ReinforcedFrame implements IMechanicalTraitHandler {
        private static final TraitTier REDUCTION = TraitTier.of(1, 1, 2, 2);
        private static final float L4_IMMUNE_CHANCE = 0.05F;

        @Override
        public float modifyIncomingDamage(Player player, DamageSource source, float amount, int level) {
            if (level >= 4 && player.getRandom().nextFloat() < L4_IMMUNE_CHANCE) {
                return 0F;
            }
            return Math.max(0F, amount - REDUCTION.at(level));
        }
    }

    /**
     * 生命储备：最大生命 +2/+4/+6/+8（按有效等级取值）。
     * <p>统一走框架的跨器官有效等级，不再自行按全身器官汇总。
     */
    private static final class VitalReserve implements IMechanicalTraitHandler {
        private static final TraitTier BONUS = TraitTier.of(2, 4, 6, 8);

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide) {
                return;
            }
            TraitSupport.setModifier(player, Attributes.MAX_HEALTH, MechanicalTraits.VITAL_RESERVE,
                    BONUS.at(level), AttributeModifier.Operation.ADDITION);
        }

        @Override
        public void onUnequip(Player player, int level) {
            TraitSupport.setModifier(player, Attributes.MAX_HEALTH, MechanicalTraits.VITAL_RESERVE,
                    0, AttributeModifier.Operation.ADDITION);
        }
    }

    /** 自修复：脱战 8/6/5/4s 后，每 20/20/20/10 tick 回 1 点血（按有效等级单次生效）。（待调手感值） */
    private static final class SelfRepair implements IMechanicalTraitHandler {
        private static final TraitTier DELAY_SECONDS = TraitTier.of(8, 6, 5, 4);
        private static final TraitTier HEAL_INTERVAL_TICKS = TraitTier.of(20, 20, 20, 10);

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide || player.isDeadOrDying()) {
                return;
            }
            long lastCombat = Math.max(player.getLastHurtByMobTimestamp(), player.getLastHurtMobTimestamp());
            long idle = (long) player.tickCount - lastCombat;
            if (idle < (long) (DELAY_SECONDS.at(level) * 20)) {
                return;
            }
            int interval = Math.max(1, HEAL_INTERVAL_TICKS.atInt(level));
            if (player.tickCount % interval == 0 && player.getHealth() < player.getMaxHealth()) {
                player.heal(1.0F);
            }
        }
    }

    /** 烧蚀装甲：受击伤害 -10%/-15%/-20%/-25%（按有效等级单次乘算）。（待调手感值） */
    private static final class AblationArmor implements IMechanicalTraitHandler {
        private static final TraitTier RETAIN = TraitTier.of(0.90F, 0.85F, 0.80F, 0.75F);

        @Override
        public float modifyIncomingDamage(Player player, DamageSource source, float amount, int level) {
            return amount * RETAIN.at(level);
        }
    }

    /** 冲击吸收：摔落伤害保留 70%/55%/40%/0%（Lv4 完全免摔落）。（待调手感值） */
    private static final class ShockAbsorb implements IMechanicalTraitHandler {
        private static final TraitTier RETAIN = TraitTier.of(0.70F, 0.55F, 0.40F, 0.0F);

        @Override
        public float modifyIncomingDamage(Player player, DamageSource source, float amount, int level) {
            if (!source.is(DamageTypeTags.IS_FALL)) {
                return amount;
            }
            return amount * RETAIN.at(level);
        }
    }
}
