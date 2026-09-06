package com.example.akaishi.life.organ;

import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.linkage.OrganLinkage;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 器官物品：9 个槽位各一个基础物品实例，来源生物与品质存于 NBT。
 * 每个器官 = 槽位模板/生物特色效果（属性 + 被动 + 特殊）× 品质倍率 × 适配度，
 * 移植后产生排斥值。
 * NBT：gene_source（来源分组，决定品质）、entity_id（具体生物，决定特色效果）、
 * compat（适配度 0-100）、native（是否原生部件）。
 */
public class AkaishiOrganItem extends Item {

    public static final String TAG_GENE_SOURCE = "gene_source";
    public static final String TAG_ENTITY = "entity_id";
    /** 品质等级（培养舱升级时固化到 NBT，未固化时按来源映射） */
    public static final String TAG_QUALITY = "quality";
    /** 适配度（0-100）：决定属性生效比例、排斥增速与部位 debuff 等级 */
    public static final String TAG_COMPAT = "compat";
    /** 完整度（0-100）：结构台造器官时由基因序列纯度随机损耗 0-20 得到，决定品质档位 */
    public static final String TAG_PURITY = "purity";
    /** 原生部件标记：玩家初始 9 槽自动填充，与身体完全契合（适配度 100、无效果） */
    public static final String TAG_NATIVE = "native";
    /** 突变词条列表（生命培育器施加，每条存 MutantTrait id，不可移除） */
    public static final String TAG_MUTATIONS = "mutations";
    /** 本次移植期间排异中和剂的清洗次数（移植时清零） */
    public static final String TAG_WASH_USED = "wash_used";
    /** 适配度上限 */
    public static final int MAX_COMPAT = 100;
    /** 原生器官固定适配度 */
    public static final int NATIVE_COMPAT = 100;

    /** 本物品固定的躯体槽位 */
    public final BodySlot slot;

    public AkaishiOrganItem(BodySlot slot, Properties properties) {
        super(properties);
        this.slot = slot;
    }

    // ===== 工厂 =====

    /** 构造一个指定槽位 + 基因来源 + 具体生物的器官物品（品质 = 来源初始等级，适配度 = 区间内随机偏低） */
    public static ItemStack create(BodySlot slot, SampleGroup source, String entityId) {
        return create(slot, source, entityId, 0);
    }

    /**
     * 构造器官并记录完整度（结构台产出用）。
     * 适配度 = 分组区间内随机偏置：纯度越高越接近区间上限（基因质量 → 契合质量，OrganLinkage.compatRoll）。
     * 未记录纯度（0）时行为与旧版一致（纯随机偏下端）。
     */
    public static ItemStack create(BodySlot slot, SampleGroup source, String entityId, int purity) {
        ItemStack stack = new ItemStack(of(slot));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_GENE_SOURCE, source.getId());
        tag.putString(TAG_ENTITY, entityId);
        QualityTier tier = QualityTier.of(source);
        if (tier != null) {
            tag.putString(TAG_QUALITY, tier.name());
        }
        // 适配度：分组区间内随机偏置，纯度越高越接近区间上限
        tag.putInt(TAG_COMPAT, OrganLinkage.compatRoll(source, purity, ThreadLocalRandom.current()));
        return stack;
    }

    /** 构造原生器官：与身体完全契合（适配度 100），无来源基因与效果 */
    public static ItemStack createNative(BodySlot slot) {
        ItemStack stack = new ItemStack(of(slot));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_NATIVE, true);
        tag.putString(TAG_QUALITY, QualityTier.I.name());
        tag.putInt(TAG_COMPAT, NATIVE_COMPAT);
        return stack;
    }

    /** 是否原生部件（玩家初始躯体的原装器官，无效果、不产生排斥） */
    public static boolean isNative(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_NATIVE);
    }

    /** 槽位 → 物品实例（与 ModItems 注册对应） */
    public static Item of(BodySlot slot) {
        return switch (slot) {
            case EYE -> ModItems.akaishiOrganEye.get();
            case HEART -> ModItems.akaishiOrganHeart.get();
            case LUNGS -> ModItems.akaishiOrganLungs.get();
            case VISCERA -> ModItems.akaishiOrganViscera.get();
            case KIDNEYS -> ModItems.akaishiOrganKidneys.get();
            case LEFT_ARM -> ModItems.akaishiOrganLeftArm.get();
            case RIGHT_ARM -> ModItems.akaishiOrganRightArm.get();
            case LEFT_LEG -> ModItems.akaishiOrganLeftLeg.get();
            case RIGHT_LEG -> ModItems.akaishiOrganRightLeg.get();
        };
    }

    /** 器官栈 → 槽位（非器官物品返回 null；词条部位专属判定用） */
    public static BodySlot slotOf(ItemStack stack) {
        return stack.getItem() instanceof AkaishiOrganItem organ ? organ.slot : null;
    }

    // ===== 读取 =====

    /** 基因来源（未定型/原生返回 null） */
    public static SampleGroup getSource(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? SampleGroup.byId(tag.getString(TAG_GENE_SOURCE)) : null;
    }

    /** 具体生物 id（如 "minecraft:cow"，无 NBT 返回 null） */
    public static String getEntityId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_ENTITY) ? tag.getString(TAG_ENTITY) : null;
    }

    /** 品质等级：优先读 NBT（培养升级结果），否则按来源映射（未定型返回 null） */
    public static QualityTier getTier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_QUALITY)) {
            try {
                return QualityTier.valueOf(tag.getString(TAG_QUALITY));
            } catch (IllegalArgumentException ignored) {
                // NBT 损坏时回退来源映射
            }
        }
        return QualityTier.of(getSource(stack));
    }

    /** 固化品质等级（培养舱升级调用），IV 为最高级 */
    public static void setTier(ItemStack stack, QualityTier tier) {
        stack.getOrCreateTag().putString(TAG_QUALITY, tier.name());
    }

    /** 适配度（0-100），未写入时按来源映射取区间下限（旧存档兼容） */
    public static int getCompat(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_COMPAT)) {
            return Math.max(0, Math.min(MAX_COMPAT, tag.getInt(TAG_COMPAT)));
        }
        SampleGroup source = getSource(stack);
        return source != null ? source.getCompatMin() : NATIVE_COMPAT;
    }

    /** 写入适配度（培养舱升级/基因药剂提升） */
    public static void setCompat(ItemStack stack, int value) {
        stack.getOrCreateTag().putInt(TAG_COMPAT, Math.max(0, Math.min(MAX_COMPAT, value)));
    }

    /** 累加适配度（不超出 0-100） */
    public static void addCompat(ItemStack stack, int amount) {
        setCompat(stack, getCompat(stack) + amount);
    }

    /** 完整度（0-100，结构台产出；无 NBT 返回 0） */
    public static int getPurity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_PURITY) ? tag.getInt(TAG_PURITY) : 0;
    }

    /** 写入完整度（结构台造器官时设置） */
    public static void setPurity(ItemStack stack, int purity) {
        stack.getOrCreateTag().putInt(TAG_PURITY, Math.max(0, Math.min(MAX_COMPAT, purity)));
    }

    /** 本次移植期间已被排异中和剂清洗的次数（摘除重植自动清零） */
    public static int getWashUsed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_WASH_USED) ? tag.getInt(TAG_WASH_USED) : 0;
    }

    /** 写入清洗次数（排异中和剂效果结算） */
    public static void setWashUsed(ItemStack stack, int count) {
        stack.getOrCreateTag().putInt(TAG_WASH_USED, Math.max(0, count));
    }

    /**
     * 移植基础排斥值 = 品质 × 分组系数 × 完整度修正（OrganLinkage 统一推导，
     * 移植/冲突解除/tooltip 经本方法自动同步新联动数值），原生为 0。
     */
    public static int getBaseRejection(ItemStack stack) {
        return OrganLinkage.rejectionOf(stack);
    }

    // ===== 基因突变（生命培育器施加的词条） =====

    /** 解析器官携带的突变词条（未知 id 容错跳过，保证旧存档/未来词条安全） */
    public static List<MutantTrait> getMutations(ItemStack stack) {
        List<MutantTrait> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_MUTATIONS, Tag.TAG_LIST)) {
            return result;
        }
        ListTag list = tag.getList(TAG_MUTATIONS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            MutantTrait trait = MutantTrait.valueOfSafe(list.getString(i));
            if (trait != null && !result.contains(trait)) {
                result.add(trait);
            }
        }
        return result;
    }

    /** 当前突变词条数量 */
    public static int getMutationCount(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_MUTATIONS, Tag.TAG_LIST)) {
            return 0;
        }
        return tag.getList(TAG_MUTATIONS, Tag.TAG_STRING).size();
    }

    /**
     * 词条承载上限 = 品质档序数 + 1（I=1 / II=2 / III=3 / IV=4）。
     * 品质越高基因越稳定，能容纳更多突变。未定型/原生器官不可突变。
     */
    public static int maxMutations(QualityTier tier) {
        return tier == null ? 0 : tier.ordinal() + 1;
    }

    /** 是否还能再培养（非原生、已定型、未达上限） */
    public static boolean canMutate(ItemStack stack) {
        if (isNative(stack)) {
            return false;
        }
        QualityTier tier = getTier(stack);
        return tier != null && getMutationCount(stack) < maxMutations(tier);
    }

    /** 追加一条突变词条（仅在未达上限时允许，调用方负责校验；同词条去重防止计数虚高） */
    public static void addMutation(ItemStack stack, MutantTrait trait) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.contains(TAG_MUTATIONS, Tag.TAG_LIST)
                ? tag.getList(TAG_MUTATIONS, Tag.TAG_STRING)
                : new ListTag();
        for (int i = 0; i < list.size(); i++) {
            if (trait.getId().equals(list.getString(i))) {
                return;
            }
        }
        list.add(StringTag.valueOf(trait.getId()));
        tag.put(TAG_MUTATIONS, list);
    }

    /** 原位替换第 index 条突变词条（词条重铸仪调用：保持顺序与承载数不变）；越界返回 false */
    public static boolean replaceMutation(ItemStack stack, int index, MutantTrait trait) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_MUTATIONS, Tag.TAG_LIST)) {
            return false;
        }
        ListTag list = tag.getList(TAG_MUTATIONS, Tag.TAG_STRING);
        if (index < 0 || index >= list.size()) {
            return false;
        }
        list.set(index, StringTag.valueOf(trait.getId()));
        tag.put(TAG_MUTATIONS, list);
        return true;
    }

    // ===== 显示 =====

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 原生部件：与身体完全契合
        if (isNative(stack)) {
            tooltip.add(Component.translatable("gui.akaishi.organ.native"));
            return;
        }
        SampleGroup source = getSource(stack);
        QualityTier tier = getTier(stack);
        if (source == null || tier == null) {
            // 未定型器官（创造模式直接取用）：提示需培养舱定型
            tooltip.add(Component.translatable("gui.akaishi.organ.unformed"));
            return;
        }
        // 品质 + 来源基因
        tooltip.add(Component.translatable("gui.akaishi.organ.tier",
                Component.translatable("life.akaishi.organ_tier." + tier.name().toLowerCase())));
        tooltip.add(Component.translatable("gui.akaishi.organ.source",
                Component.translatable(source.getNameKey())));
        // 具体生物（如 minecraft:cow → 牛）；未注册类型回退原始 id
        String entityId = getEntityId(stack);
        if (entityId != null) {
            ResourceLocation entityLoc = ResourceLocation.tryParse(entityId);
            EntityType<?> type = entityLoc != null
                    ? BuiltInRegistries.ENTITY_TYPE.getOptional(entityLoc).orElse(null) : null;
            tooltip.add(Component.translatable("gui.akaishi.organ.entity",
                    type != null ? Component.translatable(type.getDescriptionId())
                            : Component.literal(entityId)));
        }
        // 适配度（按等级着色：≥80 绿 / 60-79 黄 / <60 红）
        int compat = getCompat(stack);
        String compatKey = compat >= 80 ? "gui.akaishi.organ.compat_high"
                : compat >= 60 ? "gui.akaishi.organ.compat_mid"
                : "gui.akaishi.organ.compat_low";
        tooltip.add(Component.translatable(compatKey, compat));
        // 完整度（决定品质档位，结构台造器官时由序列纯度损耗得到）
        int purity = getPurity(stack);
        if (purity > 0) {
            tooltip.add(Component.translatable("gui.akaishi.organ.purity", purity));
        }
        double compatFactor = compat / 100.0;
        // 实际生效数值 = 基础值 × 品质倍率 × 适配度（forge 层同公式；突破倍率仅移植后加成）
        double scale = tier.getMultiplier() * compatFactor;
        // 生物特色效果（未注册时回退槽位模板属性，槽位无模板返回空列表）
        OrganEffect effect = OrganEffectRegistry.get(getEntityId(stack), slot);
        List<MutantTrait> mutations = getMutations(stack);
        // —— 属性栏：器官本体 + 全部词条，按属性归并求和（同属性只留一行净值，消除加减重复）——
        OrganTemplate slotTemplate = OrganRegistry.get(slot);
        Map<Attribute, Double> merged = new LinkedHashMap<>();
        if (effect != null && effect.attributes() != null) {
            mergeBonuses(merged, effect.attributes(), scale);
        } else if (slotTemplate != null) {
            mergeBonuses(merged, slotTemplate.bonuses(), scale);
        }
        for (MutantTrait mutation : mutations) {
            mergeBonuses(merged, mutation.attributes(), scale);
        }
        for (Map.Entry<Attribute, Double> entry : merged.entrySet()) {
            double value = entry.getValue();
            // 移动速度基础值为小秒速（+0.03），tooltip 按百分比展示更直观（+3%）
            boolean percent = Attributes.MOVEMENT_SPEED.equals(entry.getKey());
            String text = percent ? formatPercent(value * 100.0) : formatValue(value);
            tooltip.add(Component.translatable(value < 0 ? "gui.akaishi.organ.attribute_neg" : "gui.akaishi.organ.attribute",
                    text, Component.translatable(entry.getKey().getDescriptionId())));
        }
        // —— 被动栏：器官本体 + 全部词条的被动，按被动去重（同被动只列一次，来源计数合并）——
        Set<OrganPassive> passives = new LinkedHashSet<>();
        if (effect != null && effect.passives() != null) {
            passives.addAll(effect.passives());
        }
        for (MutantTrait mutation : mutations) {
            passives.addAll(mutation.passives());
        }
        for (OrganPassive passive : passives) {
            // 负面（代价型）被动红字警示，与畸变词条同色系；正面被动沿用蓝色前缀
            tooltip.add(Component.translatable(passive.isNegative()
                            ? "gui.akaishi.organ.passive_neg"
                            : "gui.akaishi.organ.passive",
                    Component.translatable("life.akaishi.organ_passive." + passive.getId())));
        }
        // 独特机制（生物特色专属，不并入属性/被动）
        if (effect != null && effect.special() != null) {
            tooltip.add(Component.translatable("gui.akaishi.organ.special",
                    Component.translatable("life.akaishi.organ_special." + effect.special().getId())));
        }
        // —— 基因突变词条区：只列词条名（畸变以警示色显示）；效果已并入上方属性/被动栏 ——
        if (!mutations.isEmpty()) {
            tooltip.add(Component.translatable("gui.akaishi.organ.mutations",
                    mutations.size(), maxMutations(tier)));
            for (MutantTrait mutation : mutations) {
                tooltip.add(Component.translatable(mutation.isDual()
                                ? "gui.akaishi.organ.mutation_dual"
                                : "gui.akaishi.organ.mutation",
                        Component.translatable(mutation.getNameKey())));
            }
            tooltip.add(Component.translatable("gui.akaishi.organ.mutation_hint"));
        }
        // 移植排斥（含分组系数与完整度修正的联动后数值）
        tooltip.add(Component.translatable("gui.akaishi.organ.rejection", getBaseRejection(stack)));
        tooltip.add(Component.translatable("gui.akaishi.organ.hint"));
    }

    /** 属性归并：一组属性条目（本体/词条）按同属性求和并入 merged（已乘品质×适配缩放） */
    private static void mergeBonuses(Map<Attribute, Double> merged,
                                     List<OrganTemplate.AttributeBonus> bonuses, double scale) {
        for (OrganTemplate.AttributeBonus bonus : bonuses) {
            merged.merge(bonus.attribute(), bonus.base() * scale, Double::sum);
        }
    }

    /** 属性值格式化：整数显示整数，否则保留一位小数 */
    private static String formatValue(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    /** 百分比数值格式化：保留至多一位小数并带符号（正值由模板补 +，负值自带 -；2.0 → "2%"，-1.5 → "-1.5%"） */
    private static String formatPercent(double percent) {
        BigDecimal bd = BigDecimal.valueOf(Math.abs(percent)).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        return (percent < 0 ? "-" : "") + bd.toPlainString() + "%";
    }
}
