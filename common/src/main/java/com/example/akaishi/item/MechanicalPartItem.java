package com.example.akaishi.item;

import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartTemplate;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.example.akaishi.life.mechanical.MechanicalPartWeight;
import com.example.akaishi.life.mechanical.MechanicalProperty;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 机械部件模板/加工部件物品。
 * <p>
 * 通过 NBT 存储部件类型、材料、DNA、器官类型等信息。
 * 模板制造厂产出模板（processed=false），加工制作厂产出加工件（processed=true）。
 * 渲染由 BEWLR 动态合成形状纹理 + 材料纹理。
 */
public class MechanicalPartItem extends Item {

    // NBT 键
    public static final String TAG_PART_TYPE = "mech_part_type";
    public static final String TAG_MATERIAL_ID = "mech_material_id";
    public static final String TAG_ORGAN_TYPE = "mech_organ_type";
    public static final String TAG_DNA_PROFILE_ID = "mech_dna_id";
    public static final String TAG_PROCESSED = "mech_processed";

    public MechanicalPartItem(Properties properties) {
        super(properties);
    }

    // ==================== 工厂方法 ====================

    /** 创建部件模板 ItemStack */
    public static ItemStack createTemplate(MechanicalOrganType organType,
                                           MechanicalPartType partType,
                                           MechanicalMaterial material,
                                           MechanicalDnaProfile dna) {
        ItemStack stack = new ItemStack(AkaishiMechanicalItems.mechanicalPartTemplate.get());
        writeNbt(stack, organType, partType, material, dna, false);
        return stack;
    }

    /** 创建加工部件 ItemStack */
    public static ItemStack createProcessed(MechanicalOrganType organType,
                                            MechanicalPartType partType,
                                            MechanicalMaterial material,
                                            MechanicalDnaProfile dna) {
        ItemStack stack = new ItemStack(AkaishiMechanicalItems.mechanicalProcessedPart.get());
        writeNbt(stack, organType, partType, material, dna, true);
        return stack;
    }

    private static void writeNbt(ItemStack stack, MechanicalOrganType organType,
                                  MechanicalPartType partType, MechanicalMaterial material,
                                  MechanicalDnaProfile dna, boolean processed) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_PART_TYPE, partType.name());
        tag.putString(TAG_MATERIAL_ID, material.id());
        tag.putString(TAG_ORGAN_TYPE, organType.name());
        // 兜底校验：DNA 不允许装入该部件时回退 NONE（防手改 NBT / 旧存档越界），写入路径是唯一约束点
        tag.putString(TAG_DNA_PROFILE_ID, resolveAllowedDna(dna, partType).id());
        tag.putBoolean(TAG_PROCESSED, processed);
    }

    /** DNA 与部件不匹配（或缺失）时回退无调校；匹配则原样返回。 */
    private static MechanicalDnaProfile resolveAllowedDna(MechanicalDnaProfile dna, MechanicalPartType partType) {
        if (dna != null && dna.allows(partType)) {
            return dna;
        }
        MechanicalDnaProfile none = MechanicalDnaProfile.get(MechanicalDnaProfile.NONE_ID);
        return none != null ? none : dna;
    }

    // ==================== 读取器 ====================

    @Nullable
    public static MechanicalPartType getPartType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_PART_TYPE)) return null;
        try { return MechanicalPartType.valueOf(tag.getString(TAG_PART_TYPE)); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Nullable
    public static String getMaterialId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_MATERIAL_ID) : null;
    }

    @Nullable
    public static MechanicalOrganType getOrganType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ORGAN_TYPE)) return null;
        try { return MechanicalOrganType.valueOf(tag.getString(TAG_ORGAN_TYPE)); }
        catch (IllegalArgumentException e) { return null; }
    }

    @Nullable
    public static String getDnaProfileId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_DNA_PROFILE_ID) : null;
    }

    public static boolean isProcessed(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_PROCESSED);
    }

    // ==================== 覆写 ====================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                 List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        MechanicalPartType partType = getPartType(stack);
        String materialId = getMaterialId(stack);
        MechanicalOrganType organType = getOrganType(stack);
        boolean processed = isProcessed(stack);
        MechanicalMaterial material = materialId != null && !materialId.isEmpty()
                ? MechanicalMaterial.get(materialId) : null;
        MechanicalDnaProfile dna = MechanicalDnaProfile.get(getDnaProfileId(stack));

        // 部件类型
        if (partType != null) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.part_type",
                    Component.translatable("mechanical.part." + partType.name().toLowerCase())));
        }
        // 材料（已注册显示译名，未知 id 直接回显原值，避免信息丢失）
        if (material != null) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.material",
                    Component.translatable(material.descriptionKey())));
        } else if (materialId != null && !materialId.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.material",
                    Component.literal(materialId)));
        }
        // 器官类型
        if (organType != null) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.organ_type",
                    Component.translatable("mechanical.organ." + organType.name().toLowerCase())));
        }
        // DNA 来源
        if (dna != null && !MechanicalDnaProfile.NONE_ID.equals(dna.id())) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.dna",
                    Component.translatable(dna.descriptionKey())));
            Component allowed = allowedPartsLine(dna);
            if (allowed != null) {
                tooltip.add(allowed);
            }
        }
        // 十维权重（基础 + 材料 + DNA 修正，逐项 clamp 0~5；复用模板聚合逻辑保证与机器一致）
        if (organType != null && partType != null && material != null) {
            MechanicalDnaProfile dnaProfile = dna != null
                    ? dna : MechanicalDnaProfile.get(MechanicalDnaProfile.NONE_ID);
            MechanicalPartWeight weight = new MechanicalPartTemplate(organType, partType, material, dnaProfile)
                    .totalWeight();
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.weights"));
            for (MechanicalProperty prop : MechanicalProperty.values()) {
                tooltip.add(Component.translatable(prop.tooltipKey(),
                        Component.literal(String.valueOf(weight.get(prop)))));
            }
        }
        // 加工标记
        if (processed) {
            tooltip.add(Component.translatable("tooltip.akaishi.mechanical.processed"));
        }
    }

    /**
     * 「可装部件：核心 / 模块」行；无部件约束（全部允许）返回 {@code null}。
     * 供机械部件与基因来源（基因序列 Fragment 等）tooltip 复用。
     */
    @Nullable
    public static Component allowedPartsLine(MechanicalDnaProfile dna) {
        if (dna == null || !dna.hasPartConstraint()) {
            return null;
        }
        MutableComponent parts = Component.empty();
        boolean first = true;
        for (MechanicalPartType type : MechanicalPartType.values()) {
            if (!dna.allows(type)) {
                continue;
            }
            if (!first) {
                parts.append(Component.literal(" / "));
            }
            parts.append(Component.translatable("mechanical.part." + type.name().toLowerCase()));
            first = false;
        }
        return Component.translatable("tooltip.akaishi.mechanical.allowed_parts", parts);
    }
}