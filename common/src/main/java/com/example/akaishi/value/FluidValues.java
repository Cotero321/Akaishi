package com.example.akaishi.value;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.fluid.ReactorFuels;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 流体估值：按「每桶分值 / 1000」折算每 mB 分值。
 *
 * <p>不引用任何第三方流体 API。本 mod 全部液体不注册桶与液体方块（bucket 为空气），
 * 因此桶装折算对这些液体必然落空，必须补一条无桶定价链路：
 * 先查流体价值表（配置 → 内置），再退回桶装物品价值。
 *
 * <p>内置无桶值全部由项目既有语义推导，不凭空捏数：
 * 燃料取 {@link ReactorFuels} 每棒热值；衰竭 = 燃料 × 1/5（既定的 5mb→1mb 燃烧换算）；
 * 活化衰竭 = 衰竭 × 2（由「废品口专属」转为「普通管道可抽」的可及性提升）；
 * 等离子体取其成分燃料中活化衰竭的最高值；能量液体取液化源物品价值（1 物品 = 1 桶）。
 */
public final class FluidValues {

    /** 配置为 0 时的内置每 mB 上限 */
    private static final double DEFAULT_PER_MB_CAP = 0.5;
    private static final double MB_PER_BUCKET = 1000.0;
    /** 反应堆燃烧换算：5mb 燃料 → 1mb 衰竭废品 */
    private static final double EXHAUSTED_RATIO = 1.0 / 5.0;
    /** 活化衰竭增值：由「仅废品口可储」转为「普通管道可抽」，可取性提升一倍 */
    private static final double ACTIVATED_MULTIPLIER = 2.0;

    /** 反应堆燃料 id（内置推导的起点） */
    private static final List<String> FUEL_IDS = List.of(
            ModFluids.SCULK_LIFE_FUEL_ID,
            ModFluids.NETHER_COMPOUND_FUEL_ID,
            ModFluids.END_MIXTURE_FUEL_ID,
            ModFluids.ADVANCED_MIXTURE_FUEL_ID,
            ModFluids.PURE_FUEL_ID,
            ModFluids.DRAGON_FUEL_ID,
            ModFluids.ULTIMATE_MIXTURE_FUEL_ID);

    /** 等离子体 id → 参与聚合的燃料 id（取其中活化衰竭值最高者） */
    private static final Map<String, List<String>> PLASMA_SOURCES = Map.of(
            ModFluids.MIXED_PLASMA_ID, List.of(
                    ModFluids.SCULK_LIFE_FUEL_ID,
                    ModFluids.ADVANCED_MIXTURE_FUEL_ID,
                    ModFluids.ULTIMATE_MIXTURE_FUEL_ID),
            ModFluids.NETHER_PLASMA_ID, List.of(
                    ModFluids.NETHER_COMPOUND_FUEL_ID,
                    ModFluids.PURE_FUEL_ID),
            ModFluids.END_PLASMA_ID, List.of(
                    ModFluids.END_MIXTURE_FUEL_ID,
                    ModFluids.DRAGON_FUEL_ID));

    private FluidValues() {
    }

    /** 每 mB 分值上限（配置 0 = 内置默认） */
    public static double perMbCap() {
        return ModConfig.valueFluidPerMbCap > 0 ? ModConfig.valueFluidPerMbCap : DEFAULT_PER_MB_CAP;
    }

    /** 构建每 mB 分值表；流体估值关闭时返回空表 */
    public static Map<Fluid, Double> build(Map<Item, Double> itemValues) {
        Map<Fluid, Double> perMb = new HashMap<>();
        if (!ModConfig.valueFluidEnabled) {
            return perMb;
        }
        double cap = perMbCap();
        Map<String, Double> configured = parseTable(ModConfig.valueFluidValues);
        Map<String, Double> builtin = builtinBucketValues(itemValues);
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
            if (key == null) {
                continue;
            }
            String id = key.toString();
            double perBucket = lookup(configured, id);
            if (perBucket < 0) {
                perBucket = lookup(builtin, id);
            }
            if (perBucket < 0) {
                perBucket = bucketItemValue(fluid, itemValues);
            }
            if (perBucket > 0) {
                perMb.put(fluid, Math.min(cap, perBucket / MB_PER_BUCKET));
            }
        }
        return perMb;
    }

    /** 无桶流体的内置每桶分值（全部由项目既有语义推导） */
    private static Map<String, Double> builtinBucketValues(Map<Item, Double> itemValues) {
        Map<String, Double> table = new HashMap<>();
        Map<String, Double> activated = new HashMap<>();
        for (String fuelId : FUEL_IDS) {
            Fluid fuel = ModFluids.get(fuelId);
            double heat = ReactorFuels.heatValue(fuel);
            if (heat <= 0) {
                continue;
            }
            put(table, fuel, heat);
            Fluid exhausted = ModFluids.exhaustedFuelFor(fuel);
            double waste = heat * EXHAUSTED_RATIO;
            put(table, exhausted, waste);
            Fluid activatedFuel = ModFluids.activatedFuelFor(exhausted);
            double revived = waste * ACTIVATED_MULTIPLIER;
            put(table, activatedFuel, revived);
            activated.put(idOf(fuel), revived);
        }
        for (Map.Entry<String, List<String>> entry : PLASMA_SOURCES.entrySet()) {
            double best = 0.0;
            for (String fuelId : entry.getValue()) {
                Double value = activated.get(idOf(ModFluids.get(fuelId)));
                if (value != null && value > best) {
                    best = value;
                }
            }
            put(table, ModFluids.get(entry.getKey()), best);
        }
        put(table, ModFluids.get(ModFluids.NETHER_PURE_ENERGY_ID),
                itemValues.getOrDefault(Items.NETHER_STAR, 0.0));
        put(table, ModFluids.get(ModFluids.NETHER_COMPOUND_ENERGY_ID),
                itemValues.getOrDefault(Items.WITHER_ROSE, 0.0));
        return table;
    }

    /** 桶装折算：仅对有桶流体生效 */
    private static double bucketItemValue(Fluid fluid, Map<Item, Double> itemValues) {
        Item bucket;
        try {
            bucket = fluid.getBucket();
        } catch (RuntimeException e) {
            return 0.0;
        }
        if (bucket == null || bucket == Items.AIR) {
            return 0.0;
        }
        return itemValues.getOrDefault(bucket, 0.0);
    }

    /** 写入表项：空液体、id 不可解析或分值非正时跳过 */
    private static void put(Map<String, Double> table, Fluid fluid, double value) {
        if (value <= 0.0 || fluid == null || fluid == Fluids.EMPTY) {
            return;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key != null) {
            table.put(key.toString(), value);
        }
    }

    /** 取液体注册 id；不可解析返回空串 */
    private static String idOf(Fluid fluid) {
        ResourceLocation key = fluid == null ? null : BuiltInRegistries.FLUID.getKey(fluid);
        return key == null ? "" : key.toString();
    }

    /** 查表：精确 id 优先，其次通配符；未命中返回 -1（分值恒非负，可作哨兵） */
    private static double lookup(Map<String, Double> table, String fluidId) {
        if (table.isEmpty()) {
            return -1.0;
        }
        Double exact = table.get(fluidId);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, Double> entry : table.entrySet()) {
            if (entry.getKey().indexOf('*') >= 0 && ValueTables.globMatch(entry.getKey(), fluidId)) {
                return entry.getValue();
            }
        }
        return -1.0;
    }

    /** 解析流体价值表（fluidId=每桶分值，支持 * 通配）；空列表返回空表 */
    private static Map<String, Double> parseTable(String[] configured) {
        if (configured == null || configured.length == 0) {
            return Map.of();
        }
        Map<String, Double> parsed = new HashMap<>();
        for (String raw : configured) {
            String[] pair = ValueTables.splitPair(raw);
            if (pair == null) {
                continue;
            }
            double score = ValueTables.parseDouble(pair[1], -1.0);
            if (score >= 0) {
                parsed.put(pair[0].toLowerCase(Locale.ROOT), score);
            }
        }
        return Map.copyOf(parsed);
    }
}
