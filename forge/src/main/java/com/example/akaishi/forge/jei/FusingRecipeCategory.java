package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiMechanicalBlocks;
import com.example.akaishi.craft.recipe.AkaishiEnergyProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.EnergyFormat;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的「材料融合」配方类别：两种原料 + 赤能源 → 一件机械材料。
 * <p>
 * <b>配方真源 = 数据包</b>（{@code data/akaishi/recipes/fusing/*.json}），JEI 只做展示；
 * 背景与槽位采用 vanilla 灰自绘（与游戏内材料融合器界面同一套 {@link GuiWidgets} 控件）。
 * 两个输入槽<b>不分先后</b>，与机器侧匹配口径一致。
 */
public class FusingRecipeCategory implements IRecipeCategory<FusingRecipeCategory.Recipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<Recipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "material_fusing", Recipe.class);

    /** 展示数据：两格输入候选（含标签展开与逐格数量）+ 产物 + 单次耗能 */
    public record Recipe(List<ItemStack> inputA, List<ItemStack> inputB, ItemStack output, long cost) {
    }

    private final IDrawable background;
    private final IDrawable icon;

    public FusingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(AkaishiMechanicalBlocks.CHISHI_MATERIAL_FUSER.get()));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.fusing");
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
        builder.addSlot(RecipeIngredientRole.INPUT, 26, 30).addIngredients(VanillaTypes.ITEM_STACK, recipe.inputA());
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 30).addIngredients(VanillaTypes.ITEM_STACK, recipe.inputB());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slotsView, GuiGraphics gui, double mouseX, double mouseY) {
        GuiWidgets.panel(gui, 0, 0, 176, 60);
        GuiWidgets.slotBox(gui, 26, 30);
        GuiWidgets.slotBox(gui, 62, 30);
        GuiWidgets.slotBox(gui, 116, 30);
        // 两原料之间「+」、原料到产物「→」，点明"两原料不分先后"
        gui.drawString(Minecraft.getInstance().font, "+", 48, 34, 0xFF404040, false);
        gui.drawString(Minecraft.getInstance().font, "→", 88, 34, 0xFF404040, false);
        // 单次耗能（成本读配方声明，与机器侧同源）
        gui.drawString(Minecraft.getInstance().font,
                Component.translatable("gui.akaishi.material_fuser.cost", EnergyFormat.format(recipe.cost())),
                4, 52, 0xFF404040);
    }

    /** 从数据包配方生成展示列表（标签原料展开为全部候选，数量取该格声明值） */
    public static List<Recipe> getAll(RecipeManager manager) {
        List<AkaishiEnergyProcessRecipe> recipes =
                AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.FUSING.get());
        List<Recipe> list = new ArrayList<>(recipes.size());
        for (AkaishiEnergyProcessRecipe recipe : recipes) {
            Ingredient first = recipe.ingredient();
            if (first == null) {
                continue;
            }
            List<ItemStack> inputA = expand(first, recipe.inputCount());
            List<ItemStack> inputB = recipe.ingredient2() == null
                    ? List.of()
                    : expand(recipe.ingredient2(), recipe.inputCount2());
            list.add(new Recipe(inputA, inputB, recipe.result().copy(), recipe.energy()));
        }
        return list;
    }

    private static List<ItemStack> expand(Ingredient ingredient, int count) {
        ItemStack[] candidates = ingredient.getItems();
        List<ItemStack> stacks = new ArrayList<>(candidates.length);
        for (ItemStack candidate : candidates) {
            stacks.add(new ItemStack(candidate.getItem(), Math.max(1, count)));
        }
        return stacks;
    }
}
