package com.example.akaishi.forge.life.trait;

import com.example.akaishi.api.mechanical.trait.IMechanicalTraitHandler;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitHandlerRegistry;
import com.example.akaishi.life.mechanical.trait.MechanicalTraits;
import com.example.akaishi.life.mechanical.trait.TraitTier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * 机动向材料特性处理器（2 个）。
 * <p>全部数值均为<b>待调手感值</b>。
 */
final class MobilityTraitHandlers {

    private MobilityTraitHandlers() {
    }

    static void register() {
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.KINETIC_BOOST, new KineticBoost());
        MechanicalTraitHandlerRegistry.register(MechanicalTraits.PHASE_STEP, new PhaseStep());
    }

    /**
     * 动能增幅：持续移动 4/3/3/2s 后移速 +4%/6%/8%/10%（按有效等级取值）。
     * <p>统一走框架的跨器官有效等级，不再自行按全身器官汇总。
     */
    private static final class KineticBoost implements IMechanicalTraitHandler {
        private static final TraitTier DELAY_SECONDS = TraitTier.of(4, 3, 3, 2);
        private static final TraitTier BONUS = TraitTier.of(0.04F, 0.06F, 0.08F, 0.10F);

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide) {
                return;
            }
            MechanicalTraitRuntime.State st = MechanicalTraitRuntime.of(player.getUUID());
            float walk = player.walkDist;
            boolean moving = Math.abs(walk - st.lastWalkDist) > 0.001F;
            st.lastWalkDist = walk;
            st.movingTicks = moving ? st.movingTicks + 1 : 0;

            double bonus = st.movingTicks < (int) (DELAY_SECONDS.at(level) * 20) ? 0 : BONUS.at(level);
            TraitSupport.setModifier(player, Attributes.MOVEMENT_SPEED, MechanicalTraits.KINETIC_BOOST,
                    bonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        @Override
        public void onUnequip(Player player, int level) {
            TraitSupport.setModifier(player, Attributes.MOVEMENT_SPEED, MechanicalTraits.KINETIC_BOOST,
                    0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }

    /**
     * 相位步频：受击后获得 0.3/0.4/0.5/0.7s 无敌，冷却 12/10/8/6s。
     * <p>窗口内多次受击不重复触发（以冷却门控）。（待调手感值）
     */
    private static final class PhaseStep implements IMechanicalTraitHandler {
        private static final TraitTier INVULN_SECONDS = TraitTier.of(0.3F, 0.4F, 0.5F, 0.7F);
        private static final TraitTier COOLDOWN_SECONDS = TraitTier.of(12, 10, 8, 6);

        @Override
        public float modifyIncomingDamage(Player player, DamageSource source, float amount, int level) {
            MechanicalTraitRuntime.State st = MechanicalTraitRuntime.of(player.getUUID());
            long now = player.tickCount;
            if (now < st.phaseReadyTick) {
                return amount;
            }
            int invulnTicks = (int) (INVULN_SECONDS.at(level) * 20);
            player.invulnerableTime = Math.max(player.invulnerableTime, invulnTicks);
            st.phaseReadyTick = now + (long) (COOLDOWN_SECONDS.at(level) * 20);
            return amount;
        }
    }
}
