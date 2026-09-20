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

/** 赤石变化器配方：物质变化为基底（青金石粉 → 冷却基底、矿物 → 矿石基底）。 */
public class TransformerRecipeCategory extends SingleSlotRecipeCategory {

    public static final RecipeType<Recipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "transformer", Recipe.class);

    public TransformerRecipeCategory(IGuiHelper helper) {
        super(helper, TYPE, new ItemStack(ModBlocks.CHISHI_TRANSFORMER.get()), "jei.akaishi.transformer");
    }

    public static List<Recipe> getAll(RecipeManager manager) {
        return fromProcessRecipes(AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.TRANSFORMING.get()));
    }
}
