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
    /** 全部允许的部件集合（无约束） */
    private static final Set<MechanicalPartType> ALL_PARTS =
            Collections.unmodifiableSet(EnumSet.allOf(MechanicalPartType.class));
    /** ID 未注册时的兜底实例，避免每次解析都新建对象 */
    private static final MechanicalDnaProfile NONE_FALLBACK =
            new MechanicalDnaProfile(NONE_ID, MechanicalPartWeight.ZERO, MechanicalSpecialEffect.NONE);

    private final String id;
    private final MechanicalPartWeight corrections;
    private final IMechanicalDnaEffect effect;
    /** 该 DNA 允许装入的部件类型（不可变、非空；全部允许即视为无约束） */
    private final Set<MechanicalPartType> allowedParts;
    /** 是否自带「负面代价」说明（tooltip 据此决定是否显示 drawback 行） */
    private final boolean drawback;

    public MechanicalDnaProfile(String id, MechanicalPartWeight corrections, IMechanicalDnaEffect effect) {
        this(id, corrections, effect, ALL_PARTS);
    }

    public MechanicalDnaProfile(String id, MechanicalPartWeight corrections, IMechanicalDnaEffect effect,
                                Set<MechanicalPartType> allowedParts) {
        this(id, corrections, effect, allowedParts, false);
    }

    public MechanicalDnaProfile(String id, MechanicalPartWeight corrections, IMechanicalDnaEffect effect,
                                Set<MechanicalPartType> allowedParts, boolean drawback) {
        this.id = id;
        this.corrections = corrections;
        this.effect = effect != null ? effect : MechanicalSpecialEffect.NONE;
        // 空集合视为无约束（全部允许），避免附属误传空集导致基因无处可装
        this.allowedParts = allowedParts == null || allowedParts.isEmpty()
                ? ALL_PARTS
                : Collections.unmodifiableSet(EnumSet.copyOf(allowedParts));
        this.drawback = drawback;
    }

    /** DNA 唯一标识，如 "akaishi:skeleton" */
    public String id() { return id; }

    /** 该DNA提供的十维修正权重 */
    public MechanicalPartWeight corrections() { return corrections; }

    /** 特殊效果（内置或附属扩展，永不为 null） */
    public IMechanicalDnaEffect effect() { return effect; }

    /** 允许装入的部件类型（不可变集合，永不为空） */
    public Set<MechanicalPartType> allowedParts() { return allowedParts; }

    /** 该 DNA 是否可装入给定部件；null 部件一律拒绝 */
    public boolean allows(MechanicalPartType partType) {
        return partType != null && allowedParts.contains(partType);
    }

    /** 是否存在部件约束（全部允许 ⇒ false，展示层据此决定是否显示「可装部件」行） */
    public boolean hasPartConstraint() {
        return allowedParts.size() < MechanicalPartType.values().length;
    }

    /** 是否自带负面代价说明（仅机制型基因置位；展示层据此显示 drawback 行） */
    public boolean hasDrawback() {
        return drawback;
    }

    /** 负面代价说明的本地化键（仅 {@link #hasDrawback()} 为真时有对应文案） */
    public String drawbackKey() {
        return descriptionKey() + ".drawback";
    }

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
        return register(id, corrections, effect, ALL_PARTS, false);
    }

    /**
     * 注册一种 DNA 调校模板（带部件约束）。
     *
     * @param allowedParts 允许装入的部件类型（null/空集 = 全部允许，即无约束）
     * @see #register(String, MechanicalPartWeight, IMechanicalDnaEffect)
     */
    public static MechanicalDnaProfile register(String id, MechanicalPartWeight corrections,
                                                IMechanicalDnaEffect effect, Set<MechanicalPartType> allowedParts) {
        return register(id, corrections, effect, allowedParts, false);
    }

    /**
     * 注册一种 DNA 调校模板（带部件约束 + 负面代价标记）。
     *
     * @param drawback 是否自带负面代价（true ⇒ 展示层显示 {@link #drawbackKey()} 一行）
     * @see #register(String, MechanicalPartWeight, IMechanicalDnaEffect, Set)
     */
    public static MechanicalDnaProfile register(String id, MechanicalPartWeight corrections,
                                                IMechanicalDnaEffect effect, Set<MechanicalPartType> allowedParts,
                                                boolean drawback) {
        validateId(id);
        MechanicalDnaProfile dna = new MechanicalDnaProfile(id, clamp(corrections), effect, allowedParts, drawback);
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

    /** 注册本模组内置 DNA 调校模板（部件约束按「语义类别」指派，见各条注释） */
    public static void registerDefaults() {
        // 无DNA调校（NONE 恒为全部允许）
        register(NONE_ID, c(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), MechanicalSpecialEffect.NONE, ALL_PARTS);

        // ---- 分组级（与内置分组一一对应，保证各组基因全部有落点）----
        // 温血：代谢/恢复 → 散热（+外壳，兼护甲）
        register("akaishi:warm_blooded", c(0, 2, 0, 0, 1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.LOW_HEALTH_REGENERATION,
                parts(MechanicalPartType.COOLING, MechanicalPartType.SHELL));
        // 亡灵：坚韧防御 + 凋零攻击 → 外壳（+模块）
        register("akaishi:undead", c(0, 2, 1, -1, 0, 2, 0, 0, 0, 0), MechanicalSpecialEffect.WITHER_ATTACK,
                parts(MechanicalPartType.SHELL, MechanicalPartType.MODULE));
        // 爆炸：爆破功能特化 → 模块
        register("akaishi:explosive", c(0, -1, 2, 0, 0, 0, 0, 2, 0, 0), MechanicalSpecialEffect.EXPLOSION_RESIST,
                parts(MechanicalPartType.MODULE));
        // 异变：全能偏攻 → 模块
        register("akaishi:aberration", c(0, 0, 1, 1, 0, 1, 1, 0, 0, 0), MechanicalSpecialEffect.POISON_RESIST,
                parts(MechanicalPartType.MODULE));
        // 末影：瞬移/感知 → 核心
        register("akaishi:ender", c(0, -1, 0, 0, 2, 0, 0, 0, 1, 2), MechanicalSpecialEffect.TELEPORT_COOLDOWN,
                parts(MechanicalPartType.CORE));
        // 首领：压迫感/生命护甲 → 核心（+外壳）
        register("akaishi:boss", c(0, 2, 2, 0, -1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.KNOCKBACK_RESIST,
                parts(MechanicalPartType.CORE, MechanicalPartType.SHELL));
        // 龙族：倍率/火焰 → 模块（+外壳）
        register("akaishi:dragon", c(2, 2, 2, 0, -1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.FIRE_ATTACK,
                parts(MechanicalPartType.MODULE, MechanicalPartType.SHELL));

        // ---- 实体级（具体生物样本优先命中）----
        // 骷髅：精准远程（感知） → 核心
        register("akaishi:skeleton", c(0, -1, 0, 0, 0, 0, 2, 1, 1, 0), MechanicalSpecialEffect.CRITICAL_BOOST,
                parts(MechanicalPartType.CORE));
        // 蜘蛛：敏捷攀爬（神经） → 核心
        register("akaishi:spider", c(0, 0, 0, 1, 2, 0, 0, 0, 0, 1), MechanicalSpecialEffect.NONE,
                parts(MechanicalPartType.CORE));
        // 苦力怕：爆破 → 模块
        register("akaishi:creeper", c(0, 0, 1, 0, 0, 1, 0, 1, 0, 0), MechanicalSpecialEffect.EXPLOSION_RESIST,
                parts(MechanicalPartType.MODULE));
        // 蜜蜂：迅捷毒刺 → 模块
        register("akaishi:bee", c(0, 0, 0, 1, 1, 0, 0, 0, 0, 1), MechanicalSpecialEffect.POISON_RESIST,
                parts(MechanicalPartType.MODULE));
        // 僵尸：不死坚韧 → 外壳
        register("akaishi:zombie", c(0, 2, 1, -1, 0, 0, 0, 0, 0, 0), MechanicalSpecialEffect.LOW_HEALTH_REGENERATION,
                parts(MechanicalPartType.SHELL));
        // 烈焰人：烈焰爆发 → 模块
        register("akaishi:blaze", c(1, -1, 2, 0, -1, 0, 0, 1, 0, 0), MechanicalSpecialEffect.FIRE_ATTACK,
                parts(MechanicalPartType.MODULE));
        // 海豚：水生灵巧（代谢） → 散热
        register("akaishi:dolphin", c(0, 1, 0, 0, 2, 0, 0, 0, 0, 0), MechanicalSpecialEffect.UNDERWATER_SPEED,
                parts(MechanicalPartType.COOLING));
        // 猪灵：贪婪金装（防护共鸣） → 外壳
        register("akaishi:piglin", c(0, 0, 1, 0, 1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.GOLD_ARMOR_BONUS,
                parts(MechanicalPartType.SHELL));
        // 铁傀儡：重装铁壁 → 外壳
        register("akaishi:iron_golem", c(0, 2, 0, -1, -1, 2, 0, 0, 0, 0), MechanicalSpecialEffect.KNOCKBACK_RESIST,
                parts(MechanicalPartType.SHELL));
        // 末影人：空间闪现（感知） → 核心
        register("akaishi:enderman", c(1, 0, 0, 0, 1, 0, 0, 0, 1, 1), MechanicalSpecialEffect.TELEPORT_COOLDOWN,
                parts(MechanicalPartType.CORE));
        // 凋灵骷髅：凋零斩击 → 模块
        register("akaishi:wither_skeleton", c(1, -1, 1, 1, 0, 0, 0, 0, 0, 0), MechanicalSpecialEffect.WITHER_ATTACK,
                parts(MechanicalPartType.MODULE));

        // ---- T7 Stage 2 实体级扩展（12 个，各带不同的十维修正 + 一个镜像生物侧的效果）----
        // 循声守卫：声感/压迫 → 核心（暗视感知）
        register("akaishi:warden", c(1, 1, 2, 0, 0, 0, 0, 0, 1, 0), MechanicalSpecialEffect.NIGHT_VISION,
                parts(MechanicalPartType.CORE));
        // 凋灵：亡灵主宰 → 外壳/模块（负面减免）
        register("akaishi:wither", c(1, 2, 1, 0, 0, 1, 0, 0, 0, 0), MechanicalSpecialEffect.DEBUFF_RESIST,
                parts(MechanicalPartType.SHELL, MechanicalPartType.MODULE));
        // 恶魂：浮空火球 → 外壳/模块（浮空免摔）
        register("akaishi:ghast", c(1, 2, 0, -1, 0, 1, 0, 0, 1, 0), MechanicalSpecialEffect.FALL_IMMUNE,
                parts(MechanicalPartType.SHELL, MechanicalPartType.MODULE));
        // 史莱姆：黏液缓冲 → 外壳/散热（弹跳免摔）
        register("akaishi:slime", c(0, 2, 0, 0, 1, 1, 0, 0, 0, 0), MechanicalSpecialEffect.FALL_IMMUNE,
                parts(MechanicalPartType.SHELL, MechanicalPartType.COOLING));
        // 女巫：炼药采集 → 模块（自动拾取）
        register("akaishi:witch", c(0, 0, 1, 0, 0, 0, 1, 1, 1, 0), MechanicalSpecialEffect.AUTO_PICKUP,
                parts(MechanicalPartType.MODULE));
        // 守卫者：水生守卫 → 模块/外壳（水下呼吸）
        register("akaishi:guardian", c(0, 0, 2, 1, 0, 1, 0, 0, 1, 0), MechanicalSpecialEffect.WATER_BREATHING,
                parts(MechanicalPartType.MODULE, MechanicalPartType.SHELL));
        // 幻翼：夜行俯冲 → 核心（夜视）
        register("akaishi:phantom", c(0, 0, 1, 0, 2, 0, 1, 0, 0, 1), MechanicalSpecialEffect.NIGHT_VISION,
                parts(MechanicalPartType.CORE));
        // 劫掠兽：冲撞撞飞 → 模块/外壳（击退目标）
        register("akaishi:ravager", c(1, 1, 2, 1, 0, 0, 0, 0, 0, 0), MechanicalSpecialEffect.KNOCKBACK_ON_HIT,
                parts(MechanicalPartType.MODULE, MechanicalPartType.SHELL));
        // 疣猪兽：兽性冲撞 → 模块（击退目标）
        register("akaishi:hoglin", c(0, 1, 2, 0, 1, 0, 0, 0, 0, 0), MechanicalSpecialEffect.KNOCKBACK_ON_HIT,
                parts(MechanicalPartType.MODULE));
        // 炽足兽：熔岩耐受 → 散热/外壳（负面减免）
        register("akaishi:strider", c(0, 1, 0, 0, 2, 1, 0, 0, 0, 0), MechanicalSpecialEffect.DEBUFF_RESIST,
                parts(MechanicalPartType.COOLING, MechanicalPartType.SHELL));
        // 北极熊：抗寒坚体 → 外壳/散热（负面减免）
        register("akaishi:polar_bear", c(0, 2, 1, 0, 0, 2, 0, 0, 0, 0), MechanicalSpecialEffect.DEBUFF_RESIST,
                parts(MechanicalPartType.SHELL, MechanicalPartType.COOLING));
        // 美西螈：两栖再生 → 散热（水下呼吸）
        register("akaishi:axolotl", c(1, 2, 0, 0, 1, 0, 0, 0, 0, 1), MechanicalSpecialEffect.WATER_BREATHING,
                parts(MechanicalPartType.COOLING));

        // ---- 机制型基因（自带负面代价；非实体/分组来源，暂无可达采集路径 ⇒ 见报告"未决"）----
        // 过热核心：血量越低攻击越高（分段爬升），代价持续掉血 → 模块
        register("akaishi:overheat_core", c(0, -1, 2, 0, 0, 0, 0, 1, 0, 0), MechanicalSpecialEffect.OVERHEAT_CORE,
                parts(MechanicalPartType.MODULE), true);
        // 寄生共生：击杀回血，代价饥饿消耗加速 → 散热
        register("akaishi:parasitic_symbiosis", c(0, 1, 1, 0, 0, 0, 0, 0, 0, 1), MechanicalSpecialEffect.PARASITIC_SYMBIOSIS,
                parts(MechanicalPartType.COOLING), true);
        // 回声定位：周期标记周围生物，代价受爆炸伤害加重 → 核心
        register("akaishi:echolocation", c(0, -1, 0, 0, 0, 0, 1, 0, 2, 1), MechanicalSpecialEffect.ECHOLOCATION,
                parts(MechanicalPartType.CORE), true);
        // 装甲过载：临时提升护甲，代价移动速度下降 → 外壳
        register("akaishi:armor_overload", c(0, 1, 0, 0, -1, 2, 0, 0, 0, 0), MechanicalSpecialEffect.ARMOR_OVERLOAD,
                parts(MechanicalPartType.SHELL), true);
        // 神经痉挛：触发时下一击必定暴击，代价期间攻击间隔变长 → 核心
        register("akaishi:neural_spasm", c(0, -1, 1, 0, 0, 0, 2, 0, 0, 0), MechanicalSpecialEffect.NEURAL_SPASM,
                parts(MechanicalPartType.CORE), true);
        // 代谢透支：生命恢复提速，代价恢复期间附带虚弱 → 散热
        register("akaishi:metabolic_overdraft", c(0, 2, -1, 0, 1, 0, 0, 0, 0, 0), MechanicalSpecialEffect.METABOLIC_OVERDRAFT,
                parts(MechanicalPartType.COOLING), true);
    }

    /** 部件集合快捷构造（至少一个参数） */
    private static Set<MechanicalPartType> parts(MechanicalPartType... types) {
        return EnumSet.copyOf(Arrays.asList(types));
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
