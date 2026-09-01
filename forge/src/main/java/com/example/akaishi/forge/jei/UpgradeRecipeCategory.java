package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.item.AkaishiUpgradeHelper;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.GuiWidgets;
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

import java.util.Arrays;
import java.util.List;

/**
 * JEI 展示的"赤红升级"配方类别：
 * 20M 赤能源 + 1 升级模板 + 1 升级槽位 → 为赤石装备应用一种特殊能力（每种最多 3 级）。
 */
public class UpgradeRecipeCategory implements IRecipeCategory<UpgradeRecipeCategory.UpgradeRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<UpgradeRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "upgrade", UpgradeRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private final IDrawable background;
    private final IDrawable icon;

    public UpgradeRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 68);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_UPGRADE_STATION.get()));
    }

    @Override
    public RecipeType<UpgradeRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.upgrade");
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
    public void setRecipe(IRecipeLayoutBuilder builder, UpgradeRecipe recipe, IFocusGroup focuses) {
        // 槽位坐标与游戏内升级台一致：装备 44,30 / 模板 62,30 / 输出 116,30
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.gear());
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.template());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.gear());
    }

    @Override
    public void draw(UpgradeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 与游戏内一致自绘槽位框
        GuiWidgets.slotBox(guiGraphics, 44, 30);
        GuiWidgets.slotBox(guiGraphics, 62, 30);
        GuiWidgets.slotBox(guiGraphics, 116, 30);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable("jei.akaishi.cost_upgrade"), 8, 52, 0xFF404040);
        guiGraphics.drawString(Minecraft.getInstance().font, Component.translatable(recipe.ability().tooltipKey, 1).plainCopy(),
                8, 61, 0xFFA03030);
    }

    /** 升级配方展示数据 */
    public record UpgradeRecipe(AkaishiUpgradeHelper.SpecialAbility ability, ItemStack gear, ItemStack template) {

        /** 全部升级配方：6 种特殊能力（以赤石头盔为代表装备） */
        public static List<UpgradeRecipe> getAll() {
            ItemStack gear = new ItemStack(ModItems.akaishiHelmet.get());
            ItemStack template = new ItemStack(ModItems.akaishiUpgradeTemplate.get());
            return Arrays.stream(AkaishiUpgradeHelper.SpecialAbility.values())
                    .map(ability -> new UpgradeRecipe(ability, gear, template))
                    .toList();
        }
    }
}
