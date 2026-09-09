package com.example.akaishi.life.mechanical;

import java.util.*;

/**
 * 机械器官解析器：负责权重聚合、协同检测、最终属性计算。
 * 核心输入：四个部件的模板（或组装后的成品NBT）。
 * 核心输出：MechanicalAssembledStats。
 *
 * 聚合公式：各属性按部件专业系数加权求和，再乘以总体倍率系数。
 * 协同检测：当四个部件的权重分布形成特定模式时触发额外加成。
 */
public final class MechanicalOrganResolver {

    private MechanicalOrganResolver() {}

    // ==================== 聚合系数 ====================
    // 每个属性 × 每个部件类型的加权系数，突出各部件在该属性上的专业度

    /** 总体倍率聚合系数 */
    private static final double[] OM_COEFF = {0.5, 0.4, 0.3, 0.2}; // CORE, MODULE, SHELL, COOLING
    /** 生命值聚合系数 */
    private static final double[] HP_COEFF = {0.5, 0.3, 1.0, 0.3};
    /** 攻击伤害聚合系数 */
    private static final double[] AD_COEFF = {1.0, 0.8, 0.2, 0.1};
    /** 攻击速度聚合系数 */
    private static final double[] AS_COEFF = {0.8, 0.8, 0.2, 0.3};
    /** 移动速度聚合系数 */
    private static final double[] MS_COEFF = {0.6, 0.7, 0.3, 0.5};

    /** 部件按顺序索引 */
    private static final MechanicalPartType[] PART_ORDER = {
            MechanicalPartType.CORE, MechanicalPartType.MODULE,
            MechanicalPartType.SHELL, MechanicalPartType.COOLING
    };

    /**
     * 从四个部件模板计算最终属性。
     *
     * @param organType 器官类型
     * @param templates 四个部件模板（按 CORE, MODULE, SHELL, COOLING 顺序）
     * @return 聚合后的最终属性
     * @throws IllegalArgumentException 如果模板数量不为4或器官类型不匹配
     */
    public static MechanicalAssembledStats resolve(MechanicalOrganType organType,
                                                    List<MechanicalPartTemplate> templates) {
        if (templates.size() != 4) {
            throw new IllegalArgumentException("Need exactly 4 part templates, got " + templates.size());
        }

        // 验证器官类型一致性
        for (MechanicalPartTemplate t : templates) {
            if (t.organType() != organType) {
                throw new IllegalArgumentException(
                        "Organ type mismatch: expected " + organType + ", got " + t.organType());
            }
        }

        // 提取各部件权重
        MechanicalPartWeight[] weights = new MechanicalPartWeight[4];
        for (int i = 0; i < 4; i++) {
            weights[i] = templates.get(i).totalWeight();
        }

        // 计算聚合权重
        double rawOm = aggregate(weights, OM_COEFF, MechanicalProperty.OVERALL_MULTIPLIER);
        double rawHp = aggregate(weights, HP_COEFF, MechanicalProperty.HEALTH);
        double rawAd = aggregate(weights, AD_COEFF, MechanicalProperty.ATTACK_DAMAGE);
        double rawAs = aggregate(weights, AS_COEFF, MechanicalProperty.ATTACK_SPEED);
        double rawMs = aggregate(weights, MS_COEFF, MechanicalProperty.MOVEMENT_SPEED);

        // 检测协同加成
        SynergyResult synergy = detectSynergy(templates, weights);

        // 应用协同加成
        double hp = rawHp * (1.0 + synergy.healthBonus);
        double ad = rawAd * (1.0 + synergy.attackDamageBonus);
        double as = rawAs * (1.0 + synergy.attackSpeedBonus);
        double ms = rawMs * (1.0 + synergy.movementSpeedBonus);

        // 计算总体倍率系数：1.0 + (聚合权重 ÷ 10)
        // 总体倍率不受协同加成影响（它是乘算基础）
        double overallMultiplier = 1.0 + (rawOm / 10.0);

        // 收集所有DNA特殊效果
        List<MechanicalSpecialEffect> effects = collectEffects(templates);

        return new MechanicalAssembledStats(
                organType,
                overallMultiplier, hp, ad, as, ms,
                synergy.synergyKeys, effects
        );
    }

    /** 聚合单个属性的加权和 */
    private static double aggregate(MechanicalPartWeight[] weights, double[] coeffs,
                                     MechanicalProperty prop) {
        double sum = 0;
        for (int i = 0; i < 4; i++) {
            sum += weights[i].get(prop) * coeffs[i];
        }
        return sum;
    }

    // ==================== 协同检测 ====================

    /**
     * 检测四个部件的权重分布是否形成特定协同模式。
     * 返回触发协同加成描述键列表和百分比加成。
     */
    private static SynergyResult detectSynergy(List<MechanicalPartTemplate> templates,
                                                MechanicalPartWeight[] weights) {
        List<String> keys = new ArrayList<>();
        double hpBonus = 0, adBonus = 0, asBonus = 0, msBonus = 0;

        // 1. 均衡型：四项攻击伤害都在2~4之间
        boolean allBalanced = true;
        for (int i = 0; i < 4; i++) {
            int ad = weights[i].attackDamage();
            if (ad < 2 || ad > 4) { allBalanced = false; break; }
        }
        if (allBalanced) {
            keys.add("mechanical.synergy.balanced");
            adBonus += 0.05;
        }

        // 2. 极端型：某项≥5，其余≤2（检查攻击伤害）
        for (int i = 0; i < 4; i++) {
            int ad = weights[i].attackDamage();
            if (ad >= 5) {
                boolean othersLow = true;
                for (int j = 0; j < 4; j++) {
                    if (i != j && weights[j].attackDamage() > 2) {
                        othersLow = false;
                        break;
                    }
                }
                if (othersLow) {
                    keys.add("mechanical.synergy.extreme");
                    adBonus += 0.10;
                    // 其他属性-5%
                    hpBonus -= 0.05;
                    asBonus -= 0.05;
                    msBonus -= 0.05;
                    break;
                }
            }
        }

        // 3. 铁壁：四项生命值都≥3
        boolean allHp = true;
        for (int i = 0; i < 4; i++) {
            if (weights[i].health() < 3) { allHp = false; break; }
        }
        if (allHp) {
            keys.add("mechanical.synergy.iron_wall");
            hpBonus += 0.10;
        }

        // 4. 疾风：四项移动速度都≥3
        boolean allMs = true;
        for (int i = 0; i < 4; i++) {
            if (weights[i].movementSpeed() < 3) { allMs = false; break; }
        }
        if (allMs) {
            keys.add("mechanical.synergy.wind");
            msBonus += 0.10;
        }

        // 5. 同源：四个部件DNA来源相同且不为NONE
        Set<MechanicalDnaProfile> dnaSet = new HashSet<>();
        for (MechanicalPartTemplate t : templates) {
            dnaSet.add(t.dnaProfile());
        }
        if (dnaSet.size() == 1) {
            MechanicalDnaProfile dna = dnaSet.iterator().next();
            if (!"akaishi:none".equals(dna.id())) {
                keys.add("mechanical.synergy.same_source");
                hpBonus += 0.05;
                adBonus += 0.05;
                asBonus += 0.05;
                msBonus += 0.05;
            }
        }

        return new SynergyResult(keys, hpBonus, adBonus, asBonus, msBonus);
    }

    /** 收集所有部件的DNA特殊效果，去重 */
    private static List<MechanicalSpecialEffect> collectEffects(
            List<MechanicalPartTemplate> templates) {
        Set<MechanicalSpecialEffect> set = new LinkedHashSet<>();
        for (MechanicalPartTemplate t : templates) {
            if (t.dnaProfile().effect() != MechanicalSpecialEffect.NONE) {
                set.add(t.dnaProfile().effect());
            }
        }
        return List.copyOf(set);
    }

    /** 协同检测结果 */
    private record SynergyResult(
            List<String> synergyKeys,
            double healthBonus,
            double attackDamageBonus,
            double attackSpeedBonus,
            double movementSpeedBonus
    ) {}
}