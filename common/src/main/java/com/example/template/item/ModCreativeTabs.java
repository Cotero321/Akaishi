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
                                    // 创造模式能量源（测试用，无限输出）
                                    if (ModBlocks.CHISHI_CREATIVE_ENERGY_CELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CREATIVE_ENERGY_CELL.get()));
                                    }
                                    if (ModBlocks.CHISHI_CREATIVE_LIFE_CELL != null) {
                                        output.accept(new ItemStack(ModBlocks.CHISHI_CREATIVE_LIFE_CELL.get()));
                                    }
                                    // 手册“赤石研究日记”由帕秋莉按 creative_tab 自动加入，此处不重复添加
                                })
                                .build());
    }
}
