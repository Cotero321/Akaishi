package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiEnergyBlocks;
import com.example.akaishi.craft.recipe.AkaishiFluidProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.config.ModConfig;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"燃料加工"配方类别（能量处理器）：
 * 生命固态物 + 液态能量 → 反应堆燃料（复合 1000→1000mb / 至纯 100→50mb，消耗 5M 赤能源）。
 * 配方数据直接复用 {@link AkaishiEnergyProcessorBlockEntity#compoundRecipe} / {@link AkaishiEnergyProcessorBlockEntity#pureRecipe}。
 * 槽位布局：固态物 44,30 / 液体输入 62,30 / 液体输出 116,30。
 */
public class FuelProcessingRecipeCategory implements IRecipeCategory<FuelProcessingRecipeCategory.FuelProcessingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<FuelProcessingRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "fuel_processing", FuelProcessingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FuelProcessingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(AkaishiEnergyBlocks.CHISHI_ENERGY_PROCESSOR.get()));
    }

    @Override
    public RecipeType<FuelProcessingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.fuel_processing");
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
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.catalysts());
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
        // 成本读实时配置（本机成本来自 ModConfig.energyProcessorChishiCost，不写在配方里）
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.cost_process", fmt(ModConfig.energyProcessorChishiCost)),
                10, 51, 0xFFFFFFFF);
    }

    /** 赤能源缩写：50M / 10M / 5K */
    private static String fmt(long v) {
        if (v >= 1_000_000L) {
            return (v / 1_000_000L) + "M";
        }
        if (v >= 1_000L) {
            return (v / 1_000L) + "K";
        }
        return String.valueOf(v);
    }

    /** 加工配方展示数据（源自数据包配方） */
    public record FuelProcessingRecipe(List<ItemStack> catalysts, Fluid inputFluid, Fluid outputFluid,
                                      long inputAmount, long outputAmount) {

        /** 全部加工配方：取数据包里"一路进液 + 一路出液"的那些（顺序即配方表顺序） */
        public static List<FuelProcessingRecipe> getAll(RecipeManager manager) {
            List<AkaishiFluidProcessRecipe> sources =
                    AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.PROCESSING.get());
            List<FuelProcessingRecipe> list = new ArrayList<>(sources.size());
            for (AkaishiFluidProcessRecipe source : sources) {
                List<AkaishiFluidProcessRecipe.FluidSpec> ins = source.fluidInputs();
                AkaishiFluidProcessRecipe.FluidSpec out = source.fluidOutput();
                if (ins.size() != 1 || out == null) {
                    continue; // 本机固定"一路进液 + 一路出液"
                }
                List<ItemStack> catalysts = new ArrayList<>();
                if (source.ingredient() != null) {
                    for (ItemStack candidate : source.ingredient().getItems()) {
                        catalysts.add(new ItemStack(candidate.getItem(), source.inputCount()));
                    }
                }
                list.add(new FuelProcessingRecipe(List.copyOf(catalysts), ins.get(0).fluid(), out.fluid(),
                        ins.get(0).amount(), out.amount()));
            }
            return List.copyOf(list);
        }
    }
}
