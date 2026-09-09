package com.example.akaishi.life.mechanical;

import java.util.*;

/**
 * DNA调校模板：对机械部件提供固定的五维修正 + 特殊效果。
 * DNA不改变权重格局，只做小范围调整并赋予独特能力。
 * 修正范围 -1~+2/属性，总修正绝对值不超过 ±6。
 *
 * 通过 {@link #register(String, int, int, int, int, int, MechanicalSpecialEffect)} 注册，
 * 附属模组可在自己的初始化阶段调用同一 API 新增 DNA 调校模板。
 */
public final class MechanicalDnaProfile {

    private final String id;
    private final MechanicalPartWeight corrections;
    private final MechanicalSpecialEffect effect;

    public MechanicalDnaProfile(String id, MechanicalPartWeight corrections, MechanicalSpecialEffect effect) {
        this.id = id;
        this.corrections = corrections;
        this.effect = effect;
    }

    /** DNA 唯一标识，如 "akaishi:skeleton" */
    public String id() { return id; }

    /** 该DNA提供的五维修正权重 */
    public MechanicalPartWeight corrections() { return corrections; }

    /** 特殊效果 */
    public MechanicalSpecialEffect effect() { return effect; }

    /** 本地化键 */
    public String descriptionKey() {
        return "mechanical.dna." + id.replace(':', '.');
    }

    /** 效果名称键 */
    public String effectKey() {
        return "mechanical.dna.effect." + effect.name().toLowerCase();
    }

    // ==================== 注册表 ====================

    private static final Map<String, MechanicalDnaProfile> REGISTRY = new LinkedHashMap<>();

    /**
     * 注册一种 DNA 调校模板。
     * 附属模组可在 own init 中调用此方法新增 DNA 来源。
     *
     * @param id 唯一标识，推荐 "modid:source"
     * @param om 总体倍率修正 -1~+2
     * @param hp 生命值修正 -1~+2
     * @param ad 攻击伤害修正 -1~+2
     * @param as 攻击速度修正 -1~+2
     * @param ms 移动速度修正 -1~+2
     * @param effect 特殊效果
     * @return 注册好的 DNA 实例
     * @throws IllegalArgumentException 如果 id 已存在
     */
    public static MechanicalDnaProfile register(String id,
                                                int om, int hp, int ad, int as, int ms,
                                                MechanicalSpecialEffect effect) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("MechanicalDnaProfile already registered: " + id);
        }
        MechanicalPartWeight corrections = new MechanicalPartWeight(
                clampCorrection(om), clampCorrection(hp), clampCorrection(ad),
                clampCorrection(as), clampCorrection(ms));
        MechanicalDnaProfile dna = new MechanicalDnaProfile(id, corrections, effect);
        REGISTRY.put(id, dna);
        return dna;
    }

    /** 按 ID 查找 DNA，不存在返回 null */
    public static MechanicalDnaProfile get(String id) {
        return REGISTRY.get(id);
    }

    /** 获取所有已注册 DNA 的不可变视图 */
    public static Collection<MechanicalDnaProfile> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /** 注册本模组内置 DNA 调校模板 */
    public static void registerDefaults() {
        // 无DNA调校
        register("akaishi:none", 0, 0, 0, 0, 0, MechanicalSpecialEffect.NONE);
        // 常见生物（±2~3，基础微调）
        register("akaishi:skeleton", 0, -1, 1, 1, 0, MechanicalSpecialEffect.CRITICAL_BOOST);
        register("akaishi:spider", 0, 0, 0, 1, 2, MechanicalSpecialEffect.NONE);
        register("akaishi:creeper", 0, 1, 1, 0, 0, MechanicalSpecialEffect.EXPLOSION_RESIST);
        register("akaishi:bee", 0, 0, 0, 1, 1, MechanicalSpecialEffect.POISON_RESIST);
        // 强力生物（±4~5，显著倾向）
        register("akaishi:zombie", 1, 2, 0, -1, 0, MechanicalSpecialEffect.LOW_HEALTH_REGENERATION);
        register("akaishi:blaze", 1, -1, 2, 0, -1, MechanicalSpecialEffect.FIRE_ATTACK);
        register("akaishi:dolphin", 0, 1, 0, 0, 2, MechanicalSpecialEffect.UNDERWATER_SPEED);
        register("akaishi:piglin", 0, 0, 1, 0, 1, MechanicalSpecialEffect.GOLD_ARMOR_BONUS);
        // 强大生物（±5~6，风格鲜明）
        register("akaishi:iron_golem", 1, 3, 0, -1, -1, MechanicalSpecialEffect.KNOCKBACK_RESIST);
        register("akaishi:enderman", 1, 0, 0, 0, 1, MechanicalSpecialEffect.TELEPORT_COOLDOWN);
        register("akaishi:wither_skeleton", 1, 0, 1, 1, -1, MechanicalSpecialEffect.WITHER_ATTACK);
    }

    /** 将DNA修正值限制在 -1~+2 范围内 */
    private static int clampCorrection(int value) {
        return Math.max(-1, Math.min(2, value));
    }
}