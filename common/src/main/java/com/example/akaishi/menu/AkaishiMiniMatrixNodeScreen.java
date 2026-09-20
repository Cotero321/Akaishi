package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMiniMatrixNetworkNodeBlockEntity;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.jetbrains.annotations.Nullable;

/**
 * 网络节点界面（设计稿见任务记录：176×112，无背包区）。
 * <p>
 * <b>版式</b>：标题行 + 右上状态（已申领/未申领）+ 三行只读事实（绑定终端 / 归属者 / 子场域）
 * + 分隔线 + 「节点屏障」开关 + 一行只读说明（防玩家误以为此处能改绑定）。
 * <p>
 * <b>数据从哪来</b>：节点方块实体自身带同步标签（{@code getUpdateTag} / {@code getUpdatePacket}），
 * 客户端那份是实时的，故界面直接读它即可，不需要另做快照包（与矩阵终端的处法不同：那个的读数
 * 不在同步标签里，只能走自定义快照）。
 * <p>
 * <b>值一律截断</b>：右侧值按"可用宽度"截断，避免英文名 / 长坐标右对齐时压到左侧标签。
 */
public class AkaishiMiniMatrixNodeScreen extends AbstractContainerScreen<AkaishiMiniMatrixNodeMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_ON = 0xFF1E7A3C;
    /** 分隔线 / 开关行的框线（与本项目其它自绘控件同色系） */
    private static final int LINE = 0xFF8B8B8B;

    public AkaishiMiniMatrixNodeScreen(AkaishiMiniMatrixNodeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AkaishiMiniMatrixNodeMenu.PANEL_W;
        this.imageHeight = AkaishiMiniMatrixNodeMenu.PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        GuiWidgets.panel(gui, this.leftPos, this.topPos,
                AkaishiMiniMatrixNodeMenu.PANEL_W, AkaishiMiniMatrixNodeMenu.PANEL_H);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 只画标题：原版会在下方再画一行"物品栏"标签，本界面没有背包区，画了就是多余的错位文字
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);

        AkaishiMiniMatrixNetworkNodeBlockEntity node = menu.node();
        boolean bound = node != null && node.claimantId() != null;
        int x = this.leftPos;
        int y = this.topPos;

        // 右上角状态
        Component state = Component.translatable(bound
                ? "gui.akaishi.matrix.node.state_on" : "gui.akaishi.matrix.node.state_off");
        gui.drawString(this.font, state,
                x + AkaishiMiniMatrixNodeMenu.CONTENT_RIGHT - this.font.width(state),
                y + AkaishiMiniMatrixNodeMenu.STATE_Y, bound ? TEXT_ON : TEXT_DIM, false);

        // 三行只读事实
        BlockPos boundPos = node == null ? null : node.claimantPos();
        drawRow(gui, y, AkaishiMiniMatrixNodeMenu.ROW_BOUND_Y, "gui.akaishi.matrix.node.bound",
                boundPos == null ? Component.translatable("gui.akaishi.matrix.node.unbound")
                        : Component.translatable("gui.akaishi.matrix.node.bound_value",
                                boundPos.getX(), boundPos.getY(), boundPos.getZ()));
        String owner = node == null ? null : node.claimantName();
        drawRow(gui, y, AkaishiMiniMatrixNodeMenu.ROW_OWNER_Y, "gui.akaishi.matrix.node.owner",
                owner == null || owner.isEmpty() ? Component.translatable("gui.akaishi.matrix.node.unbound")
                        : Component.literal(owner));
        // 子场域以本节点为中心、半径 1 区块（与矩阵终端的申领参数同源）
        drawRow(gui, y, AkaishiMiniMatrixNodeMenu.ROW_FIELD_Y, "gui.akaishi.matrix.node.field",
                Component.translatable("gui.akaishi.matrix.node.field_value",
                        AkaishiMiniMatrixTerminalBlockEntity.NODE_FIELD_RADIUS,
                        node == null ? 0 : node.getBlockPos().getX() >> 4,
                        node == null ? 0 : node.getBlockPos().getZ() >> 4));

        // 分隔线
        gui.fill(x + AkaishiMiniMatrixNodeMenu.CONTENT_LEFT, y + AkaishiMiniMatrixNodeMenu.DIVIDER_Y,
                x + AkaishiMiniMatrixNodeMenu.CONTENT_RIGHT, y + AkaishiMiniMatrixNodeMenu.DIVIDER_Y + 1, LINE);

        // 开关行（焦点）
        int toggleY = y + AkaishiMiniMatrixNodeMenu.TOGGLE_Y;
        gui.fill(x + AkaishiMiniMatrixNodeMenu.CONTENT_LEFT, toggleY - 2,
                x + AkaishiMiniMatrixNodeMenu.CONTENT_RIGHT, toggleY - 1, LINE);
        gui.fill(x + AkaishiMiniMatrixNodeMenu.CONTENT_LEFT, toggleY + AkaishiMiniMatrixNodeMenu.PILL_H + 1,
                x + AkaishiMiniMatrixNodeMenu.CONTENT_RIGHT, toggleY + AkaishiMiniMatrixNodeMenu.PILL_H + 2, LINE);
        gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.node.barrier"),
                x + AkaishiMiniMatrixNodeMenu.CONTENT_LEFT, toggleY + 3, TEXT, false);
        boolean barrierOn = node == null || node.barrierEnabled();
        GuiWidgets.buttonText(gui, this.font,
                x + AkaishiMiniMatrixNodeMenu.PILL_X, toggleY,
                AkaishiMiniMatrixNodeMenu.PILL_W, AkaishiMiniMatrixNodeMenu.PILL_H,
                Component.translatable(barrierOn
                        ? "gui.akaishi.matrix.node.barrier_on" : "gui.akaishi.matrix.node.barrier_off"), true);

        // 只读说明
        gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.node.readonly_hint"),
                x + AkaishiMiniMatrixNodeMenu.CONTENT_LEFT, y + AkaishiMiniMatrixNodeMenu.HINT_Y, TEXT_DIM, false);

        this.renderTooltip(gui, mouseX, mouseY);
    }

    /** 一行只读事实：左侧标签（暗色）+ 右侧值（右对齐、按可用宽度截断） */
    private void drawRow(GuiGraphics gui, int top, int rowY, String labelKey, @Nullable Component value) {
        Component label = Component.translatable(labelKey);
        gui.drawString(this.font, label,
                this.leftPos + AkaishiMiniMatrixNodeMenu.CONTENT_LEFT, top + rowY, TEXT_DIM, false);
        if (value == null) {
            return;
        }
        int room = AkaishiMiniMatrixNodeMenu.CONTENT_RIGHT - AkaishiMiniMatrixNodeMenu.CONTENT_LEFT
                - this.font.width(label) - 6;
        String text = this.font.plainSubstrByWidth(value.getString(), Math.max(16, room));
        gui.drawString(this.font, text,
                this.leftPos + AkaishiMiniMatrixNodeMenu.CONTENT_RIGHT - this.font.width(text),
                top + rowY, TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isIn(AkaishiMiniMatrixNodeMenu.PILL_X, AkaishiMiniMatrixNodeMenu.TOGGLE_Y,
                AkaishiMiniMatrixNodeMenu.PILL_W, AkaishiMiniMatrixNodeMenu.PILL_H, mouseX, mouseY)) {
            AkaishiMiniMatrixNetworkNodeBlockEntity node = menu.node();
            // 开关是"目标状态"：把当前值的反面发给服务端，由服务端权威写入后再同步回来
            AkaishiMiniMatrixNodeSync.sendToggle(menu.containerId, !(node != null && node.barrierEnabled()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isIn(int rx, int ry, int w, int h, double mouseX, double mouseY) {
        return mouseX >= this.leftPos + rx && mouseX < this.leftPos + rx + w
                && mouseY >= this.topPos + ry && mouseY < this.topPos + ry + h;
    }
}
