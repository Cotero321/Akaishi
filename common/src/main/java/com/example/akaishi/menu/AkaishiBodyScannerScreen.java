package com.example.akaishi.menu;

import com.example.akaishi.effect.ForbiddenSetHooks;
import com.example.akaishi.item.MechanicalOrganItem;
import com.example.akaishi.life.body.BodyOverviewEntry;
import com.example.akaishi.life.body.BodyPassiveEntry;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.ClientBodyData;
import com.example.akaishi.life.body.PlayerBodyState;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 躯体检查仪界面：自绘"医学扫描"面板（无背包槽）。
 * 展示 9 个躯体槽位：部位名 + 移植器官图标/名称 + 排斥条/数值；
 * 底部汇总总排斥、同源套装、已吸收基因型与突破强化状态；
 * 悬停槽位行/基因区查看详细资料（器官属性、基因加成）。数据来自 ClientBodyData（S2C 同步）。
 */
public class AkaishiBodyScannerScreen extends AbstractContainerScreen<AkaishiBodyScannerMenu> {

    /** 界面尺寸（无背包，纯信息面板；加宽以容纳部位名 + 排斥条 + 数值三栏，加高容纳基因区与饰品区） */
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 242;

    /** 内容区布局 */
    private static final int ROW_X = 10;
    private static final int ROW_START_Y = 26;
    private static final int ROW_HEIGHT = 15;
    private static final int SLOT_NAME_X = ROW_X;
    private static final int ORGAN_ICON_X = 92;
    private static final int ORGAN_NAME_X = 114;
    /** 器官名称最大宽度（超宽截断，避免横穿排斥条） */
    private static final int ORGAN_NAME_MAX_W = 34;
    private static final int REJECT_BAR_X = 152;
    private static final int REJECT_BAR_W = 30;
    private static final int REJECT_BAR_H = 6;
    private static final int REJECT_NUM_X = 192;

    /** 底部信息行（9 行槽位之下） */
    private static final int SUMMARY_Y = 160;
    private static final int GENES_Y = 169;
    private static final int BREAKTHROUGH_Y = 178;
    private static final int SYNERGY_Y = 187;

    /** 饰品区（禁忌四件 · 侵蚀进度，D96/D207）：图标行 / 进度条行 / 百分比行，四条并列 */
    private static final int CURIO_X = 6;
    private static final int CURIO_COL_W = 47;
    private static final int CURIO_ICON_Y = 198;
    private static final int CURIO_BAR_Y = 216;
    private static final int CURIO_TEXT_Y = 223;
    private static final int CURIO_BAR_W = 43;
    private static final int CURIO_BAR_H = 5;

    /** 槽位边框色：乱码（§5 紫）/ 锁槽（红） */
    private static final int CORRUPT_COLOR = 0xFFAA00AA;
    private static final int LOCK_COLOR = 0xFFD64545;

    /** 饰品区槽图标（socket_1..4，与槽位扫描页同源，16×16 直绘） */
    private static final ResourceLocation[] SOCKET_ICONS = new ResourceLocation[4];
    static {
        for (int i = 0; i < SOCKET_ICONS.length; i++) {
            SOCKET_ICONS[i] = new ResourceLocation("akaishi", "textures/slot/socket_" + (i + 1) + ".png");
        }
    }

    /** 顶部页签（互斥双页：槽位扫描 / 躯体总览），右对齐于面板内容区右上角 */
    private static final int TAB_Y = 4;

    /** 躯体总览页排版：纯文字行距（收窄以容纳更多属性种类），内容区上限 12 行 */
    private static final int OVERVIEW_LINE_H = 13;
    private static final int OVERVIEW_MAX_ROWS = 12;

    /** 背景色 */
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int PANEL_COLOR = 0xFFB0B0B0;
    private static final int LINE_COLOR = 0xFF373737;

    /** 当前页面：false=槽位扫描页（默认），true=躯体总览页（互斥切换，杜绝两页内容重叠） */
    private boolean overviewMode = false;
    /** 躯体总览页滚动偏移（行单位，超出可视行数时滚轮调节） */
    private int overviewScroll = 0;
    /** 饰品区快照（每游戏刻刷新一次，见 renderCurioSection） */
    private List<ForbiddenSetHooks.CurioState> curioCache = List.of();
    private long curioCacheTick = -1L;

    public AkaishiBodyScannerScreen(AkaishiBodyScannerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        // 自绘面板背景
        gui.fill(this.leftPos, this.topPos, this.leftPos + PANEL_W, this.topPos + PANEL_H, BG_COLOR);
        // 面板内容区
        gui.fill(this.leftPos + 6, this.topPos + 18, this.leftPos + PANEL_W - 6, this.topPos + PANEL_H - 8, PANEL_COLOR);
        // 躯体总览页不复用槽位排斥条背景（互斥页各自内容，避免残留扫描页元素）
        if (overviewMode) {
            return;
        }

        // 9 行槽位：排斥条
        for (int i = 0; i < BodySlot.values().length; i++) {
            BodySlot slot = BodySlot.values()[i];
            int barY = this.topPos + ROW_START_Y + i * ROW_HEIGHT;
            int x = this.leftPos + REJECT_BAR_X;
            int rej = ClientBodyData.getRejection(slot);
            // 底色 + 填充（比例随配置上限 maxRejection，默认 100 时不变）
            gui.fill(x, barY, x + REJECT_BAR_W, barY + REJECT_BAR_H, LINE_COLOR);
            int cap = PlayerBodyState.maxRejection();
            int width = (int) (REJECT_BAR_W * Math.min(cap, rej) / (double) cap);
            if (width > 0) {
                gui.fill(x, barY, x + width, barY + REJECT_BAR_H, rejectionColor(rej));
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 标题
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);

        // 顶部页签（互斥双页）
        drawTabs(gui);
        if (overviewMode) {
            renderOverview(gui);
            return;
        }

        // 9 行槽位：部位名 + 器官图标/名称 + 排斥数值
        LocalPlayer player = this.minecraft.player;
        boolean locked = player != null && ForbiddenSetHooks.socketsLocked(player);
        BodySlot[] slots = BodySlot.values();
        for (int i = 0; i < slots.length; i++) {
            BodySlot slot = slots[i];
            int rowY = this.topPos + ROW_START_Y + i * ROW_HEIGHT;
            // 部位名
            gui.drawString(this.font, Component.translatable(slot.getNameKey()), this.leftPos + SLOT_NAME_X, rowY - 4, 0x9CC8E0, false);
            // 器官图标 + 名称
            ItemStack organ = ClientBodyData.getOrgan(slot);
            if (!organ.isEmpty()) {
                gui.renderItem(organ, this.leftPos + ORGAN_ICON_X, rowY - 3);
                gui.renderItemDecorations(this.font, organ, this.leftPos + ORGAN_ICON_X, rowY - 3);
                String name = this.font.plainSubstrByWidth(organ.getHoverName().getString(), ORGAN_NAME_MAX_W);
                gui.drawString(this.font, name, this.leftPos + ORGAN_NAME_X, rowY - 4, 0xE0E0E0, false);
            } else {
                gui.drawString(this.font, Component.translatable("gui.akaishi.body_scanner.original"),
                        this.leftPos + ORGAN_NAME_X, rowY - 4, 0x707070, false);
            }
            // 排斥数值
            int rej = ClientBodyData.getRejection(slot);
            gui.drawString(this.font, String.valueOf(rej), this.leftPos + REJECT_NUM_X - this.font.width(String.valueOf(rej)),
                    rowY - 4, rejectionColor(rej), false);
            // 乱码槽位 / 锁槽：纯代码画边（D90/D206，随状态实时变色，零新贴图）
            int frameColor = locked ? LOCK_COLOR
                    : (!organ.isEmpty() && AkaishiOrganItem.isCorrupted(organ) ? CORRUPT_COLOR : 0);
            if (frameColor != 0) {
                drawSlotFrame(gui, this.leftPos + ROW_X - 2, rowY - 6,
                        this.leftPos + PANEL_W - 7, rowY + 8, frameColor);
            }
        }

        // 底部汇总行：总排斥 + 状况评级
        int total = ClientBodyData.getTotalRejection();
        int occupied = ClientBodyData.getOccupiedCount();
        String statusKey;
        int statusColor;
        int cap = PlayerBodyState.maxRejection();
        if (total >= cap) {
            statusKey = "gui.akaishi.body_scanner.critical";
            statusColor = 0xFFD64545;
        } else if (total >= cap * 0.6) {
            statusKey = "gui.akaishi.body_scanner.warning";
            statusColor = 0xFF8B6F1E;
        } else if (total >= cap * 0.3) {
            statusKey = "gui.akaishi.body_scanner.caution";
            statusColor = 0xFFE0A63A;
        } else {
            statusKey = "gui.akaishi.body_scanner.stable";
            statusColor = 0xFF2E7D32;
        }
        int sumY = this.topPos + SUMMARY_Y;
        if (locked) {
            // 锁槽优先于常规状况评级：佩戴禁忌饰品即拒绝一切改造（D13/D115）
            statusKey = "gui.akaishi.body.curio.locked";
            statusColor = LOCK_COLOR;
        }
        Component summary = Component.translatable("gui.akaishi.body_scanner.summary", occupied, slots.length, total);
        gui.drawString(this.font, summary, this.leftPos + SLOT_NAME_X, sumY, 0xE0E0E0, false);
        Component status = Component.translatable(statusKey);
        gui.drawString(this.font, status, this.leftPos + PANEL_W - 8 - this.font.width(status), sumY, statusColor, false);

        // 基因型行：已吸收基因摘要（悬停看明细）
        String geneText = geneSummary();
        gui.drawString(this.font, geneText, this.leftPos + ROW_X, this.topPos + GENES_Y, 0x9CC8E0, false);

        // 突破强化行（激活中才显示）
        if (ClientBodyData.hasActiveBreakthrough()) {
            String btText = formatTicks(ClientBodyData.getBreakthroughRemainingTicks(this.minecraft.level.getGameTime()));
            Component bt = Component.translatable("gui.akaishi.body_scanner.bt",
                    breakthroughName(), ClientBodyData.getBreakthroughPct(), btText);
            gui.drawString(this.font, bt, this.leftPos + ROW_X, this.topPos + BREAKTHROUGH_Y, 0xE0A63A, false);
        }

        // 同源套装行：统计 ≥2 枚的同来源器官（增速 -20%；≥4 枚额外适配 +5）
        String synergyText = synergySummary();
        if (!synergyText.isEmpty()) {
            gui.drawString(this.font, synergyText, this.leftPos + ROW_X, this.topPos + SYNERGY_Y, 0x8B6F1E, false);
        }

        // 饰品区：禁忌四件 · 侵蚀进度（D96/D207）
        renderCurioSection(gui, locked);

        // ===== 悬停详情 =====
        // 槽位行：移植器官完整属性 / 原装说明
        for (int i = 0; i < slots.length; i++) {
            int rowY = this.topPos + ROW_START_Y + i * ROW_HEIGHT;
            if (mouseY >= rowY - 4 && mouseY < rowY + ROW_HEIGHT && mouseX >= this.leftPos + ROW_X
                    && mouseX < this.leftPos + PANEL_W - 6) {
                List<Component> tip = rowTooltip(slots[i]);
                gui.renderComponentTooltip(this.font, tip, mouseX, mouseY);
                return;
            }
        }
        // 基因行：吸收的基因型明细与说明
        if (mouseY >= this.topPos + GENES_Y - 1 && mouseY < this.topPos + GENES_Y + 9
                && mouseX >= this.leftPos + ROW_X && mouseX < this.leftPos + PANEL_W - 6) {
            List<Component> tip = new ArrayList<>();
            if (ClientBodyData.getGeneCount() == 0) {
                tip.add(Component.translatable("gui.akaishi.body_scanner.no_genes"));
            } else {
                for (Map.Entry<String, Integer> entry : ClientBodyData.getGeneBonuses().entrySet()) {
                    tip.add(Component.literal("§a").append(geneName(entry.getKey()))
                            .append(Component.literal("  §7"))
                            .append(Component.translatable("gui.akaishi.gene_manager.bonus", entry.getValue())));
                }
            }
            tip.add(Component.translatable("gui.akaishi.body_scanner.genes_hover"));
            gui.renderComponentTooltip(this.font, tip, mouseX, mouseY);
            return;
        }
    }

    /** 槽位边框：1px 纯代码描边（上/下/左/右），零新贴图（D206） */
    private void drawSlotFrame(GuiGraphics gui, int x1, int y1, int x2, int y2, int color) {
        gui.fill(x1, y1, x2, y1 + 1, color);
        gui.fill(x1, y2 - 1, x2, y2, color);
        gui.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        gui.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    /** 饰品区：禁忌四件（生命之触/幼崽之心/母神之印/孕育之环）侵蚀进度条（D96/D207）。
     *  数据取自 ForbiddenSetHooks.curioStates（forge 注入，common 侧零 Curios 依赖）；
     *  forge 未注入时整节隐藏；未佩戴以灰化空槽占位。锁槽时整节加红框提示（D115）。 */
    private void renderCurioSection(GuiGraphics gui, boolean locked) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        // 每游戏刻取一次（Curios 查询较贵，避免每帧重复 resolve）
        long now = player.level().getGameTime();
        if (now != curioCacheTick) {
            curioCache = ForbiddenSetHooks.curioStates(player);
            curioCacheTick = now;
        }
        List<ForbiddenSetHooks.CurioState> states = curioCache;
        if (states.isEmpty()) {
            return;
        }
        int count = Math.min(states.size(), SOCKET_ICONS.length);
        for (int i = 0; i < count; i++) {
            ForbiddenSetHooks.CurioState state = states.get(i);
            int x = this.leftPos + CURIO_X + i * CURIO_COL_W;
            int iconY = this.topPos + CURIO_ICON_Y;
            // 槽图标直绘：socket_1..4（16×16），与槽位扫描页图标同源，避免新增资源
            gui.blit(SOCKET_ICONS[i], x, iconY, 0, 0, 16, 16, 16, 16);

            int pct = state.worn() ? Math.max(0, Math.min(100, (int) Math.round(state.erosion()))) : 0;
            int barX = x + 1;
            int barY = this.topPos + CURIO_BAR_Y;
            gui.fill(barX, barY, barX + CURIO_BAR_W, barY + CURIO_BAR_H, 0xFF2B2B2B);
            int fillW = CURIO_BAR_W * pct / 100;
            if (fillW > 0) {
                gui.fill(barX, barY, barX + fillW, barY + CURIO_BAR_H, erosionColor(pct, true));
            }

            Component text = state.worn()
                    ? Component.translatable("gui.akaishi.body.curio.erosion", pct)
                    : Component.translatable("gui.akaishi.body.curio.empty");
            String shown = this.font.plainSubstrByWidth(text.getString(), CURIO_BAR_W);
            gui.drawString(this.font, shown, barX, this.topPos + CURIO_TEXT_Y,
                    state.worn() ? 0xFFC8C8C8 : 0xFF6E6E6E, false);
        }
        if (locked) {
            int top = this.topPos + CURIO_ICON_Y - 2;
            drawSlotFrame(gui, this.leftPos + CURIO_X - 2, top,
                    this.leftPos + CURIO_X + count * CURIO_COL_W, top + CURIO_TEXT_Y - CURIO_ICON_Y + 12, LOCK_COLOR);
        }
    }

    /** 侵蚀配色：0 绿 → 50 黄 → 100 红（低于阈值无警示色） */
    private static int erosionColor(int pct, boolean worn) {
        if (!worn) {
            return 0xFF4A4A4A;
        }
        int r;
        int g;
        if (pct < 50) {
            r = 0x4C + (0xC8 - 0x4C) * pct / 50;
            g = 0xC0 - (0xC0 - 0x9A) * pct / 50;
        } else {
            r = 0xC8 + (0xD6 - 0xC8) * (pct - 50) / 50;
            g = 0x9A - (0x9A - 0x45) * (pct - 50) / 50;
        }
        return 0xFF000000 | (r << 16) | (g << 8) | 0x4C;
    }

    /** 顶部页签：槽位扫描 / 躯体总览（右对齐，激活项提亮） */
    private void drawTabs(GuiGraphics gui) {
        String scan = Component.translatable("gui.akaishi.body_scanner.tab_scan").getString();
        String overview = Component.translatable("gui.akaishi.body_scanner.tab_overview").getString();
        int right = this.leftPos + PANEL_W - 6;
        int overRight = right;
        int overLeft = overRight - this.font.width(overview);
        int scanRight = overLeft - 10;
        int scanLeft = scanRight - this.font.width(scan);
        int y = this.topPos + TAB_Y;
        int scanColor = overviewMode ? 0xFF8B8B8B : 0xFFE0E0E0;
        int overColor = overviewMode ? 0xFFE0E0E0 : 0xFF8B8B8B;
        gui.drawString(this.font, scan, scanLeft, y, scanColor, false);
        gui.drawString(this.font, overview, overLeft, y, overColor, false);
    }

    /** 躯体总览页：身体系统当前实际生效的属性净加成 + 被动叠加（服务端计算随包下发）。
     *  属性在前、被动在后统一成行模型；超出可视行数（OVERVIEW_MAX_ROWS）由滚轮滚动，右侧绘制滚动条。
     *  被动叠加 ≥2 来源时右侧显示罗马数字等级（与强度升级一一对应） */
    private void renderOverview(GuiGraphics gui) {
        List<BodyOverviewEntry> attributes = ClientBodyData.getOverview();
        List<BodyPassiveEntry> passives = ClientBodyData.getPassives();
        List<OverviewRow> rows = new ArrayList<>();
        for (BodyOverviewEntry entry : attributes) {
            // 移动速度为小秒速（0.03），与器官 tooltip 同口径按百分比呈现（+3%）
            boolean percent = Attributes.MOVEMENT_SPEED.getDescriptionId().equals(entry.attributeKey());
            rows.add(new OverviewRow(Component.translatable(entry.attributeKey()),
                    formatOverview(entry.value(), percent),
                    entry.value() < 0 ? 0xFFD64545 : 0xFF2E7D32));
        }
        for (BodyPassiveEntry passive : passives) {
            int roman = passive.count() >= 2 ? Math.min(passive.count(), 10) : 0;
            rows.add(new OverviewRow(Component.translatable("life.akaishi.organ_passive." + passive.passiveId()),
                    roman > 0 ? toRoman(roman) : "", roman > 0 ? 0xFFC8A03C : 0xFF9E9E9E));
        }
        int x = this.leftPos + ROW_X;
        int y = this.topPos + ROW_START_Y;
        if (rows.isEmpty()) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.body_scanner.overview_empty"),
                    x, y, 0x707070, false);
            gui.drawString(this.font, Component.translatable("gui.akaishi.body_scanner.overview_hint"),
                    x, y + 13, 0x8B8B8B, false);
            return;
        }
        int total = rows.size();
        int maxOffset = Math.max(0, total - OVERVIEW_MAX_ROWS);
        if (overviewScroll > maxOffset) {
            overviewScroll = maxOffset; // 防御：数据收缩后滚动位置越界即钳制
        }
        int shown = 0;
        for (int i = overviewScroll; i < total && shown < OVERVIEW_MAX_ROWS; i++, shown++) {
            OverviewRow row = rows.get(i);
            String name = this.font.plainSubstrByWidth(row.name().getString(), 96);
            gui.drawString(this.font, name, x, y, 0xE0E0E0, false);
            if (!row.right().isEmpty()) {
                gui.drawString(this.font, row.right(),
                        this.leftPos + REJECT_NUM_X - this.font.width(row.right()), y, row.color(), false);
            }
            y += OVERVIEW_LINE_H;
        }
        if (maxOffset == 0 && shown < OVERVIEW_MAX_ROWS) {
            gui.drawString(this.font, Component.translatable("gui.akaishi.body_scanner.overview_hint"),
                    x, y, 0x8B8B8B, false);
        } else if (maxOffset > 0) {
            // 可滚动：右侧轨道 + 滑块（vanilla 灰阶，与界面配色一致）
            int trackTop = this.topPos + ROW_START_Y;
            int trackH = OVERVIEW_LINE_H * OVERVIEW_MAX_ROWS;
            int barX = this.leftPos + PANEL_W - 6; // 贴内容区右缘，与数值列（REJECT_NUM_X）留出间隙
            gui.fill(barX, trackTop, barX + 2, trackTop + trackH, 0xFF3F3F3F);
            int thumbH = Math.max(14, trackH * OVERVIEW_MAX_ROWS / total);
            int thumbY = trackTop + (trackH - thumbH) * overviewScroll / maxOffset;
            gui.fill(barX, thumbY, barX + 2, thumbY + thumbH, 0xFF8B8B8B);
        }
    }

    /** 总览页单行（属性行：右侧数值着色；被动行：右侧罗马级金色） */
    private record OverviewRow(Component name, String right, int color) {
    }

    /** 被动叠加计数 → 罗马数字（I~X，超出回退十进制） */
    private static String toRoman(int n) {
        String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n < roman.length ? roman[n] : String.valueOf(n);
    }

    /** 总览属性值格式化：整数带符号、无小数点；小数保留至多 4 位去尾零（小基数加成不显示成 0.0）；移动速度换算百分比 */
    private static String formatOverview(double value, boolean percent) {
        double disp = percent ? value * 100.0 : value;
        String sign = disp > 0 ? "+" : "";
        if (!percent && disp == Math.floor(disp) && !Double.isInfinite(disp)) {
            return sign + (long) disp;
        }
        return sign + BigDecimal.valueOf(disp).setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString() + (percent ? "%" : "");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int tab = hitTab(mouseX, mouseY);
            if (tab == 1) {
                overviewMode = false;
                overviewScroll = 0;
                return true;
            }
            if (tab == 2) {
                overviewMode = true;
                overviewScroll = 0;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 躯体总览页滚轮翻页：上滚回看、下滚下翻，越界自动钳制 */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (overviewMode) {
            int maxOffset = Math.max(0, ClientBodyData.getOverview().size()
                    + ClientBodyData.getPassives().size() - OVERVIEW_MAX_ROWS);
            if (maxOffset > 0 && delta != 0) {
                int target = overviewScroll - (delta > 0 ? 1 : -1);
                overviewScroll = Math.max(0, Math.min(maxOffset, target));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** 命中页签：1=槽位扫描，2=躯体总览，0=未命中（坐标算法与 drawTabs 一致） */
    private int hitTab(double mouseX, double mouseY) {
        if (mouseY < this.topPos + TAB_Y - 1 || mouseY >= this.topPos + TAB_Y + 10) {
            return 0;
        }
        String scan = Component.translatable("gui.akaishi.body_scanner.tab_scan").getString();
        String overview = Component.translatable("gui.akaishi.body_scanner.tab_overview").getString();
        int right = this.leftPos + PANEL_W - 6;
        int scanRight = right - this.font.width(overview) - 10;
        int scanLeft = scanRight - this.font.width(scan);
        int overRight = right;
        int overLeft = overRight - this.font.width(overview);
        if (mouseX >= scanLeft - 2 && mouseX <= scanRight + 2) {
            return 1;
        }
        if (mouseX >= overLeft - 2 && mouseX <= overRight + 2) {
            return 2;
        }
        return 0;
    }

    /** 槽位行悬停内容：器官自带属性行 + 排斥值；空槽显示原装说明 */
    private List<Component> rowTooltip(BodySlot slot) {
        List<Component> tip = new ArrayList<>();
        ItemStack organ = ClientBodyData.getOrgan(slot);
        if (organ.isEmpty()) {
            tip.add(Component.translatable(slot.getNameKey()));
            tip.add(Component.translatable("gui.akaishi.body_scanner.original_tip"));
            return tip;
        }
        // 复用原版/模组物品 tooltip（品质/来源/适配等已在 appendHoverText 提供；1.20.1 无 TooltipContext，直接传玩家与标志）
        tip.addAll(organ.getTooltipLines(this.minecraft.player,
                this.minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
        tip.add(Component.literal("§7" + Component.translatable("gui.akaishi.body_scanner.rejection_row",
                ClientBodyData.getRejection(slot)).getString()));
        // 机械义体额外显示整合度（生物器官无此概念）
        if (organ.getItem() instanceof MechanicalOrganItem) {
            tip.add(Component.literal("§7" + Component.translatable("gui.akaishi.body_scanner.integration_row",
                    ClientBodyData.getIntegration(slot)).getString()));
        }
        return tip;
    }

    /** 已吸收基因型单行摘要（截断到面板宽度），无基因返回提示文案 */
    private String geneSummary() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : ClientBodyData.getGeneBonuses().entrySet()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(geneName(entry.getKey()).getString())
                    .append('+').append(entry.getValue());
        }
        String text = sb.length() == 0
                ? Component.translatable("gui.akaishi.body_scanner.no_genes").getString()
                : Component.translatable("gui.akaishi.body_scanner.genes", sb).getString();
        return this.font.plainSubstrByWidth(text, PANEL_W - ROW_X - 14);
    }

    /** 基因来源生物显示名 */
    private Component geneName(String entityId) {
        return EntityType.byString(entityId)
                .map(type -> (Component) type.getDescription())
                .orElse(Component.literal(entityId));
    }

    /** 突破来源名（无激活时兜底空串） */
    private String breakthroughName() {
        String id = ClientBodyData.getBreakthroughEntity();
        return id.isEmpty() ? "" : geneName(id).getString();
    }

    /** tick 数 → mm:ss（不足 1 分钟显示 0:ss） */
    private String formatTicks(long ticks) {
        if (ticks <= 0) {
            return "0:00";
        }
        long sec = ticks / 20;
        return sec / 60 + ":" + String.format(java.util.Locale.ROOT, "%02d", sec % 60);
    }

    /** 汇总 ≥2 枚的同来源器官：如「同源套装：狼×3 · 猫×2」，无则返回空串 */
    private String synergySummary() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = ClientBodyData.getOrgan(slot);
            if (organ.getItem() instanceof AkaishiOrganItem && !AkaishiOrganItem.isNative(organ)) {
                String id = AkaishiOrganItem.getEntityId(organ);
                // 未定型 / 无来源器官的 id 为 null，不参与同源汇总
                if (id != null && !id.isEmpty()) {
                    counts.merge(id, 1, Integer::sum);
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() < 2) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(geneName(entry.getKey()).getString()).append('×').append(entry.getValue());
        }
        if (sb.length() == 0) {
            return "";
        }
        return this.font.plainSubstrByWidth(
                Component.translatable("gui.akaishi.body_scanner.synergy", sb.toString()).getString(),
                PANEL_W - ROW_X - 14);
    }

    /** 排斥值 → 颜色（绿/黄/橙/红），中低档按配置上限等比缩放（默认 100 时为 60/30） */
    private int rejectionColor(int rej) {
        int cap = PlayerBodyState.maxRejection();
        if (rej >= cap) return 0xFFD64545;
        if (rej >= cap * 0.6) return 0xFFE08A3A;
        if (rej >= cap * 0.3) return 0xFF8B6F1E;
        return 0xFF2E7D32;
    }
}
