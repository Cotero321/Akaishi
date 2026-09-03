package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.energy.PortableCellTier;
import com.example.akaishi.item.curio.AkaishiAntidoteBracelet;
import com.example.akaishi.item.curio.AkaishiBlastCharm;
import com.example.akaishi.item.curio.AkaishiFireNecklace;
import com.example.akaishi.item.curio.AkaishiGatheringBracelet;
import com.example.akaishi.item.curio.AkaishiHuntingRing;
import com.example.akaishi.item.curio.AkaishiSatiationCharm;
import com.example.akaishi.item.curio.AkaishiWitherCharm;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.potion.AkaishiPotionItem;
import com.example.akaishi.life.potion.AkaishiRejectionSerumItem;
import com.example.akaishi.life.sample.AkaishiLifeSampleItem;
import com.example.akaishi.life.sample.AkaishiSampleCollectorItem;
import com.example.akaishi.life.sample.AkaishiLifeEmbryoItem;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;

/**
 * 物品注册。
 * 赤石晶由矿簇开采掉落；赤石精华由赤石提纯器产出，是后续科技的高级材料。
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
    /** 聚变散热片（5 档 + 生命）：插入聚变散热框架，散热效率 5%~20%，耐久 8000（5 秒 1 点） */
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
    /** 器官物品 ID（9 槽位各一），基因来源/品质存 NBT */
    public static final String ORGAN_EYE_ID = "akaishi_organ_eye";
    /** 药剂（单物品承载永久/突破模板，模板 id 与纯度写 NBT） */
    public static final String POTION_ID = "akaishi_potion";
    /** 排异中和剂：消耗品，减轻已移植非原生器官的排斥 */
    public static final String REJECTION_SERUM_ID = "akaishi_rejection_serum";
    public static final String ORGAN_HEART_ID = "akaishi_organ_heart";
    public static final String ORGAN_LUNGS_ID = "akaishi_organ_lungs";
    public static final String ORGAN_VISCERA_ID = "akaishi_organ_viscera";
    public static final String ORGAN_KIDNEYS_ID = "akaishi_organ_kidneys";
    public static final String ORGAN_LEFT_ARM_ID = "akaishi_organ_left_arm";
    public static final String ORGAN_RIGHT_ARM_ID = "akaishi_organ_right_arm";
    public static final String ORGAN_LEFT_LEG_ID = "akaishi_organ_left_leg";
    public static final String ORGAN_RIGHT_LEG_ID = "akaishi_organ_right_leg";
    /** 无线能源便捷组件 ID */
    public static final String WIRELESS_COMPONENT_ID = "akaishi_wireless_component";
    /** 无线能源便捷终端 ID */
    public static final String WIRELESS_PORTABLE_TERMINAL_ID = "akaishi_wireless_portable_terminal";
    /** 终端身份卡 ID（无线网络认证钥匙） */
    public static final String WIRELESS_IDENTITY_CARD_ID = "akaishi_wireless_identity_card";
    /** 赤石晶延迟注册引用（注册完成后可用） */
    public static RegistrySupplier<Item> akaishiCrystal;
    /** 赤石精华延迟注册引用 */
    public static RegistrySupplier<Item> akaishiEssence;
    /** 浓缩赤石精华：由 9 个赤石精华压缩而成，赤石科技的高级材料 */
    public static RegistrySupplier<Item> akaishiEssenceCompressed;
    /** 赤红机器组件：赤石科技设备的通用部件 */
    public static RegistrySupplier<Item> akaishiMachineComponent;
    /** 赤红高级机械组件：以浓缩精华与钻石强化，用于赤能源发生机等高端设备 */
    public static RegistrySupplier<Item> akaishiAdvancedComponent;
    /** 赤能源配置器：切换管道方向模式（正常/推/拉）与断开单侧连接 */
    public static RegistrySupplier<Item> akaishiDebugTool;
    /** 赤石锭：由赤石能量聚合器以 10M 赤能源 + 下界合金锭聚合而成，赤石装备的核心材料 */
    public static RegistrySupplier<Item> akaishiIngot;
    /** 赤红升级模板：赤红升级台的消耗品，用于为赤石装备应用升级 */
    public static RegistrySupplier<Item> akaishiUpgradeTemplate;
    /** 赤石头盔 */
    public static RegistrySupplier<Item> akaishiHelmet;
    /** 赤石胸甲 */
    public static RegistrySupplier<Item> akaishiChestplate;
    /** 赤石护腿 */
    public static RegistrySupplier<Item> akaishiLeggings;
    /** 赤石靴子 */
    public static RegistrySupplier<Item> akaishiBoots;
    /** 赤石剑 */
    public static RegistrySupplier<Item> akaishiSword;
    /** 赤石镐 */
    public static RegistrySupplier<Item> akaishiPickaxe;
    /** 赤石铲 */
    public static RegistrySupplier<Item> akaishiShovel;
    /** 赤石斧 */
    public static RegistrySupplier<Item> akaishiAxe;
    /** 便携赤能源储存单元（初级） */
    public static RegistrySupplier<Item> portableCellBasic;
    /** 便携赤能源储存单元（中级） */
    public static RegistrySupplier<Item> portableCellAdvanced;
    /** 便携赤能源储存单元（高级） */
    public static RegistrySupplier<Item> portableCellSuper;
    /** 能源产生升级组件 */
    public static RegistrySupplier<Item> akaishiSpeedUpgrade;
    /** 生命能量固态物 */
    public static RegistrySupplier<Item> akaishiLifeEssenceSolid;
    /** 末地混合物 */
    public static RegistrySupplier<Item> endMixture;
    /** 巨龙混合物 */
    public static RegistrySupplier<Item> dragonMixture;
    /** 幽匿生命体 */
    public static RegistrySupplier<Item> sculkLifeform;
    /** 燃料罐（空罐） */
    public static RegistrySupplier<Item> fuelCell;
    /** 劣质散热片 */
    public static RegistrySupplier<Item> heatSinkPoor;
    /** 普通散热片 */
    public static RegistrySupplier<Item> heatSinkNormal;
    /** 良好散热片 */
    public static RegistrySupplier<Item> heatSinkGood;
    /** 优质散热片 */
    public static RegistrySupplier<Item> heatSinkFine;
    /** 精良散热片 */
    public static RegistrySupplier<Item> heatSinkExquisite;
    /** 终极散热片 */
    public static RegistrySupplier<Item> heatSinkUltimate;
    /** 赤石饱食护符 */
    public static RegistrySupplier<Item> satiationCharm;
    /** 赤石狩猎指环 */
    public static RegistrySupplier<Item> huntingRing;
    /** 赤石采集手环 */
    public static RegistrySupplier<Item> gatheringBracelet;
    /** 赤石防火吊坠 */
    public static RegistrySupplier<Item> fireNecklace;
    /** 赤石防爆护符 */
    public static RegistrySupplier<Item> blastCharm;
    /** 赤石净化手镯 */
    public static RegistrySupplier<Item> antidoteBracelet;
    /** 赤石凋零护符 */
    public static RegistrySupplier<Item> witherCharm;
    /** 样本采集器 */
    public static RegistrySupplier<Item> sampleCollector;
    /** 生命样本 */
    public static RegistrySupplier<Item> lifeSample;
    /** 基因序列片段 */
    public static RegistrySupplier<Item> geneSequence;
    /** 生命胚胎 */
    public static RegistrySupplier<Item> lifeEmbryo;
    /** 生命的融合锭 */
    public static RegistrySupplier<Item> lifeFusionIngot;
    /** 生命融合护甲（4 件） */
    public static RegistrySupplier<Item> lifeFusionHelmet;
    public static RegistrySupplier<Item> lifeFusionChestplate;
    public static RegistrySupplier<Item> lifeFusionLeggings;
    public static RegistrySupplier<Item> lifeFusionBoots;
    /** 药剂（永久/突破模板共用） */
    public static RegistrySupplier<Item> akaishiPotion;
    /** 排异中和剂（消耗品） */
    public static RegistrySupplier<Item> rejectionSerum;
    /** 器官物品（9 槽位各一） */
    public static RegistrySupplier<Item> akaishiOrganEye;
    public static RegistrySupplier<Item> akaishiOrganHeart;
    public static RegistrySupplier<Item> akaishiOrganLungs;
    public static RegistrySupplier<Item> akaishiOrganViscera;
    public static RegistrySupplier<Item> akaishiOrganKidneys;
    public static RegistrySupplier<Item> akaishiOrganLeftArm;
    public static RegistrySupplier<Item> akaishiOrganRightArm;
    public static RegistrySupplier<Item> akaishiOrganLeftLeg;
    public static RegistrySupplier<Item> akaishiOrganRightLeg;
    /** 无线能源便捷组件：无线终端体系通用合成材料 */
    public static RegistrySupplier<Item> akaishiWirelessComponent;
    /** 无线能源便捷终端：手持频道遥控面板（查看/切换频道） */
    public static RegistrySupplier<Item> akaishiWirelessPortableTerminal;
    /** 终端身份卡（无线网络认证钥匙） */
    public static RegistrySupplier<Item> akaishiWirelessIdentityCard;
    /** 衰竭结晶：生命离心机分离活化燃料的通用副产物 */
    public static RegistrySupplier<Item> exhaustedCrystal;
    /** 活化结晶（7 种）：对应七种活化燃料的分离主产物 */
    public static RegistrySupplier<Item> activatedSculkCrystal;
    public static RegistrySupplier<Item> activatedNetherCompoundCrystal;
    public static RegistrySupplier<Item> activatedEndMixtureCrystal;
    public static RegistrySupplier<Item> activatedAdvancedMixtureCrystal;
    public static RegistrySupplier<Item> activatedPureCrystal;
    public static RegistrySupplier<Item> activatedDragonCrystal;
    public static RegistrySupplier<Item> activatedUltimateMixtureCrystal;
    /** 活化成分（7 种）：活化分馏器将活化结晶深度拆分的提纯产物 */
    public static RegistrySupplier<Item> activatedSculkComponent;
    public static RegistrySupplier<Item> activatedNetherCompoundComponent;
    public static RegistrySupplier<Item> activatedEndMixtureComponent;
    public static RegistrySupplier<Item> activatedAdvancedMixtureComponent;
    public static RegistrySupplier<Item> activatedPureComponent;
    public static RegistrySupplier<Item> activatedDragonComponent;
    public static RegistrySupplier<Item> activatedUltimateMixtureComponent;
    /** 聚变反应棒（赤石锭+生命精华合成）：离子体填装器的燃料载体 */
    public static RegistrySupplier<Item> fusionRod;
    /** 等离子体燃料棒（3 种）：离子体填装器将等离子体灌入反应棒的产物 */
    public static RegistrySupplier<Item> mixedPlasmaRod;
    public static RegistrySupplier<Item> netherPlasmaRod;
    public static RegistrySupplier<Item> endPlasmaRod;
    /** 生命灰烬：聚变堆燃烧副产物 */
    public static RegistrySupplier<Item> lifeAsh;
    /** 聚变散热片（6 档：5/7/9/12/15/20%） */
    public static RegistrySupplier<Item> fusionHeatSinkTier1;
    public static RegistrySupplier<Item> fusionHeatSinkTier2;
    public static RegistrySupplier<Item> fusionHeatSinkTier3;
    public static RegistrySupplier<Item> fusionHeatSinkTier4;
    public static RegistrySupplier<Item> fusionHeatSinkTier5;
    public static RegistrySupplier<Item> fusionHeatSinkLife;
    /** 机器升级组件（速度/能量）：装入用电器升级槽，单格堆叠 8 封顶 */
    public static RegistrySupplier<Item> machineSpeedUpgrade;
    public static RegistrySupplier<Item> machineEnergyUpgrade;
    // ===== 粉末（打粉机产物 / 压缩机原料）=====
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
    // ===== 基底（变化器产物）=====
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
    /** 赤石日记物品（Patchouli 手册） */
    public static RegistrySupplier<Item> akaishiDiary;
    /** 生命之书物品（Patchouli 手册） */
    public static RegistrySupplier<Item> lifeBook;

    private ModItems() {
    }

    public static void register() {
        akaishiCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, CHISHI_CRYSTAL_ID),
                        () -> new Item(new Item.Properties()));
        akaishiEssence = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, CHISHI_ESSENCE_ID),
                        () -> new Item(new Item.Properties()));
        akaishiEssenceCompressed = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ESSENCE_COMPRESSED_ID),
                        () -> new Item(new Item.Properties()));
        akaishiMachineComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, MACHINE_COMPONENT_ID),
                        () -> new Item(new Item.Properties()));
        akaishiAdvancedComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ADVANCED_COMPONENT_ID),
                        () -> new Item(new Item.Properties()));
        akaishiDebugTool = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, DEBUG_TOOL_ID),
                        () -> new AkaishiDebugTool());
        // 赤石锭与升级模板
        akaishiIngot = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, CHISHI_INGOT_ID),
                        () -> new Item(new Item.Properties()));
        akaishiUpgradeTemplate = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, UPGRADE_TEMPLATE_ID),
                        () -> new Item(new Item.Properties()));
        // 赤石装备（基础属性 = 下界合金 × 1.25，可升级）
        akaishiHelmet = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, HELMET_ID),
                        () -> new AkaishiArmorItem(ArmorItem.Type.HELMET, new Item.Properties()));
        akaishiChestplate = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, CHESTPLATE_ID),
                        () -> new AkaishiArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        akaishiLeggings = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LEGGINGS_ID),
                        () -> new AkaishiArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
        akaishiBoots = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, BOOTS_ID),
                        () -> new AkaishiArmorItem(ArmorItem.Type.BOOTS, new Item.Properties()));
        // 攻击伤害修饰符 7：4(下界合金材质加成) + 7 = 11 = 下界合金剑 9 × 1.25
        akaishiSword = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, SWORD_ID),
                        () -> new AkaishiSwordItem(Tiers.NETHERITE, 7, -2.0F, new Item.Properties()));
        // 赤石工具（属性 = 下界合金对应工具 × 1.25，见各类注释）
        akaishiPickaxe = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, PICKAXE_ID),
                        () -> new AkaishiPickaxeItem(new Item.Properties()));
        akaishiShovel = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, SHOVEL_ID),
                        () -> new AkaishiShovelItem(new Item.Properties()));
        akaishiAxe = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, AXE_ID),
                        () -> new AkaishiAxeItem(new Item.Properties()));
        // 便携赤能源储存单元（初级/中级/高级）
        portableCellBasic = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, PORTABLE_CELL_BASIC_ID),
                        () -> new AkaishiPortableEnergyCell(PortableCellTier.BASIC, new Item.Properties()));
        portableCellAdvanced = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, PORTABLE_CELL_ADVANCED_ID),
                        () -> new AkaishiPortableEnergyCell(PortableCellTier.ADVANCED, new Item.Properties()));
        portableCellSuper = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, PORTABLE_CELL_SUPER_ID),
                        () -> new AkaishiPortableEnergyCell(PortableCellTier.SUPER, new Item.Properties()));
        akaishiSpeedUpgrade = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, SPEED_UPGRADE_ID),
                        () -> new Item(new Item.Properties()));
        // 生命能量固态物（生命能量提纯器产出，生命能源体系基础材料）
        akaishiLifeEssenceSolid = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_ESSENCE_SOLID_ID),
                        () -> new Item(new Item.Properties()));
        // 末地混合物（工作台合成，液化装置 → 末地混合燃料）
        endMixture = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, END_MIXTURE_ID),
                        () -> new Item(new Item.Properties()));
        // 巨龙混合物（工作台合成，液化装置 → 末地巨龙燃料）
        dragonMixture = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, DRAGON_MIXTURE_ID),
                        () -> new Item(new Item.Properties()));
        // 幽匿生命体（工作台合成，液化装置 → 幽匿生命燃料）
        sculkLifeform = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, SCULK_LIFEFORM_ID),
                        () -> new Item(new Item.Properties()));
        // 燃料罐（装罐机充装液体燃料，空罐可堆叠 64）
        fuelCell = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FUEL_CELL_ID),
                        () -> new AkaishiFuelCellItem(new Item.Properties()));
        // 散热片（5 品质）：插入反应堆散热组件，消耗品带耐久
        heatSinkPoor = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, HEAT_SINK_POOR_ID),
                        () -> new AkaishiHeatSinkItem(HeatSinkQuality.POOR));
        heatSinkNormal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, HEAT_SINK_NORMAL_ID),
                        () -> new AkaishiHeatSinkItem(HeatSinkQuality.NORMAL));
        heatSinkGood = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, HEAT_SINK_GOOD_ID),
                        () -> new AkaishiHeatSinkItem(HeatSinkQuality.GOOD));
        heatSinkFine = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, HEAT_SINK_FINE_ID),
                        () -> new AkaishiHeatSinkItem(HeatSinkQuality.FINE));
        heatSinkExquisite = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, HEAT_SINK_EXQUISITE_ID),
                        () -> new AkaishiHeatSinkItem(HeatSinkQuality.EXQUISITE));
        heatSinkUltimate = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, HEAT_SINK_ULTIMATE_ID),
                        () -> new AkaishiHeatSinkItem(HeatSinkQuality.ULTIMATE));
        // 赤石饰品（Curios 槽位：charm/ring/hands/necklace/body/bracelet/belt）
        satiationCharm = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, SATIATION_CHARM_ID),
                        () -> new AkaishiSatiationCharm(new Item.Properties()));
        huntingRing = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, HUNTING_RING_ID),
                        () -> new AkaishiHuntingRing(new Item.Properties()));
        gatheringBracelet = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, GATHERING_BRACELET_ID),
                        () -> new AkaishiGatheringBracelet(new Item.Properties()));
        fireNecklace = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FIRE_NECKLACE_ID),
                        () -> new AkaishiFireNecklace(new Item.Properties()));
        blastCharm = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, BLAST_CHARM_ID),
                        () -> new AkaishiBlastCharm(new Item.Properties()));
        antidoteBracelet = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ANTIDOTE_BRACELET_ID),
                        () -> new AkaishiAntidoteBracelet(new Item.Properties()));
        witherCharm = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, WITHER_CHARM_ID),
                        () -> new AkaishiWitherCharm(new Item.Properties()));
        // 生命科技：样本采集器（不可堆叠）+ 生命样本（可堆叠 64）
        sampleCollector = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, SAMPLE_COLLECTOR_ID),
                        () -> new AkaishiSampleCollectorItem(new Item.Properties().stacksTo(1)));
        lifeSample = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_SAMPLE_ID),
                        () -> new AkaishiLifeSampleItem(new Item.Properties()));
        geneSequence = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, GENE_SEQUENCE_ID),
                        () -> new AkaishiGeneSequenceItem(new Item.Properties()));
        // 生命胚胎（8 生命固态 + 鸡蛋：无 NBT 铭刻的生命之始，献给母神祭坛）
        lifeEmbryo = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_EMBRYO_ID),
                        () -> new AkaishiLifeEmbryoItem(new Item.Properties()));
        // 生命的融合锭（母神祭坛仪式产物，无 NBT 的纯粹生命结晶）
        lifeFusionIngot = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_FUSION_INGOT_ID),
                        () -> new Item(new Item.Properties()));
        // 生命融合护甲（赤石护甲 2 倍基础数值，融合砧产出，保留升级数据）
        lifeFusionHelmet = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_FUSION_HELMET_ID),
                        () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.HELMET, new Item.Properties()));
        lifeFusionChestplate = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_FUSION_CHESTPLATE_ID),
                        () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        lifeFusionLeggings = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_FUSION_LEGGINGS_ID),
                        () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
        lifeFusionBoots = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_FUSION_BOOTS_ID),
                        () -> new AkaishiLifeFusionArmorItem(ArmorItem.Type.BOOTS, new Item.Properties()));
        // 药剂（永久/突破模板，模板 id + 纯度写 NBT，可堆叠）
        akaishiPotion = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, POTION_ID),
                        () -> new AkaishiPotionItem(new Item.Properties().stacksTo(16)));
        // 排异中和剂（消耗品，可堆叠）
        rejectionSerum = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, REJECTION_SERUM_ID),
                        () -> new AkaishiRejectionSerumItem(new Item.Properties().stacksTo(16)));
        // 器官物品（9 槽位各一，不可堆叠）
        akaishiOrganEye = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_EYE_ID),
                        () -> new AkaishiOrganItem(BodySlot.EYE, new Item.Properties().stacksTo(1)));
        akaishiOrganHeart = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_HEART_ID),
                        () -> new AkaishiOrganItem(BodySlot.HEART, new Item.Properties().stacksTo(1)));
        akaishiOrganLungs = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_LUNGS_ID),
                        () -> new AkaishiOrganItem(BodySlot.LUNGS, new Item.Properties().stacksTo(1)));
        akaishiOrganViscera = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_VISCERA_ID),
                        () -> new AkaishiOrganItem(BodySlot.VISCERA, new Item.Properties().stacksTo(1)));
        akaishiOrganKidneys = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_KIDNEYS_ID),
                        () -> new AkaishiOrganItem(BodySlot.KIDNEYS, new Item.Properties().stacksTo(1)));
        akaishiOrganLeftArm = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_LEFT_ARM_ID),
                        () -> new AkaishiOrganItem(BodySlot.LEFT_ARM, new Item.Properties().stacksTo(1)));
        akaishiOrganRightArm = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_RIGHT_ARM_ID),
                        () -> new AkaishiOrganItem(BodySlot.RIGHT_ARM, new Item.Properties().stacksTo(1)));
        akaishiOrganLeftLeg = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_LEFT_LEG_ID),
                        () -> new AkaishiOrganItem(BodySlot.LEFT_LEG, new Item.Properties().stacksTo(1)));
        akaishiOrganRightLeg = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, ORGAN_RIGHT_LEG_ID),
                        () -> new AkaishiOrganItem(BodySlot.RIGHT_LEG, new Item.Properties().stacksTo(1)));
        // 无线赤能源体系：便捷组件（合成材料）/ 便捷终端（手持遥控面板）
        akaishiWirelessComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, WIRELESS_COMPONENT_ID),
                        () -> new Item(new Item.Properties()));
        akaishiWirelessPortableTerminal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, WIRELESS_PORTABLE_TERMINAL_ID),
                        () -> new AkaishiWirelessPortableTerminalItem(new Item.Properties().stacksTo(1)));
        // 终端身份卡：无线网络认证钥匙（唯一 UUID + 等级，单格堆叠）
        akaishiWirelessIdentityCard = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, WIRELESS_IDENTITY_CARD_ID),
                        () -> new AkaishiWirelessIdentityCardItem(new Item.Properties().stacksTo(1)));
        // 离心结晶：生命离心机分离活化燃料的产物（1 通用衰竭结晶 + 7 对应活化结晶）
        exhaustedCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_exhausted_crystal"),
                        () -> new Item(new Item.Properties()));
        activatedSculkCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_sculk_crystal"),
                        () -> new Item(new Item.Properties()));
        activatedNetherCompoundCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_nether_compound_crystal"),
                        () -> new Item(new Item.Properties()));
        activatedEndMixtureCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_end_mixture_crystal"),
                        () -> new Item(new Item.Properties()));
        activatedAdvancedMixtureCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_advanced_mixture_crystal"),
                        () -> new Item(new Item.Properties()));
        activatedPureCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_pure_crystal"),
                        () -> new Item(new Item.Properties()));
        activatedDragonCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_dragon_crystal"),
                        () -> new Item(new Item.Properties()));
        activatedUltimateMixtureCrystal = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_ultimate_mixture_crystal"),
                        () -> new Item(new Item.Properties()));
        // 活化成分：活化分馏器将活化结晶深度拆分（1 结晶 → 1 活化成分 + 1 衰竭结晶）
        activatedSculkComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_sculk_component"),
                        () -> new Item(new Item.Properties()));
        activatedNetherCompoundComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_nether_compound_component"),
                        () -> new Item(new Item.Properties()));
        activatedEndMixtureComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_end_mixture_component"),
                        () -> new Item(new Item.Properties()));
        activatedAdvancedMixtureComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_advanced_mixture_component"),
                        () -> new Item(new Item.Properties()));
        activatedPureComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_pure_component"),
                        () -> new Item(new Item.Properties()));
        activatedDragonComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_dragon_component"),
                        () -> new Item(new Item.Properties()));
        activatedUltimateMixtureComponent = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_activated_ultimate_mixture_component"),
                        () -> new Item(new Item.Properties()));
        // 聚变反应棒：赤石锭 + 生命精华合成，作为离子体填装器的燃料载体
        fusionRod = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FUSION_ROD_ID),
                        () -> new Item(new Item.Properties()));
        // 等离子体燃料棒（3 种）：填装器将对应等离子体灌入反应棒的产物，带能量 NBT 与类型参数
        mixedPlasmaRod = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, MIXED_PLASMA_ROD_ID),
                        () -> new AkaishiPlasmaRodItem(AkaishiPlasmaRodItem.RodType.MIXED));
        netherPlasmaRod = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, NETHER_PLASMA_ROD_ID),
                        () -> new AkaishiPlasmaRodItem(AkaishiPlasmaRodItem.RodType.NETHER));
        endPlasmaRod = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, END_PLASMA_ROD_ID),
                        () -> new AkaishiPlasmaRodItem(AkaishiPlasmaRodItem.RodType.END));
        // 生命灰烬：聚变堆燃烧副产物（物品输出口推出，生命散热片合成材料）
        lifeAsh = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_ASH_ID),
                        () -> new Item(new Item.Properties()));
        // 聚变散热片（6 档）：插入聚变散热框架的消耗品，效率 5%~20%，耐久 8000
        fusionHeatSinkTier1 = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FUSION_HEAT_SINK_TIER1_ID),
                        () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER1));
        fusionHeatSinkTier2 = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FUSION_HEAT_SINK_TIER2_ID),
                        () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER2));
        fusionHeatSinkTier3 = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FUSION_HEAT_SINK_TIER3_ID),
                        () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER3));
        fusionHeatSinkTier4 = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FUSION_HEAT_SINK_TIER4_ID),
                        () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER4));
        fusionHeatSinkTier5 = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FUSION_HEAT_SINK_TIER5_ID),
                        () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.TIER5));
        fusionHeatSinkLife = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, FUSION_HEAT_SINK_LIFE_ID),
                        () -> new AkaishiFusionHeatSinkItem(FusionHeatSinkQuality.LIFE));
        // 机器升级组件（速度/能量）：装入用电器升级槽，单格堆叠 8 封顶
        machineSpeedUpgrade = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, MACHINE_SPEED_UPGRADE_ID),
                        () -> new AkaishiMachineUpgradeItem(MachineUpgradeType.SPEED));
        machineEnergyUpgrade = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, MACHINE_ENERGY_UPGRADE_ID),
                        () -> new AkaishiMachineUpgradeItem(MachineUpgradeType.ENERGY));
        // ===== 粉末（打粉机产物 / 压缩机原料）=====
        akaishiDust = item(CHISHI_DUST_ID);
        coalDust = item(COAL_DUST_ID);
        ironDust = item(IRON_DUST_ID);
        copperDust = item(COPPER_DUST_ID);
        goldDust = item(GOLD_DUST_ID);
        lapisDust = item(LAPIS_DUST_ID);
        diamondDust = item(DIAMOND_DUST_ID);
        emeraldDust = item(EMERALD_DUST_ID);
        quartzDust = item(QUARTZ_DUST_ID);
        netheriteDust = item(NETHERITE_DUST_ID);
        obsidianDust = item(OBSIDIAN_DUST_ID);
        // ===== 基底（变化器产物）=====
        coolingBase = item(COOLING_BASE_ID);
        coalOreBase = item(COAL_ORE_BASE_ID);
        ironOreBase = item(IRON_ORE_BASE_ID);
        copperOreBase = item(COPPER_ORE_BASE_ID);
        goldOreBase = item(GOLD_ORE_BASE_ID);
        redstoneOreBase = item(REDSTONE_ORE_BASE_ID);
        lapisOreBase = item(LAPIS_ORE_BASE_ID);
        diamondOreBase = item(DIAMOND_ORE_BASE_ID);
        emeraldOreBase = item(EMERALD_ORE_BASE_ID);
        quartzOreBase = item(QUARTZ_ORE_BASE_ID);
        netheriteOreBase = item(NETHERITE_ORE_BASE_ID);
        akaishiOreBase = item(CHISHI_ORE_BASE_ID);
        // Patchouli 手册物品（右键打开对应书籍）
        akaishiDiary = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, AKAISHI_DIARY_ID),
                        () -> new AkaishiBookItem(new ResourceLocation(AkaishiMod.MOD_ID, AKAISHI_DIARY_ID), new Item.Properties()));
        lifeBook = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_BOOK_ID),
                        () -> new AkaishiBookItem(new ResourceLocation(AkaishiMod.MOD_ID, "life_secrets"), new Item.Properties()));
    }

    /** 注册普通物品（generated 模型引用 textures/item/<id>.png） */
    private static RegistrySupplier<Item> item(String id) {
        return RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), () -> new Item(new Item.Properties()));
    }
}
