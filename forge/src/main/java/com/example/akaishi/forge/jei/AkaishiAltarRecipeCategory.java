package com.example.akaishi.forge.jei;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiMotherAltarBlocks;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.item.ModItems;
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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * JEI「母神祭坛仪式」配方类别（D162）：
 * 巨坛供奉槽 1 件主祭品，外圈 8 座子祭坛各供奉 1 件，配方齐备并注入足量生命能量即自动结算。
 * <p>外圈 8 槽按 3×3 环形排布（中心为主祭品位），与实机祭坛结构一一对应，便于玩家照图搭建。
 */
public class AkaishiAltarRecipeCategory implements IRecipeCategory<AkaishiAltarRecipeCategory.AltarRecipe> {

    /** JEI 配方类型标识 */
    public static final RecipeType<AltarRecipe> TYPE =
            RecipeType.create(AkaishiMod.MOD_ID, "altar_ritual", AltarRecipe.class);

    private static final Component TIP = Component.translatable("jei.akaishi.altar_tip");
    private static final int WIDTH = 176;
    private static final int TIP_WIDTH = 168;
    private static final int LINE_HEIGHT = 10;
    /** 槽位边长与 3×3 环形的行列步距 */
    private static final int SLOT = 18;
    private static final int PITCH = 20;
    private static final int GRID_X = 8;
    /** 外圈 8 槽在 3×3 网格中的位置（列,行），中心 (1,1) 留给主祭品 */
    private static final int[][] RING = {
            {0, 0}, {1, 0}, {2, 0}, {0, 1}, {2, 1}, {0, 2}, {1, 2}, {2, 2}};
    /** 箭头起点与产物槽横坐标 */
    private static final int ARROW_X = 74;
    private static final int OUTPUT_X = 94;

    private final IDrawable background;
    private final IDrawable icon;
    private final int slotY;
    private final int infoY;
    private final int cardHeight;

    public AkaishiAltarRecipeCategory(IGuiHelper helper) {
        // 说明文案中/英长度不同，按实际换行数自适应高度，避免出界
        int tipLines = Math.max(1, Minecraft.getInstance().font.split(TIP, TIP_WIDTH).size());
        this.slotY = 12 + tipLines * LINE_HEIGHT;
        this.infoY = slotY + 2 * PITCH + SLOT + 4;
        this.cardHeight = infoY + 2 * LINE_HEIGHT + 2;
        this.background = helper.createBlankDrawable(WIDTH, cardHeight);
        this.icon = helper.createDrawableItemStack(new ItemStack(AkaishiMotherAltarBlocks.CHISHI_MOTHER_ALTAR.get()));
    }

    @Override
    public RecipeType<AltarRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.akaishi.altar");
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
    public void setRecipe(IRecipeLayoutBuilder builder, AltarRecipe recipe, IFocusGroup focuses) {
        // 主祭品居中，外圈 8 槽环绕，产物在右侧
        builder.addSlot(RecipeIngredientRole.INPUT, GRID_X + PITCH, slotY + PITCH)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.host());
        List<ItemStack> outer = recipe.outer();
        for (int i = 0; i < outer.size() && i < RING.length; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT,
                            GRID_X + RING[i][0] * PITCH, slotY + RING[i][1] * PITCH)
                    .addIngredient(VanillaTypes.ITEM_STACK, outer.get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, slotY + PITCH)
                .addIngredient(VanillaTypes.ITEM_STACK, recipe.output());
    }

    @Override
    public void draw(AltarRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        GuiWidgets.panel(guiGraphics, 0, 0, WIDTH, cardHeight);
        Font font = Minecraft.getInstance().font;
        // 顶部机制说明：按换行结果逐行绘制，永不超出面板宽度
        int y = 8;
        for (FormattedCharSequence line : font.split(TIP, TIP_WIDTH)) {
            guiGraphics.drawString(font, line, 8, y, 0xFF404040);
            y += LINE_HEIGHT;
        }
        // 槽位底框
        GuiWidgets.slotBox(guiGraphics, GRID_X + PITCH, slotY + PITCH);
        for (int[] cell : RING) {
            GuiWidgets.slotBox(guiGraphics, GRID_X + cell[0] * PITCH, slotY + cell[1] * PITCH);
        }
        GuiWidgets.slotBox(guiGraphics, OUTPUT_X, slotY + PITCH);
        // 主祭品 → 产物的指示箭头
        guiGraphics.drawString(font, "→", ARROW_X, slotY + PITCH + 5, 0xFF606060);
        // 门槛数值：等级与生命能量各占一行，文案短不换行
        guiGraphics.drawString(font,
                Component.translatable("jei.akaishi.altar.tier", recipe.tierRequired()).getString(),
                8, infoY, 0xFF404040);
        guiGraphics.drawString(font,
                Component.translatable("jei.akaishi.altar.energy", compact(recipe.progressRequired())).getString(),
                8, infoY + LINE_HEIGHT, 0xFF404040);
    }

    /** 生命能量数值紧凑显示：800000 → 800K、1500000 → 1.5M */
    private static String compact(long value) {
        if (value >= 1_000_000L) {
            return trim(value / 1_000_000.0D) + "M";
        }
        if (value >= 1_000L) {
            return trim(value / 1_000.0D) + "K";
        }
        return Long.toString(value);
    }

    private static String trim(double value) {
        if (value == Math.floor(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * 祭坛仪式展示数据：{@code outer} 固定 8 件，与巨坛外圈 8 座子祭坛顺序一致。
     *
     * @param tierRequired     所需祭坛等级
     * @param progressRequired 所需生命能量（进度阈值）
     */
    public record AltarRecipe(ItemStack host, List<ItemStack> outer, ItemStack output,
                              int tierRequired, long progressRequired) {

        /** 全部 5 条仪式配方：旧配方 1 条 + 四件禁断饰品 4 条（顺序与实机配方表一致） */
        public static List<AltarRecipe> getAll() {
            return List.of(
                    // 旧配方：赤石锭 + 外圈 8 件 → 生命融合锭（不限等级，仅需结构成型）
                    new AltarRecipe(
                            new ItemStack(ModItems.akaishiIngot.get()),
                            List.of(
                                    stack(ModItems.lifeEmbryo.get()), stack(ModItems.lifeEmbryo.get()),
                                    stack(ModItems.lifeAsh.get()), stack(ModItems.lifeAsh.get()),
                                    stack(ModItems.geneSequence.get()), stack(ModItems.geneSequence.get()),
                                    stack(ModBlocks.CHISHI_ESSENCE_BLOCK.get().asItem()),
                                    stack(ModBlocks.CHISHI_ESSENCE_BLOCK.get().asItem())),
                            new ItemStack(ModItems.lifeFusionIngot.get()),
                            1, 80_000L),
                    // 四套新配方：生命融合锭 + 主题器官×2 + 基础素材×2 + 基因序列×2 + 药剂×2
                    advanced(
                            List.of(stack(ModItems.akaishiOrganLeftArm.get()), stack(ModItems.akaishiOrganRightArm.get()),
                                    stack(ModItems.lifeEmbryo.get()), stack(ModItems.lifeEmbryo.get())),
                            ModItems.lifeTouch.get()),
                    advanced(
                            List.of(stack(ModItems.akaishiOrganHeart.get()), stack(ModItems.akaishiOrganHeart.get()),
                                    stack(ModItems.lifeEmbryo.get()), stack(ModItems.lifeEmbryo.get())),
                            ModItems.cubHeart.get()),
                    advanced(
                            List.of(stack(ModItems.akaishiOrganEye.get()), stack(ModItems.akaishiOrganEye.get()),
                                    stack(ModItems.lifeAsh.get()), stack(ModItems.lifeAsh.get())),
                            ModItems.motherSeal.get()),
                    advanced(
                            List.of(stack(ModItems.akaishiOrganViscera.get()), stack(ModItems.akaishiOrganViscera.get()),
                                    stack(ModItems.lifeAsh.get()), stack(ModItems.lifeAsh.get())),
                            ModItems.fertilityRing.get()));
        }

        /** 新配方：补足基因序列×2 与药剂×2，等级/能量取 common 侧同一配置口径 */
        private static AltarRecipe advanced(List<ItemStack> head, net.minecraft.world.item.Item output) {
            java.util.ArrayList<ItemStack> outer = new java.util.ArrayList<>(head);
            outer.add(stack(ModItems.geneSequence.get()));
            outer.add(stack(ModItems.geneSequence.get()));
            outer.add(stack(ModItems.akaishiPotion.get()));
            outer.add(stack(ModItems.akaishiPotion.get()));
            return new AltarRecipe(
                    new ItemStack(ModItems.lifeFusionIngot.get()),
                    List.copyOf(outer),
                    new ItemStack(output),
                    com.example.akaishi.config.ModConfig.altarNewRecipeTierRequired,
                    com.example.akaishi.config.ModConfig.altarNewRecipeProgressMax);
        }

        private static ItemStack stack(net.minecraft.world.item.Item item) {
            return new ItemStack(item);
        }
    }
}
