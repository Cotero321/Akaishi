package com.example.akaishi.item;

import com.example.akaishi.api.mechanical.IMechanicalDnaEffect;
import com.example.akaishi.api.mechanical.trait.IMechanicalTrait;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitRegistry;
import com.example.akaishi.life.mechanical.MechanicalAssembledStats;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalLevels;
import com.example.akaishi.life.mechanical.MechanicalMaterial;
import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.example.akaishi.life.mechanical.MechanicalProperty;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 机械器官成品悬浮文本（企划 §2.5 · T6）。
 * <p>
 * 分页规则（按键态由客户端隔离读取）：
 * <ul>
 *   <li><b>未按键</b>：器官类型 + 十维属性概览 + 「按住 [Shift]/[Ctrl]」提示行；</li>
 *   <li><b>Shift</b>：材料特性区，逐条「特性名 Lv → 当前档效果描述」（等级实时算出，不落存档）；</li>
 *   <li><b>Ctrl</b>：材料与 DNA 区，四个部件逐行「[部件] 材料 ／ DNA来源 Lv」+ 调校/协同/效果。</li>
 * </ul>
 * <p>
 * <b>客户端隔离</b>：{@code net.minecraft.client.gui.screens.Screen} 是客户端类，本类位于 common 模块。
 * 按键读取统一走 {@link EnvExecutor#getEnvSpecific}（项目既有惯例）：服务端分支返回常量、不实例化引用
 * Screen 的内层 lambda，故专服启动/运行全程不会加载该客户端类（避免重蹈 {@code AkaishiModForge}
 * 构造期加载客户端类致专服崩溃的覆辙）。
 * <p>
 * <b>可测性</b>：行构造落在纯函数 {@link #buildLines(ItemStack, boolean, boolean)}（只依赖 NBT 与注册表，
 * 不触碰任何客户端类），可直接单测或探针调用。
 */
public final class MechanicalOrganTooltip {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    private static final String KEY_ORGAN_TYPE = "tooltip.akaishi.mechanical.organ_type";
    private static final String KEY_DNA = "tooltip.akaishi.mechanical.dna";
    private static final String KEY_SYNERGY = "tooltip.akaishi.mechanical.synergy";
    private static final String KEY_EFFECT = "tooltip.akaishi.mechanical.effect";
    private static final String KEY_HOLD_HINT = "tooltip.akaishi.mechanical.hold_hint";
    private static final String KEY_TRAITS = "tooltip.akaishi.mechanical.traits";
    private static final String KEY_TRAIT_LINE = "tooltip.akaishi.mechanical.trait_line";
    private static final String KEY_MATERIALS_DNA = "tooltip.akaishi.mechanical.materials_dna";
    private static final String KEY_PART_LINE = "tooltip.akaishi.mechanical.part_line";
    /** 等级口径提示：本页显示的是「该器官自身」等级，实际生效取全身最高（跨器官） */
    private static final String KEY_LEVEL_SCOPE = "tooltip.akaishi.mechanical.level_scope";
    /** 特性区标题后的上限提示（语言无关的灰度数字，提示企划 D5「单器官特性上限」） */
    private static final Component TRAITS_CAP_HINT =
            Component.literal(" §8[≤" + MechanicalLevels.MAX_TRAITS + "]");

    private MechanicalOrganTooltip() {
    }

    /** 悬停入口：客户端隔离读取按键状态后构造扩展行（由 {@code MechanicalOrganItem#appendHoverText} 调用） */
    public static List<Component> buildHoverLines(ItemStack stack) {
        return buildLines(stack, shiftDown(), ctrlDown());
    }

    /**
     * 纯函数：按按键状态产出扩展悬浮文本行（不含物品名本身，原版自动显示）。
     *
     * @param shift 是否按住 Shift（材料特性页）
     * @param ctrl  是否按住 Ctrl（材料与 DNA 页；Shift 优先）
     */
    public static List<Component> buildLines(ItemStack stack, boolean shift, boolean ctrl) {
        List<Component> lines = new ArrayList<>();
        MechanicalOrganType organType = MechanicalOrganItem.getOrganType(stack);
        if (organType == null) {
            return lines; // 非成品（无器官类型）：不附加任何行
        }
        lines.add(Component.translatable(KEY_ORGAN_TYPE,
                Component.translatable("mechanical.organ." + organType.name().toLowerCase())));

        if (shift) {
            appendTraits(stack, lines);
        } else if (ctrl) {
            appendMaterialsAndDna(stack, lines);
        } else {
            appendOverview(stack, lines);
            lines.add(Component.translatable(KEY_HOLD_HINT));
        }
        return lines;
    }

    // ==================== 分支内容 ====================

    /** 未按键：十维属性概览（既有展示口径，属"一句话概览"的数值本体） */
    private static void appendOverview(ItemStack stack, List<Component> lines) {
        MechanicalAssembledStats stats = MechanicalOrganItem.getStats(stack);
        if (stats == null) {
            return;
        }
        for (MechanicalProperty prop : MechanicalProperty.values()) {
            lines.add(Component.translatable(prop.tooltipKey(),
                    Component.literal(DF.format(stats.get(prop)))));
        }
    }

    /** Shift：材料特性区（特性名 Lv → 当前档效果描述） */
    private static void appendTraits(ItemStack stack, List<Component> lines) {
        lines.add(Component.translatable(KEY_TRAITS).copy().append(TRAITS_CAP_HINT));
        for (Map.Entry<String, Integer> entry : MechanicalLevels.traitLevels(stack).entrySet()) {
            IMechanicalTrait trait = MechanicalTraitRegistry.get(entry.getKey());
            if (trait == null) {
                continue;
            }
            int level = entry.getValue();
            lines.add(Component.translatable(KEY_TRAIT_LINE,
                    Component.translatable(trait.getTranslationKey()),
                    level,
                    Component.translatable(trait.descriptionKey(level))));
        }
        lines.add(Component.translatable(KEY_LEVEL_SCOPE));
    }

    /** Ctrl：材料与 DNA 区（四部件逐行 + 调校/协同/效果） */
    private static void appendMaterialsAndDna(ItemStack stack, List<Component> lines) {
        lines.add(Component.translatable(KEY_MATERIALS_DNA));

        List<String> materials = MechanicalOrganItem.getMaterialIds(stack);
        List<String> partDna = resolvePartDna(stack);
        Map<String, Integer> dnaLevels = MechanicalLevels.dnaLevels(stack);
        MechanicalPartType[] partTypes = MechanicalPartType.values();

        int parts = Math.min(Math.max(materials.size(), partDna.size()), partTypes.length);
        for (int i = 0; i < parts; i++) {
            String dnaId = i < partDna.size() ? partDna.get(i) : null;
            lines.add(Component.translatable(KEY_PART_LINE,
                    Component.translatable("mechanical.part." + partTypes[i].name().toLowerCase()),
                    materialName(i < materials.size() ? materials.get(i) : null),
                    dnaName(dnaId),
                    dnaLevelOf(dnaId, dnaLevels)));
        }
        lines.add(Component.translatable(KEY_LEVEL_SCOPE));

        // DNA 调校总览（旧单字段：仅四部件同源时才有值，与既有展示口径一致）
        String dnaId = MechanicalOrganItem.getDnaProfileId(stack);
        if (!MechanicalDnaProfile.NONE_ID.equals(dnaId)) {
            MechanicalDnaProfile dna = MechanicalDnaProfile.get(dnaId);
            if (dna != null) {
                lines.add(Component.translatable(KEY_DNA, Component.translatable(dna.descriptionKey())));
            }
        }

        // 协同加成
        for (String key : MechanicalOrganItem.getSynergies(stack)) {
            lines.add(Component.translatable(KEY_SYNERGY, Component.translatable(key)));
        }

        // 特殊效果：T3 起 NBT 允许同效果重复承载（供按部件计数），显示层按唯一 ID 折叠，等级另由 DNA Lv 体现
        for (IMechanicalDnaEffect effect : uniqueEffects(stack)) {
            lines.add(Component.translatable(KEY_EFFECT, Component.translatable(effect.getTranslationKey())));
        }
    }

    // ==================== 工具 ====================

    /** 逐部件 DNA 列表；旧存档只有单字段时视为「四部件同源」（与 MechanicalLevels 的回退口径一致） */
    private static List<String> resolvePartDna(ItemStack stack) {
        List<String> partDna = MechanicalOrganItem.getPartDnaIds(stack);
        if (!partDna.isEmpty()) {
            return partDna;
        }
        String legacy = MechanicalOrganItem.getDnaProfileId(stack);
        if (legacy != null && !legacy.isBlank() && !MechanicalDnaProfile.NONE_ID.equals(legacy)) {
            return List.of(legacy, legacy, legacy, legacy);
        }
        return List.of();
    }

    /** 效果按唯一 ID 折叠（保序），修掉 T3 取消 collectEffects 去重后同效果重复显示的问题 */
    private static List<IMechanicalDnaEffect> uniqueEffects(ItemStack stack) {
        Map<String, IMechanicalDnaEffect> unique = new LinkedHashMap<>();
        for (IMechanicalDnaEffect effect : MechanicalOrganItem.getEffects(stack)) {
            unique.putIfAbsent(effect.getId(), effect);
        }
        return new ArrayList<>(unique.values());
    }

    /** 材料名（未注册回退裸 id，缺失回退占位） */
    private static Component materialName(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return Component.literal("-");
        }
        MechanicalMaterial material = MechanicalMaterial.get(materialId);
        return material != null ? Component.translatable(material.descriptionKey()) : Component.literal(materialId);
    }

    /** DNA 来源名（none/缺失显示「无调校」，未注册回退裸 id） */
    private static Component dnaName(String dnaId) {
        if (dnaId == null || dnaId.isBlank() || MechanicalDnaProfile.NONE_ID.equals(dnaId)) {
            MechanicalDnaProfile none = MechanicalDnaProfile.get(MechanicalDnaProfile.NONE_ID);
            return Component.translatable(none != null ? none.descriptionKey()
                    : "mechanical.dna." + MechanicalDnaProfile.NONE_ID.replace(':', '.'));
        }
        MechanicalDnaProfile dna = MechanicalDnaProfile.get(dnaId);
        return dna != null ? Component.translatable(dna.descriptionKey()) : Component.literal(dnaId);
    }

    /** DNA 等级：该来源在本器官内的出现次数（无来源显示 0） */
    private static int dnaLevelOf(String dnaId, Map<String, Integer> dnaLevels) {
        if (dnaId == null || dnaId.isBlank() || MechanicalDnaProfile.NONE_ID.equals(dnaId)) {
            return 0;
        }
        return dnaLevels.getOrDefault(dnaId, 1);
    }

    // ==================== 客户端隔离 ====================

    /** 读取 Shift 状态；服务端恒 false，且不加载 Screen（内层 lambda 不会在服务端被实例化/执行） */
    private static boolean shiftDown() {
        return EnvExecutor.getEnvSpecific(() -> () -> Screen.hasShiftDown(), () -> () -> Boolean.FALSE);
    }

    /** 读取 Ctrl 状态；服务端恒 false，且不加载 Screen */
    private static boolean ctrlDown() {
        return EnvExecutor.getEnvSpecific(() -> () -> Screen.hasControlDown(), () -> () -> Boolean.FALSE);
    }
}
