package com.example.akaishi.life.mechanical;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffect;
import com.example.akaishi.api.mechanical.MechanicalEffectRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 机械特殊效果（内置实现）。
 * <p>
 * 由枚举改造为「兼容门面 + 注册表」：静态常量名保持与原枚举一致，历史调用点无需修改；
 * 底层统一以稳定命名空间 ID（如 {@code akaishi:critical_boost}）登记到
 * {@link MechanicalEffectRegistry}，附属模组可注册自定义 {@link IMechanicalDnaEffect}
 * 并按 ID 与本集合共同参与持久化 / 展示 / 执行。
 * <p>
 * 每个 DNA 来源最多携带一个特殊效果。
 */
public final class MechanicalSpecialEffect implements IMechanicalDnaEffect {

    // 注意：映射表须先于常量初始化，create() 依赖它们
    private static final Map<String, MechanicalSpecialEffect> BY_LEGACY_NAME = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, MechanicalSpecialEffect> BY_ID = new ConcurrentHashMap<>();
    private static final List<MechanicalSpecialEffect> VALUES = new ArrayList<>();

    /** 无效果 */
    public static final MechanicalSpecialEffect NONE = create("NONE", "akaishi:none");
    /** 暴击率提升 +5% */
    public static final MechanicalSpecialEffect CRITICAL_BOOST = create("CRITICAL_BOOST", "akaishi:critical_boost");
    /** 爆炸抗性 */
    public static final MechanicalSpecialEffect EXPLOSION_RESIST = create("EXPLOSION_RESIST", "akaishi:explosion_resist");
    /** 毒素免疫 */
    public static final MechanicalSpecialEffect POISON_RESIST = create("POISON_RESIST", "akaishi:poison_resist");
    /** 低血量自动回复 */
    public static final MechanicalSpecialEffect LOW_HEALTH_REGENERATION = create("LOW_HEALTH_REGENERATION", "akaishi:low_health_regeneration");
    /** 攻击附带火焰 */
    public static final MechanicalSpecialEffect FIRE_ATTACK = create("FIRE_ATTACK", "akaishi:fire_attack");
    /** 水下加速 */
    public static final MechanicalSpecialEffect UNDERWATER_SPEED = create("UNDERWATER_SPEED", "akaishi:underwater_speed");
    /** 金装备额外加成 */
    public static final MechanicalSpecialEffect GOLD_ARMOR_BONUS = create("GOLD_ARMOR_BONUS", "akaishi:gold_armor_bonus");
    /** 击退抗性 */
    public static final MechanicalSpecialEffect KNOCKBACK_RESIST = create("KNOCKBACK_RESIST", "akaishi:knockback_resist");
    /** 瞬移冷却减少 */
    public static final MechanicalSpecialEffect TELEPORT_COOLDOWN = create("TELEPORT_COOLDOWN", "akaishi:teleport_cooldown");
    /** 攻击附带凋零 */
    public static final MechanicalSpecialEffect WITHER_ATTACK = create("WITHER_ATTACK", "akaishi:wither_attack");

    static {
        // 内置效果注入公共注册表：附属模组可按 ID 查询 / 展示，并挂接执行钩子
        for (MechanicalSpecialEffect effect : VALUES) {
            MechanicalEffectRegistry.override(effect);
        }
    }

    /** 原枚举名（历史 NBT 兼容用） */
    private final String legacyName;
    private final ResourceLocation id;
    /** 效果名称本地化键（沿用原枚举命名规则，保证既有 lang 键不失效） */
    private final String translationKey;

    private MechanicalSpecialEffect(String legacyName, ResourceLocation id) {
        this.legacyName = legacyName;
        this.id = id;
        this.translationKey = "mechanical.dna.effect." + legacyName.toLowerCase(Locale.ROOT);
    }

    private static MechanicalSpecialEffect create(String legacyName, String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        if (parsed == null) {
            throw new IllegalStateException("Invalid built-in mechanical effect id: " + id);
        }
        MechanicalSpecialEffect effect = new MechanicalSpecialEffect(legacyName, parsed);
        BY_LEGACY_NAME.put(legacyName, effect);
        BY_ID.put(parsed, effect);
        VALUES.add(effect);
        return effect;
    }

    // ==================== 契约 ====================

    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    /** 原枚举名（NBT 兼容） */
    public String name() {
        return legacyName;
    }

    public String translationKey() {
        return translationKey;
    }

    /** 是否「无效果」 */
    public boolean isNone() {
        return this == NONE;
    }

    // ==================== 查询 ====================

    /** 全部内置效果（不可变，顺序与声明一致） */
    public static List<MechanicalSpecialEffect> values() {
        return Collections.unmodifiableList(VALUES);
    }

    /** 旧枚举名 → 内置效果；未知返回 null */
    public static MechanicalSpecialEffect byLegacyName(String legacyName) {
        return legacyName == null ? null : BY_LEGACY_NAME.get(legacyName);
    }

    /** 完整 ID → 内置效果；未知返回 null */
    public static MechanicalSpecialEffect byId(String id) {
        ResourceLocation parsed = id == null ? null : ResourceLocation.tryParse(id);
        return parsed == null ? null : BY_ID.get(parsed);
    }

    /** 宽松解析内置效果：旧枚举名 → 完整 ID → {@code akaishi:} 裸 path 回退；未知回退 {@link #NONE} */
    public static MechanicalSpecialEffect resolve(String key) {
        if (key == null || key.isBlank()) {
            return NONE;
        }
        MechanicalSpecialEffect byLegacy = BY_LEGACY_NAME.get(key);
        if (byLegacy != null) {
            return byLegacy;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(key);
        if (parsed != null) {
            MechanicalSpecialEffect byId = BY_ID.get(parsed);
            if (byId != null) {
                return byId;
            }
        }
        String path = key.indexOf(':') >= 0 ? key.substring(key.indexOf(':') + 1) : key;
        ResourceLocation fallback = ResourceLocation.tryParse("akaishi:" + path.toLowerCase(Locale.ROOT));
        MechanicalSpecialEffect byPath = fallback == null ? null : BY_ID.get(fallback);
        return byPath != null ? byPath : NONE;
    }

    /** 判断效果是否为「无效果」（含 null，兼容第三方实现） */
    public static boolean isNone(IMechanicalDnaEffect effect) {
        return effect == null || NONE.getId().equals(effect.getId());
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
