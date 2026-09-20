package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiPlasmaFillerBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.fluid.ModFluids;
import com.example.akaishi.item.ModItems;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"等离子燃料棒灌装"配方类别（等离子填装器）：
 * 空反应棒 ×1 + 等离子体 X mB → 对应燃料棒 ×1（带燃料 NBT）。
 * <p><b>配方数据直接读机器侧的 {@link AkaishiPlasmaFillerBlockEntity#ROD_RECIPES}</b>
 * （下标即机器罐索引），不另抄一份 —— 避免"罐序 / 产物"在两处各自维护。
 * <p>版式与燃料链其它卡片一致：空反应棒 44,30 / 等离子体 62,30 / 燃料棒 116,30 + 深色信息条。
 */
public class PlasmaFillingRecipeCategory implements IRecipeCategory<PlasmaFillingRecipeCategory.FillingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<FillingRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "plasma_filling", FillingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int ROD_X = 44;
    private static final int PLASMA_X = 62;
    private static final int SLOT_Y = 30;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 30;
    /** 深色信息条：y=50..59（与燃料链其它卡片同款，白字在深底上清晰） */
    private static final int BAR_TOP = 50;
    private static final int BAR_BOTTOM = 59;

    private final IDrawable background;
    private final IDrawable icon;

    public PlasmaFillingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_PLASMA_FILLER.get()));
    }

    @Override
    public RecipeType<FillingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.plasma_filling");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FillingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, ROD_X, SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.fusionRod.get()));
        builder.addSlot(RecipeIngredientRole.INPUT, PLASMA_X, SLOT_Y)
                .addIngredient(ForgeTypes.FLUID_STACK,
                        new FluidStack(ModFluids.get(recipe.plasmaId()), (int) ModConfig.fillerPlasmaPerRod));
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(recipe.rod()));
    }

    @Override
    public void draw(FillingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, ROD_X, SLOT_Y);
        GuiWidgets.slotBox(guiGraphics, PLASMA_X, SLOT_Y);
        GuiWidgets.slotBox(guiGraphics, OUTPUT_X, OUTPUT_Y);
        guiGraphics.fill(8, BAR_TOP, 168, BAR_BOTTOM, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.plasma_filling.note", ModConfig.fillerPlasmaPerRod),
                10, BAR_TOP + 1, 0xFFFFFFFF);
    }

    /** 一条灌装配方：等离子体（按流体 id 记）+ 产出的燃料棒 */
    public record FillingRecipe(String plasmaId, Item rod) {

        /** 全部灌装配方：直接映射机器侧 {@code ROD_RECIPES}（顺序即罐索引，稳定） */
        public static List<FillingRecipe> getAll() {
            List<AkaishiPlasmaFillerBlockEntity.RodRecipe> sources = AkaishiPlasmaFillerBlockEntity.ROD_RECIPES;
            List<FillingRecipe> list = new ArrayList<>(sources.size());
            for (AkaishiPlasmaFillerBlockEntity.RodRecipe source : sources) {
                list.add(new FillingRecipe(source.plasmaId(), source.rod()));
            }
            return List.copyOf(list);
        }
    }
}
