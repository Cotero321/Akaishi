package com.example.akaishi.life.mechanical;

import java.util.*;

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

    public MechanicalMaterial(String id, int totalPoints, MechanicalPartWeight distribution) {
        this.id = id;
        this.totalPoints = totalPoints;
        this.distribution = distribution;
    }

    /** 材料唯一标识，如 "akaishi:iron" */
    public String id() { return id; }

    /** 该材料的总点数（5~11） */
    public int totalPoints() { return totalPoints; }

    /** 该材料的十维分布权重（{@link MechanicalProperty} 顺序） */
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
     * @param totalPoints 总点数（1~20，用于等级分组）
     * @param distribution 十维分布权重（每项 0~5，建议各项之和 == totalPoints）
     * @return 注册好的材料实例
     * @throws IllegalArgumentException 如果 id 已存在或点数不符合约束
     */
    public static MechanicalMaterial register(String id, int totalPoints,
                                              MechanicalPartWeight distribution) {
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException("MechanicalMaterial already registered: " + id);
        }
        if (totalPoints < 1 || totalPoints > 20) {
            throw new IllegalArgumentException("totalPoints must be 1~20, got " + totalPoints);
        }
        MechanicalMaterial mat = new MechanicalMaterial(id, totalPoints, distribution);
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

    /** 注册本模组内置材料（十维分布，和 == 总点数） */
    public static void registerDefaults() {
        // 基础（5点）——均衡基石
        register("akaishi:iron", 5, w(1, 1, 1, 1, 0, 1, 0, 0, 0, 0));
        // 中级（7点）——单轴专精
        register("akaishi:redstone_alloy", 7, w(1, 0, 2, 3, 0, 0, 1, 0, 0, 0));      // 迅捷脉冲：攻速/攻击
        register("akaishi:ceramic_composite", 7, w(1, 0, 0, 0, 4, 0, 0, 0, 0, 2));   // 轻质机动：移速/闪避
        register("akaishi:resistant_steel", 7, w(1, 3, 0, 0, 0, 3, 0, 0, 0, 0));     // 重装卸力：生命/护甲
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
