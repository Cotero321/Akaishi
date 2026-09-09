package com.example.akaishi.life.mechanical;

/**
 * 机械部件模板：记录单个部件的完整权重构成。
 * 由模板制造厂产出，是加工制作厂的输入。
 * 权重 = 基础权重 + 材料点数 + DNA修正。
 */
public class MechanicalPartTemplate {

    private final MechanicalOrganType organType;
    private final MechanicalPartType partType;
    private final MechanicalMaterial material;
    private final MechanicalDnaProfile dnaProfile;
    private final MechanicalPartWeight totalWeight;

    public MechanicalPartTemplate(MechanicalOrganType organType,
                                  MechanicalPartType partType,
                                  MechanicalMaterial material,
                                  MechanicalDnaProfile dnaProfile) {
        this.organType = organType;
        this.partType = partType;
        this.material = material;
        this.dnaProfile = dnaProfile;

        // 叠加计算最终权重
        MechanicalPartWeight base = MechanicalPartsDefinition.getBaseWeight(organType, partType);
        MechanicalPartWeight materialWeight = material.distribution();
        MechanicalPartWeight dnaCorrection = dnaProfile.corrections();

        // 基础权重（五项总和=10）+ 材料点数（5~11） + DNA修正（小范围）
        // 注意：材料总点数不直接加，而是用材料的分布权重叠加
        this.totalWeight = base.add(materialWeight).add(dnaCorrection);
    }

    public MechanicalOrganType organType() { return organType; }
    public MechanicalPartType partType() { return partType; }
    public MechanicalMaterial material() { return material; }
    public MechanicalDnaProfile dnaProfile() { return dnaProfile; }
    public MechanicalPartWeight totalWeight() { return totalWeight; }

    /** 该部件的总点数（基础10 + 材料点数 + DNA修正总和） */
    public int totalPoints() {
        return 10 + material.totalPoints() + dnaProfile.corrections().sum();
    }
}