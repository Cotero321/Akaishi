package com.example.akaishi.menu;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.ClientBodyData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 躯体检查仪界面：自绘"医学扫描"面板（无背包槽）。
 * 展示 9 个躯体槽位：部位名 + 移植器官图标/名称 + 排斥条/数值；
 * 底部汇总总排斥与身体状况评级。数据来自 ClientBodyData（S2C 同步）。
 */
public class AkaishiBodyScannerScreen extends AbstractContainerScreen<AkaishiBodyScannerMenu> {

    /** 界面尺寸（无背包，纯信息面板；加宽以容纳部位名 + 排斥条 + 数值三栏） */
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 180;

    /** 内容区布局 */
    private static final int ROW_X = 10;
    private static final int ROW_START_Y = 26;
    private static final int ROW_HEIGHT = 15;
    private static final int SLOT_NAME_X = ROW_X;
    private static final int ORGAN_ICON_X = 92;
    private static final int ORGAN_NAME_X = 114;
    /** 器官名称最大宽度（超宽截断，避免横穿排斥条） */
    private static final int ORGAN_NAME_MAX_W = 34;
    private static final int REJECT_BAR_X = 152;
    private static final int REJECT_BAR_W = 30;
    private static final int REJECT_BAR_H = 6;
    private static final int REJECT_NUM_X = 192;

    /** 背景色 */
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int PANEL_COLOR = 0xFFB0B0B0;
    private static final int LINE_COLOR = 0xFF373737;

    public AkaishiBodyScannerScreen(AkaishiBodyScannerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        // 自绘面板背景
        gui.fill(this.leftPos, this.topPos, this.leftPos + PANEL_W, this.topPos + PANEL_H, BG_COLOR);
        // 面板内容区
        gui.fill(this.leftPos + 6, this.topPos + 18, this.leftPos + PANEL_W - 6, this.topPos + 158, PANEL_COLOR);

        // 9 行槽位：排斥条
        for (int i = 0; i < BodySlot.values().length; i++) {
            BodySlot slot = BodySlot.values()[i];
            int barY = this.topPos + ROW_START_Y + i * ROW_HEIGHT;
            int x = this.leftPos + REJECT_BAR_X;
            int rej = ClientBodyData.getRejection(slot);
            // 底色 + 填充
            gui.fill(x, barY, x + REJECT_BAR_W, barY + REJECT_BAR_H, LINE_COLOR);
            int width = (int) (REJECT_BAR_W * Math.min(100, rej) / 100.0);
            if (width > 0) {
                gui.fill(x, barY, x + width, barY + REJECT_BAR_H, rejectionColor(rej));
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 标题
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);

        // 9 行槽位：部位名 + 器官图标/名称 + 排斥数值
        for (int i = 0; i < BodySlot.values().length; i++) {
            BodySlot slot = BodySlot.values()[i];
            int rowY = this.topPos + ROW_START_Y + i * ROW_HEIGHT;
            // 部位名
            gui.drawString(this.font, Component.translatable(slot.getNameKey()), this.leftPos + SLOT_NAME_X, rowY - 4, 0x9CC8E0, false);
            // 器官图标 + 名称
            ItemStack organ = ClientBodyData.getOrgan(slot);
            if (!organ.isEmpty()) {
                gui.renderItem(organ, this.leftPos + ORGAN_ICON_X, rowY - 3);
                gui.renderItemDecorations(this.font, organ, this.leftPos + ORGAN_ICON_X, rowY - 3);
                String name = this.font.plainSubstrByWidth(organ.getHoverName().getString(), ORGAN_NAME_MAX_W);
                gui.drawString(this.font, name, this.leftPos + ORGAN_NAME_X, rowY - 4, 0xE0E0E0, false);
            } else {
                gui.drawString(this.font, Component.translatable("gui.akaishi.body_scanner.original"),
                        this.leftPos + ORGAN_NAME_X, rowY - 4, 0x707070, false);
            }
            // 排斥数值
            int rej = ClientBodyData.getRejection(slot);
            gui.drawString(this.font, String.valueOf(rej), this.leftPos + REJECT_NUM_X - this.font.width(String.valueOf(rej)),
                    rowY - 4, rejectionColor(rej), false);
        }

        // 底部汇总：总排斥 + 状况评级
        int total = ClientBodyData.getTotalRejection();
        int occupied = ClientBodyData.getOccupiedCount();
        String statusKey;
        int statusColor;
        if (total >= 100) {
            statusKey = "gui.akaishi.body_scanner.critical";
            statusColor = 0xFFD64545;
        } else if (total >= 60) {
            statusKey = "gui.akaishi.body_scanner.warning";
            statusColor = 0xFF8B6F1E;
        } else if (total >= 30) {
            statusKey = "gui.akaishi.body_scanner.caution";
            statusColor = 0xFFE0A63A;
        } else {
            statusKey = "gui.akaishi.body_scanner.stable";
            statusColor = 0xFF2E7D32;
        }
        int sumY = this.topPos + 164;
        Component summary = Component.translatable("gui.akaishi.body_scanner.summary", occupied, BodySlot.values().length, total);
        gui.drawString(this.font, summary, this.leftPos + SLOT_NAME_X, sumY, 0xE0E0E0, false);
        Component status = Component.translatable(statusKey);
        gui.drawString(this.font, status, this.leftPos + PANEL_W - 8 - this.font.width(status), sumY, statusColor, false);
    }

    /** 排斥值 → 颜色（绿/黄/橙/红） */
    private int rejectionColor(int rej) {
        if (rej >= 100) return 0xFFD64545;
        if (rej >= 60) return 0xFFE08A3A;
        if (rej >= 30) return 0xFF8B6F1E;
        return 0xFF2E7D32;
    }
}
