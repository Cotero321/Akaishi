package com.example.akaishi.forge.life;

import com.example.akaishi.life.body.BodyGeneHelper;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.body.PlayerBodyState;
import com.example.akaishi.life.linkage.OrganLinkage;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.OrganEffectResolver;
import com.example.akaishi.life.organ.OrganPassive;
import com.example.akaishi.life.organ.OrganSpecial;
import com.example.akaishi.life.organ.OrganTemplate;
import com.example.akaishi.life.organ.QualityTier;
import com.example.akaishi.life.sample.SampleGroup;
import com.example.akaishi.item.AkaishiLifeFusionSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 器官被动效果处理器（Forge 服务端 tick + 进食交互）：
 * - 属性重建：生命上限按百分比公式 20×(1+Σ加成%×倍率×适配度−Σ空槽权重%) 聚合，
 *   其余属性 = 基础值 × 品质倍率 × 适配度，摘要缓存避免每 tick 重挂
 * - 被动技能：常驻药水 / 免疫 / 再生 / 敌意高亮 / 自动拾取
 * - 排斥增长：速率 × 100/适配度（低适配度排异更快，<60 重度再翻倍）
 * - 部位 debuff：适配度 <80 出现该部位负面效果（≥80 无 / 60-79 轻度 / <60 重度）
 * - 天敌冲突：排斥锁满 + 器官失效 + 周期性爆炸反噬，移除一侧自动解除
 * - 独特机制：牛胃（吃小麦 / 禁肉）、末影怕水（接触水源瞬移掉血）
 * 所有效果数据来自 common 的 OrganEffectResolver，本类仅做平台事件桥接。
 */
public final class AkaishiBodyPassiveHandler {

    public static final AkaishiBodyPassiveHandler INSTANCE = new AkaishiBodyPassiveHandler();

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADDITION;
    /** 玩家基础生命值（生命百分比公式的基准） */
    private static final double BASE_HEALTH = 20.0;
    /** MAX_HEALTH 聚合修饰符的固定键名 */
    private static final String HEALTH_KEY = "HEALTH";
    /** 属性摘要缓存：玩家 → 上次摘要（无变化不重建，避免每 tick 重挂修饰符） */
    private static final Map<Player, String> ATTRIBUTE_DIGEST = new WeakHashMap<>();
    /** 已挂载修饰符记录：玩家 → 修饰符键 + 属性（重建时先精确移除旧的） */
    private static final Map<Player, List<AppliedAttr>> APPLIED = new WeakHashMap<>();
    /** 怕水瞬移冷却记录 */
    private static final Map<Player, Integer> WATER_FEAR_COOLDOWN = new WeakHashMap<>();
    /** 天敌冲突锁定记录：用于冲突解除时重置排斥 */
    private static final Map<Player, Set<BodySlot>> CONFLICT_LOCKED = new WeakHashMap<>();

    /** 排斥负面效果触发阈值 */
    private static final int REJECTION_WARNING = 60;
    /** 排斥中毒阈值 */
    private static final int REJECTION_POISON = 80;
    /** 适配度 ≥80 无部位 debuff */
    private static final int COMPAT_CLEAN = 80;
    /** 适配度 <60 部位 debuff 重度（且排斥速率 ×2） */
    private static final int COMPAT_SEVERE = 60;
    /** 天敌反噬间隔（tick） */
    private static final int CONFLICT_PUNISH_INTERVAL = 100;
    /** 排斥增长间隔下限（tick = 15s/点）：限制低适配/强基因的加速惩罚，避免顶级器官过快报废 */
    private static final int GROWTH_INTERVAL_MIN_TICKS = 300;
    /** 天敌反噬每次对玩家自身造成的伤害（爆炸伤害源，吃护甲减伤） */
    private static final float CONFLICT_PUNISH_DAMAGE = 5.0F;
    /** 长臂被动：每臂近战攻击距离加成（格） */
    private static final double REACH_PER_ARM = 0.75;
    /** 攻击距离修饰符固定 UUID（akaishi:long_reach） */
    private static final UUID REACH_UUID = UUID.nameUUIDFromBytes(
            "akaishi:long_reach".getBytes(StandardCharsets.UTF_8));
    /** 已挂攻击距离加成记录：玩家 → 当前加成（变化时才重建修饰符） */
    private static final Map<Player, Double> REACH_CACHE = new WeakHashMap<>();
    /** 生命融合套装飞行状态缓存：玩家 → 是否已授予飞行（变化时才同步，避免每 tick 刷包） */
    private static final Map<Player, Boolean> FLIGHT_CACHE = new WeakHashMap<>();

    private record AppliedAttr(String key, Attribute attribute) {
    }

    private AkaishiBodyPassiveHandler() {
    }

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
        // 突破激活到期（30 分钟计时到）：结束并提示（actionbar，与药剂/管理器提示一致），属性随后自动重建回落
        if (state.tickBreakthrough(player.level().getGameTime())) {
            player.displayClientMessage(Component.translatable("message.akaishi.potion.breakthrough_end"), true);
        }
        rebuildAttributes(player, state);
        applyLifeFusionSet(player);
        applyPassives(player, state);
        applySynergy(player, state);
        tickRejection(player, state);
        tickConflict(player, state);
        applyOverload(player, state);
        applySlotDebuffs(player, state);
        tickSpecial(player, state);
    }

    /** 进食增强（鸡砂囊·食物恢复）：食物饥饿与饱和 +25% */
    @SubscribeEvent
    public void onItemEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null || !OrganEffectResolver.hasPassive(state, OrganPassive.FOOD_BOOST)) {
            return;
        }
        ItemStack stack = event.getItem();
        if (!stack.isEdible()) {
            return;
        }
        FoodProperties food = stack.getFoodProperties(player);
        if (food == null) {
            return;
        }
        // 补 25% 饥饿值并按原食物饱和模数结算（饱和度系统自带封顶）
        int bonus = Math.max(1, (int) Math.ceil(food.getNutrition() * 0.25F));
        player.getFoodData().eat(bonus, food.getSaturationModifier());
    }

    // ===== 属性重建 =====

    /** 移植/摘除/适配度/排斥变化时重建器官属性修饰（摘要相同直接跳过） */
    private static void rebuildAttributes(Player player, IPlayerBodyState state) {
        String digest = digestOf(state, player);
        String last = ATTRIBUTE_DIGEST.get(player);
        if (digest.equals(last)) {
            return;
        }
        // 移除旧修饰
        for (AppliedAttr prev : APPLIED.getOrDefault(player, List.of())) {
            AttributeInstance inst = player.getAttribute(prev.attribute());
            if (inst != null) {
                inst.removeModifier(uuidOf(prev.key(), prev.attribute()));
            }
        }
        List<AppliedAttr> next = new ArrayList<>();
        // 1) 空槽惩罚：每个空槽按部位权重扣减（心脏 20%，其余 10%）
        double healthPct = 0.0;
        for (BodySlot slot : BodySlot.values()) {
            if (state.getOrgan(slot).isEmpty()) {
                healthPct -= slot.getLifeWeight();
            }
        }
        // 2) 生效器官：MAX_HEALTH 按百分比聚合，其余属性按基础值 × 倍率 × 适配度（突破激活时正数基础值
        //    再 ×(1+pct%)，负数基础值词条暂时失效）——适配度含基因加成与突破额外适配，可临时超 100
        // 生命融合套装加成：全基因适配（每件 +2）与器官强度倍率（全套 ×1.2）
        int gearCompat = AkaishiLifeFusionSet.geneCompatBonus(player);
        double strengthMult = AkaishiLifeFusionSet.isFullSet(player)
                ? AkaishiLifeFusionSet.ORGAN_STRENGTH_MULTIPLIER : 1.0;
        for (OrganEffectResolver.ActiveOrgan organ : OrganEffectResolver.collect(state)) {
            double compatFactor = BodyGeneHelper.effectiveCompat(state, organ.stack(), gearCompat) / 100.0;
            // 突破激活且来源匹配：正数基础值乘 (1+pct/100)，负数基础值词条期间跳过
            boolean btActive = state.isBreakthroughActive(AkaishiOrganItem.getEntityId(organ.stack()));
            double breakthroughFactor = btActive ? 1.0 + state.getBreakthroughPct() / 100.0 : 1.0;
            // 按属性先聚合再挂载：模板词条与突变词条可能给同一槽位提供同一属性，
            // 逐个 addTransientModifier 会因 uuid（槽位:属性）相同抛 "Modifier is already applied" → 求和后只挂一个
            Map<Attribute, Double> perAttr = new HashMap<>();
            for (OrganTemplate.AttributeBonus b : OrganEffectResolver.bonusesOf(organ.stack(), organ.slot(), organ.effect())) {
                double base = b.base();
                if (btActive && base < 0) {
                    continue; // 负基础值词条：突破激活期内暂时失效
                }
                double value = base * breakthroughFactor * organ.tier().getMultiplier() * compatFactor * strengthMult;
                if (b.attribute() == Attributes.MAX_HEALTH) {
                    // 生命加成按基础生命百分比计算：value/20 即 +value×5%
                    healthPct += value / BASE_HEALTH;
                    continue;
                }
                perAttr.merge(b.attribute(), value, Double::sum);
            }
            for (Map.Entry<Attribute, Double> e : perAttr.entrySet()) {
                if (e.getValue() == 0.0) {
                    continue;
                }
                AttributeInstance inst = player.getAttribute(e.getKey());
                if (inst == null) {
                    continue;
                }
                UUID uuid = uuidOf(organ.slot().name(), e.getKey());
                // 防御：异常残留或外部占用同 UUID 时先清后挂，杜绝 "Modifier is already applied"
                if (inst.getModifier(uuid) != null) {
                    inst.removeModifier(uuid);
                }
                inst.addTransientModifier(new AttributeModifier(uuid, "Akaishi organ", e.getValue(), ADD));
                next.add(new AppliedAttr(organ.slot().name(), e.getKey()));
            }
        }
        // 生命融合套装·BOSS/龙肢体：移植 BOSS 或龙族来源器官时额外 +10 最大生命
        if (AkaishiLifeFusionSet.isFullSet(player) && AkaishiLifeFusionSet.hasBossOrDragonOrgan(player, state)) {
            healthPct += AkaishiLifeFusionSet.BOSS_DRAGON_HEALTH_BONUS / BASE_HEALTH;
        }
        // 3) 生命上限 = 20 × (1 + 加成% − 空槽权重%)
        double healthDelta = BASE_HEALTH * (1.0 + healthPct) - BASE_HEALTH;
        if (Math.abs(healthDelta) > 0.001) {
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                UUID healthUuid = uuidOf(HEALTH_KEY, Attributes.MAX_HEALTH);
                if (health.getModifier(healthUuid) != null) {
                    health.removeModifier(healthUuid);
                }
                health.addTransientModifier(new AttributeModifier(healthUuid, "Akaishi organ health", healthDelta, ADD));
                next.add(new AppliedAttr(HEALTH_KEY, Attributes.MAX_HEALTH));
            }
        }
        APPLIED.put(player, next);
        // 全部挂载成功后统一提交摘要：中途异常时摘要不更新 → 下一 tick 自动重试自愈（契合单异常不崩溃规范）
        ATTRIBUTE_DIGEST.put(player, digest);
    }

    /** 键 + 属性 → 稳定 UUID（移植/摘除后精确移除同一修饰符） */
    private static UUID uuidOf(String key, Attribute attribute) {
        return UUID.nameUUIDFromBytes((key + ":" + attribute.getDescriptionId()).getBytes(StandardCharsets.UTF_8));
    }

    /** 摘要：各槽位器官来源/品质/适配度 + 排斥值 + 空槽 + 基因强化 + 突破激活 + 生命融合装备（变化才触发重建） */
    private static String digestOf(IPlayerBodyState state, Player player) {
        StringBuilder sb = new StringBuilder();
        // 基因强化（吸收/卸载后立即触发属性重建）
        sb.append(state.getGeneBonuses()).append(';');
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            sb.append(slot).append('|');
            if (organ.getItem() instanceof AkaishiOrganItem) {
                if (AkaishiOrganItem.isNative(organ)) {
                    sb.append("native");
                } else {
                    sb.append(AkaishiOrganItem.getEntityId(organ)).append(':')
                            .append(AkaishiOrganItem.getTier(organ)).append(':')
                            .append(AkaishiOrganItem.getCompat(organ))
                            // 突变词条参与摘要：突变变更需触发属性重建
                            .append(':').append(AkaishiOrganItem.getMutations(organ));
                }
            }
            sb.append(':').append(state.getRejection(slot)).append(';');
        }
        // 突破激活（激活开始/结束/数值变化时触发属性重建，属性随激活生效/回落）
        sb.append("BT").append(state.getBreakthroughEntity()).append(':')
                .append(state.getBreakthroughExtra()).append(':')
                .append(state.getBreakthroughPct()).append(':')
                .append(state.getBreakthroughUntil()).append(';');
        // 生命融合装备穿戴件数（+2 全基因适配 / 全套器官强度与 +10 生命随穿脱即时触发重建）
        sb.append("LF").append(AkaishiLifeFusionSet.countWorn(player)).append(';');
        return sb.toString();
    }

    // ===== 被动技能（常驻/被动触发） =====

    private static void applyPassives(Player player, IPlayerBodyState state) {
        // 长臂被动走 Forge 专属属性（ENTITY_REACH），无条件同步以防摘除后残留
        syncReach(player, state);
        for (OrganEffectResolver.ActiveOrgan organ : OrganEffectResolver.collect(state)) {
            // 突变词条被动与生物被动统一生效（passivesOf 合并去重）
            for (OrganPassive passive : OrganEffectResolver.passivesOf(organ.stack(), organ.effect())) {
                applyPassive(player, state, passive);
            }
        }
    }

    // ===== 生态套装 =====

    /** 生态套装·纯温血：小共鸣再生 I / 大共鸣再生 II（纯种构筑奖励） */
    private static void applySynergy(Player player, IPlayerBodyState state) {
        OrganEffectResolver.Synergy synergy = OrganEffectResolver.synergyOf(state, player.level());
        if (synergy.group() == SampleGroup.WARM_BLOODED) {
            applyPotion(player, MobEffects.REGENERATION, synergy.isMajor() ? 1 : 0);
        }
    }

    /**
     * 生命融合套装飞行：穿齐 4 件授予无消耗飞行（mayfly，不扣经验/食物），
     * 脱下后若非创造/旁观则撤销飞行能力。状态变化才同步，避免每 tick 刷包。
     */
    private static void applyLifeFusionSet(Player player) {
        boolean full = AkaishiLifeFusionSet.isFullSet(player);
        Boolean last = FLIGHT_CACHE.get(player);
        if (last != null && last == full) {
            return;
        }
        FLIGHT_CACHE.put(player, full);
        if (full) {
            player.getAbilities().mayfly = true;
        } else if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    /**
     * 同步近战攻击距离：统计 LONG_REACH 数量 × 每臂加成，挂到 ForgeMod.ENTITY_REACH。
     * 数量无变化跳过；摘除全部后移除修饰符，避免属性残留。
     */
    private static void syncReach(Player player, IPlayerBodyState state) {
        int arms = 0;
        for (OrganEffectResolver.ActiveOrgan organ : OrganEffectResolver.collect(state)) {
            if (OrganEffectResolver.passivesOf(organ.stack(), organ.effect()).contains(OrganPassive.LONG_REACH)) {
                arms++;
            }
        }
        double amount = arms * REACH_PER_ARM;
        Double last = REACH_CACHE.get(player);
        if (last != null && Math.abs(last - amount) < 1e-9) {
            return;
        }
        AttributeInstance inst = player.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (inst != null) {
            if (last != null) {
                inst.removeModifier(REACH_UUID);
            }
            if (amount > 0.0) {
                inst.addTransientModifier(new AttributeModifier(REACH_UUID,
                        "Akaishi long reach", amount, AttributeModifier.Operation.ADDITION));
            }
        }
        REACH_CACHE.put(player, amount);
    }

    private static void applyPassive(Player player, IPlayerBodyState state, OrganPassive passive) {
        switch (passive) {
            case JUMP_BOOST -> applyPotion(player, MobEffects.JUMP, 1);
            case NIGHT_VISION -> applyPotion(player, MobEffects.NIGHT_VISION, 0);
            case WATER_BREATHING -> applyPotion(player, MobEffects.WATER_BREATHING, 0);
            case SWIM_BOOST -> applyPotion(player, MobEffects.DOLPHINS_GRACE, 0);
            case GLOW -> applyPotion(player, MobEffects.GLOWING, 0);
            case FALL_IMMUNE -> player.fallDistance = 0.0F;
            case SLOW_IMMUNE -> removeEffect(player, MobEffects.MOVEMENT_SLOWDOWN);
            case REGEN -> {
                if (player.tickCount % 40 == 0) {
                    player.heal(1.0F);
                }
            }
            case ENEMY_GLOW -> {
                if (player.tickCount % 20 == 0) {
                    glowNearbyHostiles(player);
                }
            }
            case AUTO_PICKUP -> {
                if (player.tickCount % 10 == 0) {
                    pickupNearbyItems(player);
                }
            }
            case GLIDE -> applyPotion(player, MobEffects.SLOW_FALLING, 0);
            case ANTIDOTE -> {
                if (player.tickCount % 100 == 0) {
                    removeEffect(player, MobEffects.POISON);
                }
            }
            case ANTIFREEZE -> {
                // 寒髓：免疫细雪冻结并清除缓慢（流浪者冰箭/细雪/冰冻）
                if (player.getTicksFrozen() > 0) {
                    player.setTicksFrozen(0);
                }
                removeEffect(player, MobEffects.MOVEMENT_SLOWDOWN);
            }
            case FIRE_IMMUNE -> {
                // 烈焰之心/末影龙之心：免疫火焰——每 tick 清除着火状态
                // （火焰/岩浆伤害本身由战斗处理器的受害方分支按来源拦截）
                if (player.isOnFire()) {
                    player.clearFire();
                }
            }
            default -> {
                // 战斗类被动（SLOW_ON_HIT/POISON_ON_HIT 等）由战斗处理器承接
            }
        }
    }

    /** 缺失时补充指定药水效果（2 秒刷新，脱器官自然消退） */
    private static void applyPotion(Player player, MobEffect effect, int amplifier) {
        if (!player.hasEffect(effect)) {
            player.addEffect(new MobEffectInstance(effect, 40, amplifier, false, false));
        }
    }

    private static void removeEffect(Player player, MobEffect effect) {
        if (player.hasEffect(effect)) {
            player.removeEffect(effect);
        }
    }

    /** 敌意侦测：高亮 24 格内以玩家为目标的怪物（每 20 tick 刷新标签） */
    private static void glowNearbyHostiles(Player player) {
        AABB box = player.getBoundingBox().inflate(24.0);
        for (Monster mob : player.level().getEntitiesOfClass(Monster.class, box, Monster::isAlive)) {
            if (mob.getTarget() == player) {
                mob.setGlowingTag(true);
            }
        }
    }

    /** 悦灵之心：自动拾取 5 格内掉落物（跳过刚掉落冷却，背包满则停止） */
    private static void pickupNearbyItems(Player player) {
        AABB box = player.getBoundingBox().inflate(5.0);
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            if (item.hasPickUpDelay()) {
                continue;
            }
            if (player.getInventory().add(item.getItem())) {
                item.discard();
            }
        }
    }

    // ===== 排斥增长 + 负面效果 =====

    private static void tickRejection(Player player, IPlayerBodyState state) {
        // 生命融合装备全基因适配加成（每件 +2，减缓排斥）
        int gearCompat = AkaishiLifeFusionSet.geneCompatBonus(player);
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            // 原生器官与身体完全契合，不产生排斥
            if (!(organ.getItem() instanceof AkaishiOrganItem) || AkaishiOrganItem.isNative(organ)) {
                continue;
            }
            QualityTier tier = AkaishiOrganItem.getTier(organ);
            if (tier == null) {
                continue;
            }
            // 排斥速率 × 100/有效适配度：适配度越低排斥涨得越快；<60 重度排异再翻倍
            // 有效适配度含身体基因加成与装备全基因适配（吸收基因药剂/穿戴生命融合装备可减缓排斥）
            int compat = BodyGeneHelper.effectiveCompat(state, organ, gearCompat);
            double factor = 100.0 / Math.max(1, compat);
            if (compat < COMPAT_SEVERE) {
                factor *= 2.0;
            }
            // 同源套装：同一来源 ≥2 枚已移植器官 → 排斥增速 -20%（身体"认可"这套基因）
            if (BodyGeneHelper.sameSourceCount(state, AkaishiOrganItem.getEntityId(organ)) >= 2) {
                factor *= 0.8;
            }
            // 基因强度联动：强基因（末影/龙）排斥增长更快（OrganLinkage.rejectionFactorOf）
            factor *= OrganLinkage.rejectionFactorOf(organ);
            // 生命融合套装：排斥增长速度 -25%
            if (AkaishiLifeFusionSet.isFullSet(player)) {
                factor *= AkaishiLifeFusionSet.REJECTION_SLOW_FACTOR;
            }
            // 增长间隔 ≥300t：即使极端低适配也不低于 15s/点，保留梯度同时封住爆炸增速
            int interval = (int) Math.max(GROWTH_INTERVAL_MIN_TICKS, tier.getGrowthIntervalSeconds() * 20.0 / factor);
            if (player.tickCount % interval == 0) {
                state.addRejection(slot, 1);
            }
        }
        // 高排斥负面：每 6 秒检查一次已移植槽位的排斥值
        if (player.tickCount % 120 != 0) {
            return;
        }
        for (BodySlot slot : BodySlot.values()) {
            int rejection = state.getRejection(slot);
            if (rejection < REJECTION_WARNING) {
                continue;
            }
            if (rejection >= REJECTION_POISON && player.getRandom().nextFloat() < 0.3F) {
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0, false, false));
            } else if (player.getRandom().nextFloat() < 0.4F) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, false));
            }
        }
    }

    // ===== 躯体超载（排斥预算） =====

    /** 全身总排斥 ≥ 此值：躯体超载 I（移动缓慢） */
    private static final int OVERLOAD_LIGHT = 320;
    /** 全身总排斥 ≥ 此值：躯体超载 II（移动缓慢 II + 虚弱） */
    private static final int OVERLOAD_HEAVY = 450;

    /** 躯体超载：排斥预算（全身总排斥）过高 → 施加持续负面（缓慢/虚弱），每 5 秒刷新一次 */
    private static void applyOverload(Player player, IPlayerBodyState state) {
        if (player.tickCount % 100 != 0) {
            return;
        }
        int total = 0;
        for (BodySlot slot : BodySlot.values()) {
            int rej = state.getRejection(slot);
            // 完全失效（=100）的器官已无收益，不再计入超载负担
            if (rej < PlayerBodyState.MAX_REJECTION) {
                total += rej;
            }
        }
        if (total >= OVERLOAD_HEAVY) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false));
        } else if (total >= OVERLOAD_LIGHT) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false));
        }
    }

    // ===== 部位 debuff（按适配度分级）=====

    /**
     * 移植器官适配度低于阈值时，该部位持续承受负面效果：
     * 眼→黑暗 / 心·肺→虚弱 / 内体→饥饿 / 肾→中毒 / 臂→挖掘疲劳 / 腿→缓慢。
     * 适配度 ≥80 无 / 60-79 轻度 / <60 重度。每 6 秒刷新一次。
     */
    private static void applySlotDebuffs(Player player, IPlayerBodyState state) {
        if (player.tickCount % 120 != 0) {
            return;
        }
        // 生命融合装备全基因适配加成（每件 +2，抬高部位 debuff 判定阈值）
        int gearCompat = AkaishiLifeFusionSet.geneCompatBonus(player);
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (!(organ.getItem() instanceof AkaishiOrganItem) || AkaishiOrganItem.isNative(organ)) {
                continue;
            }
            int compat = BodyGeneHelper.effectiveCompat(state, organ, gearCompat);
            if (compat >= COMPAT_CLEAN) {
                continue; // 完全适应
            }
            MobEffect effect = slot.getDebuff();
            if (effect == null) {
                continue;
            }
            int amplifier = compat < COMPAT_SEVERE ? 1 : 0;
            player.addEffect(new MobEffectInstance(effect, 160, amplifier, false, false));
        }
    }

    // ===== 天敌器官冲突 =====

    /**
     * 天敌生物器官同时移植 → 排斥锁满（不可降低）+ 器官失效 + 周期性爆炸反噬。
     * 移除任一侧后解除，排斥重置回移植基础值。
     */
    private static void tickConflict(Player player, IPlayerBodyState state) {
        Set<BodySlot> conflicts = OrganEffectResolver.findConflicts(state);
        if (conflicts.isEmpty()) {
            // 冲突解除：重置曾被锁定的排斥值（重新积累）
            Set<BodySlot> locked = CONFLICT_LOCKED.remove(player);
            if (locked != null) {
                for (BodySlot slot : locked) {
                    ItemStack organ = state.getOrgan(slot);
                    if (organ.getItem() instanceof AkaishiOrganItem) {
                        state.setRejection(slot, AkaishiOrganItem.getBaseRejection(organ));
                    } else {
                        state.setRejection(slot, 0); // 槽位已空，排斥清零
                    }
                }
            }
            return;
        }
        CONFLICT_LOCKED.put(player, conflicts);
        // 排斥锁满：器官随即失效，且不可被药剂降低
        for (BodySlot slot : conflicts) {
            state.setRejection(slot, PlayerBodyState.MAX_REJECTION);
        }
        // 周期性天敌反噬：仅对玩家自身结算爆炸伤害 + 爆炸视觉，避免误伤同队/宠物
        if (player.tickCount % CONFLICT_PUNISH_INTERVAL == 0) {
            player.hurt(player.damageSources().explosion(null), CONFLICT_PUNISH_DAMAGE);
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            player.sendSystemMessage(Component.translatable("message.akaishi.body.conflict"));
        }
    }

    // ===== 独特机制（tick 类） =====

    private static void tickSpecial(Player player, IPlayerBodyState state) {
        // 末影怕水：接触水源时瞬移躲避并损失生命值（10 秒冷却）
        if (OrganEffectResolver.hasSpecial(state, OrganSpecial.ENDER_WATER_FEAR)
                && player.isInWater()
                && player.tickCount - WATER_FEAR_COOLDOWN.getOrDefault(player, -1000) > 200) {
            WATER_FEAR_COOLDOWN.put(player, player.tickCount);
            if (teleportRandomly(player)) {
                player.hurt(player.damageSources().magic(), 3.0F);
            }
        }
    }

    /** 随机瞬移：尝试 16 次找无碰撞、不浸水且不落入危险方块（熔岩/火/岩浆块/仙人掌）的落点 */
    private static boolean teleportRandomly(Player player) {
        Level level = player.level();
        for (int i = 0; i < 16; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 12.0;
            double y = player.getY() + player.getRandom().nextInt(7) - 3;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 12.0;
            if (level.noCollision(player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ()))
                    && !level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)
                    && isSafeLanding(level, x, y, z)) {
                player.teleportTo(x, y, z);
                return true;
            }
        }
        return false;
    }

    /** 落点安全判定：身体/脚下无熔岩，且脚下方块非火、岩浆块、仙人掌 */
    private static boolean isSafeLanding(Level level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockPos below = pos.below();
        return !level.getFluidState(pos).is(FluidTags.LAVA)
                && !level.getFluidState(below).is(FluidTags.LAVA)
                && !level.getBlockState(pos).is(Blocks.FIRE)
                && !level.getBlockState(below).is(Blocks.FIRE)
                && !level.getBlockState(below).is(Blocks.MAGMA_BLOCK)
                && !level.getBlockState(below).is(Blocks.CACTUS);
    }

    // ===== 独特机制（进食类：牛胃） =====

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null || !OrganEffectResolver.hasSpecial(state, OrganSpecial.COW_STOMACH)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        // 牛胃可以吃小麦：原版小麦不可食用，直接消耗并恢复饥饿
        if (stack.is(Items.WHEAT)) {
            event.setCanceled(true);
            stack.shrink(1);
            player.getFoodData().eat(3, 0.6F);
            player.displayClientMessage(Component.translatable("message.akaishi.cow_stomach.eat"), true);
            return;
        }
        // 牛胃不能消化肉类：拦截肉类食物使用
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null && food.isMeat()) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.akaishi.cow_stomach.block"), true);
        }
    }
}
