package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiCrystalBlocks;
import com.example.akaishi.block.AkaishiEnergyBlocks;
import com.example.akaishi.block.AkaishiFusionBlocks;
import com.example.akaishi.block.AkaishiLifeBlocks;
import com.example.akaishi.block.AkaishiReactorBlocks;
import com.example.akaishi.block.AkaishiWirelessBlocks;
import com.example.akaishi.block.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 集成入口：注册提纯配方类别、展示配方，并为催化器/收集器提供物品信息说明。
 */
@JeiPlugin
public class AkaishiModJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AkaishiMod.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new PurificationRecipeCategory(helper),
                new AggregationRecipeCategory(helper),
                new ForgingRecipeCategory(helper),
                // 生命融合锻台：赤石装备 + 生命融合锭 → 生命融合装备（获得途径展示）
                new LifeFusionAnvilRecipeCategory(helper),
                new UpgradeRecipeCategory(helper),
                // 燃料生产链：液化 → 加工 → 调和（燃料产生展示）
                new LiquefactionRecipeCategory(helper),
                new FuelProcessingRecipeCategory(helper),
                new FuelMixingRecipeCategory(helper),
                // 物品重构：衰竭结晶为代价的嬗变配方
                new ReconstructRecipeCategory(helper),
                // 单输入单输出处理机器：压缩机 / 打粉机 / 变化器 / 植物培养机
                new CompressorRecipeCategory(helper),
                new PulverizerRecipeCategory(helper),
                new TransformerRecipeCategory(helper),
                new PlantCultivatorRecipeCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PurificationRecipeCategory.TYPE, PurificationRecipeCategory.PurificationRecipe.getAll());
        registration.addRecipes(AggregationRecipeCategory.TYPE, AggregationRecipeCategory.AggregationRecipe.getAll());
        registration.addRecipes(ForgingRecipeCategory.TYPE, ForgingRecipeCategory.ForgingRecipe.getAll());
        registration.addRecipes(LifeFusionAnvilRecipeCategory.TYPE, LifeFusionAnvilRecipeCategory.LifeFusionRecipe.getAll());
        registration.addRecipes(UpgradeRecipeCategory.TYPE, UpgradeRecipeCategory.UpgradeRecipe.getAll());
        // 燃料生产链配方
        registration.addRecipes(LiquefactionRecipeCategory.TYPE, LiquefactionRecipeCategory.LiquefactionRecipe.getAll());
        registration.addRecipes(FuelProcessingRecipeCategory.TYPE, FuelProcessingRecipeCategory.FuelProcessingRecipe.getAll());
        registration.addRecipes(FuelMixingRecipeCategory.TYPE, FuelMixingRecipeCategory.FuelMixingRecipe.getAll());
        // 物品重构配方（衰竭结晶嬗变）
        registration.addRecipes(ReconstructRecipeCategory.TYPE, ReconstructRecipeCategory.ReconstructRecipe.getAll());
        // 单输入单输出处理机器配方
        registration.addRecipes(CompressorRecipeCategory.TYPE, CompressorRecipeCategory.getAll());
        registration.addRecipes(PulverizerRecipeCategory.TYPE, PulverizerRecipeCategory.getAll());
        registration.addRecipes(TransformerRecipeCategory.TYPE, TransformerRecipeCategory.getAll());
        registration.addRecipes(PlantCultivatorRecipeCategory.TYPE, PlantCultivatorRecipeCategory.getAll());

        // 催化器与收集器不是合成机器，用物品信息说明其功能与等级数值
        addIngredientInfo(registration, AkaishiCrystalBlocks.CHISHI_CATALYST_BASIC.get(), "jei.akaishi.catalyst_basic");
        addIngredientInfo(registration, AkaishiCrystalBlocks.CHISHI_CATALYST_MEDIUM.get(), "jei.akaishi.catalyst_medium");
        addIngredientInfo(registration, AkaishiCrystalBlocks.CHISHI_CATALYST_ADVANCED.get(), "jei.akaishi.catalyst_advanced");
        addIngredientInfo(registration, AkaishiCrystalBlocks.CHISHI_CATALYST_ULTIMATE.get(), "jei.akaishi.catalyst_ultimate");
        addIngredientInfo(registration, AkaishiCrystalBlocks.CHISHI_COLLECTOR_BASIC.get(), "jei.akaishi.collector_basic");
        addIngredientInfo(registration, AkaishiCrystalBlocks.CHISHI_COLLECTOR_MEDIUM.get(), "jei.akaishi.collector_medium");
        addIngredientInfo(registration, AkaishiCrystalBlocks.CHISHI_COLLECTOR_ADVANCED.get(), "jei.akaishi.collector_advanced");
        addIngredientInfo(registration, AkaishiCrystalBlocks.CHISHI_COLLECTOR_ULTIMATE.get(), "jei.akaishi.collector_ultimate");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.akaishiSpeedUpgrade.get(), "jei.akaishi.speed_upgrade");
        // 物品管道（4 级）：物流网络中继，等级越高每 tick 传输物品越多
        addIngredientInfo(registration, ModBlocks.CHISHI_ITEM_PIPE.get(), "jei.akaishi.item_pipe_basic");
        addIngredientInfo(registration, ModBlocks.CHISHI_ITEM_PIPE_ADVANCED.get(), "jei.akaishi.item_pipe_advanced");
        addIngredientInfo(registration, ModBlocks.CHISHI_ITEM_PIPE_ELITE.get(), "jei.akaishi.item_pipe_elite");
        addIngredientInfo(registration, ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE.get(), "jei.akaishi.item_pipe_ultimate");
        // 生命能量提纯器与固态物：双能量输入的固化设备，用物品信息说明数值
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_LIFE_PURIFIER.get(), "jei.akaishi.life_purifier");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.akaishiLifeEssenceSolid.get(), "jei.akaishi.life_essence_solid");

        // ===== 生命系统：样本 → 基因 → 器官 → 移植 全链路（物品信息说明机制） =====
        // 工具与原料
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.sampleCollector.get(), "jei.akaishi.sample_collector");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.lifeSample.get(), "jei.akaishi.life_sample");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.geneSequence.get(), "jei.akaishi.gene_sequence");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.akaishiPotion.get(), "jei.akaishi.potion");
        // 器官（9 槽位同一机制，以心脏为代表说明）
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.akaishiOrganHeart.get(), "jei.akaishi.organ");
        // 生命机器
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_GENE_ANALYZER.get(), "jei.akaishi.gene_analyzer");
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_CULTIVATOR.get(), "jei.akaishi.cultivator");
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_LIFE_STRUCT.get(), "jei.akaishi.life_struct");
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_SURGERY.get(), "jei.akaishi.surgery");
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_POTION_TABLE.get(), "jei.akaishi.potion_table");
        // 存储库
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_SAMPLE_VAULT.get(), "jei.akaishi.sample_vault");
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_ORGAN_VAULT.get(), "jei.akaishi.organ_vault");
        addIngredientInfo(registration, AkaishiLifeBlocks.CHISHI_POTION_CABINET.get(), "jei.akaishi.potion_cabinet");

        // ===== 反应堆体系：多方块发电（方块） =====
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_REACTOR_CONTROLLER.get(), "jei.akaishi.reactor_controller");
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_REACTOR_SHELL.get(), "jei.akaishi.reactor_shell");
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_ROD.get(), "jei.akaishi.reactor_fuel_rod");
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_REACTOR_COOLER.get(), "jei.akaishi.reactor_cooler");
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_REACTOR_CORE.get(), "jei.akaishi.reactor_core");
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_REACTOR_FUEL_PORT.get(), "jei.akaishi.reactor_fuel_port");
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_REACTOR_ENERGY_OUTPUT.get(), "jei.akaishi.reactor_energy_output");
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_REACTOR_WASTE_PORT.get(), "jei.akaishi.reactor_waste_port");
        addIngredientInfo(registration, AkaishiReactorBlocks.CHISHI_EXHAUSTED_BARREL.get(), "jei.akaishi.exhausted_barrel");
        // 散热片（5 品质共用同一说明）
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.heatSinkPoor.get(), "jei.akaishi.heat_sink");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.heatSinkNormal.get(), "jei.akaishi.heat_sink");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.heatSinkGood.get(), "jei.akaishi.heat_sink");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.heatSinkFine.get(), "jei.akaishi.heat_sink");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.heatSinkExquisite.get(), "jei.akaishi.heat_sink");
        // 燃料原料与燃料罐
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.endMixture.get(), "jei.akaishi.end_mixture");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.dragonMixture.get(), "jei.akaishi.dragon_mixture");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.sculkLifeform.get(), "jei.akaishi.sculk_lifeform");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.fuelCell.get(), "jei.akaishi.fuel_cell");
        // 燃料加工机
        addIngredientInfo(registration, AkaishiEnergyBlocks.CHISHI_ENERGY_LIQUEFIER.get(), "jei.akaishi.energy_liquefier");
        addIngredientInfo(registration, AkaishiEnergyBlocks.CHISHI_ENERGY_PROCESSOR.get(), "jei.akaishi.energy_processor");
        addIngredientInfo(registration, ModBlocks.CHISHI_FUEL_CANNER.get(), "jei.akaishi.fuel_canner");
        addIngredientInfo(registration, ModBlocks.CHISHI_FUEL_MIXER.get(), "jei.akaishi.fuel_mixer");

        // ===== 聚变燃料体系：赤能源聚合等离子体 → 填装器灌入反应棒 → 燃料棒 =====
        addIngredientInfo(registration, AkaishiFusionBlocks.CHISHI_FUSION_FUEL_AGGREGATOR.get(), "jei.akaishi.fusion_fuel_aggregator");
        addIngredientInfo(registration, ModBlocks.CHISHI_PLASMA_FILLER.get(), "jei.akaishi.plasma_filler");
        addIngredientInfo(registration, ModBlocks.CHISHI_PLASMA_PIPE.get(), "jei.akaishi.plasma_pipe");
        addIngredientInfo(registration, ModBlocks.CHISHI_PLASMA_TANK.get(), "jei.akaishi.plasma_tank");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.fusionRod.get(), "jei.akaishi.fusion_rod");
        // 3 种等离子体燃料棒共用同一说明（混合/下界/末地）
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.mixedPlasmaRod.get(), "jei.akaishi.plasma_rod");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.netherPlasmaRod.get(), "jei.akaishi.plasma_rod");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.endPlasmaRod.get(), "jei.akaishi.plasma_rod");

        // ===== 无线赤能源体系：身份卡认证 + 多方块终端 + 无线口（参考 MEK 量子传输/Flux） =====
        addIngredientInfo(registration, AkaishiWirelessBlocks.CHISHI_WIRELESS_TERMINAL.get(), "jei.akaishi.wireless");
        addIngredientInfo(registration, AkaishiWirelessBlocks.CHISHI_WIRELESS_SECURITY.get(), "jei.akaishi.wireless");
        addIngredientInfo(registration, AkaishiWirelessBlocks.CHISHI_WIRELESS_CORE.get(), "jei.akaishi.wireless");
        addIngredientInfo(registration, AkaishiWirelessBlocks.CHISHI_WIRELESS_INPUT_PORT.get(), "jei.akaishi.wireless");
        addIngredientInfo(registration, AkaishiWirelessBlocks.CHISHI_WIRELESS_OUTPUT_PORT.get(), "jei.akaishi.wireless");

        // ===== 活化产物系列获取说明：活化结晶（7）/ 活化成分（7）/ 衰竭结晶 / 生命灰烬 =====
        // 活化结晶：7 种同机制（生命离心机分离活化衰竭液体，共用说明）
        Item[] activatedCrystals = {
                com.example.akaishi.item.ModItems.activatedSculkCrystal.get(),
                com.example.akaishi.item.ModItems.activatedNetherCompoundCrystal.get(),
                com.example.akaishi.item.ModItems.activatedEndMixtureCrystal.get(),
                com.example.akaishi.item.ModItems.activatedAdvancedMixtureCrystal.get(),
                com.example.akaishi.item.ModItems.activatedPureCrystal.get(),
                com.example.akaishi.item.ModItems.activatedDragonCrystal.get(),
                com.example.akaishi.item.ModItems.activatedUltimateMixtureCrystal.get()};
        for (Item item : activatedCrystals) {
            addIngredientInfo(registration, item, "jei.akaishi.activated_crystal");
        }
        // 活化成分：7 种同机制（活化分馏器拆分对应活化结晶，共用说明）
        Item[] activatedComponents = {
                com.example.akaishi.item.ModItems.activatedSculkComponent.get(),
                com.example.akaishi.item.ModItems.activatedNetherCompoundComponent.get(),
                com.example.akaishi.item.ModItems.activatedEndMixtureComponent.get(),
                com.example.akaishi.item.ModItems.activatedAdvancedMixtureComponent.get(),
                com.example.akaishi.item.ModItems.activatedPureComponent.get(),
                com.example.akaishi.item.ModItems.activatedDragonComponent.get(),
                com.example.akaishi.item.ModItems.activatedUltimateMixtureComponent.get()};
        for (Item item : activatedComponents) {
            addIngredientInfo(registration, item, "jei.akaishi.activated_component");
        }
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.exhaustedCrystal.get(), "jei.akaishi.exhausted_crystal");
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.lifeAsh.get(), "jei.akaishi.life_ash");
        // 生命融合锭：无合成配方，为黑山羊之母祭坛仪式专属产物
        addIngredientInfo(registration, com.example.akaishi.item.ModItems.lifeFusionIngot.get(), "jei.akaishi.life_fusion_ingot");
    }

    private static void addIngredientInfo(IRecipeRegistration registration, net.minecraft.world.level.block.Block block, String langKey, Object... args) {
        registration.addIngredientInfo(new ItemStack(block), VanillaTypes.ITEM_STACK, Component.translatable(langKey, args));
    }

    private static void addIngredientInfo(IRecipeRegistration registration, net.minecraft.world.level.block.Block block, String langKey) {
        addIngredientInfo(registration, block, langKey, new Object[0]);
    }

    private static void addIngredientInfo(IRecipeRegistration registration, net.minecraft.world.item.Item item, String langKey) {
        registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable(langKey));
    }
}
