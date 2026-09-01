package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiItemReconstructorBlockEntity;
import com.example.akaishi.item.ModItems;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI 展示的"物品重构"配方类别：
 * 原料 + N 衰竭结晶 → 产物（配方表见 {@link AkaishiItemReconstructorBlockEntity#RECIPES}）。
 * 槽位布局与游戏内重构仪一致：原料 26 / 代价结晶 62 / 产物 98。
 */
public class ReconstructRecipeCategory implements IRecipeCategory<ReconstructRecipeCategory.ReconstructRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<ReconstructRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "reconstruct", ReconstructRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");

    private final IDrawable background;
    private final IDrawable icon;

    public ReconstructRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 68);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_ITEM_RECONSTRUCTOR.get()));
    }

    @Override
    public RecipeType<ReconstructRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.reconstruct");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ReconstructRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 26, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.input()));
        // 代价结晶槽显示所需数量（游戏内结晶槽 mayPlace 限制衰竭结晶）
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.crystalStack());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 98, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.output()));
    }

    @Override
    public void draw(ReconstructRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 与游戏内一致自绘槽位框
        GuiWidgets.slotBox(guiGraphics, 26, 30);
        GuiWidgets.slotBox(guiGraphics, 62, 30);
        GuiWidgets.slotBox(guiGraphics, 98, 30);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.reconstruct_cost", recipe.crystalCost()), 8, 52, 0xFF404040);
    }

    /** 重构配方展示数据 */
    public record ReconstructRecipe(Item input, Item output, int crystalCost) {

        /** 代价结晶物品（数量=所需结晶数） */
        public ItemStack crystalStack() {
            return new ItemStack(ModItems.exhaustedCrystal.get(), crystalCost);
        }

        /** 全部重构配方：直接读取游戏内配方表 */
        public static List<ReconstructRecipe> getAll() {
            return AkaishiItemReconstructorBlockEntity.RECIPES.entrySet().stream()
                    .map(e -> new ReconstructRecipe(e.getKey(), e.getValue().output(), e.getValue().crystalCost()))
                    .toList();
        }
    }
}
