package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
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
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"生命离心分离"配方类别（生命离心机）：
 * 活化燃料 X mB → 对应活化结晶 ×1 + 衰竭结晶 ×1（副产）。
 * <p><b>配方真源 = 数据包</b>（{@code data/akaishi/recipes/centrifuging/*.json}，
 * 类型 {@code akaishi:centrifuging}）；一批多少 mB 也由配方声明。
 * <p>版式：流体 44,30 / 主产物 80,30 / 副产 116,30 + 两行深色信息条。
 */
public class LifeCentrifugingRecipeCategory implements IRecipeCategory<LifeCentrifugingRecipeCategory.CentrifugingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<CentrifugingRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "life_centrifuging", CentrifugingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int INPUT_X = 44;
    private static final int OUTPUT_X = 80;
    private static final int BYPRODUCT_X = 116;
    private static final int SLOT_Y = 30;
    /** 卡片比常规高 12px：信息条要放两行（槽底 45 之下 → 48 与 58） */
    private static final int CARD_HEIGHT = 72;
    private static final int BAR_TOP = 46;
    private static final int BAR_BOTTOM = 69;
    private static final int LINE1_Y = 48;
    private static final int LINE2_Y = 58;

    private final IDrawable background;
    private final IDrawable icon;

    public LifeCentrifugingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, CARD_HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_LIFE_CENTRIFUGE.get()));
    }

    @Override
    public RecipeType<CentrifugingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.life_centrifuging");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CentrifugingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, SLOT_Y)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.fuel(), (int) recipe.batchMb()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, SLOT_Y).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
        if (!recipe.byproduct().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, BYPRODUCT_X, SLOT_Y)
                    .addIngredient(VanillaTypes.ITEM_STACK, recipe.byproduct());
        }
    }

    @Override
    public void draw(CentrifugingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, INPUT_X, SLOT_Y);
        GuiWidgets.slotBox(guiGraphics, OUTPUT_X, SLOT_Y);
        if (!recipe.byproduct().isEmpty()) {
            GuiWidgets.slotBox(guiGraphics, BYPRODUCT_X, SLOT_Y);
        }
        guiGraphics.fill(8, BAR_TOP, 168, BAR_BOTTOM, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.life_centrifuging.note_batch", recipe.batchMb()),
                10, LINE1_Y, 0xFFFFFFFF);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.life_centrifuging.note_byproduct"), 10, LINE2_Y, 0xFFFFFFFF);
    }

    /** 一条离心配方：活化燃料（流体）+ 一批用量 → 活化结晶（+ 衰竭结晶副产） */
    public record CentrifugingRecipe(Fluid fuel, long batchMb, ItemStack output, ItemStack byproduct) {

        /** 全部离心配方：取数据包里"一路进液 + 一个主产物"的那些（顺序即配方表顺序） */
        public static List<CentrifugingRecipe> getAll(RecipeManager manager) {
            List<AkaishiFluidProcessRecipe> sources =
                    AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.CENTRIFUGING.get());
            List<CentrifugingRecipe> list = new ArrayList<>(sources.size());
            for (AkaishiFluidProcessRecipe source : sources) {
                List<AkaishiFluidProcessRecipe.FluidSpec> ins = source.fluidInputs();
                if (ins.size() != 1 || source.result().isEmpty()) {
                    continue; // 本机固定"一路进液 + 一个主产物"
                }
                list.add(new CentrifugingRecipe(ins.get(0).fluid(), ins.get(0).amount(),
                        source.result().copy(), source.byproduct().copy()));
            }
            return List.copyOf(list);
        }
    }
}
