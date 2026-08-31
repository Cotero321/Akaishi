package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.config.ModConfig;
import com.example.template.block.entity.ChishiReactorControllerBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 反应堆控制器界面：三页互斥切换（燃料 / 温度 / 状态）。
 *
 * <p>三个页签展示同一容器的不同视图，机器区内容随页签切换、互不重叠：
 * <ul>
 *   <li>燃料页：燃料棒槽（5×2 居中，数量随结构内燃料棒数变化），槽位仅在本页激活</li>
 *   <li>温度页：5×4 散热片槽位（随结构散热组件数变化，可审视/更换）+ 右侧温度读数</li>
 *   <li>状态页：运行状态 + 废品（衰竭燃料）流体储量条</li>
 * </ul>
 *
 * <p>版面 176×206，贴图仅含常驻的玩家背包/快捷栏槽位框（18px 与机器槽一致），机器区全部按页用代码绘制：
 * <pre>
 *   y=6        标题
 *   y=18-29    三页签
 *   y=40-114   机器内容区（三页互斥）
 *   y=122      物品栏标签
 *   y=124-178  玩家背包 3×9
 *   y=180-198  快捷栏
 * </pre>
 */
public class ChishiReactorControllerScreen extends AbstractContainerScreen<ChishiReactorControllerMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_reactor_controller.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 206;

    /** 页签 */
    private static final int TAB_Y = 18, TAB_H = 12, TAB_W = 52, TAB_X0 = 6, TAB_DX = 56;

    /** 燃料槽 5×2（x=44 起对齐标准 18px 网格，与 Menu 坐标一致） */
    private static final int FUEL_X0 = 44, FUEL_Y0 = 58, FUEL_COLS = 5, FUEL_ROWS = 2;

    /** 温度页：散热片槽位 5×4（x=8..98, y=42..114，与 Menu 坐标一致） */
    private static final int SINK_X0 = 8, SINK_Y0 = 42, SINK_COLS = 5, SINK_ROWS = 4;
    /** 温度页：右侧温度读数（右移至 x=106，与 5×4 槽位区分离） */
    private static final int TEMP_TEXT_X = 106, TEMP_TEXT_Y0 = 44, TEMP_TEXT_STEP = 12;

    /** 状态页：废品（衰竭燃料）流体条 */
    private static final int WASTE_X = 20, WASTE_Y = 78, WASTE_W = 136, WASTE_H = 12;

    /** 原版风格配色 */
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int OK = 0xFF2E7D32;
    private static final int WARN = 0xFF8B6F1E;
    private static final int BAD = 0xFFA52A2A;

    public ChishiReactorControllerScreen(ChishiReactorControllerMenu menu, Inventory inv, Component title) {
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
            case 0 -> drawFuelPage(gui, x, y);
            case 1 -> drawTempPage(gui, x, y);
            default -> drawStatusPage(gui, x, y);
        }
    }

    /** 燃料页：绘制燃料槽框（数量 = 结构内燃料棒数 rodCount，≤ 5×2，跟随结构变化） */
    private void drawFuelPage(GuiGraphics gui, int x, int y) {
        int n = Math.max(0, Math.min(menu.getRodCount(), FUEL_COLS * FUEL_ROWS));
        for (int i = 0; i < n; i++) {
            GuiWidgets.slotBox(gui, x + FUEL_X0 + (i % FUEL_COLS) * 18, y + FUEL_Y0 + (i / FUEL_COLS) * 18);
        }
    }

    /** 温度页：绘制 5×4 散热片槽位框（数量 = 结构内散热组件数），审视/更换散热片 */
    private void drawTempPage(GuiGraphics gui, int x, int y) {
        int n = Math.max(0, Math.min(menu.getCoolerCount(), ChishiReactorControllerMenu.COOLER_SLOT_COUNT));
        for (int i = 0; i < n; i++) {
            GuiWidgets.slotBox(gui, x + SINK_X0 + (i % SINK_COLS) * 18, y + SINK_Y0 + (i / SINK_COLS) * 18);
        }
    }

    /** 状态页：废品流体条，自左向右填充 */
    private void drawStatusPage(GuiGraphics gui, int x, int y) {
        GuiWidgets.track(gui, x + WASTE_X, y + WASTE_Y, WASTE_W, WASTE_H);
        long waste = menu.getWasteAmount();
        long cap = Math.max(1L, menu.getWasteMax());
        int w = (int) Math.round((WASTE_W - 2) * Math.min(1.0D, waste / (double) cap));
        if (w > 0) {
            gui.fill(x + WASTE_X + 1, y + WASTE_Y + 1, x + WASTE_X + 1 + w, y + WASTE_Y + WASTE_H - 1,
                    menu.isWasteFull() ? BAD : 0xFF6A5ACD);
        }
    }

    /** 页签按钮（选中凹陷白字，未选凸起深灰字） */
    private void drawTabs(GuiGraphics gui, int x, int y, int mouseX, int mouseY) {
        String[] keys = {"gui.template_mod.reactor.tab_fuel",
                "gui.template_mod.reactor.tab_temp",
                "gui.template_mod.reactor.tab_status"};
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

        ChishiReactorControllerMenu m = this.menu;
        switch (m.getPage()) {
            case 0 -> drawFuelLabels(gui, m);
            case 1 -> drawTempLabels(gui, m);
            default -> drawStatusLabels(gui, m);
        }
    }

    /** 燃料页：上方结构状态、下方燃料占用（中间为 5×2 燃料槽） */
    private void drawFuelLabels(GuiGraphics gui, ChishiReactorControllerMenu m) {
        gui.drawString(this.font,
                Component.translatable(m.isFormed() ? "gui.template_mod.reactor.formed"
                        : "gui.template_mod.reactor.unformed"),
                8, 42, m.isFormed() ? OK : WARN, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.fuel_used",
                usedFuelSlots(), ChishiReactorControllerMenu.FUEL_SLOT_COUNT), 8, 98, TEXT_DIM, false);
    }

    /** 温度页：右侧温度/散热/有效/耐久读数（文本右移，避开 5×4 槽位区） */
    private void drawTempLabels(GuiGraphics gui, ChishiReactorControllerMenu m) {
        int t = clampTemp(m.getTemp());
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.temp_label"),
                TEMP_TEXT_X, TEMP_TEXT_Y0, TEXT_DIM, false);
        gui.drawString(this.font, t + "\u2103", TEMP_TEXT_X, TEMP_TEXT_Y0 + TEMP_TEXT_STEP, tempColor(t), false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.cooling",
                m.getCoolingPercent()), TEMP_TEXT_X, TEMP_TEXT_Y0 + TEMP_TEXT_STEP * 2, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.coolers",
                m.getEffectiveCoolers()), TEMP_TEXT_X, TEMP_TEXT_Y0 + TEMP_TEXT_STEP * 3, TEXT, false);
        int dur = m.getCoolerDurability();
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.cooler_durability", dur),
                TEMP_TEXT_X, TEMP_TEXT_Y0 + TEMP_TEXT_STEP * 4, dur <= 25 ? BAD : TEXT, false);
    }

    /** 状态页：运行状态（高温警告中产量降低）+ 废品储量（图二内容归入本页） */
    private void drawStatusLabels(GuiGraphics gui, ChishiReactorControllerMenu m) {
        boolean warn = m.isWarning();
        gui.drawString(this.font, Component.translatable(warn
                ? "gui.template_mod.reactor.warning.on" : "gui.template_mod.reactor.warning.off"),
                8, 40, warn ? BAD : OK, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.energy_rate",
                EnergyFormat.format(m.getEnergyPerTick())), 8, 52, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.reactor.waste_label"),
                WASTE_X, 68, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable(m.isWasteFull()
                ? "gui.template_mod.reactor.waste_need_clear" : "gui.template_mod.reactor.waste_ok"),
                WASTE_X, 96, m.isWasteFull() ? BAD : OK, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 悬停提示：废品条（状态页），含衰竭燃料种类数
        if (menu.getPage() == 2 && isHovering(WASTE_X, WASTE_Y, WASTE_W, WASTE_H, mouseX, mouseY)) {
            Component tip = Component.translatable("gui.template_mod.reactor.waste",
                    EnergyFormat.format(menu.getWasteAmount()), EnergyFormat.format(menu.getWasteMax()));
            int types = menu.getWasteTypes();
            if (types > 0) {
                tip = tip.copy().append(Component.translatable("gui.template_mod.reactor.waste_types", types));
            }
            gui.renderTooltip(this.font, tip, mouseX, mouseY);
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
        for (int i = 0; i < ChishiReactorControllerMenu.FUEL_SLOT_COUNT; i++) {
            if (this.menu.getSlot(i).hasItem()) {
                n++;
            }
        }
        return n;
    }

    private static int clampTemp(int temp) {
        return Math.max(0, Math.min(ModConfig.reactorTempMax, temp));
    }

    /** 温度分档着色：正常绿 / 偏高黄 / 危险红 */
    private static int tempColor(int temp) {
        if (temp >= ModConfig.reactorTempWarn) return BAD;
        if (temp >= ModConfig.reactorTempOptMin
                && temp <= ModConfig.reactorTempOptMax) return OK;
        return WARN;
    }
}
