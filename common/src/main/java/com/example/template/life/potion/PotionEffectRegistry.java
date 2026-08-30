package com.example.template.life.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;

/**
 * 生物药剂效果注册表（增量式）："生物来源" → 药剂效果定义。
 * 与 OrganEffectRegistry 同构：新生物逐个注册即可，未注册生物回退默认值。
 * 设计原则：
 * - 温和生物（牛/猪/鸡）适配度加成低、副作用温和（反胃/饥饿）
 * - 凶猛生物（狼/熊/蜘蛛）加成中高、副作用凶（中毒/虚弱）
 * - 高危生物（末影/烈焰/凋灵/末影龙）加成高、副作用烈（凋零/剧毒/失明）
 */
public final class PotionEffectRegistry {

    /** 默认永久药剂适配度加成 */
    public static final int DEFAULT_COMPAT_BONUS = 15;
    /** 默认突破药剂副作用池 */
    public static final MobEffect[] DEFAULT_SIDE_EFFECTS = {
            MobEffects.POISON, MobEffects.WEAKNESS, MobEffects.HUNGER,
            MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN, MobEffects.CONFUSION
    };

    private static final Map<String, PotionEffect> EFFECTS = new HashMap<>();

    static {
        // ===== 温血食草系：低加成、温和副作用 =====
        register("minecraft:cow", new PotionEffect("minecraft:cow", 14,
                new MobEffect[]{MobEffects.MOVEMENT_SLOWDOWN, MobEffects.CONFUSION}));
        register("minecraft:pig", new PotionEffect("minecraft:pig", 14,
                new MobEffect[]{MobEffects.HUNGER, MobEffects.CONFUSION}));
        register("minecraft:sheep", new PotionEffect("minecraft:sheep", 14,
                new MobEffect[]{MobEffects.MOVEMENT_SLOWDOWN}));
        register("minecraft:rabbit", new PotionEffect("minecraft:rabbit", 14,
                new MobEffect[]{MobEffects.HUNGER}));
        register("minecraft:chicken", new PotionEffect("minecraft:chicken", 14,
                new MobEffect[]{MobEffects.HUNGER, MobEffects.CONFUSION}));
        register("minecraft:goat", new PotionEffect("minecraft:goat", 15,
                new MobEffect[]{MobEffects.MOVEMENT_SLOWDOWN, MobEffects.HUNGER}));
        register("minecraft:horse", new PotionEffect("minecraft:horse", 15,
                new MobEffect[]{MobEffects.MOVEMENT_SLOWDOWN}));
        register("minecraft:frog", new PotionEffect("minecraft:frog", 15,
                new MobEffect[]{MobEffects.CONFUSION}));
        register("minecraft:turtle", new PotionEffect("minecraft:turtle", 16,
                new MobEffect[]{MobEffects.MOVEMENT_SLOWDOWN}));
        // ===== 水生系：离水副作用 =====
        register("minecraft:dolphin", new PotionEffect("minecraft:dolphin", 15,
                new MobEffect[]{MobEffects.CONFUSION, MobEffects.MOVEMENT_SLOWDOWN}));
        register("minecraft:cod", new PotionEffect("minecraft:cod", 14,
                new MobEffect[]{MobEffects.MOVEMENT_SLOWDOWN}));
        register("minecraft:glow_squid", new PotionEffect("minecraft:glow_squid", 16,
                new MobEffect[]{MobEffects.BLINDNESS, MobEffects.CONFUSION}));
        // ===== 捕食/敏捷系：中高加成、毒性副作用 =====
        register("minecraft:cat", new PotionEffect("minecraft:cat", 16,
                new MobEffect[]{MobEffects.POISON}));
        register("minecraft:fox", new PotionEffect("minecraft:fox", 16,
                new MobEffect[]{MobEffects.POISON, MobEffects.HUNGER}));
        register("minecraft:wolf", new PotionEffect("minecraft:wolf", 17,
                new MobEffect[]{MobEffects.POISON, MobEffects.WEAKNESS}));
        register("minecraft:spider", new PotionEffect("minecraft:spider", 16,
                new MobEffect[]{MobEffects.POISON, MobEffects.CONFUSION}));
        register("minecraft:bee", new PotionEffect("minecraft:bee", 16,
                new MobEffect[]{MobEffects.POISON}));
        register("minecraft:polar_bear", new PotionEffect("minecraft:polar_bear", 18,
                new MobEffect[]{MobEffects.WEAKNESS, MobEffects.MOVEMENT_SLOWDOWN}));
        register("minecraft:bat", new PotionEffect("minecraft:bat", 15,
                new MobEffect[]{MobEffects.HUNGER, MobEffects.CONFUSION}));
        // ===== 亡灵系：加成高、虚弱/饥饿副作用 =====
        register("minecraft:zombie", new PotionEffect("minecraft:zombie", 18,
                new MobEffect[]{MobEffects.WEAKNESS, MobEffects.HUNGER}));
        register("minecraft:skeleton", new PotionEffect("minecraft:skeleton", 18,
                new MobEffect[]{MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN}));
        register("minecraft:wither", new PotionEffect("minecraft:wither", 22,
                new MobEffect[]{MobEffects.WITHER, MobEffects.POISON}));
        // ===== 高危系：高加成、烈性副作用 =====
        register("minecraft:creeper", new PotionEffect("minecraft:creeper", 16,
                new MobEffect[]{MobEffects.POISON, MobEffects.CONFUSION}));
        register("minecraft:blaze", new PotionEffect("minecraft:blaze", 18,
                new MobEffect[]{MobEffects.WEAKNESS, MobEffects.WITHER}));
        register("minecraft:enderman", new PotionEffect("minecraft:enderman", 20,
                new MobEffect[]{MobEffects.MOVEMENT_SLOWDOWN, MobEffects.BLINDNESS}));
        register("minecraft:iron_golem", new PotionEffect("minecraft:iron_golem", 18,
                new MobEffect[]{MobEffects.MOVEMENT_SLOWDOWN, MobEffects.DIG_SLOWDOWN}));
        register("minecraft:guardian", new PotionEffect("minecraft:guardian", 17,
                new MobEffect[]{MobEffects.POISON, MobEffects.WEAKNESS}));
        register("minecraft:slime", new PotionEffect("minecraft:slime", 16,
                new MobEffect[]{MobEffects.CONFUSION, MobEffects.MOVEMENT_SLOWDOWN}));
        register("minecraft:allay", new PotionEffect("minecraft:allay", 16,
                new MobEffect[]{MobEffects.HUNGER}));
        // ===== 终局系：最高加成、最烈副作用 =====
        register("minecraft:ender_dragon", new PotionEffect("minecraft:ender_dragon", 25,
                new MobEffect[]{MobEffects.WITHER, MobEffects.WEAKNESS, MobEffects.BLINDNESS}));
    }

    private PotionEffectRegistry() {
    }

    private static void register(String entityId, PotionEffect effect) {
        EFFECTS.put(entityId, effect);
    }

    /** 查询生物药剂效果（未注册返回 null） */
    public static PotionEffect get(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return null;
        }
        return EFFECTS.get(entityId);
    }

    /** 永久药剂适配度加成（未注册生物回退默认 15） */
    public static int compatBonusOf(String entityId) {
        PotionEffect effect = get(entityId);
        return effect != null ? effect.compatBonus() : DEFAULT_COMPAT_BONUS;
    }

    /** 突破药剂副作用池（未注册生物回退通用池） */
    public static MobEffect[] sideEffectsOf(String entityId) {
        PotionEffect effect = get(entityId);
        return effect != null ? effect.sideEffects() : DEFAULT_SIDE_EFFECTS;
    }
}
