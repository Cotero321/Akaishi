package com.example.akaishi.forge.client;

import com.example.akaishi.forge.config.AkaishiConfig;
import com.example.akaishi.forge.config.AkaishiConfigSync;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏内配置界面（Cloth Config）：镜像 common.toml 新增的器官/排斥/培养机/机器倍率/开关区块。
 * 保存时写回 ForgeConfigSpec 并持久化到文件，再经 AkaishiConfigSync.sync() 推入
 * common ModConfig 并向在线玩家广播 S2C（客户端界面标尺即时跟随）。
 * 通过 Forge 扩展点注册到 Mods 列表的"配置"按钮。
 */
public final class AkaishiConfigScreenFactory {

    private AkaishiConfigScreenFactory() {
    }

    /** 注册 Mods 界面配置按钮（onClientSetup 客户端调用） */
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> build(parent)));
    }

    private static net.minecraft.client.gui.screens.Screen build(
            net.minecraft.client.gui.screens.Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.akaishi.title"))
                .setDefaultBackgroundTexture(new ResourceLocation("textures/block/deepslate_tiles.png"));
        // 点"完成"保存时：写回配置文件 → 推入 ModConfig → S2C 广播在线玩家
        builder.setSavingRunnable(() -> {
            AkaishiConfig.SPEC.save();
            AkaishiConfigSync.sync();
        });
        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory organQuality = category(builder, "organ_quality");
        doubleList(organQuality, eb, "organ_quality.multiplier", AkaishiConfig.ORGAN_TIER_MULTIPLIER);
        intList(organQuality, eb, "organ_quality.baseRejection", AkaishiConfig.ORGAN_TIER_BASE_REJECTION);
        intList(organQuality, eb, "organ_quality.growthInterval", AkaishiConfig.ORGAN_TIER_GROWTH_INTERVAL);

        ConfigCategory groups = category(builder, "sample_groups");
        doubleList(groups, eb, "sample_groups.groupFactor", AkaishiConfig.GROUP_REJECTION_FACTOR);

        ConfigCategory purity = category(builder, "purity");
        doubleField(purity, eb, "purity.purityRejectionCap", AkaishiConfig.PURITY_REJECTION_CAP);
        doubleField(purity, eb, "purity.purityCompatWeight", AkaishiConfig.PURITY_COMPAT_WEIGHT);

        ConfigCategory rejection = category(builder, "rejection");
        intField(rejection, eb, "rejection.maxRejection", AkaishiConfig.MAX_REJECTION);
        intField(rejection, eb, "rejection.warning", AkaishiConfig.REJECTION_WARNING);
        intField(rejection, eb, "rejection.poison", AkaishiConfig.REJECTION_POISON);
        intField(rejection, eb, "rejection.compatSevere", AkaishiConfig.COMPAT_SEVERE_THRESHOLD);
        intField(rejection, eb, "rejection.slotDebuffClean", AkaishiConfig.SLOT_DEBUFF_CLEAN_THRESHOLD);
        intField(rejection, eb, "rejection.slotDebuffSevere", AkaishiConfig.SLOT_DEBUFF_SEVERE_THRESHOLD);
        intField(rejection, eb, "rejection.growthIntervalMinTicks", AkaishiConfig.GROWTH_INTERVAL_MIN_TICKS);
        intField(rejection, eb, "rejection.conflictInterval", AkaishiConfig.CONFLICT_PUNISH_INTERVAL_TICKS);
        doubleField(rejection, eb, "rejection.conflictDamage", AkaishiConfig.CONFLICT_PUNISH_DAMAGE);
        intField(rejection, eb, "rejection.overloadLight", AkaishiConfig.OVERLOAD_LIGHT);
        intField(rejection, eb, "rejection.overloadHeavy", AkaishiConfig.OVERLOAD_HEAVY);

        ConfigCategory serum = category(builder, "serum");
        intField(serum, eb, "serum.washReduce", AkaishiConfig.SERUM_WASH_REDUCE);
        intField(serum, eb, "serum.washLimit", AkaishiConfig.SERUM_WASH_LIMIT);
        intField(serum, eb, "serum.cooldown", AkaishiConfig.SERUM_COOLDOWN_TICKS);

        ConfigCategory trait = category(builder, "trait");
        doubleField(trait, eb, "trait.benignRatio", AkaishiConfig.TRAIT_BENIGN_RATIO);
        intField(trait, eb, "trait.rarityHigh", AkaishiConfig.TRAIT_RARITY_HIGH_THRESHOLD);
        intField(trait, eb, "trait.rarityMid", AkaishiConfig.TRAIT_RARITY_MID_THRESHOLD);

        ConfigCategory cultivator = category(builder, "cultivator_upgrade");
        intList(cultivator, eb, "cultivator_upgrade.successRate", AkaishiConfig.CULTIVATOR_UPGRADE_SUCCESS);
        intList(cultivator, eb, "cultivator_upgrade.energyCost", AkaishiConfig.CULTIVATOR_UPGRADE_ENERGY);
        intList(cultivator, eb, "cultivator_upgrade.solidCost", AkaishiConfig.CULTIVATOR_UPGRADE_SOLID);
        intList(cultivator, eb, "cultivator_upgrade.processTicks", AkaishiConfig.CULTIVATOR_UPGRADE_TICKS);
        intField(cultivator, eb, "cultivator_upgrade.compatBonus", AkaishiConfig.CULTIVATOR_UPGRADE_COMPAT_BONUS);

        ConfigCategory machine = category(builder, "machine");
        doubleField(machine, eb, "machine.workSpeed", AkaishiConfig.MACHINE_WORK_SPEED);
        doubleField(machine, eb, "machine.costMultiplier", AkaishiConfig.MACHINE_COST_MULTIPLIER);

        ConfigCategory toggles = category(builder, "toggles");
        booleanToggle(toggles, eb, "toggles.decayZone", AkaishiConfig.DECAY_ZONE_ENABLED);
        booleanToggle(toggles, eb, "toggles.sunlightBurn", AkaishiConfig.SUNLIGHT_BURN_ENABLED);
        booleanToggle(toggles, eb, "toggles.overloadToggle", AkaishiConfig.OVERLOAD_ENABLED);

        // ===== 生命研究机器 =====
        ConfigCategory lifeMachines = category(builder, "life_machines");
        longField(lifeMachines, eb, "life_machines.geneAnalyzerLifeCost", AkaishiConfig.GENE_ANALYZER_LIFE_COST);
        longField(lifeMachines, eb, "life_machines.geneAnalyzerLifeCapacity", AkaishiConfig.GENE_ANALYZER_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.geneAnalyzerProcessTicks", AkaishiConfig.GENE_ANALYZER_PROCESS_TICKS);
        doubleField(lifeMachines, eb, "life_machines.geneAnalyzerMinSuccessRate", AkaishiConfig.GENE_ANALYZER_MIN_SUCCESS);
        doubleField(lifeMachines, eb, "life_machines.geneAnalyzerMaxSuccessRate", AkaishiConfig.GENE_ANALYZER_MAX_SUCCESS);
        longField(lifeMachines, eb, "life_machines.lifeStructLifeCost", AkaishiConfig.LIFE_STRUCT_LIFE_COST);
        intField(lifeMachines, eb, "life_machines.lifeStructSolidCost", AkaishiConfig.LIFE_STRUCT_SOLID_COST);
        longField(lifeMachines, eb, "life_machines.lifeStructLifeCapacity", AkaishiConfig.LIFE_STRUCT_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.lifeStructProcessTicks", AkaishiConfig.LIFE_STRUCT_PROCESS_TICKS);
        longField(lifeMachines, eb, "life_machines.lifeBreederLifeCost", AkaishiConfig.LIFE_BREEDER_LIFE_COST);
        intField(lifeMachines, eb, "life_machines.lifeBreederCrystalCost", AkaishiConfig.LIFE_BREEDER_CRYSTAL_COST);
        longField(lifeMachines, eb, "life_machines.lifeBreederLifeCapacity", AkaishiConfig.LIFE_BREEDER_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.lifeBreederProcessTicks", AkaishiConfig.LIFE_BREEDER_PROCESS_TICKS);
        doubleField(lifeMachines, eb, "life_machines.lifeBreederMinSuccessRate", AkaishiConfig.LIFE_BREEDER_MIN_SUCCESS);
        doubleField(lifeMachines, eb, "life_machines.lifeBreederMaxSuccessRate", AkaishiConfig.LIFE_BREEDER_MAX_SUCCESS);
        longField(lifeMachines, eb, "life_machines.traitReforgerLifeCost", AkaishiConfig.TRAIT_REFORGER_LIFE_COST);
        longField(lifeMachines, eb, "life_machines.traitReforgerLifeCapacity", AkaishiConfig.TRAIT_REFORGER_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.traitReforgerProcessTicks", AkaishiConfig.TRAIT_REFORGER_PROCESS_TICKS);
        intField(lifeMachines, eb, "life_machines.traitReforgerCrystalPerRarity", AkaishiConfig.TRAIT_REFORGER_CRYSTAL_PER_RARITY);
        longField(lifeMachines, eb, "life_machines.transgeneFactoryLifeCost", AkaishiConfig.TRANSGENE_FACTORY_LIFE_COST);
        longField(lifeMachines, eb, "life_machines.transgeneFactoryLifeCapacity", AkaishiConfig.TRANSGENE_FACTORY_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.transgeneFactoryProcessTicks", AkaishiConfig.TRANSGENE_FACTORY_PROCESS_TICKS);
        intField(lifeMachines, eb, "life_machines.surgeryImplantSolidCost", AkaishiConfig.SURGERY_IMPLANT_SOLID_COST);
        longField(lifeMachines, eb, "life_machines.surgeryImplantLifeCost", AkaishiConfig.SURGERY_IMPLANT_LIFE_COST);
        intField(lifeMachines, eb, "life_machines.surgeryExtractSolidCost", AkaishiConfig.SURGERY_EXTRACT_SOLID_COST);
        longField(lifeMachines, eb, "life_machines.surgeryExtractLifeCost", AkaishiConfig.SURGERY_EXTRACT_LIFE_COST);
        longField(lifeMachines, eb, "life_machines.surgeryLifeCapacity", AkaishiConfig.SURGERY_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.surgeryProcessTicks", AkaishiConfig.SURGERY_PROCESS_TICKS);
        longField(lifeMachines, eb, "life_machines.organVaultLifeCapacity", AkaishiConfig.ORGAN_VAULT_LIFE_CAPACITY);
        longField(lifeMachines, eb, "life_machines.organVaultKeepCostPerTick", AkaishiConfig.ORGAN_VAULT_KEEP_COST);
        longField(lifeMachines, eb, "life_machines.potionTableLifeCapacity", AkaishiConfig.POTION_TABLE_LIFE_CAPACITY);

        // ===== 能量机器 =====
        ConfigCategory energyMachines = category(builder, "energy_machines");
        longField(energyMachines, eb, "energy_machines.energyProcessorChishiRate", AkaishiConfig.ENERGY_PROCESSOR_CHISHI_RATE);
        longField(energyMachines, eb, "energy_machines.energyProcessorChishiCapacity", AkaishiConfig.ENERGY_PROCESSOR_CHISHI_CAPACITY);
        longField(energyMachines, eb, "energy_machines.energyProcessorTankCapacity", AkaishiConfig.ENERGY_PROCESSOR_TANK_CAPACITY);
        longField(energyMachines, eb, "energy_machines.energyProcessorChishiCost", AkaishiConfig.ENERGY_PROCESSOR_CHISHI_COST);
        longField(energyMachines, eb, "energy_machines.energyLiquefierChishiRate", AkaishiConfig.ENERGY_LIQUEFIER_CHISHI_RATE);
        longField(energyMachines, eb, "energy_machines.energyLiquefierChishiCapacity", AkaishiConfig.ENERGY_LIQUEFIER_CHISHI_CAPACITY);
        longField(energyMachines, eb, "energy_machines.energyLiquefierTankCapacity", AkaishiConfig.ENERGY_LIQUEFIER_TANK_CAPACITY);
        longField(energyMachines, eb, "energy_machines.fuelMixerChishiRate", AkaishiConfig.FUEL_MIXER_CHISHI_RATE);
        longField(energyMachines, eb, "energy_machines.fuelMixerChishiCapacity", AkaishiConfig.FUEL_MIXER_CHISHI_CAPACITY);
        longField(energyMachines, eb, "energy_machines.fuelMixerChishiCost", AkaishiConfig.FUEL_MIXER_CHISHI_COST);
        longField(energyMachines, eb, "energy_machines.fuelMixerTankCapacity", AkaishiConfig.FUEL_MIXER_TANK_CAPACITY);
        longField(energyMachines, eb, "energy_machines.fuelCannerTankCapacity", AkaishiConfig.FUEL_CANNER_TANK_CAPACITY);
        longField(energyMachines, eb, "energy_machines.fuelCannerFillRate", AkaishiConfig.FUEL_CANNER_FILL_RATE);
        longField(energyMachines, eb, "energy_machines.energyAggregatorEnergyPerIngot", AkaishiConfig.ENERGY_AGGREGATOR_PER_INGOT);
        longField(energyMachines, eb, "energy_machines.energyAggregatorEnergyPerGeodeUpgrade", AkaishiConfig.ENERGY_AGGREGATOR_PER_GEODE);
        longField(energyMachines, eb, "energy_machines.energyAggregatorEnergyCapacity", AkaishiConfig.ENERGY_AGGREGATOR_CAPACITY);
        intField(energyMachines, eb, "energy_machines.energyGeneratorGenerateRate", AkaishiConfig.ENERGY_GENERATOR_RATE);
        intField(energyMachines, eb, "energy_machines.energyAssemblyGenerateRate", AkaishiConfig.ENERGY_ASSEMBLY_RATE);
        intField(energyMachines, eb, "energy_machines.superGeneratorCoreGenerateRate", AkaishiConfig.SUPER_GENERATOR_CORE_RATE);
        longField(energyMachines, eb, "energy_machines.energyCellSerializerBaseCapacity", AkaishiConfig.ENERGY_CELL_BASE_CAPACITY);
        longField(energyMachines, eb, "energy_machines.upgradeStationEnergyPerUpgrade", AkaishiConfig.UPGRADE_STATION_PER_UPGRADE);
        longField(energyMachines, eb, "energy_machines.upgradeStationEnergyCapacity", AkaishiConfig.UPGRADE_STATION_CAPACITY);
        longField(energyMachines, eb, "energy_machines.equipmentForgerEnergyPerForge", AkaishiConfig.EQUIPMENT_FORGER_PER_FORGE);
        longField(energyMachines, eb, "energy_machines.equipmentForgerEnergyCapacity", AkaishiConfig.EQUIPMENT_FORGER_CAPACITY);

        // ===== 净化与矩阵 =====
        ConfigCategory purifierMatrix = category(builder, "purifier_matrix");
        intField(purifierMatrix, eb, "purifier_matrix.purifierEnergyPerTick", AkaishiConfig.PURIFIER_ENERGY_PER_TICK);
        intField(purifierMatrix, eb, "purifier_matrix.purifierBurnRate", AkaishiConfig.PURIFIER_BURN_RATE);
        longField(purifierMatrix, eb, "purifier_matrix.purifierTotalCost", AkaishiConfig.PURIFIER_TOTAL_COST);
        longField(purifierMatrix, eb, "purifier_matrix.purifierRateFormed", AkaishiConfig.PURIFIER_RATE_FORMED);
        longField(purifierMatrix, eb, "purifier_matrix.purifierMatrixTotalCost", AkaishiConfig.PURIFIER_MATRIX_TOTAL_COST);
        longField(purifierMatrix, eb, "purifier_matrix.purifierMatrixRateFormed", AkaishiConfig.PURIFIER_MATRIX_RATE_FORMED);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierChishiRate", AkaishiConfig.LIFE_PURIFIER_CHISHI_RATE);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierTotalCost", AkaishiConfig.LIFE_PURIFIER_TOTAL_COST);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierLifeCost", AkaishiConfig.LIFE_PURIFIER_LIFE_COST);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierChishiCapacity", AkaishiConfig.LIFE_PURIFIER_CHISHI_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierLifeCapacity", AkaishiConfig.LIFE_PURIFIER_LIFE_CAPACITY);
        intField(purifierMatrix, eb, "purifier_matrix.lifeMatrixConversionsPerTick", AkaishiConfig.LIFE_MATRIX_CONVERSIONS_PER_TICK);
        longField(purifierMatrix, eb, "purifier_matrix.lifeMatrixConversionCost", AkaishiConfig.LIFE_MATRIX_CONVERSION_COST);
        longField(purifierMatrix, eb, "purifier_matrix.lifeMatrixChishiCapacity", AkaishiConfig.LIFE_MATRIX_CHISHI_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifeMatrixLifeCapacity", AkaishiConfig.LIFE_MATRIX_LIFE_CAPACITY);
        intField(purifierMatrix, eb, "purifier_matrix.lifeConversionConversionsPerTick", AkaishiConfig.LIFE_CONVERSION_PER_TICK);
        longField(purifierMatrix, eb, "purifier_matrix.lifeConversionChishiCapacity", AkaishiConfig.LIFE_CONVERSION_CHISHI_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifeConversionLifeCapacity", AkaishiConfig.LIFE_CONVERSION_LIFE_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifeAggregationConversionCost", AkaishiConfig.LIFE_AGGREGATION_COST);
        longField(purifierMatrix, eb, "purifier_matrix.lifeAggregationConversionOutput", AkaishiConfig.LIFE_AGGREGATION_OUTPUT);
        longField(purifierMatrix, eb, "purifier_matrix.lifeAggregationChishiCapacity", AkaishiConfig.LIFE_AGGREGATION_CHISHI_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifeAggregationLifeCapacity", AkaishiConfig.LIFE_AGGREGATION_LIFE_CAPACITY);

        // ===== 端口与电池缓冲 =====
        ConfigCategory buffers = category(builder, "buffers");
        longField(buffers, eb, "buffers.lifeMatrixInputPortBufferCapacity", AkaishiConfig.LIFE_MATRIX_INPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.lifeMatrixOutputPortBufferCapacity", AkaishiConfig.LIFE_MATRIX_OUTPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.purifierEnergyInputPortBufferCapacity", AkaishiConfig.PURIFIER_INPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.minerPortBufferCapacity", AkaishiConfig.MINER_PORT_BUFFER);
        longField(buffers, eb, "buffers.minerEnergyInputBufferCapacity", AkaishiConfig.MINER_ENERGY_INPUT_BUFFER);
        longField(buffers, eb, "buffers.wirelessInputPortBufferCapacity", AkaishiConfig.WIRELESS_INPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.wirelessOutputPortBufferCapacity", AkaishiConfig.WIRELESS_OUTPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.genEnergyOutputPortBufferCapacity", AkaishiConfig.GEN_ENERGY_OUTPUT_BUFFER);
        longField(buffers, eb, "buffers.fusionEnergyOutputBufferCapacity", AkaishiConfig.FUSION_ENERGY_OUTPUT_BUFFER);
        longField(buffers, eb, "buffers.reactorEnergyOutputBufferCapacity", AkaishiConfig.REACTOR_ENERGY_OUTPUT_BUFFER);
        longField(buffers, eb, "buffers.lifeEnergyCellLifeCapacity", AkaishiConfig.LIFE_ENERGY_CELL_CAPACITY);
        longField(buffers, eb, "buffers.plasmaTankCapacity", AkaishiConfig.PLASMA_TANK_CAPACITY);

        // ===== 培养机提纯与分馏机 =====
        ConfigCategory cultivatorFractionator = category(builder, "cultivator_fractionator");
        longField(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorLifeCapacity", AkaishiConfig.CULTIVATOR_LIFE_CAPACITY);
        intList(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifySuccess", AkaishiConfig.CULTIVATOR_PURIFY_SUCCESS);
        longList(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifyEnergy", AkaishiConfig.CULTIVATOR_PURIFY_ENERGY);
        intList(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifySolid", AkaishiConfig.CULTIVATOR_PURIFY_SOLID);
        intList(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifyTicks", AkaishiConfig.CULTIVATOR_PURIFY_TICKS);
        intField(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifyGain", AkaishiConfig.CULTIVATOR_PURIFY_GAIN);
        longField(cultivatorFractionator, eb, "cultivator_fractionator.fractionatorEnergyCapacity", AkaishiConfig.FRACTIONATOR_ENERGY_CAPACITY);
        longField(cultivatorFractionator, eb, "cultivator_fractionator.fractionatorCostPerCraft", AkaishiConfig.FRACTIONATOR_COST_PER_CRAFT);
        intField(cultivatorFractionator, eb, "cultivator_fractionator.fractionatorProcessTicks", AkaishiConfig.FRACTIONATOR_PROCESS_TICKS);

        return builder.build();
    }

    private static ConfigCategory category(ConfigBuilder builder, String key) {
        return builder.getOrCreateCategory(Component.translatable("config.akaishi.cat." + key));
    }

    private static void intField(ConfigCategory cat, ConfigEntryBuilder eb,
                                 String key, ForgeConfigSpec.IntValue spec) {
        cat.addEntry(eb.startIntField(Component.translatable("config.akaishi." + key), spec.get())
                .setSaveConsumer(spec::set)
                .build());
    }

    private static void doubleField(ConfigCategory cat, ConfigEntryBuilder eb,
                                    String key, ForgeConfigSpec.DoubleValue spec) {
        cat.addEntry(eb.startDoubleField(Component.translatable("config.akaishi." + key), spec.get())
                .setSaveConsumer(spec::set)
                .build());
    }

    private static void longField(ConfigCategory cat, ConfigEntryBuilder eb,
                                  String key, ForgeConfigSpec.LongValue spec) {
        cat.addEntry(eb.startLongField(Component.translatable("config.akaishi." + key), spec.get())
                .setSaveConsumer(spec::set)
                .build());
    }

    private static void longList(ConfigCategory cat, ConfigEntryBuilder eb, String key,
                                 ForgeConfigSpec.ConfigValue<List<? extends Long>> spec) {
        List<Long> current = new ArrayList<>();
        for (Long v : spec.get()) {
            current.add(v);
        }
        cat.addEntry(eb.startLongList(Component.translatable("config.akaishi." + key), current)
                .setSaveConsumer(v -> spec.set(new ArrayList<>(v)))
                .build());
    }

    private static void booleanToggle(ConfigCategory cat, ConfigEntryBuilder eb,
                                      String key, ForgeConfigSpec.BooleanValue spec) {
        cat.addEntry(eb.startBooleanToggle(Component.translatable("config.akaishi." + key), spec.get())
                .setSaveConsumer(spec::set)
                .build());
    }

    private static void intList(ConfigCategory cat, ConfigEntryBuilder eb, String key,
                                ForgeConfigSpec.ConfigValue<List<? extends Integer>> spec) {
        List<Integer> current = new ArrayList<>();
        for (Integer v : spec.get()) {
            current.add(v);
        }
        cat.addEntry(eb.startIntList(Component.translatable("config.akaishi." + key), current)
                .setSaveConsumer(v -> spec.set(new ArrayList<>(v)))
                .build());
    }

    private static void doubleList(ConfigCategory cat, ConfigEntryBuilder eb, String key,
                                   ForgeConfigSpec.ConfigValue<List<? extends Double>> spec) {
        List<Double> current = new ArrayList<>();
        for (Double v : spec.get()) {
            current.add(v);
        }
        cat.addEntry(eb.startDoubleList(Component.translatable("config.akaishi." + key), current)
                .setSaveConsumer(v -> spec.set(new ArrayList<>(v)))
                .build());
    }
}
