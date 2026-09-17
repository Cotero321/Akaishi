package com.example.akaishi.forge.config;

import com.example.akaishi.config.ConfigSyncS2C;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.value.ValueReloadHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

/**
 * Forge 配置 → common 值同步。在配置加载/重载时把 ForgeConfigSpec 的当前值
 * 写入 {@link ModConfig} 的 volatile 字段，实现热更新（改配置后无需重启）。
 */
public final class AkaishiConfigSync {

    private AkaishiConfigSync() {
    }

    /** 注册到 MOD 事件总线（ModConfigEvent.Loading / Reloading） */
    public static void onModConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() == AkaishiConfig.SPEC) {
            sync();
        }
    }

    /** spec 当前值 → ModConfig（配置加载/重载与游戏内界面保存时调用，末尾附带 S2C 广播） */
    public static void sync() {
        // 反应堆
        ModConfig.reactorEnergyPerSlot = AkaishiConfig.REACTOR_ENERGY_PER_SLOT.get();
        ModConfig.reactorBaseTemp = AkaishiConfig.REACTOR_BASE_TEMP.get();
        ModConfig.reactorPassiveCool = AkaishiConfig.REACTOR_PASSIVE_COOL.get();
        ModConfig.reactorCoolerCool = AkaishiConfig.REACTOR_COOLER_COOL.get();
        ModConfig.reactorDrainBase = AkaishiConfig.REACTOR_DRAIN_BASE.get();
        ModConfig.reactorWasteRatio = AkaishiConfig.REACTOR_WASTE_RATIO.get();
        ModConfig.reactorWasteCapacity = AkaishiConfig.REACTOR_WASTE_CAPACITY.get();
        ModConfig.reactorTempMax = AkaishiConfig.REACTOR_TEMP_MAX.get();
        ModConfig.reactorTempOptMin = AkaishiConfig.REACTOR_TEMP_OPT_MIN.get();
        ModConfig.reactorTempOptMax = AkaishiConfig.REACTOR_TEMP_OPT_MAX.get();
        ModConfig.reactorTempWarn = AkaishiConfig.REACTOR_TEMP_WARN.get();
        ModConfig.reactorExplosionDelayTicks = AkaishiConfig.REACTOR_EXPLOSION_DELAY_TICKS.get();
        // 液体管道
        ModConfig.fluidPipeRate = AkaishiConfig.FLUID_PIPE_RATE.get();
        // 废品口
        ModConfig.wastePortBufferCapacity = AkaishiConfig.WASTE_PORT_BUFFER_CAPACITY.get();
        // 保存桶
        ModConfig.exhaustedBarrelCapacity = AkaishiConfig.EXHAUSTED_BARREL_CAPACITY.get();
        // 生命活化器
        ModConfig.lifeActivatorLifeCapacity = AkaishiConfig.LIFE_ACTIVATOR_LIFE_CAPACITY.get();
        ModConfig.lifeActivatorCostPerMb = AkaishiConfig.LIFE_ACTIVATOR_COST_PER_MB.get();
        ModConfig.lifeActivatorInputCapacity = AkaishiConfig.LIFE_ACTIVATOR_INPUT_CAPACITY.get();
        ModConfig.lifeActivatorOutputCapacity = AkaishiConfig.LIFE_ACTIVATOR_OUTPUT_CAPACITY.get();
        ModConfig.lifeActivatorConvertRate = AkaishiConfig.LIFE_ACTIVATOR_CONVERT_RATE.get();
        // 生命离心机
        ModConfig.lifeCentrifugeEnergyCapacity = AkaishiConfig.LIFE_CENTRIFUGE_ENERGY_CAPACITY.get();
        ModConfig.lifeCentrifugeInputCapacity = AkaishiConfig.LIFE_CENTRIFUGE_INPUT_CAPACITY.get();
        ModConfig.lifeCentrifugeConvertRate = AkaishiConfig.LIFE_CENTRIFUGE_CONVERT_RATE.get();
        ModConfig.lifeCentrifugeCostPerMb = AkaishiConfig.LIFE_CENTRIFUGE_COST_PER_MB.get();
        // 物品重构仪
        ModConfig.reconstructorEnergyCapacity = AkaishiConfig.RECONSTRUCTOR_ENERGY_CAPACITY.get();
        ModConfig.reconstructorCostPerCrystal = AkaishiConfig.RECONSTRUCTOR_COST_PER_CRYSTAL.get();
        // 聚变燃料聚合器
        ModConfig.aggregatorEnergyCapacity = AkaishiConfig.AGGREGATOR_ENERGY_CAPACITY.get();
        ModConfig.aggregatorCostPerCraft = AkaishiConfig.AGGREGATOR_COST_PER_CRAFT.get();
        ModConfig.aggregatorProcessTicks = AkaishiConfig.AGGREGATOR_PROCESS_TICKS.get();
        ModConfig.aggregatorPlasmaCapacity = AkaishiConfig.AGGREGATOR_PLASMA_CAPACITY.get();
        ModConfig.aggregatorProducePerCraft = AkaishiConfig.AGGREGATOR_PRODUCE_PER_CRAFT.get();
        // 离子体填装器
        ModConfig.fillerPlasmaCapacity = AkaishiConfig.FILLER_PLASMA_CAPACITY.get();
        ModConfig.fillerPlasmaPerRod = AkaishiConfig.FILLER_PLASMA_PER_ROD.get();
        ModConfig.fillerProcessTicks = AkaishiConfig.FILLER_PROCESS_TICKS.get();

        ModConfig.plantCultivatorEnergyCapacity = AkaishiConfig.PLANT_CULTIVATOR_ENERGY_CAPACITY.get();
        ModConfig.plantCultivatorTicks = AkaishiConfig.PLANT_CULTIVATOR_TICKS.get();
        ModConfig.plantCultivatorCostPerTick = AkaishiConfig.PLANT_CULTIVATOR_COST_PER_TICK.get();
        ModConfig.compressorEnergyCapacity = AkaishiConfig.COMPRESSOR_ENERGY_CAPACITY.get();
        ModConfig.compressorTicks = AkaishiConfig.COMPRESSOR_TICKS.get();
        ModConfig.compressorCostPerTick = AkaishiConfig.COMPRESSOR_COST_PER_TICK.get();
        ModConfig.pulverizerEnergyCapacity = AkaishiConfig.PULVERIZER_ENERGY_CAPACITY.get();
        ModConfig.pulverizerTicks = AkaishiConfig.PULVERIZER_TICKS.get();
        ModConfig.pulverizerCostPerTick = AkaishiConfig.PULVERIZER_COST_PER_TICK.get();
        ModConfig.transformerEnergyCapacity = AkaishiConfig.TRANSFORMER_ENERGY_CAPACITY.get();
        ModConfig.transformerTicks = AkaishiConfig.TRANSFORMER_TICKS.get();
        ModConfig.transformerCostPerTick = AkaishiConfig.TRANSFORMER_COST_PER_TICK.get();
        ModConfig.minerTicksBase = AkaishiConfig.MINER_TICKS_BASE.get();
        ModConfig.minerCostPerTickBase = AkaishiConfig.MINER_COST_PER_TICK_BASE.get();
        ModConfig.minerPreciseFortuneDivisor = AkaishiConfig.MINER_PRECISE_FORTUNE_DIVISOR.get();
        ModConfig.minerExtraOreWeight = AkaishiConfig.MINER_EXTRA_ORE_WEIGHT.get();

        ModConfig.decayZoneDurationTicks = AkaishiConfig.DECAY_ZONE_DURATION_TICKS.get();
        ModConfig.decayZoneEnvSamplesPerTick = AkaishiConfig.DECAY_ZONE_SAMPLES_PER_TICK.get();
        // 衰变净化塔
        ModConfig.decayPurifierEnergyCapacity = AkaishiConfig.DECAY_PURIFIER_ENERGY_CAPACITY.get();
        ModConfig.decayPurifierRange = AkaishiConfig.DECAY_PURIFIER_RANGE.get();
        ModConfig.decayPurifierCostPerTick = AkaishiConfig.DECAY_PURIFIER_COST_PER_TICK.get();
        ModConfig.decayPurifierTicksPerTick = AkaishiConfig.DECAY_PURIFIER_TICKS_PER_TICK.get();
        // 聚变堆
        ModConfig.fusionEfficiencyGrowth = AkaishiConfig.FUSION_EFFICIENCY_GROWTH.get();
        ModConfig.fusionCoolerFrameBonus = AkaishiConfig.FUSION_COOLER_FRAME_BONUS.get();
        ModConfig.fusionCoolingPerPercent = AkaishiConfig.FUSION_COOLING_PER_PERCENT.get();
        ModConfig.fusionBaseTemp = AkaishiConfig.FUSION_BASE_TEMP.get();
        ModConfig.fusionTempMax = AkaishiConfig.FUSION_TEMP_MAX.get();
        ModConfig.fusionTempTrip = AkaishiConfig.FUSION_TEMP_TRIP.get();
        ModConfig.fusionTempOptMin = AkaishiConfig.FUSION_TEMP_OPT_MIN.get();
        ModConfig.fusionTempOptMax = AkaishiConfig.FUSION_TEMP_OPT_MAX.get();
        ModConfig.fusionTempResume = AkaishiConfig.FUSION_TEMP_RESUME.get();
        ModConfig.fusionTempStep = AkaishiConfig.FUSION_TEMP_STEP.get();
        ModConfig.fusionCoolerDurabilityInterval = AkaishiConfig.FUSION_COOLER_DURABILITY_INTERVAL.get();
        ModConfig.fusionAshPerEnergy = AkaishiConfig.FUSION_ASH_PER_ENERGY.get();
        ModConfig.fusionRodEnergy = AkaishiConfig.FUSION_ROD_ENERGY.get();
        // 无线赤能源
        ModConfig.wirelessBaseLoss = AkaishiConfig.WIRELESS_BASE_LOSS.get();
        ModConfig.wirelessLossPerBlock = AkaishiConfig.WIRELESS_LOSS_PER_BLOCK.get();
        ModConfig.wirelessMaxLoss = AkaishiConfig.WIRELESS_MAX_LOSS.get();
        ModConfig.wirelessCrossDimLoss = AkaishiConfig.WIRELESS_CROSS_DIM_LOSS.get();
        ModConfig.wirelessLossReductionPerModule = AkaishiConfig.WIRELESS_LOSS_REDUCTION_PER_MODULE.get();

        // 器官·品质曲线（列表 → 数组整体发布，volatile 引用保证读取端原子可见）
        ModConfig.organTierMultiplier = toDoubleArray(AkaishiConfig.ORGAN_TIER_MULTIPLIER.get());
        ModConfig.organTierBaseRejection = toIntArray(AkaishiConfig.ORGAN_TIER_BASE_REJECTION.get());
        ModConfig.organTierGrowthInterval = toIntArray(AkaishiConfig.ORGAN_TIER_GROWTH_INTERVAL.get());
        // 基因来源组排斥系数
        ModConfig.groupRejectionFactor = toDoubleArray(AkaishiConfig.GROUP_REJECTION_FACTOR.get());
        // 纯度联动
        ModConfig.purityRejectionCap = AkaishiConfig.PURITY_REJECTION_CAP.get();
        ModConfig.purityCompatWeight = AkaishiConfig.PURITY_COMPAT_WEIGHT.get();
        // 排斥标尺与阈值
        ModConfig.maxRejection = AkaishiConfig.MAX_REJECTION.get();
        ModConfig.rejectionWarning = AkaishiConfig.REJECTION_WARNING.get();
        ModConfig.rejectionPoison = AkaishiConfig.REJECTION_POISON.get();
        ModConfig.compatSevereThreshold = AkaishiConfig.COMPAT_SEVERE_THRESHOLD.get();
        ModConfig.slotDebuffCleanThreshold = AkaishiConfig.SLOT_DEBUFF_CLEAN_THRESHOLD.get();
        ModConfig.slotDebuffSevereThreshold = AkaishiConfig.SLOT_DEBUFF_SEVERE_THRESHOLD.get();
        ModConfig.growthIntervalMinTicks = AkaishiConfig.GROWTH_INTERVAL_MIN_TICKS.get();
        ModConfig.conflictPunishIntervalTicks = AkaishiConfig.CONFLICT_PUNISH_INTERVAL_TICKS.get();
        ModConfig.conflictPunishDamage = AkaishiConfig.CONFLICT_PUNISH_DAMAGE.get();
        ModConfig.overloadLight = AkaishiConfig.OVERLOAD_LIGHT.get();
        ModConfig.overloadHeavy = AkaishiConfig.OVERLOAD_HEAVY.get();
        // 血清
        ModConfig.serumWashReduce = AkaishiConfig.SERUM_WASH_REDUCE.get();
        ModConfig.serumWashLimit = AkaishiConfig.SERUM_WASH_LIMIT.get();
        ModConfig.serumCooldownTicks = AkaishiConfig.SERUM_COOLDOWN_TICKS.get();
        // 突变词条
        ModConfig.traitBenignRatio = AkaishiConfig.TRAIT_BENIGN_RATIO.get();
        ModConfig.traitRarityHighThreshold = AkaishiConfig.TRAIT_RARITY_HIGH_THRESHOLD.get();
        ModConfig.traitRarityMidThreshold = AkaishiConfig.TRAIT_RARITY_MID_THRESHOLD.get();
        // 培养机品质升级
        ModConfig.cultivatorUpgradeSuccess = toIntArray(AkaishiConfig.CULTIVATOR_UPGRADE_SUCCESS.get());
        ModConfig.cultivatorUpgradeEnergy = toIntArray(AkaishiConfig.CULTIVATOR_UPGRADE_ENERGY.get());
        ModConfig.cultivatorUpgradeSolid = toIntArray(AkaishiConfig.CULTIVATOR_UPGRADE_SOLID.get());
        ModConfig.cultivatorUpgradeTicks = toIntArray(AkaishiConfig.CULTIVATOR_UPGRADE_TICKS.get());
        ModConfig.cultivatorUpgradeCompatBonus = AkaishiConfig.CULTIVATOR_UPGRADE_COMPAT_BONUS.get();
        // 机器全局倍率
        ModConfig.machineWorkSpeed = AkaishiConfig.MACHINE_WORK_SPEED.get();
        ModConfig.machineCostMultiplier = AkaishiConfig.MACHINE_COST_MULTIPLIER.get();
        // 机制开关
        ModConfig.decayZoneEnabled = AkaishiConfig.DECAY_ZONE_ENABLED.get();
        ModConfig.sunlightBurnEnabled = AkaishiConfig.SUNLIGHT_BURN_ENABLED.get();
        ModConfig.overloadEnabled = AkaishiConfig.OVERLOAD_ENABLED.get();
        // 赤石饰品扩展槽
        ModConfig.curioSlotUnlockRequired = AkaishiConfig.CURIO_SLOT_UNLOCK_REQUIRED.get();
        ModConfig.curioSlotUnlockThresholds = toIntArray(AkaishiConfig.CURIO_SLOT_UNLOCK_THRESHOLDS.get());

        // 禁断四件 · 1 生命之触
        ModConfig.lifeTouchEnabled = AkaishiConfig.LIFE_TOUCH_ENABLED.get();
        ModConfig.lifeTouchReachBonus = AkaishiConfig.LIFE_TOUCH_REACH_BONUS.get();
        ModConfig.lifeTouchDoubleStrikeChance = AkaishiConfig.LIFE_TOUCH_DOUBLE_STRIKE_CHANCE.get();
        ModConfig.lifeTouchSelfHurtChance = AkaishiConfig.LIFE_TOUCH_SELF_HURT_CHANCE.get();
        ModConfig.lifeTouchSelfHurtDamage = AkaishiConfig.LIFE_TOUCH_SELF_HURT_DAMAGE.get();
        ModConfig.lifeTouchHungerCostChance = AkaishiConfig.LIFE_TOUCH_HUNGER_COST_CHANCE.get();
        ModConfig.lifeTouchHungerCostAmount = AkaishiConfig.LIFE_TOUCH_HUNGER_COST_AMOUNT.get();
        ModConfig.lifeTouchHungerRestoreChance = AkaishiConfig.LIFE_TOUCH_HUNGER_RESTORE_CHANCE.get();
        ModConfig.lifeTouchHungerRestoreAmount = AkaishiConfig.LIFE_TOUCH_HUNGER_RESTORE_AMOUNT.get();
        ModConfig.lifeTouchHitCacheTicks = AkaishiConfig.LIFE_TOUCH_HIT_CACHE_TICKS.get();
        // 禁断四件 · 2 幼崽之心
        ModConfig.cubHeartEnabled = AkaishiConfig.CUB_HEART_ENABLED.get();
        ModConfig.cubHeartAbsorptionRatio = AkaishiConfig.CUB_HEART_ABSORPTION_RATIO.get();
        ModConfig.cubHeartEffectInterval = AkaishiConfig.CUB_HEART_EFFECT_INTERVAL.get();
        ModConfig.cubHeartSlowHasteChance = AkaishiConfig.CUB_HEART_SLOW_HASTE_CHANCE.get();
        ModConfig.cubHeartSlowHasteAmplitude = AkaishiConfig.CUB_HEART_SLOW_HASTE_AMPLITUDE.get();
        ModConfig.cubHeartSlowHasteTicks = AkaishiConfig.CUB_HEART_SLOW_HASTE_TICKS.get();
        ModConfig.cubHeartDamageShiftChance = AkaishiConfig.CUB_HEART_DAMAGE_SHIFT_CHANCE.get();
        ModConfig.cubHeartDamageShiftAmount = AkaishiConfig.CUB_HEART_DAMAGE_SHIFT_AMOUNT.get();
        ModConfig.cubHeartDamageShiftTicks = AkaishiConfig.CUB_HEART_DAMAGE_SHIFT_TICKS.get();
        ModConfig.cubHeartUnnameableChance = AkaishiConfig.CUB_HEART_UNNAMEABLE_CHANCE.get();
        ModConfig.cubHeartUnnameableAmplifier = AkaishiConfig.CUB_HEART_UNNAMEABLE_AMPLIFIER.get();
        ModConfig.cubHeartUnnameableTicks = AkaishiConfig.CUB_HEART_UNNAMEABLE_TICKS.get();
        ModConfig.cubHeartExcitementAttackSpeed = AkaishiConfig.CUB_HEART_EXCITEMENT_ATTACK_SPEED.get();
        ModConfig.cubHeartExcitementTicks = AkaishiConfig.CUB_HEART_EXCITEMENT_TICKS.get();
        // 禁断四件 · 3 母神之印
        ModConfig.motherSealEnabled = AkaishiConfig.MOTHER_SEAL_ENABLED.get();
        ModConfig.motherSealDamageBonus = AkaishiConfig.MOTHER_SEAL_DAMAGE_BONUS.get();
        ModConfig.motherSealHealthBonus = AkaishiConfig.MOTHER_SEAL_HEALTH_BONUS.get();
        ModConfig.motherSealSpeedBonus = AkaishiConfig.MOTHER_SEAL_SPEED_BONUS.get();
        ModConfig.motherSealAttackSpeedBonus = AkaishiConfig.MOTHER_SEAL_ATTACK_SPEED_BONUS.get();
        ModConfig.motherSealSelfUnnameablePeriod = AkaishiConfig.MOTHER_SEAL_SELF_UNNAMEABLE_PERIOD.get();
        ModConfig.motherSealSelfUnnameableTicks = AkaishiConfig.MOTHER_SEAL_SELF_UNNAMEABLE_TICKS.get();
        ModConfig.motherSealSelfUnnameableAmplifier = AkaishiConfig.MOTHER_SEAL_SELF_UNNAMEABLE_AMPLIFIER.get();
        ModConfig.motherSealVegetarianHungerCost = AkaishiConfig.MOTHER_SEAL_VEGETARIAN_HUNGER_COST.get();
        ModConfig.motherSealVegetarianNauseaTicks = AkaishiConfig.MOTHER_SEAL_VEGETARIAN_NAUSEA_TICKS.get();
        ModConfig.motherSealResistFactor = AkaishiConfig.MOTHER_SEAL_RESIST_FACTOR.get();
        // 禁断四件 · 4 孕育之环
        ModConfig.fertilityRingEnabled = AkaishiConfig.FERTILITY_RING_ENABLED.get();
        ModConfig.fertilityRingHealChance = AkaishiConfig.FERTILITY_RING_HEAL_CHANCE.get();
        ModConfig.fertilityRingHealAmount = AkaishiConfig.FERTILITY_RING_HEAL_AMOUNT.get();
        ModConfig.fertilityRingHealHungerCost = AkaishiConfig.FERTILITY_RING_HEAL_HUNGER_COST.get();
        ModConfig.fertilityRingAttackSpeedBonus = AkaishiConfig.FERTILITY_RING_ATTACK_SPEED_BONUS.get();
        ModConfig.fertilityRingAttackSpeedTicks = AkaishiConfig.FERTILITY_RING_ATTACK_SPEED_TICKS.get();
        ModConfig.fertilityRingMeatHealAmount = AkaishiConfig.FERTILITY_RING_MEAT_HEAL_AMOUNT.get();
        ModConfig.fertilityRingSatiatedMeatOnly = AkaishiConfig.FERTILITY_RING_SATIATED_MEAT_ONLY.get();
        ModConfig.fertilityRingStarveMultiplier = AkaishiConfig.FERTILITY_RING_STARVE_MULTIPLIER.get();
        ModConfig.fertilityRingStarveLethal = AkaishiConfig.FERTILITY_RING_STARVE_LETHAL.get();
        // 禁断四件 · 5 套装
        ModConfig.setAttackCountRequired = AkaishiConfig.SET_ATTACK_COUNT_REQUIRED.get();
        ModConfig.setAttackCountHungerRestore = AkaishiConfig.SET_ATTACK_COUNT_HUNGER_RESTORE.get();
        ModConfig.setUnnameableLevelBonus = AkaishiConfig.SET_UNNAMEABLE_LEVEL_BONUS.get();
        ModConfig.setSuppressDistortion = AkaishiConfig.SET_SUPPRESS_DISTORTION.get();
        ModConfig.setDamageReduction = AkaishiConfig.SET_DAMAGE_REDUCTION.get();
        ModConfig.setUnnameableCritChance = AkaishiConfig.SET_UNNAMEABLE_CRIT_CHANCE.get();
        ModConfig.setUnnameableCritDamage = AkaishiConfig.SET_UNNAMEABLE_CRIT_DAMAGE.get();
        ModConfig.setNearDeathHealPercent = AkaishiConfig.SET_NEAR_DEATH_HEAL_PERCENT.get();
        ModConfig.setNearDeathUnnameableTicks = AkaishiConfig.SET_NEAR_DEATH_UNNAMEABLE_TICKS.get();
        ModConfig.setNearDeathCooldownTicks = AkaishiConfig.SET_NEAR_DEATH_COOLDOWN_TICKS.get();
        // 禁断四件 · 6 侵蚀
        ModConfig.erosionEnabled = AkaishiConfig.EROSION_ENABLED.get();
        ModConfig.erosionDurationMinutes = AkaishiConfig.EROSION_DURATION_MINUTES.get();
        ModConfig.erosionNoticeThresholds = toIntArray(AkaishiConfig.EROSION_NOTICE_THRESHOLDS.get());
        ModConfig.erosionNoticeIntervalMinutes = AkaishiConfig.EROSION_NOTICE_INTERVAL_MINUTES.get();
        ModConfig.erosionStatRerollPercent = AkaishiConfig.EROSION_STAT_REROLL_PERCENT.get();
        ModConfig.erosionNbtFlushTicks = AkaishiConfig.EROSION_NBT_FLUSH_TICKS.get();
        ModConfig.erosionScreenFlashEnabled = AkaishiConfig.EROSION_SCREEN_FLASH_ENABLED.get();
        // 禁断四件 · 7 仪式与吸取
        ModConfig.altarDrainEnabled = AkaishiConfig.ALTAR_DRAIN_ENABLED.get();
        ModConfig.altarDrainRadius = AkaishiConfig.ALTAR_DRAIN_RADIUS.get();
        ModConfig.altarDrainIntervalTicks = AkaishiConfig.ALTAR_DRAIN_INTERVAL_TICKS.get();
        ModConfig.altarDrainHealthPercent = AkaishiConfig.ALTAR_DRAIN_HEALTH_PERCENT.get();
        ModConfig.altarDrainEnergyPerHp = AkaishiConfig.ALTAR_DRAIN_ENERGY_PER_HP.get();
        ModConfig.altarDrainMaxTargets = AkaishiConfig.ALTAR_DRAIN_MAX_TARGETS.get();
        ModConfig.altarDrainMaxEnergyPerTarget = AkaishiConfig.ALTAR_DRAIN_MAX_ENERGY_PER_TARGET.get();
        ModConfig.altarDrainExemptCreative = AkaishiConfig.ALTAR_DRAIN_EXEMPT_CREATIVE.get();
        ModConfig.altarNewRecipeTierRequired = AkaishiConfig.ALTAR_NEW_RECIPE_TIER_REQUIRED.get();
        ModConfig.altarNewRecipeProgressMax = AkaishiConfig.ALTAR_NEW_RECIPE_PROGRESS_MAX.get();
        ModConfig.altarLegacyProgressMax = AkaishiConfig.ALTAR_LEGACY_PROGRESS_MAX.get();

        // 生命研究机器
        ModConfig.geneAnalyzerLifeCost = AkaishiConfig.GENE_ANALYZER_LIFE_COST.get();
        ModConfig.geneAnalyzerLifeCapacity = AkaishiConfig.GENE_ANALYZER_LIFE_CAPACITY.get();
        ModConfig.geneAnalyzerProcessTicks = AkaishiConfig.GENE_ANALYZER_PROCESS_TICKS.get();
        ModConfig.geneAnalyzerMinSuccessRate = AkaishiConfig.GENE_ANALYZER_MIN_SUCCESS.get();
        ModConfig.geneAnalyzerMaxSuccessRate = AkaishiConfig.GENE_ANALYZER_MAX_SUCCESS.get();
        ModConfig.lifeStructLifeCost = AkaishiConfig.LIFE_STRUCT_LIFE_COST.get();
        ModConfig.lifeStructSolidCost = AkaishiConfig.LIFE_STRUCT_SOLID_COST.get();
        ModConfig.lifeStructLifeCapacity = AkaishiConfig.LIFE_STRUCT_LIFE_CAPACITY.get();
        ModConfig.lifeStructProcessTicks = AkaishiConfig.LIFE_STRUCT_PROCESS_TICKS.get();
        ModConfig.lifeBreederLifeCost = AkaishiConfig.LIFE_BREEDER_LIFE_COST.get();
        ModConfig.lifeBreederCrystalCost = AkaishiConfig.LIFE_BREEDER_CRYSTAL_COST.get();
        ModConfig.lifeBreederLifeCapacity = AkaishiConfig.LIFE_BREEDER_LIFE_CAPACITY.get();
        ModConfig.lifeBreederProcessTicks = AkaishiConfig.LIFE_BREEDER_PROCESS_TICKS.get();
        ModConfig.lifeBreederMinSuccessRate = AkaishiConfig.LIFE_BREEDER_MIN_SUCCESS.get();
        ModConfig.lifeBreederMaxSuccessRate = AkaishiConfig.LIFE_BREEDER_MAX_SUCCESS.get();
        ModConfig.traitReforgerLifeCost = AkaishiConfig.TRAIT_REFORGER_LIFE_COST.get();
        ModConfig.traitReforgerLifeCapacity = AkaishiConfig.TRAIT_REFORGER_LIFE_CAPACITY.get();
        ModConfig.traitReforgerProcessTicks = AkaishiConfig.TRAIT_REFORGER_PROCESS_TICKS.get();
        ModConfig.traitReforgerCrystalPerRarity = AkaishiConfig.TRAIT_REFORGER_CRYSTAL_PER_RARITY.get();
        ModConfig.transgeneFactoryLifeCost = AkaishiConfig.TRANSGENE_FACTORY_LIFE_COST.get();
        ModConfig.transgeneFactoryLifeCapacity = AkaishiConfig.TRANSGENE_FACTORY_LIFE_CAPACITY.get();
        ModConfig.transgeneFactoryProcessTicks = AkaishiConfig.TRANSGENE_FACTORY_PROCESS_TICKS.get();
        ModConfig.surgeryImplantSolidCost = AkaishiConfig.SURGERY_IMPLANT_SOLID_COST.get();
        ModConfig.surgeryImplantLifeCost = AkaishiConfig.SURGERY_IMPLANT_LIFE_COST.get();
        ModConfig.surgeryExtractSolidCost = AkaishiConfig.SURGERY_EXTRACT_SOLID_COST.get();
        ModConfig.surgeryExtractLifeCost = AkaishiConfig.SURGERY_EXTRACT_LIFE_COST.get();
        ModConfig.surgeryLifeCapacity = AkaishiConfig.SURGERY_LIFE_CAPACITY.get();
        ModConfig.surgeryProcessTicks = AkaishiConfig.SURGERY_PROCESS_TICKS.get();
        ModConfig.organVaultLifeCapacity = AkaishiConfig.ORGAN_VAULT_LIFE_CAPACITY.get();
        ModConfig.organVaultKeepCostPerTick = AkaishiConfig.ORGAN_VAULT_KEEP_COST.get();
        ModConfig.potionTableLifeCapacity = AkaishiConfig.POTION_TABLE_LIFE_CAPACITY.get();
        // 能量机器
        ModConfig.energyProcessorChishiRate = AkaishiConfig.ENERGY_PROCESSOR_CHISHI_RATE.get();
        ModConfig.energyProcessorChishiCapacity = AkaishiConfig.ENERGY_PROCESSOR_CHISHI_CAPACITY.get();
        ModConfig.energyProcessorTankCapacity = AkaishiConfig.ENERGY_PROCESSOR_TANK_CAPACITY.get();
        ModConfig.energyProcessorChishiCost = AkaishiConfig.ENERGY_PROCESSOR_CHISHI_COST.get();
        ModConfig.energyLiquefierChishiRate = AkaishiConfig.ENERGY_LIQUEFIER_CHISHI_RATE.get();
        ModConfig.energyLiquefierChishiCapacity = AkaishiConfig.ENERGY_LIQUEFIER_CHISHI_CAPACITY.get();
        ModConfig.energyLiquefierTankCapacity = AkaishiConfig.ENERGY_LIQUEFIER_TANK_CAPACITY.get();
        ModConfig.fuelMixerChishiRate = AkaishiConfig.FUEL_MIXER_CHISHI_RATE.get();
        ModConfig.fuelMixerChishiCapacity = AkaishiConfig.FUEL_MIXER_CHISHI_CAPACITY.get();
        ModConfig.fuelMixerChishiCost = AkaishiConfig.FUEL_MIXER_CHISHI_COST.get();
        ModConfig.fuelMixerTankCapacity = AkaishiConfig.FUEL_MIXER_TANK_CAPACITY.get();
        ModConfig.fuelCannerTankCapacity = AkaishiConfig.FUEL_CANNER_TANK_CAPACITY.get();
        ModConfig.fuelCannerFillRate = AkaishiConfig.FUEL_CANNER_FILL_RATE.get();
        ModConfig.energyAggregatorEnergyPerIngot = AkaishiConfig.ENERGY_AGGREGATOR_PER_INGOT.get();
        ModConfig.energyAggregatorEnergyPerGeodeUpgrade = AkaishiConfig.ENERGY_AGGREGATOR_PER_GEODE.get();
        ModConfig.energyAggregatorEnergyCapacity = AkaishiConfig.ENERGY_AGGREGATOR_CAPACITY.get();
        ModConfig.energyGeneratorGenerateRate = AkaishiConfig.ENERGY_GENERATOR_RATE.get();
        ModConfig.energyAssemblyGenerateRate = AkaishiConfig.ENERGY_ASSEMBLY_RATE.get();
        ModConfig.superGeneratorCoreGenerateRate = AkaishiConfig.SUPER_GENERATOR_CORE_RATE.get();
        ModConfig.energyCellSerializerBaseCapacity = AkaishiConfig.ENERGY_CELL_BASE_CAPACITY.get();
        ModConfig.upgradeStationEnergyPerUpgrade = AkaishiConfig.UPGRADE_STATION_PER_UPGRADE.get();
        ModConfig.upgradeStationEnergyCapacity = AkaishiConfig.UPGRADE_STATION_CAPACITY.get();
        ModConfig.equipmentForgerEnergyPerForge = AkaishiConfig.EQUIPMENT_FORGER_PER_FORGE.get();
        ModConfig.equipmentForgerEnergyCapacity = AkaishiConfig.EQUIPMENT_FORGER_CAPACITY.get();
        // 净化与矩阵
        ModConfig.purifierEnergyPerTick = AkaishiConfig.PURIFIER_ENERGY_PER_TICK.get();
        ModConfig.purifierBurnRate = AkaishiConfig.PURIFIER_BURN_RATE.get();
        ModConfig.purifierTotalCost = AkaishiConfig.PURIFIER_TOTAL_COST.get();
        ModConfig.purifierRateFormed = AkaishiConfig.PURIFIER_RATE_FORMED.get();
        ModConfig.purifierMatrixTotalCost = AkaishiConfig.PURIFIER_MATRIX_TOTAL_COST.get();
        ModConfig.purifierMatrixRateFormed = AkaishiConfig.PURIFIER_MATRIX_RATE_FORMED.get();
        ModConfig.lifePurifierChishiRate = AkaishiConfig.LIFE_PURIFIER_CHISHI_RATE.get();
        ModConfig.lifePurifierTotalCost = AkaishiConfig.LIFE_PURIFIER_TOTAL_COST.get();
        ModConfig.lifePurifierLifeCost = AkaishiConfig.LIFE_PURIFIER_LIFE_COST.get();
        ModConfig.lifePurifierChishiCapacity = AkaishiConfig.LIFE_PURIFIER_CHISHI_CAPACITY.get();
        ModConfig.lifePurifierLifeCapacity = AkaishiConfig.LIFE_PURIFIER_LIFE_CAPACITY.get();
        ModConfig.lifeMatrixConversionsPerTick = AkaishiConfig.LIFE_MATRIX_CONVERSIONS_PER_TICK.get();
        ModConfig.lifeMatrixConversionCost = AkaishiConfig.LIFE_MATRIX_CONVERSION_COST.get();
        ModConfig.lifeMatrixChishiCapacity = AkaishiConfig.LIFE_MATRIX_CHISHI_CAPACITY.get();
        ModConfig.lifeMatrixLifeCapacity = AkaishiConfig.LIFE_MATRIX_LIFE_CAPACITY.get();
        ModConfig.lifeConversionConversionsPerTick = AkaishiConfig.LIFE_CONVERSION_PER_TICK.get();
        ModConfig.lifeConversionChishiCapacity = AkaishiConfig.LIFE_CONVERSION_CHISHI_CAPACITY.get();
        ModConfig.lifeConversionLifeCapacity = AkaishiConfig.LIFE_CONVERSION_LIFE_CAPACITY.get();
        ModConfig.lifeAggregationConversionCost = AkaishiConfig.LIFE_AGGREGATION_COST.get();
        ModConfig.lifeAggregationConversionOutput = AkaishiConfig.LIFE_AGGREGATION_OUTPUT.get();
        ModConfig.lifeAggregationChishiCapacity = AkaishiConfig.LIFE_AGGREGATION_CHISHI_CAPACITY.get();
        ModConfig.lifeAggregationLifeCapacity = AkaishiConfig.LIFE_AGGREGATION_LIFE_CAPACITY.get();
        // 机械改造机器
        ModConfig.mechanicalChishiCapacity = AkaishiConfig.MECH_CHISHI_CAPACITY.get();
        ModConfig.mechanicalLifeCapacity = AkaishiConfig.MECH_LIFE_CAPACITY.get();
        ModConfig.mechTemplateChishiCost = AkaishiConfig.MECH_TEMPLATE_CHISHI_COST.get();
        ModConfig.mechTemplateLifeCost = AkaishiConfig.MECH_TEMPLATE_LIFE_COST.get();
        ModConfig.mechTemplateTicks = AkaishiConfig.MECH_TEMPLATE_TICKS.get();
        ModConfig.mechProcessChishiBase = toLongArray(AkaishiConfig.MECH_PROCESS_CHISHI_BASE.get());
        ModConfig.mechProcessLifeBase = toLongArray(AkaishiConfig.MECH_PROCESS_LIFE_BASE.get());
        ModConfig.mechProcessTicksBase = toIntArray(AkaishiConfig.MECH_PROCESS_TICKS_BASE.get());
        ModConfig.mechProcessPartFactor = toIntArray(AkaishiConfig.MECH_PROCESS_PART_FACTOR.get());
        ModConfig.mechProcessMaterialCount = toIntArray(AkaishiConfig.MECH_PROCESS_MATERIAL_COUNT.get());
        ModConfig.mechAssemblyChishiCost = AkaishiConfig.MECH_ASSEMBLY_CHISHI_COST.get();
        ModConfig.mechAssemblyLifeCost = AkaishiConfig.MECH_ASSEMBLY_LIFE_COST.get();
        ModConfig.mechAssemblyTicks = AkaishiConfig.MECH_ASSEMBLY_TICKS.get();
        // 机械义体属性换算
        ModConfig.mechBodyHealthScale = AkaishiConfig.MECH_BODY_HEALTH_SCALE.get();
        ModConfig.mechBodyAttackScale = AkaishiConfig.MECH_BODY_ATTACK_SCALE.get();
        ModConfig.mechBodyAttackSpeedScale = AkaishiConfig.MECH_BODY_ATTACK_SPEED_SCALE.get();
        ModConfig.mechBodyMovementSpeedScale = AkaishiConfig.MECH_BODY_MOVEMENT_SPEED_SCALE.get();
        ModConfig.mechBodyArmorScale = AkaishiConfig.MECH_BODY_ARMOR_SCALE.get();
        ModConfig.mechBodyCritChanceScale = AkaishiConfig.MECH_BODY_CRIT_CHANCE_SCALE.get();
        ModConfig.mechBodyCritDamageScale = AkaishiConfig.MECH_BODY_CRIT_DAMAGE_SCALE.get();
        ModConfig.mechBodyRangeScale = AkaishiConfig.MECH_BODY_RANGE_SCALE.get();
        ModConfig.mechBodyDodgeScale = AkaishiConfig.MECH_BODY_DODGE_SCALE.get();
        // 基因属性权重
        ModConfig.geneWeightStrength = AkaishiConfig.GENE_WEIGHT_STRENGTH.get();
        // 底层战斗（暴击/闪避）
        ModConfig.combatCritEnabled = AkaishiConfig.COMBAT_CRIT_ENABLED.get();
        ModConfig.combatDodgeEnabled = AkaishiConfig.COMBAT_DODGE_ENABLED.get();
        ModConfig.combatCritChanceCap = AkaishiConfig.COMBAT_CRIT_CHANCE_CAP.get();
        ModConfig.combatCritDamageCap = AkaishiConfig.COMBAT_CRIT_DAMAGE_CAP.get();
        ModConfig.combatDodgeChanceCap = AkaishiConfig.COMBAT_DODGE_CHANCE_CAP.get();
        // 端口与电池缓冲
        ModConfig.lifeMatrixInputPortBufferCapacity = AkaishiConfig.LIFE_MATRIX_INPUT_PORT_BUFFER.get();
        ModConfig.lifeMatrixOutputPortBufferCapacity = AkaishiConfig.LIFE_MATRIX_OUTPUT_PORT_BUFFER.get();
        ModConfig.purifierEnergyInputPortBufferCapacity = AkaishiConfig.PURIFIER_INPUT_PORT_BUFFER.get();
        ModConfig.minerPortBufferCapacity = AkaishiConfig.MINER_PORT_BUFFER.get();
        ModConfig.minerEnergyInputBufferCapacity = AkaishiConfig.MINER_ENERGY_INPUT_BUFFER.get();
        ModConfig.wirelessInputPortBufferCapacity = AkaishiConfig.WIRELESS_INPUT_PORT_BUFFER.get();
        ModConfig.wirelessOutputPortBufferCapacity = AkaishiConfig.WIRELESS_OUTPUT_PORT_BUFFER.get();
        ModConfig.genEnergyOutputPortBufferCapacity = AkaishiConfig.GEN_ENERGY_OUTPUT_BUFFER.get();
        ModConfig.fusionEnergyOutputBufferCapacity = AkaishiConfig.FUSION_ENERGY_OUTPUT_BUFFER.get();
        ModConfig.reactorEnergyOutputBufferCapacity = AkaishiConfig.REACTOR_ENERGY_OUTPUT_BUFFER.get();
        ModConfig.lifeEnergyCellSerializerBaseCapacity = AkaishiConfig.LIFE_ENERGY_CELL_SERIALIZER_CAPACITY.get();
        ModConfig.plasmaTankCapacity = AkaishiConfig.PLASMA_TANK_CAPACITY.get();
        ModConfig.itemTerminalEnergyBuffer = AkaishiConfig.ITEM_TERMINAL_ENERGY_BUFFER.get();
        ModConfig.itemTerminalEnergyPortBufferCapacity = AkaishiConfig.ITEM_TERMINAL_ENERGY_PORT_BUFFER.get();

        // 培养机提纯与分馏机
        ModConfig.cultivatorLifeCapacity = AkaishiConfig.CULTIVATOR_LIFE_CAPACITY.get();
        ModConfig.cultivatorPurifySuccess = toIntArray(AkaishiConfig.CULTIVATOR_PURIFY_SUCCESS.get());
        ModConfig.cultivatorPurifyEnergy = toLongArray(AkaishiConfig.CULTIVATOR_PURIFY_ENERGY.get());
        ModConfig.cultivatorPurifySolid = toIntArray(AkaishiConfig.CULTIVATOR_PURIFY_SOLID.get());
        ModConfig.cultivatorPurifyTicks = toIntArray(AkaishiConfig.CULTIVATOR_PURIFY_TICKS.get());
        ModConfig.cultivatorPurifyGain = AkaishiConfig.CULTIVATOR_PURIFY_GAIN.get();
        ModConfig.fractionatorEnergyCapacity = AkaishiConfig.FRACTIONATOR_ENERGY_CAPACITY.get();
        ModConfig.fractionatorCostPerCraft = AkaishiConfig.FRACTIONATOR_COST_PER_CRAFT.get();
        ModConfig.fractionatorProcessTicks = AkaishiConfig.FRACTIONATOR_PROCESS_TICKS.get();

        // 价值分（统一存储库定价内核）
        ModConfig.valueCostMultiplier = AkaishiConfig.VALUE_COST_MULTIPLIER.get();
        ModConfig.valueIngredientWeight = AkaishiConfig.VALUE_INGREDIENT_WEIGHT.get();
        ModConfig.valueMagicBonus = AkaishiConfig.VALUE_MAGIC_BONUS.get();
        ModConfig.valueIngredientCap = AkaishiConfig.VALUE_INGREDIENT_CAP.get();
        ModConfig.valueIterations = AkaishiConfig.VALUE_ITERATIONS.get();
        ModConfig.valueLootEnabled = AkaishiConfig.VALUE_LOOT_ENABLED.get();
        ModConfig.valueLootAutoApply = AkaishiConfig.VALUE_LOOT_AUTO_APPLY.get();
        ModConfig.valueLootCap = AkaishiConfig.VALUE_LOOT_CAP.get();
        ModConfig.valueFluidEnabled = AkaishiConfig.VALUE_FLUID_ENABLED.get();
        ModConfig.valueFluidPerMbCap = AkaishiConfig.VALUE_FLUID_PER_MB_CAP.get();
        ModConfig.valueOverrides = toStringArray(AkaishiConfig.VALUE_OVERRIDES.get());
        ModConfig.valueFluidValues = toStringArray(AkaishiConfig.VALUE_FLUID_VALUES.get());
        ModConfig.valueTagValues = toStringArray(AkaishiConfig.VALUE_TAG_VALUES.get());
        ModConfig.valueTierBonus = toStringArray(AkaishiConfig.VALUE_TIER_BONUS.get());
        ModConfig.valueKeywordExclusions = toStringArray(AkaishiConfig.VALUE_KEYWORD_EXCLUSIONS.get());
        ModConfig.valueLootBlacklist = toStringArray(AkaishiConfig.VALUE_LOOT_BLACKLIST.get());
        ModConfig.unifiedVaultRows = AkaishiConfig.UNIFIED_VAULT_ROWS.get();

        // 估值参数可能已变：作废快照 + 重建掉落来源索引，保证保存即生效
        ValueReloadHooks.onReload();

        // 配置热重载后把服务端权威值推送给所有在线玩家（登录推送见 AkaishiModForge）
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ConfigSyncS2C.sendToPlayer(player);
            }
        }
    }

    /** 配置 Double 列表 → double[]（空/越界条目由读取方按"0 = 内置默认"回退） */
    private static double[] toDoubleArray(List<?> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = ((Number) list.get(i)).doubleValue();
        }
        return arr;
    }

    /** 配置 Integer 列表 → int[] */
    private static int[] toIntArray(List<?> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = ((Number) list.get(i)).intValue();
        }
        return arr;
    }

    /** 配置 Long 列表 → long[] */
    private static long[] toLongArray(List<?> list) {
        long[] arr = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = ((Number) list.get(i)).longValue();
        }
        return arr;
    }

    /** 配置 String 列表 → String[]（空条目由读取端忽略） */
    private static String[] toStringArray(List<?> list) {
        String[] arr = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Object v = list.get(i);
            arr[i] = v == null ? "" : v.toString();
        }
        return arr;
    }
}
