package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.block.entity.AkaishiEquipmentForgerBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.AkaishiUpgradeHelper;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.EnergyFormat;
import com.example.akaishi.menu.GuiWidgets;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"赤石装备打造"配方类别：
 * 赤能源 + 赤石锭 + 下界合金装备 → 赤石装备（半定制，初始 4 升级槽）。
 */
public class ForgingRecipeCategory implements IRecipeCategory<ForgingRecipeCategory.ForgingRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<ForgingRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "forging", ForgingRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private final IDrawable background;
    private final IDrawable icon;

    public ForgingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.CHISHI_EQUIPMENT_FORGER.get()));
    }

    @Override
    public RecipeType<ForgingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.forging");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ForgingRecipe recipe, IFocusGroup focuses) {
        // 槽位坐标与游戏内锻造台一致：装备 38,30 / 锭 62,30 / 输出 116,30
        builder.addSlot(RecipeIngredientRole.INPUT, 38, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.base());
        builder.addSlot(RecipeIngredientRole.INPUT, 62, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.ingot());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
    }

    @Override
    public void draw(ForgingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 与游戏内一致自绘槽位框
        GuiWidgets.slotBox(guiGraphics, 38, 30);
        GuiWidgets.slotBox(guiGraphics, 62, 30);
        GuiWidgets.slotBox(guiGraphics, 116, 30);
        // 成本读实时配置：与机器侧 getCurrentCost() 同算式（基础价 + 投点数 × 单点价，单点为常量）
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.cost_forge",
                        EnergyFormat.formatFloor(ModConfig.equipmentForgerEnergyPerForge),
                        EnergyFormat.formatFloor(AkaishiUpgradeHelper.ENERGY_PER_BASE_UPGRADE)),
                8, 52, 0xFF404040);
    }

    /** 打造配方展示数据 */
    public record ForgingRecipe(ItemStack base, ItemStack ingot, ItemStack output) {

        /**
         * 全部打造配方：<b>直接遍历机器侧的 {@link AkaishiEquipmentForgerBlockEntity#FORGE_RECIPES}</b>。
         * <p>
         * 原先是 JEI 自带一份抄写，结果机器有 8 条（含镐/锹/斧）而 JEI 只列了 5 条 ——
         * 抄写的部分必然与真源脱节，故此处不再保留第二份数据。
         */
        public static List<ForgingRecipe> getAll() {
            List<ForgingRecipe> list = new ArrayList<>(AkaishiEquipmentForgerBlockEntity.FORGE_RECIPES.size());
            for (AkaishiEquipmentForgerBlockEntity.ForgeRecipe recipe
                    : AkaishiEquipmentForgerBlockEntity.FORGE_RECIPES) {
                list.add(new ForgingRecipe(new ItemStack(recipe.input()),
                        new ItemStack(ModItems.akaishiIngot.get(), recipe.ingotCost()),
                        new ItemStack(recipe.result().get())));
            }
            return List.copyOf(list);
        }
    }
}
