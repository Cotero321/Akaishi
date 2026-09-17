package com.example.akaishi.forge.life;

import com.example.akaishi.combat.ModCombatAttributes;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.effect.ForbiddenSetHooks;
import com.example.akaishi.effect.ModEffects;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.item.curio.AkaishiSocketCurioItem;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 「禁忌」四件饰品的统一生效层（Forge 服务端）。
 *
 * <p>四件共用本 Handler 的成因：效果彼此交织（连打复用生命之触缓存、黄心与亢奋同源、
 * 减伤要按「四类减半 → 套装再减」叠乘），拆成四个 Handler 反而要在处理器之间共享状态。
 * 判定口径全部读 {@link ModConfig} 的 volatile 字段，禁止硬编码（设计稿 D187）。</p>
 *
 * <p>生命周期：① 装入游戏事件总线后由 {@link #onLivingHurt} 承接攻防双方；② 常驻增益/周期自施走
 * {@link #onPlayerTick}；③ 限时属性回收走 {@link #onLivingTick}（否则非玩家目标身上的限时修饰符永不过期）。</p>
 */
public final class AkaishiSocketEffectHandler {

    public static final AkaishiSocketEffectHandler INSTANCE = new AkaishiSocketEffectHandler();

    /** 套装有效攻击计数的持久化键（D85：持久化、不衰减） */
    private static final String TAG_SET_ATTACKS = "AkaishiForbiddenSetAttacks";
    /** 濒死免死冷却结束的存档游戏刻（持久化：写 NBT，重登不清零） */
    private static final String TAG_NEAR_DEATH_READY_AT = "AkaishiForbiddenNearDeathReadyAt";

    // ===== 属性修饰 UUID（同一 UUID 不可混用常驻/限时两种挂载）=====
    private static final UUID U_REACH_ENTITY = uuid("forbidden_reach_entity");
    private static final UUID U_REACH_BLOCK = uuid("forbidden_reach_block");
    private static final UUID U_SEAL_DAMAGE = uuid("forbidden_seal_damage");
    private static final UUID U_SEAL_HEALTH = uuid("forbidden_seal_health");
    private static final UUID U_SEAL_SPEED = uuid("forbidden_seal_speed");
    private static final UUID U_SEAL_ATK_SPEED = uuid("forbidden_seal_attack_speed");
    private static final UUID U_CUB_EXCITE = uuid("forbidden_cub_excitement");
    private static final UUID U_CUB_HASTE = uuid("forbidden_cub_haste");
    private static final UUID U_CUB_SLOW = uuid("forbidden_cub_slow");
    private static final UUID U_CUB_DMG_SELF = uuid("forbidden_cub_damage_self");
    private static final UUID U_CUB_DMG_TARGET = uuid("forbidden_cub_damage_target");
    private static final UUID U_RING_ATK_SPEED = uuid("forbidden_ring_attack_speed");
    private static final UUID U_EAT_SPEED = uuid("forbidden_eat_speed");
    private static final UUID U_SET_CRIT_CHANCE = uuid("forbidden_set_crit_chance");
    private static final UUID U_SET_CRIT_DAMAGE = uuid("forbidden_set_crit_damage");

    /** 上一击缓存：攻击方 → (最终伤害, 命中 tick)。仅内存，重登/换维度清空（D119） */
    private static final Map<Player, CachedHit> HIT_CACHE = new WeakHashMap<>();
    /** 连打待结算队列：攻击方 → 待复刻的击打（延迟 1 tick，D63） */
    private static final Map<Player, List<PendingStrike>> PENDING = new WeakHashMap<>();
    /** 连打复刻中的攻击方：复刻伤害不再触发任何概率（D107） */
    private static final Map<Player, Boolean> REPLAYING = new WeakHashMap<>();
    /** 幼崽之心效果结算节流：攻击方 → 上次结算 tick */
    private static final Map<Player, Integer> CUB_LAST_TICK = new WeakHashMap<>();

    private record CachedHit(float damage, int tick) {
    }

    private record PendingStrike(LivingEntity target, float damage, int fireTick) {
    }

    private AkaishiSocketEffectHandler() {
        installHooks();
    }

    /** D257：平台侧实现注入 common 抽象接口，缺此步套装 +1 级与扭曲屏蔽恒为中性值 */
    private static void installHooks() {
        ForbiddenSetHooks.setUnnameableBonus(wearer ->
                wearer instanceof Player player
                        && ModConfig.setUnnameableLevelBonus != 0
                        && AkaishiForbiddenCurios.isFullSet(player)
                        ? ModConfig.setUnnameableLevelBonus : 0);
        ForbiddenSetHooks.setDistortionSuppressor(player ->
                ModConfig.setSuppressDistortion && AkaishiForbiddenCurios.isFullSet(player));
        // 锁槽：戴任意一件即锁死 9 槽，剩任一件仍锁、全摘下才解锁（D16/D115）
        ForbiddenSetHooks.setSocketLockQuery(player -> AkaishiForbiddenCurios.countWorn(player) > 0);
        // 躯体检查仪饰品区（D96）：固定四件顺序，未佩戴以空占位，GUI 端据此灰化
        ForbiddenSetHooks.setCurioStateProvider(player -> {
            Item[] pieces = {ModItems.lifeTouch.get(), ModItems.cubHeart.get(),
                    ModItems.motherSeal.get(), ModItems.fertilityRing.get()};
            List<ForbiddenSetHooks.CurioState> states = new ArrayList<>(pieces.length);
            for (Item piece : pieces) {
                ItemStack stack = AkaishiForbiddenCurios.find(player, piece).orElse(ItemStack.EMPTY);
                states.add(new ForbiddenSetHooks.CurioState(!stack.isEmpty(),
                        AkaishiSocketCurioItem.getErosion(stack)));
            }
            return states;
        });
    }

    // ==================== 战斗：攻防双方 ====================

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        Entity attackerEntity = event.getSource().getEntity();
        // 攻击方：生命之触 / 幼崽之心（复刻伤害期间跳过，避免二次掷点）
        if (attackerEntity instanceof Player attacker && REPLAYING.get(attacker) == null) {
            applyAttackerEffects(event, attacker, target);
        }
        // 受害方：母神之印四类减伤 → 套装再减 → 孕育之环回血
        if (target instanceof Player victim) {
            applyVictimEffects(event, victim);
        }
        // 命中缓存取「最终伤害」（连打复刻基准），并在结算后累计套装有效攻击
        if (attackerEntity instanceof Player attacker && event.getAmount() > 0.0F) {
            HIT_CACHE.put(attacker, new CachedHit(event.getAmount(), attacker.tickCount));
            if (REPLAYING.get(attacker) == null) {
                countSetAttack(attacker);
            }
        }
    }

    private static void applyAttackerEffects(LivingHurtEvent event, Player attacker, LivingEntity target) {
        if (ModConfig.lifeTouchEnabled && AkaishiForbiddenCurios.hasLifeTouch(attacker)) {
            applyLifeTouchOnHit(attacker, target);
        }
        if (ModConfig.cubHeartEnabled && AkaishiForbiddenCurios.hasCubHeart(attacker)) {
            applyCubHeartOnHit(event, attacker, target);
        }
    }

    /** 生命之触命中：连打掷点 + 代价（真伤/扣饱食） + 收益（回饱食） */
    private static void applyLifeTouchOnHit(Player attacker, LivingEntity target) {
        CachedHit last = HIT_CACHE.get(attacker);
        if (last != null && attacker.tickCount - last.tick() <= ModConfig.lifeTouchHitCacheTicks
                && attacker.getRandom().nextDouble() < ModConfig.lifeTouchDoubleStrikeChance) {
            PENDING.computeIfAbsent(attacker, p -> new ArrayList<>(1))
                    .add(new PendingStrike(target, last.damage(), attacker.tickCount + 1));
        }
        // 真伤走 magic：无视护甲/附魔，且无施害实体，不会递归触发本 Handler
        if (attacker.getRandom().nextDouble() < ModConfig.lifeTouchSelfHurtChance) {
            attacker.hurt(attacker.damageSources().magic(), (float) ModConfig.lifeTouchSelfHurtDamage);
        }
        if (attacker.getRandom().nextDouble() < ModConfig.lifeTouchHungerCostChance) {
            setHunger(attacker, -ModConfig.lifeTouchHungerCostAmount);
        }
        if (attacker.getRandom().nextDouble() < ModConfig.lifeTouchHungerRestoreChance) {
            setHunger(attacker, ModConfig.lifeTouchHungerRestoreAmount);
        }
    }

    /** 幼崽之心命中：黄心累加（每次命中） + 效果掷点（按 interval 节流） */
    private static void applyCubHeartOnHit(LivingHurtEvent event, Player attacker, LivingEntity target) {
        float gained = (float) (event.getAmount() * ModConfig.cubHeartAbsorptionRatio);
        if (gained > 0.0F) {
            attacker.setAbsorptionAmount(attacker.getAbsorptionAmount() + gained);
        }
        int last = CUB_LAST_TICK.getOrDefault(attacker, Integer.MIN_VALUE);
        if (attacker.tickCount - last < ModConfig.cubHeartEffectInterval) {
            return;
        }
        CUB_LAST_TICK.put(attacker, attacker.tickCount);
        long now = attacker.tickCount;
        // 亢奋：攻速 +20% / 5 秒（每次结算刷新，D44）
        AkaishiForbiddenAttrs.timed(attacker, U_CUB_EXCITE, Attributes.ATTACK_SPEED,
                ModConfig.cubHeartExcitementAttackSpeed, AttributeModifier.Operation.MULTIPLY_BASE,
                now + ModConfig.cubHeartExcitementTicks);
        // 减速对方 / 加速自身（±10%，刷新时长不叠加数值，D88）
        if (attacker.getRandom().nextDouble() < ModConfig.cubHeartSlowHasteChance) {
            double amp = ModConfig.cubHeartSlowHasteAmplitude;
            long expire = now + ModConfig.cubHeartSlowHasteTicks;
            AkaishiForbiddenAttrs.timed(attacker, U_CUB_HASTE, Attributes.MOVEMENT_SPEED, amp,
                    AttributeModifier.Operation.MULTIPLY_BASE, expire);
            AkaishiForbiddenAttrs.timed(target, U_CUB_SLOW, Attributes.MOVEMENT_SPEED, -amp,
                    AttributeModifier.Operation.MULTIPLY_BASE, expire);
        }
        // 自身加伤 / 对方减伤（±1 点，刷新不叠加）
        if (attacker.getRandom().nextDouble() < ModConfig.cubHeartDamageShiftChance) {
            double shift = ModConfig.cubHeartDamageShiftAmount;
            long expire = now + ModConfig.cubHeartDamageShiftTicks;
            AkaishiForbiddenAttrs.timed(attacker, U_CUB_DMG_SELF, Attributes.ATTACK_DAMAGE, shift,
                    AttributeModifier.Operation.ADDITION, expire);
            AkaishiForbiddenAttrs.timed(target, U_CUB_DMG_TARGET, Attributes.ATTACK_DAMAGE, -shift,
                    AttributeModifier.Operation.ADDITION, expire);
        }
        // 自施不可名状（所有入口统一叠加套装等级加成，D73/D191）
        if (attacker.getRandom().nextDouble() < ModConfig.cubHeartUnnameableChance) {
            attacker.addEffect(new MobEffectInstance(ModEffects.UNNAMEABLE.get(),
                    ModConfig.cubHeartUnnameableTicks,
                    ModConfig.cubHeartUnnameableAmplifier + ForbiddenSetHooks.unnameableBonus(attacker),
                    true, false, true));
        }
    }

    /** 受害方：四类减半 → 套装叠乘 → 饥饿致死倍率 → 孕育之环回血 */
    private static void applyVictimEffects(LivingHurtEvent event, Player victim) {
        float amount = event.getAmount();
        boolean seal = ModConfig.motherSealEnabled && AkaishiForbiddenCurios.hasMotherSeal(victim);
        if (seal && isResistedSource(event, victim)) {
            amount *= (float) ModConfig.motherSealResistFactor;
        }
        if (ModConfig.setDamageReduction > 0.0 && AkaishiForbiddenCurios.isFullSet(victim)) {
            amount *= (float) (1.0 - ModConfig.setDamageReduction); // 0.5 × 0.9 = 45%（D30）
        }
        if (event.getSource().is(DamageTypes.STARVE) && ModConfig.fertilityRingEnabled
                && AkaishiForbiddenCurios.hasFertilityRing(victim)) {
            amount *= (float) ModConfig.fertilityRingStarveMultiplier;
        }
        event.setAmount(Math.max(0.0F, amount));
        // 孕育之环：受伤 1/5 回 2 点 + 扣 1 格饱食 + 攻速 +20%/5 秒（D45/D169）
        if (ModConfig.fertilityRingEnabled && AkaishiForbiddenCurios.hasFertilityRing(victim)
                && victim.getRandom().nextDouble() < ModConfig.fertilityRingHealChance) {
            victim.heal((float) ModConfig.fertilityRingHealAmount);
            setHunger(victim, -ModConfig.fertilityRingHealHungerCost);
            AkaishiForbiddenAttrs.timed(victim, U_RING_ATK_SPEED, Attributes.ATTACK_SPEED,
                    ModConfig.fertilityRingAttackSpeedBonus, AttributeModifier.Operation.MULTIPLY_BASE,
                    victim.tickCount + ModConfig.fertilityRingAttackSpeedTicks);
        }
    }

    /**
     * 四类减伤口径：火（宽口径，含岩浆/烫脚）/ 爆炸 / 毒 / 凋零。
     * 1.20.1 未提供毒与凋零的伤害类型标签，二者均以 magic 结算，故以「魔法伤害 + 正中毒/凋零」判定。
     */
    private static boolean isResistedSource(LivingHurtEvent event, Player victim) {
        if (event.getSource().is(DamageTypeTags.IS_FIRE) || event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            return true;
        }
        return event.getSource().is(DamageTypes.MAGIC)
                && (victim.hasEffect(MobEffects.POISON) || victim.hasEffect(MobEffects.WITHER));
    }

    /** 套装有效攻击计数（D35：命中且最终伤害 &gt; 0；满 10 回 1 格饱食并归零，D85） */
    private static void countSetAttack(Player attacker) {
        if (ModConfig.setAttackCountRequired <= 0 || !AkaishiForbiddenCurios.isFullSet(attacker)) {
            return;
        }
        CompoundTag data = attacker.getPersistentData();
        int count = data.getInt(TAG_SET_ATTACKS) + 1;
        if (count >= ModConfig.setAttackCountRequired) {
            count = 0;
            setHunger(attacker, ModConfig.setAttackCountHungerRestore);
        }
        data.putInt(TAG_SET_ATTACKS, count);
    }

    // ==================== 濒死免死 ====================

    /**
     * 套装：濒死免死。{@link LivingDamageEvent} 的 amount 已扣除黄心（吸收）且尚未写入血量，
     * 故「本次伤害 ≥ 当前生命」即为致死判定；命中时把伤害清零（免死）、回血并施加不可名状。
     * 冷却写 {@code PersistentData} 的存档游戏刻，换维度/重登均不被刷新。
     */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0F || amount < player.getHealth() || player.isDeadOrDying()
                || !AkaishiForbiddenCurios.isFullSet(player)) {
            return;
        }
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        if (now < data.getLong(TAG_NEAR_DEATH_READY_AT)) {
            return;
        }
        data.putLong(TAG_NEAR_DEATH_READY_AT, now + ModConfig.setNearDeathCooldownTicks);
        event.setAmount(0.0F);
        player.heal((float) (player.getMaxHealth() * ModConfig.setNearDeathHealPercent));
        player.addEffect(new MobEffectInstance(ModEffects.UNNAMEABLE.get(),
                ModConfig.setNearDeathUnnameableTicks,
                ForbiddenSetHooks.unnameableBonus(player), true, false, true));
    }

    // ==================== 常驻 tick ====================

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (player.isDeadOrDying()) {
            return;
        }
        tickPendingStrikes(player);
        pruneHitCache(player);
        applyLifeTouchReach(player);
        applyMotherSealBonuses(player);
        applyMotherSealSelfAffliction(player);
        applySetUnnameableCrit(player);
        applyEatingSpeedCompensation(player);
        applyLethalStarve(player);
    }

    /** 限时属性回收：非玩家目标（被减速/减伤的怪）身上的修饰符也须到期，故挂在全体生物 tick 上 */
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        AkaishiForbiddenAttrs.tickTimed(entity, entity.tickCount);
    }

    /** 连打复刻：延迟 1 tick 按上一击最终伤害补刀，不触发任何概率、不耗饥饿、不计套装（D63/D107） */
    private static void tickPendingStrikes(Player player) {
        List<PendingStrike> list = PENDING.get(player);
        if (list == null || list.isEmpty()) {
            return;
        }
        Iterator<PendingStrike> it = list.iterator();
        while (it.hasNext()) {
            PendingStrike strike = it.next();
            if (strike.fireTick() > player.tickCount) {
                continue;
            }
            it.remove();
            LivingEntity target = strike.target();
            if (!target.isAlive() || target.level() != player.level()) {
                continue;
            }
            target.invulnerableTime = 0; // 复刻补刀须绕开受击无敌帧，否则延迟 1 tick 必被吞
            REPLAYING.put(player, Boolean.TRUE);
            try {
                target.hurt(player.damageSources().playerAttack(player), strike.damage());
            } finally {
                REPLAYING.remove(player);
            }
        }
        if (list.isEmpty()) {
            PENDING.remove(player);
        }
    }

    private static void pruneHitCache(Player player) {
        CachedHit hit = HIT_CACHE.get(player);
        if (hit != null && player.tickCount - hit.tick() > ModConfig.lifeTouchHitCacheTicks) {
            HIT_CACHE.remove(player);
        }
    }

    /** 生命之触：实体与方块交互距离各 +2 格（D33；走 Forge 专属 REACH 属性，与长臂同为 ADDITION 可叠加） */
    private static void applyLifeTouchReach(Player player) {
        double bonus = ModConfig.lifeTouchEnabled && AkaishiForbiddenCurios.hasLifeTouch(player)
                ? ModConfig.lifeTouchReachBonus : 0.0;
        applyReach(player, ForgeMod.ENTITY_REACH.get(), U_REACH_ENTITY, bonus);
        applyReach(player, ForgeMod.BLOCK_REACH.get(), U_REACH_BLOCK, bonus);
    }

    private static void applyReach(Player player, Attribute attribute, UUID uuid, double bonus) {
        AkaishiForbiddenAttrs.cache(player, uuid, attribute, bonus, AttributeModifier.Operation.ADDITION);
    }

    /** 母神之印：持「不可名状」时按等级给 伤害/生命/移速/攻速 增益（I×1.0 / II×1.5 / III+×2.0，D46） */
    private static void applyMotherSealBonuses(Player player) {
        double factor = 0.0;
        if (ModConfig.motherSealEnabled && AkaishiForbiddenCurios.hasMotherSeal(player)) {
            MobEffectInstance unnameable = player.getEffect(ModEffects.UNNAMEABLE.get());
            if (unnameable != null) {
                factor = switch (Math.min(unnameable.getAmplifier(), 2)) {
                    case 0 -> 1.0;
                    case 1 -> 1.5;
                    default -> 2.0;
                };
            }
        }
        AkaishiForbiddenAttrs.cache(player, U_SEAL_DAMAGE, Attributes.ATTACK_DAMAGE,
                ModConfig.motherSealDamageBonus * factor, AttributeModifier.Operation.ADDITION);
        AkaishiForbiddenAttrs.cache(player, U_SEAL_HEALTH, Attributes.MAX_HEALTH,
                ModConfig.motherSealHealthBonus * factor, AttributeModifier.Operation.ADDITION);
        AkaishiForbiddenAttrs.cache(player, U_SEAL_SPEED, Attributes.MOVEMENT_SPEED,
                ModConfig.motherSealSpeedBonus * factor, AttributeModifier.Operation.MULTIPLY_BASE);
        AkaishiForbiddenAttrs.cache(player, U_SEAL_ATK_SPEED, Attributes.ATTACK_SPEED,
                ModConfig.motherSealAttackSpeedBonus * factor, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    /** 母神之印自施：每 60 秒 II 级不可名状 10 秒（含套装等级加成，D69/D191） */
    private static void applyMotherSealSelfAffliction(Player player) {
        int period = ModConfig.motherSealSelfUnnameablePeriod;
        if (!ModConfig.motherSealEnabled || period <= 0 || player.tickCount % period != 0
                || !AkaishiForbiddenCurios.hasMotherSeal(player)) {
            return;
        }
        player.addEffect(new MobEffectInstance(ModEffects.UNNAMEABLE.get(),
                ModConfig.motherSealSelfUnnameableTicks,
                ModConfig.motherSealSelfUnnameableAmplifier + ForbiddenSetHooks.unnameableBonus(player),
                true, false, true));
    }

    /**
     * 套装：持「不可名状」时的暴击增益（D260）。四件集齐且身上有不可名状才挂载，
     * 走既有暴击属性（{@link ModCombatAttributes}），由战斗层统一掷点/结算，此处不重复判定。
     */
    private static void applySetUnnameableCrit(Player player) {
        boolean active = AkaishiForbiddenCurios.isFullSet(player)
                && player.hasEffect(ModEffects.UNNAMEABLE.get());
        applyCritBonus(player, ModCombatAttributes.CRIT_CHANCE, U_SET_CRIT_CHANCE,
                active ? ModConfig.setUnnameableCritChance : 0.0);
        applyCritBonus(player, ModCombatAttributes.CRIT_DAMAGE, U_SET_CRIT_DAMAGE,
                active ? ModConfig.setUnnameableCritDamage : 0.0);
    }

    /** 暴击属性延迟注册：属性表未就绪时（supplier 为空）静默跳过，避免启动期 NPE */
    private static void applyCritBonus(Player player, RegistrySupplier<Attribute> supplier, UUID uuid, double amount) {
        Attribute attribute = supplier != null ? supplier.get() : null;
        if (attribute != null) {
            AkaishiForbiddenAttrs.cache(player, uuid, attribute, amount, AttributeModifier.Operation.ADDITION);
        }
    }

    /** 孕育之环：吃肉类期间以 +0.2 ADDITION 精确抵消原版「使用物品」移速 −20%（D84） */
    private static void applyEatingSpeedCompensation(Player player) {
        boolean compensate = ModConfig.fertilityRingEnabled
                && AkaishiForbiddenCurios.hasFertilityRing(player)
                && player.isUsingItem() && isMeat(player.getUseItem(), player);
        AkaishiForbiddenAttrs.cache(player, U_EAT_SPEED, Attributes.MOVEMENT_SPEED,
                compensate ? 0.2 : 0.0, AttributeModifier.Operation.ADDITION);
    }

    /**
     * 孕育之环：饥饿致死（D67/D230）。原版在非困难难度按血量门槛挡住饥饿伤害，
     * 此处仅在原版「判定不会致伤」的区间补刀，避免与困难的同源伤害叠加成双倍。
     */
    private static void applyLethalStarve(Player player) {
        Difficulty difficulty = player.level().getDifficulty();
        if (!ModConfig.fertilityRingStarveLethal || !ModConfig.fertilityRingEnabled
                || difficulty == Difficulty.HARD
                || !AkaishiForbiddenCurios.hasFertilityRing(player)
                || player.getFoodData().getFoodLevel() > 0
                || player.tickCount % 80 != 0) {
            return;
        }
        float gate = difficulty == Difficulty.NORMAL ? 1.0F : 10.0F;
        if (player.getHealth() > gate) {
            return;
        }
        player.hurt(player.damageSources().starve(), (float) ModConfig.fertilityRingStarveMultiplier);
    }

    // ==================== 进食 ====================

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || !ModConfig.fertilityRingEnabled
                || !ModConfig.fertilityRingSatiatedMeatOnly || !AkaishiForbiddenCurios.hasFertilityRing(player)) {
            return;
        }
        FoodProperties food = event.getItem().getFoodProperties(player);
        // 满饱食时仅肉类仍可进食（金苹果等可永久食用类不受限，D66）
        if (food != null && !food.isMeat() && !food.canAlwaysEat()
                && player.getFoodData().getFoodLevel() >= 20) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onItemEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        FoodProperties food = event.getItem().getFoodProperties(player);
        if (food == null) {
            return;
        }
        // 母神之印：素食不可食——营养照常结算，但倒扣 1 格饱食并获得反胃（D65）
        if (ModConfig.motherSealEnabled && AkaishiForbiddenCurios.hasMotherSeal(player) && !food.isMeat()) {
            setHunger(player, -ModConfig.motherSealVegetarianHungerCost);
            if (ModConfig.motherSealVegetarianNauseaTicks > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,
                        ModConfig.motherSealVegetarianNauseaTicks, 0, false, false));
            }
        }
        // 孕育之环：吃肉回 3 点生命（D45）
        if (ModConfig.fertilityRingEnabled && AkaishiForbiddenCurios.hasFertilityRing(player) && food.isMeat()) {
            player.heal((float) ModConfig.fertilityRingMeatHealAmount);
        }
    }

    // ==================== 会话边界 ====================

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        clearTransient(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        clearTransient(event.getEntity());
    }

    /** 重登 / 换维度清空上一击缓存与待结算连打（D119：不写 NBT） */
    private static void clearTransient(Player player) {
        HIT_CACHE.remove(player);
        PENDING.remove(player);
        REPLAYING.remove(player);
        CUB_LAST_TICK.remove(player);
    }

    // ==================== 工具 ====================

    /** 增减饱食度：正面加、负面减，钳制在 [0, 20]（原版单次进食上限即为 20） */
    private static void setHunger(Player player, int delta) {
        if (delta == 0) {
            return;
        }
        FoodData food = player.getFoodData();
        food.setFoodLevel(Math.max(0, Math.min(20, food.getFoodLevel() + delta)));
    }

    private static boolean isMeat(ItemStack stack, LivingEntity entity) {
        FoodProperties food = stack.getFoodProperties(entity);
        return food != null && food.isMeat();
    }

    private static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(("akaishi:" + name).getBytes(StandardCharsets.UTF_8));
    }
}
