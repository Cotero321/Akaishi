package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 无线赤能源端口界面（输入口/输出口共用，两类页面：运行情况/传输情况）。
 * 页面由顶部按钮本地互斥切换（互不重叠）；解绑身份卡走服务端按钮。
 * 数据来自 {@link AkaishiWirelessPortMenu} 的 ContainerData（缓冲储能 long + 绑定卡/终端/认证/速率）。
 * 198 高布局：标题 y=6 与切页按钮 y=16 错开，内容区 y=36 起舒展排布。
 */
public class AkaishiWirelessPortScreen extends AbstractContainerScreen<AkaishiWirelessPortMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_RED = 0xFFB03030;
    private static final int TEXT_GREEN = 0xFF2E7D32;

    // 页面切换按钮（80×12，两个并排，y=16 避开标题）
    private static final int TAB_W = 80;
    private static final int TAB_H = 12;
    private static final int TAB_Y = 16;

    // 缓冲条
    private static final int BAR_X = 20;
    private static final int BAR_Y = 36;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;

    // 解绑按钮（页1）
    private static final int UNBIND_X = 8;
    private static final int UNBIND_Y = 76;
    private static final int UNBIND_W = 44;
    private static final int UNBIND_H = 12;

    /** 当前页面：0=运行情况，1=传输情况 */
    private int currentPage;

    public AkaishiWirelessPortScreen(AkaishiWirelessPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        // 顶部切页按钮
        GuiWidgets.button(gui, x + 8, y + TAB_Y, TAB_W, TAB_H);
        GuiWidgets.button(gui, x + 88, y + TAB_Y, TAB_W, TAB_H);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        // 切页标签
        drawTabLabel(gui, 8, "gui.akaishi.wireless.tab.run", currentPage == 0);
        drawTabLabel(gui, 88, "gui.akaishi.wireless.tab.transfer", currentPage == 1);

        if (currentPage == 0) {
            renderRunPage(gui);
        } else {
            renderTransferPage(gui);
        }
    }

    /** 页1：运行情况（绑定卡/认证状态/区块加载提示/解绑） */
    private void renderRunPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        boolean bound = menu.getCardHash() != 0;
        // 绑定卡
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.port.bound",
                bound ? String.format("%08X", menu.getCardHash()) : "----"),
                x + 8, y + 36, TEXT, false);
        // 认证状态
        if (menu.isAuthenticated()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.port.authed",
                    String.format("%08X", menu.getTerminalHash())),
                    x + 8, y + 48, TEXT_GREEN, false);
            // 区块弱加载提示（认证后由终端区块加载构架统一管理）
            gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.port.chunk"),
                    x + 8, y + 60, TEXT_DIM, false);
        } else {
            gui.drawString(this.font, Component.translatable(bound
                            ? "gui.akaishi.wireless.port.not_authed" : "gui.akaishi.wireless.port.no_card"),
                    x + 8, y + 48, bound ? TEXT_RED : TEXT_DIM, false);
            if (bound) {
                gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.port.bind_hint"),
                        x + 8, y + 60, TEXT_DIM, false);
            }
        }
        // 解绑按钮
        if (bound) {
            GuiWidgets.button(gui, x + UNBIND_X, y + UNBIND_Y, UNBIND_W, UNBIND_H);
            gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.port.unbind"),
                    x + UNBIND_X + 6, y + UNBIND_Y + 2, TEXT, false);
        }
    }

    /** 页2：传输情况（缓冲储能条 + 速率 + 方向提示） */
    private void renderTransferPage(GuiGraphics gui) {
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
        // 缓冲数值（右对齐防出框）
        Component energyText = Component.literal(EnergyFormat.format(energy) + " / " + EnergyFormat.format(max));
        int textWidth = this.font.width(energyText);
        gui.drawString(this.font, energyText, x + 88 - textWidth / 2, y + BAR_Y + 10, TEXT, false);
        // 传输速率（按卡自动解锁）
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.port.rate",
                EnergyFormat.format(menu.getRate()) + "/tick"), x + 8, y + 56, TEXT, false);
        // 方向提示（输入口=纯接收，输出口=纯发电）
        gui.drawString(this.font, Component.translatable(menu.isOutput()
                        ? "gui.akaishi.wireless.port.output_hint" : "gui.akaishi.wireless.port.input_hint"),
                x + 8, y + 68, TEXT_DIM, false);
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

        // 缓冲条悬停提示（页2）
        if (currentPage == 1 && isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy",
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
            // 解绑按钮（页1，服务端执行）
            if (currentPage == 0 && menu.getCardHash() != 0
                    && isIn(this.leftPos + UNBIND_X, this.topPos + UNBIND_Y, UNBIND_W, UNBIND_H, mouseX, mouseY)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, AkaishiWirelessPortMenu.BTN_UNBIND);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isIn(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
