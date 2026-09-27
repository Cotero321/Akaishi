package com.example.akaishi.menu;

import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 单输入单输出处理机器界面抽象基类（vanilla 灰自绘，176×198）。
 * <p>
 * 布局（GUI 相对坐标）——**遵循项目界面规则**：
 * <ul>
 *   <li><b>规则3</b>：升级槽固定面板右上角并排 y=8 —— 无线接收 116 / 速度 134 / 能量 152</li>
 *   <li><b>规则1</b>：赤能源条 / 加工进度条下移至机器槽下方避让升级槽（y=66 / 88）</li>
 *   <li><b>规则5</b>：元素不重叠 —— 标题右缘按右上角升级槽左框夹宽（长英文标题自动截断）</li>
 *   <li>标签行 y=28：输入 / 输出（左对齐槽位）+ 速 / 能 / 无（居中于各升级槽，置于槽位下方）</li>
 *   <li>机器行 y=40：输入 26 / 输出 98（统一 18×18 框）</li>
 *   <li>玩家背包 (8,124) / 快捷栏 (8,180)，背包标签 y=116</li>
 * </ul>
 * <b>「三套坐标」唯一真源</b>：Menu 槽位（{@code slot.x/y}）、自绘槽框、悬停命中区一律读
 * {@code menu.slots.get(i).x/y} ⇒ 三者不可能再各自漂移（历史教训：任一套不同步都同时表现为「错位 + 没提示」）。
 */
public abstract class AkaishiSingleSlotMachineScreen<T extends AkaishiSingleSlotMachineMenu>
        extends AbstractContainerScreen<T> {

    private static final int TEXT = 0xFF3F3F3F;
    /** 升级槽「已装」状态描边（青蓝，与系统识别色一致） */
    private static final int INSTALLED_OUTLINE = 0xFF2E9E8F;

    // ===== 仅「非槽位」元素需要布局常量；凡槽位一律取 Menu 槽位真值 =====
    /** 输入/输出标签行 y（槽位 y=40，槽框 39..57，标签置于其上） */
    private static final int IO_LABEL_Y = 28;
    /** 升级槽单字标签行 y（与输入/输出标签同排） */
    private static final int UPGRADE_LABEL_Y = 28;
    /** 数值条：左侧内标签 x + 右侧轨道 x / 宽 / 高 */
    private static final int BAR_LABEL_X = 20, BAR_X = 50, BAR_W = 106, BAR_H = 8;
    /** 赤能源条 / 加工进度条的 y */
    private static final int ENERGY_Y = 66, PROGRESS_Y = 88;
    /** 玩家背包标签 y（背包槽起点 y=124） */
    private static final int INV_LABEL_Y = 116;

    protected AkaishiSingleSlotMachineScreen(T menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    /**
     * 暗色背景层 + 显式触发 tooltip 绘制。
     * <p>
     * 1.20.1 的 {@code super.render} 只在「悬停格里有物品」时走默认提示，
     * 自绘的数值条 / 空槽 / 升级槽提示不会自动触发 —— 本族原先漏了这一步，
     * 表现为「打粉机内悬停任何元素都没有提示」。
     */
    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // 不透明背景面板（vanilla 灰，含四周内凹边框）
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        // 全部槽框（机器 5 格 + 玩家背包/快捷栏 36 格）按 Menu 槽位真值绘制 ⇒ 框与可交互区必然重合
        for (Slot slot : this.menu.slots) {
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }
        // 升级槽「已装」描边：速度/能量按堆叠数，无线接收按是否装入（未装则保持素框）
        outlineIfInstalled(gui, x, y, MachineUpgradeSlots.SLOT_SPEED, this.menu.getSpeedUpgradeCount() > 0);
        outlineIfInstalled(gui, x, y, MachineUpgradeSlots.SLOT_ENERGY, this.menu.getEnergyUpgradeCount() > 0);
        outlineIfInstalled(gui, x, y, MachineUpgradeSlots.SLOT_WIRELESS, this.menu.hasWirelessReceiver());
        // 赤能源条（红）
        drawBar(gui, x + BAR_X, y + ENERGY_Y, BAR_W, BAR_H,
                (float) menu.getEnergy() / Math.max(1, menu.getEnergyCapacity()), 0xFFE03030);
        // 加工进度条（金）
        drawBar(gui, x + BAR_X, y + PROGRESS_Y, BAR_W, BAR_H,
                (float) menu.getProgress() / Math.max(1, menu.getRequired()), 0xFFFFD030);
    }

    /** 绘制轨道 + 按比例填充的数值条（填充内缩 1px 保留内凹边） */
    private void drawBar(GuiGraphics gui, int x, int y, int w, int h, float ratio, int color) {
        GuiWidgets.track(gui, x, y, w, h);
        int fillW = (int) (w * Math.min(1.0F, Math.max(0.0F, ratio)));
        if (fillW > 0) {
            gui.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, color);
        }
    }

    /** 已装件的升级槽描 1px 青蓝边（槽框画在 slot.x-1/slot.y-1，18×18） */
    private void outlineIfInstalled(GuiGraphics gui, int x, int y, int index, boolean installed) {
        if (!installed || index < 0 || index >= this.menu.slots.size()) {
            return;
        }
        Slot slot = this.menu.slots.get(index);
        int bx = x + slot.x - 1;
        int by = y + slot.y - 1;
        gui.fill(bx, by, bx + 18, by + 1, INSTALLED_OUTLINE);
        gui.fill(bx, by + 17, bx + 18, by + 18, INSTALLED_OUTLINE);
        gui.fill(bx, by, bx + 1, by + 18, INSTALLED_OUTLINE);
        gui.fill(bx + 17, by, bx + 18, by + 18, INSTALLED_OUTLINE);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        // renderLabels 已 translate(leftPos,topPos)，此处为 GUI 相对坐标
        // 规则5（不重叠）：标题右缘按右上角升级槽左框夹宽，长英文标题按可用宽度截断
        gui.drawString(this.font,
                this.font.plainSubstrByWidth(this.title.getString(), titleMaxWidth()),
                this.titleLabelX, this.titleLabelY, TEXT, false);
        // 输入/输出标签（x 取槽位真值 ⇒ 与槽框严格对齐）
        drawSlotLabel(gui, AkaishiSingleSlotMachineMenu.SLOT_INPUT_INDEX, IO_LABEL_Y, "gui.akaishi.single_slot.input");
        drawSlotLabel(gui, AkaishiSingleSlotMachineMenu.SLOT_OUTPUT_INDEX, IO_LABEL_Y, "gui.akaishi.single_slot.output");
        // 升级槽单字符号（居中于 16px 物品区；原先完全无标签 ⇒ 玩家无法分辨三格用途）
        drawCenteredSlotLabel(gui, MachineUpgradeSlots.SLOT_WIRELESS, "gui.akaishi.single_slot.wireless_tag");
        drawCenteredSlotLabel(gui, MachineUpgradeSlots.SLOT_SPEED, "gui.akaishi.single_slot.speed_tag");
        drawCenteredSlotLabel(gui, MachineUpgradeSlots.SLOT_ENERGY, "gui.akaishi.single_slot.energy_tag");
        // 数值条内标签（原先两条无任何文字 ⇒ 分不清哪条是能量、哪条是进度）
        gui.drawString(this.font, Component.translatable("gui.akaishi.single_slot.energy_label"),
                BAR_LABEL_X, ENERGY_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.single_slot.progress_label"),
                BAR_LABEL_X, PROGRESS_Y, TEXT, false);
        // 玩家背包标签（背包槽起点 y=124）
        gui.drawString(this.font, Component.translatable("container.inventory"), 8, INV_LABEL_Y, TEXT, false);
    }

    /** 左对齐到槽位 x 的标签 */
    private void drawSlotLabel(GuiGraphics gui, int index, int labelY, String key) {
        gui.drawString(this.font, Component.translatable(key), this.menu.slots.get(index).x, labelY, TEXT, false);
    }

    /** 居中于槽位 16px 物品区的单字符号 */
    private void drawCenteredSlotLabel(GuiGraphics gui, int index, String key) {
        Component text = Component.translatable(key);
        int slotX = this.menu.slots.get(index).x;
        gui.drawString(this.font, text, slotX + (16 - this.font.width(text)) / 2, UPGRADE_LABEL_Y, TEXT, false);
    }

    /**
     * 标题可用最大宽度（规则5 不重叠）：右缘对齐右上角升级槽左框，留 2px 间隙。
     * <p>
     * 中文标题（5 字 ≈ 45px）远小于该值，实际只在英文长标题（如 "Akaishi Plant Cultivator"）
     * 时生效，避免与右上角升级槽相撞。
     */
    private int titleMaxWidth() {
        int left = this.imageWidth;
        for (int index : new int[] {MachineUpgradeSlots.SLOT_WIRELESS,
                MachineUpgradeSlots.SLOT_SPEED, MachineUpgradeSlots.SLOT_ENERGY}) {
            if (index >= 0 && index < this.menu.slots.size()) {
                left = Math.min(left, this.menu.slots.get(index).x - 1);
            }
        }
        return Math.max(0, left - this.titleLabelX - 2);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        // 数值条：轨道与命中区同源（BAR_X / BAR_Y / BAR_W / BAR_H）
        if (isHovering(BAR_X, ENERGY_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.energy",
                    formatEnergy(menu.getEnergy()), formatEnergy(menu.getEnergyCapacity())), mouseX, mouseY);
            return;
        }
        if (isHovering(BAR_X, PROGRESS_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.single_slot.progress",
                    menu.getProgress(), menu.getRequired()), mouseX, mouseY);
            return;
        }
        // 升级槽：任何状态都给提示（含「已装 / 空」）
        if (hoverSlot(MachineUpgradeSlots.SLOT_SPEED, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.upgrade.speed_slot",
                    menu.getSpeedUpgradeCount(), "x" + (1F + menu.getSpeedUpgradeCount())), mouseX, mouseY);
            return;
        }
        if (hoverSlot(MachineUpgradeSlots.SLOT_ENERGY, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.upgrade.energy_slot",
                    menu.getEnergyUpgradeCount(), "x" + (1F + 0.5F * menu.getEnergyUpgradeCount())), mouseX, mouseY);
            return;
        }
        if (hoverSlot(MachineUpgradeSlots.SLOT_WIRELESS, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.upgrade.wireless_slot",
                            Component.translatable(menu.hasWirelessReceiver()
                                    ? "gui.akaishi.upgrade.installed" : "gui.akaishi.upgrade.absent")),
                    mouseX, mouseY);
            return;
        }
        // 输入/输出空槽：仅空槽时提示用途（有物品时原版已显示物品名，避免重复）
        if (hoverSlot(AkaishiSingleSlotMachineMenu.SLOT_INPUT_INDEX, mouseX, mouseY)
                && menu.slots.get(AkaishiSingleSlotMachineMenu.SLOT_INPUT_INDEX).getItem().isEmpty()) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.single_slot.input_tip"), mouseX, mouseY);
            return;
        }
        if (hoverSlot(AkaishiSingleSlotMachineMenu.SLOT_OUTPUT_INDEX, mouseX, mouseY)
                && menu.slots.get(AkaishiSingleSlotMachineMenu.SLOT_OUTPUT_INDEX).getItem().isEmpty()) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.single_slot.output_tip"), mouseX, mouseY);
        }
    }

    /** 悬停判定读槽位真值（与自绘槽框、原版 hoveredSlot 同源） */
    private boolean hoverSlot(int index, int mouseX, int mouseY) {
        Slot slot = this.menu.slots.get(index);
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    /** 大数值缩写（复用统一 EnergyFormat） */
    private static String formatEnergy(long v) {
        return EnergyFormat.format(v);
    }
}
