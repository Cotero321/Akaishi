package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiPlantCultivatorBlockEntity;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 赤石植物培养机配方：种子/茎秆培养为成熟作物（输入保留不消耗）。 */
public class PlantCultivatorRecipeCategory extends SingleSlotRecipeCategory {

    public static final RecipeType<Recipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "plant_cultivator", Recipe.class);

    public PlantCultivatorRecipeCategory(IGuiHelper helper) {
        super(helper, TYPE, new ItemStack(ModBlocks.CHISHI_PLANT_CULTIVATOR.get()), "jei.akaishi.plant_cultivator");
    }

    public static List<Recipe> getAll() {
        return fromRecipes(AkaishiPlantCultivatorBlockEntity.RECIPES);
    }

    @Override
    protected void drawExtra(GuiGraphics gui) {
        gui.drawString(Minecraft.getInstance().font, Component.translatable("jei.akaishi.plant_cultivator_keep"), 8, 52, 0xFF404040);
    }
}
