package com.example.template.forge.jei;

import com.example.template.TemplateMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 集成入口：注册提纯配方类别与展示配方。
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
    }
}
