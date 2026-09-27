package com.example.akaishi.forge.config;

import com.example.akaishi.config.ConfigSyncS2C;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.forge.config.specs.BufferValueSpecs;
import com.example.akaishi.forge.config.specs.CoreMachineSpecs;
import com.example.akaishi.forge.config.specs.CurioSpecs;
import com.example.akaishi.forge.config.specs.DecayFusionSpecs;
import com.example.akaishi.forge.config.specs.LifeMachineSpecs;
import com.example.akaishi.forge.config.specs.MechSpecs;
import com.example.akaishi.forge.config.specs.OrganSpecs;
import com.example.akaishi.forge.config.specs.SanitySpecs;
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
        ModConfig.reactorEnergyPerSlot = CoreMachineSpecs.REACTOR_ENERGY_PER_SLOT.get();
        ModConfig.reactorBaseTemp = CoreMachineSpecs.REACTOR_BASE_TEMP.get();
        ModConfig.reactorPassiveCool = CoreMachineSpecs.REACTOR_PASSIVE_COOL.get();
        ModConfig.reactorCoolerCool = CoreMachineSpecs.REACTOR_COOLER_COOL.get();
        ModConfig.reactorDrainBase = CoreMachineSpecs.REACTOR_DRAIN_BASE.get();
        ModConfig.reactorWasteRatio = CoreMachineSpecs.REACTOR_WASTE_RATIO.get();
        ModConfig.reactorWasteCapacity = CoreMachineSpecs.REACTOR_WASTE_CAPACITY.get();
        ModConfig.reactorTempMax = CoreMachineSpecs.REACTOR_TEMP_MAX.get();
        ModConfig.reactorTempOptMin = CoreMachineSpecs.REACTOR_TEMP_OPT_MIN.get();
        ModConfig.reactorTempOptMax = CoreMachineSpecs.REACTOR_TEMP_OPT_MAX.get();
        ModConfig.reactorTempWarn = CoreMachineSpecs.REACTOR_TEMP_WARN.get();
        ModConfig.reactorExplosionDelayTicks = CoreMachineSpecs.REACTOR_EXPLOSION_DELAY_TICKS.get();
        // 液体管道
        ModConfig.fluidPipeRate = CoreMachineSpecs.FLUID_PIPE_RATE.get();
        // 废品口
        ModConfig.wastePortBufferCapacity = CoreMachineSpecs.WASTE_PORT_BUFFER_CAPACITY.get();
        // 保存桶
        ModConfig.exhaustedBarrelCapacity = CoreMachineSpecs.EXHAUSTED_BARREL_CAPACITY.get();
        // 生命活化器
        ModConfig.lifeActivatorLifeCapacity = CoreMachineSpecs.LIFE_ACTIVATOR_LIFE_CAPACITY.get();
        ModConfig.lifeActivatorCostPerMb = CoreMachineSpecs.LIFE_ACTIVATOR_COST_PER_MB.get();
        ModConfig.lifeActivatorInputCapacity = CoreMachineSpecs.LIFE_ACTIVATOR_INPUT_CAPACITY.get();
        ModConfig.lifeActivatorOutputCapacity = CoreMachineSpecs.LIFE_ACTIVATOR_OUTPUT_CAPACITY.get();
        ModConfig.lifeActivatorConvertRate = CoreMachineSpecs.LIFE_ACTIVATOR_CONVERT_RATE.get();
        // 生命离心机
        ModConfig.lifeCentrifugeEnergyCapacity = CoreMachineSpecs.LIFE_CENTRIFUGE_ENERGY_CAPACITY.get();
        ModConfig.lifeCentrifugeInputCapacity = CoreMachineSpecs.LIFE_CENTRIFUGE_INPUT_CAPACITY.get();
        ModConfig.lifeCentrifugeConvertRate = CoreMachineSpecs.LIFE_CENTRIFUGE_CONVERT_RATE.get();
        ModConfig.lifeCentrifugeCostPerMb = CoreMachineSpecs.LIFE_CENTRIFUGE_COST_PER_MB.get();
        // 物品重构仪
        ModConfig.reconstructorEnergyCapacity = CoreMachineSpecs.RECONSTRUCTOR_ENERGY_CAPACITY.get();
        ModConfig.reconstructorCostPerCrystal = CoreMachineSpecs.RECONSTRUCTOR_COST_PER_CRYSTAL.get();
        // 聚变燃料聚合器
        ModConfig.aggregatorEnergyCapacity = CoreMachineSpecs.AGGREGATOR_ENERGY_CAPACITY.get();
        ModConfig.aggregatorCostPerCraft = CoreMachineSpecs.AGGREGATOR_COST_PER_CRAFT.get();
        ModConfig.aggregatorProcessTicks = CoreMachineSpecs.AGGREGATOR_PROCESS_TICKS.get();
        ModConfig.aggregatorPlasmaCapacity = CoreMachineSpecs.AGGREGATOR_PLASMA_CAPACITY.get();
        ModConfig.aggregatorProducePerCraft = CoreMachineSpecs.AGGREGATOR_PRODUCE_PER_CRAFT.get();
        // 离子体填装器
        ModConfig.fillerPlasmaCapacity = CoreMachineSpecs.FILLER_PLASMA_CAPACITY.get();
        ModConfig.fillerPlasmaPerRod = CoreMachineSpecs.FILLER_PLASMA_PER_ROD.get();
        ModConfig.fillerProcessTicks = CoreMachineSpecs.FILLER_PROCESS_TICKS.get();

        ModConfig.plantCultivatorEnergyCapacity = CoreMachineSpecs.PLANT_CULTIVATOR_ENERGY_CAPACITY.get();
        ModConfig.plantCultivatorTicks = CoreMachineSpecs.PLANT_CULTIVATOR_TICKS.get();
        ModConfig.plantCultivatorCostPerTick = CoreMachineSpecs.PLANT_CULTIVATOR_COST_PER_TICK.get();
        ModConfig.compressorEnergyCapacity = CoreMachineSpecs.COMPRESSOR_ENERGY_CAPACITY.get();
        ModConfig.compressorTicks = CoreMachineSpecs.COMPRESSOR_TICKS.get();
        ModConfig.compressorCostPerTick = CoreMachineSpecs.COMPRESSOR_COST_PER_TICK.get();
        ModConfig.pulverizerEnergyCapacity = CoreMachineSpecs.PULVERIZER_ENERGY_CAPACITY.get();
        ModConfig.pulverizerTicks = CoreMachineSpecs.PULVERIZER_TICKS.get();
        ModConfig.pulverizerCostPerTick = CoreMachineSpecs.PULVERIZER_COST_PER_TICK.get();
        ModConfig.transformerEnergyCapacity = CoreMachineSpecs.TRANSFORMER_ENERGY_CAPACITY.get();
        ModConfig.transformerTicks = CoreMachineSpecs.TRANSFORMER_TICKS.get();
        ModConfig.transformerCostPerTick = CoreMachineSpecs.TRANSFORMER_COST_PER_TICK.get();
        ModConfig.minerTicksBase = CoreMachineSpecs.MINER_TICKS_BASE.get();
        ModConfig.minerCostPerTickBase = CoreMachineSpecs.MINER_COST_PER_TICK_BASE.get();
        ModConfig.minerPreciseFortuneDivisor = CoreMachineSpecs.MINER_PRECISE_FORTUNE_DIVISOR.get();
        ModConfig.minerExtraOreWeight = CoreMachineSpecs.MINER_EXTRA_ORE_WEIGHT.get();

        ModConfig.decayZoneDurationTicks = DecayFusionSpecs.DECAY_ZONE_DURATION_TICKS.get();
        ModConfig.decayZoneEnvSamplesPerTick = DecayFusionSpecs.DECAY_ZONE_SAMPLES_PER_TICK.get();
        // 衰变净化塔
        ModConfig.decayPurifierEnergyCapacity = DecayFusionSpecs.DECAY_PURIFIER_ENERGY_CAPACITY.get();
        ModConfig.decayPurifierRange = DecayFusionSpecs.DECAY_PURIFIER_RANGE.get();
        ModConfig.decayPurifierCostPerTick = DecayFusionSpecs.DECAY_PURIFIER_COST_PER_TICK.get();
        ModConfig.decayPurifierTicksPerTick = DecayFusionSpecs.DECAY_PURIFIER_TICKS_PER_TICK.get();
        // 聚变堆
        ModConfig.fusionEfficiencyGrowth = DecayFusionSpecs.FUSION_EFFICIENCY_GROWTH.get();
        ModConfig.fusionCoolerFrameBonus = DecayFusionSpecs.FUSION_COOLER_FRAME_BONUS.get();
        ModConfig.fusionCoolingPerPercent = DecayFusionSpecs.FUSION_COOLING_PER_PERCENT.get();
        ModConfig.fusionBaseTemp = DecayFusionSpecs.FUSION_BASE_TEMP.get();
        ModConfig.fusionTempMax = DecayFusionSpecs.FUSION_TEMP_MAX.get();
        ModConfig.fusionTempTrip = DecayFusionSpecs.FUSION_TEMP_TRIP.get();
        ModConfig.fusionTempOptMin = DecayFusionSpecs.FUSION_TEMP_OPT_MIN.get();
        ModConfig.fusionTempOptMax = DecayFusionSpecs.FUSION_TEMP_OPT_MAX.get();
        ModConfig.fusionTempResume = DecayFusionSpecs.FUSION_TEMP_RESUME.get();
        ModConfig.fusionTempStep = DecayFusionSpecs.FUSION_TEMP_STEP.get();
        ModConfig.fusionCoolerDurabilityInterval = DecayFusionSpecs.FUSION_COOLER_DURABILITY_INTERVAL.get();
        ModConfig.fusionAshPerEnergy = DecayFusionSpecs.FUSION_ASH_PER_ENERGY.get();
        ModConfig.fusionRodEnergy = DecayFusionSpecs.FUSION_ROD_ENERGY.get();
        // 无线赤能源
        ModConfig.wirelessBaseLoss = DecayFusionSpecs.WIRELESS_BASE_LOSS.get();
        ModConfig.wirelessLossPerBlock = DecayFusionSpecs.WIRELESS_LOSS_PER_BLOCK.get();
        ModConfig.wirelessMaxLoss = DecayFusionSpecs.WIRELESS_MAX_LOSS.get();
        ModConfig.wirelessCrossDimLoss = DecayFusionSpecs.WIRELESS_CROSS_DIM_LOSS.get();
        ModConfig.wirelessLossReductionPerModule = DecayFusionSpecs.WIRELESS_LOSS_REDUCTION_PER_MODULE.get();
        ModConfig.wirelessFieldOwnerOnly = DecayFusionSpecs.WIRELESS_FIELD_OWNER_ONLY.get();

        // 器官·品质曲线（列表 → 数组整体发布，volatile 引用保证读取端原子可见）
        ModConfig.organTierMultiplier = toDoubleArray(OrganSpecs.ORGAN_TIER_MULTIPLIER.get());
        ModConfig.organTierBaseRejection = toIntArray(OrganSpecs.ORGAN_TIER_BASE_REJECTION.get());
        ModConfig.organTierGrowthInterval = toIntArray(OrganSpecs.ORGAN_TIER_GROWTH_INTERVAL.get());
        // 基因来源组排斥系数
        ModConfig.groupRejectionFactor = toDoubleArray(OrganSpecs.GROUP_REJECTION_FACTOR.get());
        // 纯度联动
        ModConfig.purityRejectionCap = OrganSpecs.PURITY_REJECTION_CAP.get();
        ModConfig.purityCompatWeight = OrganSpecs.PURITY_COMPAT_WEIGHT.get();
        // 排斥标尺与阈值
        ModConfig.maxRejection = OrganSpecs.MAX_REJECTION.get();
        ModConfig.rejectionWarning = OrganSpecs.REJECTION_WARNING.get();
        ModConfig.rejectionPoison = OrganSpecs.REJECTION_POISON.get();
        ModConfig.compatSevereThreshold = OrganSpecs.COMPAT_SEVERE_THRESHOLD.get();
        ModConfig.slotDebuffCleanThreshold = OrganSpecs.SLOT_DEBUFF_CLEAN_THRESHOLD.get();
        ModConfig.slotDebuffSevereThreshold = OrganSpecs.SLOT_DEBUFF_SEVERE_THRESHOLD.get();
        ModConfig.growthIntervalMinTicks = OrganSpecs.GROWTH_INTERVAL_MIN_TICKS.get();
        ModConfig.conflictPunishIntervalTicks = OrganSpecs.CONFLICT_PUNISH_INTERVAL_TICKS.get();
        ModConfig.conflictPunishDamage = OrganSpecs.CONFLICT_PUNISH_DAMAGE.get();
        ModConfig.overloadLight = OrganSpecs.OVERLOAD_LIGHT.get();
        ModConfig.overloadHeavy = OrganSpecs.OVERLOAD_HEAVY.get();
        // 血清
        ModConfig.serumWashReduce = OrganSpecs.SERUM_WASH_REDUCE.get();
        ModConfig.serumWashLimit = OrganSpecs.SERUM_WASH_LIMIT.get();
        ModConfig.serumCooldownTicks = OrganSpecs.SERUM_COOLDOWN_TICKS.get();
        // 突变词条
        ModConfig.traitBenignRatio = OrganSpecs.TRAIT_BENIGN_RATIO.get();
        ModConfig.traitRarityHighThreshold = OrganSpecs.TRAIT_RARITY_HIGH_THRESHOLD.get();
        ModConfig.traitRarityMidThreshold = OrganSpecs.TRAIT_RARITY_MID_THRESHOLD.get();
        // 培养机品质升级
        ModConfig.cultivatorUpgradeSuccess = toIntArray(OrganSpecs.CULTIVATOR_UPGRADE_SUCCESS.get());
        ModConfig.cultivatorUpgradeEnergy = toIntArray(OrganSpecs.CULTIVATOR_UPGRADE_ENERGY.get());
        ModConfig.cultivatorUpgradeSolid = toIntArray(OrganSpecs.CULTIVATOR_UPGRADE_SOLID.get());
        ModConfig.cultivatorUpgradeTicks = toIntArray(OrganSpecs.CULTIVATOR_UPGRADE_TICKS.get());
        ModConfig.cultivatorUpgradeCompatBonus = OrganSpecs.CULTIVATOR_UPGRADE_COMPAT_BONUS.get();
        // 机器全局倍率
        ModConfig.machineWorkSpeed = MechSpecs.MACHINE_WORK_SPEED.get();
        ModConfig.machineCostMultiplier = MechSpecs.MACHINE_COST_MULTIPLIER.get();
        // 机制开关
        ModConfig.decayZoneEnabled = MechSpecs.DECAY_ZONE_ENABLED.get();
        ModConfig.sunlightBurnEnabled = MechSpecs.SUNLIGHT_BURN_ENABLED.get();
        ModConfig.overloadEnabled = MechSpecs.OVERLOAD_ENABLED.get();
        // 理智系统（总开关 + 三项手感时长；总开关随配置同步包下发，客户端 HUD 据此停画）
        ModConfig.sanityEnabled = MechSpecs.SANITY_ENABLED.get();
        ModConfig.sanityDarkLightCooldownTicks = SanitySpecs.SANITY_DARK_LIGHT_COOLDOWN_TICKS.get();
        ModConfig.sanityFoodRefreshTicks = SanitySpecs.SANITY_FOOD_REFRESH_TICKS.get();
        ModConfig.sanityNetherRoofY = SanitySpecs.SANITY_NETHER_ROOF_Y.get();
        ModConfig.sanityNaturalRegenPeriodTicks = SanitySpecs.SANITY_NATURAL_REGEN_PERIOD_TICKS.get();
        ModConfig.sanityFlowerCountThreshold = SanitySpecs.SANITY_FLOWER_COUNT_THRESHOLD.get();
        ModConfig.sanitySleepDeprivationDays = SanitySpecs.SANITY_SLEEP_DEPRIVATION_DAYS.get();
        ModConfig.sanitySleepDeprivationDailyDebit = SanitySpecs.SANITY_SLEEP_DEPRIVATION_DAILY_DEBIT.get();
        // 赤石饰品扩展槽
        ModConfig.curioSlotUnlockRequired = SanitySpecs.CURIO_SLOT_UNLOCK_REQUIRED.get();
        ModConfig.curioSlotUnlockThresholds = toIntArray(SanitySpecs.CURIO_SLOT_UNLOCK_THRESHOLDS.get());

        // 禁断四件 · 1 生命之触
        ModConfig.lifeTouchEnabled = CurioSpecs.LIFE_TOUCH_ENABLED.get();
        ModConfig.lifeTouchReachBonus = CurioSpecs.LIFE_TOUCH_REACH_BONUS.get();
        ModConfig.lifeTouchDoubleStrikeChance = CurioSpecs.LIFE_TOUCH_DOUBLE_STRIKE_CHANCE.get();
        ModConfig.lifeTouchSelfHurtChance = CurioSpecs.LIFE_TOUCH_SELF_HURT_CHANCE.get();
        ModConfig.lifeTouchSelfHurtDamage = CurioSpecs.LIFE_TOUCH_SELF_HURT_DAMAGE.get();
        ModConfig.lifeTouchHungerCostChance = CurioSpecs.LIFE_TOUCH_HUNGER_COST_CHANCE.get();
        ModConfig.lifeTouchHungerCostAmount = CurioSpecs.LIFE_TOUCH_HUNGER_COST_AMOUNT.get();
        ModConfig.lifeTouchHungerRestoreChance = CurioSpecs.LIFE_TOUCH_HUNGER_RESTORE_CHANCE.get();
        ModConfig.lifeTouchHungerRestoreAmount = CurioSpecs.LIFE_TOUCH_HUNGER_RESTORE_AMOUNT.get();
        ModConfig.lifeTouchHitCacheTicks = CurioSpecs.LIFE_TOUCH_HIT_CACHE_TICKS.get();
        // 禁断四件 · 2 幼崽之心
        ModConfig.cubHeartEnabled = CurioSpecs.CUB_HEART_ENABLED.get();
        ModConfig.cubHeartAbsorptionRatio = CurioSpecs.CUB_HEART_ABSORPTION_RATIO.get();
        ModConfig.cubHeartEffectInterval = CurioSpecs.CUB_HEART_EFFECT_INTERVAL.get();
        ModConfig.cubHeartSlowHasteChance = CurioSpecs.CUB_HEART_SLOW_HASTE_CHANCE.get();
        ModConfig.cubHeartSlowHasteAmplitude = CurioSpecs.CUB_HEART_SLOW_HASTE_AMPLITUDE.get();
        ModConfig.cubHeartSlowHasteTicks = CurioSpecs.CUB_HEART_SLOW_HASTE_TICKS.get();
        ModConfig.cubHeartDamageShiftChance = CurioSpecs.CUB_HEART_DAMAGE_SHIFT_CHANCE.get();
        ModConfig.cubHeartDamageShiftAmount = CurioSpecs.CUB_HEART_DAMAGE_SHIFT_AMOUNT.get();
        ModConfig.cubHeartDamageShiftTicks = CurioSpecs.CUB_HEART_DAMAGE_SHIFT_TICKS.get();
        ModConfig.cubHeartUnnameableChance = CurioSpecs.CUB_HEART_UNNAMEABLE_CHANCE.get();
        ModConfig.cubHeartUnnameableAmplifier = CurioSpecs.CUB_HEART_UNNAMEABLE_AMPLIFIER.get();
        ModConfig.cubHeartUnnameableTicks = CurioSpecs.CUB_HEART_UNNAMEABLE_TICKS.get();
        ModConfig.cubHeartExcitementAttackSpeed = CurioSpecs.CUB_HEART_EXCITEMENT_ATTACK_SPEED.get();
        ModConfig.cubHeartExcitementTicks = CurioSpecs.CUB_HEART_EXCITEMENT_TICKS.get();
        // 禁断四件 · 3 母神之印
        ModConfig.motherSealEnabled = CurioSpecs.MOTHER_SEAL_ENABLED.get();
        ModConfig.motherSealDamageBonus = CurioSpecs.MOTHER_SEAL_DAMAGE_BONUS.get();
        ModConfig.motherSealHealthBonus = CurioSpecs.MOTHER_SEAL_HEALTH_BONUS.get();
        ModConfig.motherSealSpeedBonus = CurioSpecs.MOTHER_SEAL_SPEED_BONUS.get();
        ModConfig.motherSealAttackSpeedBonus = CurioSpecs.MOTHER_SEAL_ATTACK_SPEED_BONUS.get();
        ModConfig.motherSealSelfUnnameablePeriod = CurioSpecs.MOTHER_SEAL_SELF_UNNAMEABLE_PERIOD.get();
        ModConfig.motherSealSelfUnnameableTicks = CurioSpecs.MOTHER_SEAL_SELF_UNNAMEABLE_TICKS.get();
        ModConfig.motherSealSelfUnnameableAmplifier = CurioSpecs.MOTHER_SEAL_SELF_UNNAMEABLE_AMPLIFIER.get();
        ModConfig.motherSealVegetarianHungerCost = CurioSpecs.MOTHER_SEAL_VEGETARIAN_HUNGER_COST.get();
        ModConfig.motherSealVegetarianNauseaTicks = CurioSpecs.MOTHER_SEAL_VEGETARIAN_NAUSEA_TICKS.get();
        ModConfig.motherSealResistFactor = CurioSpecs.MOTHER_SEAL_RESIST_FACTOR.get();
        // 禁断四件 · 4 孕育之环
        ModConfig.fertilityRingEnabled = CurioSpecs.FERTILITY_RING_ENABLED.get();
        ModConfig.fertilityRingHealChance = CurioSpecs.FERTILITY_RING_HEAL_CHANCE.get();
        ModConfig.fertilityRingHealAmount = CurioSpecs.FERTILITY_RING_HEAL_AMOUNT.get();
        ModConfig.fertilityRingHealHungerCost = CurioSpecs.FERTILITY_RING_HEAL_HUNGER_COST.get();
        ModConfig.fertilityRingAttackSpeedBonus = CurioSpecs.FERTILITY_RING_ATTACK_SPEED_BONUS.get();
        ModConfig.fertilityRingAttackSpeedTicks = CurioSpecs.FERTILITY_RING_ATTACK_SPEED_TICKS.get();
        ModConfig.fertilityRingMeatHealAmount = CurioSpecs.FERTILITY_RING_MEAT_HEAL_AMOUNT.get();
        ModConfig.fertilityRingSatiatedMeatOnly = CurioSpecs.FERTILITY_RING_SATIATED_MEAT_ONLY.get();
        ModConfig.fertilityRingStarveMultiplier = CurioSpecs.FERTILITY_RING_STARVE_MULTIPLIER.get();
        ModConfig.fertilityRingStarveLethal = CurioSpecs.FERTILITY_RING_STARVE_LETHAL.get();
        // 禁断四件 · 5 套装
        ModConfig.setAttackCountRequired = CurioSpecs.SET_ATTACK_COUNT_REQUIRED.get();
        ModConfig.setAttackCountHungerRestore = CurioSpecs.SET_ATTACK_COUNT_HUNGER_RESTORE.get();
        ModConfig.setUnnameableLevelBonus = CurioSpecs.SET_UNNAMEABLE_LEVEL_BONUS.get();
        ModConfig.setSuppressDistortion = CurioSpecs.SET_SUPPRESS_DISTORTION.get();
        ModConfig.setDamageReduction = CurioSpecs.SET_DAMAGE_REDUCTION.get();
        ModConfig.setUnnameableCritChance = CurioSpecs.SET_UNNAMEABLE_CRIT_CHANCE.get();
        ModConfig.setUnnameableCritDamage = CurioSpecs.SET_UNNAMEABLE_CRIT_DAMAGE.get();
        ModConfig.setNearDeathHealPercent = CurioSpecs.SET_NEAR_DEATH_HEAL_PERCENT.get();
        ModConfig.setNearDeathUnnameableTicks = CurioSpecs.SET_NEAR_DEATH_UNNAMEABLE_TICKS.get();
        ModConfig.setNearDeathCooldownTicks = CurioSpecs.SET_NEAR_DEATH_COOLDOWN_TICKS.get();
        // 禁断四件 · 6 侵蚀
        ModConfig.erosionEnabled = CurioSpecs.EROSION_ENABLED.get();
        ModConfig.erosionDurationMinutes = CurioSpecs.EROSION_DURATION_MINUTES.get();
        ModConfig.erosionNoticeThresholds = toIntArray(CurioSpecs.EROSION_NOTICE_THRESHOLDS.get());
        ModConfig.erosionNoticeIntervalMinutes = CurioSpecs.EROSION_NOTICE_INTERVAL_MINUTES.get();
        ModConfig.erosionStatRerollPercent = CurioSpecs.EROSION_STAT_REROLL_PERCENT.get();
        ModConfig.erosionNbtFlushTicks = CurioSpecs.EROSION_NBT_FLUSH_TICKS.get();
        ModConfig.erosionScreenFlashEnabled = CurioSpecs.EROSION_SCREEN_FLASH_ENABLED.get();
        // 禁断四件 · 7 仪式与吸取
        ModConfig.altarDrainEnabled = CurioSpecs.ALTAR_DRAIN_ENABLED.get();
        ModConfig.altarDrainRadius = CurioSpecs.ALTAR_DRAIN_RADIUS.get();
        ModConfig.altarDrainIntervalTicks = CurioSpecs.ALTAR_DRAIN_INTERVAL_TICKS.get();
        ModConfig.altarDrainHealthPercent = CurioSpecs.ALTAR_DRAIN_HEALTH_PERCENT.get();
        ModConfig.altarDrainEnergyPerHp = CurioSpecs.ALTAR_DRAIN_ENERGY_PER_HP.get();
        ModConfig.altarDrainMaxTargets = CurioSpecs.ALTAR_DRAIN_MAX_TARGETS.get();
        ModConfig.altarDrainMaxEnergyPerTarget = CurioSpecs.ALTAR_DRAIN_MAX_ENERGY_PER_TARGET.get();
        ModConfig.altarDrainExemptCreative = CurioSpecs.ALTAR_DRAIN_EXEMPT_CREATIVE.get();
        ModConfig.altarNewRecipeTierRequired = CurioSpecs.ALTAR_NEW_RECIPE_TIER_REQUIRED.get();
        ModConfig.altarNewRecipeProgressMax = CurioSpecs.ALTAR_NEW_RECIPE_PROGRESS_MAX.get();
        ModConfig.altarLegacyProgressMax = CurioSpecs.ALTAR_LEGACY_PROGRESS_MAX.get();

        // 生命研究机器
        ModConfig.geneAnalyzerLifeCost = LifeMachineSpecs.GENE_ANALYZER_LIFE_COST.get();
        ModConfig.geneAnalyzerLifeCapacity = LifeMachineSpecs.GENE_ANALYZER_LIFE_CAPACITY.get();
        ModConfig.geneAnalyzerProcessTicks = LifeMachineSpecs.GENE_ANALYZER_PROCESS_TICKS.get();
        ModConfig.geneAnalyzerMinSuccessRate = LifeMachineSpecs.GENE_ANALYZER_MIN_SUCCESS.get();
        ModConfig.geneAnalyzerMaxSuccessRate = LifeMachineSpecs.GENE_ANALYZER_MAX_SUCCESS.get();
        ModConfig.lifeStructLifeCost = LifeMachineSpecs.LIFE_STRUCT_LIFE_COST.get();
        ModConfig.lifeStructSolidCost = LifeMachineSpecs.LIFE_STRUCT_SOLID_COST.get();
        ModConfig.lifeStructLifeCapacity = LifeMachineSpecs.LIFE_STRUCT_LIFE_CAPACITY.get();
        ModConfig.lifeStructProcessTicks = LifeMachineSpecs.LIFE_STRUCT_PROCESS_TICKS.get();
        ModConfig.lifeBreederLifeCost = LifeMachineSpecs.LIFE_BREEDER_LIFE_COST.get();
        ModConfig.lifeBreederCrystalCost = LifeMachineSpecs.LIFE_BREEDER_CRYSTAL_COST.get();
        ModConfig.lifeBreederLifeCapacity = LifeMachineSpecs.LIFE_BREEDER_LIFE_CAPACITY.get();
        ModConfig.lifeBreederProcessTicks = LifeMachineSpecs.LIFE_BREEDER_PROCESS_TICKS.get();
        ModConfig.lifeBreederMinSuccessRate = LifeMachineSpecs.LIFE_BREEDER_MIN_SUCCESS.get();
        ModConfig.lifeBreederMaxSuccessRate = LifeMachineSpecs.LIFE_BREEDER_MAX_SUCCESS.get();
        ModConfig.traitReforgerLifeCost = LifeMachineSpecs.TRAIT_REFORGER_LIFE_COST.get();
        ModConfig.traitReforgerLifeCapacity = LifeMachineSpecs.TRAIT_REFORGER_LIFE_CAPACITY.get();
        ModConfig.traitReforgerProcessTicks = LifeMachineSpecs.TRAIT_REFORGER_PROCESS_TICKS.get();
        ModConfig.traitReforgerCrystalPerRarity = LifeMachineSpecs.TRAIT_REFORGER_CRYSTAL_PER_RARITY.get();
        ModConfig.transgeneFactoryLifeCost = LifeMachineSpecs.TRANSGENE_FACTORY_LIFE_COST.get();
        ModConfig.transgeneFactoryLifeCapacity = LifeMachineSpecs.TRANSGENE_FACTORY_LIFE_CAPACITY.get();
        ModConfig.transgeneFactoryProcessTicks = LifeMachineSpecs.TRANSGENE_FACTORY_PROCESS_TICKS.get();
        ModConfig.surgeryImplantSolidCost = LifeMachineSpecs.SURGERY_IMPLANT_SOLID_COST.get();
        ModConfig.surgeryImplantLifeCost = LifeMachineSpecs.SURGERY_IMPLANT_LIFE_COST.get();
        ModConfig.surgeryExtractSolidCost = LifeMachineSpecs.SURGERY_EXTRACT_SOLID_COST.get();
        ModConfig.surgeryExtractLifeCost = LifeMachineSpecs.SURGERY_EXTRACT_LIFE_COST.get();
        ModConfig.surgeryLifeCapacity = LifeMachineSpecs.SURGERY_LIFE_CAPACITY.get();
        ModConfig.surgeryProcessTicks = LifeMachineSpecs.SURGERY_PROCESS_TICKS.get();
        ModConfig.organVaultLifeCapacity = LifeMachineSpecs.ORGAN_VAULT_LIFE_CAPACITY.get();
        ModConfig.organVaultKeepCostPerTick = LifeMachineSpecs.ORGAN_VAULT_KEEP_COST.get();
        ModConfig.potionTableLifeCapacity = LifeMachineSpecs.POTION_TABLE_LIFE_CAPACITY.get();
        // 能量机器
        ModConfig.energyProcessorChishiRate = LifeMachineSpecs.ENERGY_PROCESSOR_CHISHI_RATE.get();
        ModConfig.energyProcessorChishiCapacity = LifeMachineSpecs.ENERGY_PROCESSOR_CHISHI_CAPACITY.get();
        ModConfig.energyProcessorTankCapacity = LifeMachineSpecs.ENERGY_PROCESSOR_TANK_CAPACITY.get();
        ModConfig.energyProcessorChishiCost = LifeMachineSpecs.ENERGY_PROCESSOR_CHISHI_COST.get();
        ModConfig.energyLiquefierChishiRate = LifeMachineSpecs.ENERGY_LIQUEFIER_CHISHI_RATE.get();
        ModConfig.energyLiquefierChishiCapacity = LifeMachineSpecs.ENERGY_LIQUEFIER_CHISHI_CAPACITY.get();
        ModConfig.energyLiquefierTankCapacity = LifeMachineSpecs.ENERGY_LIQUEFIER_TANK_CAPACITY.get();
        ModConfig.fuelMixerChishiRate = LifeMachineSpecs.FUEL_MIXER_CHISHI_RATE.get();
        ModConfig.fuelMixerChishiCapacity = LifeMachineSpecs.FUEL_MIXER_CHISHI_CAPACITY.get();
        ModConfig.fuelMixerChishiCost = LifeMachineSpecs.FUEL_MIXER_CHISHI_COST.get();
        ModConfig.fuelMixerTankCapacity = LifeMachineSpecs.FUEL_MIXER_TANK_CAPACITY.get();
        ModConfig.fuelCannerTankCapacity = LifeMachineSpecs.FUEL_CANNER_TANK_CAPACITY.get();
        ModConfig.fuelCannerFillRate = LifeMachineSpecs.FUEL_CANNER_FILL_RATE.get();
        ModConfig.energyAggregatorEnergyPerIngot = LifeMachineSpecs.ENERGY_AGGREGATOR_PER_INGOT.get();
        ModConfig.energyAggregatorEnergyPerGeodeUpgrade = LifeMachineSpecs.ENERGY_AGGREGATOR_PER_GEODE.get();
        ModConfig.energyAggregatorEnergyCapacity = LifeMachineSpecs.ENERGY_AGGREGATOR_CAPACITY.get();
        ModConfig.energyGeneratorGenerateRate = LifeMachineSpecs.ENERGY_GENERATOR_RATE.get();
        ModConfig.energyAssemblyGenerateRate = LifeMachineSpecs.ENERGY_ASSEMBLY_RATE.get();
        ModConfig.superGeneratorCoreGenerateRate = LifeMachineSpecs.SUPER_GENERATOR_CORE_RATE.get();
        ModConfig.energyCellSerializerBaseCapacity = LifeMachineSpecs.ENERGY_CELL_BASE_CAPACITY.get();
        ModConfig.upgradeStationEnergyPerUpgrade = LifeMachineSpecs.UPGRADE_STATION_PER_UPGRADE.get();
        ModConfig.upgradeStationEnergyCapacity = LifeMachineSpecs.UPGRADE_STATION_CAPACITY.get();
        ModConfig.equipmentForgerEnergyPerForge = LifeMachineSpecs.EQUIPMENT_FORGER_PER_FORGE.get();
        ModConfig.equipmentForgerEnergyCapacity = LifeMachineSpecs.EQUIPMENT_FORGER_CAPACITY.get();
        // 净化与矩阵
        ModConfig.purifierEnergyPerTick = LifeMachineSpecs.PURIFIER_ENERGY_PER_TICK.get();
        ModConfig.purifierBurnRate = LifeMachineSpecs.PURIFIER_BURN_RATE.get();
        ModConfig.purifierTotalCost = LifeMachineSpecs.PURIFIER_TOTAL_COST.get();
        ModConfig.purifierRateFormed = LifeMachineSpecs.PURIFIER_RATE_FORMED.get();
        ModConfig.purifierMatrixTotalCost = LifeMachineSpecs.PURIFIER_MATRIX_TOTAL_COST.get();
        ModConfig.purifierMatrixRateFormed = LifeMachineSpecs.PURIFIER_MATRIX_RATE_FORMED.get();
        ModConfig.lifePurifierChishiRate = LifeMachineSpecs.LIFE_PURIFIER_CHISHI_RATE.get();
        ModConfig.lifePurifierTotalCost = LifeMachineSpecs.LIFE_PURIFIER_TOTAL_COST.get();
        ModConfig.lifePurifierLifeCost = LifeMachineSpecs.LIFE_PURIFIER_LIFE_COST.get();
        ModConfig.lifePurifierChishiCapacity = LifeMachineSpecs.LIFE_PURIFIER_CHISHI_CAPACITY.get();
        ModConfig.lifePurifierLifeCapacity = LifeMachineSpecs.LIFE_PURIFIER_LIFE_CAPACITY.get();
        ModConfig.lifeMatrixConversionsPerTick = LifeMachineSpecs.LIFE_MATRIX_CONVERSIONS_PER_TICK.get();
        ModConfig.lifeMatrixConversionCost = LifeMachineSpecs.LIFE_MATRIX_CONVERSION_COST.get();
        ModConfig.lifeMatrixChishiCapacity = LifeMachineSpecs.LIFE_MATRIX_CHISHI_CAPACITY.get();
        ModConfig.lifeMatrixLifeCapacity = LifeMachineSpecs.LIFE_MATRIX_LIFE_CAPACITY.get();
        ModConfig.lifeConversionConversionsPerTick = LifeMachineSpecs.LIFE_CONVERSION_PER_TICK.get();
        ModConfig.lifeConversionChishiCapacity = LifeMachineSpecs.LIFE_CONVERSION_CHISHI_CAPACITY.get();
        ModConfig.lifeConversionLifeCapacity = LifeMachineSpecs.LIFE_CONVERSION_LIFE_CAPACITY.get();
        ModConfig.lifeAggregationConversionCost = LifeMachineSpecs.LIFE_AGGREGATION_COST.get();
        ModConfig.lifeAggregationConversionOutput = LifeMachineSpecs.LIFE_AGGREGATION_OUTPUT.get();
        ModConfig.lifeAggregationChishiCapacity = LifeMachineSpecs.LIFE_AGGREGATION_CHISHI_CAPACITY.get();
        ModConfig.lifeAggregationLifeCapacity = LifeMachineSpecs.LIFE_AGGREGATION_LIFE_CAPACITY.get();
        // 机械改造机器
        ModConfig.mechanicalChishiCapacity = MechSpecs.MECH_CHISHI_CAPACITY.get();
        ModConfig.mechanicalLifeCapacity = MechSpecs.MECH_LIFE_CAPACITY.get();
        ModConfig.mechTemplateChishiCost = MechSpecs.MECH_TEMPLATE_CHISHI_COST.get();
        ModConfig.mechTemplateLifeCost = MechSpecs.MECH_TEMPLATE_LIFE_COST.get();
        ModConfig.mechTemplateTicks = MechSpecs.MECH_TEMPLATE_TICKS.get();
        ModConfig.mechProcessChishiBase = toLongArray(MechSpecs.MECH_PROCESS_CHISHI_BASE.get());
        ModConfig.mechProcessLifeBase = toLongArray(MechSpecs.MECH_PROCESS_LIFE_BASE.get());
        ModConfig.mechProcessTicksBase = toIntArray(MechSpecs.MECH_PROCESS_TICKS_BASE.get());
        ModConfig.mechProcessPartFactor = toIntArray(MechSpecs.MECH_PROCESS_PART_FACTOR.get());
        ModConfig.mechProcessMaterialCount = toIntArray(MechSpecs.MECH_PROCESS_MATERIAL_COUNT.get());
        ModConfig.mechAssemblyChishiCost = MechSpecs.MECH_ASSEMBLY_CHISHI_COST.get();
        ModConfig.mechAssemblyLifeCost = MechSpecs.MECH_ASSEMBLY_LIFE_COST.get();
        ModConfig.mechAssemblyTicks = MechSpecs.MECH_ASSEMBLY_TICKS.get();
        // 机械义体属性换算
        ModConfig.mechBodyHealthScale = MechSpecs.MECH_BODY_HEALTH_SCALE.get();
        ModConfig.mechBodyAttackScale = MechSpecs.MECH_BODY_ATTACK_SCALE.get();
        ModConfig.mechBodyAttackSpeedScale = MechSpecs.MECH_BODY_ATTACK_SPEED_SCALE.get();
        ModConfig.mechBodyMovementSpeedScale = MechSpecs.MECH_BODY_MOVEMENT_SPEED_SCALE.get();
        ModConfig.mechBodyArmorScale = MechSpecs.MECH_BODY_ARMOR_SCALE.get();
        ModConfig.mechBodyCritChanceScale = MechSpecs.MECH_BODY_CRIT_CHANCE_SCALE.get();
        ModConfig.mechBodyCritDamageScale = MechSpecs.MECH_BODY_CRIT_DAMAGE_SCALE.get();
        ModConfig.mechBodyRangeScale = MechSpecs.MECH_BODY_RANGE_SCALE.get();
        ModConfig.mechBodyDodgeScale = MechSpecs.MECH_BODY_DODGE_SCALE.get();
        // 基因属性权重
        ModConfig.geneWeightStrength = MechSpecs.GENE_WEIGHT_STRENGTH.get();
        // 底层战斗（暴击/闪避）
        ModConfig.combatCritEnabled = MechSpecs.COMBAT_CRIT_ENABLED.get();
        ModConfig.combatDodgeEnabled = MechSpecs.COMBAT_DODGE_ENABLED.get();
        ModConfig.combatCritChanceCap = MechSpecs.COMBAT_CRIT_CHANCE_CAP.get();
        ModConfig.combatCritDamageCap = MechSpecs.COMBAT_CRIT_DAMAGE_CAP.get();
        ModConfig.combatDodgeChanceCap = MechSpecs.COMBAT_DODGE_CHANCE_CAP.get();
        // 端口与电池缓冲
        ModConfig.lifeMatrixInputPortBufferCapacity = BufferValueSpecs.LIFE_MATRIX_INPUT_PORT_BUFFER.get();
        ModConfig.lifeMatrixOutputPortBufferCapacity = BufferValueSpecs.LIFE_MATRIX_OUTPUT_PORT_BUFFER.get();
        ModConfig.purifierEnergyInputPortBufferCapacity = BufferValueSpecs.PURIFIER_INPUT_PORT_BUFFER.get();
        ModConfig.minerPortBufferCapacity = BufferValueSpecs.MINER_PORT_BUFFER.get();
        ModConfig.minerEnergyInputBufferCapacity = BufferValueSpecs.MINER_ENERGY_INPUT_BUFFER.get();
        ModConfig.wirelessInputPortBufferCapacity = BufferValueSpecs.WIRELESS_INPUT_PORT_BUFFER.get();
        ModConfig.wirelessOutputPortBufferCapacity = BufferValueSpecs.WIRELESS_OUTPUT_PORT_BUFFER.get();
        ModConfig.genEnergyOutputPortBufferCapacity = BufferValueSpecs.GEN_ENERGY_OUTPUT_BUFFER.get();
        ModConfig.fusionEnergyOutputBufferCapacity = BufferValueSpecs.FUSION_ENERGY_OUTPUT_BUFFER.get();
        ModConfig.reactorEnergyOutputBufferCapacity = BufferValueSpecs.REACTOR_ENERGY_OUTPUT_BUFFER.get();
        ModConfig.lifeEnergyCellSerializerBaseCapacity = BufferValueSpecs.LIFE_ENERGY_CELL_SERIALIZER_CAPACITY.get();
        ModConfig.plasmaTankCapacity = BufferValueSpecs.PLASMA_TANK_CAPACITY.get();
        ModConfig.itemTerminalEnergyBuffer = BufferValueSpecs.ITEM_TERMINAL_ENERGY_BUFFER.get();
        ModConfig.itemTerminalEnergyPortBufferCapacity = BufferValueSpecs.ITEM_TERMINAL_ENERGY_PORT_BUFFER.get();

        // 培养机提纯与分馏机
        ModConfig.cultivatorLifeCapacity = BufferValueSpecs.CULTIVATOR_LIFE_CAPACITY.get();
        ModConfig.cultivatorPurifySuccess = toIntArray(BufferValueSpecs.CULTIVATOR_PURIFY_SUCCESS.get());
        ModConfig.cultivatorPurifyEnergy = toLongArray(BufferValueSpecs.CULTIVATOR_PURIFY_ENERGY.get());
        ModConfig.cultivatorPurifySolid = toIntArray(BufferValueSpecs.CULTIVATOR_PURIFY_SOLID.get());
        ModConfig.cultivatorPurifyTicks = toIntArray(BufferValueSpecs.CULTIVATOR_PURIFY_TICKS.get());
        ModConfig.cultivatorPurifyGain = BufferValueSpecs.CULTIVATOR_PURIFY_GAIN.get();
        ModConfig.fractionatorEnergyCapacity = BufferValueSpecs.FRACTIONATOR_ENERGY_CAPACITY.get();
        ModConfig.fractionatorCostPerCraft = BufferValueSpecs.FRACTIONATOR_COST_PER_CRAFT.get();
        ModConfig.fractionatorProcessTicks = BufferValueSpecs.FRACTIONATOR_PROCESS_TICKS.get();

        // 价值分（统一存储库定价内核）
        ModConfig.valueCostMultiplier = BufferValueSpecs.VALUE_COST_MULTIPLIER.get();
        ModConfig.valueIngredientWeight = BufferValueSpecs.VALUE_INGREDIENT_WEIGHT.get();
        ModConfig.valueMagicBonus = BufferValueSpecs.VALUE_MAGIC_BONUS.get();
        ModConfig.valueIngredientCap = BufferValueSpecs.VALUE_INGREDIENT_CAP.get();
        ModConfig.valueIterations = BufferValueSpecs.VALUE_ITERATIONS.get();
        ModConfig.valueLootEnabled = BufferValueSpecs.VALUE_LOOT_ENABLED.get();
        ModConfig.valueLootAutoApply = BufferValueSpecs.VALUE_LOOT_AUTO_APPLY.get();
        ModConfig.valueLootCap = BufferValueSpecs.VALUE_LOOT_CAP.get();
        ModConfig.valueFluidEnabled = BufferValueSpecs.VALUE_FLUID_ENABLED.get();
        ModConfig.valueFluidPerMbCap = BufferValueSpecs.VALUE_FLUID_PER_MB_CAP.get();
        ModConfig.valueOverrides = toStringArray(BufferValueSpecs.VALUE_OVERRIDES.get());
        ModConfig.valueFluidValues = toStringArray(BufferValueSpecs.VALUE_FLUID_VALUES.get());
        ModConfig.valueTagValues = toStringArray(BufferValueSpecs.VALUE_TAG_VALUES.get());
        ModConfig.valueTierBonus = toStringArray(BufferValueSpecs.VALUE_TIER_BONUS.get());
        ModConfig.valueKeywordExclusions = toStringArray(BufferValueSpecs.VALUE_KEYWORD_EXCLUSIONS.get());
        ModConfig.valueLootBlacklist = toStringArray(BufferValueSpecs.VALUE_LOOT_BLACKLIST.get());
        ModConfig.unifiedVaultRows = BufferValueSpecs.UNIFIED_VAULT_ROWS.get();

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
