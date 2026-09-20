package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import net.minecraft.world.item.crafting.RecipeManager;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 赤石打粉机配方：矿物/赤石/黑曜石打成粉末。 */
public class PulverizerRecipeCategory extends SingleSlotRecipeCategory {

    public static final RecipeType<Recipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "pulverizer", Recipe.class);

    public PulverizerRecipeCategory(IGuiHelper helper) {
        super(helper, TYPE, new ItemStack(ModBlocks.CHISHI_PULVERIZER.get()), "jei.akaishi.pulverizer");
    }

    public static List<Recipe> getAll(RecipeManager manager) {
        return fromProcessRecipes(AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.PULVERIZING.get()));
    }
}
