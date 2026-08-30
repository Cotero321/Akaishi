package com.example.template.item;

import com.example.template.TemplateMod;
import com.example.template.block.ChishiOreDef;
import com.example.template.block.ModBlocks;
import dev.architectury.registry.registries.RegistrarManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式物品栏分类“赤石之章”。
 * 集中收纳赤石科技相关的方块与物品，分类 id 同时供帕秋莉手册引用。
 */
public final class ModCreativeTabs {

    /** 分类 id，帕秋莉手册 book.json 的 creative_tab 也引用该 id */
    public static final String CHISHI_TAB_ID = "chishi";

    private ModCreativeTabs() {
    }

    public static void register() {
        // 注册创造模式物品栏分类（惰性构建，displayItems 在游戏启动后回调）
        RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.CREATIVE_MODE_TAB)
                .register(new ResourceLocation(TemplateMod.MOD_ID, CHISHI_TAB_ID),
                        () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                                .title(Component.translatable("itemGroup.template_mod.chishi"))
                                .icon(() -> new ItemStack(ModBlocks.get(ModBlocks.ALL_ORES.get(0))))
                                .displayItems((params, output) -> {
                                    // 16 个赤石矿簇方块
                                    for (ChishiOreDef def : ModBlocks.ALL_ORES) {
                                        output.accept(new ItemStack(ModBlocks.get(def)));
                                    }
                                    // 赤石晶（注册完成后才可用，防御性判空）
                                    if (ModItems.chishiCrystal != null) {
                                        output.accept(new ItemStack(ModItems.chishiCrystal.get()));
                                    }
                                    // 粗制赤石块 + 赤石提纯器
                                    if (ModBlocks.RAW_CHISHI_BLOCK != null) {
                                        output.accept(new ItemStack(ModBlocks.RAW_CHISHI_BLOCK.get()));
                                    }
                                    if (ModBlocks.CHISHI_PURIFIER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PURIFIER.get()));
                                    }
                                    if (ModBlocks.CHISHI_ADVANCED_PURIFIER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ADVANCED_PURIFIER.get()));
                                    }
                                    // 赤能源储存单元 + 管道
                                    if (ModBlocks.CHISHI_ENERGY_CELL_BASIC != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_CELL_BASIC.get()));
                                    }
                                    if (ModBlocks.CHISHI_ENERGY_CELL_ADVANCED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_CELL_ADVANCED.get()));
                                    }
                                    if (ModBlocks.CHISHI_ENERGY_CELL_SUPER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_CELL_SUPER.get()));
                                    }
                                    if (ModBlocks.CHISHI_ENERGY_PIPE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_PIPE.get()));
                                    }
                                    if (ModBlocks.CHISHI_ENERGY_PIPE_ADVANCED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_PIPE_ADVANCED.get()));
                                    }
                                    if (ModBlocks.CHISHI_ENERGY_PIPE_ELITE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_PIPE_ELITE.get()));
                                    }
                                    if (ModBlocks.CHISHI_ENERGY_PIPE_ULTIMATE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_PIPE_ULTIMATE.get()));
                                    }
                                    // 赤能源调试工具（切换管道方向模式）
                                    if (ModItems.chishiDebugTool != null) {
                                        output.accept(new ItemStack(ModItems.chishiDebugTool.get()));
                                    }
                                    // 赤红机器组件（赤石科技通用部件）
                                    if (ModItems.chishiMachineComponent != null) {
                                        output.accept(new ItemStack(ModItems.chishiMachineComponent.get()));
                                    }
                                    // 赤红高级机械组件
                                    if (ModItems.chishiAdvancedComponent != null) {
                                        output.accept(new ItemStack(ModItems.chishiAdvancedComponent.get()));
                                    }
                                    // 能源产生升级组件（发生器装配加速）
                                    if (ModItems.chishiSpeedUpgrade != null) {
                                        output.accept(new ItemStack(ModItems.chishiSpeedUpgrade.get()));
                                    }
                                    // 赤石精华
                                    if (ModItems.chishiEssence != null) {
                                        output.accept(new ItemStack(ModItems.chishiEssence.get()));
                                    }
                                    // 浓缩赤石精华
                                    if (ModItems.chishiEssenceCompressed != null) {
                                        output.accept(new ItemStack(ModItems.chishiEssenceCompressed.get()));
                                    }
                                    // 浓缩赤石精华块
                                    if (ModBlocks.CHISHI_ESSENCE_BLOCK != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ESSENCE_BLOCK.get()));
                                    }
                                    // 赤能源发生机 + 小型赤能源组合结构
                                    if (ModBlocks.CHISHI_ENERGY_GENERATOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_GENERATOR.get()));
                                    }
                                    if (ModBlocks.CHISHI_ENERGY_ASSEMBLY != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_ASSEMBLY.get()));
                                    }
                                    // 赤能源储存串联器（多方块主方块）
                                    if (ModBlocks.CHISHI_ENERGY_CELL_SERIALIZER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_CELL_SERIALIZER.get()));
                                    }
                                    // 超级发生器架构核心（5×5×5 多方块主方块）
                                    if (ModBlocks.CHISHI_SUPER_GENERATOR_CORE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_SUPER_GENERATOR_CORE.get()));
                                    }
                                    // 生命能量管道 + 生命聚合转换器 + 生命转换架构
                                    if (ModBlocks.CHISHI_LIFE_ENERGY_PIPE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_ENERGY_PIPE.get()));
                                    }
                                    if (ModBlocks.CHISHI_LIFE_AGGREGATION_CONVERTER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_AGGREGATION_CONVERTER.get()));
                                    }
                                    if (ModBlocks.CHISHI_LIFE_CONVERSION_ARCHITECTURE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_CONVERSION_ARCHITECTURE.get()));
                                    }
                                    // 生命能量储存器（纯生命能量存储）
                                    if (ModBlocks.CHISHI_LIFE_ENERGY_CELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_ENERGY_CELL.get()));
                                    }
                                    // 赤石锭 + 赤红升级模板
                                    if (ModItems.chishiIngot != null) {
                                        output.accept(new ItemStack(ModItems.chishiIngot.get()));
                                    }
                                    if (ModItems.chishiUpgradeTemplate != null) {
                                        output.accept(new ItemStack(ModItems.chishiUpgradeTemplate.get()));
                                    }
                                    // 赤石能量聚合器 + 赤石装备打造器 + 赤红升级台
                                    if (ModBlocks.CHISHI_ENERGY_AGGREGATOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_AGGREGATOR.get()));
                                    }
                                    if (ModBlocks.CHISHI_EQUIPMENT_FORGER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_EQUIPMENT_FORGER.get()));
                                    }
                                    if (ModBlocks.CHISHI_UPGRADE_STATION != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_UPGRADE_STATION.get()));
                                    }
                                    // 赤石装备（头盔/胸甲/护腿/靴子/剑）
                                    if (ModItems.chishiHelmet != null) {
                                        output.accept(new ItemStack(ModItems.chishiHelmet.get()));
                                    }
                                    if (ModItems.chishiChestplate != null) {
                                        output.accept(new ItemStack(ModItems.chishiChestplate.get()));
                                    }
                                    if (ModItems.chishiLeggings != null) {
                                        output.accept(new ItemStack(ModItems.chishiLeggings.get()));
                                    }
                                    if (ModItems.chishiBoots != null) {
                                        output.accept(new ItemStack(ModItems.chishiBoots.get()));
                                    }
                                    if (ModItems.chishiSword != null) {
                                        output.accept(new ItemStack(ModItems.chishiSword.get()));
                                    }
                                    if (ModItems.chishiPickaxe != null) {
                                        output.accept(new ItemStack(ModItems.chishiPickaxe.get()));
                                    }
                                    if (ModItems.chishiShovel != null) {
                                        output.accept(new ItemStack(ModItems.chishiShovel.get()));
                                    }
                                    if (ModItems.chishiAxe != null) {
                                        output.accept(new ItemStack(ModItems.chishiAxe.get()));
                                    }
                                    // 便捷赤能源储存单元（初级/中级/高级）
                                    if (ModItems.portableCellBasic != null) {
                                        output.accept(new ItemStack(ModItems.portableCellBasic.get()));
                                    }
                                    if (ModItems.portableCellAdvanced != null) {
                                        output.accept(new ItemStack(ModItems.portableCellAdvanced.get()));
                                    }
                                    if (ModItems.portableCellSuper != null) {
                                        output.accept(new ItemStack(ModItems.portableCellSuper.get()));
                                    }
                                    // 赤石水晶体系：4 级母岩 + 水晶簇 + 水晶块
                                    if (ModBlocks.CHISHI_GEODE_FLAWED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEODE_FLAWED.get()));
                                    }
                                    if (ModBlocks.CHISHI_GEODE_NORMAL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEODE_NORMAL.get()));
                                    }
                                    if (ModBlocks.CHISHI_GEODE_PRISTINE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEODE_PRISTINE.get()));
                                    }
                                    if (ModBlocks.CHISHI_GEODE_PERFECT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEODE_PERFECT.get()));
                                    }
                                    if (ModBlocks.CHISHI_CRYSTAL_CLUSTER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CRYSTAL_CLUSTER.get()));
                                    }
                                    if (ModBlocks.CHISHI_CRYSTAL_BLOCK != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CRYSTAL_BLOCK.get()));
                                    }
                                    // 赤石催化器（4 级）：催生母岩生长
                                    if (ModBlocks.CHISHI_CATALYST_BASIC != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CATALYST_BASIC.get()));
                                    }
                                    if (ModBlocks.CHISHI_CATALYST_MEDIUM != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CATALYST_MEDIUM.get()));
                                    }
                                    if (ModBlocks.CHISHI_CATALYST_ADVANCED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CATALYST_ADVANCED.get()));
                                    }
                                    if (ModBlocks.CHISHI_CATALYST_ULTIMATE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CATALYST_ULTIMATE.get()));
                                    }
                                    // 自动收集器（4 级）：自动收获水晶簇
                                    if (ModBlocks.CHISHI_COLLECTOR_BASIC != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_COLLECTOR_BASIC.get()));
                                    }
                                    if (ModBlocks.CHISHI_COLLECTOR_MEDIUM != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_COLLECTOR_MEDIUM.get()));
                                    }
                                    if (ModBlocks.CHISHI_COLLECTOR_ADVANCED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_COLLECTOR_ADVANCED.get()));
                                    }
                                    if (ModBlocks.CHISHI_COLLECTOR_ULTIMATE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_COLLECTOR_ULTIMATE.get()));
                                    }
                                    // 物品管道（4 级）：物流网络中继，传输物品到相连容器/机器
                                    if (ModBlocks.CHISHI_ITEM_PIPE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ITEM_PIPE.get()));
                                    }
                                    if (ModBlocks.CHISHI_ITEM_PIPE_ADVANCED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ITEM_PIPE_ADVANCED.get()));
                                    }
                                    if (ModBlocks.CHISHI_ITEM_PIPE_ELITE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ITEM_PIPE_ELITE.get()));
                                    }
                                    if (ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE.get()));
                                    }
                                    // 生命能量提纯器 + 生命能量固态物
                                    if (ModBlocks.CHISHI_LIFE_PURIFIER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_PURIFIER.get()));
                                    }
                                    if (ModItems.chishiLifeEssenceSolid != null) {
                                        output.accept(new ItemStack(ModItems.chishiLifeEssenceSolid.get()));
                                    }
                                    // 创造模式能量源（测试用，无限输出）
                                    if (ModBlocks.CHISHI_CREATIVE_ENERGY_CELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CREATIVE_ENERGY_CELL.get()));
                                    }
                                    if (ModBlocks.CHISHI_CREATIVE_LIFE_CELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CREATIVE_LIFE_CELL.get()));
                                    }
                                    // 液体管道 + 液体储罐（3 级）
                                    if (ModBlocks.CHISHI_FLUID_PIPE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FLUID_PIPE.get()));
                                    }
                                    if (ModBlocks.CHISHI_FLUID_TANK_BASIC != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FLUID_TANK_BASIC.get()));
                                    }
                                    if (ModBlocks.CHISHI_FLUID_TANK_ADVANCED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FLUID_TANK_ADVANCED.get()));
                                    }
                                    if (ModBlocks.CHISHI_FLUID_TANK_SUPER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FLUID_TANK_SUPER.get()));
                                    }
                                    // 能量液化装置 + 能量加工器（燃料加工核心）
                                    if (ModBlocks.CHISHI_ENERGY_LIQUEFIER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_LIQUEFIER.get()));
                                    }
                                    if (ModBlocks.CHISHI_ENERGY_PROCESSOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ENERGY_PROCESSOR.get()));
                                    }
                                    // 燃料装罐机 + 燃料混合器 + 燃料罐
                                    if (ModBlocks.CHISHI_FUEL_CANNER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUEL_CANNER.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUEL_MIXER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUEL_MIXER.get()));
                                    }
                                    if (ModItems.fuelCell != null) {
                                        output.accept(new ItemStack(ModItems.fuelCell.get()));
                                    }
                                    // 末地/巨龙/幽匿混合物（燃料液化原料）
                                    if (ModItems.endMixture != null) {
                                        output.accept(new ItemStack(ModItems.endMixture.get()));
                                    }
                                    if (ModItems.dragonMixture != null) {
                                        output.accept(new ItemStack(ModItems.dragonMixture.get()));
                                    }
                                    if (ModItems.sculkLifeform != null) {
                                        output.accept(new ItemStack(ModItems.sculkLifeform.get()));
                                    }
                                    // 反应堆体系：外壳/控制器/投放口/输出口/废品口/燃料棒/散热组件/核心
                                    if (ModBlocks.CHISHI_REACTOR_SHELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_SHELL.get()));
                                    }
                                    if (ModBlocks.CHISHI_REACTOR_CONTROLLER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_CONTROLLER.get()));
                                    }
                                    if (ModBlocks.CHISHI_REACTOR_FUEL_PORT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_FUEL_PORT.get()));
                                    }
                                    if (ModBlocks.CHISHI_REACTOR_ENERGY_OUTPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_ENERGY_OUTPUT.get()));
                                    }
                                    if (ModBlocks.CHISHI_REACTOR_WASTE_PORT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_WASTE_PORT.get()));
                                    }
                                    if (ModBlocks.CHISHI_REACTOR_FUEL_ROD != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_FUEL_ROD.get()));
                                    }
                                    if (ModBlocks.CHISHI_REACTOR_COOLER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_COOLER.get()));
                                    }
                                    if (ModBlocks.CHISHI_REACTOR_CORE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_CORE.get()));
                                    }
                                    if (ModBlocks.CHISHI_EXHAUSTED_BARREL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_EXHAUSTED_BARREL.get()));
                                    }
                                    // 发生器矩阵体系（类反应堆式：外壳/控制器/能量输出口/燃料输入口）
                                    if (ModBlocks.CHISHI_GEN_MATRIX_CASING != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEN_MATRIX_CASING.get()));
                                    }
                                    if (ModBlocks.CHISHI_GEN_MATRIX_CONTROLLER_BASIC != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEN_MATRIX_CONTROLLER_BASIC.get()));
                                    }
                                    if (ModBlocks.CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEN_MATRIX_CONTROLLER_ADVANCED.get()));
                                    }
                                    if (ModBlocks.CHISHI_GEN_ENERGY_OUTPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEN_ENERGY_OUTPUT.get()));
                                    }
                                    if (ModBlocks.CHISHI_GEN_FUEL_INPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEN_FUEL_INPUT.get()));
                                    }
                                    // 提纯矩阵体系（外壳/控制器/能量输入口/物品输入口/物品输出口）
                                    if (ModBlocks.CHISHI_PURIFIER_MATRIX_CASING != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PURIFIER_MATRIX_CASING.get()));
                                    }
                                    if (ModBlocks.CHISHI_PURIFIER_MATRIX_CONTROLLER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PURIFIER_MATRIX_CONTROLLER.get()));
                                    }
                                    if (ModBlocks.CHISHI_PURIFIER_ENERGY_INPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PURIFIER_ENERGY_INPUT.get()));
                                    }
                                    if (ModBlocks.CHISHI_PURIFIER_ITEM_INPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PURIFIER_ITEM_INPUT.get()));
                                    }
                                    if (ModBlocks.CHISHI_PURIFIER_ITEM_OUTPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PURIFIER_ITEM_OUTPUT.get()));
                                    }
                                    // 生命转换矩阵体系（外壳/控制器/能源输入口/能源输出口）
                                    if (ModBlocks.CHISHI_LIFE_MATRIX_CASING != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_MATRIX_CASING.get()));
                                    }
                                    if (ModBlocks.CHISHI_LIFE_MATRIX_CONTROLLER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_MATRIX_CONTROLLER.get()));
                                    }
                                    if (ModBlocks.CHISHI_LIFE_MATRIX_ENERGY_INPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_MATRIX_ENERGY_INPUT.get()));
                                    }
                                    if (ModBlocks.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_MATRIX_ENERGY_OUTPUT.get()));
                                    }
                                    // 散热片（5 品质）
                                    if (ModItems.heatSinkPoor != null) {
                                        output.accept(new ItemStack(ModItems.heatSinkPoor.get()));
                                    }
                                    if (ModItems.heatSinkNormal != null) {
                                        output.accept(new ItemStack(ModItems.heatSinkNormal.get()));
                                    }
                                    if (ModItems.heatSinkGood != null) {
                                        output.accept(new ItemStack(ModItems.heatSinkGood.get()));
                                    }
                                    if (ModItems.heatSinkFine != null) {
                                        output.accept(new ItemStack(ModItems.heatSinkFine.get()));
                                    }
                                    if (ModItems.heatSinkExquisite != null) {
                                        output.accept(new ItemStack(ModItems.heatSinkExquisite.get()));
                                    }
                                    // 赤石饰品（Curios 槽位：charm/ring/hands/necklace/body/bracelet/belt）
                                    if (ModItems.satiationCharm != null) {
                                        output.accept(new ItemStack(ModItems.satiationCharm.get()));
                                    }
                                    if (ModItems.huntingRing != null) {
                                        output.accept(new ItemStack(ModItems.huntingRing.get()));
                                    }
                                    if (ModItems.gatheringBracelet != null) {
                                        output.accept(new ItemStack(ModItems.gatheringBracelet.get()));
                                    }
                                    if (ModItems.fireNecklace != null) {
                                        output.accept(new ItemStack(ModItems.fireNecklace.get()));
                                    }
                                    if (ModItems.blastCharm != null) {
                                        output.accept(new ItemStack(ModItems.blastCharm.get()));
                                    }
                                    if (ModItems.antidoteBracelet != null) {
                                        output.accept(new ItemStack(ModItems.antidoteBracelet.get()));
                                    }
                                    if (ModItems.witherCharm != null) {
                                        output.accept(new ItemStack(ModItems.witherCharm.get()));
                                    }
                                    // 躯体检查仪（生命科技：体检面板）
                                    if (ModBlocks.CHISHI_BODY_SCANNER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_BODY_SCANNER.get()));
                                    }
                                    // 生命分析台（生命科技：样本解构）
                                    if (ModBlocks.CHISHI_GENE_ANALYZER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GENE_ANALYZER.get()));
                                    }
                                    // 部件培养舱（生命科技：提纯 + 器官升级）
                                    if (ModBlocks.CHISHI_CULTIVATOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CULTIVATOR.get()));
                                    }
                                    // 生命结构台（生命科技：基因序列 → 器官）
                                    if (ModBlocks.CHISHI_LIFE_STRUCT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_STRUCT.get()));
                                    }
                                    // 手术仓（生命科技：器官移植/摘除）
                                    if (ModBlocks.CHISHI_SURGERY != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_SURGERY.get()));
                                    }
                                    // 药剂台（生命科技：永久/突破药剂）
                                    if (ModBlocks.CHISHI_POTION_TABLE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_POTION_TABLE.get()));
                                    }
                                    // 药剂（生命科技：永久/突破模板）
                                    if (ModItems.chishiPotion != null) {
                                        output.accept(new ItemStack(ModItems.chishiPotion.get()));
                                    }
                                    // 器官储藏库（生命科技：按槽位分页的器官仓库）
                                    if (ModBlocks.CHISHI_ORGAN_VAULT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ORGAN_VAULT.get()));
                                    }
                                    // 药剂库（生命科技：大容量药剂仓库，自动合并）
                                    if (ModBlocks.CHISHI_POTION_CABINET != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_POTION_CABINET.get()));
                                    }
                                    // 生命科技：样本采集器 + 生命样本 + 基因序列片段
                                    if (ModItems.sampleCollector != null) {
                                        output.accept(new ItemStack(ModItems.sampleCollector.get()));
                                    }
                                    if (ModItems.geneSequence != null) {
                                        output.accept(new ItemStack(ModItems.geneSequence.get()));
                                    }
                                    // 生命科技：9 个槽位的基础器官物品
                                    output.accept(new ItemStack(ModItems.chishiOrganEye.get()));
                                    output.accept(new ItemStack(ModItems.chishiOrganHeart.get()));
                                    output.accept(new ItemStack(ModItems.chishiOrganLungs.get()));
                                    output.accept(new ItemStack(ModItems.chishiOrganViscera.get()));
                                    output.accept(new ItemStack(ModItems.chishiOrganKidneys.get()));
                                    output.accept(new ItemStack(ModItems.chishiOrganLeftArm.get()));
                                    output.accept(new ItemStack(ModItems.chishiOrganRightArm.get()));
                                    output.accept(new ItemStack(ModItems.chishiOrganLeftLeg.get()));
                                    output.accept(new ItemStack(ModItems.chishiOrganRightLeg.get()));
                                    // 手册“赤石研究日记”由帕秋莉按 creative_tab 自动加入，此处不重复添加
                                })
                                .build());
    }
}
