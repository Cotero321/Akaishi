package com.example.template.forge.jei;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import com.example.template.block.entity.ChishiEnergyLiquefierBlockEntity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * JEI 展示的"燃料液化"配方类别（能量液化机）：
 * 高能量材料 + 可选生命固态物 + 赤能源 → 液态能量/燃料。
 * 配方数据直接复用 {@link ChishiEnergyLiquefierBlockEntity#recipeFor}，保证与机器逻辑一致。
 * 槽位布局：材料 56,17 / 固态物 56,53（仅需消耗的配方）/ 液体输出 116,35。
 */
public class LiquefactionRecipeCategory implements IRecipeCategory<LiquefactionRecipeCategory.LiquefactionRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<LiquefactionRecipe> TYPE =
            RecipeType.create(TemplateMod.MOD_ID, "liquefaction", LiquefactionRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_energy_cell.png");

    private final IDrawable background;
    private final IDrawable icon;

    public LiquefactionRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 84);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_ENERGY_LIQUEFIER.get()));
    }

    @Override
    public RecipeType<LiquefactionRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.template_mod.liquefaction");
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
        builder.addSlot(RecipeIngredientRole.INPUT, 56, 17).addIngredient(VanillaTypes.ITEM_STACK, recipe.input());
        // 仅需消耗生命固态物的配方展示固态槽（如末地/幽匿/巨龙燃料液化）
        if (recipe.needsSolid()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 56, 53)
                    .addIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.chishiLifeEssenceSolid.get()));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 35)
                .addIngredient(ForgeTypes.FLUID_STACK, new FluidStack(recipe.output(), (int) recipe.amount()));
    }

    @Override
    public void draw(LiquefactionRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, 56, 17);
        if (recipe.needsSolid()) {
            GuiWidgets.slotBox(guiGraphics, 56, 53);
        }
        GuiWidgets.slotBox(guiGraphics, 116, 35);
        // 深色信息条 + 白字：描边融入深底，文字清晰锐利
        guiGraphics.fill(8, 73, 168, 82, 0xC0282828);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.template_mod.cost_liquefy", fmt(recipe.cost())), 10, 74, 0xFFFFFFFF);
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

    /** 液化配方展示数据（源自液化机机器配方） */
    public record LiquefactionRecipe(ItemStack input, boolean needsSolid, long cost, long amount, Fluid output) {

        /** 全部液化配方：下界之星 / 凋零玫瑰 / 末地混合物 / 巨龙混合物 / 幽匿生命体 */
        public static List<LiquefactionRecipe> getAll() {
            return List.of(
                    from(ChishiEnergyLiquefierBlockEntity.recipeFor(new ItemStack(Items.NETHER_STAR))),
                    from(ChishiEnergyLiquefierBlockEntity.recipeFor(new ItemStack(Items.WITHER_ROSE))),
                    from(ChishiEnergyLiquefierBlockEntity.recipeFor(new ItemStack(ModItems.endMixture.get()))),
                    from(ChishiEnergyLiquefierBlockEntity.recipeFor(new ItemStack(ModItems.dragonMixture.get()))),
                    from(ChishiEnergyLiquefierBlockEntity.recipeFor(new ItemStack(ModItems.sculkLifeform.get()))));
        }

        private static LiquefactionRecipe from(ChishiEnergyLiquefierBlockEntity.Recipe r) {
            return new LiquefactionRecipe(r.input(), r.needsSolid(), r.cost(), r.amount(), r.output());
        }
    }
}
