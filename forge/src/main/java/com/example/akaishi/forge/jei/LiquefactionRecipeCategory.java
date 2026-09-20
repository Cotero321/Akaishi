package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiEnergyBlocks;
import com.example.akaishi.craft.recipe.AkaishiFluidProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.GuiWidgets;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"燃料液化"配方类别（能量液化机）：
 * 高能量材料（+ 可选辅料）+ 赤能源 → 液态能量/燃料。
 * <p><b>配方真源 = 数据包</b>（{@code data/akaishi/recipes/liquefying/*.json}，
 * 类型 {@code akaishi:liquefying}）；机器侧读同一份，JEI 只做展示映射 ——
 * 辅料也不再写死（哪个物品当辅料由配方 {@code catalyst} 声明）。
 * <p>槽位布局与实机一致：材料 56,17 / 辅料 56,53（仅声明辅料的配方）/ 液体输出 116,35。
 */
public class LiquefactionRecipeCategory implements IRecipeCategory<LiquefactionRecipeCategory.LiquefactionRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<LiquefactionRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "liquefaction", LiquefactionRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int INPUT_X = 56;
    private static final int INPUT_Y = 17;
    private static final int CATALYST_X = 56;
    private static final int CATALYST_Y = 53;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 35;
    private static final int BAR_TOP = 73;
    private static final int BAR_BOTTOM = 82;

    private final IDrawable background;
    private final IDrawable icon;

    public LiquefactionRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 84);
        this.icon = helper.createDrawableItemStack(new ItemStack(AkaishiEnergyBlocks.CHISHI_ENERGY_LIQUEFIER.get()));
    }

    @Override
    public RecipeType<LiquefactionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.liquefaction");
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
    public void setRecipe(IRecipeLayoutBuilder builder, LiquefactionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.inputs());
        // 仅声明了 catalyst 的配方展示辅料槽（如末地/幽匿/巨龙燃料液化需 1 个生命固态物）
        if (!recipe.catalysts().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, CATALYST_X, CATALYST_Y)
                    .addIngredients(VanillaTypes.ITEM_STACK, recipe.catalysts());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.output(), (int) recipe.amount()));
    }

    @Override
    public void draw(LiquefactionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, INPUT_X, INPUT_Y);
        if (!recipe.catalysts().isEmpty()) {
            GuiWidgets.slotBox(guiGraphics, CATALYST_X, CATALYST_Y);
        }
        GuiWidgets.slotBox(guiGraphics, OUTPUT_X, OUTPUT_Y);
        // 深色信息条 + 白字：描边融入深底，文字清晰锐利
        guiGraphics.fill(8, BAR_TOP, 168, BAR_BOTTOM, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.cost_liquefy", fmt(recipe.cost())), 10, BAR_TOP + 1, 0xFFFFFFFF);
    }

    /** 赤能源缩写：50M / 10M / 5M */
    private static String fmt(long v) {
        if (v >= 1_000_000L) {
            return (v / 1_000_000L) + "M";
        }
        if (v >= 1_000L) {
            return (v / 1_000L) + "K";
        }
        return String.valueOf(v);
    }

    /**
     * 液化配方展示数据（源自数据包配方）。
     *
     * @param catalysts 辅料候选；空 = 本配方不需要辅料
     */
    public record LiquefactionRecipe(List<ItemStack> inputs, List<ItemStack> catalysts, long cost, long amount,
                                     Fluid output) {

        /** 全部液化配方：取数据包里"产液体"的那些（顺序即配方表顺序） */
        public static List<LiquefactionRecipe> getAll(RecipeManager manager) {
            List<AkaishiFluidProcessRecipe> sources =
                    AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.LIQUEFYING.get());
            List<LiquefactionRecipe> list = new ArrayList<>(sources.size());
            for (AkaishiFluidProcessRecipe source : sources) {
                AkaishiFluidProcessRecipe.FluidSpec fluidOut = source.fluidOutput();
                if (fluidOut == null) {
                    continue; // 本机只跑"产液体"的配方，无流体产物的配方不属于本类别
                }
                list.add(new LiquefactionRecipe(stacks(source.ingredient(), source.inputCount()),
                        stacks(source.catalyst(), source.inputCount()),
                        source.energy(), fluidOut.amount(), fluidOut.fluid()));
            }
            return List.copyOf(list);
        }

        /** 标签原料展开为全部候选，数量取配方声明的单格消耗；原料为空则返回空列表 */
        private static List<ItemStack> stacks(Ingredient ingredient, int count) {
            if (ingredient == null) {
                return List.of();
            }
            List<ItemStack> list = new ArrayList<>();
            for (ItemStack candidate : ingredient.getItems()) {
                list.add(new ItemStack(candidate.getItem(), count));
            }
            return List.copyOf(list);
        }
    }
}
