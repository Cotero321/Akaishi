package com.example.akaishi.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.value.RecipeIngredients.RecipeCost;
import com.google.common.collect.Multimap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 价值分内核：纯计算，不持有平台对象、不产生副作用。
 *
 * <p>算法链路：
 * <ol>
 *   <li>自身分 intrinsicScore：稀有度 + 属性 + 耐久 + 品质词 + 硬度 + 标签价 + 手动覆盖；</li>
 *   <li>流体每 mB 价值（{@link FluidValues}）；</li>
 *   <li>配方原料项：4 遍不动点迭代求物品价值（{@link IngredientValues}）；</li>
 *   <li>功能项（魔法类）与掉落来源项（无配方物品）作为补充加权。</li>
 * </ol>
 *
 * <p>已修复上游缺陷：P0-3 硬度取值使用 {@link EmptyBlockGetter#INSTANCE} 而非 {@code null}，
 * 避免 {@code getDestroySpeed(null, null)} 抛空指针；P0-4 的死代码未移植。
 */
public final class ValueKernel {

    // ==================== 内置默认（配置为 0 时生效） ====================
    private static final double DEFAULT_COST_MULTIPLIER = 10.0;
    private static final double DEFAULT_INGREDIENT_WEIGHT = 0.75;
    private static final double DEFAULT_MAGIC_BONUS = 40.0;
    private static final double DEFAULT_LOOT_CAP = 60.0;

    /** 不可堆叠物品（工具/装备类）加成 */
    private static final double UNIQUE_ITEM_BONUS = 8.0;
    /** 硬度分封顶（硬度 / 2 后封顶） */
    private static final double HARDNESS_CAP = 25.0;
    /** 标签价分封顶 */
    private static final double TAG_VALUE_CAP = 60.0;
    /** 属性加成权重 */
    private static final double ATTACK_WEIGHT = 4.0;
    private static final double DEFENSE_WEIGHT = 3.0;

    /** 参与属性统计的装备槽（主手攻击 + 四件护甲） */
    private static final EquipmentSlot[] SCORED_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ValueKernel() {
    }

    // ==================== 配置取值（0 = 用内置默认） ====================

    public static double costMultiplier() {
        return pick(ModConfig.valueCostMultiplier, DEFAULT_COST_MULTIPLIER);
    }

    public static double ingredientWeight() {
        return pick(ModConfig.valueIngredientWeight, DEFAULT_INGREDIENT_WEIGHT);
    }

    public static double magicBonus() {
        return pick(ModConfig.valueMagicBonus, DEFAULT_MAGIC_BONUS);
    }

    public static double lootCap() {
        return pick(ModConfig.valueLootCap, DEFAULT_LOOT_CAP);
    }

    private static double pick(double configured, double fallback) {
        return configured > 0 ? configured : fallback;
    }

    /** 物品注册 id，未注册返回 null */
    public static String itemId(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key == null ? null : key.toString();
    }

    // ==================== 自身分 ====================

    /** 自身分：不依赖其他物品的固有价值 */
    public static double intrinsicScore(Item item, ValueTables tables) {
        String id = itemId(item);
        if (id == null || item == Items.AIR) {
            return 0.0;
        }
        double override = tables.overrideValue(id, item);
        if (override >= 0) {
            return override;
        }
        double score = baseByRarity(item);
        if (!(item instanceof BlockItem)) {
            score += 1.0;
        }
        score += attributeScore(item);
        score += Math.min(20.0, item.getMaxDamage() / 300.0);
        score += tables.tierBonus(id);
        if (item.getMaxStackSize() == 1) {
            score += UNIQUE_ITEM_BONUS;
        }
        score += Math.min(HARDNESS_CAP, hardness(item) / 2.0);
        score += Math.min(TAG_VALUE_CAP, tables.tagValue(item));
        return score;
    }

    private static double baseByRarity(Item item) {
        Rarity rarity = item.getRarity(new ItemStack(item));
        if (rarity == Rarity.EPIC) {
            return 12.0;
        }
        if (rarity == Rarity.RARE) {
            return 6.0;
        }
        if (rarity == Rarity.UNCOMMON) {
            return 3.0;
        }
        return 1.0;
    }

    private static double attributeScore(Item item) {
        double attack = 0.0;
        double defense = 0.0;
        for (EquipmentSlot slot : SCORED_SLOTS) {
            Multimap<Attribute, AttributeModifier> modifiers;
            try {
                modifiers = item.getDefaultAttributeModifiers(slot);
            } catch (RuntimeException e) {
                continue;
            }
            for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
                AttributeModifier modifier = entry.getValue();
                if (modifier.getOperation() != AttributeModifier.Operation.ADDITION) {
                    continue;
                }
                if (entry.getKey() == Attributes.ATTACK_DAMAGE) {
                    attack += modifier.getAmount();
                } else if (entry.getKey() == Attributes.ARMOR || entry.getKey() == Attributes.ARMOR_TOUGHNESS) {
                    defense += modifier.getAmount();
                }
            }
        }
        return attack * ATTACK_WEIGHT + defense * DEFENSE_WEIGHT;
    }

    /** 方块硬度分；P0-3 修正：用 EmptyBlockGetter 代替 null，异常不致命 */
    private static double hardness(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return 0.0;
        }
        try {
            BlockState state = blockItem.getBlock().defaultBlockState();
            return Math.max(0.0, state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        } catch (RuntimeException e) {
            return 0.0;
        }
    }

    // ==================== 造价分 ====================

    /** 造价分：自身分 + 原料项×权重 + 功能项 + 掉落项，再乘亲和 / 产出 / 倍率修正 */
    public static int computeCost(double baseScore, double ingredientTerm, double functionTerm, double lootTerm,
                                  int affinity, int outputCount) {
        double score = Math.max(0.0, baseScore)
                + Math.max(0.0, ingredientTerm) * ingredientWeight()
                + Math.max(0.0, functionTerm)
                + Math.max(0.0, lootTerm);
        double affinityMul = Math.max(0.25, 1.0 - Math.max(0, affinity) / 100.0);
        double raw = Math.max(3.0, Math.ceil(score * affinityMul * (1.0 + Math.max(0, outputCount) * 0.2)));
        double cost = raw * costMultiplier();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0, Math.round(cost)));
    }

    /** 用快照中的各项分值计算造价分（界面展示用） */
    public static int computeCost(Item item, ValueCache.Snapshot snapshot, int affinity, int outputCount) {
        return computeCost(
                snapshot.itemValue(item),
                snapshot.ingredientTerms().getOrDefault(item, 0.0),
                snapshot.functionTerms().getOrDefault(item, 0.0),
                snapshot.lootTerms().getOrDefault(item, 0.0),
                affinity, outputCount);
    }

    // ==================== 快照构建 ====================

    /**
     * 构建一次完整估值快照（工作量大，由 {@link ValueCache} 缓存）。
     *
     * @param manager      当前配方管理器
     * @param access       注册表访问（取配方产物）
     * @param tables       静态价值表
     * @param lootResolver 掉落来源分值解析（无配方物品），可为 null
     */
    public static ValueCache.Snapshot buildSnapshot(RecipeManager manager, RegistryAccess access,
                                                    ValueTables tables, ToDoubleFunction<Item> lootResolver) {
        List<RecipeCost> costs = RecipeIngredients.collect(manager, access);

        // 产物 → 配方列表
        Map<Item, List<RecipeCost>> byResult = new HashMap<>();
        for (RecipeCost cost : costs) {
            byResult.computeIfAbsent(cost.resultItem(), key -> new ArrayList<>()).add(cost);
        }

        // 1) 全物品自身分打底（含无配方物品，保证存储库中任意物品都有基础分）
        Map<Item, Double> values = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                values.put(item, intrinsicScore(item, tables));
            }
        }

        // 2) 流体每 mB 价值：桶装物品价值折算
        Map<Fluid, Double> fluidPerMb = FluidValues.build(values);

        // 3) 物品价值不动点迭代（单调不减，单项原料封顶保证收敛）
        IngredientValues.iterate(values, byResult, fluidPerMb);

        // 4) 原料项（展示用，与迭代同口径）与功能项
        Map<Item, Double> ingredientTerms = IngredientValues.terms(byResult, values, fluidPerMb);
        Map<Item, Double> functionTerms = new HashMap<>();
        double magic = magicBonus();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            String id = itemId(item);
            if (id != null && tables.isMagic(item, id)) {
                functionTerms.put(item, magic);
            }
        }

        // 5) 掉落来源项：无配方且未黑名单的物品
        Map<Item, Double> lootTerms = new HashMap<>();
        if (ModConfig.valueLootEnabled && lootResolver != null) {
            double lootCap = lootCap();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR || byResult.containsKey(item)) {
                    continue;
                }
                String id = itemId(item);
                if (id == null || tables.isLootBlacklisted(id)) {
                    continue;
                }
                double term = Math.min(lootCap, Math.max(0.0, lootResolver.applyAsDouble(item)));
                if (term <= 0.0) {
                    continue;
                }
                lootTerms.put(item, term);
                if (ModConfig.valueLootAutoApply) {
                    values.merge(item, term, Math::max);
                }
            }
        }

        return new ValueCache.Snapshot(
                ValueCache.fingerprint(manager),
                Map.copyOf(values),
                Map.copyOf(fluidPerMb),
                Map.copyOf(ingredientTerms),
                Map.copyOf(functionTerms),
                Map.copyOf(lootTerms));
    }
}
