package com.example.akaishi.forge.config.specs;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 配置域：生命研究机器、能量机器、净化与矩阵。
 * 仅由 {@link com.example.akaishi.forge.config.AkaishiConfig} 的 static 初始化按固定顺序调用；
 * 键名/注释/默认值/范围与拆分前逐字一致，调用顺序即 common.toml 分节顺序。
 */
public final class LifeMachineSpecs {

    // ---- 生命研究机器 ----
    public static ForgeConfigSpec.LongValue GENE_ANALYZER_LIFE_COST;
    public static ForgeConfigSpec.LongValue GENE_ANALYZER_LIFE_CAPACITY;
    public static ForgeConfigSpec.IntValue GENE_ANALYZER_PROCESS_TICKS;
    public static ForgeConfigSpec.DoubleValue GENE_ANALYZER_MIN_SUCCESS;
    public static ForgeConfigSpec.DoubleValue GENE_ANALYZER_MAX_SUCCESS;
    public static ForgeConfigSpec.LongValue LIFE_STRUCT_LIFE_COST;
    public static ForgeConfigSpec.IntValue LIFE_STRUCT_SOLID_COST;
    public static ForgeConfigSpec.LongValue LIFE_STRUCT_LIFE_CAPACITY;
    public static ForgeConfigSpec.IntValue LIFE_STRUCT_PROCESS_TICKS;
    public static ForgeConfigSpec.LongValue LIFE_BREEDER_LIFE_COST;
    public static ForgeConfigSpec.IntValue LIFE_BREEDER_CRYSTAL_COST;
    public static ForgeConfigSpec.LongValue LIFE_BREEDER_LIFE_CAPACITY;
    public static ForgeConfigSpec.IntValue LIFE_BREEDER_PROCESS_TICKS;
    public static ForgeConfigSpec.DoubleValue LIFE_BREEDER_MIN_SUCCESS;
    public static ForgeConfigSpec.DoubleValue LIFE_BREEDER_MAX_SUCCESS;
    public static ForgeConfigSpec.LongValue TRAIT_REFORGER_LIFE_COST;
    public static ForgeConfigSpec.LongValue TRAIT_REFORGER_LIFE_CAPACITY;
    public static ForgeConfigSpec.IntValue TRAIT_REFORGER_PROCESS_TICKS;
    public static ForgeConfigSpec.IntValue TRAIT_REFORGER_CRYSTAL_PER_RARITY;
    public static ForgeConfigSpec.LongValue TRANSGENE_FACTORY_LIFE_COST;
    public static ForgeConfigSpec.LongValue TRANSGENE_FACTORY_LIFE_CAPACITY;
    public static ForgeConfigSpec.IntValue TRANSGENE_FACTORY_PROCESS_TICKS;
    public static ForgeConfigSpec.IntValue SURGERY_IMPLANT_SOLID_COST;
    public static ForgeConfigSpec.LongValue SURGERY_IMPLANT_LIFE_COST;
    public static ForgeConfigSpec.IntValue SURGERY_EXTRACT_SOLID_COST;
    public static ForgeConfigSpec.LongValue SURGERY_EXTRACT_LIFE_COST;
    public static ForgeConfigSpec.LongValue SURGERY_LIFE_CAPACITY;
    public static ForgeConfigSpec.IntValue SURGERY_PROCESS_TICKS;
    public static ForgeConfigSpec.LongValue ORGAN_VAULT_LIFE_CAPACITY;
    public static ForgeConfigSpec.LongValue ORGAN_VAULT_KEEP_COST;
    public static ForgeConfigSpec.LongValue POTION_TABLE_LIFE_CAPACITY;

    // ---- 能量机器 ----
    public static ForgeConfigSpec.LongValue ENERGY_PROCESSOR_CHISHI_RATE;
    public static ForgeConfigSpec.LongValue ENERGY_PROCESSOR_CHISHI_CAPACITY;
    public static ForgeConfigSpec.LongValue ENERGY_PROCESSOR_TANK_CAPACITY;
    public static ForgeConfigSpec.LongValue ENERGY_PROCESSOR_CHISHI_COST;
    public static ForgeConfigSpec.LongValue ENERGY_LIQUEFIER_CHISHI_RATE;
    public static ForgeConfigSpec.LongValue ENERGY_LIQUEFIER_CHISHI_CAPACITY;
    public static ForgeConfigSpec.LongValue ENERGY_LIQUEFIER_TANK_CAPACITY;
    public static ForgeConfigSpec.LongValue FUEL_MIXER_CHISHI_RATE;
    public static ForgeConfigSpec.LongValue FUEL_MIXER_CHISHI_CAPACITY;
    public static ForgeConfigSpec.LongValue FUEL_MIXER_CHISHI_COST;
    public static ForgeConfigSpec.LongValue FUEL_MIXER_TANK_CAPACITY;
    public static ForgeConfigSpec.LongValue FUEL_CANNER_TANK_CAPACITY;
    public static ForgeConfigSpec.LongValue FUEL_CANNER_FILL_RATE;
    public static ForgeConfigSpec.LongValue ENERGY_AGGREGATOR_PER_INGOT;
    public static ForgeConfigSpec.LongValue ENERGY_AGGREGATOR_PER_GEODE;
    public static ForgeConfigSpec.LongValue ENERGY_AGGREGATOR_CAPACITY;
    public static ForgeConfigSpec.IntValue ENERGY_GENERATOR_RATE;
    public static ForgeConfigSpec.IntValue ENERGY_ASSEMBLY_RATE;
    public static ForgeConfigSpec.IntValue SUPER_GENERATOR_CORE_RATE;
    public static ForgeConfigSpec.LongValue ENERGY_CELL_BASE_CAPACITY;
    public static ForgeConfigSpec.LongValue UPGRADE_STATION_PER_UPGRADE;
    public static ForgeConfigSpec.LongValue UPGRADE_STATION_CAPACITY;
    public static ForgeConfigSpec.LongValue EQUIPMENT_FORGER_PER_FORGE;
    public static ForgeConfigSpec.LongValue EQUIPMENT_FORGER_CAPACITY;

    // ---- 净化与矩阵 ----
    public static ForgeConfigSpec.IntValue PURIFIER_ENERGY_PER_TICK;
    public static ForgeConfigSpec.IntValue PURIFIER_BURN_RATE;
    public static ForgeConfigSpec.LongValue PURIFIER_TOTAL_COST;
    public static ForgeConfigSpec.LongValue PURIFIER_RATE_FORMED;
    public static ForgeConfigSpec.LongValue PURIFIER_MATRIX_TOTAL_COST;
    public static ForgeConfigSpec.LongValue PURIFIER_MATRIX_RATE_FORMED;
    public static ForgeConfigSpec.LongValue LIFE_PURIFIER_CHISHI_RATE;
    public static ForgeConfigSpec.LongValue LIFE_PURIFIER_TOTAL_COST;
    public static ForgeConfigSpec.LongValue LIFE_PURIFIER_LIFE_COST;
    public static ForgeConfigSpec.LongValue LIFE_PURIFIER_CHISHI_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_PURIFIER_LIFE_CAPACITY;
    public static ForgeConfigSpec.IntValue LIFE_MATRIX_CONVERSIONS_PER_TICK;
    public static ForgeConfigSpec.LongValue LIFE_MATRIX_CONVERSION_COST;
    public static ForgeConfigSpec.LongValue LIFE_MATRIX_CHISHI_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_MATRIX_LIFE_CAPACITY;
    public static ForgeConfigSpec.IntValue LIFE_CONVERSION_PER_TICK;
    public static ForgeConfigSpec.LongValue LIFE_CONVERSION_CHISHI_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_CONVERSION_LIFE_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_AGGREGATION_COST;
    public static ForgeConfigSpec.LongValue LIFE_AGGREGATION_OUTPUT;
    public static ForgeConfigSpec.LongValue LIFE_AGGREGATION_CHISHI_CAPACITY;
    public static ForgeConfigSpec.LongValue LIFE_AGGREGATION_LIFE_CAPACITY;

    private LifeMachineSpecs() {
    }

    /** 按拆分前的定义顺序在共享 Builder 上构建本域各节（仅由 AkaishiConfig 调用） */
    public static void build(ForgeConfigSpec.Builder b) {
        // ==================== 生命研究机器 ====================
        b.push("life_machines");
        GENE_ANALYZER_LIFE_COST = b.comment("基因分析仪：解构一次消耗的生命能量")
                .defineInRange("geneAnalyzerLifeCost", 5_000L, 0L, Long.MAX_VALUE);
        GENE_ANALYZER_LIFE_CAPACITY = b.comment("基因分析仪：生命能量缓冲容量")
                .defineInRange("geneAnalyzerLifeCapacity", 10_000L, 0L, Long.MAX_VALUE);
        GENE_ANALYZER_PROCESS_TICKS = b.comment("基因分析仪：解构耗时 (tick)")
                .defineInRange("geneAnalyzerProcessTicks", 100, 1, Integer.MAX_VALUE);
        GENE_ANALYZER_MIN_SUCCESS = b.comment("基因分析仪：最低成功率（纯度 25）")
                .defineInRange("geneAnalyzerMinSuccessRate", 0.70, 0.0, 1.0);
        GENE_ANALYZER_MAX_SUCCESS = b.comment("基因分析仪：最高成功率（纯度 100）")
                .defineInRange("geneAnalyzerMaxSuccessRate", 0.95, 0.0, 1.0);
        LIFE_STRUCT_LIFE_COST = b.comment("生命结构台：构造一次消耗的生命能量")
                .defineInRange("lifeStructLifeCost", 80_000L, 0L, Long.MAX_VALUE);
        LIFE_STRUCT_SOLID_COST = b.comment("生命结构台：构造一次消耗的固态生命精华")
                .defineInRange("lifeStructSolidCost", 5, 0, 64);
        LIFE_STRUCT_LIFE_CAPACITY = b.comment("生命结构台：生命能量缓冲容量")
                .defineInRange("lifeStructLifeCapacity", 160_000L, 0L, Long.MAX_VALUE);
        LIFE_STRUCT_PROCESS_TICKS = b.comment("生命结构台：构造耗时 (tick)")
                .defineInRange("lifeStructProcessTicks", 120, 1, Integer.MAX_VALUE);
        LIFE_BREEDER_LIFE_COST = b.comment("生命培育器：培育一次消耗的生命能量")
                .defineInRange("lifeBreederLifeCost", 60_000L, 0L, Long.MAX_VALUE);
        LIFE_BREEDER_CRYSTAL_COST = b.comment("生命培育器：培育一次消耗的赤水晶")
                .defineInRange("lifeBreederCrystalCost", 2, 0, 64);
        LIFE_BREEDER_LIFE_CAPACITY = b.comment("生命培育器：生命能量缓冲容量")
                .defineInRange("lifeBreederLifeCapacity", 120_000L, 0L, Long.MAX_VALUE);
        LIFE_BREEDER_PROCESS_TICKS = b.comment("生命培育器：培育耗时 (tick)")
                .defineInRange("lifeBreederProcessTicks", 1000, 1, Integer.MAX_VALUE);
        LIFE_BREEDER_MIN_SUCCESS = b.comment("生命培育器：最低成功率")
                .defineInRange("lifeBreederMinSuccessRate", 0.35, 0.0, 1.0);
        LIFE_BREEDER_MAX_SUCCESS = b.comment("生命培育器：最高成功率")
                .defineInRange("lifeBreederMaxSuccessRate", 0.70, 0.0, 1.0);
        TRAIT_REFORGER_LIFE_COST = b.comment("词条重铸仪：重铸一次消耗的生命能量")
                .defineInRange("traitReforgerLifeCost", 120_000L, 0L, Long.MAX_VALUE);
        TRAIT_REFORGER_LIFE_CAPACITY = b.comment("词条重铸仪：生命能量缓冲容量")
                .defineInRange("traitReforgerLifeCapacity", 240_000L, 0L, Long.MAX_VALUE);
        TRAIT_REFORGER_PROCESS_TICKS = b.comment("词条重铸仪：重铸耗时 (tick)")
                .defineInRange("traitReforgerProcessTicks", 600, 1, Integer.MAX_VALUE);
        TRAIT_REFORGER_CRYSTAL_PER_RARITY = b.comment("词条重铸仪：每级稀有度消耗的赤水晶")
                .defineInRange("traitReforgerCrystalPerRarity", 2, 0, 64);
        TRANSGENE_FACTORY_LIFE_COST = b.comment("转基因工厂：加工一次消耗的生命能量")
                .defineInRange("transgeneFactoryLifeCost", 5_000L, 0L, Long.MAX_VALUE);
        TRANSGENE_FACTORY_LIFE_CAPACITY = b.comment("转基因工厂：生命能量缓冲容量")
                .defineInRange("transgeneFactoryLifeCapacity", 10_000L, 0L, Long.MAX_VALUE);
        TRANSGENE_FACTORY_PROCESS_TICKS = b.comment("转基因工厂：加工耗时 (tick)")
                .defineInRange("transgeneFactoryProcessTicks", 100, 1, Integer.MAX_VALUE);
        SURGERY_IMPLANT_SOLID_COST = b.comment("手术仓：移植消耗的固态生命精华")
                .defineInRange("surgeryImplantSolidCost", 3, 0, 64);
        SURGERY_IMPLANT_LIFE_COST = b.comment("手术仓：移植消耗的生命能量")
                .defineInRange("surgeryImplantLifeCost", 20_000L, 0L, Long.MAX_VALUE);
        SURGERY_EXTRACT_SOLID_COST = b.comment("手术仓：摘除消耗的固态生命精华")
                .defineInRange("surgeryExtractSolidCost", 1, 0, 64);
        SURGERY_EXTRACT_LIFE_COST = b.comment("手术仓：摘除消耗的生命能量")
                .defineInRange("surgeryExtractLifeCost", 5_000L, 0L, Long.MAX_VALUE);
        SURGERY_LIFE_CAPACITY = b.comment("手术仓：生命能量缓冲容量")
                .defineInRange("surgeryLifeCapacity", 100_000L, 0L, Long.MAX_VALUE);
        SURGERY_PROCESS_TICKS = b.comment("手术仓：单次手术耗时 (tick)")
                .defineInRange("surgeryProcessTicks", 80, 1, Integer.MAX_VALUE);
        ORGAN_VAULT_LIFE_CAPACITY = b.comment("器官储藏库：生命能量缓冲容量")
                .defineInRange("organVaultLifeCapacity", 100_000L, 0L, Long.MAX_VALUE);
        ORGAN_VAULT_KEEP_COST = b.comment("器官储藏库：每 tick 保育消耗（有器官时）")
                .defineInRange("organVaultKeepCostPerTick", 1L, 0L, Long.MAX_VALUE);
        POTION_TABLE_LIFE_CAPACITY = b.comment("药剂台：生命能量缓冲容量")
                .defineInRange("potionTableLifeCapacity", 100_000L, 0L, Long.MAX_VALUE);
        b.pop();

        // ==================== 能量机器 ====================
        b.push("energy_machines");
        ENERGY_PROCESSOR_CHISHI_RATE = b.comment("能量加工机：每 tick 抽取赤能源上限")
                .defineInRange("energyProcessorChishiRate", 1_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_PROCESSOR_CHISHI_CAPACITY = b.comment("能量加工机：赤能源池容量")
                .defineInRange("energyProcessorChishiCapacity", 20_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_PROCESSOR_TANK_CAPACITY = b.comment("能量加工机：各液体罐容量 (mb)")
                .defineInRange("energyProcessorTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        ENERGY_PROCESSOR_CHISHI_COST = b.comment("能量加工机：每次加工消耗的赤能源")
                .defineInRange("energyProcessorChishiCost", 5_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_LIQUEFIER_CHISHI_RATE = b.comment("能量液化器：每 tick 抽取赤能源上限")
                .defineInRange("energyLiquefierChishiRate", 1_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_LIQUEFIER_CHISHI_CAPACITY = b.comment("能量液化器：赤能源池容量")
                .defineInRange("energyLiquefierChishiCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_LIQUEFIER_TANK_CAPACITY = b.comment("能量液化器：液体罐容量 (mb)")
                .defineInRange("energyLiquefierTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        FUEL_MIXER_CHISHI_RATE = b.comment("燃料混合器：每 tick 抽取赤能源上限")
                .defineInRange("fuelMixerChishiRate", 1_000_000L, 0L, Long.MAX_VALUE);
        FUEL_MIXER_CHISHI_CAPACITY = b.comment("燃料混合器：赤能源池容量")
                .defineInRange("fuelMixerChishiCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        FUEL_MIXER_CHISHI_COST = b.comment("燃料混合器：每次混合消耗的赤能源")
                .defineInRange("fuelMixerChishiCost", 2_000_000L, 0L, Long.MAX_VALUE);
        FUEL_MIXER_TANK_CAPACITY = b.comment("燃料混合器：液体罐容量 (mb)")
                .defineInRange("fuelMixerTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        FUEL_CANNER_TANK_CAPACITY = b.comment("燃料灌装机：液体罐容量 (mb)")
                .defineInRange("fuelCannerTankCapacity", 16_000L, 0L, Long.MAX_VALUE);
        FUEL_CANNER_FILL_RATE = b.comment("燃料灌装机：每 tick 灌装量 (mb)")
                .defineInRange("fuelCannerFillRate", 1_000L, 0L, Long.MAX_VALUE);
        ENERGY_AGGREGATOR_PER_INGOT = b.comment("能量聚合器：每颗赤石粉聚合消耗的赤能源")
                .defineInRange("energyAggregatorEnergyPerIngot", 10_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_AGGREGATOR_PER_GEODE = b.comment("能量聚合器：晶洞升级一次消耗的赤能源")
                .defineInRange("energyAggregatorEnergyPerGeodeUpgrade", 10_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_AGGREGATOR_CAPACITY = b.comment("能量聚合器：赤能源存储容量")
                .defineInRange("energyAggregatorEnergyCapacity", 200_000_000L, 0L, Long.MAX_VALUE);
        ENERGY_GENERATOR_RATE = b.comment("能量发电机：每 tick 发电量")
                .defineInRange("energyGeneratorGenerateRate", 75, 0, Integer.MAX_VALUE);
        ENERGY_ASSEMBLY_RATE = b.comment("能量组装机：每 tick 发电量")
                .defineInRange("energyAssemblyGenerateRate", 3375, 0, Integer.MAX_VALUE);
        SUPER_GENERATOR_CORE_RATE = b.comment("超级发电机核心：每 tick 发电量")
                .defineInRange("superGeneratorCoreGenerateRate", 15_000, 0, Integer.MAX_VALUE);
        ENERGY_CELL_BASE_CAPACITY = b.comment("能量池：基础容量")
                .defineInRange("energyCellSerializerBaseCapacity", 1_000_000_000L, 0L, Long.MAX_VALUE);
        UPGRADE_STATION_PER_UPGRADE = b.comment("升级工作台：每次升级消耗的赤能源")
                .defineInRange("upgradeStationEnergyPerUpgrade", 20_000_000L, 0L, Long.MAX_VALUE);
        UPGRADE_STATION_CAPACITY = b.comment("升级工作台：赤能源存储容量")
                .defineInRange("upgradeStationEnergyCapacity", 40_000_000L, 0L, Long.MAX_VALUE);
        EQUIPMENT_FORGER_PER_FORGE = b.comment("装备锻造台：每次锻造消耗的赤能源")
                .defineInRange("equipmentForgerEnergyPerForge", 50_000_000L, 0L, Long.MAX_VALUE);
        EQUIPMENT_FORGER_CAPACITY = b.comment("装备锻造台：赤能源存储容量")
                .defineInRange("equipmentForgerEnergyCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        b.pop();

        // ==================== 净化与矩阵 ====================
        b.push("purifier_matrix");
        PURIFIER_ENERGY_PER_TICK = b.comment("净化塔：每 tick 消耗的赤能源")
                .defineInRange("purifierEnergyPerTick", 5, 0, Integer.MAX_VALUE);
        PURIFIER_BURN_RATE = b.comment("净化塔：燃料燃烧速率（每点产能 tick 数）")
                .defineInRange("purifierBurnRate", 10, 1, Integer.MAX_VALUE);
        PURIFIER_TOTAL_COST = b.comment("净化塔：单次提纯消耗（生命能量）")
                .defineInRange("purifierTotalCost", 500L, 0L, Long.MAX_VALUE);
        PURIFIER_RATE_FORMED = b.comment("净化塔：成型后每 tick 提纯量")
                .defineInRange("purifierRateFormed", 150L, 0L, Long.MAX_VALUE);
        PURIFIER_MATRIX_TOTAL_COST = b.comment("净化矩阵：单次提纯消耗（生命能量）")
                .defineInRange("purifierMatrixTotalCost", 500L, 0L, Long.MAX_VALUE);
        PURIFIER_MATRIX_RATE_FORMED = b.comment("净化矩阵：成型后每 tick 提纯量")
                .defineInRange("purifierMatrixRateFormed", 150L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_CHISHI_RATE = b.comment("生命净化机：每 tick 抽取赤能源上限")
                .defineInRange("lifePurifierChishiRate", 1_000_000L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_TOTAL_COST = b.comment("生命净化机：单次固化消耗的赤能源")
                .defineInRange("lifePurifierTotalCost", 10_000_000L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_LIFE_COST = b.comment("生命净化机：单次固化消耗的生命能量")
                .defineInRange("lifePurifierLifeCost", 1_000L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_CHISHI_CAPACITY = b.comment("生命净化机：赤能源池容量")
                .defineInRange("lifePurifierChishiCapacity", 20_000_000L, 0L, Long.MAX_VALUE);
        LIFE_PURIFIER_LIFE_CAPACITY = b.comment("生命净化机：生命能量缓冲容量")
                .defineInRange("lifePurifierLifeCapacity", 5_000L, 0L, Long.MAX_VALUE);
        LIFE_MATRIX_CONVERSIONS_PER_TICK = b.comment("生命矩阵：每 tick 转化次数")
                .defineInRange("lifeMatrixConversionsPerTick", 45, 0, Integer.MAX_VALUE);
        LIFE_MATRIX_CONVERSION_COST = b.comment("生命矩阵：单次转化消耗的赤能源")
                .defineInRange("lifeMatrixConversionCost", 10_000_000L, 0L, Long.MAX_VALUE);
        LIFE_MATRIX_CHISHI_CAPACITY = b.comment("生命矩阵：赤能源池容量")
                .defineInRange("lifeMatrixChishiCapacity", 500_000_000L, 0L, Long.MAX_VALUE);
        LIFE_MATRIX_LIFE_CAPACITY = b.comment("生命矩阵：生命能量缓冲容量")
                .defineInRange("lifeMatrixLifeCapacity", 5_000L, 0L, Long.MAX_VALUE);
        LIFE_CONVERSION_PER_TICK = b.comment("生命转化架构【外接】：每 tick 转化次数")
                .defineInRange("lifeConversionConversionsPerTick", 45, 0, Integer.MAX_VALUE);
        LIFE_CONVERSION_CHISHI_CAPACITY = b.comment("生命转化架构【外接】：赤能源池容量")
                .defineInRange("lifeConversionChishiCapacity", 500_000_000L, 0L, Long.MAX_VALUE);
        LIFE_CONVERSION_LIFE_CAPACITY = b.comment("生命转化架构【外接】：生命能量缓冲容量")
                .defineInRange("lifeConversionLifeCapacity", 5_000L, 0L, Long.MAX_VALUE);
        LIFE_AGGREGATION_COST = b.comment("生命聚合转化器：单次转化消耗的赤能源")
                .defineInRange("lifeAggregationConversionCost", 10_000_000L, 0L, Long.MAX_VALUE);
        LIFE_AGGREGATION_OUTPUT = b.comment("生命聚合转化器：单次转化产出的生命能量")
                .defineInRange("lifeAggregationConversionOutput", 10L, 0L, Long.MAX_VALUE);
        LIFE_AGGREGATION_CHISHI_CAPACITY = b.comment("生命聚合转化器：赤能源池容量")
                .defineInRange("lifeAggregationChishiCapacity", 100_000_000L, 0L, Long.MAX_VALUE);
        LIFE_AGGREGATION_LIFE_CAPACITY = b.comment("生命聚合转化器：生命能量缓冲容量")
                .defineInRange("lifeAggregationLifeCapacity", 100L, 0L, Long.MAX_VALUE);
        b.pop();
    }
}
