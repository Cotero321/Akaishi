package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiFuelMixerBlockEntity;
import com.example.akaishi.fluid.ModFluids;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * JEI 展示的"燃料调和"配方类别（燃料混合机）：
 * 两种燃料液体 1:1:1 → 高阶混合燃料（消耗 2M 赤能源）。
 * 配方数据直接复用 {@link AkaishiFuelMixerBlockEntity#recipeFor}，保证与机器逻辑一致。
 * 槽位布局：液体输入Ⅰ 44,30 / 输入Ⅱ 62,30 / 液体输出 116,30。
 */
public class FuelMixingRecipeCategory implements IRecipeCategory<FuelMixingRecipeCategory.FuelMixingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<FuelMixingRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "fuel_mixing", FuelMixingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FuelMixingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_FUEL_MIXER.get()));
    }

    @Override
    public RecipeType<FuelMixingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.fuel_mixing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FuelMixingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 30)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.in1(), (int) recipe.in1Amount()));
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 30)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.in2(), (int) recipe.in2Amount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 30)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.out(), (int) recipe.outAmount()));
    }

    @Override
    public void draw(FuelMixingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, 44, 30);
        GuiWidgets.slotBox(guiGraphics, 62, 30);
        GuiWidgets.slotBox(guiGraphics, 116, 30);
        // 深色信息条 + 白字：描边融入深底，文字清晰锐利
        guiGraphics.fill(8, 50, 168, 59, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.cost_mix"), 10, 51, 0xFFFFFFFF);
    }

    /** 调和配方展示数据（源自混合机机器配方） */
    public record FuelMixingRecipe(Fluid in1, long in1Amount, Fluid in2, long in2Amount, Fluid out, long outAmount) {

        /** 全部调和配方：高级（末地+下界复合）与终极（巨龙+至纯） */
        public static List<FuelMixingRecipe> getAll() {
            Fluid end = ModFluids.get(ModFluids.END_MIXTURE_FUEL_ID);
            Fluid compound = ModFluids.get(ModFluids.NETHER_COMPOUND_FUEL_ID);
            Fluid dragon = ModFluids.get(ModFluids.DRAGON_FUEL_ID);
            Fluid pure = ModFluids.get(ModFluids.PURE_FUEL_ID);
            return List.of(
                    from(AkaishiFuelMixerBlockEntity.recipeFor(end, compound)),
                    from(AkaishiFuelMixerBlockEntity.recipeFor(dragon, pure)));
        }

        private static FuelMixingRecipe from(AkaishiFuelMixerBlockEntity.Recipe r) {
            return new FuelMixingRecipe(r.in1(), r.in1Amount(), r.in2(), r.in2Amount(), r.out(), r.outAmount());
        }
    }
}
