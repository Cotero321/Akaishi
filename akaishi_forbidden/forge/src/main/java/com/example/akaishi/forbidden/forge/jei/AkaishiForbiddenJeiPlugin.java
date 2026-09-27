package com.example.akaishi.forbidden.forge.jei;

import com.example.akaishi.forbidden.AkaishiForbiddenMod;
import com.example.akaishi.forbidden.item.AkaishiForbiddenItems;
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
 * 禁忌模块的 JEI 集成入口（P3d 随生命融合 / 母神祭坛配方类别从本体 {@code AkaishiModJeiPlugin} 迁入）。
 *
 * <p>只注册随内容迁走的两个类别：生命融合锻台与母神祭坛仪式；
 * 其余类别仍由本体插件注册。插件 UID 使用本模块命名空间以避免与本体插件冲突。
 */
@JeiPlugin
public class AkaishiForbiddenJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(AkaishiForbiddenMod.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        // 生命融合锻台：赤石装备 + 生命融合锭 → 生命融合装备（获得途径展示）
        // 母神祭坛仪式：巨坛主祭品 + 外圈 8 座子祭坛供奉 → 生命融合锭 / 四件禁断饰品
        registration.addRecipeCategories(new LifeFusionAnvilRecipeCategory(helper),
                new AkaishiAltarRecipeCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(LifeFusionAnvilRecipeCategory.TYPE,
                LifeFusionAnvilRecipeCategory.LifeFusionRecipe.getAll());
        // 母神祭坛仪式配方（旧 1 条 + 新 4 条）
        registration.addRecipes(AkaishiAltarRecipeCategory.TYPE, AkaishiAltarRecipeCategory.AltarRecipe.getAll());
        // 生命融合锭：无合成配方，为黑山羊之母祭坛仪式专属产物
        registration.addIngredientInfo(new ItemStack(AkaishiForbiddenItems.lifeFusionIngot.get()),
                VanillaTypes.ITEM_STACK, Component.translatable("jei.akaishi.life_fusion_ingot"));
    }
}
