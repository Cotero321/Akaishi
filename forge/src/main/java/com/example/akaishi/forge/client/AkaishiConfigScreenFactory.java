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

        // ===== 机械改造机器 =====
        ConfigCategory mechMachines = category(builder, "mechanical_machines");
        longField(mechMachines, eb, "mechanical_machines.chishiCapacity", AkaishiConfig.MECH_CHISHI_CAPACITY);
        longField(mechMachines, eb, "mechanical_machines.lifeCapacity", AkaishiConfig.MECH_LIFE_CAPACITY);
        longField(mechMachines, eb, "mechanical_machines.templateChishiCost", AkaishiConfig.MECH_TEMPLATE_CHISHI_COST);
        longField(mechMachines, eb, "mechanical_machines.templateLifeCost", AkaishiConfig.MECH_TEMPLATE_LIFE_COST);
        intField(mechMachines, eb, "mechanical_machines.templateTicks", AkaishiConfig.MECH_TEMPLATE_TICKS);
        longList(mechMachines, eb, "mechanical_machines.processChishiBase", AkaishiConfig.MECH_PROCESS_CHISHI_BASE);
        longList(mechMachines, eb, "mechanical_machines.processLifeBase", AkaishiConfig.MECH_PROCESS_LIFE_BASE);
        intList(mechMachines, eb, "mechanical_machines.processTicksBase", AkaishiConfig.MECH_PROCESS_TICKS_BASE);
        intList(mechMachines, eb, "mechanical_machines.processPartFactor", AkaishiConfig.MECH_PROCESS_PART_FACTOR);
        intList(mechMachines, eb, "mechanical_machines.processMaterialCount", AkaishiConfig.MECH_PROCESS_MATERIAL_COUNT);
        longField(mechMachines, eb, "mechanical_machines.assemblyChishiCost", AkaishiConfig.MECH_ASSEMBLY_CHISHI_COST);
        longField(mechMachines, eb, "mechanical_machines.assemblyLifeCost", AkaishiConfig.MECH_ASSEMBLY_LIFE_COST);
        intField(mechMachines, eb, "mechanical_machines.assemblyTicks", AkaishiConfig.MECH_ASSEMBLY_TICKS);

        // ===== 机械义体属性换算 =====
        ConfigCategory mechBody = category(builder, "mechanical_body");
        doubleField(mechBody, eb, "mechanical_body.healthScale", AkaishiConfig.MECH_BODY_HEALTH_SCALE);
        doubleField(mechBody, eb, "mechanical_body.attackScale", AkaishiConfig.MECH_BODY_ATTACK_SCALE);
        doubleField(mechBody, eb, "mechanical_body.attackSpeedScale", AkaishiConfig.MECH_BODY_ATTACK_SPEED_SCALE);
        doubleField(mechBody, eb, "mechanical_body.movementSpeedScale", AkaishiConfig.MECH_BODY_MOVEMENT_SPEED_SCALE);
        doubleField(mechBody, eb, "mechanical_body.armorScale", AkaishiConfig.MECH_BODY_ARMOR_SCALE);
        doubleField(mechBody, eb, "mechanical_body.critChanceScale", AkaishiConfig.MECH_BODY_CRIT_CHANCE_SCALE);
        doubleField(mechBody, eb, "mechanical_body.critDamageScale", AkaishiConfig.MECH_BODY_CRIT_DAMAGE_SCALE);
        doubleField(mechBody, eb, "mechanical_body.rangeScale", AkaishiConfig.MECH_BODY_RANGE_SCALE);
        doubleField(mechBody, eb, "mechanical_body.dodgeScale", AkaishiConfig.MECH_BODY_DODGE_SCALE);

        // ===== 基因属性权重 =====
        ConfigCategory geneWeight = category(builder, "gene_weight");
        doubleField(geneWeight, eb, "gene_weight.strength", AkaishiConfig.GENE_WEIGHT_STRENGTH);

        // ===== 底层战斗（暴击/闪避）=====
        ConfigCategory combat = category(builder, "combat");
        booleanToggle(combat, eb, "combat.critEnabled", AkaishiConfig.COMBAT_CRIT_ENABLED);
        booleanToggle(combat, eb, "combat.dodgeEnabled", AkaishiConfig.COMBAT_DODGE_ENABLED);
        doubleField(combat, eb, "combat.critChanceCap", AkaishiConfig.COMBAT_CRIT_CHANCE_CAP);
        doubleField(combat, eb, "combat.critDamageCap", AkaishiConfig.COMBAT_CRIT_DAMAGE_CAP);
        doubleField(combat, eb, "combat.dodgeChanceCap", AkaishiConfig.COMBAT_DODGE_CHANCE_CAP);

        ConfigCategory toggles = category(builder, "toggles");
        booleanToggle(toggles, eb, "toggles.decayZone", AkaishiConfig.DECAY_ZONE_ENABLED);
        booleanToggle(toggles, eb, "toggles.sunlightBurn", AkaishiConfig.SUNLIGHT_BURN_ENABLED);
        booleanToggle(toggles, eb, "toggles.overloadToggle", AkaishiConfig.OVERLOAD_ENABLED);
        // 场域屏障可见性：默认所有人可见（环境提示），可改为仅归属者与同队可见
        booleanToggle(toggles, eb, "toggles.wirelessFieldOwnerOnly", AkaishiConfig.WIRELESS_FIELD_OWNER_ONLY);

        // ===== 赤石饰品扩展槽 =====
        ConfigCategory curioSlots = category(builder, "curio_slots");
        booleanToggle(curioSlots, eb, "curio_slots.unlockRequired", AkaishiConfig.CURIO_SLOT_UNLOCK_REQUIRED);
        intList(curioSlots, eb, "curio_slots.unlockThresholds", AkaishiConfig.CURIO_SLOT_UNLOCK_THRESHOLDS);

        // ===== 禁断四件 · 1 生命之触 =====
        ConfigCategory curioLifeTouch = category(builder, "curio_life_touch");
        booleanToggle(curioLifeTouch, eb, "curio.lifeTouch.enabled", AkaishiConfig.LIFE_TOUCH_ENABLED);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.reachBonus", AkaishiConfig.LIFE_TOUCH_REACH_BONUS);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.doubleStrikeChance", AkaishiConfig.LIFE_TOUCH_DOUBLE_STRIKE_CHANCE);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.selfHurtChance", AkaishiConfig.LIFE_TOUCH_SELF_HURT_CHANCE);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.selfHurtDamage", AkaishiConfig.LIFE_TOUCH_SELF_HURT_DAMAGE);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.hungerCostChance", AkaishiConfig.LIFE_TOUCH_HUNGER_COST_CHANCE);
        intField(curioLifeTouch, eb, "curio.lifeTouch.hungerCostAmount", AkaishiConfig.LIFE_TOUCH_HUNGER_COST_AMOUNT);
        doubleField(curioLifeTouch, eb, "curio.lifeTouch.hungerRestoreChance", AkaishiConfig.LIFE_TOUCH_HUNGER_RESTORE_CHANCE);
        intField(curioLifeTouch, eb, "curio.lifeTouch.hungerRestoreAmount", AkaishiConfig.LIFE_TOUCH_HUNGER_RESTORE_AMOUNT);
        intField(curioLifeTouch, eb, "curio.lifeTouch.hitCacheTicks", AkaishiConfig.LIFE_TOUCH_HIT_CACHE_TICKS);

        // ===== 禁断四件 · 2 幼崽之心 =====
        ConfigCategory curioCubHeart = category(builder, "curio_cub_heart");
        booleanToggle(curioCubHeart, eb, "curio.cubHeart.enabled", AkaishiConfig.CUB_HEART_ENABLED);
        doubleField(curioCubHeart, eb, "curio.cubHeart.absorptionRatio", AkaishiConfig.CUB_HEART_ABSORPTION_RATIO);
        intField(curioCubHeart, eb, "curio.cubHeart.effectInterval", AkaishiConfig.CUB_HEART_EFFECT_INTERVAL);
        doubleField(curioCubHeart, eb, "curio.cubHeart.slowHasteChance", AkaishiConfig.CUB_HEART_SLOW_HASTE_CHANCE);
        doubleField(curioCubHeart, eb, "curio.cubHeart.slowHasteAmplitude", AkaishiConfig.CUB_HEART_SLOW_HASTE_AMPLITUDE);
        intField(curioCubHeart, eb, "curio.cubHeart.slowHasteTicks", AkaishiConfig.CUB_HEART_SLOW_HASTE_TICKS);
        doubleField(curioCubHeart, eb, "curio.cubHeart.damageShiftChance", AkaishiConfig.CUB_HEART_DAMAGE_SHIFT_CHANCE);
        doubleField(curioCubHeart, eb, "curio.cubHeart.damageShiftAmount", AkaishiConfig.CUB_HEART_DAMAGE_SHIFT_AMOUNT);
        intField(curioCubHeart, eb, "curio.cubHeart.damageShiftTicks", AkaishiConfig.CUB_HEART_DAMAGE_SHIFT_TICKS);
        doubleField(curioCubHeart, eb, "curio.cubHeart.unnameableChance", AkaishiConfig.CUB_HEART_UNNAMEABLE_CHANCE);
        intField(curioCubHeart, eb, "curio.cubHeart.unnameableAmplifier", AkaishiConfig.CUB_HEART_UNNAMEABLE_AMPLIFIER);
        intField(curioCubHeart, eb, "curio.cubHeart.unnameableTicks", AkaishiConfig.CUB_HEART_UNNAMEABLE_TICKS);
        doubleField(curioCubHeart, eb, "curio.cubHeart.excitementAttackSpeed", AkaishiConfig.CUB_HEART_EXCITEMENT_ATTACK_SPEED);
        intField(curioCubHeart, eb, "curio.cubHeart.excitementTicks", AkaishiConfig.CUB_HEART_EXCITEMENT_TICKS);

        // ===== 禁断四件 · 3 母神之印 =====
        ConfigCategory curioMotherSeal = category(builder, "curio_mother_seal");
        booleanToggle(curioMotherSeal, eb, "curio.motherSeal.enabled", AkaishiConfig.MOTHER_SEAL_ENABLED);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.damageBonus", AkaishiConfig.MOTHER_SEAL_DAMAGE_BONUS);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.healthBonus", AkaishiConfig.MOTHER_SEAL_HEALTH_BONUS);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.speedBonus", AkaishiConfig.MOTHER_SEAL_SPEED_BONUS);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.attackSpeedBonus", AkaishiConfig.MOTHER_SEAL_ATTACK_SPEED_BONUS);
        intField(curioMotherSeal, eb, "curio.motherSeal.selfUnnameablePeriod", AkaishiConfig.MOTHER_SEAL_SELF_UNNAMEABLE_PERIOD);
        intField(curioMotherSeal, eb, "curio.motherSeal.selfUnnameableTicks", AkaishiConfig.MOTHER_SEAL_SELF_UNNAMEABLE_TICKS);
        intField(curioMotherSeal, eb, "curio.motherSeal.selfUnnameableAmplifier", AkaishiConfig.MOTHER_SEAL_SELF_UNNAMEABLE_AMPLIFIER);
        intField(curioMotherSeal, eb, "curio.motherSeal.vegetarianHungerCost", AkaishiConfig.MOTHER_SEAL_VEGETARIAN_HUNGER_COST);
        intField(curioMotherSeal, eb, "curio.motherSeal.vegetarianNauseaTicks", AkaishiConfig.MOTHER_SEAL_VEGETARIAN_NAUSEA_TICKS);
        doubleField(curioMotherSeal, eb, "curio.motherSeal.resistFactor", AkaishiConfig.MOTHER_SEAL_RESIST_FACTOR);

        // ===== 禁断四件 · 4 孕育之环 =====
        ConfigCategory curioFertilityRing = category(builder, "curio_fertility_ring");
        booleanToggle(curioFertilityRing, eb, "curio.fertilityRing.enabled", AkaishiConfig.FERTILITY_RING_ENABLED);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.healChance", AkaishiConfig.FERTILITY_RING_HEAL_CHANCE);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.healAmount", AkaishiConfig.FERTILITY_RING_HEAL_AMOUNT);
        intField(curioFertilityRing, eb, "curio.fertilityRing.healHungerCost", AkaishiConfig.FERTILITY_RING_HEAL_HUNGER_COST);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.attackSpeedBonus", AkaishiConfig.FERTILITY_RING_ATTACK_SPEED_BONUS);
        intField(curioFertilityRing, eb, "curio.fertilityRing.attackSpeedTicks", AkaishiConfig.FERTILITY_RING_ATTACK_SPEED_TICKS);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.meatHealAmount", AkaishiConfig.FERTILITY_RING_MEAT_HEAL_AMOUNT);
        booleanToggle(curioFertilityRing, eb, "curio.fertilityRing.satiatedMeatOnly", AkaishiConfig.FERTILITY_RING_SATIATED_MEAT_ONLY);
        doubleField(curioFertilityRing, eb, "curio.fertilityRing.starveMultiplier", AkaishiConfig.FERTILITY_RING_STARVE_MULTIPLIER);
        booleanToggle(curioFertilityRing, eb, "curio.fertilityRing.starveLethal", AkaishiConfig.FERTILITY_RING_STARVE_LETHAL);

        // ===== 禁断四件 · 5 套装 =====
        ConfigCategory curioSet = category(builder, "curio_set");
        intField(curioSet, eb, "curio.set.attackCountRequired", AkaishiConfig.SET_ATTACK_COUNT_REQUIRED);
        intField(curioSet, eb, "curio.set.attackCountHungerRestore", AkaishiConfig.SET_ATTACK_COUNT_HUNGER_RESTORE);
        intField(curioSet, eb, "curio.set.unnameableLevelBonus", AkaishiConfig.SET_UNNAMEABLE_LEVEL_BONUS);
        booleanToggle(curioSet, eb, "curio.set.suppressDistortion", AkaishiConfig.SET_SUPPRESS_DISTORTION);
        doubleField(curioSet, eb, "curio.set.damageReduction", AkaishiConfig.SET_DAMAGE_REDUCTION);
        doubleField(curioSet, eb, "curio.set.unnameableCritChance", AkaishiConfig.SET_UNNAMEABLE_CRIT_CHANCE);
        doubleField(curioSet, eb, "curio.set.unnameableCritDamage", AkaishiConfig.SET_UNNAMEABLE_CRIT_DAMAGE);
        doubleField(curioSet, eb, "curio.set.nearDeathHealPercent", AkaishiConfig.SET_NEAR_DEATH_HEAL_PERCENT);
        intField(curioSet, eb, "curio.set.nearDeathUnnameableTicks", AkaishiConfig.SET_NEAR_DEATH_UNNAMEABLE_TICKS);
        intField(curioSet, eb, "curio.set.nearDeathCooldownTicks", AkaishiConfig.SET_NEAR_DEATH_COOLDOWN_TICKS);

        // ===== 禁断四件 · 6 侵蚀 =====
        ConfigCategory curioErosion = category(builder, "curio_erosion");
        booleanToggle(curioErosion, eb, "curio.erosion.enabled", AkaishiConfig.EROSION_ENABLED);
        intField(curioErosion, eb, "curio.erosion.durationMinutes", AkaishiConfig.EROSION_DURATION_MINUTES);
        intList(curioErosion, eb, "curio.erosion.noticeThresholds", AkaishiConfig.EROSION_NOTICE_THRESHOLDS);
        intField(curioErosion, eb, "curio.erosion.noticeIntervalMinutes", AkaishiConfig.EROSION_NOTICE_INTERVAL_MINUTES);
        doubleField(curioErosion, eb, "curio.erosion.statRerollPercent", AkaishiConfig.EROSION_STAT_REROLL_PERCENT);
        intField(curioErosion, eb, "curio.erosion.nbtFlushTicks", AkaishiConfig.EROSION_NBT_FLUSH_TICKS);
        booleanToggle(curioErosion, eb, "curio.erosion.screenFlashEnabled", AkaishiConfig.EROSION_SCREEN_FLASH_ENABLED);

        // ===== 禁断四件 · 7 仪式与吸取 =====
        ConfigCategory curioAltar = category(builder, "curio_altar");
        booleanToggle(curioAltar, eb, "curio.altar.drainEnabled", AkaishiConfig.ALTAR_DRAIN_ENABLED);
        intField(curioAltar, eb, "curio.altar.drainRadius", AkaishiConfig.ALTAR_DRAIN_RADIUS);
        intField(curioAltar, eb, "curio.altar.drainIntervalTicks", AkaishiConfig.ALTAR_DRAIN_INTERVAL_TICKS);
        doubleField(curioAltar, eb, "curio.altar.drainHealthPercent", AkaishiConfig.ALTAR_DRAIN_HEALTH_PERCENT);
        longField(curioAltar, eb, "curio.altar.drainEnergyPerHp", AkaishiConfig.ALTAR_DRAIN_ENERGY_PER_HP);
        intField(curioAltar, eb, "curio.altar.drainMaxTargets", AkaishiConfig.ALTAR_DRAIN_MAX_TARGETS);
        longField(curioAltar, eb, "curio.altar.drainMaxEnergyPerTarget", AkaishiConfig.ALTAR_DRAIN_MAX_ENERGY_PER_TARGET);
        booleanToggle(curioAltar, eb, "curio.altar.drainExemptCreative", AkaishiConfig.ALTAR_DRAIN_EXEMPT_CREATIVE);
        intField(curioAltar, eb, "curio.altar.newRecipeTierRequired", AkaishiConfig.ALTAR_NEW_RECIPE_TIER_REQUIRED);
        longField(curioAltar, eb, "curio.altar.newRecipeProgressMax", AkaishiConfig.ALTAR_NEW_RECIPE_PROGRESS_MAX);
        longField(curioAltar, eb, "curio.altar.legacyProgressMax", AkaishiConfig.ALTAR_LEGACY_PROGRESS_MAX);

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
        longField(buffers, eb, "buffers.lifeEnergyCellSerializerBaseCapacity", AkaishiConfig.LIFE_ENERGY_CELL_SERIALIZER_CAPACITY);
        longField(buffers, eb, "buffers.plasmaTankCapacity", AkaishiConfig.PLASMA_TANK_CAPACITY);
        longField(buffers, eb, "buffers.itemTerminalEnergyBuffer", AkaishiConfig.ITEM_TERMINAL_ENERGY_BUFFER);
        longField(buffers, eb, "buffers.itemTerminalEnergyPortBufferCapacity", AkaishiConfig.ITEM_TERMINAL_ENERGY_PORT_BUFFER);

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

        // ===== 价值分（统一存储库定价内核） =====
        ConfigCategory value = category(builder, "value");
        doubleField(value, eb, "value.costMultiplier", AkaishiConfig.VALUE_COST_MULTIPLIER);
        doubleField(value, eb, "value.ingredientWeight", AkaishiConfig.VALUE_INGREDIENT_WEIGHT);
        doubleField(value, eb, "value.magicBonus", AkaishiConfig.VALUE_MAGIC_BONUS);
        doubleField(value, eb, "value.ingredientCap", AkaishiConfig.VALUE_INGREDIENT_CAP);
        intField(value, eb, "value.iterations", AkaishiConfig.VALUE_ITERATIONS);
        booleanToggle(value, eb, "value.lootEnabled", AkaishiConfig.VALUE_LOOT_ENABLED);
        booleanToggle(value, eb, "value.lootAutoApply", AkaishiConfig.VALUE_LOOT_AUTO_APPLY);
        doubleField(value, eb, "value.lootCap", AkaishiConfig.VALUE_LOOT_CAP);
        booleanToggle(value, eb, "value.fluidEnabled", AkaishiConfig.VALUE_FLUID_ENABLED);
        doubleField(value, eb, "value.fluidPerMbCap", AkaishiConfig.VALUE_FLUID_PER_MB_CAP);
        stringList(value, eb, "value.fluidValues", AkaishiConfig.VALUE_FLUID_VALUES);
        stringList(value, eb, "value.overrides", AkaishiConfig.VALUE_OVERRIDES);
        stringList(value, eb, "value.tagValues", AkaishiConfig.VALUE_TAG_VALUES);
        stringList(value, eb, "value.tierBonus", AkaishiConfig.VALUE_TIER_BONUS);
        stringList(value, eb, "value.keywordExclusions", AkaishiConfig.VALUE_KEYWORD_EXCLUSIONS);
        stringList(value, eb, "value.lootBlacklist", AkaishiConfig.VALUE_LOOT_BLACKLIST);
        intField(value, eb, "value.unifiedVaultRows", AkaishiConfig.UNIFIED_VAULT_ROWS);

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
