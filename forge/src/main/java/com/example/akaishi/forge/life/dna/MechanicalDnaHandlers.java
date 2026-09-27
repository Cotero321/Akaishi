package com.example.akaishi.forge.life.dna;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffectHandler;
import com.example.akaishi.api.mechanical.MechanicalEffectHandlerRegistry;
import com.example.akaishi.combat.ModCombatAttributes;
import com.example.akaishi.life.mechanical.MechanicalSpecialEffect;
import com.example.akaishi.life.mechanical.trait.TraitTier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 机械基因的运行时实现入口（forge 侧）。
 * <p>
 * 机制型基因（6 个）：每个 = 一个 {@link MechanicalSpecialEffect}（效果定义，注册于 common 双注册表之一）+ 本类的一个
 * {@link IMechanicalDnaEffectHandler} 实现（运行时逻辑，注册于 {@link MechanicalEffectHandlerRegistry}）。
 * 另含属性型基因 {@code akaishi:long_reach}（卫道士）：需要挂载/清理瞬时属性修饰符，故同样以 handler 承载
 * （复用同一套 onEquip/onUnequip 基础设施）。与既有内置效果完全同构，未新造机制。
 * <p>
 * <b>强度口径</b>：全部数值按「跨器官有效等级」1~4 档取值（{@code MechanicalAggregation.effectLevels}，取各器官最高等级），
 * 分发层<b>每个效果只回调一次</b>，禁止逐器官线性叠加。全部数值均为<b>待调手感值</b>。
 * <p>
 * <b>自带负面</b>：6 个机制型基因各有一条真实生效的代价（掉血 / 掉饥饿 / 受伤加重 / 减速 / 攻速下降 / 虚弱）。
 */
public final class MechanicalDnaHandlers {

    private MechanicalDnaHandlers() {
    }

    /** 注册全部基因处理器；须在效果定义与机械基因注册之后调用（仅调用一次）。 */
    public static void register() {
        MechanicalEffectHandlerRegistry.register(MechanicalSpecialEffect.OVERHEAT_CORE.getId(), new OverheatCore());
        MechanicalEffectHandlerRegistry.register(MechanicalSpecialEffect.PARASITIC_SYMBIOSIS.getId(), new ParasiticSymbiosis());
        MechanicalEffectHandlerRegistry.register(MechanicalSpecialEffect.ECHOLOCATION.getId(), new Echolocation());
        MechanicalEffectHandlerRegistry.register(MechanicalSpecialEffect.ARMOR_OVERLOAD.getId(), new ArmorOverload());
        MechanicalEffectHandlerRegistry.register(MechanicalSpecialEffect.NEURAL_SPASM.getId(), new NeuralSpasm());
        MechanicalEffectHandlerRegistry.register(MechanicalSpecialEffect.METABOLIC_OVERDRAFT.getId(), new MetabolicOverdraft());
        // 属性型：长臂（近战攻击距离提升，镜像 OrganPassive.LONG_REACH）
        MechanicalEffectHandlerRegistry.register(MechanicalSpecialEffect.LONG_REACH.getId(), new LongReach());
    }

    private static int clamp(int level) {
        return Math.max(1, Math.min(4, level));
    }

    /** 按固定 key 挂载瞬时属性修饰符（amount == 0 时移除）；不写 NBT，摘除义体后由 onUnequip 清理。 */
    private static void setModifier(LivingEntity entity, Attribute attribute, String key,
                                    double amount, AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        UUID id = UUID.nameUUIDFromBytes(("akaishi:mech_dna:" + key).getBytes(StandardCharsets.UTF_8));
        AttributeModifier existing = instance.getModifier(id);
        if (amount == 0) {
            if (existing != null) {
                instance.removeModifier(id);
            }
            return;
        }
        if (existing != null) {
            if (existing.getAmount() == amount) {
                return;
            }
            instance.removeModifier(id);
        }
        instance.addTransientModifier(new AttributeModifier(id, "akaishi_mech_dna_" + key, amount, operation));
    }

    // ==================== 过热核心 ====================

    /**
     * 过热核心：<b>正</b>——血量越低攻击越高（按 3 段阈值分段爬升，每跨一段叠加一份增伤）；
     * <b>负</b>——按固定间隔持续缓慢掉血（不致死：生命 &le; 1 时停手）。
     */
    private static final class OverheatCore implements IMechanicalDnaEffectHandler {
        /** 每跨一段血量的增伤（Lv1~4），待调手感值 */
        private static final float[] SEGMENT_BONUS = {0.04F, 0.06F, 0.08F, 0.10F};
        /** 分段阈值：血量占比低于该值即跨过一段（待调手感值） */
        private static final float[] SEGMENT_THRESHOLD = {0.75F, 0.50F, 0.25F};
        /** 掉血间隔 tick（Lv1~4 = 120/90/60/40，约 0.5~1.5 生命/秒），待调手感值 */
        private static final TraitTier DRAIN_INTERVAL = TraitTier.of(120, 90, 60, 40);
        /** 每次扣除的生命，待调手感值 */
        private static final float DRAIN_AMOUNT = 1.0F;
        /** 掉血下限：不因本基因致死 */
        private static final float DRAIN_FLOOR = 1.0F;

        /** 正向：血量占比 healthRatio 时该等级的伤害倍率（供探针/单测）。 */
        static float damageMultiplier(float healthRatio, int level) {
            int segments = 0;
            for (float threshold : SEGMENT_THRESHOLD) {
                if (healthRatio < threshold) {
                    segments++;
                }
            }
            return 1F + segments * SEGMENT_BONUS[clamp(level) - 1];
        }

        /** 负向：该等级的掉血间隔 tick（供探针/单测）。 */
        static int drainIntervalTicks(int level) {
            return Math.max(1, DRAIN_INTERVAL.atInt(level));
        }

        @Override
        public float modifyOutgoingDamage(Player attacker, LivingEntity target, float amount, int level) {
            float max = attacker.getMaxHealth();
            float ratio = max > 0F ? attacker.getHealth() / max : 1F;
            return amount * damageMultiplier(ratio, level);
        }

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide || player.isDeadOrDying()) {
                return;
            }
            if (player.tickCount % drainIntervalTicks(level) != 0) {
                return;
            }
            float health = player.getHealth();
            if (health > DRAIN_FLOOR) {
                player.setHealth(Math.max(DRAIN_FLOOR, health - DRAIN_AMOUNT));
            }
        }
    }

    // ==================== 寄生共生 ====================

    /**
     * 寄生共生：<b>正</b>——击杀目标回复生命；<b>负</b>——周期性增加饥饿消耗（食物消耗加速）。
     */
    private static final class ParasiticSymbiosis implements IMechanicalDnaEffectHandler {
        /** 击杀回复生命（Lv1~4 = 2/3/4/5），待调手感值 */
        private static final TraitTier KILL_HEAL = TraitTier.of(2, 3, 4, 5);
        /** 每 20 tick 追加的饥饿消耗（Lv1~4），待调手感值 */
        private static final TraitTier EXHAUSTION = TraitTier.of(0.05F, 0.08F, 0.12F, 0.16F);
        private static final int EXHAUSTION_INTERVAL = 20;

        /** 正向：该等级的击杀回血量（供探针/单测）。 */
        static float killHeal(int level) {
            return KILL_HEAL.at(level);
        }

        /** 负向：该等级每个消耗周期的饥饿消耗（供探针/单测）。 */
        static float exhaustionPerInterval(int level) {
            return EXHAUSTION.at(level);
        }

        @Override
        public void onKill(Player player, LivingEntity victim, int level) {
            if (player.level().isClientSide) {
                return;
            }
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(KILL_HEAL.at(level));
            }
        }

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide || player.isDeadOrDying()) {
                return;
            }
            if (player.tickCount % EXHAUSTION_INTERVAL == 0) {
                player.causeFoodExhaustion(EXHAUSTION.at(level));
            }
        }
    }

    // ==================== 回声定位 ====================

    /**
     * 回声定位：<b>正</b>——周期性给周围生物（含潜行目标）打发光标记；<b>负</b>——受到的爆炸伤害加重。
     */
    private static final class Echolocation implements IMechanicalDnaEffectHandler {
        /** 标记半径（Lv1~4 = 12/16/20/24），待调手感值 */
        private static final TraitTier PING_RADIUS = TraitTier.of(12, 16, 20, 24);
        /** 标记持续 tick（Lv1~4 = 80/100/120/140），待调手感值 */
        private static final TraitTier GLOW_TICKS = TraitTier.of(80, 100, 120, 140);
        /** 爆炸伤害加重比例（Lv1~4 = +15/25/35/50%），待调手感值 */
        private static final TraitTier EXPLOSION_PENALTY = TraitTier.of(0.15F, 0.25F, 0.35F, 0.50F);
        private static final int PING_INTERVAL = 20;

        /** 正向：该等级的标记半径（供探针/单测）。 */
        static int pingRadius(int level) {
            return PING_RADIUS.atInt(level);
        }

        /** 负向：该等级的爆炸伤害加成倍率（供探针/单测）。 */
        static float explosionMultiplier(int level) {
            return 1F + EXPLOSION_PENALTY.at(level);
        }

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide || player.isDeadOrDying()) {
                return;
            }
            if (player.tickCount % PING_INTERVAL != 0) {
                return;
            }
            AABB box = player.getBoundingBox().inflate(PING_RADIUS.at(level));
            for (LivingEntity nearby : player.level().getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
                if (nearby == player) {
                    continue;
                }
                // 发光标记：潜行目标同样会被勾出轮廓（本基因"含潜行目标"的落地方式）
                nearby.addEffect(new MobEffectInstance(MobEffects.GLOWING, GLOW_TICKS.atInt(level), 0, false, false));
            }
        }

        @Override
        public float modifyIncomingDamage(Player player, DamageSource source, float amount, int level) {
            if (source.is(DamageTypeTags.IS_EXPLOSION)) {
                return amount * explosionMultiplier(level);
            }
            return amount;
        }
    }

    // ==================== 装甲过载 ====================

    /**
     * 装甲过载：<b>正</b>——临时提升护甲；<b>负</b>——移动速度下降（两者同时生效，形成取舍）。
     */
    private static final class ArmorOverload implements IMechanicalDnaEffectHandler {
        /** 护甲提升（Lv1~4 = 2/4/6/8），待调手感值 */
        private static final TraitTier ARMOR_BONUS = TraitTier.of(2, 4, 6, 8);
        /** 移速下降比例（Lv1~4 = 5/8/12/16%），待调手感值 */
        private static final TraitTier SPEED_PENALTY = TraitTier.of(0.05F, 0.08F, 0.12F, 0.16F);
        private static final String KEY_ARMOR = "armor_overload_armor";
        private static final String KEY_SPEED = "armor_overload_speed";

        /** 正向：该等级的护甲提升（供探针/单测）。 */
        static double armorBonus(int level) {
            return ARMOR_BONUS.at(level);
        }

        /** 负向：该等级的移速修饰值（负，供探针/单测）。 */
        static double speedPenalty(int level) {
            return -SPEED_PENALTY.at(level);
        }

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide || player.isDeadOrDying()) {
                return;
            }
            setModifier(player, Attributes.ARMOR, KEY_ARMOR, armorBonus(level), AttributeModifier.Operation.ADDITION);
            setModifier(player, Attributes.MOVEMENT_SPEED, KEY_SPEED, speedPenalty(level),
                    AttributeModifier.Operation.MULTIPLY_BASE);
        }

        @Override
        public void onUnequip(Player player, int level) {
            setModifier(player, Attributes.ARMOR, KEY_ARMOR, 0, AttributeModifier.Operation.ADDITION);
            setModifier(player, Attributes.MOVEMENT_SPEED, KEY_SPEED, 0, AttributeModifier.Operation.MULTIPLY_BASE);
        }
    }

    // ==================== 神经痉挛 ====================

    /**
     * 神经痉挛：<b>正</b>——周期进入「痉挛窗口」，窗口内暴击率封顶至 1.0（下一击必定暴击）；
     * <b>负</b>——痉挛窗口内攻击速度下降（攻击间隔变长）。
     * <p>全部状态由 {@code tickCount % 周期} 推导，不落存档、无需额外玩家状态表。
     */
    private static final class NeuralSpasm implements IMechanicalDnaEffectHandler {
        /** 触发周期 tick（Lv1~4 = 240/200/160/120），待调手感值 */
        private static final TraitTier INTERVAL = TraitTier.of(240, 200, 160, 120);
        /** 痉挛窗口 tick（Lv1~4 = 40/60/80/100），待调手感值 */
        private static final TraitTier WINDOW = TraitTier.of(40, 60, 80, 100);
        /** 窗口内攻速下降比例（Lv1~4 = 10/15/20/28%），待调手感值 */
        private static final TraitTier SPEED_PENALTY = TraitTier.of(0.10F, 0.15F, 0.20F, 0.28F);
        /** 暴击率加成：叠加到总暴击率后被 1.0 上限裁剪 ⇒ 必定暴击 */
        private static final double CRIT_CHANCE_BOOST = 5.0D;
        private static final String KEY_SPEED = "neural_spasm_speed";
        private static final String KEY_CRIT = "neural_spasm_crit";

        /** 正向：该等级是否处于痉挛窗口（供探针/单测）。 */
        static boolean inWindow(int tickCount, int level) {
            return tickCount % Math.max(1, INTERVAL.atInt(level)) < Math.max(1, WINDOW.atInt(level));
        }

        /** 负向：该等级的攻速修饰值（负，供探针/单测）。 */
        static double speedPenalty(int level) {
            return -SPEED_PENALTY.at(level);
        }

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide || player.isDeadOrDying()) {
                return;
            }
            boolean active = inWindow(player.tickCount, level);
            setModifier(player, Attributes.ATTACK_SPEED, KEY_SPEED,
                    active ? speedPenalty(level) : 0, AttributeModifier.Operation.MULTIPLY_BASE);
            setModifier(player, ModCombatAttributes.CRIT_CHANCE.get(), KEY_CRIT,
                    active ? CRIT_CHANCE_BOOST : 0, AttributeModifier.Operation.ADDITION);
        }

        @Override
        public void onUnequip(Player player, int level) {
            setModifier(player, Attributes.ATTACK_SPEED, KEY_SPEED, 0, AttributeModifier.Operation.MULTIPLY_BASE);
            setModifier(player, ModCombatAttributes.CRIT_CHANCE.get(), KEY_CRIT, 0, AttributeModifier.Operation.ADDITION);
        }
    }

    // ==================== 代谢透支 ====================

    /**
     * 代谢透支：<b>正</b>——按更短的间隔周期回复生命；<b>负</b>——处于恢复状态（生命未满）时持续附带虚弱。
     */
    private static final class MetabolicOverdraft implements IMechanicalDnaEffectHandler {
        /** 回血间隔 tick（Lv1~4 = 100/80/60/40），待调手感值 */
        private static final TraitTier HEAL_INTERVAL = TraitTier.of(100, 80, 60, 40);
        /** 虚弱持续 tick（在恢复窗口内保持），待调手感值 */
        private static final int WEAKNESS_TICKS = 40;

        /** 正向：该等级的回血间隔 tick（供探针/单测）。 */
        static int healIntervalTicks(int level) {
            return Math.max(1, HEAL_INTERVAL.atInt(level));
        }

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide || player.isDeadOrDying()) {
                return;
            }
            if (player.getHealth() >= player.getMaxHealth()) {
                return;
            }
            // 负：恢复期间附带虚弱（未满血即为恢复窗口，保持虚弱可见）
            if (!player.hasEffect(MobEffects.WEAKNESS)) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAKNESS_TICKS, 0, false, false));
            }
            // 正：按等级间隔回血
            if (player.tickCount % healIntervalTicks(level) == 0) {
                player.heal(1.0F);
            }
        }
    }

    // ==================== 长臂（属性型） ====================

    /**
     * 长臂：按等级提升近战攻击距离（挂 Forge 专属属性 {@code ENTITY_REACH}，与生物侧 LONG_REACH 同属性）。
     * <p>无负面；摘除义体后由 {@link #onUnequip} 移除修饰符，避免属性残留。
     */
    private static final class LongReach implements IMechanicalDnaEffectHandler {
        /** 近战攻击距离加成（格，Lv1~4 = +0.5/1.0/1.5/2.0），待调手感值 */
        private static final TraitTier REACH_BONUS = TraitTier.of(0.5F, 1.0F, 1.5F, 2.0F);
        private static final String KEY_REACH = "long_reach";

        /** 正向：该等级的攻击距离加成（供探针/单测）。 */
        static double reachBonus(int level) {
            return REACH_BONUS.at(level);
        }

        @Override
        public void onPlayerTick(Player player, int level) {
            if (player.level().isClientSide) {
                return;
            }
            setModifier(player, ForgeMod.ENTITY_REACH.get(), KEY_REACH, reachBonus(level),
                    AttributeModifier.Operation.ADDITION);
        }

        @Override
        public void onUnequip(Player player, int level) {
            setModifier(player, ForgeMod.ENTITY_REACH.get(), KEY_REACH, 0, AttributeModifier.Operation.ADDITION);
        }
    }
}
