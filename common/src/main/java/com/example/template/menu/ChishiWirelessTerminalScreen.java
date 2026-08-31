package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.wireless.WirelessNetworkManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 无线赤能源终端界面（终端方块主界面，四个互斥页面）。
 * 顶部四个切页按钮本地互斥切换（互不重叠）；安全卡认证页含授权槽 + 授权/移除按钮（服务端生效）。
 * 数据来自 {@link ChishiWirelessTerminalMenu} 的 ContainerData（储能 long + 状态/组件/授权数）。
 * 198 高专属纹理：标题 y=6 与切页按钮 y=16 错开，内容区 y=34 起舒展排布。
 */
public class ChishiWirelessTerminalScreen extends AbstractContainerScreen<ChishiWirelessTerminalMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(TemplateMod.MOD_ID, "textures/gui/chishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_RED = 0xFFB03030;
    private static final int TEXT_GREEN = 0xFF2E7D32;

    // 切页按钮（40×12，四个并排，y=16 避开标题）
    private static final int TAB_W = 40;
    private static final int TAB_H = 12;
    private static final int TAB_Y = 16;
    private static final int[] TAB_X = {8, 48, 88, 128};
    private static final String[] TAB_KEY = {
            "gui.template_mod.wireless.tab.run",
            "gui.template_mod.wireless.tab.energy",
            "gui.template_mod.wireless.tab.security",
            "gui.template_mod.wireless.tab.transfer"};

    // 储能条（页2）
    private static final int BAR_X = 20;
    private static final int BAR_Y = 34;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;

    // 安全页：授权/移除按钮（与授权槽同排）
    private static final int BTN_X = 88;
    private static final int BTN_Y = 46;
    private static final int BTN_W = 40;
    private static final int BTN_H = 12;

    /** 当前页面（0=运行，1=储能，2=安全认证，3=传输） */
    private int currentPage;

    public ChishiWirelessTerminalScreen(ChishiWirelessTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
        // 安全方块/终端方块打开时经网络缓冲指定初始页
        this.currentPage = menu.getInitialPage();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        for (int i = 0; i < 4; i++) {
            GuiWidgets.button(gui, x + TAB_X[i], y + TAB_Y, TAB_W, TAB_H);
        }
        // 安全页：授权槽背景框
        if (currentPage == ChishiWirelessTerminalMenu.PAGE_SECURITY) {
            GuiWidgets.slotBox(gui, x + ChishiWirelessTerminalMenu.CARD_SLOT_X, y + ChishiWirelessTerminalMenu.CARD_SLOT_Y);
            GuiWidgets.button(gui, x + BTN_X, y + BTN_Y, BTN_W, BTN_H);
            GuiWidgets.button(gui, x + BTN_X + 42, y + BTN_Y, BTN_W, BTN_H);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        for (int i = 0; i < 4; i++) {
            Component label = Component.translatable(TAB_KEY[i]);
            int w = this.font.width(label);
            gui.drawString(this.font, label,
                    TAB_X[i] + (TAB_W - w) / 2, TAB_Y + 2, currentPage == i ? TEXT : TEXT_DIM, false);
        }
        switch (currentPage) {
            case ChishiWirelessTerminalMenu.PAGE_RUN -> renderRunPage(gui);
            case ChishiWirelessTerminalMenu.PAGE_ENERGY -> renderEnergyPage(gui);
            case ChishiWirelessTerminalMenu.PAGE_SECURITY -> renderSecurityPage(gui);
            case ChishiWirelessTerminalMenu.PAGE_TRANSFER -> renderTransferPage(gui);
            default -> {
            }
        }
    }

    /** 页1：终端运行情况（成型/ID/口统计/绑定储能/组件状态） */
    private void renderRunPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        gui.drawString(this.font, Component.translatable(menu.isFormed()
                        ? "gui.template_mod.wireless.terminal.formed" : "gui.template_mod.wireless.terminal.unformed"),
                x + 8, y + 36, menu.isFormed() ? TEXT_GREEN : TEXT_RED, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.id",
                menu.getTerminalShortId()), x + 8, y + 48, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.ports",
                menu.getInputCount(), menu.getOutputCount()), x + 8, y + 60, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.bound",
                menu.getBoundSerializers()), x + 8, y + 72, TEXT, false);
        // 组件状态（跨维/区块加载/范围），长文本截断防出框
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.components",
                menu.isCrossDim() ? "OK" : "NO",
                menu.isChunkLoad() ? "OK" : "NO",
                menu.isChunkRange() ? "3x3" : "1x1"),
                x + 8, y + 84, TEXT_DIM, false);
    }

    /** 页2：能量储存情况（储能条 + 数值 + 容量 + 绑定单元） */
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
        Component energyText = Component.literal(EnergyFormat.format(energy) + " / " + EnergyFormat.format(max));
        int textWidth = this.font.width(energyText);
        gui.drawString(this.font, energyText, x + 88 - textWidth / 2, y + BAR_Y + 10, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.capacity",
                EnergyFormat.format(max)), x + 8, y + 60, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.bound",
                menu.getBoundSerializers()), x + 8, y + 72, TEXT_DIM, false);
    }

    /** 页3：安全卡认证（授权卡数 + 授权槽 + 授权/移除按钮） */
    private void renderSecurityPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.authorized",
                menu.getAuthorizedCount(), menu.maxAuthorized()), x + 8, y + 36, TEXT, false);
        // 授权槽（y=46）上方的说明文字
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.auth_hint"),
                x + 8, y + 68, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.auth_hint2"),
                x + 8, y + 78, TEXT_DIM, false);
        // 按钮标签（授权槽上方）
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.authorize"),
                x + BTN_X + 8, y + BTN_Y + 2, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.revoke"),
                x + BTN_X + 42 + 8, y + BTN_Y + 2, TEXT, false);
    }

    /** 页4：能量传输（口统计/速率/损耗规则/抑制/区块加载） */
    private void renderTransferPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.ports",
                menu.getInputCount(), menu.getOutputCount()), x + 8, y + 36, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.rate_hint"),
                x + 8, y + 48, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable(menu.isCrossDim()
                        ? "gui.template_mod.wireless.terminal.crossdim_on"
                        : "gui.template_mod.wireless.terminal.crossdim_off"),
                x + 8, y + 60, menu.isCrossDim() ? TEXT_GREEN : TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("gui.template_mod.wireless.terminal.loss",
                menu.inputLossModules(), menu.outputLossModules()), x + 8, y + 72, TEXT, false);
        gui.drawString(this.font, Component.translatable(menu.isChunkLoad()
                        ? (menu.isChunkRange() ? "gui.template_mod.wireless.terminal.chunk_3x3"
                        : "gui.template_mod.wireless.terminal.chunk_1x1")
                        : "gui.template_mod.wireless.terminal.chunk_off"),
                x + 8, y + 84, menu.isChunkLoad() ? TEXT_GREEN : TEXT_DIM, false);
    }

    /** 非安全页时停用授权槽（isActive=false → 不渲染也不可点击） */
    private void syncCardSlot() {
        this.menu.setSecuritySlotActive(currentPage == ChishiWirelessTerminalMenu.PAGE_SECURITY);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        syncCardSlot();
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
        // 储能条悬停提示（页2）
        if (currentPage == ChishiWirelessTerminalMenu.PAGE_ENERGY
                && isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                    mouseX, mouseY);
        }
        // 安全页：授权卡上限提示
        if (currentPage == ChishiWirelessTerminalMenu.PAGE_SECURITY
                && menu.getAuthorizedCount() >= WirelessNetworkManager.MAX_AUTHORIZED_CARDS) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.template_mod.wireless.terminal.auth_full"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 切页按钮（本地互斥切换）
            for (int i = 0; i < 4; i++) {
                if (isIn(this.leftPos + TAB_X[i], this.topPos + TAB_Y, TAB_W, TAB_H, mouseX, mouseY)) {
                    currentPage = i;
                    return true;
                }
            }
            // 授权/移除按钮（仅安全页，服务端执行）
            if (currentPage == ChishiWirelessTerminalMenu.PAGE_SECURITY) {
                if (isIn(this.leftPos + BTN_X, this.topPos + BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                            ChishiWirelessTerminalMenu.BTN_AUTHORIZE);
                    return true;
                }
                if (isIn(this.leftPos + BTN_X + 42, this.topPos + BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                            ChishiWirelessTerminalMenu.BTN_REVOKE);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isIn(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
