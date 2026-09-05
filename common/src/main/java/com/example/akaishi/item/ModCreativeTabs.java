package com.example.akaishi.item;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiDecayBlocks;
import com.example.akaishi.block.AkaishiOreDef;
import com.example.akaishi.block.ModBlocks;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 创造模式物品栏分类“赤石之章”。
 * 集中收纳赤石科技相关的方块与物品，分类 id 同时供帕秋莉手册引用。
 */
public final class ModCreativeTabs {

    /** 分类 id，帕秋莉手册 book.json 的 creative_tab 也引用该 id */
    public static final String CHISHI_TAB_ID = "akaishi";

    private ModCreativeTabs() {
    }

    public static void register() {
        // 注册创造模式物品栏分类（惰性构建，displayItems 在游戏启动后回调）
        RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.CREATIVE_MODE_TAB)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, CHISHI_TAB_ID),
                        () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                                .title(Component.translatable("itemGroup.akaishi.akaishi"))
                                .icon(() -> new ItemStack(ModBlocks.get(ModBlocks.ALL_ORES.get(0))))
                                .displayItems((params, output) -> {
                                    // 16 个赤石矿簇方块
                                    for (AkaishiOreDef def : ModBlocks.ALL_ORES) {
                                        output.accept(new ItemStack(ModBlocks.get(def)));
                                    }
                                    // 赤石晶（注册完成后才可用，防御性判空）
                                    if (ModItems.akaishiCrystal != null) {
                                        output.accept(new ItemStack(ModItems.akaishiCrystal.get()));
                                    }
                                    // 手册（赤石日记 / 生命秘闻）由帕秋莉按 book.json 的
                                    // crafting_recipe + creative_tab 自动加入，此处不重复添加
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
                                    if (ModItems.akaishiDebugTool != null) {
                                        output.accept(new ItemStack(ModItems.akaishiDebugTool.get()));
                                    }
                                    // 赤红机器组件（赤石科技通用部件）
                                    if (ModItems.akaishiMachineComponent != null) {
                                        output.accept(new ItemStack(ModItems.akaishiMachineComponent.get()));
                                    }
                                    // 赤红高级机械组件
                                    if (ModItems.akaishiAdvancedComponent != null) {
                                        output.accept(new ItemStack(ModItems.akaishiAdvancedComponent.get()));
                                    }
                                    // 能源产生升级组件（发生器装配加速）
                                    if (ModItems.akaishiSpeedUpgrade != null) {
                                        output.accept(new ItemStack(ModItems.akaishiSpeedUpgrade.get()));
                                    }
                                    // 机器升级组件（速度/能量，装通用电器升级槽）
                                    if (ModItems.machineSpeedUpgrade != null) {
                                        output.accept(new ItemStack(ModItems.machineSpeedUpgrade.get()));
                                    }
                                    if (ModItems.machineEnergyUpgrade != null) {
                                        output.accept(new ItemStack(ModItems.machineEnergyUpgrade.get()));
                                    }
                                    // 赤石精华
                                    if (ModItems.akaishiEssence != null) {
                                        output.accept(new ItemStack(ModItems.akaishiEssence.get()));
                                    }
                                    // 浓缩赤石精华
                                    if (ModItems.akaishiEssenceCompressed != null) {
                                        output.accept(new ItemStack(ModItems.akaishiEssenceCompressed.get()));
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
                                    if (ModItems.akaishiIngot != null) {
                                        output.accept(new ItemStack(ModItems.akaishiIngot.get()));
                                    }
                                    if (ModItems.akaishiUpgradeTemplate != null) {
                                        output.accept(new ItemStack(ModItems.akaishiUpgradeTemplate.get()));
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
                                    if (ModItems.akaishiHelmet != null) {
                                        output.accept(new ItemStack(ModItems.akaishiHelmet.get()));
                                    }
                                    if (ModItems.akaishiChestplate != null) {
                                        output.accept(new ItemStack(ModItems.akaishiChestplate.get()));
                                    }
                                    if (ModItems.akaishiLeggings != null) {
                                        output.accept(new ItemStack(ModItems.akaishiLeggings.get()));
                                    }
                                    if (ModItems.akaishiBoots != null) {
                                        output.accept(new ItemStack(ModItems.akaishiBoots.get()));
                                    }
                                    if (ModItems.akaishiSword != null) {
                                        output.accept(new ItemStack(ModItems.akaishiSword.get()));
                                    }
                                    if (ModItems.akaishiPickaxe != null) {
                                        output.accept(new ItemStack(ModItems.akaishiPickaxe.get()));
                                    }
                                    if (ModItems.akaishiShovel != null) {
                                        output.accept(new ItemStack(ModItems.akaishiShovel.get()));
                                    }
                                    if (ModItems.akaishiAxe != null) {
                                        output.accept(new ItemStack(ModItems.akaishiAxe.get()));
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
                                    if (ModItems.akaishiLifeEssenceSolid != null) {
                                        output.accept(new ItemStack(ModItems.akaishiLifeEssenceSolid.get()));
                                    }
                                    // 生命活化器（生命科技：消耗生命能量无害化衰竭燃料）
                                    if (ModBlocks.CHISHI_LIFE_ACTIVATOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_ACTIVATOR.get()));
                                    }
                                    // 生命离心机（分离活化燃料为结晶产物）
                                    if (ModBlocks.CHISHI_LIFE_CENTRIFUGE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_CENTRIFUGE.get()));
                                    }
                                    // 物品重构仪（以衰竭结晶为代价嬗变物品）
                                    if (ModBlocks.CHISHI_ITEM_RECONSTRUCTOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ITEM_RECONSTRUCTOR.get()));
                                    }
                                    // 活化分馏器（活化结晶深度拆分：活化成分 + 衰竭结晶）
                                    if (ModBlocks.CHISHI_ACTIVATED_FRACTIONATOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ACTIVATED_FRACTIONATOR.get()));
                                    }
                                    // 创造模式能量源（测试用，无限输出）
                                    if (ModBlocks.CHISHI_CREATIVE_ENERGY_CELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CREATIVE_ENERGY_CELL.get()));
                                    }
                                    if (ModBlocks.CHISHI_CREATIVE_LIFE_CELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CREATIVE_LIFE_CELL.get()));
                                    }
                                    // 液体管道 + 液体储罐（3 级）+ 废料专用管道
                                    if (ModBlocks.CHISHI_FLUID_PIPE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FLUID_PIPE.get()));
                                    }
                                    if (ModBlocks.CHISHI_EXHAUSTED_PIPE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_EXHAUSTED_PIPE.get()));
                                    }
                                    if (ModBlocks.CHISHI_MULTI_FLUID_WASTE_PIPE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MULTI_FLUID_WASTE_PIPE.get()));
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
                                    if (ModBlocks.CHISHI_REACTOR_STRUCTURE_GLASS != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_REACTOR_STRUCTURE_GLASS.get()));
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
                                    // 聚变燃料体系：聚合器（赤能源产等离子体）+ 填装器（反应棒灌等离子体）+ 等离子体管道
                                    if (ModBlocks.CHISHI_FUSION_FUEL_AGGREGATOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_FUEL_AGGREGATOR.get()));
                                    }
                                    if (ModBlocks.CHISHI_PLASMA_FILLER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PLASMA_FILLER.get()));
                                    }
                                    if (ModBlocks.CHISHI_PLASMA_PIPE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PLASMA_PIPE.get()));
                                    }
                                    if (ModBlocks.CHISHI_PLASMA_TANK != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PLASMA_TANK.get()));
                                    }
                                    // 聚变反应棒 + 3 种等离子体燃料棒（混合/下界/末地）
                                    if (ModItems.fusionRod != null) {
                                        output.accept(new ItemStack(ModItems.fusionRod.get()));
                                    }
                                    if (ModItems.mixedPlasmaRod != null) {
                                        output.accept(new ItemStack(ModItems.mixedPlasmaRod.get()));
                                    }
                                    if (ModItems.netherPlasmaRod != null) {
                                        output.accept(new ItemStack(ModItems.netherPlasmaRod.get()));
                                    }
                                    if (ModItems.endPlasmaRod != null) {
                                        output.accept(new ItemStack(ModItems.endPlasmaRod.get()));
                                    }
                                    // 离心结晶（衰竭燃料处理体系产物：通用副产物 + 7 种活化结晶）
                                    if (ModItems.exhaustedCrystal != null) {
                                        output.accept(new ItemStack(ModItems.exhaustedCrystal.get()));
                                    }
                                    if (ModItems.activatedSculkCrystal != null) {
                                        output.accept(new ItemStack(ModItems.activatedSculkCrystal.get()));
                                    }
                                    if (ModItems.activatedNetherCompoundCrystal != null) {
                                        output.accept(new ItemStack(ModItems.activatedNetherCompoundCrystal.get()));
                                    }
                                    if (ModItems.activatedEndMixtureCrystal != null) {
                                        output.accept(new ItemStack(ModItems.activatedEndMixtureCrystal.get()));
                                    }
                                    if (ModItems.activatedAdvancedMixtureCrystal != null) {
                                        output.accept(new ItemStack(ModItems.activatedAdvancedMixtureCrystal.get()));
                                    }
                                    if (ModItems.activatedPureCrystal != null) {
                                        output.accept(new ItemStack(ModItems.activatedPureCrystal.get()));
                                    }
                                    if (ModItems.activatedDragonCrystal != null) {
                                        output.accept(new ItemStack(ModItems.activatedDragonCrystal.get()));
                                    }
                                    if (ModItems.activatedUltimateMixtureCrystal != null) {
                                        output.accept(new ItemStack(ModItems.activatedUltimateMixtureCrystal.get()));
                                    }
                                    // 活化成分（活化分馏器深度拆分产物：7 种对应活化燃料）
                                    if (ModItems.activatedSculkComponent != null) {
                                        output.accept(new ItemStack(ModItems.activatedSculkComponent.get()));
                                    }
                                    if (ModItems.activatedNetherCompoundComponent != null) {
                                        output.accept(new ItemStack(ModItems.activatedNetherCompoundComponent.get()));
                                    }
                                    if (ModItems.activatedEndMixtureComponent != null) {
                                        output.accept(new ItemStack(ModItems.activatedEndMixtureComponent.get()));
                                    }
                                    if (ModItems.activatedAdvancedMixtureComponent != null) {
                                        output.accept(new ItemStack(ModItems.activatedAdvancedMixtureComponent.get()));
                                    }
                                    if (ModItems.activatedPureComponent != null) {
                                        output.accept(new ItemStack(ModItems.activatedPureComponent.get()));
                                    }
                                    if (ModItems.activatedDragonComponent != null) {
                                        output.accept(new ItemStack(ModItems.activatedDragonComponent.get()));
                                    }
                                    if (ModItems.activatedUltimateMixtureComponent != null) {
                                        output.accept(new ItemStack(ModItems.activatedUltimateMixtureComponent.get()));
                                    }
                                    // 发生器矩阵体系（类反应堆式：外壳/控制器/能量输出口/燃料输入口）
                                    if (ModBlocks.CHISHI_GEN_MATRIX_CASING != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEN_MATRIX_CASING.get()));
                                    }
                                    if (ModBlocks.CHISHI_GEN_MATRIX_STRUCTURE_GLASS != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GEN_MATRIX_STRUCTURE_GLASS.get()));
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
                                    if (ModBlocks.CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS.get()));
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
                                    if (ModBlocks.CHISHI_LIFE_MATRIX_STRUCTURE_GLASS != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_MATRIX_STRUCTURE_GLASS.get()));
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
                                    if (ModItems.heatSinkUltimate != null) {
                                        output.accept(new ItemStack(ModItems.heatSinkUltimate.get()));
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
                                    // 基因管理器（生命科技：已吸收基因强化管理/卸载）
                                    if (ModBlocks.CHISHI_GENE_MANAGER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GENE_MANAGER.get()));
                                    }
                                    // 生命分析台（生命科技：样本解构）
                                    if (ModBlocks.CHISHI_GENE_ANALYZER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_GENE_ANALYZER.get()));
                                    }
                                    // 转基因工厂（生命科技：凋零骷髅基因 + 生物质 → 凋零藤）
                                    if (ModBlocks.CHISHI_TRANSGENE_FACTORY != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_TRANSGENE_FACTORY.get()));
                                    }
                                    // 部件培养舱（生命科技：提纯 + 器官升级）
                                    if (ModBlocks.CHISHI_CULTIVATOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CULTIVATOR.get()));
                                    }
                                    // 生命结构台（生命科技：基因序列 → 器官）
                                    if (ModBlocks.CHISHI_LIFE_STRUCT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_STRUCT.get()));
                                    }
                                    // 生命培育器（生命科技：器官施加突变词条）
                                    if (ModBlocks.CHISHI_LIFE_BREEDER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_BREEDER.get()));
                                    }
                                    // 词条重铸仪（生命科技：原位重铸指定突变词条）
                                    if (ModBlocks.CHISHI_TRAIT_REFORGER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_TRAIT_REFORGER.get()));
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
                                    if (ModItems.akaishiPotion != null) {
                                        output.accept(new ItemStack(ModItems.akaishiPotion.get()));
                                    }
                                    if (ModItems.rejectionSerum != null) {
                                        output.accept(new ItemStack(ModItems.rejectionSerum.get()));
                                    }
                                    // 器官储藏库（生命科技：按槽位分页的器官仓库）
                                    if (ModBlocks.CHISHI_ORGAN_VAULT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ORGAN_VAULT.get()));
                                    }
                                    // 样本库（生命科技：大容量样本仓库，自动合并）
                                    if (ModBlocks.CHISHI_SAMPLE_VAULT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_SAMPLE_VAULT.get()));
                                    }
                                    // 药剂库（生命科技：大容量药剂仓库，自动合并）
                                    if (ModBlocks.CHISHI_POTION_CABINET != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_POTION_CABINET.get()));
                                    }
                                    // 黑山羊之母祭坛（生命线终局：献上生命造物，识别 NBT 的祭坛）
                                    if (ModBlocks.CHISHI_MOTHER_ALTAR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MOTHER_ALTAR.get()));
                                    }
                                    // 母神祭坛石（祭坛结构件/装饰建材）
                                    if (ModBlocks.CHISHI_ALTAR_STONE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_ALTAR_STONE.get()));
                                    }
                                    // 衰竭区域污染产物（衰竭土壤/衰竭木：装饰建材与体系原料）
                                    if (ModBlocks.CHISHI_DECAY_SOIL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_DECAY_SOIL.get()));
                                    }
                                    if (ModBlocks.CHISHI_DECAY_LOG != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_DECAY_LOG.get()));
                                    }
                                    // 衰竭全家桶：衰竭岩石组 / 衰竭木完整组 / 衰竭地表组
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
                                    // 凋零藤种子（转基因工厂产物）+ 成熟收获物：凋零凝聚体
                                    if (ModItems.akaishiWitherSeed != null) {
                                        output.accept(new ItemStack(ModItems.akaishiWitherSeed.get()));
                                    }
                                    if (ModItems.akaishiWitherCondensate != null) {
                                        output.accept(new ItemStack(ModItems.akaishiWitherCondensate.get()));
                                    }
                                    // 生命科技：样本采集器 + 生命样本 + 基因序列片段
                                    if (ModItems.sampleCollector != null) {
                                        output.accept(new ItemStack(ModItems.sampleCollector.get()));
                                    }
                                    if (ModItems.lifeSample != null) {
                                        output.accept(new ItemStack(ModItems.lifeSample.get()));
                                    }
                                    // 生命胚胎（8 生命固态 + 鸡蛋，母神祭坛祭品）
                                    if (ModItems.lifeEmbryo != null) {
                                        output.accept(new ItemStack(ModItems.lifeEmbryo.get()));
                                    }
                                    if (ModItems.geneSequence != null) {
                                        output.accept(new ItemStack(ModItems.geneSequence.get()));
                                    }
                                    // 生命的融合锭 + 生命的融合砧 + 生命融合护甲（赤石护甲 2 倍基础数值）
                                    if (ModItems.lifeFusionIngot != null) {
                                        output.accept(new ItemStack(ModItems.lifeFusionIngot.get()));
                                    }
                                    if (ModBlocks.CHISHI_LIFE_FUSION_ANVIL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_LIFE_FUSION_ANVIL.get()));
                                    }
                                    if (ModItems.lifeFusionHelmet != null) {
                                        output.accept(new ItemStack(ModItems.lifeFusionHelmet.get()));
                                    }
                                    if (ModItems.lifeFusionChestplate != null) {
                                        output.accept(new ItemStack(ModItems.lifeFusionChestplate.get()));
                                    }
                                    if (ModItems.lifeFusionLeggings != null) {
                                        output.accept(new ItemStack(ModItems.lifeFusionLeggings.get()));
                                    }
                                    if (ModItems.lifeFusionBoots != null) {
                                        output.accept(new ItemStack(ModItems.lifeFusionBoots.get()));
                                    }
                                    // 生命科技：9 个槽位的基础器官物品
                                    output.accept(new ItemStack(ModItems.akaishiOrganEye.get()));
                                    output.accept(new ItemStack(ModItems.akaishiOrganHeart.get()));
                                    output.accept(new ItemStack(ModItems.akaishiOrganLungs.get()));
                                    output.accept(new ItemStack(ModItems.akaishiOrganViscera.get()));
                                    output.accept(new ItemStack(ModItems.akaishiOrganKidneys.get()));
                                    output.accept(new ItemStack(ModItems.akaishiOrganLeftArm.get()));
                                    output.accept(new ItemStack(ModItems.akaishiOrganRightArm.get()));
                                    output.accept(new ItemStack(ModItems.akaishiOrganLeftLeg.get()));
                                    output.accept(new ItemStack(ModItems.akaishiOrganRightLeg.get()));
                                    // 无线赤能源体系：外壳/终端核心/控制器/输入口/输出口
                                    if (ModBlocks.CHISHI_WIRELESS_SHELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_SHELL.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_STRUCTURE_GLASS != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_STRUCTURE_GLASS.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_TERMINAL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_TERMINAL.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_SECURITY != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_SECURITY.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_CORE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_CORE.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_CONTROLLER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_CONTROLLER.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_INPUT_PORT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_INPUT_PORT.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_OUTPUT_PORT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_OUTPUT_PORT.get()));
                                    }
                                    // 无线赤能源体系：内腔功能组件（跨维/区块加载/区块扩展/入损/出损抑制）
                                    if (ModBlocks.CHISHI_WIRELESS_DIM_BRIDGE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_DIM_BRIDGE.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_CHUNK_LOADER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_CHUNK_LOADER.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_CHUNK_RANGE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_CHUNK_RANGE.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_INPUT_LOSS != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_INPUT_LOSS.get()));
                                    }
                                    if (ModBlocks.CHISHI_WIRELESS_OUTPUT_LOSS != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_WIRELESS_OUTPUT_LOSS.get()));
                                    }
                                    // 无线赤能源体系：便捷组件/便捷终端/身份卡
                                    if (ModItems.akaishiWirelessComponent != null) {
                                        output.accept(new ItemStack(ModItems.akaishiWirelessComponent.get()));
                                    }
                                    if (ModItems.akaishiWirelessPortableTerminal != null) {
                                        output.accept(new ItemStack(ModItems.akaishiWirelessPortableTerminal.get()));
                                    }
                                    if (ModItems.akaishiWirelessIdentityCard != null) {
                                        output.accept(new ItemStack(ModItems.akaishiWirelessIdentityCard.get()));
                                    }
                                    // 聚变堆体系：外壳/隔热层/控制器/核心/框架（散热/燃料/效率）/接口（能量/物品输入/物品输出）
                                    if (ModBlocks.CHISHI_FUSION_SHELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_SHELL.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_STRUCTURE_GLASS != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_STRUCTURE_GLASS.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_INSULATION != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_INSULATION.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_CONTROLLER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_CONTROLLER.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_CORE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_CORE.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_COOLER_FRAME != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_COOLER_FRAME.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_FUEL_FRAME != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_FUEL_FRAME.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_EFFICIENCY_FRAME != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_EFFICIENCY_FRAME.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_ENERGY_OUTPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_ENERGY_OUTPUT.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_ITEM_INPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_ITEM_INPUT.get()));
                                    }
                                    if (ModBlocks.CHISHI_FUSION_ITEM_OUTPUT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_FUSION_ITEM_OUTPUT.get()));
                                    }
                                    // 聚变堆体系：生命灰烬 + 6 档聚变散热片（5%/7%/9%/12%/15%/生命 20%）
                                    if (ModItems.lifeAsh != null) {
                                        output.accept(new ItemStack(ModItems.lifeAsh.get()));
                                    }
                                    if (ModItems.fusionHeatSinkTier1 != null) {
                                        output.accept(new ItemStack(ModItems.fusionHeatSinkTier1.get()));
                                    }
                                    if (ModItems.fusionHeatSinkTier2 != null) {
                                        output.accept(new ItemStack(ModItems.fusionHeatSinkTier2.get()));
                                    }
                                    if (ModItems.fusionHeatSinkTier3 != null) {
                                        output.accept(new ItemStack(ModItems.fusionHeatSinkTier3.get()));
                                    }
                                    if (ModItems.fusionHeatSinkTier4 != null) {
                                        output.accept(new ItemStack(ModItems.fusionHeatSinkTier4.get()));
                                    }
                                    if (ModItems.fusionHeatSinkTier5 != null) {
                                        output.accept(new ItemStack(ModItems.fusionHeatSinkTier5.get()));
                                    }
                                    if (ModItems.fusionHeatSinkLife != null) {
                                        output.accept(new ItemStack(ModItems.fusionHeatSinkLife.get()));
                                    }
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
                                    // 基底体系（变化器产物：冷却基底 + 各矿物矿石基底）
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
                                    // 单槽处理机器（植物培养机/压缩机/打粉机/变化器）
                                    if (ModBlocks.CHISHI_PLANT_CULTIVATOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PLANT_CULTIVATOR.get()));
                                    }
                                    if (ModBlocks.CHISHI_COMPRESSOR != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_COMPRESSOR.get()));
                                    }
                                    if (ModBlocks.CHISHI_PULVERIZER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_PULVERIZER.get()));
                                    }
                                    if (ModBlocks.CHISHI_TRANSFORMER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_TRANSFORMER.get()));
                                    }
                                    // 赤石矿机体系：4 级控制器 + 架构/升级框架 + 转口
                                    if (ModBlocks.CHISHI_MINER_CONTROLLER_BASIC != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_CONTROLLER_BASIC.get()));
                                    }
                                    if (ModBlocks.CHISHI_MINER_CONTROLLER_ADVANCED != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_CONTROLLER_ADVANCED.get()));
                                    }
                                    if (ModBlocks.CHISHI_MINER_CONTROLLER_SUPER != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_CONTROLLER_SUPER.get()));
                                    }
                                    if (ModBlocks.CHISHI_MINER_CONTROLLER_ULTIMATE != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_CONTROLLER_ULTIMATE.get()));
                                    }
                                    if (ModBlocks.CHISHI_MINER_FRAME != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_FRAME.get()));
                                    }
                                    if (ModBlocks.CHISHI_MINER_UPGRADE_FRAME != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_UPGRADE_FRAME.get()));
                                    }
                                    if (ModBlocks.CHISHI_MINER_PORT != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_PORT.get()));
                                    }
                                    // 矿机升级模块方块（速度/时运/储能，安装于升级框架位置）
                                    if (ModBlocks.CHISHI_MINER_SPEED_UPGRADE_BLOCK != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_SPEED_UPGRADE_BLOCK.get()));
                                    }
                                    if (ModBlocks.CHISHI_MINER_FORTUNE_UPGRADE_BLOCK != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_FORTUNE_UPGRADE_BLOCK.get()));
                                    }
                                    if (ModBlocks.CHISHI_MINER_STORAGE_UPGRADE_BLOCK != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_MINER_STORAGE_UPGRADE_BLOCK.get()));
                                    }
                                    // 手册：赤石研究日记（akaishi_diary）+ 生命的奥秘（akaishi_life_book），作为 Patchouli 自定义书物品
                                    accept(output, ModItems.akaishiDiary);
                                    accept(output, ModItems.lifeBook);
                                })
                                .build());
    }

    /** 判空后把注册物品放入创造标签（注册完成前为 null，防御性跳过） */
    private static void accept(CreativeModeTab.Output output, RegistrySupplier<Item> item) {
        if (item != null) {
            output.accept(new ItemStack(item.get()));
        }
    }
}
