package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.craft.MachineProcessEnergy;
import com.example.akaishi.craft.recipe.AkaishiFluidProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.EnergyFormat;
import com.example.akaishi.menu.GuiWidgets;
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
 * JEI 展示的"生命活化"配方类别（生命活化器）：
 * 衰竭燃料 → 对应活化燃料，<b>按 1:1 转化</b>，每 mB 消耗生命能量。
 * <p><b>配方真源 = 数据包</b>（{@code data/akaishi/recipes/activating/*.json}，
 * 类型 {@code akaishi:activating}）；成本读 {@link MachineProcessEnergy}，
 * 与加工报价、机器实扣同一口径（配置热重载后即时反映）。
 * <p>版式：输入流体 44,30 / 输出流体 116,30 + 深色信息条。
 */
public class LifeActivatingRecipeCategory implements IRecipeCategory<LifeActivatingRecipeCategory.ActivatingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<ActivatingRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "life_activating", ActivatingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int INPUT_X = 44;
    private static final int OUTPUT_X = 116;
    private static final int SLOT_Y = 30;
    private static final int BAR_TOP = 50;
    private static final int BAR_BOTTOM = 59;

    private final IDrawable background;
    private final IDrawable icon;

    public LifeActivatingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_LIFE_ACTIVATOR.get()));
    }

    @Override
    public RecipeType<ActivatingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.life_activating");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ActivatingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.exhausted(), (int) recipe.batchMb()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.activated(), (int) recipe.batchMb()));
    }

    @Override
    public void draw(ActivatingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, INPUT_X, SLOT_Y);
        GuiWidgets.slotBox(guiGraphics, OUTPUT_X, SLOT_Y);
        guiGraphics.fill(8, BAR_TOP, 168, BAR_BOTTOM, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.life_activating.note", recipe.batchMb(),
                        EnergyFormat.format(recipe.lifeCost())),
                10, BAR_TOP + 1, 0xFFFFFFFF);
    }

    /**
     * 一条活化配方：衰竭燃料 → 活化燃料（1:1）。
     *
     * @param batchMb  计价单位（配方声明的 mB）
     * @param lifeCost 该批量的生命能量成本（取自 {@link MachineProcessEnergy}，与机器同源）
     */
    public record ActivatingRecipe(Fluid exhausted, Fluid activated, long batchMb, long lifeCost) {

        /** 全部活化配方：取数据包里"一路进液 + 一路出液"的那些（顺序即配方表顺序） */
        public static List<ActivatingRecipe> getAll(RecipeManager manager) {
            List<AkaishiFluidProcessRecipe> sources =
                    AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.ACTIVATING.get());
            List<ActivatingRecipe> list = new ArrayList<>(sources.size());
            for (AkaishiFluidProcessRecipe source : sources) {
                List<AkaishiFluidProcessRecipe.FluidSpec> ins = source.fluidInputs();
                AkaishiFluidProcessRecipe.FluidSpec out = source.fluidOutput();
                if (ins.size() != 1 || out == null) {
                    continue; // 本机固定"一进一出"
                }
                list.add(new ActivatingRecipe(ins.get(0).fluid(), out.fluid(), ins.get(0).amount(),
                        MachineProcessEnergy.costPerRun(source).life()));
            }
            return List.copyOf(list);
        }
    }
}
