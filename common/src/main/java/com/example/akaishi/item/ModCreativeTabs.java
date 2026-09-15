package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiCrystalBlocks;
import com.example.akaishi.block.AkaishiDecayBlocks;
import com.example.akaishi.block.AkaishiEnergyBlocks;
import com.example.akaishi.block.AkaishiOreDef;
import com.example.akaishi.block.AkaishiFusionBlocks;
import com.example.akaishi.block.AkaishiLifeBlocks;
import com.example.akaishi.block.AkaishiMinerBlocks;
import com.example.akaishi.block.AkaishiMotherAltarBlocks;
import com.example.akaishi.block.AkaishiMatrixBlocks;
import com.example.akaishi.block.AkaishiReactorBlocks;
import com.example.akaishi.block.AkaishiWirelessBlocks;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.AkaishiMechanicalBlocks;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * 创造模式物品栏分类（按体系拆分为 4 栏）：
 * <ol>
 *   <li>{@link #CHISHI_TAB_ID} 赤石之章：通用材料 / 装备 / 工具 / 采集体系（主栏），帕秋莉手册引用</li>
 *   <li>{@link #MECHANICAL_TAB_ID} 机械改造：机械器官 / 部件模板 / 加工件 / 机械材料 / 3 台机器</li>
 *   <li>{@link #LIFE_TAB_ID} 生命科技：生物器官 / 样本 / 药剂 / 生命能量 / 手术与基因机器</li>
 *   <li>{@link #MACHINES_TAB_ID} 机器与结构：能源 / 管道 / 储罐 / 多方块结构件</li>
 * </ol>
 */
public final class ModCreativeTabs {

    /** 主栏 id，帕秋莉手册 book.json 的 creative_tab 也引用该 id */
    public static final String CHISHI_TAB_ID = "akaishi";
    /** 机械改造栏 id */
    public static final String MECHANICAL_TAB_ID = "akaishi_mechanical";
    /** 生命科技栏 id */
    public static final String LIFE_TAB_ID = "akaishi_life";
    /** 机器与结构栏 id */
    public static final String MACHINES_TAB_ID = "akaishi_machines";

    private ModCreativeTabs() {
    }

    public static void register() {
        var tabs = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.CREATIVE_MODE_TAB);

        // 1. 主栏：赤石之章（通用材料 / 装备 / 工具 / 采集体系）
        tabs.register(new ResourceLocation(AkaishiMod.MOD_ID, CHISHI_TAB_ID),
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.akaishi.akaishi"))
                        .icon(() -> new ItemStack(ModBlocks.get(ModBlocks.ALL_ORES.get(0))))
                        .displayItems((params, output) -> addMainItems(output))
                        .build());

        // 2. 机械改造栏
        tabs.register(new ResourceLocation(AkaishiMod.MOD_ID, MECHANICAL_TAB_ID),
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
                        .title(Component.translatable("itemGroup.akaishi.mechanical"))
                        .icon(() -> new ItemStack(AkaishiMechanicalBlocks.CHISHI_MECHANICAL_TEMPLATE_FACTORY.get()))
                        .displayItems((params, output) -> addMechanicalItems(output))
                        .build());

        // 3. 生命科技栏
        tabs.register(new ResourceLocation(AkaishiMod.MOD_ID, LIFE_TAB_ID),
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 2)
                        .title(Component.translatable("itemGroup.akaishi.life"))
                        .icon(() -> new ItemStack(AkaishiLifeBlocks.CHISHI_SURGERY.get()))
                        .displayItems((params, output) -> addLifeItems(output))
                        .build());

        // 4. 机器与结构栏
        tabs.register(new ResourceLocation(AkaishiMod.MOD_ID, MACHINES_TAB_ID),
                () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 3)
                        .title(Component.translatable("itemGroup.akaishi.machines"))
                        .icon(() -> new ItemStack(AkaishiReactorBlocks.CHISHI_REACTOR_CONTROLLER.get()))
                        .displayItems((params, output) -> addMachinesItems(output))
                        .build());
    }

    // ==================== 栏 1：主栏（通用材料 / 装备 / 工具 / 采集体系） ====================

    private static void addMainItems(CreativeModeTab.Output output) {
        // 16 个赤石矿簇方块
        for (AkaishiOreDef def : ModBlocks.ALL_ORES) {
            output.accept(new ItemStack(ModBlocks.get(def)));
        }
        // 赤石晶
        accept(output, ModItems.akaishiCrystal);
        // 粗制赤石块
        accept(output, ModBlocks.RAW_CHISHI_BLOCK);
        // 赤石精华 + 浓缩精华 + 精华块
        accept(output, ModItems.akaishiEssence);
        accept(output, ModItems.akaishiEssenceCompressed);
        accept(output, ModBlocks.CHISHI_ESSENCE_BLOCK);
        // 赤石锭 + 升级模板
        accept(output, ModItems.akaishiIngot);
        accept(output, ModItems.akaishiUpgradeTemplate);
        // 通用机器组件
        accept(output, ModItems.akaishiMachineComponent);
        accept(output, ModItems.akaishiAdvancedComponent);
        // 升级组件（能源产生/机器通用）
        accept(output, ModItems.akaishiSpeedUpgrade);
        accept(output, ModItems.machineSpeedUpgrade);
        accept(output, ModItems.machineEnergyUpgrade);
        // 调试工具
        accept(output, ModItems.akaishiDebugTool);
        // 赤石装备（头盔/胸甲/护腿/靴子/剑/镐/铲/斧）
        accept(output, ModItems.akaishiHelmet);
        accept(output, ModItems.akaishiChestplate);
        accept(output, ModItems.akaishiLeggings);
        accept(output, ModItems.akaishiBoots);
        accept(output, ModItems.akaishiSword);
        accept(output, ModItems.akaishiPickaxe);
        accept(output, ModItems.akaishiShovel);
        accept(output, ModItems.akaishiAxe);
        // 便捷赤能源储存单元（初级/中级/高级）
        accept(output, ModItems.portableCellBasic);
        accept(output, ModItems.portableCellAdvanced);
        accept(output, ModItems.portableCellSuper);
        // 赤石水晶体系：4 级母岩 + 水晶簇 + 水晶块
        accept(output, AkaishiCrystalBlocks.CHISHI_GEODE_FLAWED);
        accept(output, AkaishiCrystalBlocks.CHISHI_GEODE_NORMAL);
        accept(output, AkaishiCrystalBlocks.CHISHI_GEODE_PRISTINE);
        accept(output, AkaishiCrystalBlocks.CHISHI_GEODE_PERFECT);
        accept(output, AkaishiCrystalBlocks.CHISHI_CRYSTAL_CLUSTER);
        accept(output, AkaishiCrystalBlocks.CHISHI_CRYSTAL_BLOCK);
        // 赤石催化器（4 级）
        accept(output, AkaishiCrystalBlocks.CHISHI_CATALYST_BASIC);
        accept(output, AkaishiCrystalBlocks.CHISHI_CATALYST_MEDIUM);
        accept(output, AkaishiCrystalBlocks.CHISHI_CATALYST_ADVANCED);
        accept(output, AkaishiCrystalBlocks.CHISHI_CATALYST_ULTIMATE);
        // 自动收集器（4 级）
        accept(output, AkaishiCrystalBlocks.CHISHI_COLLECTOR_BASIC);
        accept(output, AkaishiCrystalBlocks.CHISHI_COLLECTOR_MEDIUM);
        accept(output, AkaishiCrystalBlocks.CHISHI_COLLECTOR_ADVANCED);
        accept(output, AkaishiCrystalBlocks.CHISHI_COLLECTOR_ULTIMATE);
        // 赤石饰品（Curios）
        accept(output, ModItems.satiationCharm);
        accept(output, ModItems.huntingRing);
        accept(output, ModItems.gatheringBracelet);
        accept(output, ModItems.fireNecklace);
        accept(output, ModItems.blastCharm);
        accept(output, ModItems.antidoteBracelet);
        accept(output, ModItems.witherCharm);
        // 散热片（5 品质 + 终极）
        accept(output, ModItems.heatSinkPoor);
        accept(output, ModItems.heatSinkNormal);
        accept(output, ModItems.heatSinkGood);
        accept(output, ModItems.heatSinkFine);
        accept(output, ModItems.heatSinkExquisite);
        accept(output, ModItems.heatSinkUltimate);
        // 粉末体系（打粉机产物 / 压缩机原料 / 变化器原料）
        accept(output, ModItems.akaishiDust);
        accept(output, ModItems.coalDust);
        accept(output, ModItems.ironDust);
        accept(output, ModItems.copperDust);
        accept(output, ModItems.goldDust);
        accept(output, ModItems.lapisDust);
        accept(output, ModItems.diamondDust);
        accept(output, ModItems.emeraldDust);
        accept(output, ModItems.quartzDust);
        accept(output, ModItems.netheriteDust);
        accept(output, ModItems.obsidianDust);
        // 基底体系（变化器产物）
        accept(output, ModItems.coolingBase);
        accept(output, ModItems.coalOreBase);
        accept(output, ModItems.ironOreBase);
        accept(output, ModItems.copperOreBase);
        accept(output, ModItems.goldOreBase);
        accept(output, ModItems.redstoneOreBase);
        accept(output, ModItems.lapisOreBase);
        accept(output, ModItems.diamondOreBase);
        accept(output, ModItems.emeraldOreBase);
        accept(output, ModItems.quartzOreBase);
        accept(output, ModItems.netheriteOreBase);
        accept(output, ModItems.akaishiOreBase);
        // 山羊头旗帜图案（织布机图案物）
        accept(output, ModItems.goatSkullBannerPattern);
        // 手册
        accept(output, ModItems.akaishiDiary);
        accept(output, ModItems.lifeBook);
        accept(output, ModItems.geneBook);
    }

    // ==================== 栏 2：机械改造 ====================

    private static void addMechanicalItems(CreativeModeTab.Output output) {
        // 生产链三台机器
        accept(output, AkaishiMechanicalBlocks.CHISHI_MECHANICAL_TEMPLATE_FACTORY);
        accept(output, AkaishiMechanicalBlocks.CHISHI_MECHANICAL_PROCESSING_FACTORY);
        accept(output, AkaishiMechanicalBlocks.CHISHI_MECHANICAL_ASSEMBLY_STATION);
        // 机械器官（9 槽位）
        accept(output, ModItems.mechanicalEye);
        accept(output, ModItems.mechanicalHeart);
        accept(output, ModItems.mechanicalLungs);
        accept(output, ModItems.mechanicalViscera);
        accept(output, ModItems.mechanicalKidneys);
        accept(output, ModItems.mechanicalLeftArm);
        accept(output, ModItems.mechanicalRightArm);
        accept(output, ModItems.mechanicalLeftLeg);
        accept(output, ModItems.mechanicalRightLeg);
        // 通用部件塑形模板 + 部位模板 + 加工部件
        accept(output, ModItems.genericPartMould);
        accept(output, ModItems.mechanicalPartTemplate);
        accept(output, ModItems.mechanicalProcessedPart);
        // 机械材料（9 种）
        accept(output, ModItems.redstoneAlloyIngot);
        accept(output, ModItems.ceramicCompositePlate);
        accept(output, ModItems.resistantSteelIngot);
        accept(output, ModItems.precisionAlloyIngot);
        accept(output, ModItems.polymerizedRedstoneCore);
        accept(output, ModItems.bioCeramicPlate);
        accept(output, ModItems.refinedCore);
        accept(output, ModItems.alloySteelIngot);
        accept(output, ModItems.psionicCompositeIngot);
    }

    // ==================== 栏 3：生命科技 ====================

    private static void addLifeItems(CreativeModeTab.Output output) {
        // 生物器官（9 槽位）
        accept(output, ModItems.akaishiOrganEye);
        accept(output, ModItems.akaishiOrganHeart);
        accept(output, ModItems.akaishiOrganLungs);
        accept(output, ModItems.akaishiOrganViscera);
        accept(output, ModItems.akaishiOrganKidneys);
        accept(output, ModItems.akaishiOrganLeftArm);
        accept(output, ModItems.akaishiOrganRightArm);
        accept(output, ModItems.akaishiOrganLeftLeg);
        accept(output, ModItems.akaishiOrganRightLeg);
        // 生命能量体系：管道（基础/中级/高级/超级）+ 聚合转换器 + 转换架构
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE_ADVANCED);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE_ELITE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE_ULTIMATE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_PIPE_INFINITE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_AGGREGATION_CONVERTER);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_CONVERSION_ARCHITECTURE);
        // 生命能量储存器（基础/高级/超级）+ 串联器
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_CELL);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_CELL_ADVANCED);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_CELL_SUPER);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_CELL_SERIALIZER);
        // 生命能量发射器 + 权杖（权杖两步式绑定发射目标）
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_ENERGY_EMITTER);
        accept(output, ModItems.lifeEnergyWand);
        // 生命能量无线终端族
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_SHELL);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CORE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CONTROLLER);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_STRUCTURE_GLASS);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_TERMINAL);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_INPUT_PORT);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_OUTPUT_PORT);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_DIM_BRIDGE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CHUNK_LOADER);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_CHUNK_RANGE);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_INPUT_LOSS);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_WIRELESS_OUTPUT_LOSS);
        accept(output, ModItems.akaishiLifeWirelessPortableTerminal);
        // 生命能量提纯器 + 固态物
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_PURIFIER);
        accept(output, ModItems.akaishiLifeEssenceSolid);
        // 生命处理机器族
        accept(output, ModBlocks.CHISHI_LIFE_ACTIVATOR);
        accept(output, ModBlocks.CHISHI_LIFE_CENTRIFUGE);
        accept(output, ModBlocks.CHISHI_ITEM_RECONSTRUCTOR);
        accept(output, ModBlocks.CHISHI_ACTIVATED_FRACTIONATOR);
        // 生命科技核心机器
        accept(output, AkaishiLifeBlocks.CHISHI_BODY_SCANNER);
        accept(output, AkaishiLifeBlocks.CHISHI_GENE_MANAGER);
        accept(output, AkaishiLifeBlocks.CHISHI_GENE_ANALYZER);
        accept(output, AkaishiLifeBlocks.CHISHI_TRANSGENE_FACTORY);
        accept(output, AkaishiLifeBlocks.CHISHI_CULTIVATOR);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_STRUCT);
        accept(output, AkaishiLifeBlocks.CHISHI_LIFE_BREEDER);
        accept(output, AkaishiLifeBlocks.CHISHI_TRAIT_REFORGER);
        accept(output, AkaishiLifeBlocks.CHISHI_SURGERY);
        accept(output, AkaishiLifeBlocks.CHISHI_POTION_TABLE);
        // 药剂
        accept(output, ModItems.akaishiPotion);
        accept(output, ModItems.rejectionSerum);
        // 仓储
        accept(output, AkaishiLifeBlocks.CHISHI_ORGAN_VAULT);
        accept(output, AkaishiLifeBlocks.CHISHI_SAMPLE_VAULT);
        accept(output, AkaishiLifeBlocks.CHISHI_POTION_CABINET);
        // 母神祭坛体系
        accept(output, AkaishiMotherAltarBlocks.CHISHI_MOTHER_ALTAR);
        accept(output, AkaishiMotherAltarBlocks.CHISHI_ALTAR_STONE);
        accept(output, AkaishiMotherAltarBlocks.CRYING_OBSIDIAN_RED);
        // 样本 / 基因 / 胚胎
        accept(output, ModItems.sampleCollector);
        accept(output, ModItems.lifeSample);
        accept(output, ModItems.lifeEmbryo);
        accept(output, ModItems.geneSequence);
        // 转基因/培育产物
        accept(output, ModItems.akaishiWitherSeed);
        accept(output, ModItems.akaishiWitherCondensate);
        accept(output, ModItems.akaishiBlazeSeed);
        accept(output, ModItems.akaishiBlazeCondensate);
        accept(output, ModItems.akaishiCurseVineSeed);
        accept(output, ModItems.akaishiCurseBlossom);
        // 离心结晶 + 活化成分
        accept(output, ModItems.exhaustedCrystal);
        accept(output, ModItems.activatedSculkCrystal);
        accept(output, ModItems.activatedNetherCompoundCrystal);
        accept(output, ModItems.activatedEndMixtureCrystal);
        accept(output, ModItems.activatedAdvancedMixtureCrystal);
        accept(output, ModItems.activatedPureCrystal);
        accept(output, ModItems.activatedDragonCrystal);
        accept(output, ModItems.activatedUltimateMixtureCrystal);
        accept(output, ModItems.activatedSculkComponent);
        accept(output, ModItems.activatedNetherCompoundComponent);
        accept(output, ModItems.activatedEndMixtureComponent);
        accept(output, ModItems.activatedAdvancedMixtureComponent);
        accept(output, ModItems.activatedPureComponent);
        accept(output, ModItems.activatedDragonComponent);
        accept(output, ModItems.activatedUltimateMixtureComponent);
        // 生命融合体系
        accept(output, ModItems.lifeFusionIngot);
        accept(output, ModBlocks.CHISHI_LIFE_FUSION_ANVIL);
        accept(output, ModItems.lifeFusionHelmet);
        accept(output, ModItems.lifeFusionChestplate);
        accept(output, ModItems.lifeFusionLeggings);
        accept(output, ModItems.lifeFusionBoots);
    }

    // ==================== 栏 4：机器与结构 ====================

    private static void addMachinesItems(CreativeModeTab.Output output) {
        // 矿石→锭基础机器
        accept(output, ModBlocks.CHISHI_PURIFIER);
        accept(output, ModBlocks.CHISHI_ADVANCED_PURIFIER);
        // 单槽处理机器（植物培养机/压缩机/打粉机/变化器）
        accept(output, ModBlocks.CHISHI_PLANT_CULTIVATOR);
        accept(output, ModBlocks.CHISHI_COMPRESSOR);
        accept(output, ModBlocks.CHISHI_PULVERIZER);
        accept(output, ModBlocks.CHISHI_TRANSFORMER);
        // 赤能源体系：电池 3 档 + 管道 4 档
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_CELL_BASIC);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_CELL_ADVANCED);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_CELL_SUPER);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE_ADVANCED);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE_ELITE);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE_ULTIMATE);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PIPE_INFINITE);
        // 赤能源发生机 + 小型组合结构 + 聚合器 + 串联器 + 超级发生器架构核心
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_GENERATOR);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_ASSEMBLY);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_AGGREGATOR);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_CELL_SERIALIZER);
        accept(output, AkaishiEnergyBlocks.CHISHI_SUPER_GENERATOR_CORE);
        // 物品管道（4 级）
        accept(output, ModBlocks.CHISHI_ITEM_PIPE);
        accept(output, ModBlocks.CHISHI_ITEM_PIPE_ADVANCED);
        accept(output, ModBlocks.CHISHI_ITEM_PIPE_ELITE);
        accept(output, ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE);
        accept(output, ModBlocks.CHISHI_ITEM_PIPE_INFINITE);
        // 液体管道 + 废料管道 + 多重废液管道 + 储罐 3 档
        accept(output, ModBlocks.CHISHI_FLUID_PIPE);
        accept(output, ModBlocks.CHISHI_EXHAUSTED_PIPE);
        accept(output, ModBlocks.CHISHI_MULTI_FLUID_WASTE_PIPE);
        accept(output, ModBlocks.CHISHI_FLUID_TANK_BASIC);
        accept(output, ModBlocks.CHISHI_FLUID_TANK_ADVANCED);
        accept(output, ModBlocks.CHISHI_FLUID_TANK_SUPER);
        // 能源加工链：液化 + 加工器 + 装罐机 + 混合器 + 燃料罐
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_LIQUEFIER);
        accept(output, AkaishiEnergyBlocks.CHISHI_ENERGY_PROCESSOR);
        accept(output, ModBlocks.CHISHI_FUEL_CANNER);
        accept(output, ModBlocks.CHISHI_FUEL_MIXER);
        accept(output, ModItems.fuelCell);
        // 燃料原料
        accept(output, ModItems.endMixture);
        accept(output, ModItems.dragonMixture);
        accept(output, ModItems.sculkLifeform);
        // 反应堆体系
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_SHELL);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_STRUCTURE_GLASS);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_CONTROLLER);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_PORT);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_ENERGY_OUTPUT);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_WASTE_PORT);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_ROD);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_COOLER);
        accept(output, AkaishiReactorBlocks.CHISHI_REACTOR_CORE);
        accept(output, AkaishiReactorBlocks.CHISHI_EXHAUSTED_BARREL);
        // 聚变燃料体系
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_FUEL_AGGREGATOR);
        accept(output, ModBlocks.CHISHI_PLASMA_FILLER);
        accept(output, ModBlocks.CHISHI_PLASMA_PIPE);
        accept(output, ModBlocks.CHISHI_PLASMA_TANK);
        accept(output, ModItems.fusionRod);
        accept(output, ModItems.mixedPlasmaRod);
        accept(output, ModItems.netherPlasmaRod);
        accept(output, ModItems.endPlasmaRod);
        // 发生器矩阵 / 提纯矩阵 / 生命转换矩阵
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_CASING);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_STRUCTURE_GLASS);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_CONTROLLER_BASIC);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_ENERGY_OUTPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_GEN_FUEL_INPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_MATRIX_CASING);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_MATRIX_CONTROLLER);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_ENERGY_INPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_ITEM_INPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_PURIFIER_ITEM_OUTPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_CASING);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_STRUCTURE_GLASS);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_CONTROLLER);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_ENERGY_INPUT);
        accept(output, AkaishiMatrixBlocks.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT);
        // 无线赤能源体系（方块族 + 便捷件）
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_SHELL);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_STRUCTURE_GLASS);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_TERMINAL);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_SECURITY);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_CORE);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_CONTROLLER);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_INPUT_PORT);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_OUTPUT_PORT);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_DIM_BRIDGE);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_CHUNK_LOADER);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_CHUNK_RANGE);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_INPUT_LOSS);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_OUTPUT_LOSS);
        accept(output, AkaishiWirelessBlocks.CHISHI_WIRELESS_TRANSMIT_FRAME);
        accept(output, ModItems.akaishiWirelessComponent);
        accept(output, ModItems.akaishiWirelessPortableTerminal);
        accept(output, ModItems.akaishiWirelessIdentityCard);
        // 聚变堆体系
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_SHELL);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_STRUCTURE_GLASS);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_INSULATION);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_CONTROLLER);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_CORE);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_COOLER_FRAME);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_FUEL_FRAME);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_EFFICIENCY_FRAME);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_ENERGY_OUTPUT);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_ITEM_INPUT);
        accept(output, AkaishiFusionBlocks.CHISHI_FUSION_ITEM_OUTPUT);
        accept(output, ModItems.lifeAsh);
        accept(output, ModItems.fusionHeatSinkTier1);
        accept(output, ModItems.fusionHeatSinkTier2);
        accept(output, ModItems.fusionHeatSinkTier3);
        accept(output, ModItems.fusionHeatSinkTier4);
        accept(output, ModItems.fusionHeatSinkTier5);
        accept(output, ModItems.fusionHeatSinkLife);
        // 矿机体系
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_CONTROLLER_BASIC);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_CONTROLLER_ADVANCED);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_CONTROLLER_SUPER);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_CONTROLLER_ULTIMATE);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_FRAME);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_UPGRADE_FRAME);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_PORT);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_DRILL_BIT);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_FRAME_EXTERNAL);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_ENERGY_INPUT);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_ITEM_OUTPUT);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_SPEED_UPGRADE_BLOCK);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_FORTUNE_UPGRADE_BLOCK);
        accept(output, AkaishiMinerBlocks.CHISHI_MINER_STORAGE_UPGRADE_BLOCK);
        // 衰竭净化塔 + 衰竭全家桶（岩石/木/地表组）
        accept(output, AkaishiDecayBlocks.CHISHI_DECAY_PURIFIER);
        accept(output, ModBlocks.CHISHI_DECAY_SOIL);
        accept(output, ModBlocks.CHISHI_DECAY_LOG);
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_COBBLESTONE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE_BRICKS.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE_BRICK_STAIRS.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE_BRICK_SLAB.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STONE_BRICK_WALL.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_PLANKS.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_STAIRS.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_SLAB.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_FENCE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_FENCE_GATE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_DOOR.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_TRAPDOOR.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_BUTTON.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_PRESSURE_PLATE.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_SAND.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_GRAVEL.get()));
        output.accept(new ItemStack(AkaishiDecayBlocks.CHISHI_DECAY_GRASS_BLOCK.get()));
        // 创造模式能量源（测试用）
        accept(output, AkaishiEnergyBlocks.CHISHI_CREATIVE_ENERGY_CELL);
        accept(output, AkaishiLifeBlocks.CHISHI_CREATIVE_LIFE_CELL);
    }

    /** 判空后把注册内容（物品/方块，均实现 ItemLike）放入创造标签（注册完成前为 null，防御性跳过） */
    private static void accept(CreativeModeTab.Output output, RegistrySupplier<? extends ItemLike> supplier) {
        if (supplier != null) {
            output.accept(new ItemStack(supplier.get()));
        }
    }
}