package com.example.akaishi.life.organ;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 器官效果解析器：从玩家躯体状态解析"当前生效"的器官效果。
 * - 排斥值达到上限（100）的槽位器官视为完全排异失效（属性/被动/特殊全部不生效）
 * - 原生器官（原装部件）无效果，不参与任何加成
 * - 天敌生物器官冲突：同时移植天敌双方的器官 → 双方全部失效并触发排斥惩罚
 * common 供平台生效层（forge tick/事件）统一调用，避免效果判定逻辑散落平台侧。
 */
public final class OrganEffectResolver {

    /** 器官失效的排斥阈值（排斥达到该值器官完全失效） */
    public static final int MAX_SAFE_REJECTION = 100;

    /** 单个生效器官的解析结果 */
    public record ActiveOrgan(BodySlot slot, ItemStack stack, QualityTier tier, OrganEffect effect) {
    }

    /** 天敌生物配对：同时拥有双方器官 → 剧烈排异（排斥锁满、器官失效、周期性反噬惩罚） */
    private static final String[][] CONFLICT_PAIRS = {
            {"minecraft:cat", "minecraft:creeper"},      // 猫 × 苦力怕
            {"minecraft:fox", "minecraft:chicken"},      // 狐狸 × 鸡
            {"minecraft:wolf", "minecraft:sheep"},       // 狼 × 羊
            {"minecraft:frog", "minecraft:slime"}        // 青蛙 × 史莱姆
    };

    private OrganEffectResolver() {
    }

    /** 收集玩家全部生效器官（排斥未达上限、非原生），含槽位/品质/特色效果 */
    public static List<ActiveOrgan> collect(IPlayerBodyState state) {
        List<ActiveOrgan> result = new ArrayList<>();
        if (state == null) {
            return result;
        }
        Set<BodySlot> conflicts = findConflicts(state);
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (!(organ.getItem() instanceof AkaishiOrganItem)) {
                continue;
            }
            // 原生器官无效果；排斥达上限 → 完全排异失效
            if (AkaishiOrganItem.isNative(organ)
                    || state.getRejection(slot) >= MAX_SAFE_REJECTION
                    || conflicts.contains(slot)) {
                continue;
            }
            QualityTier tier = AkaishiOrganItem.getTier(organ);
            if (tier == null) {
                continue;
            }
            OrganEffect effect = OrganEffectRegistry.get(AkaishiOrganItem.getEntityId(organ), slot);
            result.add(new ActiveOrgan(slot, organ, tier, effect));
        }
        return result;
    }

    /** 是否存在天敌器官冲突：返回参与冲突的槽位集合（空集合 = 无冲突） */
    public static Set<BodySlot> findConflicts(IPlayerBodyState state) {
        Set<BodySlot> result = EnumSet.noneOf(BodySlot.class);
        if (state == null) {
            return result;
        }
        // 当前已移植的非原生器官：槽位 → 生物 id
        Map<BodySlot, String> present = new EnumMap<>(BodySlot.class);
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (organ.getItem() instanceof AkaishiOrganItem && !AkaishiOrganItem.isNative(organ)) {
                present.put(slot, AkaishiOrganItem.getEntityId(organ));
            }
        }
        if (present.size() < 2) {
            return result;
        }
        for (String[] pair : CONFLICT_PAIRS) {
            boolean hasA = present.containsValue(pair[0]);
            boolean hasB = present.containsValue(pair[1]);
            if (!hasA || !hasB) {
                continue;
            }
            for (Map.Entry<BodySlot, String> entry : present.entrySet()) {
                if (pair[0].equals(entry.getValue()) || pair[1].equals(entry.getValue())) {
                    result.add(entry.getKey());
                }
            }
        }
        return result;
    }

    /** 是否拥有指定独特机制（任意生效器官） */
    public static boolean hasSpecial(IPlayerBodyState state, OrganSpecial special) {
        for (ActiveOrgan organ : collect(state)) {
            if (organ.effect() != null && organ.effect().special() == special) {
                return true;
            }
        }
        return false;
    }

    /** 是否拥有指定被动技能（任意生效器官） */
    public static boolean hasPassive(IPlayerBodyState state, OrganPassive passive) {
        return countPassive(state, passive) > 0;
    }

    /** 指定被动技能的持有数量（多器官可叠加） */
    public static int countPassive(IPlayerBodyState state, OrganPassive passive) {
        int count = 0;
        for (ActiveOrgan organ : collect(state)) {
            if (organ.effect() != null && organ.effect().passives() != null
                    && organ.effect().passives().contains(passive)) {
                count++;
            }
        }
        return count;
    }

    /** 器官属性集：特色覆盖优先，否则回退槽位模板（槽位无模板时返回空列表，避免 NPE） */
    public static List<OrganTemplate.AttributeBonus> bonusesOf(ItemStack organ, BodySlot slot, OrganEffect effect) {
        if (effect != null && effect.attributes() != null) {
            return effect.attributes();
        }
        OrganTemplate template = OrganRegistry.get(slot);
        return template != null ? template.bonuses() : Collections.emptyList();
    }
}
