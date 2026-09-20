package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiPurifierBlockEntity;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI 展示的"赤石提纯"配方类别：粗制赤石块 + 燃料 → 赤石精华。
 * 背景使用提纯机 GUI 贴图，槽位坐标与游戏内一致（输入 56,17 / 输出 116,35 / 燃料 56,53）。
 */
public class PurificationRecipeCategory implements IRecipeCategory<PurificationRecipeCategory.PurificationRecipe> {

    /** JEI 配方类型标识，注册配方与类别共用 */
    public static final RecipeType<PurificationRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "purification", PurificationRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_purifier.png");

    private final IDrawable background;
    private final IDrawable icon;

    public PurificationRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 72);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_PURIFIER.get()));
    }

    @Override
    public RecipeType<PurificationRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.purification");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PurificationRecipe recipe, IFocusGroup focuses) {
        // 槽位坐标与游戏内提纯机一致：输入 56,17 / 输出 116,35 / 燃料 56,53
        builder.addSlot(RecipeIngredientRole.INPUT, 56, 17).addIngredient(VanillaTypes.ITEM_STACK, recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 35).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
        builder.addSlot(RecipeIngredientRole.INPUT, 56, 53).addIngredient(VanillaTypes.ITEM_STACK, recipe.fuel());
    }

    @Override
    public void draw(PurificationRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 与游戏内一致自绘槽位框
        GuiWidgets.slotBox(guiGraphics, 56, 17);
        GuiWidgets.slotBox(guiGraphics, 116, 35);
        GuiWidgets.slotBox(guiGraphics, 56, 53);
    }

    /** 提纯配方展示数据 */
    public record PurificationRecipe(ItemStack input, ItemStack fuel, ItemStack output) {

        /**
         * 全部展示配方：<b>由机器侧的 {@link AkaishiPurifierBlockEntity#INPUT_OUTPUTS}
         * × {@link AkaishiPurifierBlockEntity#FUEL_ENERGIES} 组合生成</b>。
         * <p>
         * 原先是 JEI 手抄 3 条，与机器规则不等价：机器允许「2 种输入 × 2 种燃料」共 4 种组合，
         * 而 JEI 漏了「赤石水晶块 + 粗制赤石块」这条；抄写既不全也会过期，故改为按表生成。
         */
        public static List<PurificationRecipe> getAll() {
            List<PurificationRecipe> list = new ArrayList<>(
                    AkaishiPurifierBlockEntity.INPUT_OUTPUTS.size()
                            * AkaishiPurifierBlockEntity.FUEL_ENERGIES.size());
            for (Map.Entry<Item, Integer> input : AkaishiPurifierBlockEntity.INPUT_OUTPUTS.entrySet()) {
                ItemStack inputStack = new ItemStack(input.getKey());
                ItemStack output = new ItemStack(ModItems.akaishiEssence.get(), input.getValue());
                for (Item fuel : AkaishiPurifierBlockEntity.FUEL_ENERGIES.keySet()) {
                    list.add(new PurificationRecipe(inputStack.copy(), new ItemStack(fuel), output.copy()));
                }
            }
            return List.copyOf(list);
        }
    }
}
