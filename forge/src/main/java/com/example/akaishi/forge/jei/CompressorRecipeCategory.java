package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiCompressorBlockEntity;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 赤石压缩机配方：粉末压缩为块、赤石粉压缩为赤石精华。 */
public class CompressorRecipeCategory extends SingleSlotRecipeCategory {

    public static final RecipeType<Recipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "compressor", Recipe.class);

    public CompressorRecipeCategory(IGuiHelper helper) {
        super(helper, TYPE, new ItemStack(ModBlocks.CHISHI_COMPRESSOR.get()), "jei.akaishi.compressor");
    }

    public static List<Recipe> getAll() {
        return fromRecipes(AkaishiCompressorBlockEntity.RECIPES);
    }
}
