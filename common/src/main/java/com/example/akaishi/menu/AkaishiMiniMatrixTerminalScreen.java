package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiMiniMatrixUpgradeType;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 微缩矩阵终端界面：四页互斥（芯片列表 / 总览 / 加工 / 安全认证）。
 * <p>
 * 版式沿用 176×198 终端贴图（切页按钮 y=16、内容区 y=28 起、玩家背包 y=124）；
 * 安全页整页复用 {@link SecurityPage}（与无线/物品终端同一套版式与交互）。
 * <p>
 * 页面数据全是服务端快照（{@link AkaishiMiniMatrixSync} / {@link AkaishiTerminalSecuritySync}），
 * 界面本身不持有任何权威数据；升级槽内容走原版槽位同步。
 * <p>
 * <b>加工页搜索在客户端做</b>：服务端只下发一次"可合成物目录"，过滤用与库页同一套
 * {@code ItemTerminalSearch}（专用服务器上 {@code getHoverName()} 会退化成翻译键，
 * 中文/模组名只有客户端滤得对）。
 */
public class AkaishiMiniMatrixTerminalScreen extends AbstractContainerScreen<AkaishiMiniMatrixTerminalMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");
    private static final int TEXT = 0xFF3F3F3F;
    private static final int TEXT_DIM = 0xFF707070;
    private static final int TEXT_RED = 0xFFB03030;
    private static final int TEXT_GREEN = 0xFF2E7D32;

    // 切页按钮（32×12，四个并排，y=16 避开标题；176 面板按 34 步进）
    private static final int TAB_W = 32;
    private static final int TAB_H = 12;
    private static final int TAB_Y = 16;
    private static final int[] TAB_X = {8, 42, 76, 110};
    private static final String[] TAB_KEY = {
            "gui.akaishi.matrix.tab.chips",
            "gui.akaishi.matrix.tab.overview",
            "gui.akaishi.matrix.tab.craft",
            "gui.akaishi.matrix.tab.security"};

    /**
     * 左列（面板左侧新增的一竖列）：三个「把玩家送到那枚芯片自己的界面」的页签，
     * 以及同族多于一枚时的翻页。
     * <p>
     * <b>与主页签是两套东西，互不影响</b>：主页签切的是本界面内部的页（芯片/总览/加工/安全）；
     * 左列页签点完就离开本界面（转到芯片方块自己的界面），因此左列没有"当前页"概念、不做选中态，
     * 也不会与主页签的选中态打架。点击区上，左列占面板左侧 x&lt;0 的 48px，
     * 面板与全部既有控件都在 x≥0，两者零重叠。
     */
    private static final int LEFT_COL_W = 48;
    private static final int LEFT_TAB_X = 4;
    private static final int LEFT_TAB_W = 44;
    private static final int LEFT_TAB_H = 14;
    /** 三个页签的 y（间隔 4px）：赤能源 / 生命能量 / 储存 */
    private static final int[] LEFT_TAB_Y = {16, 34, 52};
    /** 翻页控件行 y（页签下方 4px；只在悬停的那一族有 2 枚以上时出现） */
    private static final int LEFT_PAGER_Y = 70;
    private static final int LEFT_PAGER_BTN_W = 10;
    private static final int LEFT_PAGER_TEXT_W = 16;
    /** 三个页签对应的芯片族 id（与 miniature 适配器登记的族 id 一致） */
    private static final ResourceLocation[] LEFT_FAMILY = {
            new ResourceLocation(AkaishiMod.MOD_ID, "chishi_wireless_terminal"),
            new ResourceLocation(AkaishiMod.MOD_ID, "life_wireless_terminal"),
            new ResourceLocation(AkaishiMod.MOD_ID, "item_terminal")};
    private static final String[] LEFT_TAB_KEY = {
            "gui.akaishi.matrix.side.red",
            "gui.akaishi.matrix.side.life",
            "gui.akaishi.matrix.side.store"};
    /**
     * 左列各族"下次打开第几枚"：客户端会话记忆（静态，跨界面重开保持），
     * 使同族多枚时的翻页有意义（否则每次回来都从第 1 枚重新开始）。
     */
    private static final int[] SIDE_INDEX = new int[LEFT_TAB_Y.length];

    /** 面板 176，左右各留 8 ⇒ 文本可用宽度 */
    private static final int CONTENT_W = 160;

    // 加工页布局：标题行（标题 + 长搜索框）→ 图标格（8×4，滚轮 + 右侧滚动条）→ 进度条
    // 可用区 y=30..122（背包线 124）：标题 30..44、格区 46..118、条 119..124
    private static final int CRAFT_TITLE_Y = 30;
    /** 搜索框拉长到标题行的剩余宽度（提示文字同时精简，避免长提示溢出压到别处） */
    private static final int CRAFT_SEARCH_X = 56;
    private static final int CRAFT_SEARCH_W = 112;
    private static final int CRAFT_SEARCH_H = 14;
    /**
     * 图标格区：8 列 × 4 行、格距 18（16px 图标 + 2px 间隔），整行滚动。
     * <p>
     * 与储存终端同一套观感（格框 + 图标 + 右侧滚动条）：这里只画图标，物品名与成本走悬停提示。
     */
    private static final int CRAFT_COLS = 8;
    private static final int CRAFT_ROWS = 4;
    private static final int CRAFT_CELL = 18;
    private static final int CRAFT_GRID_X = 12;
    private static final int CRAFT_GRID_Y = 46;
    /** 右侧滚动条（轨道常驻，条目超出一屏才画把柄） */
    private static final int CRAFT_SCROLLBAR_X = 160;
    private static final int CRAFT_SCROLLBAR_W = 6;
    /**
     * 进度条与剩余时间文字：同处最底一行（条 8..108、文字 112..168）。
     * 成本信息（耗时 / 赤能源 / IP）已移入格子悬停 —— 图标格要占满中部，面板排不下常驻信息行。
     */
    private static final int CRAFT_BAR_Y = 119;
    private static final int CRAFT_BAR_H = 5;
    private static final int CRAFT_BAR_W = 100;
    private static final int CRAFT_BAR_TEXT_X = 112;
    private static final int CRAFT_BAR_TEXT_Y = 117;

    // 加工详情页（点格子进入）：贴图 + 合成方式（直接材料格）+ 成本 + 数量 + 开始加工
    private static final int DETAIL_TOP = 30;
    private static final int DETAIL_BTN_H = 12;
    private static final int DETAIL_BACK_X = 8;
    private static final int DETAIL_BACK_W = 32;
    private static final int DETAIL_ICON_X = 46;
    private static final int DETAIL_NAME_X = 68;
    private static final int DETAIL_RECIPE_Y = 52;
    private static final int DETAIL_GRID_X = 9;
    private static final int DETAIL_GRID_Y = 62;
    private static final int DETAIL_COLS = 8;
    /** 材料格上限（两行 × 8 列） */
    private static final int DETAIL_MATERIALS = 16;
    private static final int DETAIL_COST_Y = 100;
    private static final int DETAIL_OP_Y = 109;
    private static final int DETAIL_AMOUNT_X = 40;
    private static final int DETAIL_AMOUNT_W = 48;
    private static final int DETAIL_START_X = 100;
    private static final int DETAIL_START_W = 68;
    /** 单次加工的数量上限（与 {@code VirtualCraftPlanner.MAX_TARGET_COUNT} 同口径，防手输大数把服务端算爆） */
    private static final int DETAIL_AMOUNT_MAX = 9999;

    // 芯片列表（行高 10px，最多 7 行：y=40..110，溢出提示 y=113..121，距背包线 y=124 仍有 2px）
    private static final int ROW_X = 8;
    private static final int ROW_Y = 40;
    private static final int ROW_H = 10;
    private static final int MAX_ROWS = 7;

    // 总览页：升级信息（只读）+ 运行情况。行高 10 + 起点 40 ⇒ 5 行升级 + 1 行小标题 + 2 行回执
    // 恰好收在 y=119，距背包线 y=124 仍有 5px；未成型提示占 y=30 一行，两者不重叠
    private static final int UPGRADE_LINE_X = 8;
    private static final int UPGRADE_LINE_Y = 40;
    private static final int UPGRADE_LINE_H = 10;

    /** 当前页面（0=芯片，1=总览，2=加工，3=安全） */
    private int currentPage;

    /** 加工页搜索框（原版 EditBox，关掉自带边框以贴合面板配色） */
    private EditBox searchBox;

    /** 本地过滤缓存：目录或查询串变化才重算（每键只做本地过滤，不再往返服务端） */
    private List<ItemStack> craftCatalogSource;
    private String craftQuery = "";
    private List<ItemStack> craftResults = List.of();
    /** 过滤结果里"可直接制作"的条目数（目录前段的前缀太长时按过滤后重算）：前段高亮、其余压暗 */
    private int craftReadyBoundary;
    /** 上次用于分段的目录 readyCount（变化时要重算边界） */
    private int craftReadySource = -1;
    /** 加工图标格的滚动行（滚轮调整；目录或查询串变化时归零） */
    private int craftScrollRow;
    /** 详情页正在看的物品（null = 在加工列表页） */
    private Item detailItem;
    /** 当前详情是哪一次数量请求回来的（用于判断输入框里的数量变了要不要重算） */
    private int detailFetchedAmount = -1;
    /** 上一次见到的账（对象身份）：用于识别"账被目录刷新作废"，从而重取一次 */
    @Nullable
    private AkaishiMatrixCraftSync.PlanView lastSeenPlan;
    /** 详情页的数量输入框 */
    private EditBox amountBox;

    public AkaishiMiniMatrixTerminalScreen(AkaishiMiniMatrixTerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // 高度不变；宽度含左列（面板本身仍是 176，见 renderBg 的 blit 宽度）
        this.imageWidth = AkaishiMiniMatrixTerminalMenu.PANEL_W + LEFT_COL_W;
        this.imageHeight = AkaishiMiniMatrixTerminalMenu.PANEL_H;
    }

    @Override
    protected void init() {
        super.init();
        // leftPos 仍是"面板左边界"，故面板内部坐标与全部槽位一行都不用改；
        // 这里只是把整个界面右移，让"面板 + 左列"作为整体居中。
        this.leftPos = Math.max(LEFT_COL_W, (this.width - this.imageWidth) / 2 + LEFT_COL_W);
        // 已经回到矩阵界面：清掉"跳转来源"，芯片界面上的返回页签随之消失
        MiniMatrixReturn.clear();
        this.searchBox = new EditBox(this.font,
                this.leftPos + CRAFT_SEARCH_X + 4, this.topPos + CRAFT_TITLE_Y + 3,
                CRAFT_SEARCH_W - 8, CRAFT_SEARCH_H - 6,
                Component.translatable("gui.akaishi.matrix.craft.search"));
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xFFFFFFFF);
        this.searchBox.setTextColorUneditable(0xFFFFFFFF);
        this.searchBox.setHint(Component.translatable("gui.akaishi.matrix.craft.search_hint"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setVisible(false);
        addRenderableWidget(this.searchBox);
        this.amountBox = new EditBox(this.font,
                this.leftPos + DETAIL_AMOUNT_X + 4, this.topPos + DETAIL_OP_Y + 3,
                DETAIL_AMOUNT_W - 8, DETAIL_BTN_H - 6,
                Component.translatable("gui.akaishi.matrix.craft.detail.amount"));
        this.amountBox.setBordered(false);
        this.amountBox.setTextColor(0xFFFFFFFF);
        this.amountBox.setTextColorUneditable(0xFFFFFFFF);
        this.amountBox.setHint(Component.literal("1"));
        this.amountBox.setMaxLength(4);
        // 超上限的输入当场夹回：框里显示几件，服务端就得按几件做（可见即可用）
        this.amountBox.setResponder(value -> {
            try {
                if (Integer.parseInt(value.trim()) > DETAIL_AMOUNT_MAX) {
                    this.amountBox.setValue(Integer.toString(DETAIL_AMOUNT_MAX));
                }
            } catch (NumberFormatException ignored) {
                // 空 / 非数字：不打断玩家输入，由 detailAmount() 统一回落
            }
        });
        this.amountBox.setVisible(false);
        addRenderableWidget(this.amountBox);
    }

    /**
     * 搜索框聚焦时吞掉其它按键。
     * <p>
     * 必须拦：原版 {@code EditBox} 只在 {@code charTyped} 里吃字符，{@code keyPressed} 对字母键不消费，
     * 于是按下 e/w 会冒泡给 {@code Screen}，被当成「打开背包 / 前进」直接关掉界面或让玩家走路。
     * ESC / TAB 除外（保留关界面与切焦点）。
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 详情页里 ESC = 退回列表（而不是直接关掉整个界面），并顺带清掉选中态
        if (keyCode == InputConstants.KEY_ESCAPE && detailItem != null) {
            closeCraftDetail();
            return true;
        }
        if (searchBox != null && searchBox.isVisible() && searchBox.isFocused()
                && keyCode != InputConstants.KEY_ESCAPE && keyCode != InputConstants.KEY_TAB) {
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (amountBox != null && amountBox.isVisible() && amountBox.isFocused()) {
            if (keyCode == InputConstants.KEY_RETURN) {
                refreshDetailPlan();
                return true;
            }
            if (keyCode != InputConstants.KEY_ESCAPE && keyCode != InputConstants.KEY_TAB) {
                amountBox.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 数量输入框的软节流：每 tick 最多发一次"按新数量重算"的请求。
     * <p>
     * 不这样做就得在按键回调里发包（每敲一个数字一次），也不必要求玩家先敲回车。
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        refreshDetailPlan();
    }

    /** 详情页数量与"已取回的数量"不一致时，向服务端重取一次账（按新数量） */
    private void refreshDetailPlan() {
        if (detailItem == null) {
            return;
        }
        // 目录刷新会把账作废（{@code acceptCraftCatalog} 清 craftPlan）：检测到"原本有账、现在没了"
        // 就重取一次，否则详情页会一直停在"正在规划…"。用对象身份判断，避免每 tick 空发请求。
        AkaishiMatrixCraftSync.PlanView plan = menu.craftPlan();
        if (plan != lastSeenPlan) {
            lastSeenPlan = plan;
            if (plan == null) {
                detailFetchedAmount = -1;
            }
        }
        int amount = detailAmount();
        if (amount == detailFetchedAmount) {
            return;
        }
        detailFetchedAmount = amount;
        AkaishiMatrixCraftSync.sendAction(menu.containerId, AkaishiMatrixCraftSync.ACTION_SELECT,
                craftQuery, new ItemStack(detailItem, amount));
    }

    /** 详情页当前数量（输入框非法值一律回落到 1） */
    private int detailAmount() {
        if (amountBox == null) {
            return 1;
        }
        String text = amountBox.getValue().trim();
        if (text.isEmpty()) {
            return 1;
        }
        try {
            return Math.max(1, Math.min(Integer.parseInt(text), DETAIL_AMOUNT_MAX));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 底图只有 176 宽：imageWidth 现在含左列，整段 blit 会把贴图外的像素拉进来
        gui.blit(TEXTURE, x, y, 0, 0, AkaishiMiniMatrixTerminalMenu.PANEL_W, this.imageHeight);
        renderSideColumn(gui, x, y, mouseX, mouseY);
        for (int i = 0; i < TAB_X.length; i++) {
            GuiWidgets.button(gui, x + TAB_X[i], y + TAB_Y, TAB_W, TAB_H);
        }
        if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_SECURITY) {
            SecurityPage.renderBg(gui, x, y, this.menu, mouseY);
        } else if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT) {
            // 搜索框 / 数量框的凹槽：EditBox 自带边框已关，底色由这里统一画（与面板同色系）
            if (detailItem == null) {
                GuiWidgets.inputWell(gui, x + CRAFT_SEARCH_X, y + CRAFT_TITLE_Y, CRAFT_SEARCH_W, CRAFT_SEARCH_H);
            } else {
                GuiWidgets.inputWell(gui, x + DETAIL_AMOUNT_X, y + DETAIL_OP_Y, DETAIL_AMOUNT_W, DETAIL_BTN_H);
            }
        }
    }

    /**
     * 左列：三个芯片族页签 + 同族多枚时的翻页。
     * <p>
     * 该族没有已识别芯片 ⇒ 页签按禁用态绘制（压暗 + 灰字），点了也不做任何事：
     * 宁可"看着不可点"，也不给一个点了必然失败的入口。
     * 翻页只在<b>鼠标当前悬停的那一族真有 2 枚以上</b>时出现 —— 同样不做假按钮。
     */
    private void renderSideColumn(GuiGraphics gui, int panelX, int panelY, int mouseX, int mouseY) {
        int colX = panelX - LEFT_COL_W;
        GuiWidgets.panel(gui, colX, panelY, LEFT_COL_W, this.imageHeight);
        for (int i = 0; i < LEFT_TAB_Y.length; i++) {
            GuiWidgets.buttonText(gui, this.font, colX + LEFT_TAB_X, panelY + LEFT_TAB_Y[i],
                    LEFT_TAB_W, LEFT_TAB_H, Component.translatable(LEFT_TAB_KEY[i]), !sideFamily(i).isEmpty());
        }
        int hovered = hoveredSideTab(mouseX, mouseY);
        if (hovered < 0 || sideFamily(hovered).size() < 2) {
            return;
        }
        int rows = sideFamily(hovered).size();
        int y = panelY + LEFT_PAGER_Y;
        GuiWidgets.buttonText(gui, this.font, colX + LEFT_TAB_X, y,
                LEFT_PAGER_BTN_W, LEFT_TAB_H, Component.literal("<"), true);
        GuiWidgets.buttonText(gui, this.font, colX + LEFT_TAB_X + LEFT_PAGER_BTN_W + 3, y,
                LEFT_PAGER_TEXT_W, LEFT_TAB_H, Component.translatable("gui.akaishi.matrix.side.page",
                        Math.floorMod(SIDE_INDEX[hovered], rows) + 1, rows), false);
        GuiWidgets.buttonText(gui, this.font,
                colX + LEFT_TAB_X + LEFT_PAGER_BTN_W + LEFT_PAGER_TEXT_W + 6, y,
                LEFT_PAGER_BTN_W, LEFT_TAB_H, Component.literal(">"), true);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TEXT, false);
        for (int i = 0; i < TAB_X.length; i++) {
            // 标签一律截到页签宽度内：步进 34px / 宽 32px，英文 "Overview"/"Security" 宽 ≥40px，不截就与邻页签互压
            String label = this.font.plainSubstrByWidth(
                    Component.translatable(TAB_KEY[i]).getString(), TAB_W - 2);
            int w = this.font.width(label);
            gui.drawString(this.font, label,
                    TAB_X[i] + (TAB_W - w) / 2, TAB_Y + 2, currentPage == i ? TEXT : TEXT_DIM, false);
        }
        switch (currentPage) {
            case AkaishiMiniMatrixTerminalMenu.PAGE_CHIPS -> renderChipPage(gui);
            case AkaishiMiniMatrixTerminalMenu.PAGE_OVERVIEW -> renderUpgradePage(gui);
            case AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT -> {
                if (detailItem == null) {
                    renderCraftPage(gui);
                } else {
                    renderCraftDetail(gui);
                }
            }
            case AkaishiMiniMatrixTerminalMenu.PAGE_SECURITY ->
                    SecurityPage.renderLabels(gui, this.font, 0, 0, this.menu, TEXT, TEXT_DIM, TEXT_GREEN);
            default -> {
            }
        }
    }

    /** 页1：成型状态 + 芯片逐行读数（行号 / 名称 / 短 ID / 类型 / IP / 能量） */
    private void renderChipPage(GuiGraphics gui) {
        Component status = Component.translatable(menu.isFormed()
                ? "gui.akaishi.matrix.formed" : "gui.akaishi.matrix.unformed");
        Component countText = Component.translatable("gui.akaishi.matrix.chip_count", menu.chipRows().size());
        int countWidth = this.font.width(countText);
        // 两串文字同处 y=30：左侧状态必须让出右侧计数的宽度。
        // 英文 "Structure incomplete (5x5x5 closed box)" 约 210px，不截会与计数重叠并压出面板
        gui.drawString(this.font,
                this.font.plainSubstrByWidth(status.getString(), Math.max(24, CONTENT_W - countWidth - 6)),
                8, 30, menu.isFormed() ? TEXT_GREEN : TEXT_RED, false);
        gui.drawString(this.font, countText, CONTENT_W + 8 - countWidth, 30, TEXT_DIM, false);
        int count = menu.chipRows().size();
        if (count == 0) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.chip_none"),
                    8, ROW_Y + 2, TEXT_DIM, false);
            return;
        }
        int shown = Math.min(count, MAX_ROWS);
        for (int i = 0; i < shown; i++) {
            AkaishiMiniMatrixSync.ChipRow row = menu.chipRows().get(i);
            String line = chipLine(i, row);
            gui.drawString(this.font, this.font.plainSubstrByWidth(line, CONTENT_W),
                    8, ROW_Y + i * ROW_H + 2,
                    row.loaded() ? TEXT : TEXT_DIM, false);
        }
        // 溢出提示：只显示前 MAX_ROWS 行，其余计数告知（避免"少了几枚以为丢了"）
        if (count > shown) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.chip_more", count - shown),
                    8, ROW_Y + shown * ROW_H + 3, TEXT_DIM, false);
        }
    }

    /** 单行只留「编号 + 类型名」；短 ID / 族 id / IP / 能量移到悬停提示与左列页签里 */
    private static String chipLine(int index, AkaishiMiniMatrixSync.ChipRow row) {
        String line = "#" + (index + 1) + " " + row.name();
        if (!row.loaded()) {
            return line + " " + Component.translatable("gui.akaishi.matrix.chip_empty").getString();
        }
        return line;
    }

    /** 页2（总览）：升级信息（只读）+ 运行情况。内腔组件是方块，不在此界面装配 */
    private void renderUpgradePage(GuiGraphics gui) {
        // 成型时不再占行（成型是常态）；未成型必须显式提示，否则玩家会以为组件生效了
        if (!menu.isFormed()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.upgrade.unformed"),
                    8, 30, TEXT_RED, false);
        }
        AkaishiMiniMatrixUpgradeType[] types = AkaishiMiniMatrixUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            AkaishiMiniMatrixUpgradeType type = types[i];
            int count = menu.upgradeCount(type.ordinal());
            String line = Component.translatable("gui.akaishi.matrix.upgrade.level",
                    Component.translatable(type.nameKey()).getString(), count, type.maxCount()).getString();
            gui.drawString(this.font, this.font.plainSubstrByWidth(line, CONTENT_W),
                    UPGRADE_LINE_X, UPGRADE_LINE_Y + i * UPGRADE_LINE_H, count > 0 ? TEXT : TEXT_DIM, false);
        }
        // 运行情况：把"两条链路真的在跑"变成可核对回执（否则玩家无法确认升级是否生效）
        int infoY = UPGRADE_LINE_Y + types.length * UPGRADE_LINE_H;
        gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.overview.runtime"),
                UPGRADE_LINE_X, infoY, TEXT_DIM, false);
        // 直供要同时装「操控 + 联动」：缺任一个都不会送能，故两枚都在位才标绿
        boolean supplying = menu.upgradeCount(AkaishiMiniMatrixUpgradeType.CONTROL.ordinal()) > 0
                && menu.upgradeCount(AkaishiMiniMatrixUpgradeType.LINK.ordinal()) > 0;
        gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.upgrade.push",
                        menu.pushedEnergy()), UPGRADE_LINE_X, infoY + UPGRADE_LINE_H,
                supplying ? TEXT_GREEN : TEXT_DIM, false);
        // 芯片间搬运是矩阵内部总线、不需要任何升级，故只按"本轮是否真搬了"着色
        long transfer = menu.chipTransfer();
        gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.upgrade.chip_transfer", transfer),
                UPGRADE_LINE_X, infoY + 2 * UPGRADE_LINE_H, transfer > 0L ? TEXT_GREEN : TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        // 非安全页时授权槽失活：不渲染、不可点击、Shift 也塞不进
        this.menu.setSecuritySlotActive(currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_SECURITY);
        if (searchBox != null) {
            // 搜索框只在"加工列表页"出现，且切走时主动失焦（否则按键会被看不见的框吞掉）
            boolean craftList = currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT && detailItem == null;
            searchBox.setVisible(craftList);
            if (!craftList) {
                searchBox.setFocused(false);
            }
        }
        if (amountBox != null) {
            // 数量框只在详情页出现
            boolean detail = currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT && detailItem != null;
            amountBox.setVisible(detail);
            if (!detail) {
                amountBox.setFocused(false);
            }
        }
        super.render(gui, mouseX, mouseY, partialTick);
        if (renderSideTooltip(gui, mouseX, mouseY)) {
            return;
        }
        this.renderTooltip(gui, mouseX, mouseY);
        if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CHIPS) {
            renderChipTooltip(gui, mouseX, mouseY);
        } else if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_OVERVIEW) {
            renderUpgradeTooltip(gui, mouseX, mouseY);
        } else if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT) {
            if (detailItem == null) {
                renderCraftTooltip(gui, mouseX, mouseY);
            } else {
                renderCraftDetailTooltip(gui, mouseX, mouseY);
            }
        } else if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_SECURITY) {
            SecurityPage.renderTooltip(gui, this.font, this.leftPos, this.topPos, mouseX, mouseY, this.menu);
        }
    }

    /**
     * 左列悬停：先说清"这一下会打开哪一枚芯片"；该族没有芯片时说明为什么点不动。
     * 返回 true 表示已经出过提示（此时不再走其它提示，避免两框叠在一起）。
     */
    private boolean renderSideTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        int family = hoveredSideTab(mouseX, mouseY);
        if (family < 0) {
            return false;
        }
        List<AkaishiMiniMatrixSync.ChipRow> rows = sideFamily(family);
        List<Component> lines = new ArrayList<>();
        if (rows.isEmpty()) {
            lines.add(Component.translatable("gui.akaishi.matrix.side.none"));
        } else {
            lines.add(Component.translatable("gui.akaishi.matrix.side.open",
                    rows.get(Math.floorMod(SIDE_INDEX[family], rows.size())).name()));
        }
        gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        return true;
    }

    /** 芯片行悬停：完整名称 / 短 ID / 族类型 / IP 与能量（行内文本被截断，靠这里看全） */
    private void renderChipTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        int row = hoveredChipRow(mouseX, mouseY);
        if (row < 0 || row >= menu.chipRows().size()) {
            return;
        }
        AkaishiMiniMatrixSync.ChipRow chip = menu.chipRows().get(row);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(chip.name()));
        if (!chip.loaded()) {
            lines.add(Component.translatable("gui.akaishi.matrix.chip_empty"));
        } else {
            lines.add(Component.translatable("gui.akaishi.matrix.tip.id", chip.shortId()));
            lines.add(Component.translatable("gui.akaishi.matrix.tip.type", chip.type()));
            if (chip.ipCapacity() > 0) {
                lines.add(Component.translatable("gui.akaishi.matrix.tip.ip",
                        chip.ipStored(), chip.ipCapacity()));
            }
            if (chip.energyCapacity() > 0) {
                lines.add(Component.translatable("gui.akaishi.matrix.tip.energy",
                        chip.energyStored(), chip.energyCapacity()));
            }
        }
        gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /**
     * 页3：加工（标题 → 图标格 → 选中 → 再点一次开始）。
     * <p>
     * 列表是<b>客户端本地过滤</b>的结果（目录由服务端一次性下发）；
     * 格子里只画物品图标（与储存终端同一套观感），物品名与成本走悬停提示；
     * 滚轮整行翻页，右侧滚动条指示位置。
     */
    private void renderCraftPage(GuiGraphics gui) {
        if (menu.upgradeCount(AkaishiMiniMatrixUpgradeType.CRAFT.ordinal()) <= 0) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.craft.need_upgrade"),
                    8, CRAFT_TITLE_Y, TEXT_RED, false);
            return;
        }
        // 标题与搜索框同处 y=33：必须让出搜索框起点。英文 "Craftable now" 约 70px，不截会压到搜索框与其中文字上
        gui.drawString(this.font,
                this.font.plainSubstrByWidth(Component.translatable("gui.akaishi.matrix.craft.title").getString(),
                        CRAFT_SEARCH_X - 8 - 4),
                8, CRAFT_TITLE_Y + 3, TEXT, false);
        List<ItemStack> results = craftResults();
        if (results.isEmpty()) {
            // 索引还在服务端分片构建时显示"准备中"，而不是"没有可合成的物品"：
            // 后者会让玩家以为没配方，实际只是首次进世界后还没建完
            gui.drawString(this.font, Component.translatable(menu.craftCatalogBuilding()
                            ? "gui.akaishi.matrix.craft.building"
                            : "gui.akaishi.matrix.craft.empty"),
                    8, CRAFT_GRID_Y + 4, TEXT_DIM, false);
            renderCraftTask(gui);
            return;
        }
        int start = craftScrollStart(results.size());
        int shown = Math.min(results.size() - start, CRAFT_COLS * CRAFT_ROWS);
        for (int i = 0; i < shown; i++) {
            int cellX = CRAFT_GRID_X + (i % CRAFT_COLS) * CRAFT_CELL;
            int cellY = CRAFT_GRID_Y + (i / CRAFT_COLS) * CRAFT_CELL;
            ItemStack stack = results.get(start + i);
            // 可直接制作（目录前段）：格内淡绿底做高亮；缺料的压暗，一眼看出现在做不了
            boolean canMake = (start + i) < craftReadyBoundary;
            // 格框与图标：与背包槽位同一套（slotBox 传入"物品区左上角"，框落在 -1 处）
            GuiWidgets.slotBox(gui, cellX, cellY);
            if (canMake) {
                gui.fill(cellX, cellY, cellX + 16, cellY + 16, 0x4020A020);
            }
            gui.renderItem(stack, cellX, cellY);
            if (!canMake) {
                gui.fill(cellX, cellY, cellX + 16, cellY + 16, 0x99000000);
            }
        }
        drawCraftScrollbar(gui, results.size());
        renderCraftTask(gui);
    }

    // ===== 加工详情页（点格子进入） =====

    /** 打开物品详情页：先按数量 1 取一次账（异步），数量框可直接改 */
    private void openCraftDetail(Item item) {
        detailItem = item;
        detailFetchedAmount = 1;
        if (amountBox != null) {
            amountBox.setValue("1");
            // 全选：直接敲数字即可替换，不必先删掉这个 1
            amountBox.setCursorPosition(0);
            amountBox.setHighlightPos(1);
        }
        AkaishiMatrixCraftSync.sendAction(menu.containerId, AkaishiMatrixCraftSync.ACTION_SELECT,
                craftQuery, new ItemStack(item, 1));
    }

    /** 回到加工列表 */
    private void closeCraftDetail() {
        detailItem = null;
        detailFetchedAmount = -1;
        if (amountBox != null) {
            amountBox.setFocused(false);
        }
    }

    /** 详情页点击：返回 / 开始加工（材料格只做展示，说明走悬停） */
    private boolean handleCraftDetailClick(double mouseX, double mouseY) {
        if (isIn(this.leftPos + DETAIL_BACK_X, this.topPos + DETAIL_TOP,
                DETAIL_BACK_W, DETAIL_BTN_H, mouseX, mouseY)) {
            closeCraftDetail();
            return true;
        }
        if (isIn(this.leftPos + DETAIL_START_X, this.topPos + DETAIL_OP_Y,
                DETAIL_START_W, DETAIL_BTN_H, mouseX, mouseY)) {
            // 数量随输入框走：服务端按该数量重新规划、扣料、入库。
            // 必须与绘制端同判 affordable：置灰的按钮若能点，命中区就与视觉状态不一致（服务端会拒，但体验是坏的）
            AkaishiMatrixCraftSync.PlanView ready = planFor(detailItem, detailAmount());
            if (ready != null && ready.affordable()) {
                AkaishiMatrixCraftSync.sendAction(menu.containerId, AkaishiMatrixCraftSync.ACTION_START,
                        craftQuery, new ItemStack(detailItem, detailAmount()));
                closeCraftDetail();
            }
            return true;
        }
        return false;
    }

    /**
     * 加工详情页：物品贴图 + 合成方式（直接材料格，缺的标红）+ 成本 + 数量 + 开始加工。
     * <p>
     * 只画"最上面那一层"的直接材料（玩家看得懂"要 3 木板 + 2 木棍"）；其余细节走悬停提示。
     */
    private void renderCraftDetail(GuiGraphics gui) {
        GuiWidgets.buttonText(gui, this.font, DETAIL_BACK_X, DETAIL_TOP, DETAIL_BACK_W, DETAIL_BTN_H,
                Component.translatable("gui.akaishi.matrix.craft.detail.back"), true);
        ItemStack icon = new ItemStack(detailItem);
        GuiWidgets.slotBox(gui, DETAIL_ICON_X, DETAIL_TOP);
        gui.renderItem(icon, DETAIL_ICON_X, DETAIL_TOP);
        gui.drawString(this.font,
                this.font.plainSubstrByWidth(icon.getHoverName().getString(), 168 - DETAIL_NAME_X),
                DETAIL_NAME_X, DETAIL_TOP + 4, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.craft.detail.recipe"),
                8, DETAIL_RECIPE_Y, TEXT_DIM, false);
        // 账必须与"当前物品 + 当前件数"都对得上：旧包（上一件、或上一个数量）一律不显示
        AkaishiMatrixCraftSync.PlanView plan = planFor(detailItem, detailFetchedAmount);
        if (plan == null) {
            // "算不出来"与"还没算完"分开说，否则玩家会一直等一个永远不会来的结果
            gui.drawString(this.font, Component.translatable(planUnavailable(detailFetchedAmount)
                            ? "gui.akaishi.matrix.craft.unresolvable"
                            : "message.akaishi.matrix.craft.planning"),
                    8, DETAIL_GRID_Y + 4, TEXT_DIM, false);
            return;
        }
        List<AkaishiMatrixCraftSync.LeafView> materials = plan.leaves();
        for (int i = 0; i < materials.size() && i < DETAIL_MATERIALS; i++) {
            int cellX = DETAIL_GRID_X + (i % DETAIL_COLS) * CRAFT_CELL;
            int cellY = DETAIL_GRID_Y + (i / DETAIL_COLS) * CRAFT_CELL;
            AkaishiMatrixCraftSync.LeafView material = materials.get(i);
            GuiWidgets.slotBox(gui, cellX, cellY);
            gui.renderItem(material.stack(), cellX, cellY);
            GuiWidgets.amountLabel(gui, this.font, cellX, cellY, Long.toString(material.count()));
            if (!material.enough()) {
                gui.fill(cellX, cellY, cellX + 16, cellY + 16, 0x66FF2020); // 缺料：红罩
            }
        }
        // 成本行：三个数都走统一缩写（加上机器能耗/耗时后量级可达百万级，原样输出会撑出面板）。
        // 取整方向按 EnergyFormat 的既有约定：能量是<b>应付量</b>（向上，不误导少备能量）、
        // 材料 IP 是只读占用量（向下，绝不暗示还有余量）、耗时是中性的读数。
        // 行尾按"最硬的拦路条件"替换（保证整行不超出面板宽度）：
        // 缺机台 > 要付生命能量（纯能量配方没有材料，材料 IP 恒为 0）> 材料 IP。
        String seconds = EnergyFormat.format(plan.ticks() / 20L);
        String chishi = EnergyFormat.formatCeil(plan.energy());
        Component costLine;
        if (plan.machineMissing()) {
            costLine = Component.translatable("gui.akaishi.matrix.craft.detail.cost_machine", seconds, chishi);
        } else if (plan.powerMissing()) {
            // 机台在场但拿不到能量（终端缺「操控」/「联动」）：真机加工下机台转不起来，与缺机台同为硬拦路
            costLine = Component.translatable("gui.akaishi.matrix.craft.detail.cost_power", seconds, chishi);
        } else if (plan.lifeEnergy() > 0L) {
            costLine = Component.translatable("gui.akaishi.matrix.craft.detail.cost_life", seconds, chishi,
                    EnergyFormat.formatCeil(plan.lifeEnergy()));
        } else {
            costLine = Component.translatable("gui.akaishi.matrix.craft.detail.cost", seconds, chishi,
                    EnergyFormat.formatFloor(plan.materialIp()));
        }
        // 必须按内容宽度截断：中文模板约 116px 尚可，英文模板（"%s s · energy %s · material IP %s"）本身已约 152px，
        // 再加三个缩写值必然超出 CONTENT_W(160)。原先注释写了"保证不超宽"，但这里其实没有任何宽度约束。
        gui.drawString(this.font, this.font.plainSubstrByWidth(costLine.getString(), CONTENT_W),
                8, DETAIL_COST_Y, plan.affordable() ? TEXT : TEXT_RED, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.matrix.craft.detail.amount"),
                8, DETAIL_OP_Y + 3, TEXT_DIM, false);
        GuiWidgets.buttonText(gui, this.font, DETAIL_START_X, DETAIL_OP_Y, DETAIL_START_W, DETAIL_BTN_H,
                Component.translatable("gui.akaishi.matrix.craft.detail.start"), plan.affordable());
    }

    /**
     * 「这条工序由谁提供」的悬停文本：标题 + 逐条来源。不需要机台（纯原版配方）时给一句说明。
     */
    private List<Component> processLines(AkaishiMatrixCraftSync.PlanView plan) {
        List<Component> lines = new ArrayList<>();
        if (plan.processes().isEmpty()) {
            lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.source.none"));
            return lines;
        }
        lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.source.header"));
        for (AkaishiMatrixCraftSync.ProcessView process : plan.processes()) {
            Component name = processName(process.processId());
            lines.add(switch (process.tier()) {
                case 0 -> Component.translatable("gui.akaishi.matrix.craft.detail.source.own", name);
                case 1 -> Component.translatable("gui.akaishi.matrix.craft.detail.source.declared", name,
                        ownerNames(process));
                default -> Component.translatable("gui.akaishi.matrix.craft.detail.source.generic", name);
            });
        }
        return lines;
    }

    /**
     * 工序显示名：自研族取 {@code gui.akaishi.process.<配方类型 path>}（= 机器族名，玩家认机器不认配方 id），
     * 第三方则直接用配方类型 id。缺键时回落 id —— 绝不显示裸 key。
     */
    private static Component processName(String processId) {
        ResourceLocation id = ResourceLocation.tryParse(processId);
        if (id == null) {
            return Component.literal(processId);
        }
        return Component.translatableWithFallback("gui.akaishi.process." + id.getPath(), processId);
    }

    /** 已声明工序的提供方块名（在客户端翻成当前语言；方块不存在时退回 id 本身） */
    private static String ownerNames(AkaishiMatrixCraftSync.ProcessView process) {
        List<String> names = new ArrayList<>(process.owners().size());
        for (String ownerId : process.owners()) {
            ResourceLocation id = ResourceLocation.tryParse(ownerId);
            Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
            names.add(block == null || block == Blocks.AIR ? ownerId : block.getName().getString());
        }
        return String.join(" / ", names);
    }

    /** 详情页悬停：贴图格 → 物品名；材料格 → 材料名 + 需要数量 + 够/缺 */
    private void renderCraftDetailTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        AkaishiMatrixCraftSync.PlanView plan = planFor(detailItem, detailFetchedAmount);
        if (isIn(this.leftPos + DETAIL_ICON_X, this.topPos + DETAIL_TOP, 16, 16, mouseX, mouseY)) {
            lines.add(new ItemStack(detailItem).getHoverName());
        } else if (plan != null && plan.machineMissing()
                && isIn(this.leftPos + 8, this.topPos + DETAIL_COST_Y, 160, 9, mouseX, mouseY)) {
            // 行尾那三个字（缺机台）说不清要怎么办，悬停给完整说法
            lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.machine_missing"));
        } else if (plan != null && plan.powerMissing()
                && isIn(this.leftPos + 8, this.topPos + DETAIL_COST_Y, 160, 9, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.power_missing"));
        } else if (plan != null && isIn(this.leftPos + 8, this.topPos + DETAIL_RECIPE_Y - 1, CONTENT_W, 10,
                mouseX, mouseY)) {
            // 工序来源：这条订单到底由谁提供（自研机台族 / 第三方已声明到方块 / 第三方粗粒度）
            lines.addAll(processLines(plan));
        } else if (plan != null) {
            int relX = (int) mouseX - this.leftPos - DETAIL_GRID_X;
            int relY = (int) mouseY - this.topPos - DETAIL_GRID_Y;
            if (relX < 0 || relY < 0) {
                return;
            }
            int col = relX / CRAFT_CELL;
            int index = (relY / CRAFT_CELL) * DETAIL_COLS + col;
            List<AkaishiMatrixCraftSync.LeafView> materials = plan.leaves();
            if (col >= DETAIL_COLS || index >= Math.min(materials.size(), DETAIL_MATERIALS)) {
                return;
            }
            AkaishiMatrixCraftSync.LeafView material = materials.get(index);
            lines.add(material.stack().getHoverName());
            lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.material_line", material.count()));
            lines.add(Component.translatable(material.enough()
                    ? "gui.akaishi.matrix.craft.detail.material_ok"
                    : "gui.akaishi.matrix.craft.detail.material_missing"));
        } else {
            return;
        }
        gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /** 右侧滚动条：轨道常驻，条目超出一屏才画把柄（与储存终端同一套观感） */
    private void drawCraftScrollbar(GuiGraphics gui, int size) {
        int trackH = CRAFT_ROWS * CRAFT_CELL;
        GuiWidgets.track(gui, CRAFT_SCROLLBAR_X, CRAFT_GRID_Y, CRAFT_SCROLLBAR_W, trackH);
        int rows = (size + CRAFT_COLS - 1) / CRAFT_COLS;
        if (rows <= CRAFT_ROWS) {
            return;
        }
        int maxRow = rows - CRAFT_ROWS;
        int handleH = Math.max(6, trackH * CRAFT_ROWS / rows);
        int handleY = CRAFT_GRID_Y + (trackH - handleH) * clampedScrollRow(size) / maxRow;
        gui.fill(CRAFT_SCROLLBAR_X + 1, handleY, CRAFT_SCROLLBAR_X + CRAFT_SCROLLBAR_W - 1,
                handleY + handleH, 0xFF9A9A9A);
    }

    /**
     * 加工任务进度：进度条 + 目标 + 剩余秒数。
     * <p>
     * 无任务时整块不画（不留空槽），这是界面上唯一的"正在加工"反馈；
     * 但若上一次是<b>失败</b>结束，那一行改用红字说明原因 —— 失败的任务在服务端已被丢弃，
     * 这里是玩家唯一能看到"为什么停了"的地方。
     */
    private void renderCraftTask(GuiGraphics gui) {
        AkaishiMatrixCraftSync.TaskView task = menu.craftTaskView();
        if (task == null) {
            String fail = menu.craftTaskFail();
            if (fail != null) {
                Component line = Component.translatable("gui.akaishi.matrix.craft.fail", failLine(fail));
                gui.drawString(this.font, this.font.plainSubstrByWidth(line.getString(), CONTENT_W),
                        8, CRAFT_BAR_TEXT_Y, TEXT_RED, false);
            }
            return;
        }
        if (task.type() != AkaishiMatrixCraftSync.TASK_CRAFT || task.totalTicks() <= 0) {
            return;
        }
        GuiWidgets.track(gui, 8, CRAFT_BAR_Y, CRAFT_BAR_W, CRAFT_BAR_H);
        int done = task.totalTicks() - task.remainingTicks();
        int width = CRAFT_BAR_W * Math.max(0, Math.min(done, task.totalTicks())) / task.totalTicks();
        if (width > 0) {
            gui.fill(8, CRAFT_BAR_Y, 8 + width, CRAFT_BAR_Y + CRAFT_BAR_H, 0xFF3A5FA8);
        }
        String line = Component.translatable("gui.akaishi.matrix.craft.running",
                task.target().getHoverName(), (task.remainingTicks() + 19) / 20).getString();
        gui.drawString(this.font, this.font.plainSubstrByWidth(line, 168 - CRAFT_BAR_TEXT_X),
                CRAFT_BAR_TEXT_X, CRAFT_BAR_TEXT_Y, TEXT_DIM, false);
    }

    /**
     * 失败原因代号 → 当前语言文案。
     * <p>用 {@code translatableWithFallback} 而非直接拼 key：将来新增失败原因而语言文件没跟上时，
     * 界面显示的是代号本身（信息不丢），不会出现裸 key 或空白。
     */
    private static Component failLine(String reason) {
        return Component.translatableWithFallback("gui.akaishi.matrix.craft.fail." + reason, reason);
    }

    /**
     * 加工页悬停：结果格子 → 该项的完整详情（名称 + 清单 / 耗时 / 赤能源 / IP + "再点一次开始"）。
     * <p>
     * 格子里只画图标（与储存终端一致），所以物品名与成本都在这里给全。
     */
    private void renderCraftTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        // 底行（进度条那一行）：悬停给"真机加工的真实进度"。机台耗时不归我们算（第三方连速度都读不到），
        // 只有节点数是我们确知的，故这里把它与目标、预估一并给全 —— 常驻那一行放不下这么多字
        AkaishiMatrixCraftSync.TaskView task = menu.craftTaskView();
        if (task != null && task.type() == AkaishiMatrixCraftSync.TASK_CRAFT
                && isIn(8, CRAFT_BAR_TEXT_Y - 1, CONTENT_W, CRAFT_BAR_H + 3, mouseX, mouseY)) {
            gui.renderComponentTooltip(this.font, List.of(
                    task.target().getHoverName(),
                    Component.translatable("gui.akaishi.matrix.craft.tip.steps",
                            Math.max(0, Math.min(task.collected(), task.elapsed())), Math.max(0, task.elapsed())),
                    Component.translatable("gui.akaishi.matrix.craft.tip.eta",
                            (task.remainingTicks() + 19) / 20)), mouseX, mouseY);
            return;
        }
        List<ItemStack> results = craftResults();
        int index = hoveredCraftIndex(mouseX, mouseY, results.size());
        if (index < 0) {
            return;
        }
        ItemStack stack = results.get(index);
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        // 能不能现在做：与格子的高亮/压暗同一判据，避免"看着能做、点下去说缺料"
        lines.add(Component.translatable(index < craftReadyBoundary
                ? "gui.akaishi.matrix.craft.tip.ready"
                : "gui.akaishi.matrix.craft.tip.missing"));
        // 详情（合成方式 / 数量 / 开始加工）都在详情页里，列表页只给入口
        lines.add(Component.translatable("gui.akaishi.matrix.craft.tip.select"));
        gui.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    /** 升级行悬停：该项说明（组件是方块，界面只报数量，这里补足「装几个会怎样」） */
    private void renderUpgradeTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        AkaishiMiniMatrixUpgradeType[] types = AkaishiMiniMatrixUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            int lineY = this.topPos + UPGRADE_LINE_Y + i * UPGRADE_LINE_H;
            if (mouseX >= this.leftPos + UPGRADE_LINE_X && mouseX < this.leftPos + CONTENT_W + 8
                    && mouseY >= lineY && mouseY < lineY + UPGRADE_LINE_H - 1) {
                gui.renderComponentTooltip(this.font,
                        List.of(Component.translatable(types[i].nameKey()),
                                Component.translatable(types[i].hintKey())),
                        mouseX, mouseY);
                return;
            }
        }
    }

    /** 鼠标所在芯片行；不在列表内返回 -1 */
    private int hoveredChipRow(int mouseX, int mouseY) {
        int relY = mouseY - this.topPos - ROW_Y;
        if (mouseX < this.leftPos + ROW_X || mouseX >= this.leftPos + CONTENT_W + 8 || relY < 0) {
            return -1;
        }
        int row = relY / ROW_H;
        return row < MAX_ROWS ? row : -1;
    }

    /**
     * 详情页当前该显示的那份账：<b>物品 + 件数</b>都要对得上，否则返回 null。
     * <p>
     * 只有物品对得上是不够的：玩家把数量从 1 改成 8 时，先前那个"按 1 件算"的包可能后到，
     * 显示就会被永久卡在旧账上（现象：有时显示不正确）。件数是随包回显的请求件数。
     */
    @Nullable
    private AkaishiMatrixCraftSync.PlanView planFor(Item item, int amount) {
        AkaishiMatrixCraftSync.PlanView plan = menu.craftPlan();
        return plan != null && !plan.target().isEmpty() && plan.target().getItem() == item
                && plan.amount() == amount ? plan : null;
    }

    /**
     * 该件数是否"已经算过、但规划不出来"：服务端把空 target 的账连同件数一起回显。
     * <p>
     * 与"还没有回包"必须分开：前者要明说"做不了"，后者才显示"正在规划…"。
     */
    private boolean planUnavailable(int amount) {
        AkaishiMatrixCraftSync.PlanView plan = menu.craftPlan();
        return plan != null && plan.target().isEmpty() && plan.amount() == amount;
    }

    /**
     * 本地过滤后的结果列表：目录 / 查询串 / 可直接制作的边界变化才重算。
     * <p>
     * 与库页同用 {@code ItemTerminalSearch}（名称 + 拼音 + 注册名），
     * 客户端语言环境完整 ⇒ 中文名与模组名都搜得到，且每次按键不再发包。
     */
    private List<ItemStack> craftResults() {
        String query = searchBox == null ? "" : searchBox.getValue();
        List<ItemStack> catalog = menu.craftCatalog();
        int ready = menu.craftReadyCount();
        if (catalog != craftCatalogSource || !query.equals(craftQuery) || ready != craftReadySource) {
            craftCatalogSource = catalog;
            craftQuery = query;
            craftReadySource = ready;
            craftResults = ItemTerminalSearch.filterStacks(catalog, query);
            // 目录是"可直接制作在前"的有序表，过滤保持顺序 ⇒ 结果里的可直接制作项仍是前缀，
            // 但边界数量要按"过滤后还剩几项"重算（不能直接挪用目录的 readyCount）
            Set<Item> readyItems = new HashSet<>();
            for (int i = 0; i < ready && i < catalog.size(); i++) {
                readyItems.add(catalog.get(i).getItem());
            }
            int boundary = 0;
            for (ItemStack stack : craftResults) {
                if (readyItems.contains(stack.getItem())) {
                    boundary++;
                }
            }
            craftReadyBoundary = boundary;
            // 结果变了就回到顶部：否则会停在越界位置，看起来像"格子空了"
            craftScrollRow = 0;
        }
        return craftResults;
    }

    /** 当前滚动行（按结果行数夹紧：结果变少时不会停在越界位置） */
    private int clampedScrollRow(int size) {
        int rows = (size + CRAFT_COLS - 1) / CRAFT_COLS;
        return Math.max(0, Math.min(craftScrollRow, Math.max(0, rows - CRAFT_ROWS)));
    }

    /** 滚动起始下标（整行对齐：格子必须整行滚动，否则会露出半行） */
    private int craftScrollStart(int size) {
        return clampedScrollRow(size) * CRAFT_COLS;
    }

    /** 鼠标下的格子下标（含滚动偏移；不在格区返回 -1） */
    private int hoveredCraftIndex(double mouseX, double mouseY, int size) {
        int relX = (int) mouseX - this.leftPos - CRAFT_GRID_X;
        int relY = (int) mouseY - this.topPos - CRAFT_GRID_Y;
        if (relX < 0 || relY < 0) {
            return -1;
        }
        int col = relX / CRAFT_CELL;
        int row = relY / CRAFT_CELL;
        if (col >= CRAFT_COLS || row >= CRAFT_ROWS) {
            return -1;
        }
        int index = craftScrollStart(size) + row * CRAFT_COLS + col;
        return index < size ? index : -1;
    }

    /**
     * 加工页滚轮翻页（整行滚动）。
     * <p>
     * 面板里只有这一处可滚动，故本页直接消费滚轮，不额外要求"必须悬停在格子上"；
     * 其它页原样交回原版（背包滚动等）。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT && delta != 0.0D) {
            int rows = (craftResults().size() + CRAFT_COLS - 1) / CRAFT_COLS;
            int maxRow = Math.max(0, rows - CRAFT_ROWS);
            if (maxRow > 0) {
                craftScrollRow = Math.max(0, Math.min(maxRow, craftScrollRow - (int) Math.signum(delta)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** 请求可合成物目录（切到加工页 / 重开界面 = 刷新入口；服务端不再按查询串搜索） */
    private void requestCraftCatalog() {
        AkaishiMatrixCraftSync.sendAction(menu.containerId, AkaishiMatrixCraftSync.ACTION_SEARCH,
                "", ItemStack.EMPTY);
    }

    /** 该族当前被识别到的芯片行（左列页签的灰态与翻页都据此判定） */
    private List<AkaishiMiniMatrixSync.ChipRow> sideFamily(int family) {
        String id = LEFT_FAMILY[family].toString();
        List<AkaishiMiniMatrixSync.ChipRow> found = new ArrayList<>();
        for (AkaishiMiniMatrixSync.ChipRow row : menu.chipRows()) {
            if (row.loaded() && id.equals(row.type())) {
                found.add(row);
            }
        }
        return found;
    }

    /** 鼠标下的左列页签序号（无则 -1） */
    private int hoveredSideTab(double mouseX, double mouseY) {
        int colX = this.leftPos - LEFT_COL_W;
        for (int i = 0; i < LEFT_TAB_Y.length; i++) {
            if (isIn(colX + LEFT_TAB_X, this.topPos + LEFT_TAB_Y[i], LEFT_TAB_W, LEFT_TAB_H, mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 把玩家送到第 {@code family} 族的第 {@code index} 枚芯片的界面（同族多枚时按枚数取模）。
     * <p>
     * 只发坐标，由服务端对照"矩阵已识别的芯片"白名单后再打开 —— 客户端不做信任来源。
     */
    private void openChip(int family, int index) {
        List<AkaishiMiniMatrixSync.ChipRow> rows = sideFamily(family);
        if (rows.isEmpty()) {
            return;
        }
        int k = Math.floorMod(index, rows.size());
        SIDE_INDEX[family] = k;
        // 记下"从哪儿跳来的"：芯片界面左侧会据此显示返回页签（服务端仍会校验坐标）
        MiniMatrixReturn.mark(menu.matrixPos());
        AkaishiMatrixCraftSync.sendAction(menu.containerId, AkaishiMatrixCraftSync.ACTION_OPEN_CHIP,
                "", ItemStack.EMPTY, ItemStack.EMPTY, rows.get(k).pos());
    }

    /** 左列点击：页签 = 打开该族当前那一枚，翻页 = 换一枚并打开。返回是否已消费这次点击 */
    private boolean handleSideColumnClick(double mouseX, double mouseY) {
        int colX = this.leftPos - LEFT_COL_W;
        for (int i = 0; i < LEFT_TAB_Y.length; i++) {
            if (isIn(colX + LEFT_TAB_X, this.topPos + LEFT_TAB_Y[i], LEFT_TAB_W, LEFT_TAB_H, mouseX, mouseY)) {
                // 灰态也吞掉点击：避免穿透到下面的控件上
                openChip(i, SIDE_INDEX[i]);
                return true;
            }
        }
        int hovered = hoveredSideTab(mouseX, mouseY);
        if (hovered < 0 || sideFamily(hovered).size() < 2) {
            return false;
        }
        int y = this.topPos + LEFT_PAGER_Y;
        if (isIn(colX + LEFT_TAB_X, y, LEFT_PAGER_BTN_W, LEFT_TAB_H, mouseX, mouseY)) {
            openChip(hovered, SIDE_INDEX[hovered] - 1);
            return true;
        }
        int nextX = colX + LEFT_TAB_X + LEFT_PAGER_BTN_W + LEFT_PAGER_TEXT_W + 6;
        if (isIn(nextX, y, LEFT_PAGER_BTN_W, LEFT_TAB_H, mouseX, mouseY)) {
            openChip(hovered, SIDE_INDEX[hovered] + 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (handleSideColumnClick(mouseX, mouseY)) {
                return true;
            }
            for (int i = 0; i < TAB_X.length; i++) {
                if (isIn(this.leftPos + TAB_X[i], this.topPos + TAB_Y, TAB_W, TAB_H, mouseX, mouseY)) {
                    currentPage = i;
                    // 切到加工页时拉一次目录（配方表变化后的刷新入口）
                    if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT) {
                        requestCraftCatalog();
                    }
                    return true;
                }
            }
            if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT) {
                if (detailItem != null) {
                    if (handleCraftDetailClick(mouseX, mouseY)) {
                        return true;
                    }
                } else {
                    // 点格子 → 进入该物品的详情页（合成方式 / 数量 / 开始加工都在详情页里）
                    List<ItemStack> results = craftResults();
                    int index = hoveredCraftIndex(mouseX, mouseY, results.size());
                    if (index >= 0) {
                        openCraftDetail(results.get(index).getItem());
                        return true;
                    }
                }
            }
            if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_SECURITY
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
