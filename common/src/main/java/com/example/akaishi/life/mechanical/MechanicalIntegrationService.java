package com.example.akaishi.life.mechanical;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalIntegration;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartType;

import java.util.*;

/**
 * 机械整合度服务：负责整合度的 tick 增长计算和修正。
 * 与生物排异不同，整合度只涨不降，且不会导致器官失效。
 *
 * 增长公式：
 *   growthPerTick = BASE_GROWTH
 *     × heartModifier（机械心脏加速）
 *     × dnaMatchModifier（DNA与玩家身体匹配）
 *     × sameSourceModifier（同源加成）
 *     × materialBaseModifier（材料基础整合度）
 */
public final class MechanicalIntegrationService {

    private MechanicalIntegrationService() {}

    /** 基础增长速率：每 tick 0.0001 = 1% 每 100 tick ≈ 0.5%/s */
    private static final double BASE_GROWTH = 0.0001;
    /** 无机械心脏时的减速系数 */
    private static final double NO_HEART_PENALTY = 0.5;
    /** 有机械心脏时的加速系数 */
    private static final double HEART_BOOST = 1.5;
    /** DNA 匹配加速系数 */
    private static final double DNA_MATCH_BOOST = 1.5;
    /** 全身机械件同源加速系数 */
    private static final double SAME_SOURCE_BOOST = 1.3;

    /**
     * 执行一次整合度 tick 增长。
     * 应在玩家 tick 事件中调用（每秒约 20 次）。
     *
     * @param integration 整合度数据
     * @param installedSlots 当前已安装机械器官的槽位列表
     * @param hasMechanicalHeart 是否安装了机械心脏
     * @param slotToTemplate 槽位 → 机械部件模板映射（用于读取材料/DNA）
     * @param slotToOrganType 槽位 → 机械器官类型映射
     */
    public static void tick(MechanicalIntegration integration,
                            Collection<BodySlot> installedSlots,
                            boolean hasMechanicalHeart,
                            Map<BodySlot, MechanicalPartTemplate> slotToTemplate,
                            Map<BodySlot, MechanicalOrganType> slotToOrganType) {
        for (BodySlot slot : installedSlots) {
            if (integration.get(slot) >= MechanicalIntegration.MAX_INTEGRATION) {
                continue;
            }

            double growth = BASE_GROWTH;

            // 机械心脏修正
            growth *= hasMechanicalHeart ? HEART_BOOST : NO_HEART_PENALTY;

            // 材料基础整合度修正
            MechanicalPartTemplate template = slotToTemplate.get(slot);
            if (template != null) {
                growth *= getMaterialGrowthModifier(template.material());
            }

            // 同源修正
            if (isAllSameSource(installedSlots, slotToTemplate)) {
                growth *= SAME_SOURCE_BOOST;
            }

            // 浮点累加：累积满 1 点才消耗，避免因 growth 太小而始终为 0
            integration.tryConsumeGrowth(slot, growth);
        }
    }

    /**
     * 获取某个机械器官的初始整合度（安装时设定）。
     * 某些材料（如生命陶瓷）自带初始整合度。
     */
    public static int getInitialIntegration(MechanicalMaterial material) {
        // 材料自带的初始整合度点数
        return switch (material.id()) {
            case "akaishi:bio_ceramic" -> 20;  // 生命陶瓷，生物兼容性最好
            case "akaishi:psionic_composite" -> 10; // 灵能复合，兼容性良好
            case "akaishi:refined_core" -> 0;  // 赤石精炼核心，极端但难适配
            default -> 0;
        };
    }

    /**
     * 计算整合度对面板属性的折扣系数。
     * 0% 整合度 = 20% 生效，100% 整合度 = 100% 生效。
     */
    public static double getEffectiveMultiplier(int integrationValue) {
        return MechanicalIntegration.MIN_EFFECTIVE_RATIO
                + integrationValue * (1.0 - MechanicalIntegration.MIN_EFFECTIVE_RATIO)
                        / MechanicalIntegration.MAX_INTEGRATION;
    }

    /**
     * 获取材料对增长速度的修正系数。
     */
    private static double getMaterialGrowthModifier(MechanicalMaterial material) {
        return switch (material.id()) {
            case "akaishi:bio_ceramic" -> 1.3;   // 生物兼容材料，整合最快
            case "akaishi:psionic_composite" -> 1.2; // 灵能复合，整合较快
            case "akaishi:refined_core" -> 0.7;  // 极端材料，整合最慢
            case "akaishi:alloy_steel" -> 0.9;   // 重型材料，稍慢
            default -> 1.0;                       // 标准材料
        };
    }

    /**
     * 检查所有机械器官的 DNA 来源是否相同且不为 NONE。
     */
    private static boolean isAllSameSource(Collection<BodySlot> installedSlots,
                                            Map<BodySlot, MechanicalPartTemplate> slotToTemplate) {
        Set<String> dnaIds = new HashSet<>();
        for (BodySlot slot : installedSlots) {
            MechanicalPartTemplate template = slotToTemplate.get(slot);
            if (template == null) return false;
            String dnaId = template.dnaProfile().id();
            if ("akaishi:none".equals(dnaId)) return false;
            dnaIds.add(dnaId);
        }
        return dnaIds.size() == 1;
    }
}