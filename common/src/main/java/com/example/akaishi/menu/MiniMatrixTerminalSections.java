package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.AkaishiMiniMatrixUpgradeType;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 微缩矩阵终端界面 · 区域渲染协作类（纯机械搬迁自 {@link AkaishiMiniMatrixTerminalScreen}）。
 * <p>
 * 承接<b>成块的面板/区域渲染、悬停提示与命中判定</b>：芯片列表页、总览页、
 * 左列「芯片族跳转页签」（含翻页）、以及它们各自的悬停提示与命中判定；
 * 主页签（TAB_*）与文本配色真源也落在本类。
 * <p>
 * 各方法首参 {@code scr} = 宿主界面（访问 menu / font / leftPos / topPos），
 * 方法体与原实现逐位一致；命中判定与绘制共用同一份几何常量。
 */
final class MiniMatrixTerminalSections {

    // ===== 文本配色（真源；界面与加工页协作类经同名别名引用） =====
    static final int TEXT = 0xFF3F3F3F;
    static final int TEXT_DIM = 0xFF707070;
    static final int TEXT_RED = 0xFFB03030;
    static final int TEXT_GREEN = 0xFF2E7D32;

    /**
     * 左列（面板左侧新增的一竖列）：三个「把玩家送到那枚芯片自己的界面」的页签，
     * 以及同族多于一枚时的翻页。
     * <p>
     * <b>与主页签是两套东西，互不影响</b>：主页签切的是本界面内部的页（芯片/总览/加工/安全）；
     * 左列页签点完就离开本界面（转到芯片方块自己的界面），因此左列没有"当前页"概念、不做选中态，
     * 也不会与主页签的选中态打架。点击区上，左列占面板左侧 x&lt;0 的 48px，
     * 面板与全部既有控件都在 x≥0，两者零重叠。
     */
    static final int LEFT_TAB_X = 4;
    static final int LEFT_TAB_W = 44;
    static final int LEFT_TAB_H = 14;
    /** 三个页签的 y（间隔 4px）：赤能源 / 生命能量 / 储存 */
    static final int[] LEFT_TAB_Y = {16, 34, 52};
    /** 翻页控件行 y（页签下方 4px；只在悬停的那一族有 2 枚以上时出现） */
    static final int LEFT_PAGER_Y = 70;
    static final int LEFT_PAGER_BTN_W = 10;
    static final int LEFT_PAGER_TEXT_W = 16;
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
    static final int CONTENT_W = 160;

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

    private MiniMatrixTerminalSections() {
    }

    /**
     * 左列：三个芯片族页签 + 同族多枚时的翻页。
     * <p>
     * 该族没有已识别芯片 ⇒ 页签按禁用态绘制（压暗 + 灰字），点了也不做任何事：
     * 宁可"看着不可点"，也不给一个点了必然失败的入口。
     * 翻页只在<b>鼠标当前悬停的那一族真有 2 枚以上</b>时出现 —— 同样不做假按钮。
     */
    static void renderSideColumn(AkaishiMiniMatrixTerminalScreen scr, GuiGraphics gui, int panelX, int panelY, int mouseX, int mouseY) {
        int colX = panelX - AkaishiMiniMatrixTerminalScreen.LEFT_COL_W;
        GuiWidgets.panel(gui, colX, panelY, AkaishiMiniMatrixTerminalScreen.LEFT_COL_W, scr.imageHeight());
        for (int i = 0; i < LEFT_TAB_Y.length; i++) {
            GuiWidgets.buttonText(gui, scr.font(), colX + LEFT_TAB_X, panelY + LEFT_TAB_Y[i],
                    LEFT_TAB_W, LEFT_TAB_H, Component.translatable(LEFT_TAB_KEY[i]), !sideFamily(scr, i).isEmpty());
        }
        int hovered = hoveredSideTab(scr, mouseX, mouseY);
        if (hovered < 0 || sideFamily(scr, hovered).size() < 2) {
            return;
        }
        int rows = sideFamily(scr, hovered).size();
        int y = panelY + LEFT_PAGER_Y;
        GuiWidgets.buttonText(gui, scr.font(), colX + LEFT_TAB_X, y,
                LEFT_PAGER_BTN_W, LEFT_TAB_H, Component.literal("<"), true);
        GuiWidgets.buttonText(gui, scr.font(), colX + LEFT_TAB_X + LEFT_PAGER_BTN_W + 3, y,
                LEFT_PAGER_TEXT_W, LEFT_TAB_H, Component.translatable("gui.akaishi.matrix.side.page",
                        Math.floorMod(SIDE_INDEX[hovered], rows) + 1, rows), false);
        GuiWidgets.buttonText(gui, scr.font(),
                colX + LEFT_TAB_X + LEFT_PAGER_BTN_W + LEFT_PAGER_TEXT_W + 6, y,
                LEFT_PAGER_BTN_W, LEFT_TAB_H, Component.literal(">"), true);
    }

    /** 页1：成型状态 + 芯片逐行读数（行号 / 名称 / 短 ID / 类型 / IP / 能量） */
    static void renderChipPage(AkaishiMiniMatrixTerminalScreen scr, GuiGraphics gui) {
        AkaishiMiniMatrixTerminalMenu menu = scr.menu();
        Component status = Component.translatable(menu.isFormed()
                ? "gui.akaishi.matrix.formed" : "gui.akaishi.matrix.unformed");
        Component countText = Component.translatable("gui.akaishi.matrix.chip_count", menu.chipRows().size());
        int countWidth = scr.font().width(countText);
        // 两串文字同处 y=30：左侧状态必须让出右侧计数的宽度。
        // 英文 "Structure incomplete (5x5x5 closed box)" 约 210px，不截会与计数重叠并压出面板
        gui.drawString(scr.font(),
                scr.font().plainSubstrByWidth(status.getString(), Math.max(24, CONTENT_W - countWidth - 6)),
                8, 30, menu.isFormed() ? TEXT_GREEN : TEXT_RED, false);
        gui.drawString(scr.font(), countText, CONTENT_W + 8 - countWidth, 30, TEXT_DIM, false);
        int count = menu.chipRows().size();
        if (count == 0) {
            gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.chip_none"),
                    8, ROW_Y + 2, TEXT_DIM, false);
            return;
        }
        int shown = Math.min(count, MAX_ROWS);
        for (int i = 0; i < shown; i++) {
            AkaishiMiniMatrixSync.ChipRow row = menu.chipRows().get(i);
            String line = chipLine(i, row);
            gui.drawString(scr.font(), scr.font().plainSubstrByWidth(line, CONTENT_W),
                    8, ROW_Y + i * ROW_H + 2,
                    row.loaded() ? TEXT : TEXT_DIM, false);
        }
        // 溢出提示：只显示前 MAX_ROWS 行，其余计数告知（避免"少了几枚以为丢了"）
        if (count > shown) {
            gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.chip_more", count - shown),
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
    static void renderUpgradePage(AkaishiMiniMatrixTerminalScreen scr, GuiGraphics gui) {
        AkaishiMiniMatrixTerminalMenu menu = scr.menu();
        // 成型时不再占行（成型是常态）；未成型必须显式提示，否则玩家会以为组件生效了
        if (!menu.isFormed()) {
            gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.upgrade.unformed"),
                    8, 30, TEXT_RED, false);
        }
        AkaishiMiniMatrixUpgradeType[] types = AkaishiMiniMatrixUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            AkaishiMiniMatrixUpgradeType type = types[i];
            int count = menu.upgradeCount(type.ordinal());
            String line = Component.translatable("gui.akaishi.matrix.upgrade.level",
                    Component.translatable(type.nameKey()).getString(), count, type.maxCount()).getString();
            gui.drawString(scr.font(), scr.font().plainSubstrByWidth(line, CONTENT_W),
                    UPGRADE_LINE_X, UPGRADE_LINE_Y + i * UPGRADE_LINE_H, count > 0 ? TEXT : TEXT_DIM, false);
        }
        // 运行情况：把"两条链路真的在跑"变成可核对回执（否则玩家无法确认升级是否生效）
        int infoY = UPGRADE_LINE_Y + types.length * UPGRADE_LINE_H;
        gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.overview.runtime"),
                UPGRADE_LINE_X, infoY, TEXT_DIM, false);
        // 直供要同时装「操控 + 联动」：缺任一个都不会送能，故两枚都在位才标绿
        boolean supplying = menu.upgradeCount(AkaishiMiniMatrixUpgradeType.CONTROL.ordinal()) > 0
                && menu.upgradeCount(AkaishiMiniMatrixUpgradeType.LINK.ordinal()) > 0;
        gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.upgrade.push",
                        menu.pushedEnergy()), UPGRADE_LINE_X, infoY + UPGRADE_LINE_H,
                supplying ? TEXT_GREEN : TEXT_DIM, false);
        // 芯片间搬运是矩阵内部总线、不需要任何升级，故只按"本轮是否真搬了"着色
        long transfer = menu.chipTransfer();
        gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.upgrade.chip_transfer", transfer),
                UPGRADE_LINE_X, infoY + 2 * UPGRADE_LINE_H, transfer > 0L ? TEXT_GREEN : TEXT_DIM, false);
    }

    /**
     * 左列悬停：先说清"这一下会打开哪一枚芯片"；该族没有芯片时说明为什么点不动。
     * 返回 true 表示已经出过提示（此时不再走其它提示，避免两框叠在一起）。
     */
    static boolean renderSideTooltip(AkaishiMiniMatrixTerminalScreen scr, GuiGraphics gui, int mouseX, int mouseY) {
        int family = hoveredSideTab(scr, mouseX, mouseY);
        if (family < 0) {
            return false;
        }
        List<AkaishiMiniMatrixSync.ChipRow> rows = sideFamily(scr, family);
        List<Component> lines = new ArrayList<>();
        if (rows.isEmpty()) {
            lines.add(Component.translatable("gui.akaishi.matrix.side.none"));
        } else {
            lines.add(Component.translatable("gui.akaishi.matrix.side.open",
                    rows.get(Math.floorMod(SIDE_INDEX[family], rows.size())).name()));
        }
        gui.renderComponentTooltip(scr.font(), lines, mouseX, mouseY);
        return true;
    }

    /** 芯片行悬停：完整名称 / 短 ID / 族类型 / IP 与能量（行内文本被截断，靠这里看全） */
    static void renderChipTooltip(AkaishiMiniMatrixTerminalScreen scr, GuiGraphics gui, int mouseX, int mouseY) {
        int row = hoveredChipRow(scr, mouseX, mouseY);
        if (row < 0 || row >= scr.menu().chipRows().size()) {
            return;
        }
        AkaishiMiniMatrixSync.ChipRow chip = scr.menu().chipRows().get(row);
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
        gui.renderComponentTooltip(scr.font(), lines, mouseX, mouseY);
    }

    /** 升级行悬停：该项说明（组件是方块，界面只报数量，这里补足「装几个会怎样」） */
    static void renderUpgradeTooltip(AkaishiMiniMatrixTerminalScreen scr, GuiGraphics gui, int mouseX, int mouseY) {
        AkaishiMiniMatrixUpgradeType[] types = AkaishiMiniMatrixUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            int lineY = scr.topPos() + UPGRADE_LINE_Y + i * UPGRADE_LINE_H;
            if (mouseX >= scr.leftPos() + UPGRADE_LINE_X && mouseX < scr.leftPos() + CONTENT_W + 8
                    && mouseY >= lineY && mouseY < lineY + UPGRADE_LINE_H - 1) {
                gui.renderComponentTooltip(scr.font(),
                        List.of(Component.translatable(types[i].nameKey()),
                                Component.translatable(types[i].hintKey())),
                        mouseX, mouseY);
                return;
            }
        }
    }

    /** 鼠标所在芯片行；不在列表内返回 -1 */
    static int hoveredChipRow(AkaishiMiniMatrixTerminalScreen scr, int mouseX, int mouseY) {
        int relY = mouseY - scr.topPos() - ROW_Y;
        if (mouseX < scr.leftPos() + ROW_X || mouseX >= scr.leftPos() + CONTENT_W + 8 || relY < 0) {
            return -1;
        }
        int row = relY / ROW_H;
        return row < MAX_ROWS ? row : -1;
    }

    /** 该族当前被识别到的芯片行（左列页签的灰态与翻页都据此判定） */
    static List<AkaishiMiniMatrixSync.ChipRow> sideFamily(AkaishiMiniMatrixTerminalScreen scr, int family) {
        String id = LEFT_FAMILY[family].toString();
        List<AkaishiMiniMatrixSync.ChipRow> found = new ArrayList<>();
        for (AkaishiMiniMatrixSync.ChipRow row : scr.menu().chipRows()) {
            if (row.loaded() && id.equals(row.type())) {
                found.add(row);
            }
        }
        return found;
    }

    /** 鼠标下的左列页签序号（无则 -1） */
    static int hoveredSideTab(AkaishiMiniMatrixTerminalScreen scr, double mouseX, double mouseY) {
        int colX = scr.leftPos() - AkaishiMiniMatrixTerminalScreen.LEFT_COL_W;
        for (int i = 0; i < LEFT_TAB_Y.length; i++) {
            if (AkaishiMiniMatrixTerminalScreen.isIn(colX + LEFT_TAB_X, scr.topPos() + LEFT_TAB_Y[i],
                    LEFT_TAB_W, LEFT_TAB_H, mouseX, mouseY)) {
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
    static void openChip(AkaishiMiniMatrixTerminalScreen scr, int family, int index) {
        List<AkaishiMiniMatrixSync.ChipRow> rows = sideFamily(scr, family);
        if (rows.isEmpty()) {
            return;
        }
        int k = Math.floorMod(index, rows.size());
        SIDE_INDEX[family] = k;
        // 记下"从哪儿跳来的"：芯片界面左侧会据此显示返回页签（服务端仍会校验坐标）
        MiniMatrixReturn.mark(scr.menu().matrixPos());
        AkaishiMatrixCraftSync.sendAction(scr.menu().containerId, AkaishiMatrixCraftSync.ACTION_OPEN_CHIP,
                "", ItemStack.EMPTY, ItemStack.EMPTY, rows.get(k).pos());
    }

    /** 左列点击：页签 = 打开该族当前那一枚，翻页 = 换一枚并打开。返回是否已消费这次点击 */
    static boolean handleSideColumnClick(AkaishiMiniMatrixTerminalScreen scr, double mouseX, double mouseY) {
        int colX = scr.leftPos() - AkaishiMiniMatrixTerminalScreen.LEFT_COL_W;
        for (int i = 0; i < LEFT_TAB_Y.length; i++) {
            if (AkaishiMiniMatrixTerminalScreen.isIn(colX + LEFT_TAB_X, scr.topPos() + LEFT_TAB_Y[i],
                    LEFT_TAB_W, LEFT_TAB_H, mouseX, mouseY)) {
                // 灰态也吞掉点击：避免穿透到下面的控件上
                openChip(scr, i, SIDE_INDEX[i]);
                return true;
            }
        }
        int hovered = hoveredSideTab(scr, mouseX, mouseY);
        if (hovered < 0 || sideFamily(scr, hovered).size() < 2) {
            return false;
        }
        int y = scr.topPos() + LEFT_PAGER_Y;
        if (AkaishiMiniMatrixTerminalScreen.isIn(colX + LEFT_TAB_X, y, LEFT_PAGER_BTN_W, LEFT_TAB_H, mouseX, mouseY)) {
            openChip(scr, hovered, SIDE_INDEX[hovered] - 1);
            return true;
        }
        int nextX = colX + LEFT_TAB_X + LEFT_PAGER_BTN_W + LEFT_PAGER_TEXT_W + 6;
        if (AkaishiMiniMatrixTerminalScreen.isIn(nextX, y, LEFT_PAGER_BTN_W, LEFT_TAB_H, mouseX, mouseY)) {
            openChip(scr, hovered, SIDE_INDEX[hovered] + 1);
            return true;
        }
        return false;
    }
}
