package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 无线生命能量端口界面（输入口/输出口共用，两类页面：运行情况/传输情况，赤版生命镜像）。
 * 页面由顶部按钮本地互斥切换（互不重叠）；解绑身份卡走服务端按钮。
 * 数据来自 {@link AkaishiLifeWirelessPortMenu} 的 ContainerData（缓冲储能 long + 绑定卡/终端/认证/方向）。
 * 布局/坐标与 {@link AkaishiWirelessPortScreen} 完全一致（仅能量条颜色换生命绿）。
 * 198 高布局：标题 y=6 与切页按钮 y=16 错开，内容区 y=36 起舒展排布。
 */
public class AkaishiLifeWirelessPortScreen extends AbstractContainerScreen<AkaishiLifeWirelessPortMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_RED = 0xFFB03030;
    private static final int TEXT_GREEN = 0xFF2E7D32;

    // 页面切换按钮（80×12，两个并排，y=16 避开标题）
    private static final int TAB_W = 53;
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

    /** 当前页面：0=运行情况，1=传输情况，2=远程绑定 */
    private int currentPage;

    // 绑定页布局：状态两行 + 最多 6 行候选终端（行高 11，止于 y≈122，背包自 124）
    private static final int TAB_STEP = 55;
    private static final int BIND_STATUS_Y = 36;
    private static final int BIND_LIST_Y = 58;
    private static final int BIND_ROW_H = 11;
    private static final int BIND_LIST_W = 160;
    private static final int BIND_ROWS = 6;

    public AkaishiLifeWirelessPortScreen(AkaishiLifeWirelessPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        // 顶部切页按钮（3 页：运行 / 传输 / 绑定）
        for (int i = 0; i < 3; i++) {
            GuiWidgets.button(gui, x + 8 + i * TAB_STEP, y + TAB_Y, TAB_W, TAB_H);
        }
        // 绑定页：候选行悬停高亮（行高仅 11，靠底色区分行）
        if (currentPage == 2) {
            int row = bindRowAt(mouseX, mouseY);
            if (row >= 0 && row < this.menu.bindingEntries().size()) {
                gui.fill(x + 8, y + BIND_LIST_Y + row * BIND_ROW_H - 1, x + 168,
                        y + BIND_LIST_Y + row * BIND_ROW_H + BIND_ROW_H - 1, 0x30FFFFFF);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        // 切页标签
        drawTabLabel(gui, 8, "gui.akaishi.wireless.tab.run", currentPage == 0);
        drawTabLabel(gui, 8 + TAB_STEP, "gui.akaishi.wireless.tab.transfer", currentPage == 1);
        drawTabLabel(gui, 8 + TAB_STEP * 2, "gui.akaishi.wireless.tab.bind", currentPage == 2);

        if (currentPage == 0) {
            renderRunPage(gui);
        } else if (currentPage == 1) {
            renderTransferPage(gui);
        } else {
            renderBindPage(gui);
        }
    }

    /**
     * 页3：远程绑定（列出对你有「布局」权限的在线终端，点一行即绑定，无需身份卡）。
     * 绑定身份 = 你本人，端口每 tick 会按该身份校验方向权限（输入口=存入 / 输出口=取出）。
     */
    private void renderBindPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        String bound = menu.boundShortId();
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.port.bound_to",
                bound.isEmpty() ? "----" : bound), x + 8, y + BIND_STATUS_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable(menu.isAuthenticated()
                        ? "gui.akaishi.wireless.port.authenticated"
                        : "gui.akaishi.wireless.port.not_authenticated"),
                x + 8, y + BIND_STATUS_Y + 10, menu.isAuthenticated() ? TEXT_GREEN : TEXT_RED, false);
        java.util.List<AkaishiPortBindingSync.Entry> entries = menu.bindingEntries();
        if (entries.isEmpty()) {
            gui.drawString(this.font, this.font.plainSubstrByWidth(
                            Component.translatable("gui.akaishi.wireless.port.no_terminal").getString(),
                            BIND_LIST_W),
                    x + 8, y + BIND_LIST_Y, TEXT_DIM, false);
            return;
        }
        for (int i = 0; i < entries.size() && i < BIND_ROWS; i++) {
            AkaishiPortBindingSync.Entry entry = entries.get(i);
            boolean selected = entry.shortId().equals(bound);
            Component label = Component.translatable("gui.akaishi.wireless.port.entry",
                    entry.shortId(), entry.ownerName().isEmpty() ? "----" : entry.ownerName());
            int rowY = y + BIND_LIST_Y + i * BIND_ROW_H;
            // 选中行：标记在**行首**，终端信息接在其后 —— 避免长文字与行尾标记挤在一起
            int labelX = x + 10;
            if (selected) {
                Component marker = Component.translatable("gui.akaishi.wireless.port.selected");
                gui.drawString(this.font, marker, labelX, rowY, TEXT_GREEN, false);
                labelX += this.font.width(marker) + 4;
            }
            gui.drawString(this.font, this.font.plainSubstrByWidth(label.getString(), 158 - (labelX - x - 10)),
                    labelX, rowY, selected ? TEXT_GREEN : TEXT, false);
        }
    }

    /** 鼠标所在候选行（不在列表内返回 -1） */
    private int bindRowAt(double mouseX, double mouseY) {
        int relY = (int) mouseY - this.topPos - BIND_LIST_Y;
        if (relY < 0 || mouseX < this.leftPos + 8 || mouseX >= this.leftPos + 168) {
            return -1;
        }
        int row = relY / BIND_ROW_H;
        return row < BIND_ROWS ? row : -1;
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

    /** 页2：传输情况（缓冲储能条 + 不限速提示 + 方向提示） */
    private void renderTransferPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        long energy = Math.max(0, Math.min(menu.getEnergy(), menu.getMaxEnergy()));
        long max = Math.max(1, menu.getMaxEnergy());
        int barWidth = (int) (BAR_W * energy / max);
        if (barWidth > 0) {
            gui.fill(x + BAR_X, y + BAR_Y, x + BAR_X + barWidth, y + BAR_Y + BAR_H, 0xFF28B428);
        }
        // 缓冲数值（右对齐防出框）
        Component energyText = Component.literal(EnergyFormat.format(energy) + " / " + EnergyFormat.format(max));
        int textWidth = this.font.width(energyText);
        gui.drawString(this.font, energyText, x + 88 - textWidth / 2, y + BAR_Y + 10, TEXT, false);
        // 无线传输不限速（卡为纯凭证，无速率上限）
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.port.rate_unlimited"),
                x + 8, y + 56, TEXT, false);
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
        // 绑定页：候选行悬停提示（操作说明 + 权限口径）
        if (currentPage == 2 && bindRowAt(mouseX, mouseY) >= 0) {
            gui.renderComponentTooltip(this.font, java.util.List.of(
                    Component.translatable("gui.akaishi.wireless.port.bind_row.tip"),
                    Component.translatable("gui.akaishi.wireless.port.bind_row.dir")), mouseX, mouseY);
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
            if (isIn(this.leftPos + 8 + TAB_STEP, this.topPos + TAB_Y, TAB_W, TAB_H, mouseX, mouseY)) {
                currentPage = 1;
                return true;
            }
            if (isIn(this.leftPos + 8 + TAB_STEP * 2, this.topPos + TAB_Y, TAB_W, TAB_H, mouseX, mouseY)) {
                currentPage = 2;
                return true;
            }
            // 绑定页：点候选行 = 远程绑定到该终端（解绑仍在页1按钮）
            if (currentPage == 2) {
                int row = bindRowAt(mouseX, mouseY);
                java.util.List<AkaishiPortBindingSync.Entry> entries = menu.bindingEntries();
                if (row >= 0 && row < entries.size()) {
                    AkaishiPortBindingSync.sendAction(this.menu.containerId, AkaishiPortBindingSync.ACTION_BIND,
                            entries.get(row).terminalId());
                    return true;
                }
            }
            // 解绑按钮（页1，服务端执行）
            if (currentPage == 0 && menu.getCardHash() != 0
                    && isIn(this.leftPos + UNBIND_X, this.topPos + UNBIND_Y, UNBIND_W, UNBIND_H, mouseX, mouseY)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, AkaishiLifeWirelessPortMenu.BTN_UNBIND);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isIn(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
