package com.example.akaishi.menu;

import com.example.akaishi.block.AkaishiMiniMatrixUpgradeType;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 微缩矩阵终端界面 · 加工列表页协作类（纯机械搬迁自 {@link AkaishiMiniMatrixTerminalScreen}）。
 * <p>
 * 承接加工页（列表态）的成块渲染与本地过滤状态：目录过滤、图标格与滚动条、
 * 加工任务进度条、列表悬停提示、滚轮整行翻页与目录请求；
 * 加工页布局常量（CRAFT_*）真源落在本类（详情页经 {@link MiniMatrixCraftDetail} 同名别名共用）。
 * <p>
 * 列表是<b>客户端本地过滤</b>的结果（目录由服务端一次性下发），本类持有过滤缓存与滚动行；
 * 每个宿主界面一个实例（状态为界面实例态，非静态）。
 */
final class MiniMatrixCraftList {

    // 加工页布局：标题行（标题 + 长搜索框）→ 图标格（8×4，滚轮 + 右侧滚动条）→ 进度条
    // 可用区 y=30..122（背包线 124）：标题 30..44、格区 46..118、条 119..124
    // （CRAFT_TITLE_Y / CRAFT_SEARCH_* 为包私有：宿主 init 的搜索框创建按原坐标引用同一真源）
    static final int CRAFT_TITLE_Y = 30;
    /** 搜索框拉长到标题行的剩余宽度（提示文字同时精简，避免长提示溢出压到别处） */
    static final int CRAFT_SEARCH_X = 56;
    static final int CRAFT_SEARCH_W = 112;
    static final int CRAFT_SEARCH_H = 14;
    /**
     * 图标格区：8 列 × 4 行、格距 18（16px 图标 + 2px 间隔），整行滚动。
     * <p>
     * 与储存终端同一套观感（格框 + 图标 + 右侧滚动条）：这里只画图标，物品名与成本走悬停提示。
     */
    private static final int CRAFT_COLS = 8;
    private static final int CRAFT_ROWS = 4;
    /** 格距（详情页材料格共用同一格距，经 {@link MiniMatrixCraftDetail#CRAFT_CELL} 别名引用） */
    static final int CRAFT_CELL = 18;
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

    /** 文本配色别名（真源见 {@link MiniMatrixTerminalSections}） */
    private static final int TEXT = MiniMatrixTerminalSections.TEXT;
    private static final int TEXT_DIM = MiniMatrixTerminalSections.TEXT_DIM;
    private static final int TEXT_RED = MiniMatrixTerminalSections.TEXT_RED;

    /** 宿主界面（访问 menu / font / leftPos / topPos / searchBox） */
    private final AkaishiMiniMatrixTerminalScreen scr;

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

    MiniMatrixCraftList(AkaishiMiniMatrixTerminalScreen scr) {
        this.scr = scr;
    }

    /** 当前查询串（详情页发动作包时随包携带） */
    String query() {
        return craftQuery;
    }

    /**
     * 加工页滚轮翻页（整行滚动）。返回 true = 本页消费了这次滚动。
     * <p>
     * 面板里只有这一处可滚动，故本页直接消费滚轮，不额外要求"必须悬停在格子上"；
     * 其它页原样交回原版（背包滚动等，由宿主判断当前页后调用）。
     */
    boolean scrollBy(double delta) {
        if (delta == 0.0D) {
            return false;
        }
        int rows = (craftResults().size() + CRAFT_COLS - 1) / CRAFT_COLS;
        int maxRow = Math.max(0, rows - CRAFT_ROWS);
        if (maxRow > 0) {
            craftScrollRow = Math.max(0, Math.min(maxRow, craftScrollRow - (int) Math.signum(delta)));
        }
        return true;
    }

    /** 请求可合成物目录（切到加工页 / 重开界面 = 刷新入口；服务端不再按查询串搜索） */
    void requestCraftCatalog() {
        AkaishiMatrixCraftSync.sendAction(scr.menu().containerId, AkaishiMatrixCraftSync.ACTION_SEARCH,
                "", ItemStack.EMPTY);
    }

    /**
     * 页3：加工（标题 → 图标格 → 选中 → 再点一次开始）。
     * <p>
     * 列表是<b>客户端本地过滤</b>的结果（目录由服务端一次性下发）；
     * 格子里只画物品图标（与储存终端同一套观感），物品名与成本走悬停提示；
     * 滚轮整行翻页，右侧滚动条指示位置。
     */
    void renderCraftPage(GuiGraphics gui) {
        if (scr.menu().upgradeCount(AkaishiMiniMatrixUpgradeType.CRAFT.ordinal()) <= 0) {
            gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.craft.need_upgrade"),
                    8, CRAFT_TITLE_Y, TEXT_RED, false);
            return;
        }
        // 标题与搜索框同处 y=33：必须让出搜索框起点。英文 "Craftable now" 约 70px，不截会压到搜索框与其中文字上
        gui.drawString(scr.font(),
                scr.font().plainSubstrByWidth(Component.translatable("gui.akaishi.matrix.craft.title").getString(),
                        CRAFT_SEARCH_X - 8 - 4),
                8, CRAFT_TITLE_Y + 3, TEXT, false);
        List<ItemStack> results = craftResults();
        if (results.isEmpty()) {
            // 索引还在服务端分片构建时显示"准备中"，而不是"没有可合成的物品"：
            // 后者会让玩家以为没配方，实际只是首次进世界后还没建完
            gui.drawString(scr.font(), Component.translatable(scr.menu().craftCatalogBuilding()
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
        AkaishiMatrixCraftSync.TaskView task = scr.menu().craftTaskView();
        if (task == null) {
            String fail = scr.menu().craftTaskFail();
            if (fail != null) {
                Component line = Component.translatable("gui.akaishi.matrix.craft.fail", failLine(fail));
                gui.drawString(scr.font(), scr.font().plainSubstrByWidth(line.getString(),
                        MiniMatrixTerminalSections.CONTENT_W),
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
        gui.drawString(scr.font(), scr.font().plainSubstrByWidth(line, 168 - CRAFT_BAR_TEXT_X),
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
    void renderCraftTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        // 底行（进度条那一行）：悬停给"真机加工的真实进度"。机台耗时不归我们算（第三方连速度都读不到），
        // 只有节点数是我们确知的，故这里把它与目标、预估一并给全 —— 常驻那一行放不下这么多字
        AkaishiMatrixCraftSync.TaskView task = scr.menu().craftTaskView();
        if (task != null && task.type() == AkaishiMatrixCraftSync.TASK_CRAFT
                && AkaishiMiniMatrixTerminalScreen.isIn(8, CRAFT_BAR_TEXT_Y - 1,
                        MiniMatrixTerminalSections.CONTENT_W, CRAFT_BAR_H + 3, mouseX, mouseY)) {
            gui.renderComponentTooltip(scr.font(), List.of(
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
        gui.renderComponentTooltip(scr.font(), lines, mouseX, mouseY);
    }

    /**
     * 本地过滤后的结果列表：目录 / 查询串 / 可直接制作的边界变化才重算。
     * <p>
     * 与库页同用 {@code ItemTerminalSearch}（名称 + 拼音 + 注册名），
     * 客户端语言环境完整 ⇒ 中文名与模组名都搜得到，且每次按键不再发包。
     */
    List<ItemStack> craftResults() {
        String query = scr.searchBox == null ? "" : scr.searchBox.getValue();
        List<ItemStack> catalog = scr.menu().craftCatalog();
        int ready = scr.menu().craftReadyCount();
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
    int hoveredCraftIndex(double mouseX, double mouseY, int size) {
        int relX = (int) mouseX - scr.leftPos() - CRAFT_GRID_X;
        int relY = (int) mouseY - scr.topPos() - CRAFT_GRID_Y;
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
}
