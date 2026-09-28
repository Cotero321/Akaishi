package com.example.akaishi.forge.client;

import com.example.akaishi.forge.config.AkaishiConfig;
import com.example.akaishi.forge.config.AkaishiConfigSync;
import com.example.akaishi.forge.config.specs.BufferValueSpecs;
import com.example.akaishi.forge.config.specs.CoreMachineSpecs;
import com.example.akaishi.forge.config.specs.CurioSpecs;
import com.example.akaishi.forge.config.specs.DecayFusionSpecs;
import com.example.akaishi.forge.config.specs.LifeMachineSpecs;
import com.example.akaishi.forge.config.specs.MechSpecs;
import com.example.akaishi.forge.config.specs.OrganSpecs;
import com.example.akaishi.forge.config.specs.SanitySpecs;

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

    /** 注册 Mods 界面配置按钮（模组构造阶段，仅物理客户端调用） */
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
        doubleList(organQuality, eb, "organ_quality.multiplier", OrganSpecs.ORGAN_TIER_MULTIPLIER);
        intList(organQuality, eb, "organ_quality.baseRejection", OrganSpecs.ORGAN_TIER_BASE_REJECTION);
        intList(organQuality, eb, "organ_quality.growthInterval", OrganSpecs.ORGAN_TIER_GROWTH_INTERVAL);

        ConfigCategory groups = category(builder, "sample_groups");
        doubleList(groups, eb, "sample_groups.groupFactor", OrganSpecs.GROUP_REJECTION_FACTOR);

        ConfigCategory purity = category(builder, "purity");
        doubleField(purity, eb, "purity.purityRejectionCap", OrganSpecs.PURITY_REJECTION_CAP);
        doubleField(purity, eb, "purity.purityCompatWeight", OrganSpecs.PURITY_COMPAT_WEIGHT);

        ConfigCategory rejection = category(builder, "rejection");
        intField(rejection, eb, "rejection.maxRejection", OrganSpecs.MAX_REJECTION);
        intField(rejection, eb, "rejection.warning", OrganSpecs.REJECTION_WARNING);
        intField(rejection, eb, "rejection.poison", OrganSpecs.REJECTION_POISON);
        intField(rejection, eb, "rejection.compatSevere", OrganSpecs.COMPAT_SEVERE_THRESHOLD);
        intField(rejection, eb, "rejection.slotDebuffClean", OrganSpecs.SLOT_DEBUFF_CLEAN_THRESHOLD);
        intField(rejection, eb, "rejection.slotDebuffSevere", OrganSpecs.SLOT_DEBUFF_SEVERE_THRESHOLD);
        intField(rejection, eb, "rejection.growthIntervalMinTicks", OrganSpecs.GROWTH_INTERVAL_MIN_TICKS);
        intField(rejection, eb, "rejection.conflictInterval", OrganSpecs.CONFLICT_PUNISH_INTERVAL_TICKS);
        doubleField(rejection, eb, "rejection.conflictDamage", OrganSpecs.CONFLICT_PUNISH_DAMAGE);
        intField(rejection, eb, "rejection.overloadLight", OrganSpecs.OVERLOAD_LIGHT);
        intField(rejection, eb, "rejection.overloadHeavy", OrganSpecs.OVERLOAD_HEAVY);

        ConfigCategory serum = category(builder, "serum");
        intField(serum, eb, "serum.washReduce", OrganSpecs.SERUM_WASH_REDUCE);
        intField(serum, eb, "serum.washLimit", OrganSpecs.SERUM_WASH_LIMIT);
        intField(serum, eb, "serum.cooldown", OrganSpecs.SERUM_COOLDOWN_TICKS);

        ConfigCategory trait = category(builder, "trait");
        doubleField(trait, eb, "trait.benignRatio", OrganSpecs.TRAIT_BENIGN_RATIO);
        intField(trait, eb, "trait.rarityHigh", OrganSpecs.TRAIT_RARITY_HIGH_THRESHOLD);
        intField(trait, eb, "trait.rarityMid", OrganSpecs.TRAIT_RARITY_MID_THRESHOLD);

        ConfigCategory cultivator = category(builder, "cultivator_upgrade");
        intList(cultivator, eb, "cultivator_upgrade.successRate", OrganSpecs.CULTIVATOR_UPGRADE_SUCCESS);
        intList(cultivator, eb, "cultivator_upgrade.energyCost", OrganSpecs.CULTIVATOR_UPGRADE_ENERGY);
        intList(cultivator, eb, "cultivator_upgrade.solidCost", OrganSpecs.CULTIVATOR_UPGRADE_SOLID);
        intList(cultivator, eb, "cultivator_upgrade.processTicks", OrganSpecs.CULTIVATOR_UPGRADE_TICKS);
        intField(cultivator, eb, "cultivator_upgrade.compatBonus", OrganSpecs.CULTIVATOR_UPGRADE_COMPAT_BONUS);

        ConfigCategory machine = category(builder, "machine");
        doubleField(machine, eb, "machine.workSpeed", MechSpecs.MACHINE_WORK_SPEED);
        doubleField(machine, eb, "machine.costMultiplier", MechSpecs.MACHINE_COST_MULTIPLIER);

        // ===== 机械改造机器 =====
        ConfigCategory mechMachines = category(builder, "mechanical_machines");
        longField(mechMachines, eb, "mechanical_machines.chishiCapacity", MechSpecs.MECH_CHISHI_CAPACITY);
        longField(mechMachines, eb, "mechanical_machines.lifeCapacity", MechSpecs.MECH_LIFE_CAPACITY);
        longField(mechMachines, eb, "mechanical_machines.templateChishiCost", MechSpecs.MECH_TEMPLATE_CHISHI_COST);
        longField(mechMachines, eb, "mechanical_machines.templateLifeCost", MechSpecs.MECH_TEMPLATE_LIFE_COST);
        intField(mechMachines, eb, "mechanical_machines.templateTicks", MechSpecs.MECH_TEMPLATE_TICKS);
        longList(mechMachines, eb, "mechanical_machines.processChishiBase", MechSpecs.MECH_PROCESS_CHISHI_BASE);
        longList(mechMachines, eb, "mechanical_machines.processLifeBase", MechSpecs.MECH_PROCESS_LIFE_BASE);
        intList(mechMachines, eb, "mechanical_machines.processTicksBase", MechSpecs.MECH_PROCESS_TICKS_BASE);
        intList(mechMachines, eb, "mechanical_machines.processPartFactor", MechSpecs.MECH_PROCESS_PART_FACTOR);
        intList(mechMachines, eb, "mechanical_machines.processMaterialCount", MechSpecs.MECH_PROCESS_MATERIAL_COUNT);
        longField(mechMachines, eb, "mechanical_machines.assemblyChishiCost", MechSpecs.MECH_ASSEMBLY_CHISHI_COST);
        longField(mechMachines, eb, "mechanical_machines.assemblyLifeCost", MechSpecs.MECH_ASSEMBLY_LIFE_COST);
        intField(mechMachines, eb, "mechanical_machines.assemblyTicks", MechSpecs.MECH_ASSEMBLY_TICKS);

        // ===== 机械义体属性换算 =====
        ConfigCategory mechBody = category(builder, "mechanical_body");
        doubleField(mechBody, eb, "mechanical_body.healthScale", MechSpecs.MECH_BODY_HEALTH_SCALE);
        doubleField(mechBody, eb, "mechanical_body.attackScale", MechSpecs.MECH_BODY_ATTACK_SCALE);
        doubleField(mechBody, eb, "mechanical_body.attackSpeedScale", MechSpecs.MECH_BODY_ATTACK_SPEED_SCALE);
        doubleField(mechBody, eb, "mechanical_body.movementSpeedScale", MechSpecs.MECH_BODY_MOVEMENT_SPEED_SCALE);
        doubleField(mechBody, eb, "mechanical_body.armorScale", MechSpecs.MECH_BODY_ARMOR_SCALE);
        doubleField(mechBody, eb, "mechanical_body.critChanceScale", MechSpecs.MECH_BODY_CRIT_CHANCE_SCALE);
        doubleField(mechBody, eb, "mechanical_body.critDamageScale", MechSpecs.MECH_BODY_CRIT_DAMAGE_SCALE);
        doubleField(mechBody, eb, "mechanical_body.rangeScale", MechSpecs.MECH_BODY_RANGE_SCALE);
        doubleField(mechBody, eb, "mechanical_body.dodgeScale", MechSpecs.MECH_BODY_DODGE_SCALE);

        // ===== 基因属性权重 =====
        ConfigCategory geneWeight = category(builder, "gene_weight");
        doubleField(geneWeight, eb, "gene_weight.strength", MechSpecs.GENE_WEIGHT_STRENGTH);

        // ===== 底层战斗（暴击/闪避）=====
        ConfigCategory combat = category(builder, "combat");
        booleanToggle(combat, eb, "combat.critEnabled", MechSpecs.COMBAT_CRIT_ENABLED);
        booleanToggle(combat, eb, "combat.dodgeEnabled", MechSpecs.COMBAT_DODGE_ENABLED);
        doubleField(combat, eb, "combat.critChanceCap", MechSpecs.COMBAT_CRIT_CHANCE_CAP);
        doubleField(combat, eb, "combat.critDamageCap", MechSpecs.COMBAT_CRIT_DAMAGE_CAP);
        doubleField(combat, eb, "combat.dodgeChanceCap", MechSpecs.COMBAT_DODGE_CHANCE_CAP);

        ConfigCategory toggles = category(builder, "toggles");
        booleanToggle(toggles, eb, "toggles.decayZone", MechSpecs.DECAY_ZONE_ENABLED);
        booleanToggle(toggles, eb, "toggles.sunlightBurn", MechSpecs.SUNLIGHT_BURN_ENABLED);
        booleanToggle(toggles, eb, "toggles.overloadToggle", MechSpecs.OVERLOAD_ENABLED);
        // 场域屏障可见性：默认所有人可见（环境提示），可改为仅归属者与同队可见
        booleanToggle(toggles, eb, "toggles.wirelessFieldOwnerOnly", DecayFusionSpecs.WIRELESS_FIELD_OWNER_ONLY);
        // 理智系统总开关：关掉即整套停摆（规则/食补/HUD），调试指令只保留查询
        booleanToggle(toggles, eb, "toggles.sanityEnabled", MechSpecs.SANITY_ENABLED);

        // ===== 理智系统（三项手感时长；数值本身冻结，仅暴露"最影响体感"的三项）=====
        ConfigCategory sanity = category(builder, "sanity");
        intField(sanity, eb, "sanity.darkLightCooldownTicks", SanitySpecs.SANITY_DARK_LIGHT_COOLDOWN_TICKS);
        intField(sanity, eb, "sanity.foodRefreshTicks", SanitySpecs.SANITY_FOOD_REFRESH_TICKS);
        intField(sanity, eb, "sanity.netherRoofY", SanitySpecs.SANITY_NETHER_ROOF_Y);
        intField(sanity, eb, "sanity.naturalRegenPeriodTicks", SanitySpecs.SANITY_NATURAL_REGEN_PERIOD_TICKS);
        intField(sanity, eb, "sanity.flowerCountThreshold", SanitySpecs.SANITY_FLOWER_COUNT_THRESHOLD);
        intField(sanity, eb, "sanity.sleepDeprivationDays", SanitySpecs.SANITY_SLEEP_DEPRIVATION_DAYS);
        doubleField(sanity, eb, "sanity.sleepDeprivationDailyDebit", SanitySpecs.SANITY_SLEEP_DEPRIVATION_DAILY_DEBIT);

        // ===== 赤石饰品扩展槽 =====
        ConfigCategory curioSlots = category(builder, "curio_slots");
        booleanToggle(curioSlots, eb, "curio_slots.unlockRequired", SanitySpecs.CURIO_SLOT_UNLOCK_REQUIRED);
        intList(curioSlots, eb, "curio_slots.unlockThresholds", SanitySpecs.CURIO_SLOT_UNLOCK_THRESHOLDS);

        // ===== 禁断四件 · 1 生命之触 =====
        ConfigCategory curioLifeTouch = category(builder, "curio_life_touch");
        booleanToggle(curioLifeTouch, eb, "curio.lifeTouch.enabled", CurioSpecs.LIFE_TOUCH_ENABLED);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.reachBonus", CurioSpecs.LIFE_TOUCH_REACH_BONUS);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.doubleStrikeChance", CurioSpecs.LIFE_TOUCH_DOUBLE_STRIKE_CHANCE);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.selfHurtChance", CurioSpecs.LIFE_TOUCH_SELF_HURT_CHANCE);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.selfHurtDamage", CurioSpecs.LIFE_TOUCH_SELF_HURT_DAMAGE);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.hungerCostChance", CurioSpecs.LIFE_TOUCH_HUNGER_COST_CHANCE);
        intField(curioLifeTouch, eb, "curio.lifeTouch.hungerCostAmount", CurioSpecs.LIFE_TOUCH_HUNGER_COST_AMOUNT);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.hungerRestoreChance", CurioSpecs.LIFE_TOUCH_HUNGER_RESTORE_CHANCE);
        intField(curioLifeTouch, eb, "curio.lifeTouch.hungerRestoreAmount", CurioSpecs.LIFE_TOUCH_HUNGER_RESTORE_AMOUNT);
        intField(curioLifeTouch, eb, "curio.lifeTouch.hitCacheTicks", CurioSpecs.LIFE_TOUCH_HIT_CACHE_TICKS);

        // ===== 禁断四件 · 2 幼崽之心 =====
        ConfigCategory curioCubHeart = category(builder, "curio_cub_heart");
        booleanToggle(curioCubHeart, eb, "curio.cubHeart.enabled", CurioSpecs.CUB_HEART_ENABLED);
        doubleField(curioCubHeart, eb, "curio.cubHeart.absorptionRatio", CurioSpecs.CUB_HEART_ABSORPTION_RATIO);
        intField(curioCubHeart, eb, "curio.cubHeart.effectInterval", CurioSpecs.CUB_HEART_EFFECT_INTERVAL);
        doubleField(curioCubHeart, eb, "curio.cubHeart.slowHasteChance", CurioSpecs.CUB_HEART_SLOW_HASTE_CHANCE);
        doubleField(curioCubHeart, eb, "curio.cubHeart.slowHasteAmplitude", CurioSpecs.CUB_HEART_SLOW_HASTE_AMPLITUDE);
        intField(curioCubHeart, eb, "curio.cubHeart.slowHasteTicks", CurioSpecs.CUB_HEART_SLOW_HASTE_TICKS);
        doubleField(curioCubHeart, eb, "curio.cubHeart.damageShiftChance", CurioSpecs.CUB_HEART_DAMAGE_SHIFT_CHANCE);
        doubleField(curioCubHeart, eb, "curio.cubHeart.damageShiftAmount", CurioSpecs.CUB_HEART_DAMAGE_SHIFT_AMOUNT);
        intField(curioCubHeart, eb, "curio.cubHeart.damageShiftTicks", CurioSpecs.CUB_HEART_DAMAGE_SHIFT_TICKS);
        doubleField(curioCubHeart, eb, "curio.cubHeart.unnameableChance", CurioSpecs.CUB_HEART_UNNAMEABLE_CHANCE);
        intField(curioCubHeart, eb, "curio.cubHeart.unnameableAmplifier", CurioSpecs.CUB_HEART_UNNAMEABLE_AMPLIFIER);
        intField(curioCubHeart, eb, "curio.cubHeart.unnameableTicks", CurioSpecs.CUB_HEART_UNNAMEABLE_TICKS);
        doubleField(curioCubHeart, eb, "curio.cubHeart.excitementAttackSpeed", CurioSpecs.CUB_HEART_EXCITEMENT_ATTACK_SPEED);
        intField(curioCubHeart, eb, "curio.cubHeart.excitementTicks", CurioSpecs.CUB_HEART_EXCITEMENT_TICKS);

        // ===== 禁断四件 · 3 母神之印 =====
        ConfigCategory curioMotherSeal = category(builder, "curio_mother_seal");
        booleanToggle(curioMotherSeal, eb, "curio.motherSeal.enabled", CurioSpecs.MOTHER_SEAL_ENABLED);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.damageBonus", CurioSpecs.MOTHER_SEAL_DAMAGE_BONUS);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.healthBonus", CurioSpecs.MOTHER_SEAL_HEALTH_BONUS);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.speedBonus", CurioSpecs.MOTHER_SEAL_SPEED_BONUS);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.attackSpeedBonus", CurioSpecs.MOTHER_SEAL_ATTACK_SPEED_BONUS);
        intField(curioMotherSeal, eb, "curio.motherSeal.selfUnnameablePeriod", CurioSpecs.MOTHER_SEAL_SELF_UNNAMEABLE_PERIOD);
        intField(curioMotherSeal, eb, "curio.motherSeal.selfUnnameableTicks", CurioSpecs.MOTHER_SEAL_SELF_UNNAMEABLE_TICKS);
        intField(curioMotherSeal, eb, "curio.motherSeal.selfUnnameableAmplifier", CurioSpecs.MOTHER_SEAL_SELF_UNNAMEABLE_AMPLIFIER);
        intField(curioMotherSeal, eb, "curio.motherSeal.vegetarianHungerCost", CurioSpecs.MOTHER_SEAL_VEGETARIAN_HUNGER_COST);
        intField(curioMotherSeal, eb, "curio.motherSeal.vegetarianNauseaTicks", CurioSpecs.MOTHER_SEAL_VEGETARIAN_NAUSEA_TICKS);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.resistFactor", CurioSpecs.MOTHER_SEAL_RESIST_FACTOR);

        // ===== 禁断四件 · 4 孕育之环 =====
        ConfigCategory curioFertilityRing = category(builder, "curio_fertility_ring");
        booleanToggle(curioFertilityRing, eb, "curio.fertilityRing.enabled", CurioSpecs.FERTILITY_RING_ENABLED);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.healChance", CurioSpecs.FERTILITY_RING_HEAL_CHANCE);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.healAmount", CurioSpecs.FERTILITY_RING_HEAL_AMOUNT);
        intField(curioFertilityRing, eb, "curio.fertilityRing.healHungerCost", CurioSpecs.FERTILITY_RING_HEAL_HUNGER_COST);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.attackSpeedBonus", CurioSpecs.FERTILITY_RING_ATTACK_SPEED_BONUS);
        intField(curioFertilityRing, eb, "curio.fertilityRing.attackSpeedTicks", CurioSpecs.FERTILITY_RING_ATTACK_SPEED_TICKS);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.meatHealAmount", CurioSpecs.FERTILITY_RING_MEAT_HEAL_AMOUNT);
        booleanToggle(curioFertilityRing, eb, "curio.fertilityRing.satiatedMeatOnly", CurioSpecs.FERTILITY_RING_SATIATED_MEAT_ONLY);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.starveMultiplier", CurioSpecs.FERTILITY_RING_STARVE_MULTIPLIER);
        booleanToggle(curioFertilityRing, eb, "curio.fertilityRing.starveLethal", CurioSpecs.FERTILITY_RING_STARVE_LETHAL);

        // ===== 禁断四件 · 5 套装 =====
        ConfigCategory curioSet = category(builder, "curio_set");
        intField(curioSet, eb, "curio.set.attackCountRequired", CurioSpecs.SET_ATTACK_COUNT_REQUIRED);
        intField(curioSet, eb, "curio.set.attackCountHungerRestore", CurioSpecs.SET_ATTACK_COUNT_HUNGER_RESTORE);
        intField(curioSet, eb, "curio.set.unnameableLevelBonus", CurioSpecs.SET_UNNAMEABLE_LEVEL_BONUS);
        booleanToggle(curioSet, eb, "curio.set.suppressDistortion", CurioSpecs.SET_SUPPRESS_DISTORTION);
        doubleField(curioSet, eb, "curio.set.damageReduction", CurioSpecs.SET_DAMAGE_REDUCTION);
        doubleField(curioSet, eb, "curio.set.unnameableCritChance", CurioSpecs.SET_UNNAMEABLE_CRIT_CHANCE);
        doubleField(curioSet, eb, "curio.set.unnameableCritDamage", CurioSpecs.SET_UNNAMEABLE_CRIT_DAMAGE);
        doubleField(curioSet, eb, "curio.set.nearDeathHealPercent", CurioSpecs.SET_NEAR_DEATH_HEAL_PERCENT);
        intField(curioSet, eb, "curio.set.nearDeathUnnameableTicks", CurioSpecs.SET_NEAR_DEATH_UNNAMEABLE_TICKS);
        intField(curioSet, eb, "curio.set.nearDeathCooldownTicks", CurioSpecs.SET_NEAR_DEATH_COOLDOWN_TICKS);

        // ===== 禁断四件 · 6 侵蚀 =====
        ConfigCategory curioErosion = category(builder, "curio_erosion");
        booleanToggle(curioErosion, eb, "curio.erosion.enabled", CurioSpecs.EROSION_ENABLED);
        intField(curioErosion, eb, "curio.erosion.durationMinutes", CurioSpecs.EROSION_DURATION_MINUTES);
        intList(curioErosion, eb, "curio.erosion.noticeThresholds", CurioSpecs.EROSION_NOTICE_THRESHOLDS);
        intField(curioErosion, eb, "curio.erosion.noticeIntervalMinutes", CurioSpecs.EROSION_NOTICE_INTERVAL_MINUTES);
        doubleField(curioErosion, eb, "curio.erosion.statRerollPercent", CurioSpecs.EROSION_STAT_REROLL_PERCENT);
        intField(curioErosion, eb, "curio.erosion.nbtFlushTicks", CurioSpecs.EROSION_NBT_FLUSH_TICKS);
        booleanToggle(curioErosion, eb, "curio.erosion.screenFlashEnabled", CurioSpecs.EROSION_SCREEN_FLASH_ENABLED);

        // ===== 禁断四件 · 7 仪式与吸取 =====
        ConfigCategory curioAltar = category(builder, "curio_altar");
        booleanToggle(curioAltar, eb, "curio.altar.drainEnabled", CurioSpecs.ALTAR_DRAIN_ENABLED);
        intField(curioAltar, eb, "curio.altar.drainRadius", CurioSpecs.ALTAR_DRAIN_RADIUS);
        intField(curioAltar, eb, "curio.altar.drainIntervalTicks", CurioSpecs.ALTAR_DRAIN_INTERVAL_TICKS);
        doubleField(curioAltar, eb, "curio.altar.drainHealthPercent", CurioSpecs.ALTAR_DRAIN_HEALTH_PERCENT);
        longField(curioAltar, eb, "curio.altar.drainEnergyPerHp", CurioSpecs.ALTAR_DRAIN_ENERGY_PER_HP);
        intField(curioAltar, eb, "curio.altar.drainMaxTargets", CurioSpecs.ALTAR_DRAIN_MAX_TARGETS);
        longField(curioAltar, eb, "curio.altar.drainMaxEnergyPerTarget", CurioSpecs.ALTAR_DRAIN_MAX_ENERGY_PER_TARGET);
        booleanToggle(curioAltar, eb, "curio.altar.drainExemptCreative", CurioSpecs.ALTAR_DRAIN_EXEMPT_CREATIVE);
        intField(curioAltar, eb, "curio.altar.newRecipeTierRequired", CurioSpecs.ALTAR_NEW_RECIPE_TIER_REQUIRED);
        longField(curioAltar, eb, "curio.altar.newRecipeProgressMax", CurioSpecs.ALTAR_NEW_RECIPE_PROGRESS_MAX);
        longField(curioAltar, eb, "curio.altar.legacyProgressMax", CurioSpecs.ALTAR_LEGACY_PROGRESS_MAX);

        // ===== 生命研究机器 =====
        ConfigCategory lifeMachines = category(builder, "life_machines");
        longField(lifeMachines, eb, "life_machines.geneAnalyzerLifeCost", LifeMachineSpecs.GENE_ANALYZER_LIFE_COST);
        longField(lifeMachines, eb, "life_machines.geneAnalyzerLifeCapacity", LifeMachineSpecs.GENE_ANALYZER_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.geneAnalyzerProcessTicks", LifeMachineSpecs.GENE_ANALYZER_PROCESS_TICKS);
        doubleField(lifeMachines, eb, "life_machines.geneAnalyzerMinSuccessRate", LifeMachineSpecs.GENE_ANALYZER_MIN_SUCCESS);
        doubleField(lifeMachines, eb, "life_machines.geneAnalyzerMaxSuccessRate", LifeMachineSpecs.GENE_ANALYZER_MAX_SUCCESS);
        longField(lifeMachines, eb, "life_machines.lifeStructLifeCost", LifeMachineSpecs.LIFE_STRUCT_LIFE_COST);
        intField(lifeMachines, eb, "life_machines.lifeStructSolidCost", LifeMachineSpecs.LIFE_STRUCT_SOLID_COST);
        longField(lifeMachines, eb, "life_machines.lifeStructLifeCapacity", LifeMachineSpecs.LIFE_STRUCT_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.lifeStructProcessTicks", LifeMachineSpecs.LIFE_STRUCT_PROCESS_TICKS);
        longField(lifeMachines, eb, "life_machines.lifeBreederLifeCost", LifeMachineSpecs.LIFE_BREEDER_LIFE_COST);
        intField(lifeMachines, eb, "life_machines.lifeBreederCrystalCost", LifeMachineSpecs.LIFE_BREEDER_CRYSTAL_COST);
        longField(lifeMachines, eb, "life_machines.lifeBreederLifeCapacity", LifeMachineSpecs.LIFE_BREEDER_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.lifeBreederProcessTicks", LifeMachineSpecs.LIFE_BREEDER_PROCESS_TICKS);
        doubleField(lifeMachines, eb, "life_machines.lifeBreederMinSuccessRate", LifeMachineSpecs.LIFE_BREEDER_MIN_SUCCESS);
        doubleField(lifeMachines, eb, "life_machines.lifeBreederMaxSuccessRate", LifeMachineSpecs.LIFE_BREEDER_MAX_SUCCESS);
        longField(lifeMachines, eb, "life_machines.traitReforgerLifeCost", LifeMachineSpecs.TRAIT_REFORGER_LIFE_COST);
        longField(lifeMachines, eb, "life_machines.traitReforgerLifeCapacity", LifeMachineSpecs.TRAIT_REFORGER_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.traitReforgerProcessTicks", LifeMachineSpecs.TRAIT_REFORGER_PROCESS_TICKS);
        intField(lifeMachines, eb, "life_machines.traitReforgerCrystalPerRarity", LifeMachineSpecs.TRAIT_REFORGER_CRYSTAL_PER_RARITY);
        longField(lifeMachines, eb, "life_machines.transgeneFactoryLifeCost", LifeMachineSpecs.TRANSGENE_FACTORY_LIFE_COST);
        longField(lifeMachines, eb, "life_machines.transgeneFactoryLifeCapacity", LifeMachineSpecs.TRANSGENE_FACTORY_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.transgeneFactoryProcessTicks", LifeMachineSpecs.TRANSGENE_FACTORY_PROCESS_TICKS);
        intField(lifeMachines, eb, "life_machines.surgeryImplantSolidCost", LifeMachineSpecs.SURGERY_IMPLANT_SOLID_COST);
        longField(lifeMachines, eb, "life_machines.surgeryImplantLifeCost", LifeMachineSpecs.SURGERY_IMPLANT_LIFE_COST);
        intField(lifeMachines, eb, "life_machines.surgeryExtractSolidCost", LifeMachineSpecs.SURGERY_EXTRACT_SOLID_COST);
        longField(lifeMachines, eb, "life_machines.surgeryExtractLifeCost", LifeMachineSpecs.SURGERY_EXTRACT_LIFE_COST);
        longField(lifeMachines, eb, "life_machines.surgeryLifeCapacity", LifeMachineSpecs.SURGERY_LIFE_CAPACITY);
        intField(lifeMachines, eb, "life_machines.surgeryProcessTicks", LifeMachineSpecs.SURGERY_PROCESS_TICKS);
        longField(lifeMachines, eb, "life_machines.organVaultLifeCapacity", LifeMachineSpecs.ORGAN_VAULT_LIFE_CAPACITY);
        longField(lifeMachines, eb, "life_machines.organVaultKeepCostPerTick", LifeMachineSpecs.ORGAN_VAULT_KEEP_COST);
        longField(lifeMachines, eb, "life_machines.potionTableLifeCapacity", LifeMachineSpecs.POTION_TABLE_LIFE_CAPACITY);

        // ===== 能量机器 =====
        ConfigCategory energyMachines = category(builder, "energy_machines");
        longField(energyMachines, eb, "energy_machines.energyProcessorChishiRate", LifeMachineSpecs.ENERGY_PROCESSOR_CHISHI_RATE);
        longField(energyMachines, eb, "energy_machines.energyProcessorChishiCapacity", LifeMachineSpecs.ENERGY_PROCESSOR_CHISHI_CAPACITY);
        longField(energyMachines, eb, "energy_machines.energyProcessorTankCapacity", LifeMachineSpecs.ENERGY_PROCESSOR_TANK_CAPACITY);
        longField(energyMachines, eb, "energy_machines.energyProcessorChishiCost", LifeMachineSpecs.ENERGY_PROCESSOR_CHISHI_COST);
        longField(energyMachines, eb, "energy_machines.energyLiquefierChishiRate", LifeMachineSpecs.ENERGY_LIQUEFIER_CHISHI_RATE);
        longField(energyMachines, eb, "energy_machines.energyLiquefierChishiCapacity", LifeMachineSpecs.ENERGY_LIQUEFIER_CHISHI_CAPACITY);
        longField(energyMachines, eb, "energy_machines.energyLiquefierTankCapacity", LifeMachineSpecs.ENERGY_LIQUEFIER_TANK_CAPACITY);
        longField(energyMachines, eb, "energy_machines.fuelMixerChishiRate", LifeMachineSpecs.FUEL_MIXER_CHISHI_RATE);
        longField(energyMachines, eb, "energy_machines.fuelMixerChishiCapacity", LifeMachineSpecs.FUEL_MIXER_CHISHI_CAPACITY);
        longField(energyMachines, eb, "energy_machines.fuelMixerChishiCost", LifeMachineSpecs.FUEL_MIXER_CHISHI_COST);
        longField(energyMachines, eb, "energy_machines.fuelMixerTankCapacity", LifeMachineSpecs.FUEL_MIXER_TANK_CAPACITY);
        longField(energyMachines, eb, "energy_machines.fuelCannerTankCapacity", LifeMachineSpecs.FUEL_CANNER_TANK_CAPACITY);
        longField(energyMachines, eb, "energy_machines.fuelCannerFillRate", LifeMachineSpecs.FUEL_CANNER_FILL_RATE);
        longField(energyMachines, eb, "energy_machines.energyAggregatorEnergyPerIngot", LifeMachineSpecs.ENERGY_AGGREGATOR_PER_INGOT);
        longField(energyMachines, eb, "energy_machines.energyAggregatorEnergyPerGeodeUpgrade", LifeMachineSpecs.ENERGY_AGGREGATOR_PER_GEODE);
        longField(energyMachines, eb, "energy_machines.energyAggregatorEnergyCapacity", LifeMachineSpecs.ENERGY_AGGREGATOR_CAPACITY);
        intField(energyMachines, eb, "energy_machines.energyGeneratorGenerateRate", LifeMachineSpecs.ENERGY_GENERATOR_RATE);
        intField(energyMachines, eb, "energy_machines.energyAssemblyGenerateRate", LifeMachineSpecs.ENERGY_ASSEMBLY_RATE);
        intField(energyMachines, eb, "energy_machines.superGeneratorCoreGenerateRate", LifeMachineSpecs.SUPER_GENERATOR_CORE_RATE);
        longField(energyMachines, eb, "energy_machines.energyCellSerializerBaseCapacity", LifeMachineSpecs.ENERGY_CELL_BASE_CAPACITY);
        longField(energyMachines, eb, "energy_machines.upgradeStationEnergyPerUpgrade", LifeMachineSpecs.UPGRADE_STATION_PER_UPGRADE);
        longField(energyMachines, eb, "energy_machines.upgradeStationEnergyCapacity", LifeMachineSpecs.UPGRADE_STATION_CAPACITY);
        longField(energyMachines, eb, "energy_machines.equipmentForgerEnergyPerForge", LifeMachineSpecs.EQUIPMENT_FORGER_PER_FORGE);
        longField(energyMachines, eb, "energy_machines.equipmentForgerEnergyCapacity", LifeMachineSpecs.EQUIPMENT_FORGER_CAPACITY);

        // ===== 净化与矩阵 =====
        ConfigCategory purifierMatrix = category(builder, "purifier_matrix");
        intField(purifierMatrix, eb, "purifier_matrix.purifierEnergyPerTick", LifeMachineSpecs.PURIFIER_ENERGY_PER_TICK);
        intField(purifierMatrix, eb, "purifier_matrix.purifierBurnRate", LifeMachineSpecs.PURIFIER_BURN_RATE);
        longField(purifierMatrix, eb, "purifier_matrix.purifierTotalCost", LifeMachineSpecs.PURIFIER_TOTAL_COST);
        longField(purifierMatrix, eb, "purifier_matrix.purifierRateFormed", LifeMachineSpecs.PURIFIER_RATE_FORMED);
        longField(purifierMatrix, eb, "purifier_matrix.purifierMatrixTotalCost", LifeMachineSpecs.PURIFIER_MATRIX_TOTAL_COST);
        longField(purifierMatrix, eb, "purifier_matrix.purifierMatrixRateFormed", LifeMachineSpecs.PURIFIER_MATRIX_RATE_FORMED);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierChishiRate", LifeMachineSpecs.LIFE_PURIFIER_CHISHI_RATE);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierTotalCost", LifeMachineSpecs.LIFE_PURIFIER_TOTAL_COST);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierLifeCost", LifeMachineSpecs.LIFE_PURIFIER_LIFE_COST);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierChishiCapacity", LifeMachineSpecs.LIFE_PURIFIER_CHISHI_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifePurifierLifeCapacity", LifeMachineSpecs.LIFE_PURIFIER_LIFE_CAPACITY);
        intField(purifierMatrix, eb, "purifier_matrix.lifeMatrixConversionsPerTick", LifeMachineSpecs.LIFE_MATRIX_CONVERSIONS_PER_TICK);
        longField(purifierMatrix, eb, "purifier_matrix.lifeMatrixConversionCost", LifeMachineSpecs.LIFE_MATRIX_CONVERSION_COST);
        longField(purifierMatrix, eb, "purifier_matrix.lifeMatrixChishiCapacity", LifeMachineSpecs.LIFE_MATRIX_CHISHI_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifeMatrixLifeCapacity", LifeMachineSpecs.LIFE_MATRIX_LIFE_CAPACITY);
        intField(purifierMatrix, eb, "purifier_matrix.lifeConversionConversionsPerTick", LifeMachineSpecs.LIFE_CONVERSION_PER_TICK);
        longField(purifierMatrix, eb, "purifier_matrix.lifeConversionChishiCapacity", LifeMachineSpecs.LIFE_CONVERSION_CHISHI_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifeConversionLifeCapacity", LifeMachineSpecs.LIFE_CONVERSION_LIFE_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifeAggregationConversionCost", LifeMachineSpecs.LIFE_AGGREGATION_COST);
        longField(purifierMatrix, eb, "purifier_matrix.lifeAggregationConversionOutput", LifeMachineSpecs.LIFE_AGGREGATION_OUTPUT);
        longField(purifierMatrix, eb, "purifier_matrix.lifeAggregationChishiCapacity", LifeMachineSpecs.LIFE_AGGREGATION_CHISHI_CAPACITY);
        longField(purifierMatrix, eb, "purifier_matrix.lifeAggregationLifeCapacity", LifeMachineSpecs.LIFE_AGGREGATION_LIFE_CAPACITY);

        // ===== 端口与电池缓冲 =====
        ConfigCategory buffers = category(builder, "buffers");
        longField(buffers, eb, "buffers.lifeMatrixInputPortBufferCapacity", BufferValueSpecs.LIFE_MATRIX_INPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.lifeMatrixOutputPortBufferCapacity", BufferValueSpecs.LIFE_MATRIX_OUTPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.purifierEnergyInputPortBufferCapacity", BufferValueSpecs.PURIFIER_INPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.minerPortBufferCapacity", BufferValueSpecs.MINER_PORT_BUFFER);
        longField(buffers, eb, "buffers.minerEnergyInputBufferCapacity", BufferValueSpecs.MINER_ENERGY_INPUT_BUFFER);
        longField(buffers, eb, "buffers.wirelessInputPortBufferCapacity", BufferValueSpecs.WIRELESS_INPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.wirelessOutputPortBufferCapacity", BufferValueSpecs.WIRELESS_OUTPUT_PORT_BUFFER);
        longField(buffers, eb, "buffers.genEnergyOutputPortBufferCapacity", BufferValueSpecs.GEN_ENERGY_OUTPUT_BUFFER);
        longField(buffers, eb, "buffers.fusionEnergyOutputBufferCapacity", BufferValueSpecs.FUSION_ENERGY_OUTPUT_BUFFER);
        longField(buffers, eb, "buffers.reactorEnergyOutputBufferCapacity", BufferValueSpecs.REACTOR_ENERGY_OUTPUT_BUFFER);
        longField(buffers, eb, "buffers.lifeEnergyCellSerializerBaseCapacity", BufferValueSpecs.LIFE_ENERGY_CELL_SERIALIZER_CAPACITY);
        longField(buffers, eb, "buffers.plasmaTankCapacity", BufferValueSpecs.PLASMA_TANK_CAPACITY);
        longField(buffers, eb, "buffers.itemTerminalEnergyBuffer", BufferValueSpecs.ITEM_TERMINAL_ENERGY_BUFFER);
        longField(buffers, eb, "buffers.itemTerminalEnergyPortBufferCapacity", BufferValueSpecs.ITEM_TERMINAL_ENERGY_PORT_BUFFER);

        // ===== 培养机提纯与分馏机 =====
        ConfigCategory cultivatorFractionator = category(builder, "cultivator_fractionator");
        longField(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorLifeCapacity", BufferValueSpecs.CULTIVATOR_LIFE_CAPACITY);
        intList(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifySuccess", BufferValueSpecs.CULTIVATOR_PURIFY_SUCCESS);
        longList(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifyEnergy", BufferValueSpecs.CULTIVATOR_PURIFY_ENERGY);
        intList(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifySolid", BufferValueSpecs.CULTIVATOR_PURIFY_SOLID);
        intList(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifyTicks", BufferValueSpecs.CULTIVATOR_PURIFY_TICKS);
        intField(cultivatorFractionator, eb, "cultivator_fractionator.cultivatorPurifyGain", BufferValueSpecs.CULTIVATOR_PURIFY_GAIN);
        longField(cultivatorFractionator, eb, "cultivator_fractionator.fractionatorEnergyCapacity", BufferValueSpecs.FRACTIONATOR_ENERGY_CAPACITY);
        longField(cultivatorFractionator, eb, "cultivator_fractionator.fractionatorCostPerCraft", BufferValueSpecs.FRACTIONATOR_COST_PER_CRAFT);
        intField(cultivatorFractionator, eb, "cultivator_fractionator.fractionatorProcessTicks", BufferValueSpecs.FRACTIONATOR_PROCESS_TICKS);

        // ===== 价值分（统一存储库定价内核） =====
        ConfigCategory value = category(builder, "value");
        doubleField(value, eb, "value.costMultiplier", BufferValueSpecs.VALUE_COST_MULTIPLIER);
        doubleField(value, eb, "value.ingredientWeight", BufferValueSpecs.VALUE_INGREDIENT_WEIGHT);
        doubleField(value, eb, "value.magicBonus", BufferValueSpecs.VALUE_MAGIC_BONUS);
        doubleField(value, eb, "value.ingredientCap", BufferValueSpecs.VALUE_INGREDIENT_CAP);
        intField(value, eb, "value.iterations", BufferValueSpecs.VALUE_ITERATIONS);
        booleanToggle(value, eb, "value.lootEnabled", BufferValueSpecs.VALUE_LOOT_ENABLED);
        booleanToggle(value, eb, "value.lootAutoApply", BufferValueSpecs.VALUE_LOOT_AUTO_APPLY);
        doubleField(value, eb, "value.lootCap", BufferValueSpecs.VALUE_LOOT_CAP);
        booleanToggle(value, eb, "value.fluidEnabled", BufferValueSpecs.VALUE_FLUID_ENABLED);
        doubleField(value, eb, "value.fluidPerMbCap", BufferValueSpecs.VALUE_FLUID_PER_MB_CAP);
        stringList(value, eb, "value.fluidValues", BufferValueSpecs.VALUE_FLUID_VALUES);
        stringList(value, eb, "value.overrides", BufferValueSpecs.VALUE_OVERRIDES);
        stringList(value, eb, "value.tagValues", BufferValueSpecs.VALUE_TAG_VALUES);
        stringList(value, eb, "value.tierBonus", BufferValueSpecs.VALUE_TIER_BONUS);
        stringList(value, eb, "value.keywordExclusions", BufferValueSpecs.VALUE_KEYWORD_EXCLUSIONS);
        stringList(value, eb, "value.lootBlacklist", BufferValueSpecs.VALUE_LOOT_BLACKLIST);
        intField(value, eb, "value.unifiedVaultRows", BufferValueSpecs.UNIFIED_VAULT_ROWS);

        // ===== 反应堆 =====
        ConfigCategory reactor = category(builder, "reactor");
        longField(reactor, eb, "reactor.energyPerSlot", CoreMachineSpecs.REACTOR_ENERGY_PER_SLOT);
        intField(reactor, eb, "reactor.baseTemp", CoreMachineSpecs.REACTOR_BASE_TEMP);
        doubleField(reactor, eb, "reactor.passiveCool", CoreMachineSpecs.REACTOR_PASSIVE_COOL);
        doubleField(reactor, eb, "reactor.coolerCool", CoreMachineSpecs.REACTOR_COOLER_COOL);
        doubleField(reactor, eb, "reactor.drainBase", CoreMachineSpecs.REACTOR_DRAIN_BASE);
        doubleField(reactor, eb, "reactor.wasteRatio", CoreMachineSpecs.REACTOR_WASTE_RATIO);
        intField(reactor, eb, "reactor.wasteCapacity", CoreMachineSpecs.REACTOR_WASTE_CAPACITY);
        intField(reactor, eb, "reactor.tempMax", CoreMachineSpecs.REACTOR_TEMP_MAX);
        intField(reactor, eb, "reactor.tempOptMin", CoreMachineSpecs.REACTOR_TEMP_OPT_MIN);
        intField(reactor, eb, "reactor.tempOptMax", CoreMachineSpecs.REACTOR_TEMP_OPT_MAX);
        intField(reactor, eb, "reactor.tempWarn", CoreMachineSpecs.REACTOR_TEMP_WARN);
        intField(reactor, eb, "reactor.explosionDelayTicks", CoreMachineSpecs.REACTOR_EXPLOSION_DELAY_TICKS);

        // ===== 基础物流 =====
        ConfigCategory fluidPipe = category(builder, "fluid_pipe");
        intField(fluidPipe, eb, "fluid_pipe.rate", CoreMachineSpecs.FLUID_PIPE_RATE);

        ConfigCategory wastePort = category(builder, "waste_port");
        intField(wastePort, eb, "waste_port.bufferCapacity", CoreMachineSpecs.WASTE_PORT_BUFFER_CAPACITY);

        ConfigCategory exhaustedBarrel = category(builder, "exhausted_barrel");
        longField(exhaustedBarrel, eb, "exhausted_barrel.capacity", CoreMachineSpecs.EXHAUSTED_BARREL_CAPACITY);

        // ===== 生命活化器 =====
        ConfigCategory lifeActivator = category(builder, "life_activator");
        longField(lifeActivator, eb, "life_activator.lifeCapacity", CoreMachineSpecs.LIFE_ACTIVATOR_LIFE_CAPACITY);
        longField(lifeActivator, eb, "life_activator.costPerMb", CoreMachineSpecs.LIFE_ACTIVATOR_COST_PER_MB);
        longField(lifeActivator, eb, "life_activator.inputCapacity", CoreMachineSpecs.LIFE_ACTIVATOR_INPUT_CAPACITY);
        longField(lifeActivator, eb, "life_activator.outputCapacity", CoreMachineSpecs.LIFE_ACTIVATOR_OUTPUT_CAPACITY);
        longField(lifeActivator, eb, "life_activator.convertRate", CoreMachineSpecs.LIFE_ACTIVATOR_CONVERT_RATE);

        // ===== 生命离心机 =====
        ConfigCategory lifeCentrifuge = category(builder, "life_centrifuge");
        longField(lifeCentrifuge, eb, "life_centrifuge.energyCapacity", CoreMachineSpecs.LIFE_CENTRIFUGE_ENERGY_CAPACITY);
        longField(lifeCentrifuge, eb, "life_centrifuge.inputCapacity", CoreMachineSpecs.LIFE_CENTRIFUGE_INPUT_CAPACITY);
        longField(lifeCentrifuge, eb, "life_centrifuge.convertRate", CoreMachineSpecs.LIFE_CENTRIFUGE_CONVERT_RATE);
        longField(lifeCentrifuge, eb, "life_centrifuge.costPerMb", CoreMachineSpecs.LIFE_CENTRIFUGE_COST_PER_MB);

        // ===== 物品重构器 =====
        ConfigCategory itemReconstructor = category(builder, "item_reconstructor");
        longField(itemReconstructor, eb, "item_reconstructor.energyCapacity", CoreMachineSpecs.RECONSTRUCTOR_ENERGY_CAPACITY);
        longField(itemReconstructor, eb, "item_reconstructor.costPerCrystal", CoreMachineSpecs.RECONSTRUCTOR_COST_PER_CRYSTAL);

        // ===== 聚变燃料聚合器 =====
        ConfigCategory fusionFuelAggregator = category(builder, "fusion_fuel_aggregator");
        longField(fusionFuelAggregator, eb, "fusion_fuel_aggregator.energyCapacity", CoreMachineSpecs.AGGREGATOR_ENERGY_CAPACITY);
        longField(fusionFuelAggregator, eb, "fusion_fuel_aggregator.costPerCraft", CoreMachineSpecs.AGGREGATOR_COST_PER_CRAFT);
        intField(fusionFuelAggregator, eb, "fusion_fuel_aggregator.processTicks", CoreMachineSpecs.AGGREGATOR_PROCESS_TICKS);
        longField(fusionFuelAggregator, eb, "fusion_fuel_aggregator.plasmaCapacity", CoreMachineSpecs.AGGREGATOR_PLASMA_CAPACITY);
        longField(fusionFuelAggregator, eb, "fusion_fuel_aggregator.producePerCraft", CoreMachineSpecs.AGGREGATOR_PRODUCE_PER_CRAFT);

        // ===== 离子体填装器 =====
        ConfigCategory plasmaFiller = category(builder, "plasma_filler");
        longField(plasmaFiller, eb, "plasma_filler.plasmaCapacity", CoreMachineSpecs.FILLER_PLASMA_CAPACITY);
        longField(plasmaFiller, eb, "plasma_filler.plasmaPerRod", CoreMachineSpecs.FILLER_PLASMA_PER_ROD);
        intField(plasmaFiller, eb, "plasma_filler.processTicks", CoreMachineSpecs.FILLER_PROCESS_TICKS);

        // ===== 赤石植物培养机 =====
        ConfigCategory plantCultivator = category(builder, "plant_cultivator");
        longField(plantCultivator, eb, "plant_cultivator.energyCapacity", CoreMachineSpecs.PLANT_CULTIVATOR_ENERGY_CAPACITY);
        intField(plantCultivator, eb, "plant_cultivator.ticks", CoreMachineSpecs.PLANT_CULTIVATOR_TICKS);
        longField(plantCultivator, eb, "plant_cultivator.costPerTick", CoreMachineSpecs.PLANT_CULTIVATOR_COST_PER_TICK);

        // ===== 赤石压缩机 =====
        ConfigCategory compressor = category(builder, "compressor");
        longField(compressor, eb, "compressor.energyCapacity", CoreMachineSpecs.COMPRESSOR_ENERGY_CAPACITY);
        intField(compressor, eb, "compressor.ticks", CoreMachineSpecs.COMPRESSOR_TICKS);
        longField(compressor, eb, "compressor.costPerTick", CoreMachineSpecs.COMPRESSOR_COST_PER_TICK);

        // ===== 赤石打粉机 =====
        ConfigCategory pulverizer = category(builder, "pulverizer");
        longField(pulverizer, eb, "pulverizer.energyCapacity", CoreMachineSpecs.PULVERIZER_ENERGY_CAPACITY);
        intField(pulverizer, eb, "pulverizer.ticks", CoreMachineSpecs.PULVERIZER_TICKS);
        longField(pulverizer, eb, "pulverizer.costPerTick", CoreMachineSpecs.PULVERIZER_COST_PER_TICK);

        // ===== 赤石变化器 =====
        ConfigCategory transformer = category(builder, "transformer");
        longField(transformer, eb, "transformer.energyCapacity", CoreMachineSpecs.TRANSFORMER_ENERGY_CAPACITY);
        intField(transformer, eb, "transformer.ticks", CoreMachineSpecs.TRANSFORMER_TICKS);
        longField(transformer, eb, "transformer.costPerTick", CoreMachineSpecs.TRANSFORMER_COST_PER_TICK);

        // ===== 赤石矿机 =====
        ConfigCategory miner = category(builder, "miner");
        intField(miner, eb, "miner.ticksBase", CoreMachineSpecs.MINER_TICKS_BASE);
        longField(miner, eb, "miner.costPerTickBase", CoreMachineSpecs.MINER_COST_PER_TICK_BASE);
        intField(miner, eb, "miner.preciseFortuneDivisor", CoreMachineSpecs.MINER_PRECISE_FORTUNE_DIVISOR);
        intField(miner, eb, "miner.extraOreWeight", CoreMachineSpecs.MINER_EXTRA_ORE_WEIGHT);

        // ===== 衰竭区域 =====
        ConfigCategory decayZone = category(builder, "decay_zone");
        longField(decayZone, eb, "decay_zone.durationTicks", DecayFusionSpecs.DECAY_ZONE_DURATION_TICKS);
        intField(decayZone, eb, "decay_zone.samplesPerTick", DecayFusionSpecs.DECAY_ZONE_SAMPLES_PER_TICK);

        // ===== 衰变净化塔 =====
        ConfigCategory decayPurifier = category(builder, "decay_purifier");
        longField(decayPurifier, eb, "decay_purifier.energyCapacity", DecayFusionSpecs.DECAY_PURIFIER_ENERGY_CAPACITY);
        intField(decayPurifier, eb, "decay_purifier.range", DecayFusionSpecs.DECAY_PURIFIER_RANGE);
        longField(decayPurifier, eb, "decay_purifier.costPerTick", DecayFusionSpecs.DECAY_PURIFIER_COST_PER_TICK);
        longField(decayPurifier, eb, "decay_purifier.ticksPerTick", DecayFusionSpecs.DECAY_PURIFIER_TICKS_PER_TICK);

        // ===== 聚变堆 =====
        ConfigCategory fusionReactor = category(builder, "fusion_reactor");
        doubleField(fusionReactor, eb, "fusion_reactor.efficiencyGrowth", DecayFusionSpecs.FUSION_EFFICIENCY_GROWTH);
        doubleField(fusionReactor, eb, "fusion_reactor.coolerFrameBonus", DecayFusionSpecs.FUSION_COOLER_FRAME_BONUS);
        longField(fusionReactor, eb, "fusion_reactor.coolingPerPercent", DecayFusionSpecs.FUSION_COOLING_PER_PERCENT);
        intField(fusionReactor, eb, "fusion_reactor.baseTemp", DecayFusionSpecs.FUSION_BASE_TEMP);
        intField(fusionReactor, eb, "fusion_reactor.tempMax", DecayFusionSpecs.FUSION_TEMP_MAX);
        intField(fusionReactor, eb, "fusion_reactor.tempTrip", DecayFusionSpecs.FUSION_TEMP_TRIP);
        intField(fusionReactor, eb, "fusion_reactor.tempOptMin", DecayFusionSpecs.FUSION_TEMP_OPT_MIN);
        intField(fusionReactor, eb, "fusion_reactor.tempOptMax", DecayFusionSpecs.FUSION_TEMP_OPT_MAX);
        intField(fusionReactor, eb, "fusion_reactor.tempResume", DecayFusionSpecs.FUSION_TEMP_RESUME);
        intField(fusionReactor, eb, "fusion_reactor.tempStep", DecayFusionSpecs.FUSION_TEMP_STEP);
        intField(fusionReactor, eb, "fusion_reactor.coolerDurabilityInterval", DecayFusionSpecs.FUSION_COOLER_DURABILITY_INTERVAL);
        longField(fusionReactor, eb, "fusion_reactor.ashPerEnergy", DecayFusionSpecs.FUSION_ASH_PER_ENERGY);
        longField(fusionReactor, eb, "fusion_reactor.rodEnergy", DecayFusionSpecs.FUSION_ROD_ENERGY);

        // ===== 无线赤能源（损失模型；场域屏障可见性开关见「机制开关」）=====
        ConfigCategory wireless = category(builder, "wireless");
        doubleField(wireless, eb, "wireless.baseLoss", DecayFusionSpecs.WIRELESS_BASE_LOSS);
        doubleField(wireless, eb, "wireless.lossPerBlock", DecayFusionSpecs.WIRELESS_LOSS_PER_BLOCK);
        doubleField(wireless, eb, "wireless.maxLoss", DecayFusionSpecs.WIRELESS_MAX_LOSS);
        doubleField(wireless, eb, "wireless.crossDimLoss", DecayFusionSpecs.WIRELESS_CROSS_DIM_LOSS);
        doubleField(wireless, eb, "wireless.lossReductionPerModule", DecayFusionSpecs.WIRELESS_LOSS_REDUCTION_PER_MODULE);

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
        // TOML 中的纯整数会被 NightConfig 读回 Integer，不能按 Long 强转，否则界面构建抛 ClassCastException
        List<Long> current = new ArrayList<>();
        for (Object v : spec.get()) {
            if (v instanceof Number n) {
                current.add(n.longValue());
            }
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
        // 同上：按 Number 取值，兼容手改配置写入的 Long/Integer 混用
        List<Integer> current = new ArrayList<>();
        for (Object v : spec.get()) {
            if (v instanceof Number n) {
                current.add(n.intValue());
            }
        }
        cat.addEntry(eb.startIntList(Component.translatable("config.akaishi." + key), current)
                .setSaveConsumer(v -> spec.set(new ArrayList<>(v)))
                .build());
    }

    private static void doubleList(ConfigCategory cat, ConfigEntryBuilder eb, String key,
                                   ForgeConfigSpec.ConfigValue<List<? extends Double>> spec) {
        // 同上：手改配置写 [0, 0] 时读回为 Integer，按 Number 取值避免强转失败
        List<Double> current = new ArrayList<>();
        for (Object v : spec.get()) {
            if (v instanceof Number n) {
                current.add(n.doubleValue());
            }
        }
        cat.addEntry(eb.startDoubleList(Component.translatable("config.akaishi." + key), current)
                .setSaveConsumer(v -> spec.set(new ArrayList<>(v)))
                .build());
    }

    private static void stringList(ConfigCategory cat, ConfigEntryBuilder eb, String key,
                                   ForgeConfigSpec.ConfigValue<List<? extends String>> spec) {
        List<String> current = new ArrayList<>();
        for (Object v : spec.get()) {
            if (v != null) {
                current.add(v.toString());
            }
        }
        cat.addEntry(eb.startStrList(Component.translatable("config.akaishi." + key), current)
                .setSaveConsumer(v -> spec.set(new ArrayList<>(v)))
                .build());
    }
}
