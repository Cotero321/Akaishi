package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

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
 * <p>
 * 本类是<b>调度与控件生命周期宿主</b>：成块的区域渲染/命中判定搬迁到
 * {@link MiniMatrixTerminalSections}（芯片页/总览页/左列）与
 * {@link MiniMatrixCraftList} / {@link MiniMatrixCraftDetail}（加工列表页/详情页），
 * 原方法变一行委托，行为逐位不变。
 */
public class AkaishiMiniMatrixTerminalScreen extends AbstractContainerScreen<AkaishiMiniMatrixTerminalMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_wireless_terminal.png");

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
     * 左列宽度（面板左侧新增的一竖列；布局常量真源见 {@link MiniMatrixTerminalSections}）。
     * <p>
     * <b>与主页签是两套东西，互不影响</b>：主页签切的是本界面内部的页（芯片/总览/加工/安全）；
     * 左列页签点完就离开本界面（转到芯片方块自己的界面），因此左列没有"当前页"概念、不做选中态，
     * 也不会与主页签的选中态打架。点击区上，左列占面板左侧 x&lt;0 的 48px，
     * 面板与全部既有控件都在 x≥0，两者零重叠。
     */
    static final int LEFT_COL_W = 48;

    // ===== 常量别名（真源随迁到渲染协作类；此处仅供 init / renderLabels 使用，方法体零改动） =====
    private static final int TEXT = MiniMatrixTerminalSections.TEXT;
    private static final int TEXT_DIM = MiniMatrixTerminalSections.TEXT_DIM;
    private static final int CRAFT_TITLE_Y = MiniMatrixCraftList.CRAFT_TITLE_Y;
    private static final int CRAFT_SEARCH_X = MiniMatrixCraftList.CRAFT_SEARCH_X;
    private static final int CRAFT_SEARCH_W = MiniMatrixCraftList.CRAFT_SEARCH_W;
    private static final int CRAFT_SEARCH_H = MiniMatrixCraftList.CRAFT_SEARCH_H;
    private static final int DETAIL_AMOUNT_X = MiniMatrixCraftDetail.DETAIL_AMOUNT_X;
    private static final int DETAIL_AMOUNT_W = MiniMatrixCraftDetail.DETAIL_AMOUNT_W;
    private static final int DETAIL_BTN_H = MiniMatrixCraftDetail.DETAIL_BTN_H;
    private static final int DETAIL_OP_Y = MiniMatrixCraftDetail.DETAIL_OP_Y;
    private static final int DETAIL_AMOUNT_MAX = MiniMatrixCraftDetail.DETAIL_AMOUNT_MAX;

    /** 当前页面（0=芯片，1=总览，2=加工，3=安全） */
    private int currentPage;

    /** 加工页搜索框（原版 EditBox，关掉自带边框以贴合面板配色） */
    EditBox searchBox;

    /** 详情页的数量输入框 */
    EditBox amountBox;

    /** 加工列表页 / 详情页协作类（各自持有过滤与账目状态；构造只存引用，不触碰未初始化字段） */
    private final MiniMatrixCraftList craftList = new MiniMatrixCraftList(this);
    private final MiniMatrixCraftDetail craftDetail = new MiniMatrixCraftDetail(this, this.craftList);

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
        if (keyCode == InputConstants.KEY_ESCAPE && craftDetail.hasDetail()) {
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

    /** 详情页数量与"已取回的数量"不一致时，向服务端重取一次账（按新数量）→ 委托详情页协作类 */
    private void refreshDetailPlan() {
        this.craftDetail.refreshDetailPlan();
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
            if (!craftDetail.hasDetail()) {
                GuiWidgets.inputWell(gui, x + CRAFT_SEARCH_X, y + CRAFT_TITLE_Y, CRAFT_SEARCH_W, CRAFT_SEARCH_H);
            } else {
                GuiWidgets.inputWell(gui, x + DETAIL_AMOUNT_X, y + DETAIL_OP_Y, DETAIL_AMOUNT_W, DETAIL_BTN_H);
            }
        }
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
                if (!craftDetail.hasDetail()) {
                    renderCraftPage(gui);
                } else {
                    renderCraftDetail(gui);
                }
            }
            case AkaishiMiniMatrixTerminalMenu.PAGE_SECURITY ->
                    SecurityPage.renderLabels(gui, this.font, 0, 0, this.menu, TEXT, TEXT_DIM, MiniMatrixTerminalSections.TEXT_GREEN);
            default -> {
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        // 非安全页时授权槽失活：不渲染、不可点击、Shift 也塞不进
        this.menu.setSecuritySlotActive(currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_SECURITY);
        if (searchBox != null) {
            // 搜索框只在"加工列表页"出现，且切走时主动失焦（否则按键会被看不见的框吞掉）
            boolean craftList = currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT && !craftDetail.hasDetail();
            searchBox.setVisible(craftList);
            if (!craftList) {
                searchBox.setFocused(false);
            }
        }
        if (amountBox != null) {
            // 数量框只在详情页出现
            boolean detail = currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT && craftDetail.hasDetail();
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
            if (!craftDetail.hasDetail()) {
                renderCraftTooltip(gui, mouseX, mouseY);
            } else {
                renderCraftDetailTooltip(gui, mouseX, mouseY);
            }
        } else if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_SECURITY) {
            SecurityPage.renderTooltip(gui, this.font, this.leftPos, this.topPos, mouseX, mouseY, this.menu);
        }
    }

    /**
     * 加工页滚轮翻页（整行滚动，委托加工列表协作类）。
     * <p>
     * 面板里只有这一处可滚动，故本页直接消费滚轮，不额外要求"必须悬停在格子上"；
     * 其它页原样交回原版（背包滚动等）。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentPage == AkaishiMiniMatrixTerminalMenu.PAGE_CRAFT && this.craftList.scrollBy(delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
                if (craftDetail.hasDetail()) {
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

    // ===== 包私有访问器：渲染协作类（非 Screen 子类）经此访问 protected 成员（JLS 6.6.2） =====

    AkaishiMiniMatrixTerminalMenu menu() {
        return this.menu;
    }

    Font font() {
        return this.font;
    }

    int leftPos() {
        return this.leftPos;
    }

    int topPos() {
        return this.topPos;
    }

    int imageHeight() {
        return this.imageHeight;
    }

    static boolean isIn(int x, int y, int w, int h, double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ===== 一行委托（原方法签名保留，实现搬迁到渲染协作类） =====

    /** 左列渲染 → {@link MiniMatrixTerminalSections} */
    private void renderSideColumn(GuiGraphics gui, int panelX, int panelY, int mouseX, int mouseY) {
        MiniMatrixTerminalSections.renderSideColumn(this, gui, panelX, panelY, mouseX, mouseY);
    }

    /** 页1（芯片列表）渲染 → {@link MiniMatrixTerminalSections} */
    private void renderChipPage(GuiGraphics gui) {
        MiniMatrixTerminalSections.renderChipPage(this, gui);
    }

    /** 页2（总览）渲染 → {@link MiniMatrixTerminalSections} */
    private void renderUpgradePage(GuiGraphics gui) {
        MiniMatrixTerminalSections.renderUpgradePage(this, gui);
    }

    /** 左列悬停提示 → {@link MiniMatrixTerminalSections} */
    private boolean renderSideTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        return MiniMatrixTerminalSections.renderSideTooltip(this, gui, mouseX, mouseY);
    }

    /** 芯片行悬停提示 → {@link MiniMatrixTerminalSections} */
    private void renderChipTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        MiniMatrixTerminalSections.renderChipTooltip(this, gui, mouseX, mouseY);
    }

    /** 升级行悬停提示 → {@link MiniMatrixTerminalSections} */
    private void renderUpgradeTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        MiniMatrixTerminalSections.renderUpgradeTooltip(this, gui, mouseX, mouseY);
    }

    /** 左列点击 → {@link MiniMatrixTerminalSections} */
    private boolean handleSideColumnClick(double mouseX, double mouseY) {
        return MiniMatrixTerminalSections.handleSideColumnClick(this, mouseX, mouseY);
    }

    /** 加工列表页渲染 → {@link MiniMatrixCraftList} */
    private void renderCraftPage(GuiGraphics gui) {
        this.craftList.renderCraftPage(gui);
    }

    /** 加工列表页悬停提示 → {@link MiniMatrixCraftList} */
    private void renderCraftTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        this.craftList.renderCraftTooltip(gui, mouseX, mouseY);
    }

    /** 本地过滤后的结果列表 → {@link MiniMatrixCraftList} */
    private List<ItemStack> craftResults() {
        return this.craftList.craftResults();
    }

    /** 鼠标下的加工结果格子 → {@link MiniMatrixCraftList} */
    private int hoveredCraftIndex(double mouseX, double mouseY, int size) {
        return this.craftList.hoveredCraftIndex(mouseX, mouseY, size);
    }

    /** 请求可合成物目录 → {@link MiniMatrixCraftList} */
    private void requestCraftCatalog() {
        this.craftList.requestCraftCatalog();
    }

    /** 加工详情页渲染 → {@link MiniMatrixCraftDetail} */
    private void renderCraftDetail(GuiGraphics gui) {
        this.craftDetail.renderCraftDetail(gui);
    }

    /** 加工详情页悬停提示 → {@link MiniMatrixCraftDetail} */
    private void renderCraftDetailTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        this.craftDetail.renderCraftDetailTooltip(gui, mouseX, mouseY);
    }

    /** 打开物品详情页 → {@link MiniMatrixCraftDetail} */
    private void openCraftDetail(Item item) {
        this.craftDetail.openCraftDetail(item);
    }

    /** 回到加工列表 → {@link MiniMatrixCraftDetail} */
    private void closeCraftDetail() {
        this.craftDetail.closeCraftDetail();
    }

    /** 详情页点击（返回 / 开始加工）→ {@link MiniMatrixCraftDetail} */
    private boolean handleCraftDetailClick(double mouseX, double mouseY) {
        return this.craftDetail.handleCraftDetailClick(mouseX, mouseY);
    }
}
