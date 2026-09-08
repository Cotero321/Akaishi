package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiLifeBlocks;
import com.example.akaishi.block.entity.AkaishiTransgeneFactoryBlockEntity;
import com.example.akaishi.config.ModConfig;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"转基因合成"配方类别（转基因工厂）：
 * 怪物基因序列 + 缠怨藤 + 催化素材（凋零玫瑰/烈焰粉）+ 生命能量固态物 → 转基因植物种子。
 * 配方数据直接复用 {@link AkaishiTransgeneFactoryBlockEntity#RECIPES}，保证与机器逻辑一致。
 * 布局自绘原版灰面板：输入 26/44/62/80,30 基因/藤/催化/固态物，输出 134,30；下方为基因条件与能量耗时说明。
 */
public class TransgeneRecipeCategory implements IRecipeCategory<TransgeneRecipeCategory.TransgeneRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<TransgeneRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "transgene_factory", TransgeneRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public TransgeneRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(176, 84);
        this.icon = helper.createDrawableItemStack(new ItemStack(AkaishiLifeBlocks.CHISHI_TRANSGENE_FACTORY.get()));
    }

    @Override
    public RecipeType<TransgeneRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.transgene_factory");
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
    public void setRecipe(IRecipeLayoutBuilder builder, TransgeneRecipe recipe, IFocusGroup focuses) {
        // 输入与游戏内槽位一致：基因序列 / 缠怨藤 / 催化素材 / 生命能量固态物 → 产物种子
        builder.addSlot(RecipeIngredientRole.INPUT, 26, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.geneSequence.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.TWISTING_VINES));
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.catalyst()));
        builder.addSlot(RecipeIngredientRole.INPUT, 80, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.akaishiLifeEssenceSolid.get()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 134, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.output()));
    }

    @Override
    public void draw(TransgeneRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.panel(guiGraphics, 0, 0, 176, 84);
        GuiWidgets.slotBox(guiGraphics, 26, 30);
        GuiWidgets.slotBox(guiGraphics, 44, 30);
        GuiWidgets.slotBox(guiGraphics, 62, 30);
        GuiWidgets.slotBox(guiGraphics, 80, 30);
        GuiWidgets.slotBox(guiGraphics, 134, 30);
        // 深色信息条 + 白字：基因条件与能量/耗时说明
        guiGraphics.drawString(Minecraft.getInstance().font,
                Minecraft.getInstance().font.plainSubstrByWidth(
                        Component.translatable("jei.akaishi.transgene.gene_hint").getString(), 160), 8, 56, 0xFF404040);
        guiGraphics.fill(8, 69, 168, 80, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.transgene.cost_time",
                        fmt(ModConfig.transgeneFactoryLifeCost), fmtSeconds(ModConfig.transgeneFactoryProcessTicks)),
                10, 70, 0xFFFFFFFF);
    }

    /** 数值缩写（千分位）：5000 → 5,000 */
    private static String fmt(long v) {
        return String.format("%,d", v);
    }

    /** tick → 秒：100 tick → 5 秒 */
    private static String fmtSeconds(int ticks) {
        return ticks % 20 == 0 ? String.valueOf(ticks / 20) : String.format("%.1f", ticks / 20.0);
    }

    /** 转基因合成配方展示数据（源自机器配方表） */
    public record TransgeneRecipe(Item catalyst, Item output) {

        /** 全部配方：凋零骷髅基因 + 凋零玫瑰 → 凋零藤种子；烈焰人基因 + 烈焰粉 → 烈焰花种 */
        public static List<TransgeneRecipe> getAll() {
            List<TransgeneRecipe> list = new ArrayList<>();
            for (AkaishiTransgeneFactoryBlockEntity.TransgeneFactoryRecipe r : AkaishiTransgeneFactoryBlockEntity.RECIPES) {
                list.add(new TransgeneRecipe(r.catalyst(), r.output()));
            }
            return list;
        }
    }
}
