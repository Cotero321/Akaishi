package com.example.template.forge.jei;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import com.example.template.item.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"赤石能量聚合"配方类别：
 * 10M 赤能源 + 下界合金锭 → 赤石锭；10M 赤能源 + 母岩 → 上一等级母岩。
 */
public class AggregationRecipeCategory implements IRecipeCategory<AggregationRecipeCategory.AggregationRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<AggregationRecipe> TYPE =
            RecipeType.create(TemplateMod.MOD_ID, "aggregation", AggregationRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/jei_purification.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AggregationRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 116, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_ENERGY_AGGREGATOR.get()));
    }

    @Override
    public RecipeType<AggregationRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.template_mod.aggregation");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AggregationRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 23).addIngredient(VanillaTypes.ITEM_STACK, recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 23).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
    }

    @Override
    public void draw(AggregationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("jei.template_mod.cost_aggregate"), 40, 28, 0xFF404040);
    }

    /** 聚合配方展示数据 */
    public record AggregationRecipe(ItemStack input, ItemStack output) {

        /** 全部聚合配方：赤石锭 + 母岩逐级升级 */
        public static List<AggregationRecipe> getAll() {
            List<AggregationRecipe> list = new ArrayList<>();
            list.add(new AggregationRecipe(new ItemStack(Items.NETHERITE_INGOT), new ItemStack(ModItems.chishiIngot.get())));
            list.add(new AggregationRecipe(new ItemStack(ModBlocks.CHISHI_GEODE_FLAWED.get()), new ItemStack(ModBlocks.CHISHI_GEODE_NORMAL.get())));
            list.add(new AggregationRecipe(new ItemStack(ModBlocks.CHISHI_GEODE_NORMAL.get()), new ItemStack(ModBlocks.CHISHI_GEODE_PRISTINE.get())));
            list.add(new AggregationRecipe(new ItemStack(ModBlocks.CHISHI_GEODE_PRISTINE.get()), new ItemStack(ModBlocks.CHISHI_GEODE_PERFECT.get())));
            return list;
        }
    }
}
