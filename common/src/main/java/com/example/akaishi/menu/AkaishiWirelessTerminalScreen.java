package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 无线赤能源终端界面（终端方块主界面，四个互斥页面）。
 * 顶部四个切页按钮本地互斥切换（互不重叠）；安全卡认证页含授权槽 + 授权/移除按钮（服务端生效）。
 * 数据来自 {@link AkaishiWirelessTerminalMenu} 的 ContainerData（储能 long + 状态/组件/授权数）。
 * 198 高专属纹理：标题 y=6 与切页按钮 y=16 错开，内容区 y=34 起舒展排布。
 */
public class AkaishiWirelessTerminalScreen extends AbstractContainerScreen<AkaishiWirelessTerminalMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
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
            "gui.akaishi.wireless.tab.run",
            "gui.akaishi.wireless.tab.energy",
            "gui.akaishi.wireless.tab.security",
            "gui.akaishi.wireless.tab.transfer"};

    // 储能条（页2）
    private static final int BAR_X = 20;
    private static final int BAR_Y = 34;
    private static final int BAR_W = 136;
    private static final int BAR_H = 8;

    // 组件状态文本最大宽度（面板 176，留出右缘边距防出框/重叠）
    private static final int COMPONENTS_MAX_W = 148;

    /** 当前页面（0=运行，1=储能，2=安全认证，3=传输） */
    private int currentPage;

    public AkaishiWirelessTerminalScreen(AkaishiWirelessTerminalMenu menu, Inventory inv, Component title) {
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
        // 安全页：授权槽 + 登记/移除 + 权限表（共用实现 SecurityPage）
        if (currentPage == AkaishiWirelessTerminalMenu.PAGE_SECURITY) {
            SecurityPage.renderBg(gui, x, y, this.menu, mouseY);
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
            case AkaishiWirelessTerminalMenu.PAGE_RUN -> renderRunPage(gui);
            case AkaishiWirelessTerminalMenu.PAGE_ENERGY -> renderEnergyPage(gui);
            case AkaishiWirelessTerminalMenu.PAGE_SECURITY ->
                    SecurityPage.renderLabels(gui, this.font, 0, 0, this.menu, TEXT, TEXT_DIM, TEXT_GREEN);
            case AkaishiWirelessTerminalMenu.PAGE_TRANSFER -> renderTransferPage(gui);
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
                        ? "gui.akaishi.wireless.terminal.formed" : "gui.akaishi.wireless.terminal.unformed"),
                x + 8, y + 36, menu.isFormed() ? TEXT_GREEN : TEXT_RED, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.terminal.id",
                menu.getTerminalShortId()), x + 8, y + 48, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.terminal.ports",
                menu.getInputCount(), menu.getOutputCount()), x + 8, y + 60, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.terminal.bound",
                menu.getBoundSerializers()), x + 8, y + 72, TEXT, false);
        // 组件状态（跨维/区块加载/范围），长文本截断防出框
        String components = Component.translatable("gui.akaishi.wireless.terminal.components",
                menu.isCrossDim() ? "OK" : "NO",
                menu.isChunkLoad() ? "OK" : "NO",
                menu.isChunkRange() ? "3x3" : "1x1").getString();
        gui.drawString(this.font, this.font.plainSubstrByWidth(components, COMPONENTS_MAX_W),
                x + 8, y + 84, TEXT_DIM, false);
        // 区块加载状态：绿字显示当前弱加载区块数（区块加载免能量税）
        if (menu.isChunkLoad()) {
            gui.drawString(this.font,
                    Component.translatable("gui.akaishi.wireless.terminal.chunk_tax", menu.getChunkLoaded()),
                    x + 8, y + 96, TEXT_GREEN, false);
        }
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
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.terminal.capacity",
                EnergyFormat.format(max)), x + 8, y + 60, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.terminal.bound",
                menu.getBoundSerializers()), x + 8, y + 72, TEXT_DIM, false);
    }

    /** 页4：能量传输（口统计/速率/损耗规则/抑制/区块加载） */
    private void renderTransferPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.terminal.ports",
                menu.getInputCount(), menu.getOutputCount()), x + 8, y + 36, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.terminal.rate_hint"),
                x + 8, y + 48, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable(menu.isCrossDim()
                        ? "gui.akaishi.wireless.terminal.crossdim_on"
                        : "gui.akaishi.wireless.terminal.crossdim_off"),
                x + 8, y + 60, menu.isCrossDim() ? TEXT_GREEN : TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.wireless.terminal.loss",
                menu.inputLossModules(), menu.outputLossModules()), x + 8, y + 72, TEXT, false);
        gui.drawString(this.font, Component.translatable(menu.isChunkLoad()
                        ? (menu.isChunkRange() ? "gui.akaishi.wireless.terminal.chunk_3x3"
                        : "gui.akaishi.wireless.terminal.chunk_1x1")
                        : "gui.akaishi.wireless.terminal.chunk_off"),
                x + 8, y + 84, menu.isChunkLoad() ? TEXT_GREEN : TEXT_DIM, false);
    }

    /** 非安全页时停用授权槽（isActive=false → 不渲染也不可点击） */
    private void syncCardSlot() {
        this.menu.setSecuritySlotActive(currentPage == AkaishiWirelessTerminalMenu.PAGE_SECURITY);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        syncCardSlot();
        super.render(gui, mouseX, mouseY, partialTick);
        // 从矩阵左列跳来时给出的回头路（贴在面板左侧外，不与任何既有控件重叠）
        MiniMatrixReturn.render(gui, this.font, this.leftPos, this.topPos);
        this.renderTooltip(gui, mouseX, mouseY);
        // 储能条悬停提示（页2）
        if (currentPage == AkaishiWirelessTerminalMenu.PAGE_ENERGY
                && isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.energy",
                            EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getMaxEnergy())),
                    mouseX, mouseY);
        }
        // 安全页：勾选框 → 权限名 + 说明；按钮 / 归属者 → 操作说明（共用实现）
        if (currentPage == AkaishiWirelessTerminalMenu.PAGE_SECURITY) {
            SecurityPage.renderTooltip(gui, this.font, this.leftPos, this.topPos, mouseX, mouseY, this.menu);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MiniMatrixReturn.mouseClicked(mouseX, mouseY, button, this.leftPos, this.topPos,
                    this.menu.containerId)) {
                return true;
            }
            // 切页按钮（本地互斥切换）
            for (int i = 0; i < 4; i++) {
                if (isIn(this.leftPos + TAB_X[i], this.topPos + TAB_Y, TAB_W, TAB_H, mouseX, mouseY)) {
                    currentPage = i;
                    return true;
                }
            }
            // 安全页：登记 / 移除 / 勾选权限（共用实现，走 C2S 动作包 + 服务端 SECURITY 校验）
            if (currentPage == AkaishiWirelessTerminalMenu.PAGE_SECURITY
                    && SecurityPage.mouseClicked(mouseX, mouseY, this.leftPos, this.topPos, this.menu,
                            this.menu.containerId)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isIn(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
