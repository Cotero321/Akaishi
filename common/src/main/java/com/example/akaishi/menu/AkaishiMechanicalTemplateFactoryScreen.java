package com.example.akaishi.menu;

import com.example.akaishi.life.mechanical.MechanicalOrganType;
import com.example.akaishi.life.mechanical.MechanicalPartType;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 模板制造厂界面：左上双能源小半截条（赤 y=24 / 生命 y=32）+ 右上升级槽（y=8）+ 器官/部件下拉选择 + 箭头进度。
 * 下拉式选择：点击框体展开选项浮层，浮层在 super.render 之后绘制以遮蔽背包槽位与物品，避免重合。
 */
public class AkaishiMechanicalTemplateFactoryScreen extends AbstractContainerScreen<AkaishiMechanicalTemplateFactoryMenu> {

    // 双能源小半截条（左上）
    private static final int BAR_X = 20, BAR_W = 60, BAR_H = 6;
    private static final int AKASHI_BAR_Y = 24, LIFE_BAR_Y = 32;
    // 升级槽（右上角 y=8）
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 8;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 8;
    // 槽位（与 Menu 坐标一致）
    private static final int INPUT_MOULD_X = 26, INPUT_SOLID_X = 44, INPUT_Y = 56;
    private static final int OUTPUT_X = 116, OUTPUT_Y = 56;
    // 箭头即进度（指向输出槽）
    private static final int ARROW_X = 86, ARROW_Y = 56, ARROW_W = 22, ARROW_H = 18;
    // 下拉选择框（器官 / 部件）
    private static final int DROP_ORGAN_X = 20, DROP_PART_X = 92, DROP_Y = 80, DROP_W = 66, DROP_H = 11;
    // 制作按钮（能源条下方、槽行上方，居中于进度箭头之上；三机坐标一致）
    private static final int CRAFT_X = 73, CRAFT_Y = 42, CRAFT_W = 30, CRAFT_H = 12;
    // 选择浮层：独立"下一层 UI"，覆盖机器全部内容区（y=6~104，含 y=8 升级槽），展开时整页压暗且仅可点选；不侵入背包 y=104
    private static final int OV_X = 6, OV_Y = 6, OV_W = 164, OV_H = 98;
    private static final int OV_TITLE_Y = 12, OV_GRID_Y = 26, OV_CELL_H = 20;
    // 不透明遮罩：半透明会透出槽位物品，用户反馈"没遮盖住"
    private static final int COLOR_DIM = 0xFF101010;
    // 面板尺寸（背包 y=104、快捷栏 y=168，底部留白 10px）
    private static final int PANEL_W = 176, PANEL_H = 196;

    /** 当前展开的选择浮层：-1 无、0 器官、1 部件 */
    private int dropdownOpen = -1;

    public AkaishiMechanicalTemplateFactoryScreen(AkaishiMechanicalTemplateFactoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        // 选择浮层置于最上层：槽位物品 z=250，浮层须抬 z 才能通过 LEQUAL 深度测试盖住物品。
        // 先 flush 把页面与物品刷入帧缓冲，再绘制抬 z 的浮层，避免渲染类型排序导致穿透。
        if (dropdownOpen >= 0) {
            gui.flush();
            drawSelectionOverlay(gui);
            gui.flush();
        }
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiWidgets.panel(gui, x, y, PANEL_W, PANEL_H);
        GuiWidgets.playerInventory(gui, x, y);

        // 双能源小半截条
        GuiWidgets.track(gui, x + BAR_X, y + AKASHI_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.track(gui, x + BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        GuiWidgets.bar(gui, x + BAR_X, y + AKASHI_BAR_Y, BAR_W, BAR_H, menu.getAkaishiEnergy(), menu.getAkaishiMax(), 0xFFE03030);
        GuiWidgets.bar(gui, x + BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H, menu.getLifeEnergy(), menu.getLifeMax(), 0xFF28B428);

        // 升级槽
        GuiWidgets.slotBox(gui, x + SPEED_SLOT_X, y + SPEED_SLOT_Y);
        GuiWidgets.slotBox(gui, x + ENERGY_SLOT_X, y + ENERGY_SLOT_Y);

        // 输入（通用模板 / 固态物）与输出（部位模板）
        GuiWidgets.slotBox(gui, x + INPUT_MOULD_X, y + INPUT_Y);
        GuiWidgets.slotBox(gui, x + INPUT_SOLID_X, y + INPUT_Y);
        GuiWidgets.slotBox(gui, x + OUTPUT_X, y + OUTPUT_Y);

        // 箭头即进度（指向输出槽）
        GuiWidgets.progressArrow(gui, x + ARROW_X, y + ARROW_Y, ARROW_W, ARROW_H, menu.getProgressPercent());

        // 制作按钮：单次制作，条件不满足或制作中置灰
        GuiWidgets.buttonText(gui, this.font, x + CRAFT_X, y + CRAFT_Y, CRAFT_W, CRAFT_H,
                Component.translatable("gui.akaishi.mech.craft"), menu.canCraft() && !menu.isCrafting());

        // 两个收起的下拉选择框（展开的浮层在 render() 末尾绘制）
        drawDropdown(gui, x + DROP_ORGAN_X, y + DROP_Y, DROP_W, DROP_H, 0);
        drawDropdown(gui, x + DROP_PART_X, y + DROP_Y, DROP_W, DROP_H, 1);
    }

    /** 绘制单个下拉框：框体 + 选中文本 + 下箭头 */
    private void drawDropdown(GuiGraphics gui, int x, int y, int w, int h, int type) {
        GuiWidgets.dropdown(gui, x, y, w, h);
        String name = type == 0
                ? Component.translatable("mechanical.organ." + MechanicalOrganType.values()[menu.getSelectedOrgan()].name().toLowerCase()).getString()
                : Component.translatable("mechanical.part." + MechanicalPartType.values()[menu.getSelectedPart()].name().toLowerCase()).getString();
        gui.drawString(this.font, Component.literal(name), x + 3, y + 2, 0xFF3F3F3F, false);
    }

    /** 绘制选择浮层：整页压暗 + 独立面板（标题 + 网格选项），选中项高亮 */
    private void drawSelectionOverlay(GuiGraphics gui) {
        int x = this.leftPos;
        int y = this.topPos;
        // 抬 z 至 500：除槽位物品（z=250）外，物品数量文字/浮动物品还会使用更高层级。
        // 浮层展开时已屏蔽 tooltip，因此可置于其上方，保证机器内容绝不穿透。
        gui.pose().pushPose();
        gui.pose().translate(0.0F, 0.0F, 500.0F);
        gui.fill(x, y, x + PANEL_W, y + PANEL_H, COLOR_DIM);
        GuiWidgets.panel(gui, x + OV_X, y + OV_Y, OV_W, OV_H);
        gui.drawString(this.font,
                Component.translatable(dropdownOpen == 0 ? "gui.akaishi.mech.select_organ" : "gui.akaishi.mech.select_part"),
                x + OV_X + 4, y + OV_TITLE_Y, 0xFF3F3F3F, false);
        if (dropdownOpen == 0) {
            drawGrid(gui, x, y, MechanicalOrganType.values(), menu.getSelectedOrgan(), 0, 3);
        } else {
            drawGrid(gui, x, y, MechanicalPartType.values(), menu.getSelectedPart(), 1, 2);
        }
        gui.pose().popPose();
    }

    /** 网格绘制选项（cols 列，选中项高亮），options 为器官/部件枚举数组 */
    private void drawGrid(GuiGraphics gui, int baseX, int baseY, Enum<?>[] options, int selected, int type, int cols) {
        int cellW = gridCellWidth(cols);
        for (int i = 0; i < options.length; i++) {
            int cx = baseX + OV_X + 4 + (i % cols) * (cellW + 4);
            int cy = baseY + OV_GRID_Y + (i / cols) * OV_CELL_H;
            boolean sel = i == selected;
            gui.fill(cx, cy, cx + cellW, cy + OV_CELL_H - 2, sel ? 0xFFC9C9C9 : 0xFF9E9E9E);
            gui.fill(cx, cy, cx + cellW, cy + 1, 0xFF373737);
            gui.fill(cx, cy + OV_CELL_H - 3, cx + cellW, cy + OV_CELL_H - 2, 0xFFFFFFFF);
            gui.fill(cx, cy, cx + 1, cy + OV_CELL_H - 2, 0xFF373737);
            gui.fill(cx + cellW - 1, cy, cx + cellW, cy + OV_CELL_H - 2, 0xFFFFFFFF);
            String key = (type == 0 ? "mechanical.organ." : "mechanical.part.") + options[i].name().toLowerCase();
            gui.drawString(this.font, Component.translatable(key), cx + 3, cy + 4, sel ? 0xFF202020 : 0xFF3F3F3F, false);
        }
    }

    /** 网格单元宽度：面板内边距 4，列间距 4 */
    private int gridCellWidth(int cols) {
        return (OV_W - 8 - (cols - 1) * 4) / cols;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        // 浮层展开时不再叠加任何提示（含槽位物品提示），避免盖住选项列表
        if (dropdownOpen >= 0) {
            return;
        }
        super.renderTooltip(gui, mouseX, mouseY);
        // 双能源条
        if (isHovering(BAR_X, AKASHI_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.mech.akaishi_bar",
                    EnergyFormat.format(menu.getAkaishiEnergy()), EnergyFormat.format(menu.getAkaishiMax())), mouseX, mouseY);
            return;
        }
        if (isHovering(BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.mech.life_bar",
                    EnergyFormat.format(menu.getLifeEnergy()), EnergyFormat.format(menu.getLifeMax())), mouseX, mouseY);
            return;
        }
        // 箭头即进度
        if (isHovering(ARROW_X, ARROW_Y, ARROW_W, ARROW_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.mech.arrow_progress",
                    menu.getProgressPercent()), mouseX, mouseY);
            return;
        }
        // 制作按钮：按状态区分提示（可制作 / 条件不满足 / 制作中）
        if (isHovering(CRAFT_X, CRAFT_Y, CRAFT_W, CRAFT_H, mouseX, mouseY)) {
            String key = menu.isCrafting() ? "gui.akaishi.mech.craft_hint_crafting"
                    : (menu.canCraft() ? "gui.akaishi.mech.craft_hint" : "gui.akaishi.mech.craft_hint_disabled");
            gui.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
            return;
        }
        // 下拉选择框
        if (isHovering(DROP_ORGAN_X, DROP_Y, DROP_W, DROP_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.mech.organ_drop"), mouseX, mouseY);
            return;
        }
        if (isHovering(DROP_PART_X, DROP_Y, DROP_W, DROP_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.mech.part_drop"), mouseX, mouseY);
            return;
        }
        // 升级槽（空槽提示用途）
        if (slotTip(gui, MachineUpgradeSlots.SLOT_SPEED, "gui.akaishi.upgrade.speed.hint", mouseX, mouseY)) {
            return;
        }
        if (slotTip(gui, MachineUpgradeSlots.SLOT_ENERGY, "gui.akaishi.upgrade.energy.hint", mouseX, mouseY)) {
            return;
        }
        // 机器槽（通用模板 / 固态物 / 输出）
        if (slotTip(gui, AbstractMechanicalMachineMenu.UPGRADE_SLOT_COUNT, "gui.akaishi.mech.template_mould_slot", mouseX, mouseY)) {
            return;
        }
        if (slotTip(gui, AbstractMechanicalMachineMenu.UPGRADE_SLOT_COUNT + 1, "gui.akaishi.mech.solid_slot", mouseX, mouseY)) {
            return;
        }
        slotTip(gui, AbstractMechanicalMachineMenu.UPGRADE_SLOT_COUNT + 2, "gui.akaishi.mech.output_slot", mouseX, mouseY);
    }

    /** 空槽悬浮提示：命中且槽位为空时渲染对应键，返回是否已提示 */
    private boolean slotTip(GuiGraphics gui, int index, String key, int mouseX, int mouseY) {
        if (index < 0 || index >= menu.slots.size()) {
            return false;
        }
        var slot = menu.slots.get(index);
        if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.getItem().isEmpty()) {
            gui.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int bx = this.leftPos;
        int by = this.topPos;
        // 选择浮层展开时：仅可点选，浮层外点击直接关闭返回上一步，且绝不穿透到背包槽位
        if (dropdownOpen >= 0) {
            if (button == 0 && inBox(mx, my, bx + OV_X, by + OV_Y, OV_W, OV_H)) {
                int cols = dropdownOpen == 0 ? 3 : 2;
                int count = dropdownOpen == 0 ? MechanicalOrganType.values().length : MechanicalPartType.values().length;
                int idx = optionAt(mx, my, cols, count);
                if (idx >= 0) {
                    if (dropdownOpen == 0) {
                        MechanicalSelectSync.sendSelection(menu.getBlockPos(), idx, menu.getSelectedPart());
                    } else {
                        MechanicalSelectSync.sendSelection(menu.getBlockPos(), menu.getSelectedOrgan(), idx);
                    }
                    dropdownOpen = -1;
                }
                // 浮层内空白也吞掉点击，保持展开（仅可选择）
                return true;
            }
            dropdownOpen = -1;
            return true;
        }
        if (button == 0) {
            // 制作按钮：条件满足且未在制作中时发送单次制作请求
            if (inBox(mx, my, bx + CRAFT_X, by + CRAFT_Y, CRAFT_W, CRAFT_H)) {
                if (menu.canCraft() && !menu.isCrafting()) {
                    MechanicalCraftSync.sendCraft(menu.getBlockPos());
                }
                return true;
            }
            // 点击下拉框体展开选择浮层
            if (inBox(mx, my, bx + DROP_ORGAN_X, by + DROP_Y, DROP_W, DROP_H)) {
                dropdownOpen = 0;
                return true;
            }
            if (inBox(mx, my, bx + DROP_PART_X, by + DROP_Y, DROP_W, DROP_H)) {
                dropdownOpen = 1;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** 计算点击落在浮层网格的哪一项（含列/行间距剔除），未命中返回 -1 */
    private int optionAt(double mx, double my, int cols, int count) {
        int cellW = gridCellWidth(cols);
        double gx = this.leftPos + OV_X + 4;
        double gy = this.topPos + OV_GRID_Y;
        if (mx < gx || my < gy) {
            return -1;
        }
        int col = (int) ((mx - gx) / (cellW + 4));
        int row = (int) ((my - gy) / OV_CELL_H);
        if (col < 0 || col >= cols || row < 0) {
            return -1;
        }
        // 落在列/行间距内不算命中
        if ((mx - gx) % (cellW + 4) >= cellW || (my - gy) % OV_CELL_H >= OV_CELL_H - 2) {
            return -1;
        }
        int idx = row * cols + col;
        return idx >= 0 && idx < count ? idx : -1;
    }
}
