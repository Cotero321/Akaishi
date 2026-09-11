package com.example.akaishi.life.mechanical;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffect;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * DNA调校模板：对机械部件提供固定的十维修正 + 特殊效果。
 * DNA不改变权重格局，只做小范围调整并赋予独特能力。
 * 每项修正范围 -1~+2，单条总修正绝对值 ≤ ±6。
 *
 * 基因来源分两级（保证内置分组全部有落点）：
 * <ul>
 *   <li>实体级：如 {@code akaishi:zombie}，来源为具体生物样本时优先命中；</li>
 *   <li>分组级：如 {@code akaishi:undead}，与分组 id 同名，兜底覆盖整组。</li>
 * </ul>
 * 通过 {@link #register(String, MechanicalPartWeight, IMechanicalDnaEffect)} 注册，
 * 附属模组可在自己的初始化阶段调用同一 API 新增 DNA 调校模板（ID 建议带自身命名空间）。
 * 注册表线程安全且保持注册顺序；{@link #unregister(String)} / {@link #override} 供开发期调试。
 */
public final class MechanicalDnaProfile {

    /** 无调校基因（默认值） */
    public static final String NONE_ID = "akaishi:none";
    /** ID 未注册时的兜底实例，避免每次解析都新建对象 */
    private static final MechanicalDnaProfile NONE_FALLBACK =
            new MechanicalDnaProfile(NONE_ID, MechanicalPartWeight.ZERO, MechanicalSpecialEffect.NONE);

    private final String id;
    private final MechanicalPartWeight corrections;
    private final IMechanicalDnaEffect effect;

    public MechanicalDnaProfile(String id, MechanicalPartWeight corrections, IMechanicalDnaEffect effect) {
        this.id = id;
        this.corrections = corrections;
        this.effect = effect != null ? effect : MechanicalSpecialEffect.NONE;
    }

    /** DNA 唯一标识，如 "akaishi:skeleton" */
    public String id() { return id; }

    /** 该DNA提供的十维修正权重 */
    public MechanicalPartWeight corrections() { return corrections; }

    /** 特殊效果（内置或附属扩展，永不为 null） */
    public IMechanicalDnaEffect effect() { return effect; }

    /** 本地化键 */
    public String descriptionKey() {
        return "mechanical.dna." + id.replace(':', '.');
    }

    /** 效果名称键（由效果自身提供，内置与附属共用同一约定） */
    public String effectKey() {
        return effect.getTranslationKey();
    }

    // ==================== 注册表 ====================

    /** 注册表：线程安全 + 保持注册顺序（synchronizedMap 的 putIfAbsent 为原子操作） */
    private static final Map<String, MechanicalDnaProfile> REGISTRY =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * 注册一种 DNA 调校模板。
     * 附属模组可在 own init 中调用此方法新增 DNA 来源。
     *
     * @param id 唯一标识，推荐 "modid:source"
     * @param corrections 十维修正（每项 -1~+2，超出将被裁剪）
     * @param effect 特殊效果（内置或附属自定义，可为 null 表示无效果）
     * @return 注册好的 DNA 实例
     * @throws IllegalArgumentException id 非法或已存在
     */
    public static MechanicalDnaProfile register(String id, MechanicalPartWeight corrections,
                                                IMechanicalDnaEffect effect) {
        validateId(id);
        MechanicalDnaProfile dna = new MechanicalDnaProfile(id, clamp(corrections), effect);
        if (REGISTRY.putIfAbsent(id, dna) != null) {
            throw new IllegalArgumentException("MechanicalDnaProfile already registered: " + id);
        }
        return dna;
    }

    /** 覆盖已注册（或新增）DNA 模板，供开发期替换内置数据使用。 */
    public static MechanicalDnaProfile override(String id, MechanicalPartWeight corrections,
                                                IMechanicalDnaEffect effect) {
        validateId(id);
        MechanicalDnaProfile dna = new MechanicalDnaProfile(id, clamp(corrections), effect);
        REGISTRY.put(id, dna);
        return dna;
    }

    /** 注销 DNA 模板，返回被移除项；不存在返回 null。 */
    public static MechanicalDnaProfile unregister(String id) {
        return id == null ? null : REGISTRY.remove(id);
    }

    /** 按 ID 查找 DNA，不存在返回 null */
    public static MechanicalDnaProfile get(String id) {
        return id == null ? null : REGISTRY.get(id);
    }

    /** 获取所有已注册 DNA 的快照（顺序与注册顺序一致） */
    public static Collection<MechanicalDnaProfile> getAll() {
        synchronized (REGISTRY) {
            return List.copyOf(REGISTRY.values());
        }
    }

    /**
     * 按样本来源推导 DNA：先具体实体（{@code entityId}，如 minecraft:zombie），
     * 再分组（{@code groupId}，如 undead），均未命中回退 {@link #NONE_ID}。
     * <p>
     * 查找对命名空间不敏感：完整 ID 优先精确命中（支持 {@code mymod:xxx}），
     * 未命中则回退内置 {@code akaishi:path}，兼容原版实体与裸 path 分组。
     *
     * @param groupId 样本分组 id（可为 null）
     * @param entityId 实体注册名，形如 "minecraft:zombie"（可为 null）
     * @return 对应 DNA 模板，永不为 null
     */
    public static MechanicalDnaProfile resolveForSample(String groupId, String entityId) {
        MechanicalDnaProfile hit = lookup(entityId);
        if (hit != null) {
            return hit;
        }
        hit = lookup(groupId);
        if (hit != null) {
            return hit;
        }
        MechanicalDnaProfile none = REGISTRY.get(NONE_ID);
        return none != null ? none : NONE_FALLBACK;
    }

    /** 完整 ID 精确命中 → 裸 path 兼容 {@code akaishi} 命名空间 → 带命名空间回退内置 path */
    private static MechanicalDnaProfile lookup(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String trimmed = rawId.trim();
        MechanicalDnaProfile exact = REGISTRY.get(trimmed);
        if (exact != null) {
            return exact;
        }
        int colon = trimmed.indexOf(':');
        String path = colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
        return REGISTRY.get("akaishi:" + path.toLowerCase(Locale.ROOT));
    }

    private static void validateId(String id) {
        if (id == null || ResourceLocation.tryParse(id) == null) {
            throw new IllegalArgumentException("MechanicalDnaProfile id must be a valid resource location: " + id);
        }
    }

    /** 注册本模组内置 DNA 调校模板 */
    public static void registerDefaults() {
        // 无DNA调校
        register(NONE_ID, c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), MechanicalSpecialEffect.NONE);

        // ---- 分组级（与内置分组一一对应，保证各组基因全部有落点）----
        register("akaishi:warm_blooded", c(0, 2, 0, 0, 1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.LOW_HEALTH_REGENERATION); // 温血：生命/护甲/机动
        register("akaishi:undead", c(0, 2, 1, -1, 0, 2, 0, 0, 0, 0), MechanicalSpecialEffect.WITHER_ATTACK);                 // 亡灵：耐打/护甲/凋零
        register("akaishi:explosive", c(0, -1, 2, 0, 0, 0, 0, 2, 0, 0), MechanicalSpecialEffect.EXPLOSION_RESIST);           // 爆炸：攻击/暴伤
        register("akaishi:aberration", c(0, 0, 1, 1, 0, 1, 1, 0, 0, 0), MechanicalSpecialEffect.POISON_RESIST);              // 异变：全能偏攻
        register("akaishi:ender", c(0, -1, 0, 0, 2, 0, 0, 0, 1, 2), MechanicalSpecialEffect.TELEPORT_COOLDOWN);              // 末影：移速/范围/闪避
        register("akaishi:boss", c(0, 2, 2, 0, -1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.KNOCKBACK_RESIST);                // 首领：生命/攻击/护甲
        register("akaishi:dragon", c(2, 2, 2, 0, -1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.FIRE_ATTACK);                   // 龙族：倍率/生命/攻击

        // ---- 实体级（具体生物样本优先命中）----
        register("akaishi:skeleton", c(0, -1, 0, 0, 0, 0, 2, 1, 1, 0), MechanicalSpecialEffect.CRITICAL_BOOST);   // 骷髅：精准远程
        register("akaishi:spider", c(0, 0, 0, 1, 2, 0, 0, 0, 0, 1), MechanicalSpecialEffect.NONE);                // 蜘蛛：敏捷攀爬
        register("akaishi:creeper", c(0, 0, 1, 0, 0, 1, 0, 1, 0, 0), MechanicalSpecialEffect.EXPLOSION_RESIST);   // 苦力怕：爆破
        register("akaishi:bee", c(0, 0, 0, 1, 1, 0, 0, 0, 0, 1), MechanicalSpecialEffect.POISON_RESIST);           // 蜜蜂：迅捷毒刺
        register("akaishi:zombie", c(0, 2, 1, -1, 0, 0, 0, 0, 0, 0), MechanicalSpecialEffect.LOW_HEALTH_REGENERATION); // 僵尸：不死坚韧
        register("akaishi:blaze", c(1, -1, 2, 0, -1, 0, 0, 1, 0, 0), MechanicalSpecialEffect.FIRE_ATTACK);        // 烈焰人：烈焰爆发
        register("akaishi:dolphin", c(0, 1, 0, 0, 2, 0, 0, 0, 0, 0), MechanicalSpecialEffect.UNDERWATER_SPEED);   // 海豚：水生灵巧
        register("akaishi:piglin", c(0, 0, 1, 0, 1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.GOLD_ARMOR_BONUS);    // 猪灵：贪婪金装
        register("akaishi:iron_golem", c(0, 2, 0, -1, -1, 2, 0, 0, 0, 0), MechanicalSpecialEffect.KNOCKBACK_RESIST); // 铁傀儡：重装铁壁
        register("akaishi:enderman", c(1, 0, 0, 0, 1, 0, 0, 0, 1, 1), MechanicalSpecialEffect.TELEPORT_COOLDOWN);  // 末影人：空间闪现
        register("akaishi:wither_skeleton", c(1, -1, 1, 1, 0, 0, 0, 0, 0, 0), MechanicalSpecialEffect.WITHER_ATTACK); // 凋灵骷髅：凋零斩击
    }

    /** 逐项将修正值裁剪到 -1~+2 */
    private static MechanicalPartWeight clamp(MechanicalPartWeight raw) {
        MechanicalPartWeight result = raw;
        for (MechanicalProperty prop : MechanicalProperty.values()) {
            result = result.with(prop, clampCorrection(raw.get(prop)));
        }
        return result;
    }

    /** 将DNA修正值限制在 -1~+2 范围内 */
    private static int clampCorrection(int value) {
        return Math.max(-1, Math.min(2, value));
    }

    /** 十维修正快捷构造：倍率 / 生命 / 攻击伤害 / 攻击速度 / 移速 / 护甲 / 暴击率 / 暴击伤害 / 攻击范围 / 闪避 */
    private static MechanicalPartWeight c(int om, int hp, int ad, int as, int ms,
                                          int armor, int crit, int critDamage, int range, int dodge) {
        return new MechanicalPartWeight(om, hp, ad, as, ms, armor, crit, critDamage, range, dodge);
    }
}
