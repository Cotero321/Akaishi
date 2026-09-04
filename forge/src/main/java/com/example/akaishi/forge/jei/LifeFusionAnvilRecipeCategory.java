package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI「生命融合锻台」配方类别：
 * 赤石装备 + 1 枚生命融合锭 → 生命融合装备（完整保留原装备升级数据，不消耗能量）。
 * 说明文本按 168px 宽度自动折行，卡片高度随行数自适应，避免溢出框体。
 */
public class LifeFusionAnvilRecipeCategory implements IRecipeCategory<LifeFusionAnvilRecipeCategory.LifeFusionRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<LifeFusionRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "life_fusion_anvil", LifeFusionRecipe.class);

    /** 说明文本（中/英文案按各自语言显示，长度不同故需折行自适应） */
    private static final Component TIP = Component.translatable("jei.akaishi.life_fusion_anvil_tip");
    private static final int TIP_WIDTH = 168;
    private static final int LINE_HEIGHT = 10;

    private final IDrawable background;
    private final IDrawable icon;
    private final int tipLines;
    private final int slotY;
    private final int cardHeight;

    public LifeFusionAnvilRecipeCategory(IGuiHelper helper) {
        // 依据当前语言文案的实际换行数动态布局，保证中/英文都不出界
        this.tipLines = Math.max(1, Minecraft.getInstance().font.split(TIP, TIP_WIDTH).size());
        this.slotY = 12 + tipLines * LINE_HEIGHT;
        this.cardHeight = slotY + 44;
        this.background = helper.createBlankDrawable(176, cardHeight);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_LIFE_FUSION_ANVIL.get()));
    }

    @Override
    public RecipeType<LifeFusionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.life_fusion_anvil");
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
    public void setRecipe(IRecipeLayoutBuilder builder, LifeFusionRecipe recipe, IFocusGroup focuses) {
        // 槽位坐标与游戏内融合锻台一致：赤石装备 / 融合锭 / 生命融合装备
        builder.addSlot(RecipeIngredientRole.INPUT, 26, slotY).addIngredient(VanillaTypes.ITEM_STACK, recipe.base());
        builder.addSlot(RecipeIngredientRole.INPUT, 80, slotY).addIngredient(VanillaTypes.ITEM_STACK, recipe.ingot());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 134, slotY).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
    }

    @Override
    public void draw(LifeFusionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.panel(guiGraphics, 0, 0, 176, cardHeight);
        GuiWidgets.slotBox(guiGraphics, 26, slotY);
        GuiWidgets.slotBox(guiGraphics, 80, slotY);
        GuiWidgets.slotBox(guiGraphics, 134, slotY);
        // 说明按换行结果逐行绘制，永不超出面板宽度
        Font font = Minecraft.getInstance().font;
        int y = 8;
        for (FormattedCharSequence line : font.split(TIP, TIP_WIDTH)) {
            guiGraphics.drawString(font, line, 8, y, 0xFF404040);
            y += LINE_HEIGHT;
        }
    }

    /** 生命融合配方展示数据 */
    public record LifeFusionRecipe(ItemStack base, ItemStack ingot, ItemStack output) {

        /** 全部融合配方：赤石护甲 4 件 → 生命融合对应件（各消耗 1 枚融合锭） */
        public static List<LifeFusionRecipe> getAll() {
            return List.of(
                    new LifeFusionRecipe(new ItemStack(ModItems.akaishiHelmet.get()), fusionIngot(),
                            new ItemStack(ModItems.lifeFusionHelmet.get())),
                    new LifeFusionRecipe(new ItemStack(ModItems.akaishiChestplate.get()), fusionIngot(),
                            new ItemStack(ModItems.lifeFusionChestplate.get())),
                    new LifeFusionRecipe(new ItemStack(ModItems.akaishiLeggings.get()), fusionIngot(),
                            new ItemStack(ModItems.lifeFusionLeggings.get())),
                    new LifeFusionRecipe(new ItemStack(ModItems.akaishiBoots.get()), fusionIngot(),
                            new ItemStack(ModItems.lifeFusionBoots.get())));
        }

        private static ItemStack fusionIngot() {
            return new ItemStack(ModItems.lifeFusionIngot.get());
        }
    }
}
