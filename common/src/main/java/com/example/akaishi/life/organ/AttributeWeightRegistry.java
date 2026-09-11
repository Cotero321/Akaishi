package com.example.akaishi.life.organ;

import com.example.akaishi.combat.ModCombatAttributes;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.life.body.BodySlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基因属性权重表：按「生物来源 × 属性轴」派生的专精倍率，让每个生物的强项维度各不相同。
 * 语义（相对最强轴归一化）：
 * - 正词条 × w = 1 + k × (该轴强度和 ÷ 该来源最强轴强度和)   —— 长项放大到 ×(1+k)，其余按强度占比递减到 ×1
 * - 负词条 × c = 1 − k × (1 − 该轴强度和 ÷ 最强轴强度和)   —— 惩罚轴降权，越偏离该生物长项越小（≤1）
 * 数据源：{@link OrganEffectRegistry#allEffects()} 的全库器官效果；负值/零值惩罚项不参与"长项"统计。
 * 基础倍率为 1.0：与基因扁平适配加成（BodyGeneHelper）叠加而非替代，二者正交。
 *
 * <p>最终倍率 = 来源派生权重 × 槽位轴亲和权重：器官装在专精槽位时额外放大（如同攻击词条装双臂 &gt; 装肾），
 * 装在意料之外的合法槽位时保持 1.0（不放大也不削弱）。槽位亲和与 {@link OrganRegistry#allows} 矩阵同源，
 * 只允许放大"该槽位合法的轴"，非法轴不可能出现故无需表态。
 */
public final class AttributeWeightRegistry {

    /** 属性轴（8 维）：语义归并后的专精维度，同轴属性共享同一倍率 */
    public enum Axis {
        ATTACK,         // 攻击伤害
        ATTACK_SPEED,   // 攻击速度
        CRIT,           // 暴击率 + 暴击伤害
        HEALTH,         // 最大生命
        DEFENSE,        // 护甲 + 护甲韧性
        MOBILITY,       // 移动速度 + 闪避
        RESILIENCE,     // 击退抗性
        LUCK            // 幸运
    }

    /** 配置未覆盖（≤0）时使用的内置权重强度 */
    public static final double DEFAULT_STRENGTH = 0.25;

    /** 未专精槽位的亲和基准：既不放大也不削弱 */
    private static final double NEUTRAL_AFFINITY = 1.0;

    /** 来源 → 各轴正属性和（下标 = Axis.ordinal()） */
    private static final Map<String, double[]> AXIS_STRENGTH = new ConcurrentHashMap<>();
    /** 来源 → 最强轴正属性和（0 表示该来源无正属性，整体不加权） */
    private static final Map<String, Double> MAX_STRENGTH = new ConcurrentHashMap<>();
    /** 槽位 → 各轴亲和（下标 = Axis.ordinal()，全部预填 1.0 后仅覆写专精轴） */
    private static final Map<BodySlot, double[]> SLOT_AFFINITY = new EnumMap<>(BodySlot.class);

    static {
        buildSlotAffinity();
        build();
    }

    private AttributeWeightRegistry() {
    }

    /**
     * 槽位轴亲和表：只有"该轴的天然归属槽位"取 &gt;1.0，其余保持 1.0。
     * 亲和值仅放大不衰减——非法组合已被矩阵拦下（永不会出现），合法的非专精组合按 1.0 原样生效。
     */
    private static void buildSlotAffinity() {
        for (BodySlot slot : BodySlot.values()) {
            double[] axes = new double[Axis.values().length];
            Arrays.fill(axes, NEUTRAL_AFFINITY);
            SLOT_AFFINITY.put(slot, axes);
        }
        // 唯一宿主轴（矩阵仅允许单一槽位，天然满亲和）
        setAffinity(BodySlot.EYE, Axis.CRIT, 1.25);         // 暴击率仅眼
        setAffinity(BodySlot.HEART, Axis.HEALTH, 1.30);     // 最大生命仅心
        setAffinity(BodySlot.VISCERA, Axis.DEFENSE, 1.25);  // 护甲/护甲韧性仅内体
        setAffinity(BodySlot.VISCERA, Axis.LUCK, 1.30);     // 幸运仅内体
        setAffinity(BodySlot.KIDNEYS, Axis.ATTACK, 1.10);   // 肾主滤排，攻击次宿主
        // 双臂：近战主力（攻击/暴击伤害），兼顾攻速与抗击退
        for (BodySlot arm : new BodySlot[]{BodySlot.LEFT_ARM, BodySlot.RIGHT_ARM}) {
            setAffinity(arm, Axis.ATTACK, 1.20);
            setAffinity(arm, Axis.ATTACK_SPEED, 1.15);
            setAffinity(arm, Axis.CRIT, 1.20);
            setAffinity(arm, Axis.RESILIENCE, 1.10);
        }
        // 双腿：位移唯一宿主（移速/闪避），兼顾抗击退
        for (BodySlot leg : new BodySlot[]{BodySlot.LEFT_LEG, BodySlot.RIGHT_LEG}) {
            setAffinity(leg, Axis.MOBILITY, 1.30);
            setAffinity(leg, Axis.RESILIENCE, 1.15);
        }
    }

    private static void setAffinity(BodySlot slot, Axis axis, double value) {
        SLOT_AFFINITY.get(slot)[axis.ordinal()] = value;
    }

    /** 单次全库派生：按 (来源, 轴) 累加正属性强度，再取各来源最强轴作为归一化基准 */
    private static void build() {
        Map<String, double[]> acc = new HashMap<>();
        for (OrganEffect effect : OrganEffectRegistry.allEffects()) {
            if (effect.attributes() == null) {
                continue;
            }
            double[] axes = acc.computeIfAbsent(effect.entityId(), k -> new double[Axis.values().length]);
            for (OrganTemplate.AttributeBonus bonus : effect.attributes()) {
                if (bonus.base() <= 0) {
                    continue; // 惩罚/无效词条不计入长项
                }
                Axis axis = axisOf(bonus.attribute());
                if (axis != null) {
                    axes[axis.ordinal()] += bonus.base();
                }
            }
        }
        for (Map.Entry<String, double[]> entry : acc.entrySet()) {
            double max = 0.0;
            for (double v : entry.getValue()) {
                max = Math.max(max, v);
            }
            if (max <= 0.0) {
                continue; // 纯代价来源：无长项可言，保持基础倍率
            }
            AXIS_STRENGTH.put(entry.getKey(), entry.getValue());
            MAX_STRENGTH.put(entry.getKey(), max);
        }
    }

    /** 属性 → 轴（未归轴属性返回 null，不参与加权） */
    private static Axis axisOf(Attribute attribute) {
        if (attribute == Attributes.ATTACK_DAMAGE) {
            return Axis.ATTACK;
        }
        if (attribute == Attributes.ATTACK_SPEED) {
            return Axis.ATTACK_SPEED;
        }
        if (attribute == ModCombatAttributes.CRIT_CHANCE.get()
                || attribute == ModCombatAttributes.CRIT_DAMAGE.get()) {
            return Axis.CRIT;
        }
        if (attribute == Attributes.MAX_HEALTH) {
            return Axis.HEALTH;
        }
        if (attribute == Attributes.ARMOR || attribute == Attributes.ARMOR_TOUGHNESS) {
            return Axis.DEFENSE;
        }
        if (attribute == Attributes.MOVEMENT_SPEED || attribute == ModCombatAttributes.DODGE_CHANCE.get()) {
            return Axis.MOBILITY;
        }
        if (attribute == Attributes.KNOCKBACK_RESISTANCE) {
            return Axis.RESILIENCE;
        }
        if (attribute == Attributes.LUCK) {
            return Axis.LUCK;
        }
        return null;
    }

    /**
     * 取该来源该属性在指定槽位上的最终权重乘子 = 来源专精权重 × 槽位轴亲和。
     *
     * @param entityId  器官来源生物 id
     * @param slot      器官所处槽位（null = 不加成槽位亲和）
     * @param attribute 属性
     * @param base      词条基础值（决定走正词条还是惩罚轴公式）
     */
    public static double multiplier(String entityId, BodySlot slot, Attribute attribute, double base) {
        return sourceMultiplier(entityId, attribute, base) * slotAffinity(slot, axisOf(attribute));
    }

    /** 来源专精权重：长项放大 / 弱势轴（惩罚词条）降权（未知来源、未归轴属性一律 1.0） */
    private static double sourceMultiplier(String entityId, Attribute attribute, double base) {
        if (entityId == null || entityId.isEmpty() || attribute == null) {
            return 1.0;
        }
        double[] axes = AXIS_STRENGTH.get(entityId);
        Double max = MAX_STRENGTH.get(entityId);
        if (axes == null || max == null || max <= 0.0) {
            return 1.0;
        }
        Axis axis = axisOf(attribute);
        if (axis == null) {
            return 1.0;
        }
        double ratio = Math.min(1.0, axes[axis.ordinal()] / max);
        double k = strength();
        return base >= 0.0 ? 1.0 + k * ratio : 1.0 - k * (1.0 - ratio);
    }

    /** 槽位轴亲和（未专精/未知返回 1.0）；供 dev 核对器核对"放大轴必须是该槽位合法轴" */
    public static double slotAffinity(BodySlot slot, Axis axis) {
        if (slot == null || axis == null) {
            return NEUTRAL_AFFINITY;
        }
        double[] axes = SLOT_AFFINITY.get(slot);
        return axes == null ? NEUTRAL_AFFINITY : axes[axis.ordinal()];
    }

    /**
     * 该轴包含的全部属性（axisOf 的反查）。
     * 归并轴（CRIT / DEFENSE / MOBILITY）含多个属性，供核对器判定「轴→槽位合法性」：
     * 只要任一成员在该槽位合法，该轴即对该槽位有意义（如护甲韧性合法于内体 → 内体的防御轴有效）。
     */
    public static List<Attribute> members(Axis axis) {
        if (axis == null) {
            return List.of();
        }
        return switch (axis) {
            case ATTACK -> List.of(Attributes.ATTACK_DAMAGE);
            case ATTACK_SPEED -> List.of(Attributes.ATTACK_SPEED);
            case CRIT -> List.of(ModCombatAttributes.CRIT_CHANCE.get(), ModCombatAttributes.CRIT_DAMAGE.get());
            case HEALTH -> List.of(Attributes.MAX_HEALTH);
            case DEFENSE -> List.of(Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS);
            case MOBILITY -> List.of(Attributes.MOVEMENT_SPEED, ModCombatAttributes.DODGE_CHANCE.get());
            case RESILIENCE -> List.of(Attributes.KNOCKBACK_RESISTANCE);
            case LUCK -> List.of(Attributes.LUCK);
        };
    }

    /** 权重强度 k：配置 >0 用配置，否则回退内置默认（规则：0 = 不覆盖） */
    private static double strength() {
        double configured = ModConfig.geneWeightStrength;
        return configured > 0.0 ? configured : DEFAULT_STRENGTH;
    }

    /** 已纳入权重表的来源数（供 dev 核对器核对派生结果） */
    public static int sourceCount() {
        return AXIS_STRENGTH.size();
    }
}
