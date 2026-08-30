package com.example.template.forge.life;

import com.example.template.life.body.BodySlot;
import com.example.template.life.body.IPlayerBodyState;
import com.example.template.life.body.PlayerBodyHelper;
import com.example.template.life.body.PlayerBodyState;
import com.example.template.life.linkage.OrganLinkage;
import com.example.template.life.organ.ChishiOrganItem;
import com.example.template.life.organ.OrganEffectResolver;
import com.example.template.life.organ.OrganPassive;
import com.example.template.life.organ.OrganSpecial;
import com.example.template.life.organ.OrganTemplate;
import com.example.template.life.organ.QualityTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
public final class ChishiBodyPassiveHandler {

    public static final ChishiBodyPassiveHandler INSTANCE = new ChishiBodyPassiveHandler();

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

    private record AppliedAttr(String key, Attribute attribute) {
    }

    private ChishiBodyPassiveHandler() {
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
        rebuildAttributes(player, state);
        applyPassives(player, state);
        tickRejection(player, state);
        tickConflict(player, state);
        applySlotDebuffs(player, state);
        tickSpecial(player, state);
    }

    // ===== 属性重建 =====

    /** 移植/摘除/适配度/排斥变化时重建器官属性修饰（摘要相同直接跳过） */
    private static void rebuildAttributes(Player player, IPlayerBodyState state) {
        String digest = digestOf(state);
        String last = ATTRIBUTE_DIGEST.get(player);
        if (digest.equals(last)) {
            return;
        }
        ATTRIBUTE_DIGEST.put(player, digest);
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
        // 2) 生效器官：MAX_HEALTH 按百分比聚合，其余属性按基础值 × 倍率 × 适配度 × 突破倍率
        for (OrganEffectResolver.ActiveOrgan organ : OrganEffectResolver.collect(state)) {
            double compatFactor = ChishiOrganItem.getCompat(organ.stack()) / 100.0;
            double boost = ChishiOrganItem.getBoost(organ.stack());
            List<OrganTemplate.AttributeBonus> bonuses = OrganEffectResolver.bonusesOf(organ.stack(), organ.slot(), organ.effect());
            for (OrganTemplate.AttributeBonus b : bonuses) {
                if (b.attribute() == Attributes.MAX_HEALTH) {
                    // 生命加成按基础生命百分比计算：base/20 即 +base×5%
                    healthPct += (b.base() / BASE_HEALTH) * organ.tier().getMultiplier() * compatFactor * boost;
                    continue;
                }
                double value = b.base() * organ.tier().getMultiplier() * compatFactor * boost;
                AttributeInstance inst = player.getAttribute(b.attribute());
                if (inst == null || value == 0.0) {
                    continue;
                }
                inst.addTransientModifier(new AttributeModifier(uuidOf(organ.slot().name(), b.attribute()),
                        "Chishi organ", value, ADD));
                next.add(new AppliedAttr(organ.slot().name(), b.attribute()));
            }
        }
        // 3) 生命上限 = 20 × (1 + 加成% − 空槽权重%)
        double healthDelta = BASE_HEALTH * (1.0 + healthPct) - BASE_HEALTH;
        if (Math.abs(healthDelta) > 0.001) {
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.addTransientModifier(new AttributeModifier(uuidOf(HEALTH_KEY, Attributes.MAX_HEALTH),
                        "Chishi organ health", healthDelta, ADD));
                next.add(new AppliedAttr(HEALTH_KEY, Attributes.MAX_HEALTH));
            }
        }
        APPLIED.put(player, next);
    }

    /** 键 + 属性 → 稳定 UUID（移植/摘除后精确移除同一修饰符） */
    private static UUID uuidOf(String key, Attribute attribute) {
        return UUID.nameUUIDFromBytes((key + ":" + attribute.getDescriptionId()).getBytes(StandardCharsets.UTF_8));
    }

    /** 摘要：各槽位器官来源/品质/适配度 + 排斥值 + 空槽（变化才触发重建） */
    private static String digestOf(IPlayerBodyState state) {
        StringBuilder sb = new StringBuilder();
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            sb.append(slot).append('|');
            if (organ.getItem() instanceof ChishiOrganItem) {
                if (ChishiOrganItem.isNative(organ)) {
                    sb.append("native");
                } else {
                    sb.append(ChishiOrganItem.getEntityId(organ)).append(':')
                            .append(ChishiOrganItem.getTier(organ)).append(':')
                            .append(ChishiOrganItem.getCompat(organ)).append(':')
                            .append(ChishiOrganItem.getBoost(organ));
                }
            }
            sb.append(':').append(state.getRejection(slot)).append(';');
        }
        return sb.toString();
    }

    // ===== 被动技能（常驻/被动触发） =====

    private static void applyPassives(Player player, IPlayerBodyState state) {
        for (OrganEffectResolver.ActiveOrgan organ : OrganEffectResolver.collect(state)) {
            if (organ.effect() == null || organ.effect().passives() == null) {
                continue;
            }
            for (OrganPassive passive : organ.effect().passives()) {
                applyPassive(player, state, passive);
            }
        }
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
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            // 原生器官与身体完全契合，不产生排斥
            if (!(organ.getItem() instanceof ChishiOrganItem) || ChishiOrganItem.isNative(organ)) {
                continue;
            }
            QualityTier tier = ChishiOrganItem.getTier(organ);
            if (tier == null) {
                continue;
            }
            // 排斥速率 × 100/适配度：适配度越低排斥涨得越快；<60 重度排异再翻倍
            int compat = ChishiOrganItem.getCompat(organ);
            double factor = 100.0 / Math.max(1, compat);
            if (compat < COMPAT_SEVERE) {
                factor *= 2.0;
            }
            // 基因强度联动：强基因（末影/龙）排斥增长更快（OrganLinkage.rejectionFactorOf）
            factor *= OrganLinkage.rejectionFactorOf(organ);
            int interval = (int) Math.max(1, tier.getGrowthIntervalSeconds() * 20.0 / factor);
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
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (!(organ.getItem() instanceof ChishiOrganItem) || ChishiOrganItem.isNative(organ)) {
                continue;
            }
            int compat = ChishiOrganItem.getCompat(organ);
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
                    if (organ.getItem() instanceof ChishiOrganItem) {
                        state.setRejection(slot, ChishiOrganItem.getBaseRejection(organ));
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
        // 周期性天敌反噬：小型爆炸（不破坏方块）+ 提示
        if (player.tickCount % CONFLICT_PUNISH_INTERVAL == 0) {
            player.level().explode(player, player.getX(), player.getY(), player.getZ(),
                    2.0F, Level.ExplosionInteraction.NONE);
            player.sendSystemMessage(Component.translatable("message.template_mod.body.conflict"));
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

    /** 随机瞬移：尝试 16 次找无碰撞且不浸水的落点 */
    private static boolean teleportRandomly(Player player) {
        for (int i = 0; i < 16; i++) {
            double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 12.0;
            double y = player.getY() + player.getRandom().nextInt(7) - 3;
            double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 12.0;
            if (player.level().noCollision(player.getBoundingBox().move(x - player.getX(), y - player.getY(), z - player.getZ()))
                    && !player.level().getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) {
                player.teleportTo(x, y, z);
                return true;
            }
        }
        return false;
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
            player.displayClientMessage(Component.translatable("message.template_mod.cow_stomach.eat"), true);
            return;
        }
        // 牛胃不能消化肉类：拦截肉类食物使用
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null && food.isMeat()) {
            event.setCanceled(true);
            player.displayClientMessage(Component.translatable("message.template_mod.cow_stomach.block"), true);
        }
    }
}
