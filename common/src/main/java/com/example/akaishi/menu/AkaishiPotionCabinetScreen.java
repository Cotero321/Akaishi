package com.example.akaishi.menu;

import com.example.akaishi.life.potion.PotionRegistry;
import com.example.akaishi.life.potion.PotionTemplate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * 药剂库界面：顶部模板筛选按钮（全部/永久/突破，客户端本地切换），
 * 下方 54 格药剂槽——不匹配筛选的槽位隐藏（isActive 控制渲染与交互）。
 */
public class AkaishiPotionCabinetScreen extends AbstractContainerScreen<AkaishiPotionCabinetMenu> {

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 220;
    private static final int BTN_Y = 6, BTN_H = 10, BTN_GAP = 4;

    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_LINE = 0xFF373737;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_SLOT_HIDDEN = 0xFF9E9E9E;
    private static final int COLOR_SELECTED = 0xFF5B8731;

    public AkaishiPotionCabinetScreen(AkaishiPotionCabinetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + PANEL_W, y + PANEL_H, COLOR_BG);
        gui.fill(x + 6, y + 14, x + PANEL_W - 6, y + PANEL_H - 7, 0xFFB0B0B0);

        // 筛选按钮：全部 + 各模板（选中高亮）
        String filter = menu.getFilterTemplate();
        int bx = 8;
        bx = drawFilterButton(gui, x + bx, y, "", "gui.akaishi.potion_cabinet.all", filter);
        for (PotionTemplate template : PotionRegistry.all()) {
            bx = drawFilterButton(gui, x + bx, y, template.id(), template.nameKey(), filter);
        }

        // 槽位背景框（隐藏槽画暗色）
        for (var slot : this.menu.slots) {
            boolean active = slot.isActive();
            drawSlotBox(gui, x + slot.x, y + slot.y, active ? COLOR_SLOT : COLOR_SLOT_HIDDEN);
        }
    }

    /** 绘制一个筛选按钮，返回下一个按钮的 x 偏移 */
    private int drawFilterButton(GuiGraphics gui, int x, int y, String id, String nameKey, String currentFilter) {
        boolean selected = currentFilter.equals(id);
        int w = this.font.width(Component.translatable(nameKey)) + 6;
        gui.fill(x, y + BTN_Y, x + w, y + BTN_Y + BTN_H, selected ? COLOR_SELECTED : 0xFFB0B0B0);
        gui.drawString(this.font, Component.translatable(nameKey), x + 3, y + BTN_Y + 1,
                selected ? 0xFF2E7D32 : 0xFF3F3F3F, false);
        return x + w + BTN_GAP;
    }

    private void drawSlotBox(GuiGraphics gui, int x, int y, int color) {
        gui.fill(x, y, x + 18, y + 18, color);
        gui.fill(x, y, x + 18, y + 1, COLOR_LINE);
        gui.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        gui.fill(x, y, x + 1, y + 18, COLOR_LINE);
        gui.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // 顶部为筛选按钮行，取消标题绘制避免与按钮重叠（库名经浮层/容器名可见）
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= this.topPos + BTN_Y && mouseY < this.topPos + BTN_Y + BTN_H) {
            // 命中筛选按钮 → 切换筛选
            List<String> filters = new ArrayList<>();
            filters.add("");
            for (PotionTemplate template : PotionRegistry.all()) {
                filters.add(template.id());
            }
            int bx = 8;
            for (String id : filters) {
                String nameKey = id.isEmpty()
                        ? "gui.akaishi.potion_cabinet.all"
                        : PotionRegistry.get(id).nameKey();
                int w = this.font.width(Component.translatable(nameKey)) + 6;
                if (mouseX >= this.leftPos + bx && mouseX < this.leftPos + bx + w) {
                    menu.setFilter(menu.getFilterTemplate().equals(id) ? "" : id);
                    return true;
                }
                bx += w + BTN_GAP;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
