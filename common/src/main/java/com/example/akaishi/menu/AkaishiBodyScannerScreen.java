package com.example.akaishi.menu;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.ClientBodyData;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

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

    /** 界面尺寸（无背包，纯信息面板；加宽以容纳部位名 + 排斥条 + 数值三栏，加高容纳基因区） */
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 200;

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

    /** 背景色 */
    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int PANEL_COLOR = 0xFFB0B0B0;
    private static final int LINE_COLOR = 0xFF373737;

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
        gui.fill(this.leftPos + 6, this.topPos + 18, this.leftPos + PANEL_W - 6, this.topPos + 192, PANEL_COLOR);

        // 9 行槽位：排斥条
        for (int i = 0; i < BodySlot.values().length; i++) {
            BodySlot slot = BodySlot.values()[i];
            int barY = this.topPos + ROW_START_Y + i * ROW_HEIGHT;
            int x = this.leftPos + REJECT_BAR_X;
            int rej = ClientBodyData.getRejection(slot);
            // 底色 + 填充
            gui.fill(x, barY, x + REJECT_BAR_W, barY + REJECT_BAR_H, LINE_COLOR);
            int width = (int) (REJECT_BAR_W * Math.min(100, rej) / 100.0);
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

        // 9 行槽位：部位名 + 器官图标/名称 + 排斥数值
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
        }

        // 底部汇总行：总排斥 + 状况评级
        int total = ClientBodyData.getTotalRejection();
        int occupied = ClientBodyData.getOccupiedCount();
        String statusKey;
        int statusColor;
        if (total >= 100) {
            statusKey = "gui.akaishi.body_scanner.critical";
            statusColor = 0xFFD64545;
        } else if (total >= 60) {
            statusKey = "gui.akaishi.body_scanner.warning";
            statusColor = 0xFF8B6F1E;
        } else if (total >= 30) {
            statusKey = "gui.akaishi.body_scanner.caution";
            statusColor = 0xFFE0A63A;
        } else {
            statusKey = "gui.akaishi.body_scanner.stable";
            statusColor = 0xFF2E7D32;
        }
        int sumY = this.topPos + SUMMARY_Y;
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
                if (!id.isEmpty()) {
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

    /** 排斥值 → 颜色（绿/黄/橙/红） */
    private int rejectionColor(int rej) {
        if (rej >= 100) return 0xFFD64545;
        if (rej >= 60) return 0xFFE08A3A;
        if (rej >= 30) return 0xFF8B6F1E;
        return 0xFF2E7D32;
    }
}
