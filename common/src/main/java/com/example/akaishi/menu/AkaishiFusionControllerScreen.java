package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.config.ModConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 聚变控制器界面：三页互斥切换（运行情况 / 燃料 / 热量）。
 * <ul>
 *   <li>运行情况页：成型/宕机状态、温度、产率、效率系数、灰烬积累</li>
 *   <li>燃料页：2×2 燃料棒槽（数量随结构燃料框架数，仅本页激活）</li>
 *   <li>热量页：温度条（0~160M，最佳 100~130M）+ 散热统计（框架数/冷却%/最低耐久）</li>
 * </ul>
 * 版面 176×206，机器区全部按页用代码绘制（贴图仅含背包/快捷栏槽位框）。
 */
public class AkaishiFusionControllerScreen extends AbstractContainerScreen<AkaishiFusionControllerMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_fusion_controller.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 206;

    /** 页签 */
    private static final int TAB_Y = 18, TAB_H = 12, TAB_W = 52, TAB_X0 = 6, TAB_DX = 56;

    /** 燃料槽 2×2（与 Menu 坐标一致） */
    private static final int FUEL_X0 = 70, FUEL_Y0 = 58, FUEL_COLS = 2, FUEL_ROWS = 2;

    /** 热量页：温度条 */
    private static final int TEMP_X = 20, TEMP_Y = 70, TEMP_W = 136, TEMP_H = 12;

    /** 原版风格配色 */
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int OK = 0xFF2E7D32;
    private static final int WARN = 0xFF8B6F1E;
    private static final int BAD = 0xFFA52A2A;

    public AkaishiFusionControllerScreen(AkaishiFusionControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 122;
    }

    // ===== 背景：贴图 + 页签 + 当前页机器控件 =====

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, PANEL_W, PANEL_H);

        drawTabs(gui, x, y, mouseX, mouseY);

        switch (menu.getPage()) {
            case 0 -> drawRunPage(gui, x, y);
            case 1 -> drawFuelPage(gui, x, y);
            default -> drawHeatPage(gui, x, y);
        }
    }

    /** 运行情况页：无槽位，仅状态/温度/产率/效率/灰烬文本 */
    private void drawRunPage(GuiGraphics gui, int x, int y) {
        // 本页无机器槽位，槽框全部由文字信息填充
    }

    /** 燃料页：绘制燃料槽框（数量 = 结构内燃料框架数，≤ 2×2） */
    private void drawFuelPage(GuiGraphics gui, int x, int y) {
        int n = Math.max(0, Math.min(menu.getFuelFrames(), FUEL_COLS * FUEL_ROWS));
        for (int i = 0; i < n; i++) {
            GuiWidgets.slotBox(gui, x + FUEL_X0 + (i % FUEL_COLS) * 18, y + FUEL_Y0 + (i / FUEL_COLS) * 18);
        }
    }

    /** 热量页：温度条自左向右填充（0~上限，最佳区间标绿） */
    private void drawHeatPage(GuiGraphics gui, int x, int y) {
        GuiWidgets.track(gui, x + TEMP_X, y + TEMP_Y, TEMP_W, TEMP_H);
        int max = Math.max(1, ModConfig.fusionTempMax);
        int t = clampTemp(menu.getTemp());
        int w = (int) Math.round((TEMP_W - 2) * t / (double) max);
        if (w > 0) {
            gui.fill(x + TEMP_X + 1, y + TEMP_Y + 1, x + TEMP_X + 1 + w, y + TEMP_Y + TEMP_H - 1, heatColor(t));
        }
    }

    /** 页签按钮（选中凹陷白字，未选凸起深灰字） */
    private void drawTabs(GuiGraphics gui, int x, int y, int mouseX, int mouseY) {
        String[] keys = {"gui.akaishi.fusion.tab_run",
                "gui.akaishi.fusion.tab_fuel",
                "gui.akaishi.fusion.tab_heat"};
        for (int i = 0; i < keys.length; i++) {
            int bx = x + TAB_X0 + i * TAB_DX;
            boolean selected = menu.getPage() == i;
            boolean hover = !selected && isTabHover(bx, y, mouseX, mouseY);
            if (selected) {
                gui.fill(bx, y + TAB_Y, bx + TAB_W, y + TAB_Y + TAB_H, 0xFF8B8B8B);
                gui.fill(bx, y + TAB_Y, bx + TAB_W, y + TAB_Y + 1, 0xFF373737);
                gui.fill(bx, y + TAB_Y, bx + 1, y + TAB_Y + TAB_H, 0xFF373737);
                gui.fill(bx, y + TAB_Y + TAB_H - 1, bx + TAB_W, y + TAB_Y + TAB_H, 0xFFFFFFFF);
                gui.fill(bx + TAB_W - 1, y + TAB_Y, bx + TAB_W, y + TAB_Y + TAB_H, 0xFFFFFFFF);
            } else {
                gui.fill(bx, y + TAB_Y, bx + TAB_W, y + TAB_Y + TAB_H, hover ? 0xFFD4D4D4 : 0xFFC6C6C6);
                gui.fill(bx, y + TAB_Y, bx + TAB_W, y + TAB_Y + 1, 0xFFFFFFFF);
                gui.fill(bx, y + TAB_Y, bx + 1, y + TAB_Y + TAB_H, 0xFFFFFFFF);
                gui.fill(bx, y + TAB_Y + TAB_H - 1, bx + TAB_W, y + TAB_Y + TAB_H, 0xFF555555);
                gui.fill(bx + TAB_W - 1, y + TAB_Y, bx + TAB_W, y + TAB_Y + TAB_H, 0xFF555555);
            }
            gui.drawCenteredString(this.font, Component.translatable(keys[i]),
                    bx + TAB_W / 2, y + TAB_Y + 3, selected ? 0xFFFFFFFF : TEXT);
        }
    }

    // ===== 文字标签 =====

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 仅绘制标题，抑制原版"物品栏"标签避免与机器区/背包槽位重叠
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);

        AkaishiFusionControllerMenu m = this.menu;
        switch (m.getPage()) {
            case 0 -> drawRunLabels(gui, m);
            case 1 -> drawFuelLabels(gui, m);
            default -> drawHeatLabels(gui, m);
        }
    }

    /** 运行情况页：成型状态 + 温度 + 产率 + 效率 + 灰烬 */
    private void drawRunLabels(GuiGraphics gui, AkaishiFusionControllerMenu m) {
        int y0 = 42;
        if (m.isOverheated()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.overheated"), 8, y0, BAD, false);
        } else {
            gui.drawString(this.font, Component.translatable(m.isFormed()
                            ? "gui.akaishi.fusion.formed" : "gui.akaishi.fusion.unformed"),
                    8, y0, m.isFormed() ? OK : WARN, false);
        }
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.temp",
                m.getTemp(), ModConfig.fusionTempMax), 8, y0 + 12, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.yield",
                EnergyFormat.format(m.getYieldPerTick())), 8, y0 + 24, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.speed",
                m.getSpeedPercent() / 100.0), 8, y0 + 36, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.ash",
                m.getAshAmount()), 8, y0 + 48, TEXT_DIM, false);
        if (!m.isFormed()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.build_hint"),
                    8, y0 + 62, TEXT_DIM, false);
        }
    }

    /** 燃料页：上方成型状态、下方燃料占用 */
    private void drawFuelLabels(GuiGraphics gui, AkaishiFusionControllerMenu m) {
        gui.drawString(this.font,
                Component.translatable(m.isFormed() ? "gui.akaishi.fusion.formed"
                        : "gui.akaishi.fusion.unformed"),
                8, 42, m.isFormed() ? OK : WARN, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.fuel_used",
                usedFuelSlots(), AkaishiFusionControllerMenu.FUEL_SLOT_COUNT), 8, 98, TEXT_DIM, false);
    }

    /** 热量页：温度条 + 散热统计 */
    private void drawHeatLabels(GuiGraphics gui, AkaishiFusionControllerMenu m) {
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.heat_label"),
                8, 42, TEXT_DIM, false);
        gui.drawString(this.font, m.getTemp() + "M / " + ModConfig.fusionTempMax + "M",
                8, 54, heatColor(m.getTemp()), false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.coolers",
                m.getCoolerCount()), 8, 88, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.cooling",
                m.getCoolingPercent()), 8, 100, TEXT, false);
        int dur = m.getCoolerDurability();
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.cooler_durability", dur),
                8, 112, dur <= 25 ? BAD : TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 悬停提示：温度条（热量页）
        if (menu.getPage() == 2 && isHovering(TEMP_X, TEMP_Y, TEMP_W, TEMP_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.fusion.heat",
                            menu.getTemp() + "M", ModConfig.fusionTempMax + "M"),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < 3; i++) {
                int bx = this.leftPos + TAB_X0 + i * TAB_DX;
                if (isTabHover(bx, this.topPos, (int) mouseX, (int) mouseY)) {
                    this.menu.setPage(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isTabHover(int bx, int by, int mx, int my) {
        return mx >= bx && mx < bx + TAB_W && my >= by + TAB_Y && my < by + TAB_Y + TAB_H;
    }

    /** 已占用燃料槽数（客户端槽位状态） */
    private int usedFuelSlots() {
        int n = 0;
        for (int i = 0; i < AkaishiFusionControllerMenu.FUEL_SLOT_COUNT; i++) {
            if (this.menu.getSlot(i).hasItem()) {
                n++;
            }
        }
        return n;
    }

    private static int clampTemp(int temp) {
        return Math.max(0, Math.min(ModConfig.fusionTempMax, temp));
    }

    /** 温度分档着色：最佳区间绿 / 升温黄 / 高温红 */
    private static int heatColor(int temp) {
        if (temp >= ModConfig.fusionTempMax) return BAD;
        if (temp >= ModConfig.fusionTempOptMin && temp <= ModConfig.fusionTempOptMax) return OK;
        if (temp > ModConfig.fusionTempOptMax) return WARN;
        return WARN;
    }
}
