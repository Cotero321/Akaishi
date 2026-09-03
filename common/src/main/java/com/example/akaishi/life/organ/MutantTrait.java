package com.example.akaishi.life.organ;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * 突变词条池：生命培育器对器官施加的随机基因变异（双刃剑）。
 * - rarity 1~3：词条稀有度，由培养基基因序列纯度决定解锁档位（<60 只出 1 档，60-84 出到 2 档，85+ 全解锁）
 * - dual=true 表示"畸变"：强正向收益同时背负负面代价（负属性/负面被动），培养时必须承受
 * - 属性并入 bonusesOf（与生物效果同乘品质×适配×突破倍率）；被动并入 passivesOf（常驻生效）
 * 词条一旦附加不可移除、不可覆盖，移植/摘除不影响已写入的突变。
 */
public enum MutantTrait {

    // ===== rarity 1：常规安全变异（仅正向，纯种无害） =====
    DENSE_TISSUE("dense_tissue", 1, false, List.of(new OrganTemplate.AttributeBonus(Attributes.ARMOR, 1.0)), List.of()),
    MUSCLE_FIBER("muscle_fiber", 1, false, List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0)), List.of()),
    MARROW_HYPER("marrow_hyper", 1, false, List.of(new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 3.0)), List.of()),
    REFLEX_NEURON("reflex_neuron", 1, false, List.of(new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.4)), List.of()),
    ELASTIC_TENDON("elastic_tendon", 1, false, List.of(new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.015)), List.of()),

    // ===== rarity 2：进阶变异（属性强化 / 器官被动） =====
    VENOM_GLAND("venom_gland", 2, false, List.of(), List.of(OrganPassive.POISON_ON_HIT)),
    NIGHT_RETINA("night_retina", 2, false, List.of(), List.of(OrganPassive.NIGHT_VISION)),
    GILL_SLIT("gill_slit", 2, false, List.of(), List.of(OrganPassive.WATER_BREATHING)),
    GLOW_CELLS("glow_cells", 2, false, List.of(), List.of(OrganPassive.GLOW)),
    CARAPACE_GROWTH("carapace_growth", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 2.0),
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.2)), List.of()),
    // 双刃：越强越脆
    BERSERK_GENE("berserk_gene", 2, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 2.0),
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, -1.5)), List.of()),
    GLASS_BONE("glass_bone", 2, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.8),
            new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, -2.0)), List.of()),

    // ===== rarity 3：稀有变异（强烈正向 / 特效被动，常见双刃畸变） =====
    CRYSTAL_CARAPACE("crystal_carapace", 3, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 1.5)), List.of(OrganPassive.THORNS)),
    SHADOW_VEIN("shadow_vein", 3, false, List.of(), List.of(OrganPassive.TELEPORT_DODGE)),
    JUMP_TENDONS("jump_tendons", 3, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.01)), List.of(OrganPassive.JUMP_BOOST)),
    STEM_REGEN("stem_regen", 3, false, List.of(), List.of(OrganPassive.REGEN)),
    ALERT_RETINA("alert_retina", 3, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 0.5)), List.of(OrganPassive.ENEMY_GLOW)),
    // 双刃稀有畸变
    PREDATOR_INSTINCT("predator_instinct", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 3.0),
            new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, -4.0),
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)), List.of()),
    PYROBLAST_FLESH("pyroblast_flesh", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 3.0)), List.of(OrganPassive.FIRE_WEAKNESS));

    private final String id;
    private final int rarity;
    private final boolean dual;
    private final List<OrganTemplate.AttributeBonus> attributes;
    private final List<OrganPassive> passives;

    MutantTrait(String id, int rarity, boolean dual,
                List<OrganTemplate.AttributeBonus> attributes, List<OrganPassive> passives) {
        this.id = id;
        this.rarity = rarity;
        this.dual = dual;
        this.attributes = attributes;
        this.passives = passives;
    }

    public String getId() {
        return id;
    }

    /** 稀有度 1~3（决定是否被纯度门槛解锁） */
    public int getRarity() {
        return rarity;
    }

    /** 畸变词条：正向收益伴随负面代价（tooltip 以警示色显示） */
    public boolean isDual() {
        return dual;
    }

    public List<OrganTemplate.AttributeBonus> attributes() {
        return attributes;
    }

    public List<OrganPassive> passives() {
        return passives;
    }

    public String getNameKey() {
        return "life.akaishi.mutant." + id;
    }

    /** 按 id 容错解析（未知/损坏返回 null，保证存档与未来词条安全） */
    public static MutantTrait valueOfSafe(String id) {
        for (MutantTrait trait : values()) {
            if (trait.id.equals(id)) {
                return trait;
            }
        }
        return null;
    }

    /** 纯度 → 词条稀有度解锁档（1/2/3） */
    public static int maxRarity(int purity) {
        if (purity >= 85) {
            return 3;
        }
        return purity >= 60 ? 2 : 1;
    }

    /**
     * 该稀有度档（≤ maxRarity）是否存在未被排除的候选词条。
     * 词条重铸等确定性操作在启动前先校验，保证 roll 必然有结果。
     */
    public static boolean hasCandidates(int maxRarity, List<MutantTrait> excluded) {
        for (MutantTrait trait : values()) {
            if (trait.rarity <= maxRarity && !excluded.contains(trait)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 随机抽取一条突变词条：70% 常规正向 / 30% 畸变（仅稀有度 ≤ maxRarity 的池子）。
     * excluded 内词条（器官已携带）不参与抽取，避免重复词条浪费承载上限；
     * 池子为空（稀有度不足）时回退到非空池；仍无则返回 null。
     */
    public static MutantTrait roll(RandomSource random, int maxRarity, List<MutantTrait> excluded) {
        List<MutantTrait> benign = new ArrayList<>();
        List<MutantTrait> duals = new ArrayList<>();
        for (MutantTrait trait : values()) {
            if (trait.rarity > maxRarity || excluded.contains(trait)) {
                continue;
            }
            (trait.dual ? duals : benign).add(trait);
        }
        // 双刃剑：多数常规、少数畸变；空池互退
        List<MutantTrait> pool = random.nextFloat() < 0.7F ? benign : duals;
        if (pool.isEmpty()) {
            pool = pool == benign ? duals : benign;
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }
}
