package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 燃料装罐机界面：输入液体条（青）+ 空罐/满罐槽位。
 * 数据来自 {@link AkaishiFuelCannerMenu} 的 ContainerData。
 */
public class AkaishiFuelCannerScreen extends AbstractContainerScreen<AkaishiFuelCannerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_fuel_canner.png");

    /** 输入液体条区域（较原位置下移 18px，避开上方数值文字与燃料名称） */
    private static final int FLUID_BAR_X = 20, FLUID_BAR_Y = 34, BAR_W = 136, BAR_H = 8;
    private static final int FLUID_COLOR = 0xFF40C8FF;
    /** 数值文字固定行（保持原位不动） */
    private static final int TEXT_Y = 14;
    /** 燃料名称文字行（进度条上方） */
    private static final int FUEL_NAME_Y = 24;

    public AkaishiFuelCannerScreen(AkaishiFuelCannerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值缩写 */
    private static String formatEnergy(long v) {
        if (v >= 1_000_000L) {
            return trim(v / 1.0e6) + "M";
        }
        if (v >= 1_000L) {
            return trim(v / 1.0e3) + "K";
        }
        return String.valueOf(v);
    }

    private static String trim(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    private void drawBar(GuiGraphics gui, int x, int y, long energy, long max, int color) {
        long clamped = Math.max(0, Math.min(energy, max));
        long cap = Math.max(1, max);
        int barWidth = (int) (BAR_W * clamped / cap);
        if (barWidth > 0) {
            gui.fill(x, y, x + barWidth, y + BAR_H, color);
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 输入液体条（青）
        drawBar(gui, x + FLUID_BAR_X, y + FLUID_BAR_Y, menu.getFluidAmount(), menu.getFluidMax(), FLUID_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);

        // 燃料名称：显示当前输入液体（空 = 灰色"无燃料"）
        String fuelId = menu.getFuelId();
        Component fuelName = fuelId.isEmpty()
                ? Component.translatable("gui.akaishi.fuel_empty")
                : Component.translatable(fuelKey(fuelId));
        gui.drawString(this.font, fuelName, FLUID_BAR_X, FUEL_NAME_Y,
                fuelId.isEmpty() ? 0xFF808080 : 0xFF55E050, false);

        // 液体数值（保持原位 TEXT_Y 不动，带参渲染，避免无参 translatable 输出 %s 占位字面乱码）
        Component fluidText = Component.translatable("gui.akaishi.fluid",
                formatEnergy(menu.getFluidAmount()), formatEnergy(menu.getFluidMax()));
        int w = this.font.width(fluidText);
        gui.drawString(this.font, fluidText, FLUID_BAR_X + BAR_W / 2 - w / 2, TEXT_Y, 0xFF3F3F3F, false);
    }

    /** fluid 注册名 → 本地化 key（fluid.akaishi.xxx，对应语言文件流体条目） */
    private static String fuelKey(String fuelId) {
        int i = fuelId.indexOf(':');
        return "fluid." + fuelId.substring(0, i) + "." + fuelId.substring(i + 1);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        if (isHovering(FLUID_BAR_X, FLUID_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.fluid",
                            menu.getFluidAmount(), menu.getFluidMax()),
                    mouseX, mouseY);
        }
    }
}
