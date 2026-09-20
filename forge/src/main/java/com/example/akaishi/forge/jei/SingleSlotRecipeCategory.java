package com.example.akaishi.forge.jei;

import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.menu.GuiWidgets;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 单输入单输出机器的 JEI 配方类别抽象基类（压缩机/打粉机/变化器/植物培养机共用）。
 * 背景与槽位采用 vanilla 灰自绘，与游戏内单格机器界面风格一致。
 */
public abstract class SingleSlotRecipeCategory implements IRecipeCategory<SingleSlotRecipeCategory.Recipe> {

    /** 单输入单输出配方展示数据（输入为候选列表，含标签展开） */
    public record Recipe(List<ItemStack> inputs, ItemStack output) {
    }

    protected final RecipeType<Recipe> type;
    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    protected SingleSlotRecipeCategory(IGuiHelper helper, RecipeType<Recipe> type,
                                       ItemStack iconStack, String titleKey) {
        this.type = type;
        this.background = helper.createBlankDrawable(176, 60);
        this.icon = helper.createDrawableItemStack(iconStack);
        this.title = Component.translatable(titleKey);
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return type;
    }

    @Override
    public Component getTitle() {
        return title;
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
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 30).addIngredients(VanillaTypes.ITEM_STACK, recipe.inputs());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphics gui, double mouseX, double mouseY) {
        GuiWidgets.panel(gui, 0, 0, 176, 60);
        GuiWidgets.slotBox(gui, 44, 30);
        GuiWidgets.slotBox(gui, 116, 30);
        drawExtra(gui);
    }

    /** 子类可选覆写，用于绘制额外说明（如植物培养机「输入不消耗」） */
    protected void drawExtra(GuiGraphics gui) {
    }

    /** 从数据包配方生成 JEI 展示配方（标签原料展开为全部候选） */
    protected static List<Recipe> fromProcessRecipes(List<AkaishiItemProcessRecipe> processRecipes) {
        List<Recipe> list = new ArrayList<>(processRecipes.size());
        for (AkaishiItemProcessRecipe process : processRecipes) {
            List<ItemStack> inputs = new ArrayList<>();
            for (ItemStack candidate : process.ingredient().getItems()) {
                inputs.add(new ItemStack(candidate.getItem(), process.inputCount()));
            }
            list.add(new Recipe(List.copyOf(inputs), process.result().copy()));
        }
        return list;
    }
}
