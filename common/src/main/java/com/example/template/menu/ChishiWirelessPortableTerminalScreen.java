package com.example.template.menu;

import com.example.template.TemplateMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 无线能源便捷终端界面（手持物品，只读遥控面板，参考 AE2 无线终端）。
 * 两页互斥切换：运行情况（身份卡/认证终端/口统计）+ 能量储存情况（储能条）。
 * 数据由服务端 broadcastChanges 每 tick 扫背包身份卡刷新。
 * 198 高布局：标题 y=6 与切页按钮 y=16 错开，内容区 y=36 起舒展排布。
 */
public class ChishiWirelessPortableTerminalScreen extends AbstractContainerScreen<ChishiWirelessPortableTerminalMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_RED = 0xFFB03030;
    private static final int TEXT_GREEN = 0xFF2E7D32;

    // 页面切换按钮（80×12，两个并排，y=16 避开标题）
    private static final int TAB_W = 80;
    private static final int TAB_H = 12;
    private static final int TAB_Y = 16;

    // 储能条
    private static final int BAR_X = 20;
    private static final int BAR_Y = 36;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;

    /** 当前页面：0=运行情况，1=能量储存 */
    private int currentPage;

    public ChishiWirelessPortableTerminalScreen(ChishiWirelessPortableTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        GuiWidgets.button(gui, x + 8, y + TAB_Y, TAB_W, TAB_H);
        GuiWidgets.button(gui, x + 88, y + TAB_Y, TAB_W, TAB_H);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        drawTabLabel(gui, 8, "gui.template_mod.wireless.tab.run", currentPage == 0);
        drawTabLabel(gui, 88, "gui.template_mod.wireless.tab.energy", currentPage == 1);

        if (currentPage == 0) {
            renderRunPage(gui);
        } else {
            renderEnergyPage(gui);
        }
    }

    /** 页1：运行情况（身份卡/认证终端/口统计） */
    private void renderRunPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        boolean hasCard = menu.getCardHash() != 0;
        // 背包身份卡短 ID
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.port.bound",
                hasCard ? String.format("%08X", menu.getCardHash()) : "----"),
                x + 8, y + 36, TEXT, false);
        // 认证终端状态（authed 键含 %s，需传入终端短 ID）
        boolean linked = hasCard && menu.getTerminalHash() != 0 && menu.isFormed();
        gui.drawString(this.font, linked
                ? Component.translatable("gui.template_mod.wireless.port.authed",
                        String.format("%08X", menu.getTerminalHash()))
                : Component.translatable("gui.template_mod.wireless.port.no_terminal"),
                x + 8, y + 48, linked ? TEXT_GREEN : TEXT_RED, false);
        // 终端短 ID
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.id",
                menu.getTerminalHash() != 0 ? String.format("%08X", menu.getTerminalHash()) : "----"),
                x + 8, y + 60, TEXT_DIM, false);
        // 口统计
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.port_stats",
                menu.getInputCount(), menu.getOutputCount()), x + 8, y + 72, TEXT_DIM, false);
    }

    /** 页2：能量储存情况（储能条 + 数值 + 容量） */
    private void renderEnergyPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        long energy = Math.max(0, Math.min(menu.getEnergy(), menu.getMaxEnergy()));
        long max = Math.max(1, menu.getMaxEnergy());
        int barWidth = (int) (BAR_W * energy / max);
        if (barWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + barWidth, y + BAR_Y + BAR_H, 0xFFE03030);
        }
        // 数值（居中，右对齐防出框）
        Component energyText = Component.literal(EnergyFormat.format(energy) + " / " + EnergyFormat.format(max));
        int textWidth = this.font.width(energyText);
        gui.drawString(this.font, energyText, x + 88 - textWidth / 2, y + BAR_Y + 10, TEXT, false);
        // 容量
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.capacity",
                EnergyFormat.format(max)), x + 8, y + 68, TEXT_DIM, false);
    }

    private void drawTabLabel(GuiGraphics gui, int x, String key, boolean active) {
        // renderLabels 已 translate(leftPos,topPos)，x 为 GUI 相对坐标
        gui.drawString(this.font, Component.translatable(key),
                x + (TAB_W - this.font.width(Component.translatable(key))) / 2,
                TAB_Y + 2, active ? TEXT : TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 储能条悬停提示（页2）
        if (currentPage == 1 && isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 切页按钮（本地互斥切换，不出框）
            if (isIn(this.leftPos + 8, this.topPos + TAB_Y, TAB_W, TAB_H, mouseX, mouseY)) {
                currentPage = 0;
                return true;
            }
            if (isIn(this.leftPos + 88, this.topPos + TAB_Y, TAB_W, TAB_H, mouseX, mouseY)) {
                currentPage = 1;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isIn(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
