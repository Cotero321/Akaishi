package com.example.akaishi.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;

/**
 * 物品注册门面（历史兼容壳）。
 * <p>
 * 注册实现已按功能域拆分到 {@link AkaishiBaseItems} / {@link AkaishiEnergyItems} /
 * {@link AkaishiReactorItems} / {@link AkaishiFusionItems} / {@link AkaishiLifeItems} /
 * {@link AkaishiTransgeneItems} / {@link AkaishiWirelessItems}。
 * 本类仅保留 ID 常量（注册唯一来源）与字段转发，供历史代码经 {@code ModItems.xxx} 访问；
 * 新增注册一律写入对应功能域类，禁止再向本类堆积。
 */
public final class ModItems {

    public static final String CHISHI_CRYSTAL_ID = "akaishi_crystal";
    public static final String CHISHI_ESSENCE_ID = "akaishi_essence";
    public static final String ESSENCE_COMPRESSED_ID = "akaishi_essence_compressed";
    public static final String MACHINE_COMPONENT_ID = "akaishi_machine_component";
    public static final String ADVANCED_COMPONENT_ID = "akaishi_advanced_component";
    public static final String DEBUG_TOOL_ID = "akaishi_debug_tool";
    public static final String CHISHI_INGOT_ID = "akaishi_ingot";
    public static final String UPGRADE_TEMPLATE_ID = "akaishi_upgrade_template";
    public static final String HELMET_ID = "akaishi_helmet";
    public static final String CHESTPLATE_ID = "akaishi_chestplate";
    public static final String LEGGINGS_ID = "akaishi_leggings";
    public static final String BOOTS_ID = "akaishi_boots";
    public static final String SWORD_ID = "akaishi_sword";
    public static final String PICKAXE_ID = "akaishi_pickaxe";
    public static final String SHOVEL_ID = "akaishi_shovel";
    public static final String AXE_ID = "akaishi_axe";
    public static final String PORTABLE_CELL_BASIC_ID = "portable_akaishi_cell_basic";
    public static final String PORTABLE_CELL_ADVANCED_ID = "portable_akaishi_cell_advanced";
    public static final String PORTABLE_CELL_SUPER_ID = "portable_akaishi_cell_super";
    /** 能源产生升级组件：装配到发生器（单块/多方块中心），每个提升 1.75 倍产能速度、减少 1% 产出，最多 10 个（满配约 242 倍） */
    public static final String SPEED_UPGRADE_ID = "akaishi_speed_upgrade";
    /** 生命能量固态物：由生命能量提纯器以 1000 生命能量 + 10M 赤能源固化，生命能源体系的基础材料 */
    public static final String LIFE_ESSENCE_SOLID_ID = "akaishi_life_essence_solid";
    /** 末地混合物：末影之眼+潜影贝壳+紫颂果合成，液化 → 末地混合燃料 */
    public static final String END_MIXTURE_ID = "end_mixture";
    /** 巨龙混合物：龙息+末地水晶+黑曜石合成，液化 → 末地巨龙燃料 */
    public static final String DRAGON_MIXTURE_ID = "dragon_mixture";
    /** 幽匿生命体：回响碎片+幽匿块+金苹果+赤石精华块合成，液化 → 幽匿生命燃料 */
    public static final String SCULK_LIFEFORM_ID = "sculk_lifeform";
    /** 燃料罐：装罐机充装液体燃料，容量 10L（10000mb），装液后不可堆叠 */
    public static final String FUEL_CELL_ID = "fuel_cell";
    /** 劣质散热片：散热效率 1%，耐久 36k tick（反应堆散热组件消耗品） */
    public static final String HEAT_SINK_POOR_ID = "heat_sink_poor";
    /** 普通散热片：散热效率 2%，耐久 72k tick */
    public static final String HEAT_SINK_NORMAL_ID = "heat_sink_normal";
    /** 良好散热片：散热效率 3%，耐久 108k tick */
    public static final String HEAT_SINK_GOOD_ID = "heat_sink_good";
    /** 优质散热片：散热效率 4%，耐久 144k tick */
    public static final String HEAT_SINK_FINE_ID = "heat_sink_fine";
    /** 精良散热片：散热效率 5%，耐久 180k tick */
    public static final String HEAT_SINK_EXQUISITE_ID = "heat_sink_exquisite";
    /** 终极散热片：散热效率 7%，耐久 180k tick（最高档） */
    public static final String HEAT_SINK_ULTIMATE_ID = "heat_sink_ultimate";
    /** 聚变反应棒（赤石锭+生命精华合成）：离子体填装器的燃料载体 */
    public static final String FUSION_ROD_ID = "akaishi_fusion_rod";
    /** 等离子体燃料棒（3 种）：离子体填装器将等离子体灌入反应棒的产物 */
    public static final String MIXED_PLASMA_ROD_ID = "akaishi_mixed_plasma_rod";
    public static final String NETHER_PLASMA_ROD_ID = "akaishi_nether_plasma_rod";
    public static final String END_PLASMA_ROD_ID = "akaishi_end_plasma_rod";
    /** 生命灰烬：聚变堆燃烧副产物，生命散热片合成材料 */
    public static final String LIFE_ASH_ID = "akaishi_life_ash";
    /** 聚变散热片（5 档 + 生命）：经控制器热量页 GUI 放入，散热效率 5%~20%，耐久 8000（5 秒 1 点） */
    public static final String FUSION_HEAT_SINK_TIER1_ID = "fusion_heat_sink_tier1";
    public static final String FUSION_HEAT_SINK_TIER2_ID = "fusion_heat_sink_tier2";
    public static final String FUSION_HEAT_SINK_TIER3_ID = "fusion_heat_sink_tier3";
    public static final String FUSION_HEAT_SINK_TIER4_ID = "fusion_heat_sink_tier4";
    public static final String FUSION_HEAT_SINK_TIER5_ID = "fusion_heat_sink_tier5";
    public static final String FUSION_HEAT_SINK_LIFE_ID = "fusion_heat_sink_life";
    /** 赤石饱食护符（charm 槽）：消耗赤能源补充饱和度 */
    public static final String SATIATION_CHARM_ID = "akaishi_satiation_charm";
    /** 赤石狩猎指环（ring 槽）：击杀生物概率掉落赤石晶 */
    public static final String HUNTING_RING_ID = "akaishi_hunting_ring";
    /** 机器升级组件：装入用电器升级槽，单格堆叠 8 封顶（速度/能量各一格、互斥） */
    public static final String MACHINE_SPEED_UPGRADE_ID = "akaishi_machine_speed_upgrade";
    public static final String MACHINE_ENERGY_UPGRADE_ID = "akaishi_machine_energy_upgrade";
    // ===== 粉末（打粉机产物 / 压缩机原料）=====
    public static final String CHISHI_DUST_ID = "akaishi_dust";
    public static final String COAL_DUST_ID = "coal_dust";
    public static final String IRON_DUST_ID = "iron_dust";
    public static final String COPPER_DUST_ID = "copper_dust";
    public static final String GOLD_DUST_ID = "gold_dust";
    public static final String LAPIS_DUST_ID = "lapis_dust";
    public static final String DIAMOND_DUST_ID = "diamond_dust";
    public static final String EMERALD_DUST_ID = "emerald_dust";
    public static final String QUARTZ_DUST_ID = "quartz_dust";
    public static final String NETHERITE_DUST_ID = "netherite_dust";
    public static final String OBSIDIAN_DUST_ID = "obsidian_dust";
    // ===== 基底（变化器产物）=====
    public static final String COOLING_BASE_ID = "cooling_base";
    public static final String COAL_ORE_BASE_ID = "coal_ore_base";
    public static final String IRON_ORE_BASE_ID = "iron_ore_base";
    public static final String COPPER_ORE_BASE_ID = "copper_ore_base";
    public static final String GOLD_ORE_BASE_ID = "gold_ore_base";
    public static final String REDSTONE_ORE_BASE_ID = "redstone_ore_base";
    public static final String LAPIS_ORE_BASE_ID = "lapis_ore_base";
    public static final String DIAMOND_ORE_BASE_ID = "diamond_ore_base";
    public static final String EMERALD_ORE_BASE_ID = "emerald_ore_base";
    public static final String QUARTZ_ORE_BASE_ID = "quartz_ore_base";
    public static final String NETHERITE_ORE_BASE_ID = "netherite_ore_base";
    public static final String CHISHI_ORE_BASE_ID = "akaishi_ore_base";
    /** 赤石日记：Patchouli 手册物品，右键打开 akaishi:akaishi_diary */
    public static final String AKAISHI_DIARY_ID = "akaishi_diary";
    /** 生命之书：Patchouli 手册物品，右键打开 akaishi:life_secrets */
    public static final String LIFE_BOOK_ID = "akaishi_life_book";
    /** 基因详解：Patchouli 手册物品，右键打开 akaishi:gene_detail */
    public static final String GENE_BOOK_ID = "akaishi_gene_book";
    /** 赤石采集手环（hands 槽）：挖掘方块概率掉落赤石晶 */
    public static final String GATHERING_BRACELET_ID = "akaishi_gathering_bracelet";
    /** 赤石防火吊坠（necklace 槽）：消耗赤能源维持防火 */
    public static final String FIRE_NECKLACE_ID = "akaishi_fire_necklace";
    /** 赤石防爆护符（body 槽）：消耗赤能源抵消爆炸伤害 */
    public static final String BLAST_CHARM_ID = "akaishi_blast_charm";
    /** 赤石净化手镯（bracelet 槽）：消耗赤能源移除中毒 */
    public static final String ANTIDOTE_BRACELET_ID = "akaishi_antidote_bracelet";
    /** 赤石凋零护符（belt 槽）：消耗赤能源移除凋零 */
    public static final String WITHER_CHARM_ID = "akaishi_wither_charm";
    /** 样本采集器：随身生命能量容器，右键生物抽取生命样本，右键生命能量方块充能 */
    public static final String SAMPLE_COLLECTOR_ID = "akaishi_sample_collector";
    /** 生命样本：从活体生物抽取的遗传物质，生命分析台原料 */
    public static final String LIFE_SAMPLE_ID = "akaishi_life_sample";
    /** 基因序列片段：纯度 100 样本在分析台解构的产物，生命结构台原料 */
    public static final String GENE_SEQUENCE_ID = "akaishi_gene_sequence";
    /** 生命胚胎：八份生命固态 + 一枚鸡蛋 */
    public static final String LIFE_EMBRYO_ID = "akaishi_life_embryo";
    /** 生命的融合锭：母神祭坛仪式的产物 */
    public static final String LIFE_FUSION_INGOT_ID = "akaishi_life_fusion_ingot";
    /** 生命融合护甲（4 件）：赤石护甲 2 倍基础数值，保留升级数据，穿齐触发套装效果 */
    public static final String LIFE_FUSION_HELMET_ID = "akaishi_life_fusion_helmet";
    public static final String LIFE_FUSION_CHESTPLATE_ID = "akaishi_life_fusion_chestplate";
    public static final String LIFE_FUSION_LEGGINGS_ID = "akaishi_life_fusion_leggings";
    public static final String LIFE_FUSION_BOOTS_ID = "akaishi_life_fusion_boots";
    /** 药剂（单物品承载永久/突破模板，模板 id 与纯度写 NBT） */
    public static final String POTION_ID = "akaishi_potion";
    /** 排异中和剂：消耗品，减轻已移植非原生器官的排斥 */
    public static final String REJECTION_SERUM_ID = "akaishi_rejection_serum";
    /** 无线能源便捷组件 ID */
    public static final String WIRELESS_COMPONENT_ID = "akaishi_wireless_component";
    /** 无线能源便捷终端 ID */
    public static final String WIRELESS_PORTABLE_TERMINAL_ID = "akaishi_wireless_portable_terminal";
    /** 终端身份卡 ID（无线网络认证钥匙） */
    public static final String WIRELESS_IDENTITY_CARD_ID = "akaishi_wireless_identity_card";

    // ==================== 字段转发壳（注册完成后指向对应域类）====================

    // —— 赤石基础域（AkaishiBaseItems）——
    public static RegistrySupplier<Item> akaishiCrystal;
    public static RegistrySupplier<Item> akaishiEssence;
    public static RegistrySupplier<Item> akaishiEssenceCompressed;
    public static RegistrySupplier<Item> akaishiMachineComponent;
    public static RegistrySupplier<Item> akaishiAdvancedComponent;
    public static RegistrySupplier<Item> akaishiIngot;
    public static RegistrySupplier<Item> akaishiUpgradeTemplate;
    public static RegistrySupplier<Item> akaishiHelmet;
    public static RegistrySupplier<Item> akaishiChestplate;
    public static RegistrySupplier<Item> akaishiLeggings;
    public static RegistrySupplier<Item> akaishiBoots;
    public static RegistrySupplier<Item> akaishiSword;
    public static RegistrySupplier<Item> akaishiPickaxe;
    public static RegistrySupplier<Item> akaishiShovel;
    public static RegistrySupplier<Item> akaishiAxe;
    public static RegistrySupplier<Item> satiationCharm;
    public static RegistrySupplier<Item> huntingRing;
    public static RegistrySupplier<Item> gatheringBracelet;
    public static RegistrySupplier<Item> fireNecklace;
    public static RegistrySupplier<Item> blastCharm;
    public static RegistrySupplier<Item> antidoteBracelet;
    public static RegistrySupplier<Item> witherCharm;
    public static RegistrySupplier<Item> machineSpeedUpgrade;
    public static RegistrySupplier<Item> machineEnergyUpgrade;
    public static RegistrySupplier<Item> akaishiDust;
    public static RegistrySupplier<Item> coalDust;
    public static RegistrySupplier<Item> ironDust;
    public static RegistrySupplier<Item> copperDust;
    public static RegistrySupplier<Item> goldDust;
    public static RegistrySupplier<Item> lapisDust;
    public static RegistrySupplier<Item> diamondDust;
    public static RegistrySupplier<Item> emeraldDust;
    public static RegistrySupplier<Item> quartzDust;
    public static RegistrySupplier<Item> netheriteDust;
    public static RegistrySupplier<Item> obsidianDust;
    public static RegistrySupplier<Item> coolingBase;
    public static RegistrySupplier<Item> coalOreBase;
    public static RegistrySupplier<Item> ironOreBase;
    public static RegistrySupplier<Item> copperOreBase;
    public static RegistrySupplier<Item> goldOreBase;
    public static RegistrySupplier<Item> redstoneOreBase;
    public static RegistrySupplier<Item> lapisOreBase;
    public static RegistrySupplier<Item> diamondOreBase;
    public static RegistrySupplier<Item> emeraldOreBase;
    public static RegistrySupplier<Item> quartzOreBase;
    public static RegistrySupplier<Item> netheriteOreBase;
    public static RegistrySupplier<Item> akaishiOreBase;
    public static RegistrySupplier<Item> akaishiDiary;
    public static RegistrySupplier<Item> lifeBook;
    public static RegistrySupplier<Item> geneBook;

    // —— 能源域（AkaishiEnergyItems）——
    public static RegistrySupplier<Item> akaishiDebugTool;
    public static RegistrySupplier<Item> portableCellBasic;
    public static RegistrySupplier<Item> portableCellAdvanced;
    public static RegistrySupplier<Item> portableCellSuper;
    public static RegistrySupplier<Item> akaishiSpeedUpgrade;
    public static RegistrySupplier<Item> endMixture;
    public static RegistrySupplier<Item> dragonMixture;
    public static RegistrySupplier<Item> sculkLifeform;

    // —— 反应堆域（AkaishiReactorItems）——
    public static RegistrySupplier<Item> fuelCell;
    public static RegistrySupplier<Item> heatSinkPoor;
    public static RegistrySupplier<Item> heatSinkNormal;
    public static RegistrySupplier<Item> heatSinkGood;
    public static RegistrySupplier<Item> heatSinkFine;
    public static RegistrySupplier<Item> heatSinkExquisite;
    public static RegistrySupplier<Item> heatSinkUltimate;

    // —— 聚变域（AkaishiFusionItems）——
    public static RegistrySupplier<Item> fusionRod;
    public static RegistrySupplier<Item> mixedPlasmaRod;
    public static RegistrySupplier<Item> netherPlasmaRod;
    public static RegistrySupplier<Item> endPlasmaRod;
    public static RegistrySupplier<Item> lifeAsh;
    public static RegistrySupplier<Item> fusionHeatSinkTier1;
    public static RegistrySupplier<Item> fusionHeatSinkTier2;
    public static RegistrySupplier<Item> fusionHeatSinkTier3;
    public static RegistrySupplier<Item> fusionHeatSinkTier4;
    public static RegistrySupplier<Item> fusionHeatSinkTier5;
    public static RegistrySupplier<Item> fusionHeatSinkLife;

    // —— 生命域（AkaishiLifeItems）——
    public static RegistrySupplier<Item> akaishiLifeEssenceSolid;
    public static RegistrySupplier<Item> sampleCollector;
    public static RegistrySupplier<Item> lifeSample;
    public static RegistrySupplier<Item> geneSequence;
    public static RegistrySupplier<Item> lifeEmbryo;
    public static RegistrySupplier<Item> lifeFusionIngot;
    public static RegistrySupplier<Item> lifeFusionHelmet;
    public static RegistrySupplier<Item> lifeFusionChestplate;
    public static RegistrySupplier<Item> lifeFusionLeggings;
    public static RegistrySupplier<Item> lifeFusionBoots;
    public static RegistrySupplier<Item> akaishiPotion;
    public static RegistrySupplier<Item> rejectionSerum;
    public static RegistrySupplier<Item> akaishiOrganEye;
    public static RegistrySupplier<Item> akaishiOrganHeart;
    public static RegistrySupplier<Item> akaishiOrganLungs;
    public static RegistrySupplier<Item> akaishiOrganViscera;
    public static RegistrySupplier<Item> akaishiOrganKidneys;
    public static RegistrySupplier<Item> akaishiOrganLeftArm;
    public static RegistrySupplier<Item> akaishiOrganRightArm;
    public static RegistrySupplier<Item> akaishiOrganLeftLeg;
    public static RegistrySupplier<Item> akaishiOrganRightLeg;
    public static RegistrySupplier<Item> exhaustedCrystal;
    public static RegistrySupplier<Item> activatedSculkCrystal;
    public static RegistrySupplier<Item> activatedNetherCompoundCrystal;
    public static RegistrySupplier<Item> activatedEndMixtureCrystal;
    public static RegistrySupplier<Item> activatedAdvancedMixtureCrystal;
    public static RegistrySupplier<Item> activatedPureCrystal;
    public static RegistrySupplier<Item> activatedDragonCrystal;
    public static RegistrySupplier<Item> activatedUltimateMixtureCrystal;
    public static RegistrySupplier<Item> activatedSculkComponent;
    public static RegistrySupplier<Item> activatedNetherCompoundComponent;
    public static RegistrySupplier<Item> activatedEndMixtureComponent;
    public static RegistrySupplier<Item> activatedAdvancedMixtureComponent;
    public static RegistrySupplier<Item> activatedPureComponent;
    public static RegistrySupplier<Item> activatedDragonComponent;
    public static RegistrySupplier<Item> activatedUltimateMixtureComponent;

    // —— 转基因域（AkaishiTransgeneItems）：转基因植物（凋零藤）——
    public static RegistrySupplier<Item> akaishiWitherSeed;
    public static RegistrySupplier<Item> akaishiWitherCondensate;

    // —— 无线域（AkaishiWirelessItems）——
    public static RegistrySupplier<Item> akaishiWirelessComponent;
    public static RegistrySupplier<Item> akaishiWirelessPortableTerminal;
    public static RegistrySupplier<Item> akaishiWirelessIdentityCard;

    private ModItems() {
    }

    public static void register() {
        // 先完成各域注册，再转发字段供历史引用读取
        AkaishiBaseItems.register();
        AkaishiEnergyItems.register();
        AkaishiReactorItems.register();
        AkaishiFusionItems.register();
        AkaishiLifeItems.register();
        AkaishiTransgeneItems.register();
        AkaishiWirelessItems.register();

        // —— 赤石基础域 ——
        akaishiCrystal = AkaishiBaseItems.akaishiCrystal;
        akaishiEssence = AkaishiBaseItems.akaishiEssence;
        akaishiEssenceCompressed = AkaishiBaseItems.akaishiEssenceCompressed;
        akaishiMachineComponent = AkaishiBaseItems.akaishiMachineComponent;
        akaishiAdvancedComponent = AkaishiBaseItems.akaishiAdvancedComponent;
        akaishiIngot = AkaishiBaseItems.akaishiIngot;
        akaishiUpgradeTemplate = AkaishiBaseItems.akaishiUpgradeTemplate;
        akaishiHelmet = AkaishiBaseItems.akaishiHelmet;
        akaishiChestplate = AkaishiBaseItems.akaishiChestplate;
        akaishiLeggings = AkaishiBaseItems.akaishiLeggings;
        akaishiBoots = AkaishiBaseItems.akaishiBoots;
        akaishiSword = AkaishiBaseItems.akaishiSword;
        akaishiPickaxe = AkaishiBaseItems.akaishiPickaxe;
        akaishiShovel = AkaishiBaseItems.akaishiShovel;
        akaishiAxe = AkaishiBaseItems.akaishiAxe;
        satiationCharm = AkaishiBaseItems.satiationCharm;
        huntingRing = AkaishiBaseItems.huntingRing;
        gatheringBracelet = AkaishiBaseItems.gatheringBracelet;
        fireNecklace = AkaishiBaseItems.fireNecklace;
        blastCharm = AkaishiBaseItems.blastCharm;
        antidoteBracelet = AkaishiBaseItems.antidoteBracelet;
        witherCharm = AkaishiBaseItems.witherCharm;
        machineSpeedUpgrade = AkaishiBaseItems.machineSpeedUpgrade;
        machineEnergyUpgrade = AkaishiBaseItems.machineEnergyUpgrade;
        akaishiDust = AkaishiBaseItems.akaishiDust;
        coalDust = AkaishiBaseItems.coalDust;
        ironDust = AkaishiBaseItems.ironDust;
        copperDust = AkaishiBaseItems.copperDust;
        goldDust = AkaishiBaseItems.goldDust;
        lapisDust = AkaishiBaseItems.lapisDust;
        diamondDust = AkaishiBaseItems.diamondDust;
        emeraldDust = AkaishiBaseItems.emeraldDust;
        quartzDust = AkaishiBaseItems.quartzDust;
        netheriteDust = AkaishiBaseItems.netheriteDust;
        obsidianDust = AkaishiBaseItems.obsidianDust;
        coolingBase = AkaishiBaseItems.coolingBase;
        coalOreBase = AkaishiBaseItems.coalOreBase;
        ironOreBase = AkaishiBaseItems.ironOreBase;
        copperOreBase = AkaishiBaseItems.copperOreBase;
        goldOreBase = AkaishiBaseItems.goldOreBase;
        redstoneOreBase = AkaishiBaseItems.redstoneOreBase;
        lapisOreBase = AkaishiBaseItems.lapisOreBase;
        diamondOreBase = AkaishiBaseItems.diamondOreBase;
        emeraldOreBase = AkaishiBaseItems.emeraldOreBase;
        quartzOreBase = AkaishiBaseItems.quartzOreBase;
        netheriteOreBase = AkaishiBaseItems.netheriteOreBase;
        akaishiOreBase = AkaishiBaseItems.akaishiOreBase;
        akaishiDiary = AkaishiBaseItems.akaishiDiary;
        lifeBook = AkaishiBaseItems.lifeBook;
        geneBook = AkaishiBaseItems.geneBook;

        // —— 能源域 ——
        akaishiDebugTool = AkaishiEnergyItems.akaishiDebugTool;
        portableCellBasic = AkaishiEnergyItems.portableCellBasic;
        portableCellAdvanced = AkaishiEnergyItems.portableCellAdvanced;
        portableCellSuper = AkaishiEnergyItems.portableCellSuper;
        akaishiSpeedUpgrade = AkaishiEnergyItems.akaishiSpeedUpgrade;
        endMixture = AkaishiEnergyItems.endMixture;
        dragonMixture = AkaishiEnergyItems.dragonMixture;
        sculkLifeform = AkaishiEnergyItems.sculkLifeform;

        // —— 反应堆域 ——
        fuelCell = AkaishiReactorItems.fuelCell;
        heatSinkPoor = AkaishiReactorItems.heatSinkPoor;
        heatSinkNormal = AkaishiReactorItems.heatSinkNormal;
        heatSinkGood = AkaishiReactorItems.heatSinkGood;
        heatSinkFine = AkaishiReactorItems.heatSinkFine;
        heatSinkExquisite = AkaishiReactorItems.heatSinkExquisite;
        heatSinkUltimate = AkaishiReactorItems.heatSinkUltimate;

        // —— 聚变域 ——
        fusionRod = AkaishiFusionItems.fusionRod;
        mixedPlasmaRod = AkaishiFusionItems.mixedPlasmaRod;
        netherPlasmaRod = AkaishiFusionItems.netherPlasmaRod;
        endPlasmaRod = AkaishiFusionItems.endPlasmaRod;
        lifeAsh = AkaishiFusionItems.lifeAsh;
        fusionHeatSinkTier1 = AkaishiFusionItems.fusionHeatSinkTier1;
        fusionHeatSinkTier2 = AkaishiFusionItems.fusionHeatSinkTier2;
        fusionHeatSinkTier3 = AkaishiFusionItems.fusionHeatSinkTier3;
        fusionHeatSinkTier4 = AkaishiFusionItems.fusionHeatSinkTier4;
        fusionHeatSinkTier5 = AkaishiFusionItems.fusionHeatSinkTier5;
        fusionHeatSinkLife = AkaishiFusionItems.fusionHeatSinkLife;

        // —— 生命域 ——
        akaishiLifeEssenceSolid = AkaishiLifeItems.akaishiLifeEssenceSolid;
        sampleCollector = AkaishiLifeItems.sampleCollector;
        lifeSample = AkaishiLifeItems.lifeSample;
        geneSequence = AkaishiLifeItems.geneSequence;
        lifeEmbryo = AkaishiLifeItems.lifeEmbryo;
        lifeFusionIngot = AkaishiLifeItems.lifeFusionIngot;
        lifeFusionHelmet = AkaishiLifeItems.lifeFusionHelmet;
        lifeFusionChestplate = AkaishiLifeItems.lifeFusionChestplate;
        lifeFusionLeggings = AkaishiLifeItems.lifeFusionLeggings;
        lifeFusionBoots = AkaishiLifeItems.lifeFusionBoots;
        akaishiPotion = AkaishiLifeItems.akaishiPotion;
        rejectionSerum = AkaishiLifeItems.rejectionSerum;
        akaishiOrganEye = AkaishiLifeItems.akaishiOrganEye;
        akaishiOrganHeart = AkaishiLifeItems.akaishiOrganHeart;
        akaishiOrganLungs = AkaishiLifeItems.akaishiOrganLungs;
        akaishiOrganViscera = AkaishiLifeItems.akaishiOrganViscera;
        akaishiOrganKidneys = AkaishiLifeItems.akaishiOrganKidneys;
        akaishiOrganLeftArm = AkaishiLifeItems.akaishiOrganLeftArm;
        akaishiOrganRightArm = AkaishiLifeItems.akaishiOrganRightArm;
        akaishiOrganLeftLeg = AkaishiLifeItems.akaishiOrganLeftLeg;
        akaishiOrganRightLeg = AkaishiLifeItems.akaishiOrganRightLeg;
        exhaustedCrystal = AkaishiLifeItems.exhaustedCrystal;
        activatedSculkCrystal = AkaishiLifeItems.activatedSculkCrystal;
        activatedNetherCompoundCrystal = AkaishiLifeItems.activatedNetherCompoundCrystal;
        activatedEndMixtureCrystal = AkaishiLifeItems.activatedEndMixtureCrystal;
        activatedAdvancedMixtureCrystal = AkaishiLifeItems.activatedAdvancedMixtureCrystal;
        activatedPureCrystal = AkaishiLifeItems.activatedPureCrystal;
        activatedDragonCrystal = AkaishiLifeItems.activatedDragonCrystal;
        activatedUltimateMixtureCrystal = AkaishiLifeItems.activatedUltimateMixtureCrystal;
        activatedSculkComponent = AkaishiLifeItems.activatedSculkComponent;
        activatedNetherCompoundComponent = AkaishiLifeItems.activatedNetherCompoundComponent;
        activatedEndMixtureComponent = AkaishiLifeItems.activatedEndMixtureComponent;
        activatedAdvancedMixtureComponent = AkaishiLifeItems.activatedAdvancedMixtureComponent;
        activatedPureComponent = AkaishiLifeItems.activatedPureComponent;
        activatedDragonComponent = AkaishiLifeItems.activatedDragonComponent;
        activatedUltimateMixtureComponent = AkaishiLifeItems.activatedUltimateMixtureComponent;

        // —— 转基因域 ——
        akaishiWitherSeed = AkaishiTransgeneItems.akaishiWitherSeed;
        akaishiWitherCondensate = AkaishiTransgeneItems.akaishiWitherCondensate;

        // —— 无线域 ——
        akaishiWirelessComponent = AkaishiWirelessItems.akaishiWirelessComponent;
        akaishiWirelessPortableTerminal = AkaishiWirelessItems.akaishiWirelessPortableTerminal;
        akaishiWirelessIdentityCard = AkaishiWirelessItems.akaishiWirelessIdentityCard;
    }
}
