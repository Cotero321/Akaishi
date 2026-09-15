package com.example.akaishi.life.organ;

import com.example.akaishi.api.life.ISampleGroup;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyState;
import com.example.akaishi.life.sample.SampleGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 器官效果解析器：从玩家躯体状态解析"当前生效"的器官效果。
 * - 排斥值达到上限（PlayerBodyState.maxRejection()，配置可调，默认 100）的槽位器官视为完全排异失效
 * - 原生器官（原装部件）无效果，不参与任何加成
 * - 天敌生物器官冲突：同时移植天敌双方的器官 → 双方全部失效并触发排斥惩罚
 * common 供平台生效层（forge tick/事件）统一调用，避免效果判定逻辑散落平台侧。
 */
public final class OrganEffectResolver {

    /** 单个生效器官的解析结果 */
    public record ActiveOrgan(BodySlot slot, ItemStack stack, QualityTier tier, OrganEffect effect) {
    }

    /** 天敌生物配对：同时拥有双方器官 → 剧烈排异（排斥锁满、器官失效、周期性反噬惩罚） */
    private static final String[][] CONFLICT_PAIRS = {
            {"minecraft:cat", "minecraft:creeper"},      // 猫 × 苦力怕
            {"minecraft:fox", "minecraft:chicken"},      // 狐狸 × 鸡
            {"minecraft:wolf", "minecraft:sheep"},       // 狼 × 羊
            {"minecraft:frog", "minecraft:slime"},       // 青蛙 × 史莱姆
            {"minecraft:polar_bear", "minecraft:bee"}    // 北极熊 × 蜜蜂（熊袭蜂巢）
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
            // 原生器官无效果；排斥达上限（与 setRejection 钳制同源）→ 完全排异失效
            if (AkaishiOrganItem.isNative(organ)
                    || state.getRejection(slot) >= PlayerBodyState.maxRejection()
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

    /** 天敌冲突预防性检查：若移植来源为 entityId 的器官，是否会与已移植器官构成天敌冲突 */
    public static boolean wouldConflict(IPlayerBodyState state, String entityId) {
        if (state == null || entityId == null || entityId.isEmpty()) {
            return false;
        }
        for (String[] pair : CONFLICT_PAIRS) {
            // 该新器官来源属于天敌对中的一侧，则检查另一侧是否已在体内
            String enemy = entityId.equals(pair[0]) ? pair[1] : entityId.equals(pair[1]) ? pair[0] : null;
            if (enemy == null) {
                continue;
            }
            for (BodySlot slot : BodySlot.values()) {
                ItemStack organ = state.getOrgan(slot);
                if (organ.getItem() instanceof AkaishiOrganItem && !AkaishiOrganItem.isNative(organ)
                        && enemy.equals(AkaishiOrganItem.getEntityId(organ))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 是否拥有指定特殊效果（任意生效器官）；预收集版复用同一份生效器官列表，避免每 tick 重复全量收集 */
    public static boolean hasSpecial(List<ActiveOrgan> organs, OrganSpecial special) {
        for (ActiveOrgan organ : organs) {
            if (organ.effect() != null && organ.effect().special() == special) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSpecial(IPlayerBodyState state, OrganSpecial special) {
        return hasSpecial(collect(state), special);
    }

    /** 是否拥有指定被动技能（任意生效器官） */
    public static boolean hasPassive(IPlayerBodyState state, OrganPassive passive) {
        return countPassive(state, passive) > 0;
    }

    /** 预收集版：复用同一份生效器官列表 */
    public static boolean hasPassive(List<ActiveOrgan> organs, OrganPassive passive) {
        return countPassive(organs, passive) > 0;
    }

    /** 指定被动技能的持有数量（多器官可叠加，含突变词条被动） */
    public static int countPassive(IPlayerBodyState state, OrganPassive passive) {
        return strengthsOf(state, passive).count();
    }

    /** 预收集版：复用同一份生效器官列表 */
    public static int countPassive(List<ActiveOrgan> organs, OrganPassive passive) {
        return strengthsOf(organs, passive).count();
    }

    /** 被动持有强度：数量维度（跨器官叠加）+ 品质维度（携带该被动的最强来源器官品质序号，I=0 ~ IV=3）。
     *  品质阶梯真值源：数值型被动由生效层按 maxTierOrd 逐档增强（每档 +25%，与属性品质倍率同节奏），
     *  I 级行为与旧版完全一致，向后兼容不削弱。 */
    public record PassiveStrengths(int count, int maxTierOrd) {
        /** 品质阶梯系数：I=1.0 / II=1.25 / III=1.5 / IV=1.75（对数值型被动整体放大） */
        public float tierFactor() {
            return 1.0F + 0.25F * maxTierOrd;
        }
    }

    /** 聚合统计指定被动：来源数（count）+ 最高来源品质序号（maxTierOrd） */
    public static PassiveStrengths strengthsOf(IPlayerBodyState state, OrganPassive passive) {
        return strengthsOf(collect(state), passive);
    }

    /** 预收集版：复用同一份生效器官列表（每 tick 只 collect 一次，避免对每个被动重复全量收集） */
    public static PassiveStrengths strengthsOf(List<ActiveOrgan> organs, OrganPassive passive) {
        int count = 0;
        int maxOrd = -1;
        for (ActiveOrgan organ : organs) {
            boolean hit = false;
            for (OrganPassive p : passivesOf(organ.stack(), organ.effect())) {
                if (p == passive) {
                    count++;
                    hit = true;
                }
            }
            if (hit) {
                maxOrd = Math.max(maxOrd, organ.tier().ordinal());
            }
        }
        return new PassiveStrengths(count, maxOrd);
    }

    /** 器官属性集：特色覆盖优先，否则回退槽位模板（槽位无模板时返回空列表，避免 NPE） */
    public static List<OrganTemplate.AttributeBonus> bonusesOf(ItemStack organ, BodySlot slot, OrganEffect effect) {
        List<OrganTemplate.AttributeBonus> bonuses = effect != null && effect.attributes() != null
                ? effect.attributes()
                : OrganRegistry.get(slot) != null ? OrganRegistry.get(slot).bonuses() : Collections.emptyList();
        List<MutantTrait> mutations = AkaishiOrganItem.getMutations(organ);
        if (mutations.isEmpty()) {
            return bonuses;
        }
        // 合并突变词条属性（与生物效果同缩放：调用方统一乘品质×适配×突破）
        List<OrganTemplate.AttributeBonus> merged = new ArrayList<>(bonuses);
        for (MutantTrait mutation : mutations) {
            merged.addAll(mutation.attributes());
        }
        return merged;
    }

    /**
     * 被动合并视图：生物特色被动 + 突变词条被动（LinkedHashSet 去重保序）。
     * 供 tooltip 与平台常驻应用层统一读取，保证突变被动与生物被动同样生效。
     */
    public static List<OrganPassive> passivesOf(ItemStack organ, OrganEffect effect) {
        List<OrganPassive> passives = new ArrayList<>();
        Set<OrganPassive> seen = EnumSet.noneOf(OrganPassive.class);
        if (effect != null && effect.passives() != null) {
            for (OrganPassive passive : effect.passives()) {
                if (seen.add(passive)) {
                    passives.add(passive);
                }
            }
        }
        for (MutantTrait mutation : AkaishiOrganItem.getMutations(organ)) {
            for (OrganPassive passive : mutation.passives()) {
                if (seen.add(passive)) {
                    passives.add(passive);
                }
            }
        }
        return passives;
    }

    /** 生物 id → 生态分组（以空世界实体分类，结果缓存；不可采样亦缓存避免重复创建） */
    private static final Map<String, Optional<ISampleGroup>> GROUP_CACHE = new ConcurrentHashMap<>();

    public static ISampleGroup groupOf(String entityId, Level level) {
        // 未定型 / 无来源器官的 entityId 为 null：不得作为键（ConcurrentHashMap 禁用 null 键），
        // 直接返回 null 且不写缓存，交由调用方按"无生态分组"处理
        if (entityId == null || entityId.isEmpty()) {
            return null;
        }
        Optional<ISampleGroup> cached = GROUP_CACHE.get(entityId);
        if (cached != null) {
            return cached.orElse(null);
        }
        ResourceLocation id = ResourceLocation.tryParse(entityId);
        ISampleGroup group = null;
        if (id != null) {
            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id);
            if (type.isPresent()) {
                Entity entity = type.get().create(level); // 仅用于类型判定，不加入世界
                if (entity instanceof LivingEntity living) {
                    group = SampleGroup.of(living);
                }
            }
        }
        GROUP_CACHE.put(entityId, Optional.ofNullable(group));
        return group;
    }

    /** 生态套装档位：同组移植 ≥3 件激活小共鸣，≥6 件升级为大共鸣（9 槽中过半） */
    public record Synergy(ISampleGroup group, int count) {
        public boolean isActive() {
            return group != null && count >= 3;
        }

        public boolean isMajor() {
            return count >= 6;
        }
    }

    /**
     * 生态套装判定：统计非原生移植器官的同组来源数量，返回主导组档位。
     * 纯种构筑奖励——堆叠同一生态组的器官可激活隐藏效果（3/6 件两档）。
     */
    public static Synergy synergyOf(IPlayerBodyState state, Level level) {
        if (state == null) {
            return new Synergy(null, 0);
        }
        Map<ISampleGroup, Integer> counts = new HashMap<>();
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (!(organ.getItem() instanceof AkaishiOrganItem) || AkaishiOrganItem.isNative(organ)) {
                continue;
            }
            ISampleGroup group = groupOf(AkaishiOrganItem.getEntityId(organ), level);
            if (group != null) {
                counts.merge(group, 1, Integer::sum);
            }
        }
        ISampleGroup best = null;
        int bestCount = 0;
        for (Map.Entry<ISampleGroup, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return bestCount >= 3 ? new Synergy(best, bestCount) : new Synergy(null, 0);
    }
}
