package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiItemPortBlockEntity;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 储存无线输入/输出口界面（输入口/输出口共用，两页：运行 / 远程绑定）。
 * 页面由顶部按钮本地互斥切换（互不重叠）；解绑走服务端按钮，绑定走 C2S 动作包。
 * 数据来自 {@link AkaishiItemPortMenu} 的数据槽（方向/绑定态/上次搬运/绑定身份短号）。
 * 198 高布局：标题 y=6 与切页按钮 y=16 错开，内容区 y=36 起排布。
 * <p>
 * 运行页右列（x=110..168）是过滤网：标题 y=36、3×3 <b>真槽位</b> y=48..106、提示 y=108，
 * 左列文字统一裁剪到 96 宽让位。槽位取放全走原版点击管线，格内物品图标由原版槽渲染正常绘制
 * （本屏只补画槽框），物品名走原版悬浮文本，操作口径由本屏提示补齐。
 */
public class AkaishiItemPortScreen extends AbstractContainerScreen<AkaishiItemPortMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_GREEN = 0xFF2E7D32;
    private static final int TEXT_RED = 0xFFB03030;
    /** 信息态（合法但需说明，如面朝物品管道）：中性偏蓝，与红字错误明确区分 */
    private static final int TEXT_INFO = 0xFF2F6385;

    // 切页按钮（53×12，两页并排，y=16 避开标题）
    private static final int TAB_W = 53;
    private static final int TAB_H = 12;
    private static final int TAB_Y = 16;
    private static final int TAB_STEP = 55;

    // 解绑按钮（运行页）
    private static final int UNBIND_X = 8;
    private static final int UNBIND_Y = 76;
    private static final int UNBIND_W = 44;
    private static final int UNBIND_H = 12;

    // 运行页过滤网版式：几何全部取自菜单槽位定义（两处同源，自绘框与可点击区绝不会错位）
    private static final int FILTER_X = AkaishiItemPortMenu.FILTER_X;
    private static final int FILTER_Y = AkaishiItemPortMenu.FILTER_Y;
    /** 网格外框宽（含格间距） */
    private static final int FILTER_W = AkaishiItemPortMenu.FILTER_W;
    /** 过滤网标题 y（与左侧方向行同排） */
    private static final int FILTER_TITLE_Y = 36;
    /** 「空 = 全部通过」提示 y（网格正下方，止于 y≈117，不压背包） */
    private static final int FILTER_HINT_Y = FILTER_Y + FILTER_W + 2;
    /** 左列文字裁剪宽：给右侧过滤网列让位 */
    private static final int RUN_TEXT_W = 96;
    /** 上次搬运件数 y：下移到解绑按钮之下，让出右侧过滤网列 */
    private static final int LAST_MOVED_Y = 92;
    /**
     * 搬运失败原因 y（与旧「赤能源不足」告警同一格，只换文案来源；止于 y≈113，不压背包 y=124）。
     * <p>
     * 左列可用宽 {@link #RUN_TEXT_W}=96px（右列 x≥110 是过滤网），故所有原因文案按
     * <b>「一眼知道去改什么」的短句</b>写（≤8 个汉字 ≈ 72px），不靠截断。
     * <p>
     * 同格也承载<b>信息态</b>（{@code REASON_PIPE} 面朝物品管道，非错误）：用中性色 {@link #TEXT_INFO}
     * 与红字错误区分，完整说明走悬停提示（见 {@link #reasonHovered}）。
     */
    private static final int REASON_Y = 104;

    // 绑定页布局：状态两行 + 最多 6 行候选终端（行高 11，止于 y≈122，背包槽框顶边自 123）
    // 行内容 = 「[已绑定] 名称 · 短号」：坐标进悬停提示（整行宽 158 装不下 名称+短号+坐标，
    // 见本轮版式预算；按约定"放不下就重排、不压缩字号"）
    private static final int BIND_STATUS_Y = 36;
    private static final int BIND_LIST_Y = 58;
    private static final int BIND_ROW_H = 11;
    private static final int BIND_LIST_W = 160;
    private static final int BIND_ROWS = 6;

    /** 当前页面：0=运行，1=远程绑定 */
    private int currentPage;
    /** 已下发给菜单的过滤槽激活态（菜单默认运行页激活，故初值 true） */
    private boolean filterSlotsApplied = true;

    public AkaishiItemPortScreen(AkaishiItemPortMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        for (int i = 0; i < 2; i++) {
            GuiWidgets.button(gui, x + 8 + i * TAB_STEP, y + TAB_Y, TAB_W, TAB_H);
        }
        // 绑定页：候选行悬停高亮（行高仅 11，靠底色区分行）
        if (currentPage == 1) {
            int row = bindRowAt(mouseX, mouseY);
            if (row >= 0 && row < this.menu.bindingEntries().size()) {
                gui.fill(x + 8, y + BIND_LIST_Y + row * BIND_ROW_H - 1, x + 168,
                        y + BIND_LIST_Y + row * BIND_ROW_H + BIND_ROW_H - 1, 0x30FFFFFF);
            }
        } else {
            renderFilterGrid(gui);
        }
    }

    /**
     * 运行页过滤网：3×3 真槽位（取放由原版点击管线负责，本屏不拦点击）。
     * 格内物品图标与堆数由原版槽渲染绘制，本屏只补画槽框（{@code renderBg} 先于槽渲染，不会压住物品）。
     */
    private void renderFilterGrid(GuiGraphics gui) {
        for (int slot = 0; slot < AkaishiItemPortBlockEntity.FILTER_SLOTS; slot++) {
            int sx = this.leftPos + AkaishiItemPortMenu.filterSlotX(slot);
            int sy = this.topPos + AkaishiItemPortMenu.filterSlotY(slot);
            GuiWidgets.slotBox(gui, sx, sy);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        drawTabLabel(gui, 8, "gui.akaishi.item_port.tab.run", currentPage == 0);
        drawTabLabel(gui, 8 + TAB_STEP, "gui.akaishi.item_port.tab.bind", currentPage == 1);
        if (currentPage == 0) {
            renderRunPage(gui);
        } else {
            renderBindPage(gui);
        }
    }

    /** 页1：运行（方向 / 已绑定终端 / 绑定身份 / 解绑 / 上次搬运 + 失败原因 + 过滤网标题与提示） */
    private void renderRunPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        boolean bound = menu.isBound();
        // 左列文字统一按 RUN_TEXT_W 裁剪：右列 x≥110 留给过滤网，长绑定标签不得侵入
        // 朝向与实际一致：读本地方块状态的 FACING（无需协议）；文案格式「方向：输入口 · 朝下」
        gui.drawString(this.font, this.font.plainSubstrByWidth(Component.translatable(
                "gui.akaishi.item_port.direction",
                Component.translatable(menu.isOutput()
                        ? "gui.akaishi.item_port.output" : "gui.akaishi.item_port.input"),
                facingText()).getString(),
                RUN_TEXT_W), x + 8, y + 36, TEXT, false);
        gui.drawString(this.font, this.font.plainSubstrByWidth(Component.translatable(
                        "gui.akaishi.item_port.bound_to", bound ? boundText() : "----").getString(),
                RUN_TEXT_W), x + 8, y + 48, bound ? TEXT : TEXT_DIM, false);
        gui.drawString(this.font, this.font.plainSubstrByWidth(Component.translatable(
                        "gui.akaishi.item_port.identity",
                        bound ? String.format("%08X", menu.identityHash()) : "----").getString(),
                RUN_TEXT_W), x + 8, y + 60, bound ? TEXT : TEXT_DIM, false);
        if (bound) {
            GuiWidgets.button(gui, x + UNBIND_X, y + UNBIND_Y, UNBIND_W, UNBIND_H);
            gui.drawString(this.font, Component.translatable("gui.akaishi.item_port.unbind"),
                    x + UNBIND_X + 6, y + UNBIND_Y + 2, TEXT, false);
        }
        // 上次搬运件数（运行页实时刷新；解绑后归零）：下移让出右侧过滤网列
        gui.drawString(this.font, Component.translatable("gui.akaishi.item_port.last_moved", menu.lastMoved()),
                x + 8, y + LAST_MOVED_Y, TEXT_DIM, false);
        // 搬运失败原因（红字）与信息态（中性色）：十类错误显式提示 + 面朝物品管道的合法说明
        // （面朝物品管道不是错误 ⇒ 不报红，避免玩家误以为是配置问题）
        // 显示谁由服务端定（DATA_REASON）：外部通路（管道/第三方）的失败带有效期，窗口内优先于信息态，
        // 故这里"面朝物品管道"也可能被顶替成真实阻塞原因 —— 界面只负责按码分色渲染
        int reasonCode = menu.transferReason();
        Component reasonText = reasonText(reasonCode);
        if (reasonText != null) {
            gui.drawString(this.font, this.font.plainSubstrByWidth(reasonText.getString(), RUN_TEXT_W),
                    x + 8, y + REASON_Y,
                    reasonCode == AkaishiItemPortBlockEntity.REASON_PIPE ? TEXT_INFO : TEXT_RED, false);
        }
        // 右列过滤网：标题（网格本体在 renderBg 自绘）+ 「空 = 全部通过」提示
        gui.drawString(this.font, Component.translatable("gui.akaishi.item_port.filter"),
                x + FILTER_X, y + FILTER_TITLE_Y, TEXT, false);
        gui.drawString(this.font, this.font.plainSubstrByWidth(
                        Component.translatable("gui.akaishi.item_port.filter.any").getString(), FILTER_W),
                x + FILTER_X, y + FILTER_HINT_Y, TEXT_DIM, false);
    }

    /**
     * 失败原因 / 信息态文案（{@code AkaishiItemPortBlockEntity.REASON_*}；{@code REASON_NONE} 返回 null）。
     * <p>
     * {@code REASON_PERMISSION} 按本口方向选文案（输入口缺「存入」、输出口缺「取出」）—— 只用一个原因码、
     * 文案由界面按 {@link AkaishiItemPortMenu#isOutput()} 决定，避免为方向各占一个码。
     * <p>
     * {@code REASON_NO_CONTAINER} 交由 {@link #noContainerText()} 细分为空气 / 其它方块；
     * {@code REASON_PIPE} 为<b>信息态</b>（面朝物品管道），文案中性、由 {@link #renderRunPage} 用中性色绘制。
     * <p>
     * 两个过滤码（{@code REASON_FILTER_MISMATCH} = 塞进来的物品不在过滤网内 /
     * {@code REASON_NO_MATCHING_STOCK} = 终端内没有匹配过滤网的物品）同属红字错误，
     * 且<b>只可能在过滤网已配置时出现</b>（全空 = 全通 ⇒ 不产生任何过滤提示）。
     */
    private Component reasonText(int reason) {
        return switch (reason) {
            case AkaishiItemPortBlockEntity.REASON_NO_TARGET ->
                    Component.translatable("gui.akaishi.item_port.reason.no_target");
            case AkaishiItemPortBlockEntity.REASON_NOT_FORMED ->
                    Component.translatable("gui.akaishi.item_port.reason.not_formed");
            case AkaishiItemPortBlockEntity.REASON_NO_UNITS ->
                    Component.translatable("gui.akaishi.item_port.reason.no_units");
            case AkaishiItemPortBlockEntity.REASON_ENERGY_SHORT ->
                    Component.translatable("gui.akaishi.item_port.energy_short");
            case AkaishiItemPortBlockEntity.REASON_PERMISSION -> Component.translatable(menu.isOutput()
                    ? "gui.akaishi.item_port.reason.permission.extract"
                    : "gui.akaishi.item_port.reason.permission.inject");
            case AkaishiItemPortBlockEntity.REASON_NO_CONTAINER -> noContainerText();
            case AkaishiItemPortBlockEntity.REASON_TERMINAL_FULL ->
                    Component.translatable("gui.akaishi.item_port.reason.terminal_full");
            case AkaishiItemPortBlockEntity.REASON_TARGET_FULL ->
                    Component.translatable("gui.akaishi.item_port.reason.target_full");
            case AkaishiItemPortBlockEntity.REASON_PIPE ->
                    Component.translatable("gui.akaishi.item_port.reason.pipe");
            case AkaishiItemPortBlockEntity.REASON_FILTER_MISMATCH ->
                    Component.translatable("gui.akaishi.item_port.reason.filter_mismatch");
            case AkaishiItemPortBlockEntity.REASON_NO_MATCHING_STOCK ->
                    Component.translatable("gui.akaishi.item_port.reason.filter_no_match");
            default -> null;
        };
    }

    /**
     * 「面朝处没有容器」的细分文案：读<b>本地方块状态</b>区分"空气"与"其它方块"
     * （液体/能量管道、机器等非物品容器），并带上面朝方块的显示名，玩家一眼知道该换成什么。
     * <p>
     * 物品管道不走这里：面朝物品管道是合法信息态（{@code REASON_PIPE}，中性色）。
     * 读不到方块状态时退回通用文案（不显示裸 key）。
     */
    private Component noContainerText() {
        BlockState state = menu.facingState();
        if (state == null) {
            return Component.translatable("gui.akaishi.item_port.reason.no_container");
        }
        if (state.isAir()) {
            return Component.translatable("gui.akaishi.item_port.reason.facing_air");
        }
        return Component.translatable("gui.akaishi.item_port.reason.facing_block", state.getBlock().getName());
    }

    /** 朝向文案（读不到显示 {@code ----}）：六向键随双语，避免裸 key */
    private String facingText() {
        Direction facing = menu.facing();
        return facing == null ? "----" : Component.translatable(facingKey(facing)).getString();
    }

    private static String facingKey(Direction facing) {
        return switch (facing) {
            case UP -> "gui.akaishi.item_port.facing.up";
            case DOWN -> "gui.akaishi.item_port.facing.down";
            case NORTH -> "gui.akaishi.item_port.facing.north";
            case SOUTH -> "gui.akaishi.item_port.facing.south";
            case WEST -> "gui.akaishi.item_port.facing.west";
            case EAST -> "gui.akaishi.item_port.facing.east";
        };
    }

    /** 已绑定目标一行文本：「名称 · 短号」（位置放不下，改由悬停提示给出） */
    private String boundText() {
        AkaishiItemPortBindingSync.BoundTarget target = menu.boundTarget();
        return target == null ? "----" : rowName(target.name()) + " · " + target.shortId();
    }

    /** 名称兜底：空名（读不到方块实体 / 旧档残留）显示为「未知终端」，不显示裸 key */
    private String rowName(Component name) {
        return name == null || name.getString().isEmpty()
                ? Component.translatable("gui.akaishi.item_port.unknown_terminal").getString()
                : name.getString();
    }

    /**
     * 页2：远程绑定（列出对你有「布局」权限、已成型且在线的物品终端，点一行即绑定，无需身份卡）。
     * 绑定身份 = 你本人，口每 {@code PERIOD} tick 按该身份复核方向权限（输入口=存入 / 输出口=取出）。
     * <p>
     * 行内容 =「[已绑定] 名称 · 短号」；名称优先自定义名、其次默认显示名（微缩芯片为族名），
     * 短号用于区分同名终端。坐标（维度 x,y,z）改由<b>悬停提示</b>给出：整行宽 158 装不下三段，
     * 按约定重排到提示里而不是压缩字号。选中行与"已绑定"判定一律按 <b>终端 ID</b> 比对。
     */
    private void renderBindPage(GuiGraphics gui) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        int x = 0;
        int y = 0;
        gui.drawString(this.font, this.font.plainSubstrByWidth(Component.translatable(
                        "gui.akaishi.item_port.bound_to", menu.isBound() ? boundText() : "----").getString(),
                BIND_LIST_W), x + 8, y + BIND_STATUS_Y, TEXT, false);
        gui.drawString(this.font, this.font.plainSubstrByWidth(bindStateText().getString(), BIND_LIST_W),
                x + 8, y + BIND_STATUS_Y + 10, bindStateColor(), false);
        List<AkaishiItemPortBindingSync.Entry> entries = menu.bindingEntries();
        if (entries.isEmpty()) {
            gui.drawString(this.font, this.font.plainSubstrByWidth(
                            Component.translatable("gui.akaishi.item_port.no_terminal").getString(), BIND_LIST_W),
                    x + 8, y + BIND_LIST_Y, TEXT_DIM, false);
            return;
        }
        for (int i = 0; i < entries.size() && i < BIND_ROWS; i++) {
            AkaishiItemPortBindingSync.Entry entry = entries.get(i);
            boolean selected = entry.terminalId().equals(menu.boundTargetId());
            String text = Component.translatable("gui.akaishi.item_port.entry",
                    rowName(entry.name()), entry.shortId()).getString();
            int rowY = y + BIND_LIST_Y + i * BIND_ROW_H;
            // 选中行：标记在**行首**，终端信息接在其后 —— 避免长文字与行尾标记挤在一起
            int labelX = x + 10;
            if (selected) {
                Component marker = Component.translatable("gui.akaishi.item_port.selected");
                gui.drawString(this.font, marker, labelX, rowY, TEXT_GREEN, false);
                labelX += this.font.width(marker) + 4;
            }
            gui.drawString(this.font, this.font.plainSubstrByWidth(text, 158 - (labelX - x - 10)),
                    labelX, rowY, selected ? TEXT_GREEN : TEXT, false);
        }
    }

    /**
     * 绑定态文案：未绑定 / 已绑定且目标可用 / 已绑定但目标已失效。
     * <p>
     * 最后一类是本轮重点：绑定关系是落盘的，终端被拆或坍缩后再拆芯片时绑定仍在，
     * 界面必须照实说"目标已失效"，不能把缓存坐标画成一条正常绑定（幽灵行）。
     */
    private Component bindStateText() {
        AkaishiItemPortBindingSync.BoundTarget target = menu.boundTarget();
        if (target == null) {
            return Component.translatable("gui.akaishi.item_port.state.unbound");
        }
        return Component.translatable(target.live()
                ? "gui.akaishi.item_port.state.bound" : "gui.akaishi.item_port.state.invalid");
    }

    private int bindStateColor() {
        AkaishiItemPortBindingSync.BoundTarget target = menu.boundTarget();
        return target != null && target.live() ? TEXT_GREEN : TEXT_RED;
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

    private void drawTabLabel(GuiGraphics gui, int x, String key, boolean active) {
        // renderLabels 已 translate(leftPos,topPos)，x 为 GUI 相对坐标
        Component text = Component.translatable(key);
        gui.drawString(this.font, text, x + (TAB_W - this.font.width(text)) / 2,
                TAB_Y + 2, active ? TEXT : TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        syncFilterSlots();
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        // 1.20.1 原版 render 不画悬浮文本，必须由子类显式补这一趟（背包 / 快捷栏 / 过滤槽物品名）
        List<AkaishiItemPortBindingSync.Entry> entries = menu.bindingEntries();
        int row = currentPage == 1 ? bindRowAt(mouseX, mouseY) : -1;
        if (row >= 0 && row < entries.size()) {
            // 绑定页：候选行悬停提示（操作说明 + 权限口径 + 归属者 + 位置；行内装不下的信息都在这里）
            AkaishiItemPortBindingSync.Entry entry = entries.get(row);
            gui.renderComponentTooltip(this.font, List.of(
                    Component.translatable("gui.akaishi.item_port.bind_row.tip"),
                    Component.translatable("gui.akaishi.item_port.bind_row.dir"),
                    Component.translatable("gui.akaishi.item_port.bind_row.owner",
                            entry.ownerName().isEmpty() ? "----" : entry.ownerName()),
                    Component.translatable("gui.akaishi.item_port.bind_row.pos",
                            AkaishiItemPortBindingSync.label(entry.dimension(), entry.pos()))),
                    mouseX, mouseY);
        } else if (currentPage == 1 && menu.boundTarget() != null && bindStatusHovered(mouseX, mouseY)) {
            // 绑定页：状态行悬停 —— 已绑定终端的位置（行内只放名称与短号，位置在此给出）
            renderBoundPosTooltip(gui, mouseX, mouseY);
        } else if (currentPage == 0 && reasonTooltip() != null && reasonHovered(mouseX, mouseY)) {
            // 运行页原因行悬停：行内只放短句（左列 96px 装不下完整说明），解释放到提示里
            gui.renderComponentTooltip(this.font, List.of(reasonTooltip()), mouseX, mouseY);
        } else if (currentPage == 0 && hoveredFilterSlot() >= 0 && this.menu.getCarried().isEmpty()) {
            renderFilterTooltip(gui, mouseX, mouseY);
        } else {
            this.renderTooltip(gui, mouseX, mouseY);
        }
    }

    /** 鼠标是否停在绑定页的状态两行上（名称行 + 绑定态行） */
    private boolean bindStatusHovered(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + 8 && mouseX < this.leftPos + 168
                && mouseY >= this.topPos + BIND_STATUS_Y - 1
                && mouseY < this.topPos + BIND_STATUS_Y + 19;
    }

    /** 鼠标是否停在运行页的原因 / 信息行上（左列 96px 宽、9px 高的一格） */
    private boolean reasonHovered(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + 8 && mouseX < this.leftPos + 8 + RUN_TEXT_W
                && mouseY >= this.topPos + REASON_Y && mouseY < this.topPos + REASON_Y + 9;
    }

    /**
     * 原因行的悬停说明：只有"行内短句讲不清"的三类才有 ——
     * <ul>
     *   <li>{@code REASON_PIPE}：合法信息态（面朝物品管道），要解释"为何本口的抓取/推出不适用"；</li>
     *   <li>两个过滤码：要解释"只有与 9 格内物品一致的才放行、<b>9 格全空 = 全部通过</b>"，
     *       否则玩家不知道自己是没配过滤网还是配错了；</li>
     *   <li>{@code REASON_ENERGY_SHORT}：要给<b>数字</b>（本批需多少 / 当前缓冲多少），
     *       行内 96px 装不下，只能放悬停里（见 {@link #energyShortTip()}）。</li>
     * </ul>
     * 其余原因（未成型 / 无单元 / 权限 / 各种满）短句已自明，不加提示以免堆叠遮挡。
     */
    private Component reasonTooltip() {
        return switch (menu.transferReason()) {
            case AkaishiItemPortBlockEntity.REASON_PIPE ->
                    Component.translatable("gui.akaishi.item_port.reason.pipe.tip");
            case AkaishiItemPortBlockEntity.REASON_FILTER_MISMATCH,
                 AkaishiItemPortBlockEntity.REASON_NO_MATCHING_STOCK ->
                    Component.translatable("gui.akaishi.item_port.reason.filter.tip");
            case AkaishiItemPortBlockEntity.REASON_ENERGY_SHORT -> energyShortTip();
            default -> null;
        };
    }

    /**
     * 「赤能源不足」的数字提示：本批所需赤能源 + 终端当前缓冲（服务端在收费失败时算好，经追加槽同步）。
     * <p>
     * 两个数字都 ≤0 时不给提示（罕见的兜底态），免得显示成"需 0 / 缓冲 0"误导玩家。
     */
    private Component energyShortTip() {
        int need = menu.feeNeed();
        return need <= 0 ? null : Component.translatable(
                "gui.akaishi.item_port.reason.energy_short.tip", need, menu.feeHave());
    }

    /** 已绑定目标的位置提示（服务端给的「维度 x,y,z」；注册表已清空时显示"位置未知"） */
    private void renderBoundPosTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        AkaishiItemPortBindingSync.BoundTarget target = menu.boundTarget();
        if (target == null) {
            return;
        }
        String pos = target.posText().isEmpty()
                ? Component.translatable("gui.akaishi.item_port.pos_unknown").getString()
                : target.posText();
        gui.renderComponentTooltip(this.font, List.of(
                Component.translatable("gui.akaishi.item_port.bind_row.pos", pos)), mouseX, mouseY);
    }

    /**
     * 过滤槽悬停提示：<b>物品名 + 操作口径合成同一张框</b>。
     * <p>
     * 不能像其它槽那样先 {@code renderTooltip} 再补一层提示 —— 两张框都贴着鼠标画，上层会盖住物品名，
     * 下层的物品 id 等行会从缝里露出来（层级叠加、文字互串）。故本槽单独立一趟，用原版的
     * {@code getTooltipFromContainerItem} 取齐物品名（含高级提示行），再追加操作口径。
     * 无物品时原版本就无提示，只给设置口径。
     */
    private void renderFilterTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        int slot = hoveredFilterSlot();
        if (slot < 0) {
            return;
        }
        List<Component> lines = new ArrayList<>(this.getTooltipFromContainerItem(this.menu.filterSlot(slot)));
        lines.add(Component.translatable(this.menu.filterSlot(slot).isEmpty()
                ? "gui.akaishi.item_port.filter.tip.empty"
                : "gui.akaishi.item_port.filter.tip.modify"));
        gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /** 鼠标所在过滤槽（非过滤槽返回 -1）；直接用原版悬停槽判定，命中区与槽位高亮完全一致 */
    private int hoveredFilterSlot() {
        return this.hoveredSlot instanceof ItemPortFilterSlot slot ? slot.filterIndex() : -1;
    }

    /** 过滤槽只在运行页激活：绑定页隐藏，否则那 9 个看不见的格子仍会被点击命中（仅在状态变化时下发） */
    private void syncFilterSlots() {
        boolean active = currentPage == 0;
        if (active != this.filterSlotsApplied) {
            this.filterSlotsApplied = active;
            this.menu.setFilterSlotsActive(active);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 过滤槽已是 menu.slots 里的真槽位：取放一律交给原版点击管线（放 1 个 / 取回过滤物），本屏不再拦截
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
            // 绑定页：点候选行 = 远程绑定到该终端（解绑仍在运行页按钮）
            if (currentPage == 1) {
                int row = bindRowAt(mouseX, mouseY);
                List<AkaishiItemPortBindingSync.Entry> entries = menu.bindingEntries();
                if (row >= 0 && row < entries.size()) {
                    AkaishiItemPortBindingSync.Entry entry = entries.get(row);
                    AkaishiItemPortBindingSync.sendAction(this.menu.containerId,
                            AkaishiItemPortBindingSync.ACTION_BIND, entry.terminalId());
                    return true;
                }
            }
            // 解绑按钮（运行页，服务端执行）
            if (currentPage == 0 && menu.isBound()
                    && isIn(this.leftPos + UNBIND_X, this.topPos + UNBIND_Y, UNBIND_W, UNBIND_H, mouseX, mouseY)) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                        AkaishiItemPortMenu.BTN_UNBIND);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isIn(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
