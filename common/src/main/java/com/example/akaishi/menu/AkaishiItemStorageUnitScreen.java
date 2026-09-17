package com.example.akaishi.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 物品储存单元界面（176×230 自绘，D18 只读视图）：<b>按类合并</b>的内容视图 + 占用 IP 条 + 剩余容量。
 * <p>
 * 与物品终端同一套版式语言，但没有任何交互入口：这里只回答「装了什么、还剩多少」，
 * 存取一律回物品终端做（本机是被动存储，见 {@link AkaishiItemStorageUnitMenu} 说明）。
 * <p>
 * <b>合并视图由容器投影给出</b>（{@link UnitReadOnlyContainer}），但图标与总量由本界面自绘
 * （{@link #drawGrid}）：原版槽渲染已通过 {@link UnitViewSlot#getNoItemIcon()} 让出物品绘制，
 * 于是"先图标、后数字"的顺序由代码保证，不再受原版渲染时机影响。
 */
public class AkaishiItemStorageUnitScreen extends AbstractContainerScreen<AkaishiItemStorageUnitMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    /** IP 占用条识别色：与物品终端保持一致 */
    private static final int COLOR_IP_BAR = 0xFF35C8E8;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 18;
    private static final int BAR_W = 160;
    private static final int BAR_H = 7;
    private static final int HINT_Y = 138;

    public AkaishiItemStorageUnitScreen(AkaishiItemStorageUnitMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AkaishiItemStorageUnitMenu.PANEL_W;
        this.imageHeight = AkaishiItemStorageUnitMenu.PANEL_H;
    }

    /**
     * 1.20.1 的 {@code AbstractContainerScreen.render} 既不会画背景层、也不会画 tooltip，
     * 必须由子类显式调用（项目其它自绘界面同范式）——漏了会出现「无暗背景 + 悬停无提示」。
     * <p>
     * {@code super.render} 外面包了 {@code vanillaRenderPass}：该趟让视图槽对原版伪装成空槽，
     * 原版就不会画物品（它的物品几何到帧末最后一次 flush 才落屏，会把我们立即画的数字盖住）；
     * 图标 + 合并总量改由 {@link #drawGrid} 完全自绘，顺序由代码保证。
     */
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        this.menu.setVanillaRenderPass(true);
        try {
            super.render(gui, mouseX, mouseY, partialTick);
        } finally {
            this.menu.setVanillaRenderPass(false);
        }
        drawGrid(gui);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    /**
     * 自绘格子内容：按合并视图逐格画物品模型（立刻落屏）再画 K/M 总量，
     * 顺序为「先图标、后数字」，不依赖原版槽渲染的时机与渲染批次。
     */
    private void drawGrid(GuiGraphics gui) {
        gui.pose().pushPose();
        gui.pose().translate(this.leftPos, this.topPos, 0.0f);
        for (int cell = 0; cell < this.menu.viewSize(); cell++) {
            ItemStack display = this.menu.slots.get(cell).getItem();
            if (display.isEmpty()) {
                continue;
            }
            int cellX = cellX(cell);
            int cellY = cellY(cell);
            gui.renderItem(display, cellX, cellY);
            gui.flush();
            long total = this.menu.viewTotal(cell);
            if (total > 1L) {
                GuiWidgets.amountLabel(gui, this.font, cellX, cellY, EnergyFormat.format(total));
            }
        }
        gui.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);

        GuiWidgets.track(gui, x + BAR_X, y + BAR_Y, BAR_W, BAR_H);
        GuiWidgets.bar(gui, x + BAR_X + 1, y + BAR_Y + 1, BAR_W - 2, BAR_H - 2,
                this.menu.usedIp(), Math.max(1L, this.menu.capacityIp()), COLOR_IP_BAR);

        GuiWidgets.playerInventory(gui, x, y, AkaishiItemStorageUnitMenu.INV_TOP, AkaishiItemStorageUnitMenu.HOTBAR_Y);
        // 只画外框：图标由本界面 drawGrid 自绘，原版那一趟已被伪装成空槽
        for (Slot slot : this.menu.slots) {
            if (slot instanceof UnitViewSlot) {
                GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 全部自绘标签，不调用 super：避免原版标题/物品栏标签与本版式重叠
        gui.drawString(this.font, this.title, 8, 6, TEXT, false);
        Component remaining = Component.translatable("gui.akaishi.item_storage_unit.remaining",
                EnergyFormat.format(this.menu.remainingIp()));
        gui.drawString(this.font, remaining, this.imageWidth - 8 - this.font.width(remaining), 6, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.item_storage_unit.hint"),
                8, HINT_Y, TEXT_DIM, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        if (isHovering(BAR_X, BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.akaishi.item_storage_unit.ip_tip",
                            EnergyFormat.format(this.menu.usedIp()),
                            EnergyFormat.format(this.menu.capacityIp()),
                            EnergyFormat.format(this.menu.remainingIp())),
                    Component.translatable("gui.akaishi.item_lib.summary",
                            Integer.toString(this.menu.viewSize()),
                            EnergyFormat.format(this.menu.viewItemTotal()))), mouseX, mouseY);
            return;
        }
        int cell = cellIndexAt(mouseX, mouseY);
        if (cell >= 0) {
            if (cell < this.menu.viewSize()) {
                // 容器投影给出的展示堆（恒 1 件、保留 NBT）
                ItemStack display = this.menu.slots.get(cell).getItem();
                // 完整 tooltip（含 NBT 信息）+ 该类总件数
                List<Component> lines = new ArrayList<>(getTooltipFromItem(this.minecraft, display));
                lines.add(Component.translatable("gui.akaishi.item_terminal.amount",
                        EnergyFormat.exact(this.menu.viewTotal(cell))));
                gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            }
            return; // 空格不显示任何提示
        }
        super.renderTooltip(gui, mouseX, mouseY);
    }

    /** 合并格序号 → 槽位 x（Menu 坐标） */
    private static int cellX(int cell) {
        return AkaishiItemStorageUnitMenu.SLOT_X
                + (cell % AkaishiItemStorageUnitMenu.COLUMNS) * AkaishiItemStorageUnitMenu.SLOT_STEP;
    }

    /** 合并格序号 → 槽位 y（Menu 坐标）；越界返回 -1 */
    private static int cellY(int cell) {
        int row = cell / AkaishiItemStorageUnitMenu.COLUMNS;
        return row < AkaishiItemStorageUnitMenu.ROWS
                ? AkaishiItemStorageUnitMenu.SLOT_Y + row * AkaishiItemStorageUnitMenu.SLOT_STEP
                : -1;
    }

    /** 鼠标落在第几个合并格；不在单元区内返回 -1 */
    private int cellIndexAt(double mouseX, double mouseY) {
        int col = (int) Math.floor((mouseX - this.leftPos - AkaishiItemStorageUnitMenu.SLOT_X)
                / (double) AkaishiItemStorageUnitMenu.SLOT_STEP);
        int row = (int) Math.floor((mouseY - this.topPos - AkaishiItemStorageUnitMenu.SLOT_Y)
                / (double) AkaishiItemStorageUnitMenu.SLOT_STEP);
        if (col < 0 || col >= AkaishiItemStorageUnitMenu.COLUMNS
                || row < 0 || row >= AkaishiItemStorageUnitMenu.ROWS) {
            return -1;
        }
        return row * AkaishiItemStorageUnitMenu.COLUMNS + col;
    }
}
