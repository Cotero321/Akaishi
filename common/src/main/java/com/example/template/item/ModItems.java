package com.example.template.item;

import com.example.template.TemplateMod;
import com.example.template.energy.PortableCellTier;
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
    /** 能源产生升级组件：装配到发生器（单块/多方块中心），每个提升一倍产能速度、减少 1% 产出，最多 10 个 */
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
    }
}
