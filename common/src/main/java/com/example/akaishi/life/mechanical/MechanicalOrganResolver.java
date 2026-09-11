package com.example.akaishi.life.mechanical;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffect;

import java.util.*;

/**
 * 机械器官解析器：负责权重聚合、协同检测、最终属性计算。
 * 核心输入：四个部件的模板（或组装后的成品NBT）。
 * 核心输出：MechanicalAssembledStats。
 *
 * 聚合公式：各属性按部件专业系数加权求和，再乘以协同加成；
 * 总体倍率系数 = 1.0 + 聚合倍率权重/10，不受协同与整合度影响。
 * 协同检测：当四个部件的权重分布形成特定模式时触发额外加成。
 */
public final class MechanicalOrganResolver {

    private MechanicalOrganResolver() {}

    // ==================== 聚合系数 ====================
    // 下标顺序：CORE, MODULE, SHELL, COOLING；突出各部件在该属性上的专业度

    /** 属性 → 四部件聚合系数（按 {@link MechanicalProperty#ordinal()} 索引） */
    private static final double[][] COEFFS = new double[MechanicalProperty.COUNT][];

    static {
        setCoeffs(MechanicalProperty.OVERALL_MULTIPLIER, 0.5, 0.4, 0.3, 0.2);
        setCoeffs(MechanicalProperty.HEALTH, 0.5, 0.3, 1.0, 0.3);
        setCoeffs(MechanicalProperty.ATTACK_DAMAGE, 1.0, 0.8, 0.2, 0.1);
        setCoeffs(MechanicalProperty.ATTACK_SPEED, 0.8, 0.8, 0.2, 0.3);
        setCoeffs(MechanicalProperty.MOVEMENT_SPEED, 0.6, 0.7, 0.3, 0.5);
        setCoeffs(MechanicalProperty.ARMOR, 0.3, 0.3, 1.0, 0.4);
        setCoeffs(MechanicalProperty.CRIT_CHANCE, 0.5, 1.0, 0.3, 0.2);
        setCoeffs(MechanicalProperty.CRIT_DAMAGE, 1.0, 0.4, 0.2, 0.2);
        setCoeffs(MechanicalProperty.RANGE, 0.4, 1.0, 0.2, 0.3);
        setCoeffs(MechanicalProperty.DODGE, 0.7, 0.4, 0.3, 1.0);
    }

    private static void setCoeffs(MechanicalProperty prop, double core, double module,
                                  double shell, double cooling) {
        COEFFS[prop.ordinal()] = new double[]{core, module, shell, cooling};
    }

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

        // 检测协同加成
        SynergyResult synergy = detectSynergy(templates, weights);

        double[] values = new double[MechanicalProperty.COUNT];
        for (MechanicalProperty prop : MechanicalProperty.values()) {
            double raw = aggregate(weights, COEFFS[prop.ordinal()], prop);
            if (prop == MechanicalProperty.OVERALL_MULTIPLIER) {
                // 总体倍率：1.0 + 聚合权重/10，不受协同加成影响（它是乘算基础）
                values[prop.ordinal()] = 1.0 + raw / 10.0;
            } else {
                values[prop.ordinal()] = raw * (1.0 + synergy.bonuses[prop.ordinal()]);
            }
        }

        // 收集所有DNA特殊效果
        List<IMechanicalDnaEffect> effects = collectEffects(templates);

        return new MechanicalAssembledStats(organType, values, synergy.synergyKeys, effects);
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
     * 返回触发协同加成描述键列表与各属性百分比加成（按 {@link MechanicalProperty#ordinal()} 索引）。
     */
    private static SynergyResult detectSynergy(List<MechanicalPartTemplate> templates,
                                                MechanicalPartWeight[] weights) {
        List<String> keys = new ArrayList<>();
        double[] bonuses = new double[MechanicalProperty.COUNT];

        // 1. 均衡型：四项攻击伤害都在2~4之间
        boolean allBalanced = true;
        for (int i = 0; i < 4; i++) {
            int ad = weights[i].get(MechanicalProperty.ATTACK_DAMAGE);
            if (ad < 2 || ad > 4) { allBalanced = false; break; }
        }
        if (allBalanced) {
            keys.add("mechanical.synergy.balanced");
            bonuses[MechanicalProperty.ATTACK_DAMAGE.ordinal()] += 0.05;
        }

        // 2. 极端型：某项≥5，其余≤2（检查攻击伤害）
        for (int i = 0; i < 4; i++) {
            int ad = weights[i].get(MechanicalProperty.ATTACK_DAMAGE);
            if (ad >= 5) {
                boolean othersLow = true;
                for (int j = 0; j < 4; j++) {
                    if (i != j && weights[j].get(MechanicalProperty.ATTACK_DAMAGE) > 2) {
                        othersLow = false;
                        break;
                    }
                }
                if (othersLow) {
                    keys.add("mechanical.synergy.extreme");
                    bonuses[MechanicalProperty.ATTACK_DAMAGE.ordinal()] += 0.10;
                    // 其他属性-5%
                    bonuses[MechanicalProperty.HEALTH.ordinal()] -= 0.05;
                    bonuses[MechanicalProperty.ATTACK_SPEED.ordinal()] -= 0.05;
                    bonuses[MechanicalProperty.MOVEMENT_SPEED.ordinal()] -= 0.05;
                    break;
                }
            }
        }

        // 3. 铁壁：四项生命值都≥3（生命/护甲同涨）
        boolean allHp = true;
        for (int i = 0; i < 4; i++) {
            if (weights[i].get(MechanicalProperty.HEALTH) < 3) { allHp = false; break; }
        }
        if (allHp) {
            keys.add("mechanical.synergy.iron_wall");
            bonuses[MechanicalProperty.HEALTH.ordinal()] += 0.10;
            bonuses[MechanicalProperty.ARMOR.ordinal()] += 0.10;
        }

        // 4. 疾风：四项移动速度都≥3（移速/闪避同涨）
        boolean allMs = true;
        for (int i = 0; i < 4; i++) {
            if (weights[i].get(MechanicalProperty.MOVEMENT_SPEED) < 3) { allMs = false; break; }
        }
        if (allMs) {
            keys.add("mechanical.synergy.wind");
            bonuses[MechanicalProperty.MOVEMENT_SPEED.ordinal()] += 0.10;
            bonuses[MechanicalProperty.DODGE.ordinal()] += 0.10;
        }

        // 5. 同源：四个部件DNA来源相同且不为NONE（九属性全体 +5%）
        Set<MechanicalDnaProfile> dnaSet = new HashSet<>();
        for (MechanicalPartTemplate t : templates) {
            dnaSet.add(t.dnaProfile());
        }
        if (dnaSet.size() == 1) {
            MechanicalDnaProfile dna = dnaSet.iterator().next();
            if (!MechanicalDnaProfile.NONE_ID.equals(dna.id())) {
                keys.add("mechanical.synergy.same_source");
                for (MechanicalProperty prop : MechanicalProperty.values()) {
                    if (prop != MechanicalProperty.OVERALL_MULTIPLIER) {
                        bonuses[prop.ordinal()] += 0.05;
                    }
                }
            }
        }

        return new SynergyResult(keys, bonuses);
    }

    /** 收集所有部件的DNA特殊效果，去重（保持部件顺序；内置与附属效果一视同仁） */
    private static List<IMechanicalDnaEffect> collectEffects(
            List<MechanicalPartTemplate> templates) {
        Set<IMechanicalDnaEffect> set = new LinkedHashSet<>();
        for (MechanicalPartTemplate t : templates) {
            IMechanicalDnaEffect effect = t.dnaProfile().effect();
            if (!MechanicalSpecialEffect.isNone(effect)) {
                set.add(effect);
            }
        }
        return List.copyOf(set);
    }

    /** 协同检测结果：描述键 + 各属性百分比加成（按属性序数索引） */
    private record SynergyResult(List<String> synergyKeys, double[] bonuses) {}
}
