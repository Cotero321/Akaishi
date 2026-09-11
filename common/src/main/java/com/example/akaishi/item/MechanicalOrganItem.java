package com.example.akaishi.item;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffect;
import com.example.akaishi.api.mechanical.MechanicalEffectRegistry;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IInstallableOrgan;
import com.example.akaishi.life.mechanical.MechanicalAssembledStats;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalIntegrationService;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.example.akaishi.life.mechanical.MechanicalProperty;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;

/**
 * 机械器官成品物品。
 * <p>
 * 由组装加工台将四个加工部件组装而成，存储最终聚合属性。
 * 渲染由 BEWLR 根据四个材料的纹理动态合成最终外观。
 */
public class MechanicalOrganItem extends Item implements IInstallableOrgan {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    // NBT 键
    public static final String TAG_ORGAN_TYPE = "mech_organ_type";
    public static final String TAG_OVERALL_MULTIPLIER = "mech_om";
    public static final String TAG_HEALTH = "mech_hp";
    public static final String TAG_ATTACK_DAMAGE = "mech_ad";
    public static final String TAG_ATTACK_SPEED = "mech_as";
    public static final String TAG_MOVEMENT_SPEED = "mech_ms";
    public static final String TAG_ARMOR = "mech_armor";
    public static final String TAG_CRIT_CHANCE = "mech_crit";
    public static final String TAG_CRIT_DAMAGE = "mech_crit_dmg";
    public static final String TAG_RANGE = "mech_range";
    public static final String TAG_DODGE = "mech_dodge";
    /** 四个材料 ID，用于渲染合成 */
    public static final String TAG_MATERIALS = "mech_materials";
    /** 协同加成键列表 */
    public static final String TAG_SYNERGIES = "mech_synergies";
    /** 特殊效果键列表（效果稳定命名空间 ID，如 akaishi:critical_boost） */
    public static final String TAG_EFFECTS = "mech_effects";
    /** DNA 调校来源 id（四部件同源时写入，供同源协同 / 整合加速判定） */
    public static final String TAG_DNA_ID = "mech_dna_id";

    public MechanicalOrganItem(Properties properties) {
        super(properties);
    }

    // ==================== 工厂方法 ====================

    /** 创建机械器官成品 ItemStack */
    public static ItemStack create(MechanicalAssembledStats stats,
                                   List<String> materialIds) {
        ItemStack stack = new ItemStack(getOrganItem(stats.organType()).get());
        CompoundTag tag = stack.getOrCreateTag();

        tag.putString(TAG_ORGAN_TYPE, stats.organType().name());
        for (MechanicalProperty prop : MechanicalProperty.values()) {
            tag.putDouble(tagKey(prop), stats.get(prop));
        }

        // 存储材料 ID 列表
        tag.putInt(TAG_MATERIALS + "_count", materialIds.size());
        for (int i = 0; i < materialIds.size(); i++) {
            tag.putString(TAG_MATERIALS + "_" + i, materialIds.get(i));
        }

        // 协同加成
        List<String> synergies = stats.synergyBonuses();
        tag.putInt(TAG_SYNERGIES + "_count", synergies.size());
        for (int i = 0; i < synergies.size(); i++) {
            tag.putString(TAG_SYNERGIES + "_" + i, synergies.get(i));
        }

        // 特殊效果（DNA 授予，写入稳定命名空间 ID；运行时由 AkaishiMechanicalEffectHandler 消费）
        List<IMechanicalDnaEffect> effects = stats.effects();
        tag.putInt(TAG_EFFECTS + "_count", effects.size());
        for (int i = 0; i < effects.size(); i++) {
            tag.putString(TAG_EFFECTS + "_" + i, effects.get(i).getId());
        }

        return stack;
    }

    /** 根据器官类型获取对应物品的注册器引用 */
    private static RegistrySupplier<Item> getOrganItem(MechanicalOrganType type) {
        return switch (type) {
            case EYE -> AkaishiMechanicalItems.mechanicalEye;
            case HEART -> AkaishiMechanicalItems.mechanicalHeart;
            case LUNG -> AkaishiMechanicalItems.mechanicalLungs;
            case VISCERA -> AkaishiMechanicalItems.mechanicalViscera;
            case KIDNEY -> AkaishiMechanicalItems.mechanicalKidneys;
            case LEFT_ARM -> AkaishiMechanicalItems.mechanicalLeftArm;
            case RIGHT_ARM -> AkaishiMechanicalItems.mechanicalRightArm;
            case LEFT_LEG -> AkaishiMechanicalItems.mechanicalLeftLeg;
            case RIGHT_LEG -> AkaishiMechanicalItems.mechanicalRightLeg;
        };
    }

    // ==================== 读取器 ====================

    /** 属性 → NBT 键（序号与 {@link MechanicalProperty} 一一对应） */
    private static String tagKey(MechanicalProperty prop) {
        return switch (prop) {
            case OVERALL_MULTIPLIER -> TAG_OVERALL_MULTIPLIER;
            case HEALTH -> TAG_HEALTH;
            case ATTACK_DAMAGE -> TAG_ATTACK_DAMAGE;
            case ATTACK_SPEED -> TAG_ATTACK_SPEED;
            case MOVEMENT_SPEED -> TAG_MOVEMENT_SPEED;
            case ARMOR -> TAG_ARMOR;
            case CRIT_CHANCE -> TAG_CRIT_CHANCE;
            case CRIT_DAMAGE -> TAG_CRIT_DAMAGE;
            case RANGE -> TAG_RANGE;
            case DODGE -> TAG_DODGE;
        };
    }

    @Nullable
    public static MechanicalOrganType getOrganType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ORGAN_TYPE)) return null;
        try { return MechanicalOrganType.valueOf(tag.getString(TAG_ORGAN_TYPE)); }
        catch (IllegalArgumentException e) { return null; }
    }

    /** 获取用于渲染的四个材料 ID */
    public static List<String> getMaterialIds(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return List.of();
        int count = tag.getInt(TAG_MATERIALS + "_count");
        if (count <= 0) return List.of();
        java.util.ArrayList<String> ids = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(tag.getString(TAG_MATERIALS + "_" + i));
        }
        return ids;
    }

    /** 读取成品携带的机械特殊效果（NBT 缺失或单条损坏时跳过该条，不使整件成品失效） */
    public static List<IMechanicalDnaEffect> getEffects(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return List.of();
        int count = tag.getInt(TAG_EFFECTS + "_count");
        if (count <= 0) return List.of();
        java.util.ArrayList<IMechanicalDnaEffect> effects = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String raw = tag.getString(TAG_EFFECTS + "_" + i);
            // 宽松解析：完整 ID 精确命中，旧枚举名（如 CRITICAL_BOOST）与裸 path 由 akaishi 命名空间回退兼容
            IMechanicalDnaEffect effect = MechanicalEffectRegistry.resolve(raw);
            if (effect != null && !MechanicalEffectRegistry.isNone(effect)) {
                effects.add(effect);
            }
        }
        return effects;
    }

    /** 写入 DNA 来源 id（none/空 视为无来源，直接清除该键） */
    public static void setDnaProfileId(ItemStack stack, @Nullable String dnaId) {
        if (dnaId == null || dnaId.isEmpty() || MechanicalDnaProfile.NONE_ID.equals(dnaId)) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(TAG_DNA_ID);
            }
            return;
        }
        stack.getOrCreateTag().putString(TAG_DNA_ID, dnaId);
    }

    /** 读取 DNA 来源 id（缺键回退 akaishi:none，永不返回 null） */
    public static String getDnaProfileId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_DNA_ID)) {
            return MechanicalDnaProfile.NONE_ID;
        }
        return tag.getString(TAG_DNA_ID);
    }

    /** 读取成品携带的协同加成描述键 */
    public static List<String> getSynergies(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return List.of();
        int count = tag.getInt(TAG_SYNERGIES + "_count");
        if (count <= 0) return List.of();
        java.util.ArrayList<String> keys = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String key = tag.getString(TAG_SYNERGIES + "_" + i);
            if (!key.isEmpty()) keys.add(key);
        }
        return keys;
    }

    /** 从成品 NBT 还原聚合属性（缺器官类型时返回 null）；协同与特殊效果一并还原，供生效层消费 */
    @Nullable
    public static MechanicalAssembledStats getStats(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        MechanicalOrganType type = getOrganType(stack);
        if (type == null) return null;
        double[] values = new double[MechanicalProperty.COUNT];
        for (MechanicalProperty prop : MechanicalProperty.values()) {
            values[prop.ordinal()] = tag.getDouble(tagKey(prop));
        }
        return new MechanicalAssembledStats(type, values, getSynergies(stack), getEffects(stack));
    }

    // ==================== 可安装器官契约 ====================

    /** 机械器官类型 → 躯体槽位（注意 LUNG/KIDNEY 与 BodySlot 的 LUNGS/KIDNEYS 复数差异） */
    @Override
    public BodySlot bodySlot(ItemStack stack) {
        MechanicalOrganType type = getOrganType(stack);
        if (type == null) return null;
        return switch (type) {
            case EYE -> BodySlot.EYE;
            case HEART -> BodySlot.HEART;
            case LUNG -> BodySlot.LUNGS;
            case VISCERA -> BodySlot.VISCERA;
            case KIDNEY -> BodySlot.KIDNEYS;
            case LEFT_ARM -> BodySlot.LEFT_ARM;
            case RIGHT_ARM -> BodySlot.RIGHT_ARM;
            case LEFT_LEG -> BodySlot.LEFT_LEG;
            case RIGHT_LEG -> BodySlot.RIGHT_LEG;
        };
    }

    /** 安装初始整合度：由首个材料决定（生命陶瓷 20 / 灵能复合 10 / 其余 0） */
    @Override
    public int initialIntegration(ItemStack stack) {
        List<String> materials = getMaterialIds(stack);
        if (materials.isEmpty()) return 0;
        MechanicalMaterial material = MechanicalMaterial.get(materials.get(0));
        return material != null ? MechanicalIntegrationService.getInitialIntegration(material) : 0;
    }

    // ==================== 覆写 ====================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        // 器官类型
        MechanicalOrganType organType = getOrganType(stack);
        if (organType != null) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.organ_type",
                    Component.translatable("mechanical.organ." + organType.name().toLowerCase())));
        }

        // 四个部件材料（存储下标与 CORE/MODULE/SHELL/COOLING 槽位顺序一致）
        List<String> materialIds = getMaterialIds(stack);
        MechanicalPartType[] partTypes = MechanicalPartType.values();
        for (int i = 0; i < materialIds.size() && i < partTypes.length; i++) {
            String id = materialIds.get(i);
            MechanicalMaterial material = MechanicalMaterial.get(id);
            Component name = material != null
                    ? Component.translatable(material.descriptionKey())
                    : Component.literal(id);
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.material_part",
                    Component.translatable("mechanical.part." + partTypes[i].name().toLowerCase()), name));
        }

        // 十维属性（倍率轴 + 九属性）
        for (MechanicalProperty prop : MechanicalProperty.values()) {
            tooltip.add(Component.translatable(prop.tooltipKey(),
                    Component.literal(DF.format(tag.getDouble(tagKey(prop))))));
        }

        // DNA 调校来源（无来源不显示）
        String dnaId = getDnaProfileId(stack);
        if (!MechanicalDnaProfile.NONE_ID.equals(dnaId)) {
            MechanicalDnaProfile dna = MechanicalDnaProfile.get(dnaId);
            if (dna != null) {
                tooltip.add(Component.translatable("tooltip.akaishi.mechanical.dna",
                        Component.translatable(dna.descriptionKey())));
            }
        }

        // 协同加成
        int synergyCount = tag.getInt(TAG_SYNERGIES + "_count");
        for (int i = 0; i < synergyCount; i++) {
            String key = tag.getString(TAG_SYNERGIES + "_" + i);
            if (!key.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.akaishi.mechanical.synergy",
                        Component.translatable(key)));
            }
        }

        // 特殊效果
        for (IMechanicalDnaEffect effect : getEffects(stack)) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.effect",
                    Component.translatable(effect.getTranslationKey())));
        }
    }
}