package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiFusionBlocks;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiFluidProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.EnergyFormat;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JEI 展示的"等离子体聚合"配方类别（聚变燃料聚合器）：
 * 活化成分 ×1 → 等离子体（混合 / 下界 / 末地）。<b>配方数据直接读机器侧的
 * {@link AkaishiFusionFuelAggregatorBlockEntity#PLASMA_INPUTS}</b>，不另抄一份。
 * <p>同一种等离子体有多个可输入成分（如混合等离子体可由 3 种活化成分产出），
 * 故按<b>等离子体</b>归并成 3 条配方，输入槽用候选列表展示全部可替代成分。
 * <p>版式与燃料链其它卡片一致：输入 44,30 / 输出 116,30 + 深色信息条。
 */
public class FusionFuelAggregatorRecipeCategory implements IRecipeCategory<FusionFuelAggregatorRecipeCategory.PlasmaRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<PlasmaRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "fusion_fuel_aggregation", PlasmaRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int INPUT_X = 44;
    private static final int INPUT_Y = 30;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 30;
    /** 深色信息条：y=50..59（与燃料链其它卡片同款，白字在深底上清晰） */
    private static final int BAR_TOP = 50;
    private static final int BAR_BOTTOM = 59;

    private final IDrawable background;
    private final IDrawable icon;

    public FusionFuelAggregatorRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(AkaishiFusionBlocks.CHISHI_FUSION_FUEL_AGGREGATOR.get()));
    }

    @Override
    public RecipeType<PlasmaRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.fusion_aggregate");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PlasmaRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.inputs());
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.plasma(), (int) recipe.amount()));
    }

    @Override
    public void draw(PlasmaRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, INPUT_X, INPUT_Y);
        GuiWidgets.slotBox(guiGraphics, OUTPUT_X, OUTPUT_Y);
        guiGraphics.fill(8, BAR_TOP, 168, BAR_BOTTOM, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.cost_fusion_aggregate",
                        EnergyFormat.format(ModConfig.aggregatorCostPerCraft)),
                10, BAR_TOP + 1, 0xFFFFFFFF);
    }

    /**
     * 一条聚合配方（按等离子体归并）：可接受的活化成分候选 + 产出的等离子体与产量。
     *
     * @param amount 产量：配方声明正数则用配方值，否则用机器配置
     */
    public record PlasmaRecipe(List<ItemStack> inputs, Fluid plasma, long amount) {

        /**
         * 按等离子体归并生成展示配方：同为一种等离子的成分合并成一个输入槽的候选列表。
         * <p>归并顺序 = 数据包配方的遍历顺序（LinkedHashMap 保序，不会每次启动换序）。
         */
        public static List<PlasmaRecipe> getAll(RecipeManager manager) {
            Map<Fluid, List<ItemStack>> byPlasma = new LinkedHashMap<>();
            for (AkaishiFluidProcessRecipe source
                    : AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.PLASMA_AGGREGATING.get())) {
                AkaishiFluidProcessRecipe.FluidSpec out = source.fluidOutput();
                if (out == null || source.ingredient() == null) {
                    continue;
                }
                List<ItemStack> inputs = byPlasma.computeIfAbsent(out.fluid(), key -> new ArrayList<>(3));
                for (ItemStack candidate : source.ingredient().getItems()) {
                    inputs.add(new ItemStack(candidate.getItem(), source.inputCount()));
                }
            }
            List<PlasmaRecipe> list = new ArrayList<>(byPlasma.size());
            for (Map.Entry<Fluid, List<ItemStack>> entry : byPlasma.entrySet()) {
                // 产量：配方没写就用机器配置（7 条配方共用同一个产量配置）
                list.add(new PlasmaRecipe(List.copyOf(entry.getValue()), entry.getKey(),
                        ModConfig.aggregatorProducePerCraft));
            }
            return List.copyOf(list);
        }
    }
}
