package com.example.akaishi.life.organ;

import com.example.akaishi.combat.ModCombatAttributes;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.life.body.BodySlot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.EnumSet;
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
    // 甲壳生长：护甲轴（仅内体）——击退抗性改护甲韧性（内体无击退轴）
    CARAPACE_GROWTH("carapace_growth", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 2.0),
            new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 0.2)), List.of()),
    // 双刃：越强越脆。护甲已归内体，臂上的"不设防"代价改用负击退抗性（臂合法轴）
    BERSERK_GENE("berserk_gene", 2, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 2.0),
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, -0.2)), List.of()),
    // 玻璃骨：攻速轴（仅臂）——躯体脆硬化作负击退抗性
    GLASS_BONE("glass_bone", 2, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.8),
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, -0.25)), List.of()),

    // ===== rarity 3：稀有变异（强烈正向 / 特效被动，常见双刃畸变） =====
    CRYSTAL_CARAPACE("crystal_carapace", 3, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 1.5)), List.of(OrganPassive.THORNS)),
    SHADOW_VEIN("shadow_vein", 3, false, List.of(), List.of(OrganPassive.TELEPORT_DODGE)),
    JUMP_TENDONS("jump_tendons", 3, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.01)), List.of(OrganPassive.JUMP_BOOST)),
    STEM_REGEN("stem_regen", 3, false, List.of(), List.of(OrganPassive.REGEN)),
    ALERT_RETINA("alert_retina", 3, false, List.of(
            new OrganTemplate.AttributeBonus(ModCombatAttributes.CRIT_CHANCE.get(), 0.03)), List.of(OrganPassive.ENEMY_GLOW)),
    // 双刃稀有畸变
    // 掠食本能：攻击轴（仅臂）——护甲代价改负击退抗性（不设防）
    PREDATOR_INSTINCT("predator_instinct", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 3.0),
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, -0.2)), List.of()),
    PYROBLAST_FLESH("pyroblast_flesh", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 3.0)), List.of(OrganPassive.FIRE_WEAKNESS)),

    // ===== 扩充词条（沿用声明式结构：属性并入 bonusesOf、被动并入 passivesOf，自动进培养/重铸池） =====
    // rarity 1：安全微调
    // 血管网：护甲韧性轴（仅内体）
    VASCULAR_MESH("vascular_mesh", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 1.0)), List.of()),
    DENSE_BONE("dense_bone", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, 0.1)), List.of()),
    COORDINATION_NEURONS("coordination_neurons", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.LUCK, 1.0)), List.of()),
    // 骨化韧带：护甲轴（仅内体）——击退抗性改护甲韧性（内体无击退轴）
    OSSIFIED_LIGAMENT("ossified_ligament", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 0.5),
            new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 0.05)), List.of()),
    // rarity 2：进阶正向 / 双刃
    STICKY_GLANDS("sticky_glands", 2, false, List.of(), List.of(OrganPassive.SLOW_ON_HIT)),
    GLIDER_MEMBRANE("glider_membrane", 2, false, List.of(), List.of(OrganPassive.GLIDE)),
    ARCTIC_CELLS("arctic_cells", 2, false, List.of(), List.of(OrganPassive.ANTIFREEZE)),
    LYMPH_FILTER("lymph_filter", 2, false, List.of(), List.of(OrganPassive.ANTIDOTE)),
    POWER_CORD("power_cord", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.0),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.2)), List.of()),
    // 肌肉肥大：攻击+攻速轴（仅臂）——攻速代价保留（臂合法）
    HYPERTROPHY("hypertrophy", 2, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 1.5),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, -0.15)), List.of()),
    // 野性迸发：攻击轴（仅臂）——护甲代价改负击退抗性
    FERAL_SURGE("feral_surge", 2, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 2.0),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.6),
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, -0.2)), List.of()),
    // rarity 3：稀有正向 / 双刃畸变
    WITHER_TOOTH("wither_tooth", 3, false, List.of(), List.of(OrganPassive.WITHER_ON_HIT)),
    REFRACTORY_CELLS("refractory_cells", 3, false, List.of(), List.of(OrganPassive.FIRE_IMMUNE)),
    CATAPULT_MUSCLE("catapult_muscle", 3, false, List.of(), List.of(OrganPassive.JUMP_ATTACK_BOOST)),
    STREAMLINE_CARTILAGE("streamline_cartilage", 3, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.MOVEMENT_SPEED, 0.02)), List.of()),
    // 巨人症：心轴（仅生命）——移速/攻速双代价折入生命净额（该槽位无其它合法轴可承载代价）
    GIGANTISM("gigantism", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 7.0)), List.of()),
    // 肾上腺素过载：攻速轴（仅臂）——护甲代价改负击退抗性
    ADRENAL_OVERDRIVE("adrenal_overdrive", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 1.2),
            new OrganTemplate.AttributeBonus(Attributes.KNOCKBACK_RESISTANCE, -0.25)), List.of()),
    // 主宰核心：护甲轴（仅内体）——击退抗性改护甲韧性；沉重代价化作负幸运（内体仅护甲/韧性/幸运三轴）
    JUGGERNAUT_CORE("juggernaut_core", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 4.0),
            new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 0.3),
            new OrganTemplate.AttributeBonus(Attributes.LUCK, -0.5)), List.of()),

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
    // 夜骨：攻速轴（臂/肾）——攻击伤害替代原移速（移速属腿轴，收窄后不可共存）
    NOCTURNE_BONE("nocturne_bone", 3, true, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.5),
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_DAMAGE, 0.5)),
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
    // 角质层：护甲轴（仅内体）——击退抗性改护甲韧性（内体无击退轴）
    KERATIN_LAYER("keratin_layer", 1, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ARMOR, 0.5),
            new OrganTemplate.AttributeBonus(Attributes.ARMOR_TOUGHNESS, 0.1)), List.of()),
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
            new OrganTemplate.AttributeBonus(Attributes.MAX_HEALTH, 2.0)), List.of()),
    SYMBIOTE_FLORA("symbiote_flora", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.LUCK, 1.0)), List.of()),
    HORMONE_RECYCLER("hormone_recycler", 2, false, List.of(
            new OrganTemplate.AttributeBonus(Attributes.ATTACK_SPEED, 0.2)), List.of());

    private final String id;
    private final int rarity;
    private final boolean dual;
    private final List<OrganTemplate.AttributeBonus> attributes;
    private final List<OrganPassive> passives;

    /** 部位约束统一由 SLOT_ONLY 表声明：未列出 = 全部位通用（左右臂/腿等价由 appliesTo 处理） */
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

    /** 纯度 → 词条稀有度解锁档（1/2/3）。阈值读取配置 [trait]，两阈值取大者出 3 档、小者出 2 档（防填反） */
    public static int maxRarity(int purity) {
        int high = Math.max(ModConfig.traitRarityHighThreshold, ModConfig.traitRarityMidThreshold);
        int mid = Math.min(ModConfig.traitRarityHighThreshold, ModConfig.traitRarityMidThreshold);
        if (purity >= high) {
            return 3;
        }
        return purity >= mid ? 2 : 1;
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
        // 双刃剑：多数常规、少数畸变（比例可配置 [trait] benignRatio）；空池互退
        List<MutantTrait> pool = random.nextFloat() < ModConfig.traitBenignRatio ? benign : duals;
        if (pool.isEmpty()) {
            pool = pool == benign ? duals : benign;
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    // ===== 属性 → 合法槽位（对齐 OrganRegistry 属性↔槽位矩阵；左右臂/腿等价由 appliesTo 判定） =====
    private static final EnumSet<BodySlot> EYE_SLOTS = EnumSet.of(BodySlot.EYE);
    private static final EnumSet<BodySlot> LUNG_SLOTS = EnumSet.of(BodySlot.LUNGS);
    private static final EnumSet<BodySlot> HEART_SLOTS = EnumSet.of(BodySlot.HEART);
    private static final EnumSet<BodySlot> GUT_SLOTS = EnumSet.of(BodySlot.VISCERA);
    private static final EnumSet<BodySlot> KIDNEY_SLOTS = EnumSet.of(BodySlot.KIDNEYS);
    private static final EnumSet<BodySlot> ARM_SLOTS = EnumSet.of(BodySlot.LEFT_ARM);
    private static final EnumSet<BodySlot> LEG_SLOTS = EnumSet.of(BodySlot.LEFT_LEG);
    private static final EnumSet<BodySlot> ATTACK_SLOTS = EnumSet.of(BodySlot.KIDNEYS, BodySlot.LEFT_ARM);
    private static final EnumSet<BodySlot> HASTE_SLOTS = EnumSet.of(BodySlot.LEFT_ARM);
    private static final EnumSet<BodySlot> KNOCKBACK_SLOTS = EnumSet.of(BodySlot.LEFT_ARM, BodySlot.LEFT_LEG);

    /**
     * 词条 → 合法槽位（未列出 = 全部位通用）。规则：
     * - 词条的全部属性（含负值代价）都必须落在目标槽位合法轴上，故取各属性合法槽位的交集；
     *   代价无处承载时改写为同槽位合法轴代价（如移速→攻速）、或折入主属性（心槽仅生命合法）；
     * - 交集为空属跨轴违规（禁止新增），退化为并集仅为避免整表构建崩溃，
     *   由 dev 核对器断言 FAIL 兜底；
     * - 仅被动词条按语义绑定（视力→眼、水生呼吸→肺、滑翔/命中特效→臂、弹跳/游泳/缓冲→腿、消化/护甲/幸运→内体）。
     */
    private static final Map<String, EnumSet<BodySlot>> SLOT_ONLY = Map.ofEntries(
            // 眼（感知/瞄准：暴击率 + 视力被动）
            Map.entry("night_retina", EYE_SLOTS),
            Map.entry("eagle_membrane", EYE_SLOTS),
            Map.entry("alert_retina", EYE_SLOTS),
            // 肺（呼吸：仅水生呼吸留肺，滑翔/游泳等位移类已迁臂/腿）
            Map.entry("gill_slit", LUNG_SLOTS),
            // 心（生命/再生）
            Map.entry("stem_regen", HEART_SLOTS),
            Map.entry("marrow_hyper", HEART_SLOTS),
            Map.entry("gigantism", HEART_SLOTS),
            Map.entry("myocardium", HEART_SLOTS),
            // 内体（护甲/韧性/幸运/消化）
            Map.entry("venom_gland", GUT_SLOTS),
            Map.entry("alchemist_gut", GUT_SLOTS),
            Map.entry("omnivore_gut", GUT_SLOTS),
            Map.entry("coordination_neurons", GUT_SLOTS),
            Map.entry("symbiote_flora", GUT_SLOTS),
            Map.entry("dense_tissue", GUT_SLOTS),
            Map.entry("crystal_carapace", GUT_SLOTS),
            Map.entry("carapace_growth", GUT_SLOTS),
            Map.entry("keratin_layer", GUT_SLOTS),
            Map.entry("ossified_ligament", GUT_SLOTS),
            Map.entry("juggernaut_core", GUT_SLOTS),
            Map.entry("vascular_mesh", GUT_SLOTS),
            // 肾（过滤/攻击）
            Map.entry("lymph_filter", KIDNEY_SLOTS),
            Map.entry("depth_pressure", KIDNEY_SLOTS),
            // 腿（位移/弹跳/缓冲）
            Map.entry("elastic_tendon", LEG_SLOTS),
            Map.entry("streamline_cartilage", LEG_SLOTS),
            Map.entry("cushion_sole", LEG_SLOTS),
            Map.entry("jump_tendons", LEG_SLOTS),
            Map.entry("catapult_muscle", LEG_SLOTS),
            Map.entry("synovial_lining", LEG_SLOTS),
            Map.entry("abyssal_webbing", LEG_SLOTS),
            Map.entry("dense_bone", KNOCKBACK_SLOTS),
            // 臂（近战/暴击伤害/击退抗性——含"强攻 + 负击退抗"双刃）
            Map.entry("charge_muscle", ARM_SLOTS),
            Map.entry("glider_membrane", ARM_SLOTS),
            Map.entry("berserk_gene", ARM_SLOTS),
            Map.entry("glass_bone", ARM_SLOTS),
            Map.entry("feral_surge", ARM_SLOTS),
            Map.entry("adrenal_overdrive", ARM_SLOTS),
            Map.entry("predator_instinct", ARM_SLOTS),
            Map.entry("hormone_recycler", ARM_SLOTS),
            // 攻击轴（肾/臂）
            Map.entry("muscle_fiber", ATTACK_SLOTS),
            Map.entry("pyroblast_flesh", ATTACK_SLOTS),
            // 攻击 + 攻速轴（仅臂）
            Map.entry("hypertrophy", HASTE_SLOTS),
            Map.entry("reflex_neuron", HASTE_SLOTS),
            Map.entry("nocturne_bone", HASTE_SLOTS),
            Map.entry("power_cord", slots(ATTACK_SLOTS, HASTE_SLOTS)),
            Map.entry("glutton_muscle", slots(ATTACK_SLOTS, HASTE_SLOTS)),
            Map.entry("fracture_strike", slots(ATTACK_SLOTS, HASTE_SLOTS)));

    /** 多属性词条：取各属性合法槽位的交集；交集为空（跨轴违规）退化为并集兜底，由 dev 核对器报错 */
    @SafeVarargs
    private static EnumSet<BodySlot> slots(EnumSet<BodySlot>... axes) {
        EnumSet<BodySlot> intersection = EnumSet.copyOf(axes[0]);
        EnumSet<BodySlot> union = EnumSet.noneOf(BodySlot.class);
        for (EnumSet<BodySlot> axis : axes) {
            intersection.retainAll(axis);
            union.addAll(axis);
        }
        return intersection.isEmpty() ? union : intersection;
    }

    /** 目标槽位是否允许出现本词条（slot 为 null = 不限）；左右臂/腿等价 */
    public boolean appliesTo(BodySlot slot) {
        if (slot == null) {
            return true;
        }
        EnumSet<BodySlot> allowed = SLOT_ONLY.get(id);
        if (allowed == null || allowed.contains(slot)) {
            return true;
        }
        return switch (slot) {
            case RIGHT_ARM -> allowed.contains(BodySlot.LEFT_ARM);
            case RIGHT_LEG -> allowed.contains(BodySlot.LEFT_LEG);
            default -> false;
        };
    }
}
