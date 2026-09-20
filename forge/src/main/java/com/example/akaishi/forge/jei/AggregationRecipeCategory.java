package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiEnergyBlocks;
import com.example.akaishi.craft.MachineProcessEnergy;
import com.example.akaishi.craft.recipe.AkaishiEnergyProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.EnergyFormat;
import com.example.akaishi.menu.GuiWidgets;
import mezz.jei.api.constants.VanillaTypes;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 展示的"赤石能量聚合"配方类别：物品 + 赤能源 → 物品（赤石锭 / 母岩逐级升级）。
 * <p>
 * <b>配方真源 = 数据包</b>（{@code data/akaishi/recipes/aggregating/*.json}），
 * JEI 只做展示、不再自带一份列表 —— 否则数据包改了配方，JEI 还显示旧的那份（双份维护）。
 * 背景使用聚合机 GUI 贴图，槽位坐标与游戏内一致（输入 44,30 / 输出 116,30）。
 */
public class AggregationRecipeCategory implements IRecipeCategory<AkaishiEnergyProcessRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<AkaishiEnergyProcessRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "aggregation", AkaishiEnergyProcessRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private final IDrawable background;
    private final IDrawable icon;

    public AggregationRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 176, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(AkaishiEnergyBlocks.CHISHI_ENERGY_AGGREGATOR.get()));
    }

    @Override
    public RecipeType<AkaishiEnergyProcessRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.aggregation");
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
    public void setRecipe(IRecipeLayoutBuilder builder, AkaishiEnergyProcessRecipe recipe, IFocusGroup focuses) {
        // 槽位坐标与游戏内聚合机一致；纯能量配方（无 ingredient）不画输入槽
        Ingredient ingredient = recipe.ingredient();
        if (ingredient != null) {
            builder.addSlot(RecipeIngredientRole.INPUT, 44, 30).addIngredients(VanillaTypes.ITEM_STACK,
                    ingredientStacks(ingredient, recipe.inputCount()));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 30).addIngredient(VanillaTypes.ITEM_STACK, recipe.result());
    }

    @Override
    public void draw(AkaishiEnergyProcessRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 与游戏内一致自绘槽位框
        GuiWidgets.slotBox(guiGraphics, 44, 30);
        GuiWidgets.slotBox(guiGraphics, 116, 30);
        // 成本读真源：与机器侧同调 MachineProcessEnergy（配方声明 energy 优先，否则按 energy_config 档位取配置）
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.akaishi.cost_aggregate",
                        EnergyFormat.formatFloor(MachineProcessEnergy.costPerRun(recipe).chishi())),
                8, 52, 0xFF404040);
    }

    /** 从数据包配方生成展示列表（标签原料展开为全部候选，数量取配方声明的单格消耗） */
    public static List<AkaishiEnergyProcessRecipe> getAll(RecipeManager manager) {
        return AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.AGGREGATING.get());
    }

    /** 标签候选展开：每个候选带上游配方声明的单格消耗数量（输入数量不能被 Ingredient 表达） */
    private static List<ItemStack> ingredientStacks(Ingredient ingredient, int inputCount) {
        ItemStack[] candidates = ingredient.getItems();
        List<ItemStack> stacks = new ArrayList<>(candidates.length);
        for (ItemStack candidate : candidates) {
            stacks.add(new ItemStack(candidate.getItem(), Math.max(1, inputCount)));
        }
        return stacks;
    }
}
