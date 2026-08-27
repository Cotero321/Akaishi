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
                new UpgradeRecipeCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PurificationRecipeCategory.TYPE, PurificationRecipeCategory.PurificationRecipe.getAll());
        registration.addRecipes(AggregationRecipeCategory.TYPE, AggregationRecipeCategory.AggregationRecipe.getAll());
        registration.addRecipes(ForgingRecipeCategory.TYPE, ForgingRecipeCategory.ForgingRecipe.getAll());
        registration.addRecipes(UpgradeRecipeCategory.TYPE, UpgradeRecipeCategory.UpgradeRecipe.getAll());

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
    }

    private static void addIngredientInfo(IRecipeRegistration registration, net.minecraft.world.level.block.Block block, String langKey) {
        registration.addIngredientInfo(new ItemStack(block), VanillaTypes.ITEM_STACK, Component.translatable(langKey));
    }

    private static void addIngredientInfo(IRecipeRegistration registration, net.minecraft.world.item.Item item, String langKey) {
        registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable(langKey));
    }
}
