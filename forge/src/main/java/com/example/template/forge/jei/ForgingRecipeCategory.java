package com.example.template.forge.jei;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import com.example.template.item.ModItems;
import com.example.template.menu.GuiWidgets;
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

import java.util.List;

/**
 * JEI 展示的"赤石装备打造"配方类别：
 * 赤能源 + 赤石锭 + 下界合金装备 → 赤石装备（半定制，初始 4 升级槽）。
 */
public class ForgingRecipeCategory implements IRecipeCategory<ForgingRecipeCategory.ForgingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<ForgingRecipe> TYPE =
            RecipeType.create(TemplateMod.MOD_ID, "forging", ForgingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    private final IDrawable background;
    private final IDrawable icon;

    public ForgingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_EQUIPMENT_FORGER.get()));
    }

    @Override
    public RecipeType<ForgingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.template_mod.forging");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ForgingRecipe recipe, IFocusGroup focuses) {
        // 槽位坐标与游戏内锻造台一致：装备 38,30 / 锭 62,30 / 输出 116,30
        builder.addSlot(RecipeIngredientRole.INPUT, 38, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.base());
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.ingot());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
    }

    @Override
    public void draw(ForgingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 与游戏内一致自绘槽位框
        GuiWidgets.slotBox(guiGraphics, 38, 30);
        GuiWidgets.slotBox(guiGraphics, 62, 30);
        GuiWidgets.slotBox(guiGraphics, 116, 30);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("jei.template_mod.cost_forge"), 8, 52, 0xFF404040);
    }

    /** 打造配方展示数据 */
    public record ForgingRecipe(ItemStack base, ItemStack ingot, ItemStack output) {

        /** 全部打造配方：下界合金五件套 → 赤石装备 */
        public static List<ForgingRecipe> getAll() {
            return List.of(
                    new ForgingRecipe(new ItemStack(Items.NETHERITE_HELMET), ingots(5), new ItemStack(ModItems.chishiHelmet.get())),
                    new ForgingRecipe(new ItemStack(Items.NETHERITE_CHESTPLATE), ingots(8), new ItemStack(ModItems.chishiChestplate.get())),
                    new ForgingRecipe(new ItemStack(Items.NETHERITE_LEGGINGS), ingots(7), new ItemStack(ModItems.chishiLeggings.get())),
                    new ForgingRecipe(new ItemStack(Items.NETHERITE_BOOTS), ingots(4), new ItemStack(ModItems.chishiBoots.get())),
                    new ForgingRecipe(new ItemStack(Items.NETHERITE_SWORD), ingots(2), new ItemStack(ModItems.chishiSword.get())));
        }

        private static ItemStack ingots(int count) {
            return new ItemStack(ModItems.chishiIngot.get(), count);
        }
    }
}
