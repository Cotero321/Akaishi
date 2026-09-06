package com.example.akaishi.life.organ;

import com.example.akaishi.life.body.BodySlot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 3.0)), List.of(OrganPassive.FIRE_WEAKNESS)),

    // ===== 扩充词条（沿用声明式结构：属性并入 bonusesOf、被动并入 passivesOf，自动进培养/重铸池） =====
    // rarity 1：安全微调
    VASCULAR_MESH("vascular_mesh", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 1.0)), List.of()),
    DENSE_BONE("dense_bone", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.1)), List.of()),
    COORDINATION_NEURONS("coordination_neurons", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.LUCK, 1.0)), List.of()),
    OSSIFIED_LIGAMENT("ossified_ligament", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 0.5),
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.05)), List.of()),
    // rarity 2：进阶正向 / 双刃
    STICKY_GLANDS("sticky_glands", 2, false, List.of(), List.of(OrganPassive.SLOW_ON_HIT)),
    GLIDER_MEMBRANE("glider_membrane", 2, false, List.of(), List.of(OrganPassive.GLIDE)),
    ARCTIC_CELLS("arctic_cells", 2, false, List.of(), List.of(OrganPassive.ANTIFREEZE)),
    LYMPH_FILTER("lymph_filter", 2, false, List.of(), List.of(OrganPassive.ANTIDOTE)),
    POWER_CORD("power_cord", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.2)), List.of()),
    HYPERTROPHY("hypertrophy", 2, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.5),
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.015)), List.of()),
    FERAL_SURGE("feral_surge", 2, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 2.0),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.6),
            new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, -2.0)), List.of()),
    // rarity 3：稀有正向 / 双刃畸变
    WITHER_TOOTH("wither_tooth", 3, false, List.of(), List.of(OrganPassive.WITHER_ON_HIT)),
    REFRACTORY_CELLS("refractory_cells", 3, false, List.of(), List.of(OrganPassive.FIRE_IMMUNE)),
    CATAPULT_MUSCLE("catapult_muscle", 3, false, List.of(), List.of(OrganPassive.JUMP_ATTACK_BOOST)),
    STREAMLINE_CARTILAGE("streamline_cartilage", 3, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)), List.of()),
    GIGANTISM("gigantism", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 8.0),
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.02),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, -0.4)), List.of()),
    ADRENAL_OVERDRIVE("adrenal_overdrive", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 1.2),
            new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, -6.0)), List.of()),
    JUGGERNAUT_CORE("juggernaut_core", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 4.0),
            new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 2.0),
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, -0.02)), List.of()),

    // ===== 多样化扩容（第二批）：补方向盲区——命中点燃/远程强化/摔落免疫/自动拾取/爆抗/冲撞击退 +
    //      双刃代价新增三轴（阳光灼晒/高代谢/物理易伤，与负面被动联动：火免词条可解阳光灼晒） =====
    // rarity 2 良性：命中/远程/安全工具方向
    SCORCH_GLAND("scorch_gland", 2, false, List.of(), List.of(OrganPassive.IGNITE_ON_HIT)),
    EAGLE_MEMBRANE("eagle_membrane", 2, false, List.of(), List.of(OrganPassive.PROJECTILE_BOOST)),
    CUSHION_SOLE("cushion_sole", 2, false, List.of(), List.of(OrganPassive.FALL_IMMUNE)),
    // rarity 3 良性：探索便利/抗性/近战特效方向
    MAGNET_DOWN("magnet_down", 3, false, List.of(), List.of(OrganPassive.AUTO_PICKUP)),
    PRESSURE_SKIN("pressure_skin", 3, false, List.of(), List.of(OrganPassive.BLAST_RESIST)),
    CHARGE_MUSCLE("charge_muscle", 3, false, List.of(), List.of(OrganPassive.KNOCKBACK_ON_HIT)),
    // rarity 3 双刃：代价轴多元化（不再只有扣血/扣甲/火弱）
    NOCTURNE_BONE("nocturne_bone", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.5),
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)),
            List.of(OrganPassive.SUNLIGHT_BURN)),
    GLUTTON_MUSCLE("glutton_muscle", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 2.0),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.4)),
            List.of(OrganPassive.RAPID_EXHAUSTION)),
    FRACTURE_STRIKE("fracture_strike", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 2.5),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.3)),
            List.of(OrganPassive.VULNERABLE)),

    // ===== 多样化扩容（第三批）：水战/距离/续航/疲劳/抗性全采纳（墨雾除外）+ r1 混合微调 =====
    KERATIN_LAYER("keratin_layer", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 0.5),
            new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 0.5)), List.of()),
    ABYSSAL_WEBBING("abyssal_webbing", 2, false, List.of(),
            List.of(OrganPassive.SWIM_BOOST)),
    ALCHEMIST_GUT("alchemist_gut", 2, false, List.of(),
            List.of(OrganPassive.WITCH_BREW)),
    OMNIVORE_GUT("omnivore_gut", 2, false, List.of(),
            List.of(OrganPassive.FOOD_BOOST)),
    DEPTH_PRESSURE("depth_pressure", 3, false, List.of(),
            List.of(OrganPassive.WATER_ATTACK_BOOST)),
    ELONGATED_JOINTS("elongated_joints", 3, false, List.of(),
            List.of(OrganPassive.LONG_REACH)),
    LASSITUDE_GLAND("lassitude_gland", 3, false, List.of(),
            List.of(OrganPassive.FATIGUE_ON_HIT)),
    SYNOVIAL_LINING("synovial_lining", 3, false, List.of(),
            List.of(OrganPassive.SLOW_IMMUNE)),

    // ===== 部位专属补充（第三批专属）：给心/内脏/肾补足 ≥2 条专属，强化"部位定装"叙事 =====
    MYOCARDIUM("myocardium", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 2.0)), List.of(), BodySlot.HEART),
    SYMBIOTE_FLORA("symbiote_flora", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.LUCK, 1.0)), List.of(), BodySlot.VISCERA),
    HORMONE_RECYCLER("hormone_recycler", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.2)), List.of(), BodySlot.KIDNEYS);

    private final String id;
    private final int rarity;
    private final boolean dual;
    private final List<OrganTemplate.AttributeBonus> attributes;
    private final List<OrganPassive> passives;

    /** 通用构造：不带部位约束（所有槽位均可出现） */
    MutantTrait(String id, int rarity, boolean dual,
                List<OrganTemplate.AttributeBonus> attributes, List<OrganPassive> passives) {
        this(id, rarity, dual, attributes, passives, null);
    }

    /** 部位专属构造：slot 为 null = 全部位通用；否则仅对应槽位器官可 roll 到（左右臂/腿在内部等价） */
    MutantTrait(String id, int rarity, boolean dual,
                List<OrganTemplate.AttributeBonus> attributes, List<OrganPassive> passives, BodySlot slot) {
        this.id = id;
        this.rarity = rarity;
        this.dual = dual;
        this.attributes = attributes;
        this.passives = passives;
        this.slot = slot;
    }

    /** 部位约束：null = 全部位通用（SLOT_ONLY 表未列出者）；左右臂/腿等价由 appliesTo 处理 */
    private final BodySlot slot;

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
        return hasCandidates(maxRarity, excluded, null);
    }

    /** 带部位约束版本：slot 为 null = 不限部位（与旧调用等价） */
    public static boolean hasCandidates(int maxRarity, List<MutantTrait> excluded, BodySlot slot) {
        for (MutantTrait trait : values()) {
            if (trait.rarity <= maxRarity && !excluded.contains(trait)
                    && (slot == null || trait.appliesTo(slot))) {
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
        return roll(random, maxRarity, excluded, null);
    }

    /** 带部位约束版本：部位专属词条只会在对应槽位器官上 roll 到（slot 为 null = 不限部位） */
    public static MutantTrait roll(RandomSource random, int maxRarity, List<MutantTrait> excluded, BodySlot slot) {
        List<MutantTrait> benign = new ArrayList<>();
        List<MutantTrait> duals = new ArrayList<>();
        for (MutantTrait trait : values()) {
            if (trait.rarity > maxRarity || excluded.contains(trait)) {
                continue;
            }
            if (slot != null && !trait.appliesTo(slot)) {
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

    // ===== 部位专属映射 =====
    // 说明：以下词条的语义天然绑定部位（视力→眼、呼吸/滑翔→肺、弹跳/缓冲→腿……），
    // roll 时只对对应槽位的器官开放；未列出 = 全部位通用。左右臂/腿在约束判定中等价。
    private static final Map<String, BodySlot> SLOT_ONLY = Map.ofEntries(
            Map.entry("night_retina", BodySlot.EYE),
            Map.entry("eagle_membrane", BodySlot.EYE),
            Map.entry("alert_retina", BodySlot.EYE),
            Map.entry("gill_slits", BodySlot.LUNGS),
            Map.entry("glider_membrane", BodySlot.LUNGS),
            Map.entry("abyssal_webbing", BodySlot.LUNGS),
            Map.entry("cushion_sole", BodySlot.LEFT_LEG),
            Map.entry("jump_tendons", BodySlot.LEFT_LEG),
            Map.entry("catapult_muscle", BodySlot.LEFT_LEG),
            Map.entry("synovial_lining", BodySlot.LEFT_LEG),
            Map.entry("power_cord", BodySlot.LEFT_ARM),
            Map.entry("charge_muscle", BodySlot.LEFT_ARM),
            Map.entry("venom_gland", BodySlot.VISCERA),
            Map.entry("alchemist_gut", BodySlot.VISCERA),
            Map.entry("omnivore_gut", BodySlot.VISCERA),
            Map.entry("lymph_filter", BodySlot.KIDNEYS),
            Map.entry("depth_pressure", BodySlot.KIDNEYS),
            Map.entry("stem_regen", BodySlot.HEART));

    /** 部位约束：null = 全部位通用；否则仅该部位（含对侧臂/腿）可 roll 到 */
    public BodySlot slotOnly() {
        return SLOT_ONLY.get(id);
    }

    /** 目标槽位是否允许出现本词条（slot 为 null = 不限） */
    public boolean appliesTo(BodySlot slot) {
        if (slot == null) {
            return true;
        }
        BodySlot only = SLOT_ONLY.get(id);
        if (only == null || only == slot) {
            return true;
        }
        // 左右臂/腿等价：左脚器官也能 roll 到腿部专属词条
        return isPaired(only, slot);
    }

    private static boolean isPaired(BodySlot a, BodySlot b) {
        return switch (a) {
            case LEFT_ARM, RIGHT_ARM -> b == BodySlot.LEFT_ARM || b == BodySlot.RIGHT_ARM;
            case LEFT_LEG, RIGHT_LEG -> b == BodySlot.LEFT_LEG || b == BodySlot.RIGHT_LEG;
            default -> false;
        };
    }
}
