package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 反应堆控制器界面：分"燃料/散热"两页展示（按钮切换，槽位恒定 5×2）。
 * 燃料页：成型状态/活跃槽/产率/耗速/废品条；散热页：温度条/有效散热/散热效率/停机状态。
 */
public class ChishiReactorControllerScreen extends AbstractContainerScreen<ChishiReactorControllerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_reactor_controller.png");

    // 按钮（相对 GUI 坐标）
    private static final int BTN_FUEL_X = 8, BTN_COOL_X = 92, BTN_Y = 6, BTN_W = 76, BTN_H = 12;
    // 槽位区与信息区
    private static final int BAR_X = 8, BAR_Y = 50, BAR_W = 80, BAR_H = 8;
    private static final int INFO_X = 92, INFO_Y = 22;

    /** 0=燃料页 1=散热页 */
    private int page;

    public ChishiReactorControllerScreen(ChishiReactorControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int rx = (int) mouseX - this.leftPos;
            int ry = (int) mouseY - this.topPos;
            if (rx >= BTN_FUEL_X && rx < BTN_FUEL_X + BTN_W && ry >= BTN_Y && ry < BTN_Y + BTN_H) {
                this.page = 0;
                return true;
            }
            if (rx >= BTN_COOL_X && rx < BTN_COOL_X + BTN_W && ry >= BTN_Y && ry < BTN_Y + BTN_H) {
                this.page = 1;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 页签按钮高亮
        gui.fill(x + BTN_FUEL_X, y + BTN_Y, x + BTN_FUEL_X + BTN_W, y + BTN_Y + BTN_H,
                page == 0 ? 0xFF3A4A3A : 0xFF202020);
        gui.fill(x + BTN_COOL_X, y + BTN_Y, x + BTN_COOL_X + BTN_W, y + BTN_Y + BTN_H,
                page == 1 ? 0xFF3A4A3A : 0xFF202020);
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.page_fuel"),
                x + BTN_FUEL_X + 4, y + BTN_Y + 2, 0xFFE0E0E0, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.page_cool"),
                x + BTN_COOL_X + 4, y + BTN_Y + 2, 0xFFE0E0E0, false);

        // 页 1：废品条（青灰）；页 2：温度条（红），随温度 400/700 变化
        long amount = menu.getWasteAmount();
        long max = Math.max(1, menu.getWasteMax());
        if (page == 0) {
            int w = (int) (BAR_W * amount / max);
            if (w > 0) {
                gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + w, y + BAR_Y + BAR_H, 0xFF808070);
            }
        } else {
            int t = Math.max(0, Math.min(menu.getTemp(), 1000));
            int w = (int) (BAR_W * t / 1000);
            if (w > 0) {
                gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + w, y + BAR_Y + BAR_H, 0xFFE04040);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFF, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;
        int ty = y + INFO_Y;
        ChishiReactorControllerMenu m = this.menu;

        if (page == 0) {
            // 燃料页：成型/活跃/产率/耗速/废品
            gui.drawString(this.font, statusText(m.isFormed()), x + INFO_X, ty, m.isFormed() ? 0xFF55E050 : 0xFFE0C040, false);
            gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.active_slots", m.getActiveSlots(), m.getRodCount()),
                    x + INFO_X, ty + 12, 0xFFE0E0E0, false);
            gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.energy_rate", m.getEnergyPerTick()),
                    x + INFO_X, ty + 24, 0xFFE0E0E0, false);
            gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.fuel_drain",
                            String.format("%.2f", m.getFuelDrainPerTick())),
                    x + INFO_X, ty + 36, 0xFFE0E0E0, false);
            String waste = m.isWasteFull() ? " [FULL]" : "";
            gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.waste", m.getWasteAmount(), m.getWasteMax()) + waste,
                    x + INFO_X, ty + 48, m.isWasteFull() ? 0xFFE06040 : 0xFFE0E0E0, false);
        } else {
            // 散热页：温度/有效散热/散热效率/停机状态
            int temp = m.getTemp();
            int color = temp > 850 ? 0xFFE05050 : (temp >= 400 && temp <= 700 ? 0xFF50E060 : 0xFFE0C040);
            gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.temp", temp, 1000),
                    x + INFO_X, ty, color, false);
            gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.coolers", m.getEffectiveCoolers()),
                    x + INFO_X, ty + 12, 0xFFE0E0E0, false);
            gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.cooling", m.getCoolingPercent()),
                    x + INFO_X, ty + 24, 0xFFE0E0E0, false);
            boolean shutdown = m.isShutdown();
            gui.drawString(this.font, Component.translatable(shutdown
                            ? "gui.template_mod.reactor.shutdown.on"
                            : "gui.template_mod.reactor.shutdown.off"),
                    x + INFO_X, ty + 36, shutdown ? 0xFFE05050 : 0xFFE0C040, false);
        }

        // 条 tooltip
        if (page == 0 && isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.template_mod.reactor.waste",
                    m.getWasteAmount(), m.getWasteMax()), mouseX, mouseY);
        } else if (page == 1 && isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.template_mod.reactor.temp",
                    m.getTemp(), 1000), mouseX, mouseY);
        }
    }

    private static Component statusText(boolean formed) {
        return Component.translatable(formed
                ? "gui.template_mod.reactor.formed"
                : "gui.template_mod.reactor.unformed");
    }
}
