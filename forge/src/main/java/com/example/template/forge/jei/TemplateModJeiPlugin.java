package com.example.template.forge.jei;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI 集成入口：注册提纯配方类别、展示配方，并为催化器/收集器提供物品信息说明。
 */
@JeiPlugin
public class TemplateModJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(TemplateMod.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new PurificationRecipeCategory(helper),
                new AggregationRecipeCategory(helper),
                new ForgingRecipeCategory(helper),
                new UpgradeRecipeCategory(helper),
                // 燃料生产链：液化 → 加工 → 调和（燃料产生展示）
                new LiquefactionRecipeCategory(helper),
                new FuelProcessingRecipeCategory(helper),
                new FuelMixingRecipeCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PurificationRecipeCategory.TYPE, PurificationRecipeCategory.PurificationRecipe.getAll());
        registration.addRecipes(AggregationRecipeCategory.TYPE, AggregationRecipeCategory.AggregationRecipe.getAll());
        registration.addRecipes(ForgingRecipeCategory.TYPE, ForgingRecipeCategory.ForgingRecipe.getAll());
        registration.addRecipes(UpgradeRecipeCategory.TYPE, UpgradeRecipeCategory.UpgradeRecipe.getAll());
        // 燃料生产链配方
        registration.addRecipes(LiquefactionRecipeCategory.TYPE, LiquefactionRecipeCategory.LiquefactionRecipe.getAll());
        registration.addRecipes(FuelProcessingRecipeCategory.TYPE, FuelProcessingRecipeCategory.FuelProcessingRecipe.getAll());
        registration.addRecipes(FuelMixingRecipeCategory.TYPE, FuelMixingRecipeCategory.FuelMixingRecipe.getAll());

        // 催化器与收集器不是合成机器，用物品信息说明其功能与等级数值
        addIngredientInfo(registration, ModBlocks.CHISHI_CATALYST_BASIC.get(), "jei.template_mod.catalyst_basic");
        addIngredientInfo(registration, ModBlocks.CHISHI_CATALYST_MEDIUM.get(), "jei.template_mod.catalyst_medium");
        addIngredientInfo(registration, ModBlocks.CHISHI_CATALYST_ADVANCED.get(), "jei.template_mod.catalyst_advanced");
        addIngredientInfo(registration, ModBlocks.CHISHI_CATALYST_ULTIMATE.get(), "jei.template_mod.catalyst_ultimate");
        addIngredientInfo(registration, ModBlocks.CHISHI_COLLECTOR_BASIC.get(), "jei.template_mod.collector_basic");
        addIngredientInfo(registration, ModBlocks.CHISHI_COLLECTOR_MEDIUM.get(), "jei.template_mod.collector_medium");
        addIngredientInfo(registration, ModBlocks.CHISHI_COLLECTOR_ADVANCED.get(), "jei.template_mod.collector_advanced");
        addIngredientInfo(registration, ModBlocks.CHISHI_COLLECTOR_ULTIMATE.get(), "jei.template_mod.collector_ultimate");
        addIngredientInfo(registration, com.example.template.item.ModItems.chishiSpeedUpgrade.get(), "jei.template_mod.speed_upgrade");
        // 物品管道（4 级）：物流网络中继，等级越高每 tick 传输物品越多
        addIngredientInfo(registration, ModBlocks.CHISHI_ITEM_PIPE.get(), "jei.template_mod.item_pipe_basic");
        addIngredientInfo(registration, ModBlocks.CHISHI_ITEM_PIPE_ADVANCED.get(), "jei.template_mod.item_pipe_advanced");
        addIngredientInfo(registration, ModBlocks.CHISHI_ITEM_PIPE_ELITE.get(), "jei.template_mod.item_pipe_elite");
        addIngredientInfo(registration, ModBlocks.CHISHI_ITEM_PIPE_ULTIMATE.get(), "jei.template_mod.item_pipe_ultimate");
        // 生命能量提纯器与固态物：双能量输入的固化设备，用物品信息说明数值
        addIngredientInfo(registration, ModBlocks.CHISHI_LIFE_PURIFIER.get(), "jei.template_mod.life_purifier");
        addIngredientInfo(registration, com.example.template.item.ModItems.chishiLifeEssenceSolid.get(), "jei.template_mod.life_essence_solid");

        // ===== 生命系统：样本 → 基因 → 器官 → 移植 全链路（物品信息说明机制） =====
        // 工具与原料
        addIngredientInfo(registration, com.example.template.item.ModItems.sampleCollector.get(), "jei.template_mod.sample_collector");
        addIngredientInfo(registration, com.example.template.item.ModItems.lifeSample.get(), "jei.template_mod.life_sample");
        addIngredientInfo(registration, com.example.template.item.ModItems.geneSequence.get(), "jei.template_mod.gene_sequence");
        addIngredientInfo(registration, com.example.template.item.ModItems.chishiPotion.get(), "jei.template_mod.potion");
        // 器官（9 槽位同一机制，以心脏为代表说明）
        addIngredientInfo(registration, com.example.template.item.ModItems.chishiOrganHeart.get(), "jei.template_mod.organ");
        // 生命机器
        addIngredientInfo(registration, ModBlocks.CHISHI_GENE_ANALYZER.get(), "jei.template_mod.gene_analyzer");
        addIngredientInfo(registration, ModBlocks.CHISHI_CULTIVATOR.get(), "jei.template_mod.cultivator");
        addIngredientInfo(registration, ModBlocks.CHISHI_LIFE_STRUCT.get(), "jei.template_mod.life_struct");
        addIngredientInfo(registration, ModBlocks.CHISHI_SURGERY.get(), "jei.template_mod.surgery");
        addIngredientInfo(registration, ModBlocks.CHISHI_POTION_TABLE.get(), "jei.template_mod.potion_table");
        // 存储库
        addIngredientInfo(registration, ModBlocks.CHISHI_SAMPLE_VAULT.get(), "jei.template_mod.sample_vault");
        addIngredientInfo(registration, ModBlocks.CHISHI_ORGAN_VAULT.get(), "jei.template_mod.organ_vault");
        addIngredientInfo(registration, ModBlocks.CHISHI_POTION_CABINET.get(), "jei.template_mod.potion_cabinet");

        // ===== 反应堆体系：多方块发电（方块） =====
        addIngredientInfo(registration, ModBlocks.CHISHI_REACTOR_CONTROLLER.get(), "jei.template_mod.reactor_controller");
        addIngredientInfo(registration, ModBlocks.CHISHI_REACTOR_SHELL.get(), "jei.template_mod.reactor_shell");
        addIngredientInfo(registration, ModBlocks.CHISHI_REACTOR_FUEL_ROD.get(), "jei.template_mod.reactor_fuel_rod");
        addIngredientInfo(registration, ModBlocks.CHISHI_REACTOR_COOLER.get(), "jei.template_mod.reactor_cooler");
        addIngredientInfo(registration, ModBlocks.CHISHI_REACTOR_CORE.get(), "jei.template_mod.reactor_core");
        addIngredientInfo(registration, ModBlocks.CHISHI_REACTOR_FUEL_PORT.get(), "jei.template_mod.reactor_fuel_port");
        addIngredientInfo(registration, ModBlocks.CHISHI_REACTOR_ENERGY_OUTPUT.get(), "jei.template_mod.reactor_energy_output");
        addIngredientInfo(registration, ModBlocks.CHISHI_REACTOR_WASTE_PORT.get(), "jei.template_mod.reactor_waste_port");
        addIngredientInfo(registration, ModBlocks.CHISHI_EXHAUSTED_BARREL.get(), "jei.template_mod.exhausted_barrel");
        // 散热片（5 品质共用同一说明）
        addIngredientInfo(registration, com.example.template.item.ModItems.heatSinkPoor.get(), "jei.template_mod.heat_sink");
        addIngredientInfo(registration, com.example.template.item.ModItems.heatSinkNormal.get(), "jei.template_mod.heat_sink");
        addIngredientInfo(registration, com.example.template.item.ModItems.heatSinkGood.get(), "jei.template_mod.heat_sink");
        addIngredientInfo(registration, com.example.template.item.ModItems.heatSinkFine.get(), "jei.template_mod.heat_sink");
        addIngredientInfo(registration, com.example.template.item.ModItems.heatSinkExquisite.get(), "jei.template_mod.heat_sink");
        // 燃料原料与燃料罐
        addIngredientInfo(registration, com.example.template.item.ModItems.endMixture.get(), "jei.template_mod.end_mixture");
        addIngredientInfo(registration, com.example.template.item.ModItems.dragonMixture.get(), "jei.template_mod.dragon_mixture");
        addIngredientInfo(registration, com.example.template.item.ModItems.sculkLifeform.get(), "jei.template_mod.sculk_lifeform");
        addIngredientInfo(registration, com.example.template.item.ModItems.fuelCell.get(), "jei.template_mod.fuel_cell");
        // 燃料加工机
        addIngredientInfo(registration, ModBlocks.CHISHI_ENERGY_LIQUEFIER.get(), "jei.template_mod.energy_liquefier");
        addIngredientInfo(registration, ModBlocks.CHISHI_ENERGY_PROCESSOR.get(), "jei.template_mod.energy_processor");
        addIngredientInfo(registration, ModBlocks.CHISHI_FUEL_CANNER.get(), "jei.template_mod.fuel_canner");
        addIngredientInfo(registration, ModBlocks.CHISHI_FUEL_MIXER.get(), "jei.template_mod.fuel_mixer");

        // ===== 无线赤能源体系：身份卡认证 + 多方块终端 + 无线口（参考 MEK 量子传输/Flux） =====
        addIngredientInfo(registration, ModBlocks.CHISHI_WIRELESS_TERMINAL.get(), "jei.template_mod.wireless");
        addIngredientInfo(registration, ModBlocks.CHISHI_WIRELESS_SECURITY.get(), "jei.template_mod.wireless");
        addIngredientInfo(registration, ModBlocks.CHISHI_WIRELESS_CORE.get(), "jei.template_mod.wireless");
        addIngredientInfo(registration, ModBlocks.CHISHI_WIRELESS_INPUT_PORT.get(), "jei.template_mod.wireless");
        addIngredientInfo(registration, ModBlocks.CHISHI_WIRELESS_OUTPUT_PORT.get(), "jei.template_mod.wireless");
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
