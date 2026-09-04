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
 *   <li>热量页：2×5 散热片槽位网格（散热片真实物品直连控制器容器，直接放入/取出；
 *       散热片仅可放入散热框架解锁的前 N 槽，物品图标 + 原版耐久条展示磨损，悬停看品质/冷却%）+ 总冷却/最低耐久</li>
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

    /** 运行页：堆芯温度进度条（从热量页迁移而来） */
    private static final int TEMP_X = 20, TEMP_Y = 64, TEMP_W = 136, TEMP_H = 12;

    /** 热量页：散热槽位 2×5 网格（每格 18×18，与 Menu 槽坐标一致） */
    private static final int COOLER_COLS = 5;
    private static final int COOLER_ROWS = 2;
    private static final int COOLER_X0 = (PANEL_W - COOLER_COLS * 18) / 2;
    private static final int COOLER_Y0 = 50;
    private static final int COOLER_DY = 20;

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

    /** 运行情况页：绘制堆芯温度进度条（自左向右填充，最佳区间标绿；成型/宕机/未成型均显示实时温度） */
    private void drawRunPage(GuiGraphics gui, int x, int y) {
        GuiWidgets.track(gui, x + TEMP_X, y + TEMP_Y, TEMP_W, TEMP_H);
        int maxM = Math.max(1, tempM(ModConfig.fusionTempTrip));
        int tM = tempM(clampTemp(menu.getTemp()));
        int w = (int) Math.round((TEMP_W - 2) * tM / (double) maxM);
        if (w > 0) {
            gui.fill(x + TEMP_X + 1, y + TEMP_Y + 1, x + TEMP_X + 1 + w, y + TEMP_Y + TEMP_H - 1, heatColor(tM));
        }
    }

    /** 燃料页：绘制燃料槽框（数量 = 结构内燃料框架数，≤ 2×2） */
    private void drawFuelPage(GuiGraphics gui, int x, int y) {
        int n = Math.max(0, Math.min(menu.getFuelFrames(), FUEL_COLS * FUEL_ROWS));
        for (int i = 0; i < n; i++) {
            GuiWidgets.slotBox(gui, x + FUEL_X0 + (i % FUEL_COLS) * 18, y + FUEL_Y0 + (i / FUEL_COLS) * 18);
        }
    }

    /** 热量页：绘制 2×5 散热槽位框（物品图标与耐久条由原版槽渲染自动绘制） */
    private void drawHeatPage(GuiGraphics gui, int x, int y) {
        for (int i = 0; i < COOLER_COLS * COOLER_ROWS; i++) {
            GuiWidgets.slotBox(gui, x + COOLER_X0 + (i % COOLER_COLS) * 18,
                    y + COOLER_Y0 + (i / COOLER_COLS) * COOLER_DY);
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
                gui.fill(bx + TAB_W - 1, y + TAB_Y, bx + TAB_W, y + TAB_Y + TAB_H, 0xFF555555);
                gui.fill(bx, y + TAB_Y + TAB_H - 1, bx + TAB_W, y + TAB_Y + TAB_H, 0xFF555555);
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

    /** 运行情况页：成型状态 + 温度文字 + 温度条(64) + 产率/效率/灰烬（未成型时以搭建提示占位） */
    private void drawRunLabels(GuiGraphics gui, AkaishiFusionControllerMenu m) {
        if (m.isOverheated()) {
            gui.drawString(this.font,
                    Component.translatable("gui.akaishi.fusion.overheated_cooldown", m.getOverheatCooldownSec()),
                    8, 40, BAD, false);
        } else {
            gui.drawString(this.font, Component.translatable(m.isFormed()
                            ? "gui.akaishi.fusion.formed" : "gui.akaishi.fusion.unformed"),
                    8, 40, m.isFormed() ? OK : WARN, false);
        }
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.temp",
                tempM(m.getTemp()), tempM(ModConfig.fusionTempTrip)), 8, 52, TEXT, false);
        if (m.isFormed()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.yield",
                    EnergyFormat.format(m.getYieldPerTick())), 8, 84, TEXT, false);
            gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.speed",
                    m.getSpeedPercent() / 100.0), 8, 96, TEXT, false);
            gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.ash",
                    m.getAshAmount()), 8, 108, TEXT_DIM, false);
        } else {
            gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.build_hint"),
                    8, 84, TEXT_DIM, false);
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

    /** 热量页：散热片槽位标题（含已解锁数/上限）+ 总冷却/最低耐久统计（无散热片时最低耐久显示 --） */
    private void drawHeatLabels(GuiGraphics gui, AkaishiFusionControllerMenu m) {
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.heat_label",
                m.getCoolerCount(), AkaishiFusionControllerMenu.COOLER_SLOT_COUNT),
                8, 38, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.cooling",
                m.getCoolingPercent()), 8, 102, TEXT, false);
        if (hasCoolerSink()) {
            int dur = m.getCoolerDurability();
            gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.cooler_durability",
                    dur + "%"), 8, 114, dur <= 25 ? BAD : TEXT, false);
        } else {
            gui.drawString(this.font, Component.translatable("gui.akaishi.fusion.cooler_durability",
                    "--"), 8, 114, TEXT_DIM, false);
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 悬停提示：温度条（运行页）
        if (menu.getPage() == 0 && isHovering(TEMP_X, TEMP_Y, TEMP_W, TEMP_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.fusion.heat",
                            tempM(menu.getTemp()) + "M", tempM(ModConfig.fusionTempTrip) + "M"),
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

    /** 散热片槽中是否存在已装散热片（无片时最低耐久不具意义，显示 --） */
    private boolean hasCoolerSink() {
        int start = AkaishiFusionControllerMenu.FUEL_SLOT_COUNT;
        for (int i = start; i < start + AkaishiFusionControllerMenu.COOLER_SLOT_COUNT; i++) {
            if (this.menu.getSlot(i).hasItem()) {
                return true;
            }
        }
        return false;
    }

    /** 内部原始温度（0~上限整数）→ 显示用 M 单位数值（如 100000000 → 100） */
    private static int tempM(int rawTemp) {
        return rawTemp / 1_000_000;
    }

    private static int clampTemp(int temp) {
        return Math.max(0, Math.min(ModConfig.fusionTempTrip, temp));
    }

    /** 温度分档着色（参数为 M 单位）：最佳区间绿 / 升温黄 / 高温红 */
    private static int heatColor(int tempM) {
        if (tempM >= tempM(ModConfig.fusionTempTrip)) {
            return BAD;
        }
        if (tempM >= tempM(ModConfig.fusionTempOptMin) && tempM <= tempM(ModConfig.fusionTempOptMax)) {
            return OK;
        }
        return WARN;
    }
}
