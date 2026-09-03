package com.example.akaishi.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 生命的融合砧界面：输入（赤石护甲 / 融合锭）+ 输出（生命融合护甲）+ 融合按钮。
 * 纯 vanilla 灰自绘（无贴图依赖），槽框/按钮样式与升级台一致。
 */
public class AkaishiLifeFusionAnvilScreen extends AbstractContainerScreen<AkaishiLifeFusionAnvilMenu> {

    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int TEXT = 0xFF3F3F3F;

    // 与 Menu 槽位坐标保持一致（机器槽横排单行）
    private static final int SLOT_Y = 34;
    private static final int GEAR_X = 26, INGOT_X = 80, OUTPUT_X = 134;
    /** 标签位于槽位上方 12px（避开槽框与彼此，CJK 12px/字宽预留间距） */
    private static final int LABEL_Y = SLOT_Y - 12;
    private static final int FUSE_BTN_X = 30, FUSE_BTN_Y = 62, FUSE_BTN_W = 116, FUSE_BTN_H = 14;

    public AkaishiLifeFusionAnvilScreen(AkaishiLifeFusionAnvilMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 背景面板（vanilla 灰）
        gui.fill(x, y, x + this.imageWidth, y + this.imageHeight, COLOR_BG);
        // 机器槽框（无贴图自绘）
        GuiWidgets.slotBox(gui, x + GEAR_X, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + INGOT_X, y + SLOT_Y);
        GuiWidgets.slotBox(gui, x + OUTPUT_X, y + SLOT_Y);
        // 玩家背包 + 快捷栏槽框（无贴图，需逐格自绘，否则物品栏一片空白）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GuiWidgets.slotBox(gui, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            GuiWidgets.slotBox(gui, x + 8 + col * 18, y + 142);
        }
        // 融合按钮（绿色 + 亮边框，动作按钮）
        fillButton(gui, x + FUSE_BTN_X, y + FUSE_BTN_Y, FUSE_BTN_W, FUSE_BTN_H, 0xFF30A030, 0xFFE0FFE0);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.life_fusion.armor"), GEAR_X, LABEL_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.life_fusion.ingot"), INGOT_X, LABEL_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.life_fusion.output"), OUTPUT_X, LABEL_Y, TEXT, false);
        gui.drawCenteredString(this.font, Component.translatable("gui.akaishi.life_fusion.fuse"),
                FUSE_BTN_X + FUSE_BTN_W / 2, FUSE_BTN_Y + (FUSE_BTN_H - 8) / 2, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && mouseX >= this.leftPos + FUSE_BTN_X && mouseX < this.leftPos + FUSE_BTN_X + FUSE_BTN_W
                && mouseY >= this.topPos + FUSE_BTN_Y && mouseY < this.topPos + FUSE_BTN_Y + FUSE_BTN_H) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, AkaishiLifeFusionAnvilMenu.BUTTON_FUSE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 填充矩形并绘制 1px 边框 */
    private static void fillButton(GuiGraphics gui, int x, int y, int w, int h, int fill, int border) {
        gui.fill(x, y, x + w, y + h, fill);
        gui.fill(x, y, x + w, y + 1, border);
        gui.fill(x, y + h - 1, x + w, y + h, border);
        gui.fill(x, y, x + 1, y + h, border);
        gui.fill(x + w - 1, y, x + w, y + h, border);
    }
}
