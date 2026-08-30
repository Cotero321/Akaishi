package com.example.template.forge.jei;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import com.example.template.block.entity.ChishiEnergyProcessorBlockEntity;
import com.example.template.item.ModItems;
import com.example.template.menu.GuiWidgets;
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
 * JEI 展示的"燃料加工"配方类别（能量处理器）：
 * 生命固态物 + 液态能量 → 反应堆燃料（复合 1000→1000mb / 至纯 100→50mb，消耗 5M 赤能源）。
 * 配方数据直接复用 {@link ChishiEnergyProcessorBlockEntity#compoundRecipe} / {@link ChishiEnergyProcessorBlockEntity#pureRecipe}。
 * 槽位布局：固态物 44,30 / 液体输入 62,30 / 液体输出 116,30。
 */
public class FuelProcessingRecipeCategory implements IRecipeCategory<FuelProcessingRecipeCategory.FuelProcessingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<FuelProcessingRecipe> TYPE =
            RecipeType.create(TemplateMod.MOD_ID, "fuel_processing", FuelProcessingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FuelProcessingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_ENERGY_PROCESSOR.get()));
    }

    @Override
    public RecipeType<FuelProcessingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.template_mod.fuel_processing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FuelProcessingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 44, 30)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.chishiLifeEssenceSolid.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 30)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.inputFluid(), (int) recipe.inputAmount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 30)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.outputFluid(), (int) recipe.outputAmount()));
    }

    @Override
    public void draw(FuelProcessingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, 44, 30);
        GuiWidgets.slotBox(guiGraphics, 62, 30);
        GuiWidgets.slotBox(guiGraphics, 116, 30);
        // 深色信息条 + 白字：描边融入深底，文字清晰锐利
        guiGraphics.fill(8, 50, 168, 59, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.template_mod.cost_process"), 10, 51, 0xFFFFFFFF);
    }

    /** 加工配方展示数据（源自加工器机器配方） */
    public record FuelProcessingRecipe(Fluid inputFluid, Fluid outputFluid, long inputAmount, long outputAmount) {

        /** 全部加工配方：复合（1000mb）与至纯（100→50mb 浓缩） */
        public static List<FuelProcessingRecipe> getAll() {
            return List.of(
                    from(ChishiEnergyProcessorBlockEntity.compoundRecipe()),
                    from(ChishiEnergyProcessorBlockEntity.pureRecipe()));
        }

        private static FuelProcessingRecipe from(ChishiEnergyProcessorBlockEntity.Recipe r) {
            return new FuelProcessingRecipe(r.inputFluid(), r.outputFluid(), r.inputAmount(), r.outputAmount());
        }
    }
}
