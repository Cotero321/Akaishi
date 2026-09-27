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
    // ---- T7 Stage 2 新增（均镜像生物侧 OrganPassive，不新造强度天花板）----
    /** 常驻夜视（镜像 OrganPassive.NIGHT_VISION） */
    public static final MechanicalSpecialEffect NIGHT_VISION = create("NIGHT_VISION", "akaishi:night_vision");
    /** 水下呼吸（镜像 OrganPassive.WATER_BREATHING） */
    public static final MechanicalSpecialEffect WATER_BREATHING = create("WATER_BREATHING", "akaishi:water_breathing");
    /** 摔落免疫（镜像 OrganPassive.FALL_IMMUNE，按等级减伤至完全免疫） */
    public static final MechanicalSpecialEffect FALL_IMMUNE = create("FALL_IMMUNE", "akaishi:fall_immune");
    /** 自动拾取周围掉落物（镜像 OrganPassive.AUTO_PICKUP） */
    public static final MechanicalSpecialEffect AUTO_PICKUP = create("AUTO_PICKUP", "akaishi:auto_pickup");
    /** 命中把目标顶开（镜像 OrganPassive.KNOCKBACK_ON_HIT） */
    public static final MechanicalSpecialEffect KNOCKBACK_ON_HIT = create("KNOCKBACK_ON_HIT", "akaishi:knockback_on_hit");
    /** 负面时长减免（参考 OrganPassive.ANTIDOTE 思路） */
    public static final MechanicalSpecialEffect DEBUFF_RESIST = create("DEBUFF_RESIST", "akaishi:debuff_resist");
    // ---- 追加批次新增（均镜像生物侧 OrganPassive，不新造强度天花板）----
    /** 命中施加缓慢（镜像 OrganPassive.SLOW_ON_HIT） */
    public static final MechanicalSpecialEffect SLOW_ON_HIT = create("SLOW_ON_HIT", "akaishi:slow_on_hit");
    /** 命中附加中毒（镜像 OrganPassive.POISON_ON_HIT） */
    public static final MechanicalSpecialEffect POISON_ON_HIT = create("POISON_ON_HIT", "akaishi:poison_on_hit");
    /** 命中附加挖掘疲劳（镜像 OrganPassive.FATIGUE_ON_HIT） */
    public static final MechanicalSpecialEffect FATIGUE_ON_HIT = create("FATIGUE_ON_HIT", "akaishi:fatigue_on_hit");
    /** 受击概率瞬移脱身（镜像 OrganPassive.TELEPORT_DODGE） */
    public static final MechanicalSpecialEffect TELEPORT_DODGE = create("TELEPORT_DODGE", "akaishi:teleport_dodge");
    /** 弹射物伤害提升（镜像 OrganPassive.PROJECTILE_BOOST） */
    public static final MechanicalSpecialEffect PROJECTILE_BOOST = create("PROJECTILE_BOOST", "akaishi:projectile_boost");
    /** 空中攻击伤害提升（镜像 OrganPassive.JUMP_ATTACK_BOOST） */
    public static final MechanicalSpecialEffect JUMP_ATTACK_BOOST = create("JUMP_ATTACK_BOOST", "akaishi:jump_attack_boost");
    /** 近战攻击距离提升（镜像 OrganPassive.LONG_REACH） */
    public static final MechanicalSpecialEffect LONG_REACH = create("LONG_REACH", "akaishi:long_reach");
    // ---- 机制型基因（自带负面代价；无实体/分组来源，属性由 MechanicalDnaProfile 逐条挂载）----
    /** 过热核心：血量越低攻击越高，代价是持续掉血 */
    public static final MechanicalSpecialEffect OVERHEAT_CORE = create("OVERHEAT_CORE", "akaishi:overheat_core");
    /** 寄生共生：击杀回血，代价是饥饿消耗加速 */
    public static final MechanicalSpecialEffect PARASITIC_SYMBIOSIS = create("PARASITIC_SYMBIOSIS", "akaishi:parasitic_symbiosis");
    /** 回声定位：周期标记周围生物，代价是受爆炸伤害加重 */
    public static final MechanicalSpecialEffect ECHOLOCATION = create("ECHOLOCATION", "akaishi:echolocation");
    /** 装甲过载：临时提升护甲，代价是移动速度下降 */
    public static final MechanicalSpecialEffect ARMOR_OVERLOAD = create("ARMOR_OVERLOAD", "akaishi:armor_overload");
    /** 神经痉挛：触发时下一击必定暴击，代价是期间攻击间隔变长 */
    public static final MechanicalSpecialEffect NEURAL_SPASM = create("NEURAL_SPASM", "akaishi:neural_spasm");
    /** 代谢透支：生命恢复提速，代价是恢复期间附带虚弱 */
    public static final MechanicalSpecialEffect METABOLIC_OVERDRAFT = create("METABOLIC_OVERDRAFT", "akaishi:metabolic_overdraft");

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
