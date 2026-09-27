package com.example.akaishi.menu;

import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 材料融合器界面（vanilla 灰自绘，176×198）。
 * <p>
 * 布局（GUI 相对坐标）——遵循项目界面规则：
 * <ul>
 *   <li><b>规则3</b>：升级槽固定面板右上角并排 y=8 —— 无线接收 116 / 速度 134 / 能量 152</li>
 *   <li>标签行 y=28：输入A(26) / 输入B(62) / 产物(98)（左对齐槽位）+ 速 / 能 / 无（居中于各升级槽下方）</li>
 *   <li>机器行 y=40：输入A 26 / 输入B 62 / 输出 98（统一 18×18 框）</li>
 *   <li><b>规则1/5</b>：赤能源条 / 加工进度条置于机器行下方（标签在条上方，两行互不重叠）</li>
 *   <li>玩家背包 (8,124) / 快捷栏 (8,180)，背包标签 y=116</li>
 * </ul>
 * <b>「三套坐标」唯一真源</b>：Menu 槽位（{@code slot.x/y}）、自绘槽框、悬停命中区一律读
 * {@code menu.slots.get(i).x/y}；两条数值条的绘制与命中区共用同一组常量。
 */
public class AkaishiMaterialFuserScreen extends AbstractContainerScreen<AkaishiMaterialFuserMenu> {

    private static final int TEXT = 0xFF3F3F3F;
    /** 升级槽「已装」状态描边（青蓝，与系统识别色一致） */
    private static final int INSTALLED_OUTLINE = 0xFF2E9E8F;
    /** 赤能源条填充（红） / 加工进度条填充（金） */
    private static final int ENERGY_FILL = 0xFFE03030;
    private static final int PROGRESS_FILL = 0xFFFFD030;

    /** 机器行标签 y（槽位 y=40，槽框 39..57，标签置于其上） */
    private static final int LABEL_Y = 28;
    /** 数值条：标签在上、轨道在下（避免中/英文标签与轨道横向相撞） */
    private static final int BAR_LABEL_X = 20, BAR_X = 20, BAR_W = 136, BAR_H = 8;
    private static final int ENERGY_LABEL_Y = 58, ENERGY_Y = 67;
    private static final int PROGRESS_LABEL_Y = 80, PROGRESS_Y = 89;
    /** 玩家背包标签 y（背包槽起点 y=124） */
    private static final int INV_LABEL_Y = 116;

    public AkaishiMaterialFuserScreen(AkaishiMaterialFuserMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 198;
    }

    /**
     * 背景层 + 显式触发 tooltip 绘制。
     * <p>1.20.1 的 {@code super.render} 只在「悬停格里有物品」时走默认提示，
     * 自绘的数值条 / 空槽 / 升级槽提示不会自动触发（教训见设计记忆 §22.7）。
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
        GuiWidgets.panel(gui, x, y, this.imageWidth, this.imageHeight);
        // 全部槽框（机器 6 格 + 玩家背包/快捷栏 36 格）按 Menu 槽位真值绘制 ⇒ 框与可交互区必然重合
        for (Slot slot : this.menu.slots) {
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }
        // 升级槽「已装」描边：速度/能量按堆叠数，无线接收按是否装入
        outlineIfInstalled(gui, x, y, MachineUpgradeSlots.SLOT_SPEED, this.menu.getSpeedUpgradeCount() > 0);
        outlineIfInstalled(gui, x, y, MachineUpgradeSlots.SLOT_ENERGY, this.menu.getEnergyUpgradeCount() > 0);
        outlineIfInstalled(gui, x, y, MachineUpgradeSlots.SLOT_WIRELESS, this.menu.hasWirelessReceiver());
        // 赤能源条（红）
        drawBar(gui, x + BAR_X, y + ENERGY_Y, BAR_W, BAR_H,
                (float) menu.getEnergy() / Math.max(1, menu.getEnergyCapacity()), ENERGY_FILL);
        // 加工进度条（金）
        drawBar(gui, x + BAR_X, y + PROGRESS_Y, BAR_W, BAR_H,
                (float) menu.getProgress() / Math.max(1, menu.getRequired()), PROGRESS_FILL);
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
        // 机器行槽位标签（x 取槽位真值 ⇒ 与槽框严格对齐）
        drawSlotLabel(gui, AkaishiMaterialFuserMenu.SLOT_INPUT_A_INDEX, "gui.akaishi.material_fuser.input_a");
        drawSlotLabel(gui, AkaishiMaterialFuserMenu.SLOT_INPUT_B_INDEX, "gui.akaishi.material_fuser.input_b");
        drawSlotLabel(gui, AkaishiMaterialFuserMenu.SLOT_OUTPUT_INDEX, "gui.akaishi.material_fuser.output");
        // 升级槽单字符号（居中于 16px 物品区，置于槽位下方）
        drawCenteredSlotLabel(gui, MachineUpgradeSlots.SLOT_WIRELESS, "gui.akaishi.single_slot.wireless_tag");
        drawCenteredSlotLabel(gui, MachineUpgradeSlots.SLOT_SPEED, "gui.akaishi.single_slot.speed_tag");
        drawCenteredSlotLabel(gui, MachineUpgradeSlots.SLOT_ENERGY, "gui.akaishi.single_slot.energy_tag");
        // 数值条标签（置于各自轨道上方，避免与轨道横向重叠）
        gui.drawString(this.font, Component.translatable("gui.akaishi.material_fuser.energy_label"),
                BAR_LABEL_X, ENERGY_LABEL_Y, TEXT, false);
        gui.drawString(this.font, Component.translatable("gui.akaishi.material_fuser.progress_label"),
                BAR_LABEL_X, PROGRESS_LABEL_Y, TEXT, false);
        // 玩家背包标签
        gui.drawString(this.font, Component.translatable("container.inventory"), 8, INV_LABEL_Y, TEXT, false);
    }

    /** 左对齐到槽位 x 的标签 */
    private void drawSlotLabel(GuiGraphics gui, int index, String key) {
        gui.drawString(this.font, Component.translatable(key), this.menu.slots.get(index).x, LABEL_Y, TEXT, false);
    }

    /** 居中于槽位 16px 物品区的单字符号 */
    private void drawCenteredSlotLabel(GuiGraphics gui, int index, String key) {
        Component text = Component.translatable(key);
        int slotX = this.menu.slots.get(index).x;
        gui.drawString(this.font, text, slotX + (16 - this.font.width(text)) / 2, LABEL_Y, TEXT, false);
    }

    /**
     * 标题可用最大宽度（规则5 不重叠）：右缘对齐右上角升级槽左框，留 2px 间隙。
     * 中文标题（5 字 ≈ 45px）远小于该值，实际只在英文长标题时生效。
     */
    private int titleMaxWidth() {
        int left = this.imageWidth;
        for (int index : new int[]{MachineUpgradeSlots.SLOT_WIRELESS, MachineUpgradeSlots.SLOT_SPEED,
                MachineUpgradeSlots.SLOT_ENERGY}) {
            if (index >= 0 && index < this.menu.slots.size()) {
                left = Math.min(left, this.menu.slots.get(index).x - 1);
            }
        }
        return Math.max(0, left - this.titleLabelX - 2);
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        super.renderTooltip(gui, mouseX, mouseY);
        // 赤能源条：绘制与命中区同源（BAR_X / ENERGY_Y / BAR_W / BAR_H）
        if (isHovering(BAR_X, ENERGY_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font, Component.translatable("gui.akaishi.energy",
                    EnergyFormat.format(menu.getEnergy()), EnergyFormat.format(menu.getEnergyCapacity())),
                    mouseX, mouseY);
            return;
        }
        // 加工进度条：进度 + 当前配方单次耗能
        if (isHovering(BAR_X, PROGRESS_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderComponentTooltip(this.font, java.util.List.of(
                    Component.translatable("gui.akaishi.material_fuser.progress",
                            menu.getProgress(), menu.getRequired()),
                    Component.translatable("gui.akaishi.material_fuser.cost",
                            EnergyFormat.format(menu.getCost()))), mouseX, mouseY);
            return;
        }
        // 升级槽：任何状态都给提示
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
        // 输入/输出空槽：仅空槽时提示用途（有物品时原版已显示物品名）
        if (hoverEmptySlot(AkaishiMaterialFuserMenu.SLOT_INPUT_A_INDEX, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.material_fuser.input_a_tip"), mouseX, mouseY);
            return;
        }
        if (hoverEmptySlot(AkaishiMaterialFuserMenu.SLOT_INPUT_B_INDEX, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.material_fuser.input_b_tip"), mouseX, mouseY);
            return;
        }
        if (hoverEmptySlot(AkaishiMaterialFuserMenu.SLOT_OUTPUT_INDEX, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.material_fuser.output_tip"), mouseX, mouseY);
        }
    }

    /** 悬停判定读槽位真值（与自绘槽框、原版 hoveredSlot 同源） */
    private boolean hoverSlot(int index, int mouseX, int mouseY) {
        Slot slot = this.menu.slots.get(index);
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    /** 空槽才提示用途（有物品时交给原版物品名提示） */
    private boolean hoverEmptySlot(int index, int mouseX, int mouseY) {
        return this.menu.slots.get(index).getItem().isEmpty() && hoverSlot(index, mouseX, mouseY);
    }
}
