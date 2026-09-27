package com.example.akaishi.life.mechanical;

import com.example.akaishi.api.mechanical.trait.IMechanicalTrait;
import com.example.akaishi.item.MechanicalOrganItem;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 机械器官「按器官内 4 部件计数」的等级查询工具（纯查询、无副作用、不改任何现有行为）。
 * <p>
 * 口径：材料特性与 DNA 效果均为四级，等级 = clamp(该材料 / DNA 在本器官 4 个部件中出现的次数, 1, 4)；
 * 只在单器官内计算，<b>不跨器官累加</b>（跨器官由分发层取各器官单器官等级的<b>最大值</b>处理，见
 * forge 侧 {@code MechanicalAggregation}）。
 * <p>
 * 特性数量上限 {@link #MAX_TRAITS}（企划 D5）：超出时按「材料在器官中首次出现的顺序 × 材料内
 * traitIds 声明顺序」的遭遇顺序保留前 4 个。取舍：特性注册表为 ConcurrentHashMap、不保证顺序，
 * 故只能用遭遇顺序（确定性、可复现），而非全局注册顺序。
 */
public final class MechanicalLevels {

    /** 单器官特性数量上限（企划 D5） */
    public static final int MAX_TRAITS = 4;
    /** 等级上限（四级） */
    private static final int MAX_LEVEL = 4;

    private MechanicalLevels() {
    }

    /** 计数 → 等级：clamp(occurrences, 1, 4) */
    public static int levelOf(int occurrences) {
        return Math.max(1, Math.min(MAX_LEVEL, occurrences));
    }

    /** 材料 ID → 该材料在本器官的等级（出现 0 次不计入） */
    public static Map<String, Integer> materialLevels(ItemStack organ) {
        return toLevels(count(MechanicalOrganItem.getMaterialIds(organ), false));
    }

    /**
     * DNA ID → 该 DNA 在本器官的等级（none / 空白项跳过）。
     * <p>
     * 旧存档回退：逐部件列表 {@code mech_dna} 不存在时，若旧的 {@code mech_dna_id} 非空——
     * 按旧规则它必然意味着「四部件完全同源」⇒ 返回该 id 的等级 4；两者皆无 ⇒ 空 Map。
     */
    public static Map<String, Integer> dnaLevels(ItemStack organ) {
        List<String> partDna = MechanicalOrganItem.getPartDnaIds(organ);
        if (!partDna.isEmpty()) {
            return toLevels(count(partDna, true));
        }
        String legacy = MechanicalOrganItem.getDnaProfileId(organ);
        if (legacy != null && !legacy.isBlank() && !MechanicalDnaProfile.NONE_ID.equals(legacy)) {
            Map<String, Integer> fallback = new LinkedHashMap<>(1);
            fallback.put(legacy, MAX_LEVEL);
            return fallback;
        }
        return Map.of();
    }

    /**
     * 特性 ID → 该特性在本器官的等级。
     * 由材料等级汇总：同一特性被多个材料命中时取最大等级，再按 {@link IMechanicalTrait#maxLevel()} 截断；
     * 数量超过 {@link #MAX_TRAITS} 时按遭遇顺序保留前 4 个。
     */
    public static Map<String, Integer> traitLevels(ItemStack organ) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : materialLevels(organ).entrySet()) {
            MechanicalMaterial material = MechanicalMaterial.get(entry.getKey());
            if (material == null) {
                continue;
            }
            for (IMechanicalTrait trait : material.traits()) {
                int level = Math.min(entry.getValue(), trait.maxLevel());
                merged.merge(trait.getId(), level, Math::max);
            }
        }
        if (merged.size() <= MAX_TRAITS) {
            return merged;
        }
        Map<String, Integer> trimmed = new LinkedHashMap<>(MAX_TRAITS);
        int kept = 0;
        for (Map.Entry<String, Integer> entry : merged.entrySet()) {
            if (kept++ >= MAX_TRAITS) {
                break;
            }
            trimmed.put(entry.getKey(), entry.getValue());
        }
        return trimmed;
    }

    /** 统计 ID 出现次数（保持首次出现顺序；skipNone 时跳过 none 与空白项） */
    private static Map<String, Integer> count(List<String> ids, boolean skipNone) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (ids == null) {
            return counts;
        }
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            if (skipNone && MechanicalDnaProfile.NONE_ID.equals(id)) {
                continue;
            }
            counts.merge(id, 1, Integer::sum);
        }
        return counts;
    }

    /** 计数表 → 等级表（同序） */
    private static Map<String, Integer> toLevels(Map<String, Integer> counts) {
        Map<String, Integer> levels = new LinkedHashMap<>(counts.size());
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            levels.put(entry.getKey(), levelOf(entry.getValue()));
        }
        return levels;
    }
}
