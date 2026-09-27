package com.example.akaishi.forge.life;

import com.example.akaishi.api.mechanical.trait.IMechanicalTrait;
import com.example.akaishi.api.mechanical.trait.MechanicalTraitRegistry;
import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.mechanical.MechanicalDnaProfile;
import com.example.akaishi.life.mechanical.MechanicalLevels;
import com.example.akaishi.life.mechanical.MechanicalSpecialEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 机械器官「跨器官汇总」唯一入口（平衡收敛：汇总 + 绑等级）。
 * <p>
 * <b>唯一公式</b>（对每个特性 ID / 每个 DNA 效果 ID，先跨器官汇总，再只回调一次）：
 * <pre>
 * 有效等级 = clamp( max(各器官的等级) + (携带该条目的器官数 - 1) , 1 , maxLevel )
 * </pre>
 * 含义：多器官仍然有用（每个额外器官 +1 级），但受 {@code maxLevel} 封顶，不再线性失控。
 * 示例：1 器官 Lv4 ⇒ 4；2 器官各 Lv2 ⇒ 2+1=3；9 器官各 Lv4 ⇒ 4+8 截断 ⇒ 4。
 * <p>
 * <b>输入不动</b>：单器官等级仍由 {@link MechanicalLevels}（材料/DNA 在器官 4 部件中的出现次数、clamp 1~4）
 * 给出；本类只做"跨器官"这一层汇总。
 */
public final class MechanicalAggregation {

    /** DNA 效果等级上限（四级，与内置曲线一致）。 */
    public static final int EFFECT_MAX_LEVEL = 4;
    /** 特性注册表查不到定义时的兜底等级上限。 */
    private static final int DEFAULT_TRAIT_MAX_LEVEL = 4;

    private MechanicalAggregation() {
    }

    // ==================== 核心公式 ====================

    /** 汇总公式纯函数：{@code clamp(max + (器官数 - 1), 1, maxLevel)}；空集合返回 0（不生效）。 */
    public static int effectiveLevel(Collection<Integer> organLevels, int maxLevel) {
        if (organLevels == null || organLevels.isEmpty()) {
            return 0;
        }
        int max = 0;
        int count = 0;
        for (Integer level : organLevels) {
            if (level == null) {
                continue;
            }
            count++;
            if (level > max) {
                max = level;
            }
        }
        if (count == 0) {
            return 0;
        }
        int cap = Math.max(1, maxLevel);
        return Math.max(1, Math.min(cap, max + count - 1));
    }

    // ==================== 器官收集 ====================

    /** 玩家身上全部机械器官（按槽位顺序）。 */
    public static List<ItemStack> organs(IPlayerBodyState state) {
        if (state == null) {
            return List.of();
        }
        List<ItemStack> organs = new ArrayList<>(BodySlot.values().length);
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (organ.getItem() instanceof MechanicalOrganItem) {
                organs.add(organ);
            }
        }
        return organs;
    }

    // ==================== 跨器官汇总表 ====================

    /** 特性 ID → 跨器官有效等级（每个出现的特性一条）。 */
    public static Map<String, Integer> traitLevels(List<ItemStack> organs) {
        Map<String, List<Integer>> perTrait = new LinkedHashMap<>();
        for (ItemStack organ : organs) {
            MechanicalLevels.traitLevels(organ).forEach((id, level) ->
                    perTrait.computeIfAbsent(id, k -> new ArrayList<>(4)).add(level));
        }
        Map<String, Integer> out = new LinkedHashMap<>(perTrait.size());
        perTrait.forEach((id, levels) -> out.put(id, effectiveLevel(levels, traitMaxLevel(id))));
        return out;
    }

    /** DNA 效果 ID → 跨器官有效等级（每个出现的效果一条）。 */
    public static Map<ResourceLocation, Integer> effectLevels(List<ItemStack> organs) {
        Map<ResourceLocation, List<Integer>> perEffect = new LinkedHashMap<>();
        for (ItemStack organ : organs) {
            organEffectLevels(organ).forEach((id, level) ->
                    perEffect.computeIfAbsent(id, k -> new ArrayList<>(4)).add(level));
        }
        Map<ResourceLocation, Integer> out = new LinkedHashMap<>(perEffect.size());
        perEffect.forEach((id, levels) -> out.put(id, effectiveLevel(levels, EFFECT_MAX_LEVEL)));
        return out;
    }

    /**
     * 单个器官的「DNA 效果等级表」：把该器官的 DNA 来源等级汇总到其授予的效果上
     * （同一效果由多个来源授予时取该器官内最高等级）。空 Map 表示该器官无有效效果。
     */
    public static Map<ResourceLocation, Integer> organEffectLevels(ItemStack organ) {
        Map<String, Integer> dnaLevels = MechanicalLevels.dnaLevels(organ);
        if (dnaLevels.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, Integer> levels = new LinkedHashMap<>(dnaLevels.size());
        for (Map.Entry<String, Integer> entry : dnaLevels.entrySet()) {
            MechanicalDnaProfile profile = MechanicalDnaProfile.get(entry.getKey());
            if (profile == null || MechanicalSpecialEffect.isNone(profile.effect())) {
                continue;
            }
            ResourceLocation id = profile.effect().id();
            if (id != null) {
                levels.merge(id, entry.getValue(), Math::max);
            }
        }
        return levels;
    }

    /** 特性的等级上限（{@link IMechanicalTrait#maxLevel()}）；未注册时回退 4。 */
    private static int traitMaxLevel(String traitId) {
        IMechanicalTrait trait = MechanicalTraitRegistry.resolve(traitId);
        return trait != null ? trait.maxLevel() : DEFAULT_TRAIT_MAX_LEVEL;
    }
}
