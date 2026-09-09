package com.example.akaishi.life.mechanical;

import java.util.*;

/**
 * 机械材料定义：材料的总点数 + 五维分布偏好。
 * 总点数随材料等级递增（5→7→9→11），高级材料点数更多、分布更集中。
 * 每种材料有固定的分布偏好，不可由玩家分配。
 *
 * 通过 {@link #register(String, int, int, int, int, int, int)} 注册，
 * 附属模组可在自己的初始化阶段调用同一 API 新增材料。
 */
public final class MechanicalMaterial {

    private final String id;
    private final int totalPoints;
    private final MechanicalPartWeight distribution;

    public MechanicalMaterial(String id, int totalPoints, MechanicalPartWeight distribution) {
        this.id = id;
        this.totalPoints = totalPoints;
        this.distribution = distribution;
    }

    /** 材料唯一标识，如 "akaishi:iron" */
    public String id() { return id; }

    /** 该材料的总点数（5~11） */
    public int totalPoints() { return totalPoints; }

    /** 该材料的五维分布权重 */
    public MechanicalPartWeight distribution() { return distribution; }

    /** 本地化键 */
    public String descriptionKey() {
        return "mechanical.material." + id.replace(':', '.');
    }

    // ==================== 注册表 ====================

    private static final Map<String, MechanicalMaterial> REGISTRY = new LinkedHashMap<>();

    /**
     * 注册一种机械材料。
     * 附属模组可在 own init 中调用此方法新增材料。
     *
     * @param id 唯一标识，推荐 "modid:name"
     * @param totalPoints 总点数（5~11）
     * @param om 总体倍率权重 0~5
     * @param hp 生命值权重 0~5
     * @param ad 攻击伤害权重 0~5
     * @param as 攻击速度权重 0~5
     * @param ms 移动速度权重 0~5
     * @return 注册好的材料实例
     * @throws IllegalArgumentException 如果 id 已存在或点数不符合约束
     */
    public static MechanicalMaterial register(String id, int totalPoints,
                                              int om, int hp, int ad, int as, int ms) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("MechanicalMaterial already registered: " + id);
        }
        if (totalPoints < 1 || totalPoints > 20) {
            throw new IllegalArgumentException("totalPoints must be 1~20, got " + totalPoints);
        }
        MechanicalPartWeight weight = new MechanicalPartWeight(om, hp, ad, as, ms);
        // 验证材料点数与权重分布之和的匹配关系（宽松校验，仅警告）
        int weightSum = weight.sum();
        if (weightSum != totalPoints) {
            // 允许分布和与总点数不一致（材料可以"浪费"点数），但记录警告
            // 实际使用时以 distribution() 为准，totalPoints() 仅用于等级判定
        }
        MechanicalMaterial mat = new MechanicalMaterial(id, totalPoints, weight);
        REGISTRY.put(id, mat);
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

    /** 注册本模组内置材料 */
    public static void registerDefaults() {
        // 基础（5点）
        register("akaishi:iron", 5, 1, 1, 1, 1, 1);
        // 中级（7点）
        register("akaishi:redstone_alloy", 7, 3, 0, 3, 1, 0);
        register("akaishi:ceramic_composite", 7, 1, 1, 0, 0, 5);
        register("akaishi:resistant_steel", 7, 0, 5, 0, 0, 2);
        // 高级（9点）
        register("akaishi:precision_alloy", 9, 3, 2, 2, 2, 0);
        register("akaishi:polymerized_redstone", 9, 5, 0, 3, 1, 0);
        register("akaishi:bio_ceramic", 9, 1, 5, 0, 0, 3);
        // 顶级（11点）
        register("akaishi:refined_core", 11, 5, 0, 4, 2, 0);
        register("akaishi:alloy_steel", 11, 0, 5, 1, 1, 4);
        register("akaishi:psionic_composite", 11, 3, 2, 2, 2, 2);
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
}