package com.example.akaishi.menu;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.ClientBodyData;
import com.example.akaishi.life.body.PlayerBodyState;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基因管理器界面：自绘面板（vanilla 灰），展示已吸收的基因强化（最多 4 种生物来源）。
 * 每行 = 生物来源名 + 适配加成 + 卸载按钮（自绘矩形，点击发送 C2S 卸载包）；
 * 数据来自 ClientBodyData（打开瞬间 S2C 推送，卸载后服务端回推自动刷新）。
 */
public class AkaishiGeneManagerScreen extends AbstractContainerScreen<AkaishiGeneManagerMenu> {

    /** 界面尺寸（无背包，纯信息面板） */
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 160;

    /** 内容区布局：count 行与基因行均移入内面板，标题区（0..20）不再被挤占 */
    private static final int ROW_X = 10;
    private static final int COUNT_Y = 26;
    private static final int ROW_START_Y = 42;
    private static final int ROW_HEIGHT = 17;
    private static final int NAME_X = ROW_X;
    private static final int NAME_MAX_W = 78;
    private static final int BONUS_X = 94;
    /** 卸载按钮（右侧固定矩形） */
    private static final int BTN_X = 152;
    private static final int BTN_W = 38;
    private static final int BTN_H = 11;
    /** 加成文本可用宽度（到按钮左缘留缝，防压按钮） */
    private static final int BONUS_MAX_W = BTN_X - BONUS_X - 6;
    /** 突破激活状态区（基因槽下方、底部说明上方） */
    private static final int BT_Y = 106;
    private static final int BT_TIME_Y = 117;
    /** 突破区"受强化器官数"行 */
    private static final int BT_ORGANS_Y = 128;
    /** 突破行可用文本宽（面板内容宽，防溢出） */
    private static final int BT_TEXT_MAX_W = 180;
    private static final int TIP_Y = 140;
    /** 「结束突破」按钮（突破区第二行右侧） */
    private static final int BT_END_BTN_W = 44;
    private static final int BT_END_BTN_H = 11;
    private static final int BT_END_BTN_X = PANEL_W - 6 - BT_END_BTN_W;

    private static final int BG_COLOR = 0xFFC6C6C6;
    private static final int PANEL_COLOR = 0xFFB0B0B0;
    private static final int LINE_COLOR = 0xFF373737;

    public AkaishiGeneManagerScreen(AkaishiGeneManagerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    /** 已吸收条目（插入序） */
    private List<Map.Entry<String, Integer>> entries() {
        return new ArrayList<>(ClientBodyData.getGeneBonuses().entrySet());
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        gui.fill(this.leftPos, this.topPos, this.leftPos + PANEL_W, this.topPos + PANEL_H, BG_COLOR);
        // 内面板覆盖 count/基因行/突破区/底部说明，保证文字不"出框"
        gui.fill(this.leftPos + 6, this.topPos + 20, this.leftPos + PANEL_W - 6, this.topPos + 154, PANEL_COLOR);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);

        List<Map.Entry<String, Integer>> entries = entries();
        // 计数行（内面板首行，与标题分离避免重叠）
        Component count = Component.translatable("gui.akaishi.gene_manager.count",
                entries.size(), PlayerBodyState.GENE_CAPACITY);
        gui.drawString(this.font, count, this.leftPos + ROW_X, this.topPos + COUNT_Y, 0x3F3F3F, false);

        // 4 行基因槽
        for (int i = 0; i < PlayerBodyState.GENE_CAPACITY; i++) {
            int rowY = this.topPos + ROW_START_Y + i * ROW_HEIGHT;
            if (i < entries.size()) {
                Map.Entry<String, Integer> entry = entries.get(i);
                // 生物来源名
                Component name = EntityType.byString(entry.getKey())
                        .map(type -> (Component) type.getDescription())
                        .orElse(Component.literal(entry.getKey()));
                String plain = this.font.plainSubstrByWidth(name.getString(), NAME_MAX_W);
                gui.drawString(this.font, plain, this.leftPos + NAME_X, rowY - 2, 0xE0E0E0, false);
                // 适配加成（按可用宽截断，防止压到卸载按钮）
                String bonus = this.font.plainSubstrByWidth(
                        Component.translatable("gui.akaishi.gene_manager.bonus", entry.getValue()).getString(),
                        BONUS_MAX_W);
                gui.drawString(this.font, bonus, this.leftPos + BONUS_X, rowY - 2, 0x8B6F1E, false);
                // 卸载按钮（自绘）
                drawUnloadButton(gui, rowY, mouseX, mouseY);
            } else {
                // 空位提示较长：按面板可用宽截断，防止文字溢出右侧面板
                String empty = this.font.plainSubstrByWidth(
                        Component.translatable("gui.akaishi.gene_manager.row_empty").getString(),
                        BT_TEXT_MAX_W);
                gui.drawString(this.font, empty, this.leftPos + NAME_X, rowY - 2, 0x707070, false);
            }
        }
        // 突破激活状态区（激活中的基因强化与剩余时间）
        renderBreakthrough(gui, mouseX, mouseY);
        // 底部说明
        Component tip = Component.translatable("gui.akaishi.gene_manager.tip");
        gui.drawString(this.font, tip, this.leftPos + ROW_X, this.topPos + TIP_Y, 0x3F3F3F, false);
    }

    /** 突破激活状态区：激活中的基因强化（30 分钟临时），含剩余时间实时刷新与提前结束按钮 */
    private void renderBreakthrough(GuiGraphics gui, int mouseX, int mouseY) {
        long gameTime = Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getGameTime() : -1L;
        // 无镜像激活，或镜像已到期（服务端到期推送前的一段时间）均不显示，避免滞留"突破中 0:00"
        if (!ClientBodyData.hasActiveBreakthrough()
                || ClientBodyData.getBreakthroughRemainingTicks(gameTime) <= 0L) {
            return;
        }
        Component btName = EntityType.byString(ClientBodyData.getBreakthroughEntity())
                .map(type -> (Component) type.getDescription())
                .orElse(Component.literal(ClientBodyData.getBreakthroughEntity()));
        // 名称与数值整行按可用宽度截断，避免溢出面板右侧
        Component btInfo = Component.translatable("gui.akaishi.gene_manager.bt_row",
                btName.getString(), ClientBodyData.getBreakthroughExtra(), ClientBodyData.getBreakthroughPct());
        String infoPlain = this.font.plainSubstrByWidth(btInfo.getString(), BT_TEXT_MAX_W);
        gui.drawString(this.font, infoPlain, this.leftPos + ROW_X, this.topPos + BT_Y, 0x8B6F1E, false);
        // 第二行：剩余时间（mm:ss，每帧按客户端 gameTime 递减）+「结束」按钮（右侧）
        int secs = (int) (ClientBodyData.getBreakthroughRemainingTicks(gameTime) / 20L);
        String remain = String.format("%d:%02d", secs / 60, secs % 60);
        Component btTime = Component.translatable("gui.akaishi.gene_manager.bt_time", remain);
        gui.drawString(this.font, btTime, this.leftPos + ROW_X, this.topPos + BT_TIME_Y, 0x3F3F3F, false);
        drawBtEndButton(gui, mouseX, mouseY);
        // 第三行：当前受该突破强化的同源器官数量（直观确认激活有效范围）
        gui.drawString(this.font,
                Component.translatable("gui.akaishi.gene_manager.bt_organs", boostedOrganCount()),
                this.leftPos + ROW_X, this.topPos + BT_ORGANS_Y, 0x3F3F3F, false);
    }

    /** 当前正被突破强化的同源非原生器官数量（原生器官不受基因加成，不计入） */
    private int boostedOrganCount() {
        String bt = ClientBodyData.getBreakthroughEntity();
        if (bt.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = ClientBodyData.getOrgan(slot);
            if (organ.getItem() instanceof AkaishiOrganItem
                    && !AkaishiOrganItem.isNative(organ)
                    && bt.equals(AkaishiOrganItem.getEntityId(organ))) {
                count++;
            }
        }
        return count;
    }

    /** 自绘 vanilla 风格矩形按钮：上亮下暗边缘 + 灰色面板 + 居中标签 */
    private void drawRectButton(GuiGraphics gui, int x, int y, int w, int h,
                                boolean hover, Component label, int labelColor) {
        gui.fill(x, y, x + w, y + h, hover ? 0xFF9C9C9C : 0xFF8B8B8B);
        gui.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        gui.fill(x, y, x + 1, y + h - 1, 0xFFFFFFFF);
        gui.fill(x, y + h - 1, x + w, y + h, 0xFF373737);
        gui.fill(x + w - 1, y, x + w, y + h, 0xFF373737);
        gui.drawString(this.font, label, x + (w - this.font.width(label)) / 2, y + 1, labelColor, false);
    }

    /** 基因行卸载按钮 */
    private void drawUnloadButton(GuiGraphics gui, int rowY, int mouseX, int mouseY) {
        int x = this.leftPos + BTN_X;
        int y = rowY - 3;
        boolean hover = mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H;
        drawRectButton(gui, x, y, BTN_W, BTN_H, hover,
                Component.translatable("gui.akaishi.gene_manager.unload"), 0xFF3F3F3F);
    }

    /** 突破区「结束」按钮（提前结束激活，保留基因） */
    private void drawBtEndButton(GuiGraphics gui, int mouseX, int mouseY) {
        int x = this.leftPos + BT_END_BTN_X;
        int y = this.topPos + BT_TIME_Y - 2;
        boolean hover = mouseX >= x && mouseX < x + BT_END_BTN_W
                && mouseY >= y && mouseY < y + BT_END_BTN_H;
        drawRectButton(gui, x, y, BT_END_BTN_W, BT_END_BTN_H, hover,
                Component.translatable("gui.akaishi.gene_manager.bt_end"), 0xFFC05050);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 「结束突破」按钮：仅当激活仍在生效期（镜像未到期）时响应
            long gameTime = Minecraft.getInstance().level != null
                    ? Minecraft.getInstance().level.getGameTime() : -1L;
            int btX = this.leftPos + BT_END_BTN_X;
            int btY = this.topPos + BT_TIME_Y - 2;
            if (ClientBodyData.hasActiveBreakthrough()
                    && ClientBodyData.getBreakthroughRemainingTicks(gameTime) > 0L
                    && mouseX >= btX && mouseX < btX + BT_END_BTN_W
                    && mouseY >= btY && mouseY < btY + BT_END_BTN_H) {
                AkaishiGeneManagerSync.sendEndBreakthrough();
                return true;
            }
            // 基因行卸载按钮
            List<Map.Entry<String, Integer>> entries = entries();
            for (int i = 0; i < entries.size(); i++) {
                int y = this.topPos + ROW_START_Y + i * ROW_HEIGHT - 3;
                int x = this.leftPos + BTN_X;
                if (mouseX >= x && mouseX < x + BTN_W && mouseY >= y && mouseY < y + BTN_H) {
                    AkaishiGeneManagerSync.sendUnload(entries.get(i).getKey());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
