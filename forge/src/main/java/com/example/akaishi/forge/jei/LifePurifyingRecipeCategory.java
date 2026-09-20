package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiLifeBlocks;
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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

/**
 * JEI「生命能量固化」配方类别：生命提纯器<b>没有物品输入</b>，
 * 靠赤能源（驱动）+ 生命能量（原料）直接固化成固态物。
 * <p>
 * <b>配方真源 = 数据包</b>（{@code data/akaishi/recipes/life_purifying/*.json}）；
 * <b>成本不与机器脱节</b>：每次绘制实时问 {@link MachineProcessEnergy}，
 * 与加工详情页报价、机器实扣是同一套口径（配置热重载后 JEI 立刻跟着变）。
 * <p>槽位坐标与游戏内提纯器一致（输出 116,30）。
 */
public class LifePurifyingRecipeCategory implements IRecipeCategory<AkaishiEnergyProcessRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<AkaishiEnergyProcessRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "life_purifying", AkaishiEnergyProcessRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    /** 输出槽（与提纯器 GUI 一致） */
    private static final int OUTPUT_SLOT_X = 116;
    private static final int OUTPUT_SLOT_Y = 30;
    private static final int CARD_WIDTH = 176;
    private static final int CARD_HEIGHT = 72;
    /** 说明文字折行宽度：留出右侧余量，绝不顶到面板边缘 */
    private static final int TIP_WIDTH = 160;
    private static final int LINE_HEIGHT = 10;
    /** 说明最多两行（面板高度只够两行；文案已按此长度拟定） */
    private static final int TIP_MAX_LINES = 2;
    /** 成本两行：放在左上，结束于 y=26，与输出槽（y=30 起）不重叠 */
    private static final int COST_Y1 = 8;
    private static final int COST_Y2 = 18;
    /** 说明起始 y：必须在输出槽底边（45）之下 */
    private static final int TIP_Y = 46;

    private final IDrawable background;
    private final IDrawable icon;

    public LifePurifyingRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, CARD_WIDTH, CARD_HEIGHT);
        this.icon = helper.createDrawableItemStack(new ItemStack(AkaishiLifeBlocks.CHISHI_LIFE_PURIFIER.get()));
    }

    @Override
    public RecipeType<AkaishiEnergyProcessRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.life_purifying");
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
        // 无物品输入（纯能量配方）：只画输出槽
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.result());
    }

    @Override
    public void draw(AkaishiEnergyProcessRecipe recipe, IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics, double mouseX, double mouseY) {
        GuiWidgets.slotBox(guiGraphics, OUTPUT_SLOT_X, OUTPUT_SLOT_Y);
        Font font = Minecraft.getInstance().font;
        // 成本实时取自统一口径（读的是同步后的服务端权威配置，玩家热改配置即刻反映）
        MachineProcessEnergy.Cost cost = MachineProcessEnergy.costPerRun(recipe);
        guiGraphics.drawString(font, Component.translatable("jei.akaishi.life_purifying.cost_chishi",
                EnergyFormat.format(cost.chishi())), 8, COST_Y1, 0xFF404040);
        guiGraphics.drawString(font, Component.translatable("jei.akaishi.life_purifying.cost_life",
                EnergyFormat.format(cost.life())), 8, COST_Y2, 0xFF404040);
        // 说明按宽度折行绘制，最多两行，永不超出面板
        int y = TIP_Y;
        int lines = 0;
        for (FormattedCharSequence line : font.split(
                Component.translatable("jei.akaishi.life_purifying.tip"), TIP_WIDTH)) {
            if (lines++ >= TIP_MAX_LINES) {
                break;
            }
            guiGraphics.drawString(font, line, 8, y, 0xFF808080);
            y += LINE_HEIGHT;
        }
    }

    /** 从数据包配方取全部固化配方（同类型通常只有一条；多条时全部展示） */
    public static List<AkaishiEnergyProcessRecipe> getAll(RecipeManager manager) {
        return AkaishiMachineRecipeIndex.all(manager, AkaishiRecipeTypes.LIFE_PURIFYING.get());
    }
}
