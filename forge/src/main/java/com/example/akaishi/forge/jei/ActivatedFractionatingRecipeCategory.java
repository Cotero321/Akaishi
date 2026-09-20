package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"活化分馏"配方类别（活化分馏器）：
 * 活化结晶 ×1 → 对应活化成分 ×1 + 衰竭结晶 ×1（副产）。
 * <p><b>配方真源 = 数据包</b>（{@code data/akaishi/recipes/fractionating/*.json}，
 * 类型 {@code akaishi:fractionating}）；机器侧也读同一份，JEI 只做展示映射。
 * 副产由配方声明（{@code byproduct} 可省），无副产时不画副产槽。
 * <p>版式与燃料链其它卡片一致：输入 44,30 / 主产物 80,30 / 副产 116,30 + 深色信息条。
 */
public class ActivatedFractionatingRecipeCategory implements IRecipeCategory<ActivatedFractionatingRecipeCategory.FractionatingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<FractionatingRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "activated_fractionating", FractionatingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int INPUT_X = 44;
    private static final int OUTPUT_X = 80;
    private static final int BYPRODUCT_X = 116;
    private static final int SLOT_Y = 30;
    private static final int BAR_TOP = 50;
    private static final int BAR_BOTTOM = 59;

    private final IDrawable background;
    private final IDrawable icon;

    public ActivatedFractionatingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_ACTIVATED_FRACTIONATOR.get()));
    }

    @Override
    public RecipeType<FractionatingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.activated_fractionating");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FractionatingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.inputs());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
        // 副产单独一个输出槽（配方没写 byproduct 时该槽不出现）
        if (!recipe.byproduct().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, BYPRODUCT_X, SLOT_Y)
                    .addIngredient(VanillaTypes.ITEM_STACK, recipe.byproduct());
        }
    }

    @Override
    public void draw(FractionatingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, INPUT_X, SLOT_Y);
        GuiWidgets.slotBox(guiGraphics, OUTPUT_X, SLOT_Y);
        if (!recipe.byproduct().isEmpty()) {
            GuiWidgets.slotBox(guiGraphics, BYPRODUCT_X, SLOT_Y);
        }
        guiGraphics.fill(8, BAR_TOP, 168, BAR_BOTTOM, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.activated_fractionating.note"), 10, BAR_TOP + 1, 0xFFFFFFFF);
    }

    /** 一条分馏配方：候选输入（标签展开）+ 主产物 + 副产（可空） */
    public record FractionatingRecipe(List<ItemStack> inputs, ItemStack output, ItemStack byproduct) {

        /** 从数据包配方生成展示列表（顺序即配方表顺序） */
        public static List<FractionatingRecipe> getAll(RecipeManager manager) {
            List<AkaishiItemProcessRecipe> sources =
                    AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.FRACTIONATING.get());
            List<FractionatingRecipe> list = new ArrayList<>(sources.size());
            for (AkaishiItemProcessRecipe source : sources) {
                // 标签原料展开为全部候选，数量取配方声明的单格消耗
                List<ItemStack> inputs = new ArrayList<>();
                for (ItemStack candidate : source.ingredient().getItems()) {
                    inputs.add(new ItemStack(candidate.getItem(), source.inputCount()));
                }
                list.add(new FractionatingRecipe(List.copyOf(inputs), source.result().copy(),
                        source.byproduct().copy()));
            }
            return List.copyOf(list);
        }
    }
}
