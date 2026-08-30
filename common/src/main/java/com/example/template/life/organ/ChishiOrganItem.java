package com.example.template.life.organ;

import com.example.template.item.ModItems;
import com.example.template.life.body.BodySlot;
import com.example.template.life.linkage.OrganLinkage;
import com.example.template.life.sample.SampleGroup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 器官物品：9 个槽位各一个基础物品实例，来源生物与品质存于 NBT。
 * 每个器官 = 槽位模板/生物特色效果（属性 + 被动 + 特殊）× 品质倍率 × 适配度，
 * 移植后产生排斥值。
 * NBT：gene_source（来源分组，决定品质）、entity_id（具体生物，决定特色效果）、
 * compat（适配度 0-100）、native（是否原生部件）。
 */
public class ChishiOrganItem extends Item {

    public static final String TAG_GENE_SOURCE = "gene_source";
    public static final String TAG_ENTITY = "entity_id";
    /** 品质等级（培养舱升级时固化到 NBT，未固化时按来源映射） */
    public static final String TAG_QUALITY = "quality";
    /** 适配度（0-100）：决定属性生效比例、排斥增速与部位 debuff 等级 */
    public static final String TAG_COMPAT = "compat";
    /** 完整度（0-100）：结构台造器官时由基因序列纯度随机损耗 0-20 得到，决定品质档位 */
    public static final String TAG_PURITY = "purity";
    /** 突破强化倍率（突破药剂写入 1.5，默认 1.0） */
    public static final String TAG_BOOST = "boost";
    /** 原生部件标记：玩家初始 9 槽自动填充，与身体完全契合（适配度 100、无效果） */
    public static final String TAG_NATIVE = "native";
    /** 适配度上限 */
    public static final int MAX_COMPAT = 100;
    /** 原生器官固定适配度 */
    public static final int NATIVE_COMPAT = 100;

    /** 本物品固定的躯体槽位 */
    public final BodySlot slot;

    public ChishiOrganItem(BodySlot slot, Properties properties) {
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
    private static Item of(BodySlot slot) {
        return switch (slot) {
            case EYE -> ModItems.chishiOrganEye.get();
            case HEART -> ModItems.chishiOrganHeart.get();
            case LUNGS -> ModItems.chishiOrganLungs.get();
            case VISCERA -> ModItems.chishiOrganViscera.get();
            case KIDNEYS -> ModItems.chishiOrganKidneys.get();
            case LEFT_ARM -> ModItems.chishiOrganLeftArm.get();
            case RIGHT_ARM -> ModItems.chishiOrganRightArm.get();
            case LEFT_LEG -> ModItems.chishiOrganLeftLeg.get();
            case RIGHT_LEG -> ModItems.chishiOrganRightLeg.get();
        };
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

    /** 突破强化倍率（默认 1.0，未强化器官恒为 1.0） */
    public static double getBoost(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains(TAG_BOOST)) {
            return Math.max(1.0, stack.getTag().getDouble(TAG_BOOST));
        }
        return 1.0;
    }

    /** 写入突破倍率（仅接受 >1.0 的强化值） */
    public static void setBoost(ItemStack stack, double boost) {
        if (boost > 1.0) {
            stack.getOrCreateTag().putDouble(TAG_BOOST, boost);
        }
    }

    /**
     * 移植基础排斥值 = 品质 × 分组系数 × 完整度修正（OrganLinkage 统一推导，
     * 移植/冲突解除/tooltip 经本方法自动同步新联动数值），原生为 0。
     */
    public static int getBaseRejection(ItemStack stack) {
        return OrganLinkage.rejectionOf(stack);
    }

    // ===== 显示 =====

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 原生部件：与身体完全契合
        if (isNative(stack)) {
            tooltip.add(Component.translatable("gui.template_mod.organ.native"));
            return;
        }
        SampleGroup source = getSource(stack);
        QualityTier tier = getTier(stack);
        if (source == null || tier == null) {
            // 未定型器官（创造模式直接取用）：提示需培养舱定型
            tooltip.add(Component.translatable("gui.template_mod.organ.unformed"));
            return;
        }
        // 品质 + 来源基因
        tooltip.add(Component.translatable("gui.template_mod.organ.tier",
                Component.translatable("life.template_mod.organ_tier." + tier.name().toLowerCase())));
        tooltip.add(Component.translatable("gui.template_mod.organ.source",
                Component.translatable(source.getNameKey())));
        // 突破强化（突破药剂：效果 ×1.5）
        double boost = getBoost(stack);
        if (boost > 1.0) {
            tooltip.add(Component.translatable("gui.template_mod.organ.boost", formatValue(boost)));
        }
        // 适配度（按等级着色：≥80 绿 / 60-79 黄 / <60 红）
        int compat = getCompat(stack);
        String compatKey = compat >= 80 ? "gui.template_mod.organ.compat_high"
                : compat >= 60 ? "gui.template_mod.organ.compat_mid"
                : "gui.template_mod.organ.compat_low";
        tooltip.add(Component.translatable(compatKey, compat));
        // 完整度（决定品质档位，结构台造器官时由序列纯度损耗得到）
        int purity = getPurity(stack);
        if (purity > 0) {
            tooltip.add(Component.translatable("gui.template_mod.organ.purity", purity));
        }
        double compatFactor = compat / 100.0;
        // 生物特色效果（未注册时回退槽位模板属性，槽位无模板返回空列表）
        OrganEffect effect = OrganEffectRegistry.get(getEntityId(stack), slot);
        List<OrganTemplate.AttributeBonus> bonuses = OrganEffectResolver.bonusesOf(stack, slot, effect);
        // 属性加成（基础值 × 品质倍率 × 适配度 × 突破倍率）
        for (OrganTemplate.AttributeBonus bonus : bonuses) {
            double value = bonus.base() * tier.getMultiplier() * compatFactor * boost;
            boolean negative = value < 0;
            tooltip.add(Component.translatable(negative ? "gui.template_mod.organ.attribute_neg" : "gui.template_mod.organ.attribute",
                    formatValue(value), Component.translatable(bonus.attribute().getDescriptionId())));
        }
        // 被动技能（常驻/被动触发）
        if (effect != null && effect.passives() != null) {
            for (OrganPassive passive : effect.passives()) {
                tooltip.add(Component.translatable("gui.template_mod.organ.passive",
                        Component.translatable("life.template_mod.organ_passive." + passive.getId())));
            }
        }
        // 独特机制
        if (effect != null && effect.special() != null) {
            tooltip.add(Component.translatable("gui.template_mod.organ.special",
                    Component.translatable("life.template_mod.organ_special." + effect.special().getId())));
        }
        // 移植排斥（含分组系数与完整度修正的联动后数值）
        tooltip.add(Component.translatable("gui.template_mod.organ.rejection", getBaseRejection(stack)));
        tooltip.add(Component.translatable("gui.template_mod.organ.hint"));
    }

    /** 属性值格式化：整数显示整数，否则保留一位小数 */
    private static String formatValue(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }
}
