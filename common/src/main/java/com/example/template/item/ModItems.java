package com.example.template.item;

import com.example.template.TemplateMod;
import com.example.template.energy.PortableCellTier;
import com.example.template.item.curio.ChishiAntidoteBracelet;
import com.example.template.item.curio.ChishiBlastCharm;
import com.example.template.item.curio.ChishiFireNecklace;
import com.example.template.item.curio.ChishiGatheringBracelet;
import com.example.template.item.curio.ChishiHuntingRing;
import com.example.template.item.curio.ChishiSatiationCharm;
import com.example.template.item.curio.ChishiWitherCharm;
import com.example.template.life.body.BodySlot;
import com.example.template.life.organ.ChishiOrganItem;
import com.example.template.life.potion.ChishiPotionItem;
import com.example.template.life.sample.ChishiLifeSampleItem;
import com.example.template.life.sample.ChishiSampleCollectorItem;
import com.example.template.life.sequence.ChishiGeneSequenceItem;
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

    public static final String CHISHI_CRYSTAL_ID = "chishi_crystal";
    public static final String CHISHI_ESSENCE_ID = "chishi_essence";
    public static final String ESSENCE_COMPRESSED_ID = "chishi_essence_compressed";
    public static final String MACHINE_COMPONENT_ID = "chishi_machine_component";
    public static final String ADVANCED_COMPONENT_ID = "chishi_advanced_component";
    public static final String DEBUG_TOOL_ID = "chishi_debug_tool";
    public static final String CHISHI_INGOT_ID = "chishi_ingot";
    public static final String UPGRADE_TEMPLATE_ID = "chishi_upgrade_template";
    public static final String HELMET_ID = "chishi_helmet";
    public static final String CHESTPLATE_ID = "chishi_chestplate";
    public static final String LEGGINGS_ID = "chishi_leggings";
    public static final String BOOTS_ID = "chishi_boots";
    public static final String SWORD_ID = "chishi_sword";
    public static final String PICKAXE_ID = "chishi_pickaxe";
    public static final String SHOVEL_ID = "chishi_shovel";
    public static final String AXE_ID = "chishi_axe";
    public static final String PORTABLE_CELL_BASIC_ID = "portable_chishi_cell_basic";
    public static final String PORTABLE_CELL_ADVANCED_ID = "portable_chishi_cell_advanced";
    public static final String PORTABLE_CELL_SUPER_ID = "portable_chishi_cell_super";
    /** 能源产生升级组件：装配到发生器（单块/多方块中心），每个提升 1.75 倍产能速度、减少 1% 产出，最多 10 个（满配约 242 倍） */
    public static final String SPEED_UPGRADE_ID = "chishi_speed_upgrade";
    /** 生命能量固态物：由生命能量提纯器以 1000 生命能量 + 10M 赤能源固化，生命能源体系的基础材料 */
    public static final String LIFE_ESSENCE_SOLID_ID = "chishi_life_essence_solid";
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
    /** 赤石饱食护符（charm 槽）：消耗赤能源补充饱和度 */
    public static final String SATIATION_CHARM_ID = "chishi_satiation_charm";
    /** 赤石狩猎指环（ring 槽）：击杀生物概率掉落赤石晶 */
    public static final String HUNTING_RING_ID = "chishi_hunting_ring";
    /** 赤石采集手环（hands 槽）：挖掘方块概率掉落赤石晶 */
    public static final String GATHERING_BRACELET_ID = "chishi_gathering_bracelet";
    /** 赤石防火吊坠（necklace 槽）：消耗赤能源维持防火 */
    public static final String FIRE_NECKLACE_ID = "chishi_fire_necklace";
    /** 赤石防爆护符（body 槽）：消耗赤能源抵消爆炸伤害 */
    public static final String BLAST_CHARM_ID = "chishi_blast_charm";
    /** 赤石净化手镯（bracelet 槽）：消耗赤能源移除中毒 */
    public static final String ANTIDOTE_BRACELET_ID = "chishi_antidote_bracelet";
    /** 赤石凋零护符（belt 槽）：消耗赤能源移除凋零 */
    public static final String WITHER_CHARM_ID = "chishi_wither_charm";
    /** 样本采集器：随身生命能量容器，右键生物抽取生命样本，右键生命能量方块充能 */
    public static final String SAMPLE_COLLECTOR_ID = "chishi_sample_collector";
    /** 生命样本：从活体生物抽取的遗传物质，生命分析台原料 */
    public static final String LIFE_SAMPLE_ID = "chishi_life_sample";
    /** 基因序列片段：纯度 100 样本在分析台解构的产物，生命结构台原料 */
    public static final String GENE_SEQUENCE_ID = "chishi_gene_sequence";
    /** 器官物品 ID（9 槽位各一），基因来源/品质存 NBT */
    public static final String ORGAN_EYE_ID = "chishi_organ_eye";
    /** 药剂（单物品承载永久/突破模板，模板 id 与纯度写 NBT） */
    public static final String POTION_ID = "chishi_potion";
    public static final String ORGAN_HEART_ID = "chishi_organ_heart";
    public static final String ORGAN_LUNGS_ID = "chishi_organ_lungs";
    public static final String ORGAN_VISCERA_ID = "chishi_organ_viscera";
    public static final String ORGAN_KIDNEYS_ID = "chishi_organ_kidneys";
    public static final String ORGAN_LEFT_ARM_ID = "chishi_organ_left_arm";
    public static final String ORGAN_RIGHT_ARM_ID = "chishi_organ_right_arm";
    public static final String ORGAN_LEFT_LEG_ID = "chishi_organ_left_leg";
    public static final String ORGAN_RIGHT_LEG_ID = "chishi_organ_right_leg";
    /** 赤石晶延迟注册引用（注册完成后可用） */
    public static RegistrySupplier<Item> chishiCrystal;
    /** 赤石精华延迟注册引用 */
    public static RegistrySupplier<Item> chishiEssence;
    /** 浓缩赤石精华：由 9 个赤石精华压缩而成，赤石科技的高级材料 */
    public static RegistrySupplier<Item> chishiEssenceCompressed;
    /** 赤红机器组件：赤石科技设备的通用部件 */
    public static RegistrySupplier<Item> chishiMachineComponent;
    /** 赤红高级机械组件：以浓缩精华与钻石强化，用于赤能源发生机等高端设备 */
    public static RegistrySupplier<Item> chishiAdvancedComponent;
    /** 赤能源配置器：切换管道方向模式（正常/推/拉）与断开单侧连接 */
    public static RegistrySupplier<Item> chishiDebugTool;
    /** 赤石锭：由赤石能量聚合器以 10M 赤能源 + 下界合金锭聚合而成，赤石装备的核心材料 */
    public static RegistrySupplier<Item> chishiIngot;
    /** 赤红升级模板：赤红升级台的消耗品，用于为赤石装备应用升级 */
    public static RegistrySupplier<Item> chishiUpgradeTemplate;
    /** 赤石头盔 */
    public static RegistrySupplier<Item> chishiHelmet;
    /** 赤石胸甲 */
    public static RegistrySupplier<Item> chishiChestplate;
    /** 赤石护腿 */
    public static RegistrySupplier<Item> chishiLeggings;
    /** 赤石靴子 */
    public static RegistrySupplier<Item> chishiBoots;
    /** 赤石剑 */
    public static RegistrySupplier<Item> chishiSword;
    /** 赤石镐 */
    public static RegistrySupplier<Item> chishiPickaxe;
    /** 赤石铲 */
    public static RegistrySupplier<Item> chishiShovel;
    /** 赤石斧 */
    public static RegistrySupplier<Item> chishiAxe;
    /** 便携赤能源储存单元（初级） */
    public static RegistrySupplier<Item> portableCellBasic;
    /** 便携赤能源储存单元（中级） */
    public static RegistrySupplier<Item> portableCellAdvanced;
    /** 便携赤能源储存单元（高级） */
    public static RegistrySupplier<Item> portableCellSuper;
    /** 能源产生升级组件 */
    public static RegistrySupplier<Item> chishiSpeedUpgrade;
    /** 生命能量固态物 */
    public static RegistrySupplier<Item> chishiLifeEssenceSolid;
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
    /** 药剂（永久/突破模板共用） */
    public static RegistrySupplier<Item> chishiPotion;
    /** 器官物品（9 槽位各一） */
    public static RegistrySupplier<Item> chishiOrganEye;
    public static RegistrySupplier<Item> chishiOrganHeart;
    public static RegistrySupplier<Item> chishiOrganLungs;
    public static RegistrySupplier<Item> chishiOrganViscera;
    public static RegistrySupplier<Item> chishiOrganKidneys;
    public static RegistrySupplier<Item> chishiOrganLeftArm;
    public static RegistrySupplier<Item> chishiOrganRightArm;
    public static RegistrySupplier<Item> chishiOrganLeftLeg;
    public static RegistrySupplier<Item> chishiOrganRightLeg;

    private ModItems() {
    }

    public static void register() {
        chishiCrystal = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, CHISHI_CRYSTAL_ID),
                        () -> new Item(new Item.Properties()));
        chishiEssence = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, CHISHI_ESSENCE_ID),
                        () -> new Item(new Item.Properties()));
        chishiEssenceCompressed = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ESSENCE_COMPRESSED_ID),
                        () -> new Item(new Item.Properties()));
        chishiMachineComponent = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, MACHINE_COMPONENT_ID),
                        () -> new Item(new Item.Properties()));
        chishiAdvancedComponent = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ADVANCED_COMPONENT_ID),
                        () -> new Item(new Item.Properties()));
        chishiDebugTool = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, DEBUG_TOOL_ID),
                        () -> new ChishiDebugTool());
        // 赤石锭与升级模板
        chishiIngot = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, CHISHI_INGOT_ID),
                        () -> new Item(new Item.Properties()));
        chishiUpgradeTemplate = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, UPGRADE_TEMPLATE_ID),
                        () -> new Item(new Item.Properties()));
        // 赤石装备（基础属性 = 下界合金 × 1.25，可升级）
        chishiHelmet = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, HELMET_ID),
                        () -> new ChishiArmorItem(ArmorItem.Type.HELMET, new Item.Properties()));
        chishiChestplate = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, CHESTPLATE_ID),
                        () -> new ChishiArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));
        chishiLeggings = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, LEGGINGS_ID),
                        () -> new ChishiArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));
        chishiBoots = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, BOOTS_ID),
                        () -> new ChishiArmorItem(ArmorItem.Type.BOOTS, new Item.Properties()));
        // 攻击伤害修饰符 7：4(下界合金材质加成) + 7 = 11 = 下界合金剑 9 × 1.25
        chishiSword = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, SWORD_ID),
                        () -> new ChishiSwordItem(Tiers.NETHERITE, 7, -2.0F, new Item.Properties()));
        // 赤石工具（属性 = 下界合金对应工具 × 1.25，见各类注释）
        chishiPickaxe = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, PICKAXE_ID),
                        () -> new ChishiPickaxeItem(new Item.Properties()));
        chishiShovel = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, SHOVEL_ID),
                        () -> new ChishiShovelItem(new Item.Properties()));
        chishiAxe = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, AXE_ID),
                        () -> new ChishiAxeItem(new Item.Properties()));
        // 便携赤能源储存单元（初级/中级/高级）
        portableCellBasic = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, PORTABLE_CELL_BASIC_ID),
                        () -> new ChishiPortableEnergyCell(PortableCellTier.BASIC, new Item.Properties()));
        portableCellAdvanced = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, PORTABLE_CELL_ADVANCED_ID),
                        () -> new ChishiPortableEnergyCell(PortableCellTier.ADVANCED, new Item.Properties()));
        portableCellSuper = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, PORTABLE_CELL_SUPER_ID),
                        () -> new ChishiPortableEnergyCell(PortableCellTier.SUPER, new Item.Properties()));
        chishiSpeedUpgrade = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, SPEED_UPGRADE_ID),
                        () -> new Item(new Item.Properties()));
        // 生命能量固态物（生命能量提纯器产出，生命能源体系基础材料）
        chishiLifeEssenceSolid = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, LIFE_ESSENCE_SOLID_ID),
                        () -> new Item(new Item.Properties()));
        // 末地混合物（工作台合成，液化装置 → 末地混合燃料）
        endMixture = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, END_MIXTURE_ID),
                        () -> new Item(new Item.Properties()));
        // 巨龙混合物（工作台合成，液化装置 → 末地巨龙燃料）
        dragonMixture = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, DRAGON_MIXTURE_ID),
                        () -> new Item(new Item.Properties()));
        // 幽匿生命体（工作台合成，液化装置 → 幽匿生命燃料）
        sculkLifeform = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, SCULK_LIFEFORM_ID),
                        () -> new Item(new Item.Properties()));
        // 燃料罐（装罐机充装液体燃料，空罐可堆叠 64）
        fuelCell = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, FUEL_CELL_ID),
                        () -> new ChishiFuelCellItem(new Item.Properties()));
        // 散热片（5 品质）：插入反应堆散热组件，消耗品带耐久
        heatSinkPoor = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, HEAT_SINK_POOR_ID),
                        () -> new ChishiHeatSinkItem(HeatSinkQuality.POOR));
        heatSinkNormal = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, HEAT_SINK_NORMAL_ID),
                        () -> new ChishiHeatSinkItem(HeatSinkQuality.NORMAL));
        heatSinkGood = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, HEAT_SINK_GOOD_ID),
                        () -> new ChishiHeatSinkItem(HeatSinkQuality.GOOD));
        heatSinkFine = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, HEAT_SINK_FINE_ID),
                        () -> new ChishiHeatSinkItem(HeatSinkQuality.FINE));
        heatSinkExquisite = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, HEAT_SINK_EXQUISITE_ID),
                        () -> new ChishiHeatSinkItem(HeatSinkQuality.EXQUISITE));
        // 赤石饰品（Curios 槽位：charm/ring/hands/necklace/body/bracelet/belt）
        satiationCharm = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, SATIATION_CHARM_ID),
                        () -> new ChishiSatiationCharm(new Item.Properties()));
        huntingRing = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, HUNTING_RING_ID),
                        () -> new ChishiHuntingRing(new Item.Properties()));
        gatheringBracelet = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, GATHERING_BRACELET_ID),
                        () -> new ChishiGatheringBracelet(new Item.Properties()));
        fireNecklace = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, FIRE_NECKLACE_ID),
                        () -> new ChishiFireNecklace(new Item.Properties()));
        blastCharm = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, BLAST_CHARM_ID),
                        () -> new ChishiBlastCharm(new Item.Properties()));
        antidoteBracelet = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ANTIDOTE_BRACELET_ID),
                        () -> new ChishiAntidoteBracelet(new Item.Properties()));
        witherCharm = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, WITHER_CHARM_ID),
                        () -> new ChishiWitherCharm(new Item.Properties()));
        // 生命科技：样本采集器（不可堆叠）+ 生命样本（可堆叠 64）
        sampleCollector = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, SAMPLE_COLLECTOR_ID),
                        () -> new ChishiSampleCollectorItem(new Item.Properties().stacksTo(1)));
        lifeSample = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, LIFE_SAMPLE_ID),
                        () -> new ChishiLifeSampleItem(new Item.Properties()));
        geneSequence = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, GENE_SEQUENCE_ID),
                        () -> new ChishiGeneSequenceItem(new Item.Properties()));
        // 药剂（永久/突破模板，模板 id + 纯度写 NBT，可堆叠）
        chishiPotion = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, POTION_ID),
                        () -> new ChishiPotionItem(new Item.Properties().stacksTo(16)));
        // 器官物品（9 槽位各一，不可堆叠）
        chishiOrganEye = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_EYE_ID),
                        () -> new ChishiOrganItem(BodySlot.EYE, new Item.Properties().stacksTo(1)));
        chishiOrganHeart = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_HEART_ID),
                        () -> new ChishiOrganItem(BodySlot.HEART, new Item.Properties().stacksTo(1)));
        chishiOrganLungs = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_LUNGS_ID),
                        () -> new ChishiOrganItem(BodySlot.LUNGS, new Item.Properties().stacksTo(1)));
        chishiOrganViscera = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_VISCERA_ID),
                        () -> new ChishiOrganItem(BodySlot.VISCERA, new Item.Properties().stacksTo(1)));
        chishiOrganKidneys = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_KIDNEYS_ID),
                        () -> new ChishiOrganItem(BodySlot.KIDNEYS, new Item.Properties().stacksTo(1)));
        chishiOrganLeftArm = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_LEFT_ARM_ID),
                        () -> new ChishiOrganItem(BodySlot.LEFT_ARM, new Item.Properties().stacksTo(1)));
        chishiOrganRightArm = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_RIGHT_ARM_ID),
                        () -> new ChishiOrganItem(BodySlot.RIGHT_ARM, new Item.Properties().stacksTo(1)));
        chishiOrganLeftLeg = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_LEFT_LEG_ID),
                        () -> new ChishiOrganItem(BodySlot.LEFT_LEG, new Item.Properties().stacksTo(1)));
        chishiOrganRightLeg = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.ITEM)
                .register(new ResourceLocation(TemplateMod.MOD_ID, ORGAN_RIGHT_LEG_ID),
                        () -> new ChishiOrganItem(BodySlot.RIGHT_LEG, new Item.Properties().stacksTo(1)));
    }
}
