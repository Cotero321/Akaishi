package com.example.template.forge.jei;

import com.example.template.TemplateMod;
import com.example.template.block.ModBlocks;
import com.example.template.item.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"赤石提纯"配方类别：粗制赤石块 + 燃料 → 赤石精华。
 * 提纯逻辑目前为硬编码，此处以展示用配方列表呈现给玩家。
 */
public class PurificationRecipeCategory implements IRecipeCategory<PurificationRecipeCategory.PurificationRecipe> {

    /** JEI 配方类型标识，注册配方与类别共用 */
    public static final RecipeType<PurificationRecipe> TYPE =
            RecipeType.create(TemplateMod.MOD_ID, "purification", PurificationRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/jei_purification.png");

    private final IDrawable background;
    private final IDrawable icon;

    public PurificationRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 116, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_PURIFIER.get()));
    }

    @Override
    public RecipeType<PurificationRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.template_mod.purification");
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
    public void setRecipe(IRecipeLayoutBuilder builder, PurificationRecipe recipe, IFocusGroup focuses) {
        // 输入：粗制赤石块 + 燃料（赤石晶或粗制块），输出：赤石精华
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 23).addIngredient(VanillaTypes.ITEM_STACK, recipe.input());
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 23).addIngredient(VanillaTypes.ITEM_STACK, recipe.fuel());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 23).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
    }

    /** 提纯配方展示数据 */
    public record PurificationRecipe(ItemStack input, ItemStack fuel, ItemStack output) {

        /** 生成全部展示配方：不同燃料对应不同配方 */
        public static List<PurificationRecipe> getAll() {
            ItemStack raw = new ItemStack(ModBlocks.RAW_CHISHI_BLOCK.get());
            ItemStack crystalBlock = new ItemStack(ModBlocks.CHISHI_CRYSTAL_BLOCK.get());
            ItemStack essence = new ItemStack(ModItems.chishiEssence.get());
            ItemStack essence4 = new ItemStack(ModItems.chishiEssence.get(), 4);
            List<PurificationRecipe> recipes = new ArrayList<>();
            // 粗制赤石块 + 赤石晶 → 1 精华
            recipes.add(new PurificationRecipe(raw.copy(), new ItemStack(ModItems.chishiCrystal.get()), essence.copy()));
            // 粗制赤石块 + 粗制赤石块 → 1 精华
            recipes.add(new PurificationRecipe(raw.copy(), raw.copy(), essence.copy()));
            // 赤石水晶块 + 赤石晶 → 4 精华
            recipes.add(new PurificationRecipe(crystalBlock.copy(), new ItemStack(ModItems.chishiCrystal.get()), essence4.copy()));
            return recipes;
        }
    }
}
