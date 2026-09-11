package com.example.akaishi.life.mechanical;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffect;

import java.util.List;

/**
 * 机械器官/义体的聚合最终属性。
 * 由组装加工台在四个部件的基础上计算得出。
 * 值按 {@link MechanicalProperty#ordinal()} 索引存储，含倍率轴与九属性。
 *
 * 整合度折扣：通过 {@link #applyIntegration(double)} 获取整合后的实际生效值。
 * 0% 整合度 = 20% 生效，100% 整合度 = 100% 生效；倍率轴不受折扣影响。
 */
public class MechanicalAssembledStats {

    private final MechanicalOrganType organType;
    /** 各属性满整合值（含倍率轴），按 MechanicalProperty 序数索引 */
    private final double[] values;
    private final List<String> synergyBonuses; // 已触发的协同加成描述键
    private final List<IMechanicalDnaEffect> effects; // 激活的特殊效果（内置 + 附属）

    public MechanicalAssembledStats(MechanicalOrganType organType, double[] values,
                                    List<String> synergyBonuses,
                                    List<IMechanicalDnaEffect> effects) {
        this.organType = organType;
        this.values = values;
        this.synergyBonuses = synergyBonuses;
        this.effects = effects;
    }

    public MechanicalOrganType organType() { return organType; }
    public List<String> synergyBonuses() { return synergyBonuses; }
    public List<IMechanicalDnaEffect> effects() { return effects; }

    /** 获取指定属性的满整合值 */
    public double get(MechanicalProperty prop) {
        return values[prop.ordinal()];
    }

    /** 总体倍率系数（1.0 起的乘算基础） */
    public double overallMultiplier() {
        return values[MechanicalProperty.OVERALL_MULTIPLIER.ordinal()];
    }

    /**
     * 应用整合度折扣，返回整合后的实际生效值副本。
     * 整合度决定机械器官的实际生效比例，与生物排异不同，整合度只涨不降。
     *
     * @param integrationMultiplier 整合度系数，0.2~1.0（由 MechanicalIntegrationService 提供）
     * @return 整合后的实际生效属性
     */
    public MechanicalAssembledStats applyIntegration(double integrationMultiplier) {
        double[] scaled = new double[values.length];
        for (MechanicalProperty prop : MechanicalProperty.values()) {
            // 倍率轴是乘算基础，不受整合度折扣
            scaled[prop.ordinal()] = prop == MechanicalProperty.OVERALL_MULTIPLIER
                    ? values[prop.ordinal()]
                    : values[prop.ordinal()] * integrationMultiplier;
        }
        return new MechanicalAssembledStats(organType, scaled, synergyBonuses, effects);
    }

    /**
     * 获取整合后的实际属性值。总体倍率不受整合度影响。
     *
     * @param prop 属性类型
     * @param integrationMultiplier 整合度系数 0.2~1.0
     * @return 整合后的实际值
     */
    public double getEffective(MechanicalProperty prop, double integrationMultiplier) {
        if (prop == MechanicalProperty.OVERALL_MULTIPLIER) {
            return values[prop.ordinal()];
        }
        return values[prop.ordinal()] * integrationMultiplier;
    }
}
