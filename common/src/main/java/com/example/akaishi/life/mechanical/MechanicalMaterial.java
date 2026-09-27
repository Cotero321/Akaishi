package com.example.akaishi.life.mechanical;

import com.example.akaishi.api.mechanical.trait.IMechanicalTrait;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitRegistry;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 机械材料定义：材料的总点数 + 十维分布偏好。
 * 总点数随材料等级递增（5→7→9→11），高级材料点数更多、分布更集中。
 * 每种材料有固定的分布偏好，不可由玩家分配。
 *
 * 材料分布与器官无关（任何槽位皆可承载），叠加到部件基础权重上后再逐项 clamp 0~5。
 * 通过 {@link #register(String, int, MechanicalPartWeight)} 注册，
 * 附属模组可在自己的初始化阶段调用同一 API 新增材料。
 */
public final class MechanicalMaterial {

    private final String id;
    private final int totalPoints;
    private final MechanicalPartWeight distribution;
    /**
     * 本材料携带的特性 ID 列表（可空）。
     * <p>
     * 特性是「挂在材料上」的能力标签，与 DNA 特殊效果互相独立；其等级由
     * 「同一器官内 4 个部件中使用该材料的次数」决定（见 {@code IMechanicalTrait}）。
     * 使用写时复制列表：注册（init 期）后不再变更，运行期读取无需加锁。
     */
    private final List<String> traitIds = new CopyOnWriteArrayList<>();

    public MechanicalMaterial(String id, int totalPoints, MechanicalPartWeight distribution) {
        this(id, totalPoints, distribution, List.of());
    }

    public MechanicalMaterial(String id, int totalPoints, MechanicalPartWeight distribution,
                              Collection<String> traits) {
        this.id = id;
        this.totalPoints = totalPoints;
        this.distribution = distribution;
        if (traits != null) {
            for (String trait : traits) {
                if (trait != null && !trait.isBlank() && !this.traitIds.contains(trait)) {
                    this.traitIds.add(trait);
                }
            }
        }
    }

    /** 材料唯一标识，如 "akaishi:iron" */
    public String id() { return id; }

    /** 该材料的总点数（5~11） */
    public int totalPoints() { return totalPoints; }

    /** 该材料的十维分布权重（{@link MechanicalProperty} 顺序） */
    public MechanicalPartWeight distribution() { return distribution; }

    /** 本材料携带的特性 ID 列表（不可变快照，可能为空） */
    public List<String> traitIds() {
        return List.copyOf(traitIds);
    }

    /** 本材料携带的特性实例（按 ID 解析，未注册的 ID 会被跳过） */
    public List<IMechanicalTrait> traits() {
        List<IMechanicalTrait> resolved = new ArrayList<>(traitIds.size());
        for (String traitId : traitIds) {
            IMechanicalTrait trait = MechanicalTraitRegistry.get(traitId);
            if (trait != null) {
                resolved.add(trait);
            }
        }
        return resolved;
    }

    /** 本材料是否携带任何特性 */
    public boolean hasTraits() {
        return !traitIds.isEmpty();
    }

    /** 本地化键 */
    public String descriptionKey() {
        return "mechanical.material." + id.replace(':', '.');
    }

    // ==================== 注册表 ====================

    private static final Map<String, MechanicalMaterial> REGISTRY = new LinkedHashMap<>();

    /**
     * 注册一种机械材料（不带特性）。
     * 附属模组可在 own init 中调用此方法新增材料。
     *
     * @param id 唯一标识，推荐 "modid:name"
     * @param totalPoints 总点数（1~20，用于等级分组）
     * @param distribution 十维分布权重（每项 0~5，建议各项之和 == totalPoints）
     * @return 注册好的材料实例
     * @throws IllegalArgumentException 如果 id 已存在或点数不符合约束
     */
    public static MechanicalMaterial register(String id, int totalPoints,
                                              MechanicalPartWeight distribution) {
        return register(id, totalPoints, distribution, List.of());
    }

    /**
     * 注册一种机械材料（携带材料特性）。
     * <p>
     * 附属模组可在 own init 中调用此方法新增"带特性"的材料；
     * 特性 ID 须已在 {@link MechanicalTraitRegistry} 注册（未注册的 ID 只展示不出效果）。
     *
     * @param id 唯一标识，推荐 "modid:name"
     * @param totalPoints 总点数（1~20，用于等级分组）
     * @param distribution 十维分布权重（每项 0~5，建议各项之和 == totalPoints）
     * @param traitIds 该材料携带的特性 ID 列表（可为空）
     * @return 注册好的材料实例
     * @throws IllegalArgumentException 如果 id 已存在或点数不符合约束
     */
    public static MechanicalMaterial register(String id, int totalPoints,
                                              MechanicalPartWeight distribution,
                                              Collection<String> traitIds) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("MechanicalMaterial already registered: " + id);
        }
        if (totalPoints < 1 || totalPoints > 20) {
            throw new IllegalArgumentException("totalPoints must be 1~20, got " + totalPoints);
        }
        MechanicalMaterial mat = new MechanicalMaterial(id, totalPoints, distribution, traitIds);
        REGISTRY.put(id, mat);
        return mat;
    }

    /**
     * 给已注册的材料追加一个特性（就地生效，不更换实例）。
     * <p>
     * 供附属模组给内置材料（如 {@code akaishi:iron}）补充特性使用。
     *
     * @param materialId 目标材料 ID（须已注册）
     * @param traitId 要追加的特性 ID（重复追加会被忽略）
     * @return 目标材料实例
     * @throws IllegalArgumentException 材料未注册
     */
    public static MechanicalMaterial addTrait(String materialId, String traitId) {
        MechanicalMaterial mat = REGISTRY.get(materialId);
        if (mat == null) {
            throw new IllegalArgumentException("MechanicalMaterial not registered: " + materialId);
        }
        if (traitId != null && !traitId.isBlank() && !mat.traitIds.contains(traitId)) {
            mat.traitIds.add(traitId);
        }
        return mat;
    }

    /** 按 ID 查找材料，不存在返回 null */
    public static MechanicalMaterial get(String id) {
        return REGISTRY.get(id);
    }

    /** 获取所有已注册材料的不可变视图 */
    public static Collection<MechanicalMaterial> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /** 注册本模组内置材料（十维分布，和 == 总点数） */
    public static void registerDefaults() {
        // 基础（5点）——均衡基石
        register("akaishi:iron", 5, w(1, 1, 1, 1, 0, 1, 0, 0, 0, 0));
        // 中级（7点）——单轴专精
        register("akaishi:redstone_alloy", 7, w(1, 0, 2, 3, 0, 0, 1, 0, 0, 0));      // 迅捷脉冲：攻速/攻击
        register("akaishi:ceramic_composite", 7, w(1, 0, 0, 0, 4, 0, 0, 0, 0, 2));   // 轻质机动：移速/闪避
        register("akaishi:resistant_steel", 7, w(1, 3, 0, 0, 0, 3, 0, 0, 0, 0));     // 重装卸力：生命/护甲
        register("akaishi:chishi", 7, w(2, 1, 1, 1, 0, 1, 1, 0, 0, 0));              // 赤石：泛用底座（中级）
        // 高级（9点）——双轴特化
        register("akaishi:precision_alloy", 9, w(2, 0, 1, 0, 0, 0, 2, 2, 2, 0));     // 精准斩杀：暴击/范围
        register("akaishi:polymerized_redstone", 9, w(3, 0, 2, 3, 0, 0, 1, 0, 0, 0)); // 超频输出：倍率/攻速
        register("akaishi:bio_ceramic", 9, w(2, 4, 0, 0, 0, 3, 0, 0, 0, 0));         // 生体装甲：生命/护甲
        // 顶级（11点）——全能顶尖
        register("akaishi:refined_core", 11, w(4, 2, 2, 2, 0, 1, 0, 0, 0, 0));       // 核心超载：倍率主导
        register("akaishi:alloy_steel", 11, w(2, 3, 1, 0, 0, 5, 0, 0, 0, 0));        // 装甲堡垒：护甲满配
        register("akaishi:psionic_composite", 11, w(2, 2, 2, 2, 1, 0, 1, 1, 0, 0));  // 灵能均衡：全轴微调
    }

    /** 获取所有材料，按等级分组（用于 GUI 展示） */
    public static Map<String, List<MechanicalMaterial>> groupByTier() {
        Map<String, List<MechanicalMaterial>> groups = new LinkedHashMap<>();
        for (MechanicalMaterial mat : REGISTRY.values()) {
            String tier = switch (mat.totalPoints) {
                case 5 -> "basic";
                case 7 -> "mid";
                case 9 -> "advanced";
                default -> "top";
            };
            groups.computeIfAbsent(tier, k -> new ArrayList<>()).add(mat);
        }
        return groups;
    }

    /** 十维分布快捷构造：倍率 / 生命 / 攻击伤害 / 攻击速度 / 移速 / 护甲 / 暴击率 / 暴击伤害 / 攻击范围 / 闪避 */
    private static MechanicalPartWeight w(int om, int hp, int ad, int as, int ms,
                                          int armor, int crit, int critDamage, int range, int dodge) {
        return new MechanicalPartWeight(om, hp, ad, as, ms, armor, crit, critDamage, range, dodge);
    }
}
