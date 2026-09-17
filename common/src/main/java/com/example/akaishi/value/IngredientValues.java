package com.example.akaishi.value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.value.RecipeIngredients.FluidAmount;
import com.example.akaishi.value.RecipeIngredients.RecipeCost;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/**
 * 原料项估值：把配方原料链折算成产物的附加价值。
 *
 * <p>口径：同一产物的多张配方取平均 → 单张配方内每格候选取平均 → 按产物数量摊分。
 * 求解用多遍 Gauss-Seidel 不动点迭代，且<b>仅当价值上升时写入</b>（单调不减），
 * 配合单项封顶，保证配方成环时既不发散也能收敛。
 */
public final class IngredientValues {

    /** 配置为 0 时的内置单项原料价值封顶 */
    private static final double DEFAULT_CAP = 80.0;
    private static final int DEFAULT_ITERATIONS = 4;
    /** 单格候选参与平均的数量上限，避免超大标签（如 #minecraft:planks）稀释价值 */
    private static final int MAX_CANDIDATES = 8;

    private IngredientValues() {
    }

    /** 单项原料价值封顶（配置 0 = 内置默认） */
    public static double cap() {
        return ModConfig.valueIngredientCap > 0 ? ModConfig.valueIngredientCap : DEFAULT_CAP;
    }

    /** 迭代遍数（配置 0 = 内置默认） */
    public static int iterations() {
        return ModConfig.valueIterations > 0 ? ModConfig.valueIterations : DEFAULT_ITERATIONS;
    }

    /**
     * 不动点迭代，就地提升 {@code values} 中配方产物的价值。
     *
     * @param values      物品价值表（自身分打底，迭代中被提升）
     * @param byResult    配方产物 → 配方列表
     * @param fluidPerMb  流体每 mB 分值
     */
    public static void iterate(Map<Item, Double> values, Map<Item, List<RecipeCost>> byResult,
                               Map<Fluid, Double> fluidPerMb) {
        double cap = cap();
        for (int pass = 0, rounds = iterations(); pass < rounds; pass++) {
            boolean changed = false;
            for (Map.Entry<Item, List<RecipeCost>> entry : byResult.entrySet()) {
                double candidate = Math.min(cap, averageTerm(entry.getValue(), values, fluidPerMb));
                if (candidate > values.getOrDefault(entry.getKey(), 0.0) + 1.0E-6) {
                    values.put(entry.getKey(), candidate);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
    }

    /** 展示用原料项分值（与迭代同口径，已封顶） */
    public static Map<Item, Double> terms(Map<Item, List<RecipeCost>> byResult, Map<Item, Double> values,
                                          Map<Fluid, Double> fluidPerMb) {
        Map<Item, Double> terms = new HashMap<>();
        double cap = cap();
        for (Map.Entry<Item, List<RecipeCost>> entry : byResult.entrySet()) {
            double average = averageTerm(entry.getValue(), values, fluidPerMb);
            if (average > 0.0) {
                terms.put(entry.getKey(), Math.min(cap, average));
            }
        }
        return terms;
    }

    /** 同一产物的多张配方取平均（如木板可由多种原木得到，统一按均价计） */
    public static double averageTerm(List<RecipeCost> recipes, Map<Item, Double> values,
                                     Map<Fluid, Double> fluidPerMb) {
        if (recipes.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (RecipeCost recipe : recipes) {
            sum += singleTerm(recipe, values, fluidPerMb);
        }
        return sum / recipes.size();
    }

    private static double singleTerm(RecipeCost recipe, Map<Item, Double> values,
                                     Map<Fluid, Double> fluidPerMb) {
        double sum = 0.0;
        for (Item[] slot : recipe.ingredients()) {
            int limit = Math.min(slot.length, MAX_CANDIDATES);
            if (limit <= 0) {
                continue;
            }
            double slotSum = 0.0;
            for (int i = 0; i < limit; i++) {
                slotSum += values.getOrDefault(slot[i], 0.0);
            }
            sum += slotSum / limit;
        }
        for (FluidAmount fluid : recipe.fluidsIn()) {
            Double perMb = fluidPerMb.get(fluid.fluid());
            if (perMb != null) {
                sum += fluid.mb() * perMb;
            }
        }
        return sum / Math.max(1, recipe.outputCount());
    }
}
