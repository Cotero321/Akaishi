package com.example.akaishi.menu;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.lwjgl.glfw.GLFW;

/**
 * 物品终端界面（184×229 自绘）：AE2 网格终端的简化移植。
 * <p>
 * 版式：标题 + 单元数 → IP 容量条 → IP 文本 → <b>搜索框</b> → 4×9 库页可视区 + 右侧滚动条
 * → 操作提示 → 玩家背包。
 * 库页只渲染「聚合条目」：数量按 AE2 做法用 0.666 倍字体画在槽位右下角（K/M 缩写）；
 * 滚动由鼠标滚轮驱动（每档 1 行，同 AE2 的 {@code Scrollbar#pageSize}）；
 * 搜索在本机过滤（名称 / 拼音首字母 / 注册名），点击一律转成 C2S 动作包，绝不走原版点击管线。
 */
public class AkaishiItemTerminalScreen extends AbstractContainerScreen<AkaishiItemTerminalMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    /** IP 容量条识别色：与赤能源（红）、生命能量区分开 */
    private static final int COLOR_IP_BAR = 0xFF35C8E8;
    /** 滚动条把柄：暗底 + 亮/暗描边，与浅色轨道形成强对比（原先灰把柄压在灰轨道上几乎看不见） */
    private static final int COLOR_SCROLL_HANDLE = 0xFF6E6E6E;
    private static final int COLOR_SCROLL_HANDLE_LIGHT = 0xFFB8B8B8;
    private static final int COLOR_SCROLL_HANDLE_DARK = 0xFF3A3A3A;
    /** 滚动把柄最小高度，保证条目很少时仍可抓取 */
    private static final int MIN_HANDLE_H = 12;

    private static final int IP_BAR_X = 8;
    private static final int IP_BAR_Y = 17;
    private static final int IP_BAR_W = 168;
    private static final int IP_BAR_H = 8;
    /** IP 文本行 / 操作提示行 */
    private static final int IP_TEXT_Y = 27;
    private static final int HINT_Y = 133;

    /** 搜索框（原版 EditBox：自带背景/光标/选中，行为与其它模组终端一致） */
    private EditBox searchBox;

    public AkaishiItemTerminalScreen(AkaishiItemTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = AkaishiItemTerminalMenu.PANEL_W;
        this.imageHeight = AkaishiItemTerminalMenu.PANEL_H;
    }

    @Override
    protected void init() {
        super.init();
        // 库页可视槽在客户端追加（对齐 AE2 RepoSlot）：服务端没有这些槽，故玩家槽下标两侧一致。
        // init 会因窗口尺寸变化被反复调用，故先清除上一轮追加的虚拟槽，避免重复膨胀
        this.menu.slots.removeIf(slot -> slot instanceof TerminalDisplaySlot);
        for (int row = 0; row < AkaishiItemTerminalMenu.ROWS; row++) {
            for (int col = 0; col < AkaishiItemTerminalMenu.COLUMNS; col++) {
                this.menu.slots.add(new TerminalDisplaySlot(this.menu,
                        row * AkaishiItemTerminalMenu.COLUMNS + col,
                        AkaishiItemTerminalMenu.SLOT_X + col * AkaishiItemTerminalMenu.SLOT_STEP,
                        AkaishiItemTerminalMenu.SLOT_Y + row * AkaishiItemTerminalMenu.SLOT_STEP));
            }
        }
        // 搜索框：原版 EditBox 自带深色背景，落在浅色面板上很突兀 ⇒ 关掉自带边框，
        // 改为自绘浅色凹槽（与外层统一），输入框内缩 4px 让文字不贴边
        this.searchBox = new EditBox(this.font,
                this.leftPos + AkaishiItemTerminalMenu.SEARCH_X + 4,
                this.topPos + AkaishiItemTerminalMenu.SEARCH_Y + 4,
                AkaishiItemTerminalMenu.SEARCH_W - 8,
                AkaishiItemTerminalMenu.SEARCH_H - 8,
                Component.translatable("gui.akaishi.item_terminal.search"));
        this.searchBox.setBordered(false);
        // 白字 + 中深灰底槽：对比足够且都读得清（深色字落在灰底上反而发闷）
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setTextColorUneditable(0xFFFFFFFF);
        this.searchBox.setHint(Component.translatable("gui.akaishi.item_terminal.search_hint"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setValue(this.menu.searchQuery());
        this.searchBox.setResponder(this.menu::setSearchQuery);
        addRenderableWidget(this.searchBox);
    }

    /**
     * 搜索框聚焦时吞掉其它按键。
     * <p>
     * 必须拦：原版 {@code EditBox} 只在 {@code charTyped} 里吃字符，{@code keyPressed} 对字母键不消费，
     * 于是按下 e/w 会继续冒泡给 {@code Screen}，被当成「打开背包 / 前进」直接关掉界面或让玩家走路。
     * ESC / TAB 除外（保留关界面与切焦点）。
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()
                && keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_TAB) {
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 1.20.1 的 {@code AbstractContainerScreen.render} 既不会画背景层、也不会画 tooltip，
     * 必须由子类显式调用（项目其它自绘界面同范式）——漏了会出现「无暗背景 + 悬停无提示」。
     * <p>
     * {@code super.render} 外面包了 {@code vanillaRenderPass}：这一趟让显示槽对原版伪装成空槽，
     * 原版就不会画物品（它的物品几何要到帧末最后一次 flush 才落屏，会把我们立即画的数字盖住）。
     * 图标 + 数量改由 {@link #drawGrid} 完全自绘，顺序由代码保证。
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
     * 自绘格子内容：每个条目先画物品模型、立刻落屏（{@code flush}），再画 K/M 总量。
     * <p>
     * 之所以要自己画而不是交给原版槽渲染：原版那一趟的几何缓冲到帧末才 flush，
     * 时机晚于我们这一趟，数字会被物品盖住。这里按"图标 → 数字"逐格画完，顺序由代码保证。
     */
    private void drawGrid(GuiGraphics gui) {
        // 本方法在 super.render 之外调用，位姿栈没有 GUI 原点平移（slot.x/slot.y 是相对 GUI 的坐标）
        gui.pose().pushPose();
        gui.pose().translate(this.leftPos, this.topPos, 0.0f);
        for (Slot slot : this.menu.slots) {
            if (!(slot instanceof TerminalDisplaySlot displaySlot)) {
                continue;
            }
            AkaishiItemTerminalSync.Entry entry = displaySlot.entry();
            if (entry == null) {
                continue;
            }
            gui.renderItem(entry.display(), slot.x, slot.y);
            gui.flush();
            if (entry.amount() > 1L) {
                GuiWidgets.amountLabel(gui, this.font, slot.x, slot.y, EnergyFormat.format(entry.amount()));
            }
        }
        gui.pose().popPose();
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);

        // IP 容量条（内缩 1px 填充，避免覆盖轨道边框）
        GuiWidgets.track(gui, x + IP_BAR_X, y + IP_BAR_Y, IP_BAR_W, IP_BAR_H);
        GuiWidgets.bar(gui, x + IP_BAR_X + 1, y + IP_BAR_Y + 1, IP_BAR_W - 2, IP_BAR_H - 2,
                this.menu.usedIp(), Math.max(1L, this.menu.capacityIp()), COLOR_IP_BAR);
        // 搜索框凹槽：输入框自身已关掉边框（否则自带深色底与浅色面板冲突），底与外框由这里统一画
        GuiWidgets.inputWell(gui, x + AkaishiItemTerminalMenu.SEARCH_X, y + AkaishiItemTerminalMenu.SEARCH_Y,
                AkaishiItemTerminalMenu.SEARCH_W, AkaishiItemTerminalMenu.SEARCH_H);

        GuiWidgets.playerInventory(gui, x, y, AkaishiItemTerminalMenu.INV_TOP, AkaishiItemTerminalMenu.HOTBAR_Y);
        for (Slot slot : this.menu.slots) {
            if (slot instanceof TerminalDisplaySlot) {
                GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
            }
        }
        drawScrollbar(gui, x, y);
    }

    /** 右侧滚动条：轨道常驻，条目超出可视区时才画把柄（位置按滚动行等比例换算） */
    private void drawScrollbar(GuiGraphics gui, int x, int y) {
        int trackX = x + AkaishiItemTerminalMenu.SCROLLBAR_X;
        int trackY = y + AkaishiItemTerminalMenu.SLOT_Y;
        int trackH = AkaishiItemTerminalMenu.ROWS * AkaishiItemTerminalMenu.SLOT_STEP;
        int trackW = AkaishiItemTerminalMenu.SCROLLBAR_W;
        GuiWidgets.track(gui, trackX, trackY, trackW, trackH);
        int max = this.menu.maxScrollRow();
        if (max <= 0) {
            return;
        }
        int handleH = Math.max(MIN_HANDLE_H,
                trackH * AkaishiItemTerminalMenu.ROWS / this.menu.totalRows());
        int offset = (trackH - handleH) * this.menu.scrollRow() / max;
        int hx = trackX + 1;
        int hw = trackW - 2;
        int hy = trackY + offset + 1;
        int hh = handleH - 2;
        gui.fill(hx, hy, hx + hw, hy + hh, COLOR_SCROLL_HANDLE);
        gui.fill(hx, hy, hx + hw, hy + 1, COLOR_SCROLL_HANDLE_LIGHT);
        gui.fill(hx, hy, hx + 1, hy + hh, COLOR_SCROLL_HANDLE_LIGHT);
        gui.fill(hx, hy + hh - 1, hx + hw, hy + hh, COLOR_SCROLL_HANDLE_DARK);
        gui.fill(hx + hw - 1, hy, hx + hw, hy + hh, COLOR_SCROLL_HANDLE_DARK);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 全部自绘标签，不调用 super：避免原版标题/物品栏标签与本版式重叠
        gui.drawString(this.font, this.title, 8, 6, TEXT, false);
        Component units = Component.translatable("gui.akaishi.item_terminal.units", this.menu.unitCount());
        gui.drawString(this.font, units, this.imageWidth - 8 - this.font.width(units), 6, TEXT_DIM, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.item_terminal.ip",
                        EnergyFormat.format(this.menu.usedIp()), EnergyFormat.format(this.menu.capacityIp())),
                8, IP_TEXT_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.item_terminal.hint"),
                8, HINT_Y, TEXT_DIM, false);
        // 库区状态提示：未成型 / 未贴装单元 / 库空 三种原因必须分开显示 ——
        // 三者在界面上都是"空库"，但处置方式完全不同
        Component status = null;
        int statusColor = TEXT_DIM;
        if (!this.menu.formed()) {
            status = Component.translatable("gui.akaishi.item_terminal.unformed");
            statusColor = 0xFFA03030;
        } else if (this.menu.unitCount() == 0) {
            status = Component.translatable("gui.akaishi.item_terminal.fail.no_unit");
            statusColor = 0xFFA03030;
        } else if (this.menu.totalRows() == 0) {
            boolean searching = !this.menu.searchQuery().isBlank();
            status = Component.translatable(searching
                    ? "gui.akaishi.item_terminal.no_match"
                    : "gui.akaishi.item_terminal.empty");
            statusColor = searching ? 0xFFA03030 : TEXT_DIM;
        }
        if (status != null) {
            gui.drawString(this.font, status, (this.imageWidth - this.font.width(status)) / 2,
                    AkaishiItemTerminalMenu.SLOT_Y + 30, statusColor, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 每档滚动 1 行：AE2 Scrollbar 的 pageSize = max(1, 可视行数/6)，4 行时即 1 行
        int max = this.menu.maxScrollRow();
        if (max <= 0) {
            return false;
        }
        int next = Math.max(0, Math.min(max, this.menu.scrollRow() + (delta > 0 ? -1 : 1)));
        if (next == this.menu.scrollRow()) {
            return false;
        }
        this.menu.scrollTo(next);
        return true;
    }

    @Override
    public void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (slot instanceof TerminalDisplaySlot displaySlot) {
            // 虚拟槽不进原版点击管线（否则原版会尝试本地改写内容）：
            // 只把「点中的条目 + 动作」发给服务端，由服务端扣费并落账
            AkaishiItemTerminalSync.sendAction(this.menu.containerId,
                    actionFor(mouseButton, clickType), displaySlotItem(displaySlot));
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }

    /** AE2 动作映射：左键取整组 / 右键取一件 / Shift+左键整条进背包 */
    private static byte actionFor(int mouseButton, ClickType clickType) {
        if (clickType == ClickType.QUICK_MOVE) {
            return mouseButton == 1 ? TerminalActions.PICKUP_SINGLE : TerminalActions.SHIFT_CLICK;
        }
        return mouseButton == 1 ? TerminalActions.SPLIT_OR_PLACE_SINGLE : TerminalActions.PICKUP_OR_SET_DOWN;
    }

    /** 点中条目 → 该条目的展示堆；点空白格 → 空堆（服务端据此只走「放入手中物」分支） */
    private static ItemStack displaySlotItem(TerminalDisplaySlot slot) {
        AkaishiItemTerminalSync.Entry entry = slot.entry();
        return entry == null ? ItemStack.EMPTY : entry.display();
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        if (this.hoveredSlot instanceof TerminalDisplaySlot displaySlot) {
            AkaishiItemTerminalSync.Entry entry = displaySlot.entry();
            if (entry != null) {
                // 完整 tooltip：名称 + 附魔 + 进度条 + 全部 NBT 信息行（否则无法确认 NBT 功能），
                // 末尾再补一条聚合总量
                List<Component> lines = new ArrayList<>(getTooltipFromItem(this.minecraft, entry.display()));
                lines.add(Component.translatable("gui.akaishi.item_terminal.amount",
                        EnergyFormat.exact(entry.amount())));
                gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
        if (isHovering(IP_BAR_X, IP_BAR_Y, IP_BAR_W, IP_BAR_H, mouseX, mouseY)) {
            gui.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.akaishi.item_terminal.ip_tip",
                            EnergyFormat.format(this.menu.usedIp()), EnergyFormat.format(this.menu.capacityIp()),
                            EnergyFormat.format(this.menu.maxBatchIp(true)),
                            EnergyFormat.format(this.menu.maxBatchIp(false)),
                            EnergyFormat.format(this.menu.effectiveBufferCapacity()),
                            this.menu.effectiveFeeModules()),
                    // 库总览：槽位不再画数字，总量信息在这里给全
                    Component.translatable("gui.akaishi.item_lib.summary",
                            Integer.toString(this.menu.visibleEntryCount()),
                            EnergyFormat.format(this.menu.visibleItemCount()))), mouseX, mouseY);
            return;
        }
        super.renderTooltip(gui, mouseX, mouseY);
    }
}
