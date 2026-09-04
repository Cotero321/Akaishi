package com.example.akaishi.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 聚变物品口界面：27 格缓冲槽（9×3）+ 玩家背包。
 * <p>
 * 原背景贴图内容生成错乱（槽纹拉伸重叠）导致整体错位，已弃用改由 {@link GuiWidgets}
 * 代码自绘：面板灰底 + 槽位框坐标与 {@link AkaishiFusionItemPortMenu} 完全同源，杜绝错位。
 * 输入口提示"燃料棒自动供给控制器"，输出口提示"灰烬自动取出"。
 */
public class AkaishiFusionItemPortScreen extends AbstractContainerScreen<AkaishiFusionItemPortMenu> {

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;

    /** 缓冲槽 9×3 起点（与 Menu 一致） */
    private static final int BUFFER_X0 = 8, BUFFER_Y0 = 17;
    /** 玩家背包 3×9 / 快捷栏 1×9 起始 Y（与 Menu 一致） */
    private static final int INV_Y0 = 84;
    private static final int HOTBAR_Y = 142;

    public AkaishiFusionItemPortScreen(AkaishiFusionItemPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiWidgets.panel(gui, x, y, PANEL_W, PANEL_H);
        // 缓冲槽 9×3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GuiWidgets.slotBox(gui, x + BUFFER_X0 + col * 18, y + BUFFER_Y0 + row * 18);
            }
        }
        // 玩家背包 3×9 + 快捷栏 1×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                GuiWidgets.slotBox(gui, x + 8 + col * 18, y + INV_Y0 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            GuiWidgets.slotBox(gui, x + 8 + col * 18, y + HOTBAR_Y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 缓冲槽下方提示（背包区上方空档）
        Component hint = Component.translatable(this.menu.getKind() == AkaishiFusionItemPortMenu.BufferKind.INPUT_RODS
                ? "gui.akaishi.fusion.item_input_hint" : "gui.akaishi.fusion.item_output_hint");
        gui.drawString(this.font, hint, this.leftPos + 8, this.topPos + 74, 0xFF707070, false);
    }
}
