package com.example.akaishi.life.mechanical.trait;

import com.example.akaishi.api.mechanical.trait.IMechanicalTrait;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitRegistry;
import com.example.akaishi.life.mechanical.MechanicalMaterial;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置机械材料特性（T5）：17 个「特性定义」与「挂到材料」的唯一入口。
 * <p>
 * 本类只负责「特性是什么」（ID / 翻译键 / 等级上限）与「挂在哪些材料上」；
 * 运行时「它做什么」由平台侧（forge）的 {@code IMechanicalTraitHandler} 实现并注册，
 * 未注册处理器的特性只展示、不生效、不报错（企划 §3 附属接入清单）。
 * <p>
 * <b>等级口径</b>（企划 §2.1/§2.2）：单器官等级 = clamp(该材料在本器官 4 个部件中出现的次数, 1, 4)，
 * 只在单器官内计算；跨器官由分发层取「有效等级」= max(各器官单器官等级)（clamp 1~maxLevel），
 * 每个特性只回调一次（平衡收敛）。
 * <p>
 * <b>数量说明</b>：企划 §4 标题写作「16 个」，但四档表实为 <b>17</b> 行
 * （材料覆盖行漏计 {@code debuff_ward}）；为不留孤儿（RULES §1），本类全量落地 17 个。
 *
 * @see TraitTier 四档数值表
 */
public final class MechanicalTraits {

    // ==================== A. 生存向（6） ====================
    /** 强化骨架（来源：铁） */
    public static final String REINFORCED_FRAME = "akaishi:reinforced_frame";
    /** 生命储备（来源：生物陶瓷） */
    public static final String VITAL_RESERVE = "akaishi:vital_reserve";
    /** 自修复（来源：精炼核心） */
    public static final String SELF_REPAIR = "akaishi:self_repair";
    /** 烧蚀装甲（来源：耐蚀钢） */
    public static final String ABLATION_ARMOR = "akaishi:ablation_armor";
    /** 净化滤芯（来源：生物陶瓷） */
    public static final String DEBUFF_WARD = "akaishi:debuff_ward";
    /** 冲击吸收（来源：合金钢） */
    public static final String SHOCK_ABSORB = "akaishi:shock_absorb";

    // ==================== B. 攻击向（6） ====================
    /** 破甲刃（来源：精密合金） */
    public static final String ARMOR_PIERCE = "akaishi:armor_pierce";
    /** 处决程序（来源：精炼核心） */
    public static final String EXECUTIONER = "akaishi:executioner";
    /** 连击驱动（来源：红石合金） */
    public static final String COMBO_DRIVER = "akaishi:combo_driver";
    /** 血液虹吸（来源：生物陶瓷） */
    public static final String HEMOSIPHON = "akaishi:hemosiphon";
    /** 动能释放（来源：聚合红石） */
    public static final String KINETIC_SURGE = "akaishi:kinetic_surge";
    /** 反应装甲（来源：合金钢） */
    public static final String REACTIVE_PLATING = "akaishi:reactive_plating";

    // ==================== C. 机动向（2） ====================
    /** 动能增幅（来源：陶瓷复合） */
    public static final String KINETIC_BOOST = "akaishi:kinetic_boost";
    /** 相位步频（来源：灵能复合） */
    public static final String PHASE_STEP = "akaishi:phase_step";

    // ==================== D. 功能向（3） ====================
    /** 掘进模块（来源：铁） */
    public static final String EXCAVATOR = "akaishi:excavator";
    /** 探矿协议（来源：赤石） */
    public static final String PROSPECTOR = "akaishi:prospector";
    /** 数据收割（来源：灵能复合） */
    public static final String DATA_HARVEST = "akaishi:data_harvest";

    private static final List<IMechanicalTrait> ALL = new ArrayList<>();

    private MechanicalTraits() {
    }

    /**
     * 注册全部内置特性定义并把它们挂到对应材料（企划 §4 的「来源材料」列）。
     * <p>
     * 须在 {@link MechanicalMaterial#registerDefaults()} 之后调用（挂载依赖材料已注册）。
     */
    public static void register() {
        define(REINFORCED_FRAME);
        define(VITAL_RESERVE);
        define(SELF_REPAIR);
        define(ABLATION_ARMOR);
        define(DEBUFF_WARD);
        define(SHOCK_ABSORB);
        define(ARMOR_PIERCE);
        define(EXECUTIONER);
        define(COMBO_DRIVER);
        define(HEMOSIPHON);
        define(KINETIC_SURGE);
        define(REACTIVE_PLATING);
        define(KINETIC_BOOST);
        define(PHASE_STEP);
        define(EXCAVATOR);
        define(PROSPECTOR);
        define(DATA_HARVEST);
        attachToMaterials();
    }

    /** 全部内置特性定义（不可变快照，供 tooltip / 调试遍历）。 */
    public static List<IMechanicalTrait> all() {
        return List.copyOf(ALL);
    }

    private static void define(String id) {
        SimpleTrait trait = new SimpleTrait(id);
        MechanicalTraitRegistry.register(trait);
        ALL.add(trait);
    }

    /** 按企划 §4「来源材料」列挂载（不改动材料点数与十维分布）。 */
    private static void attachToMaterials() {
        MechanicalMaterial.addTrait("akaishi:iron", REINFORCED_FRAME);
        MechanicalMaterial.addTrait("akaishi:iron", EXCAVATOR);

        MechanicalMaterial.addTrait("akaishi:chishi", PROSPECTOR);

        MechanicalMaterial.addTrait("akaishi:ceramic_composite", KINETIC_BOOST);
        MechanicalMaterial.addTrait("akaishi:resistant_steel", ABLATION_ARMOR);
        MechanicalMaterial.addTrait("akaishi:redstone_alloy", COMBO_DRIVER);

        MechanicalMaterial.addTrait("akaishi:precision_alloy", ARMOR_PIERCE);
        MechanicalMaterial.addTrait("akaishi:polymerized_redstone", KINETIC_SURGE);

        MechanicalMaterial.addTrait("akaishi:bio_ceramic", VITAL_RESERVE);
        MechanicalMaterial.addTrait("akaishi:bio_ceramic", HEMOSIPHON);
        MechanicalMaterial.addTrait("akaishi:bio_ceramic", DEBUFF_WARD);

        MechanicalMaterial.addTrait("akaishi:refined_core", SELF_REPAIR);
        MechanicalMaterial.addTrait("akaishi:refined_core", EXECUTIONER);

        MechanicalMaterial.addTrait("akaishi:alloy_steel", SHOCK_ABSORB);
        MechanicalMaterial.addTrait("akaishi:alloy_steel", REACTIVE_PLATING);

        MechanicalMaterial.addTrait("akaishi:psionic_composite", PHASE_STEP);
        MechanicalMaterial.addTrait("akaishi:psionic_composite", DATA_HARVEST);
    }

    /** 极简特性定义实现：只承载 ID 与翻译键（等级上限沿用默认 4）。 */
    private static final class SimpleTrait implements IMechanicalTrait {
        private final String id;
        private final String translationKey;

        SimpleTrait(String id) {
            this.id = id;
            this.translationKey = "mechanical.trait." + id.replace(':', '.');
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
}
