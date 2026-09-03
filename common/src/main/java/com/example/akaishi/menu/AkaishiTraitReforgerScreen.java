package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiTraitReforgerBlockEntity;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.MutantTrait;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 词条重铸仪界面：生命能量条（绿）+ 词条序号按钮行（点击 C2S 选择重铸目标）+
 * 状态提示行 + 重铸进度条（黄）+ 器官/结晶/输出槽。
 * 数据来自 {@link AkaishiTraitReforgerMenu} 的 ContainerData。
 */
public class AkaishiTraitReforgerScreen extends AbstractContainerScreen<AkaishiTraitReforgerMenu> {

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;
    /** 能量条 */
    private static final int ENERGY_X = 16, ENERGY_Y = 16, ENERGY_W = 96, ENERGY_H = 8;
    /** 词条序号按钮（行内 1..4，点击选择目标词条） */
    private static final int BTN_X0 = 8, BTN_Y = 50, BTN_STEP = 42, BTN_W = 38, BTN_H = 14;
    /** 状态提示行（进度条上方） */
    private static final int STATUS_Y = 67;
    /** 重铸进度条 */
    private static final int PROGRESS_X = 56, PROGRESS_Y = 76, PROGRESS_W = 96, PROGRESS_H = 8;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，右上能量条下方空位；标签置于槽位上方） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 30;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 30;
    /** 器官输入槽的 Menu 槽位索引（机器槽第 3 个，词条读取用） */
    private static final int ORGAN_SLOT_INDEX = 2;

    private static final int COLOR_BG = 0xFFC6C6C6;
    private static final int COLOR_LINE = 0xFF373737;
    private static final int COLOR_ENERGY = 0xFF28B428;
    private static final int COLOR_PROGRESS = 0xFFFFD030;
    private static final int COLOR_SLOT = 0xFF8B8B8B;
    private static final int COLOR_BTN_SEL = 0xFF5B8731;
    private static final int COLOR_TEXT = 0xFF3F3F3F;
    private static final int COLOR_TEXT_SUB = 0xFF707070;
    private static final int COLOR_WARN = 0xFF9A7B2D;
    private static final int COLOR_DUAL = 0xFFC03030;

    public AkaishiTraitReforgerScreen(AkaishiTraitReforgerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.fill(x, y, x + PANEL_W, y + PANEL_H, COLOR_BG);

        // 存储联动浮层打开时：隐藏主 UI 内容（避免双 UI 叠加），仅保留背景 + 存储按钮 + 浮层 + 背包/联动槽
        if (menu.linkState != null && menu.linkState.open) {
            drawStorageButton(gui, x, y);
            drawStorageOverlay(gui, x, y);
            for (var slot : this.menu.slots) {
                if (!slot.isActive()) {
                    continue;
                }
                drawSlotBox(gui, x + slot.x, y + slot.y);
            }
            return;
        }

        gui.fill(x + 6, y + 14, x + PANEL_W - 6, y + PANEL_H - 7, 0xFFB0B0B0);

        // 能量条（绿）
        long energy = menu.getLifeEnergy();
        long cap = Math.max(1, menu.getLifeMax());
        int w = (int) (ENERGY_W * Math.max(0, Math.min(energy, cap)) / cap);
        gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + ENERGY_W, y + ENERGY_Y + ENERGY_H, COLOR_LINE);
        if (w > 0) {
            gui.fill(x + ENERGY_X, y + ENERGY_Y, x + ENERGY_X + w, y + ENERGY_Y + ENERGY_H, COLOR_ENERGY);
        }

        // 重铸进度条（黄）
        int p = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + PROGRESS_H, COLOR_LINE);
        if (p > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + p, y + PROGRESS_Y + PROGRESS_H, COLOR_PROGRESS);
        }

        // 词条序号按钮行（最多 4 条，对应器官承载上限 I=1 ~ IV=4）
        int count = Math.min(menu.getTraitCount(), 4);
        int target = menu.getTargetIndex();
        for (int i = 0; i < count; i++) {
            boolean selected = i == target;
            int bx = x + BTN_X0 + i * BTN_STEP;
            gui.fill(bx, y + BTN_Y, bx + BTN_W, y + BTN_Y + BTN_H, selected ? COLOR_BTN_SEL : COLOR_SLOT);
            gui.drawString(this.font, Component.literal(String.valueOf(i + 1)),
                    bx + BTN_W / 2 - 3, y + BTN_Y + 3, selected ? 0xFFFFFFFF : COLOR_TEXT, false);
        }

        // 状态提示行：无器官 / 无候选 / 正常提示
        drawStatusLine(gui, x, y);

        // 升级槽标签（槽位框已由下方循环自绘）
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y - 9, COLOR_TEXT_SUB, false);

        // 槽位背景框（机器 + 升级 + 背包 + 联动槽仅激活时）
        for (var slot : this.menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            drawSlotBox(gui, x + slot.x, y + slot.y);
        }

        // 存储联动：按钮 + 浮层
        if (menu.linkState != null) {
            drawStorageButton(gui, x, y);
            if (menu.linkState.open) {
                drawStorageOverlay(gui, x, y);
            }
        }
    }

    /** 状态提示行：按当前可用性给出引导（词条名经按钮悬停提示展示，保持行内简洁） */
    private void drawStatusLine(GuiGraphics gui, int x, int y) {
        Component msg;
        int color = COLOR_TEXT;
        if (menu.getTraitCount() <= 0) {
            msg = Component.translatable("gui.akaishi.trait_reforger.no_organ");
            color = COLOR_TEXT_SUB;
        } else {
            MutantTrait trait = currentTrait();
            if (trait != null && !MutantTrait.hasCandidates(trait.getRarity(),
                    AkaishiOrganItem.getMutations(organStack()))) {
                msg = Component.translatable("gui.akaishi.trait_reforger.no_candidate");
                color = COLOR_WARN;
            } else if (menu.getProgress() > 0) {
                msg = Component.translatable("gui.akaishi.trait_reforger.working");
            } else {
                msg = Component.translatable("gui.akaishi.trait_reforger.cost",
                        menu.getCrystalCost(), AkaishiTraitReforgerBlockEntity.LIFE_COST / 1000);
            }
        }
        gui.drawString(this.font, msg, x + 8, y + STATUS_Y, color, false);
    }

    /** 器官输入槽中的物品（客户端槽位同步副本，词条名/稀有度展示用） */
    private ItemStack organStack() {
        return this.menu.slots.get(ORGAN_SLOT_INDEX).getItem();
    }

    /** 当前目标词条（客户端副本；序号越界返回 null） */
    private MutantTrait currentTrait() {
        ItemStack organ = organStack();
        if (!(organ.getItem() instanceof AkaishiOrganItem)) {
            return null;
        }
        List<MutantTrait> mutations = AkaishiOrganItem.getMutations(organ);
        int idx = menu.getTargetIndex();
        return idx >= 0 && idx < mutations.size() ? mutations.get(idx) : null;
    }

    private void drawStorageButton(GuiGraphics gui, int x, int y) {
        boolean open = menu.linkState.open;
        gui.fill(x + PANEL_W - 40, y + 6, x + PANEL_W - 8, y + 16, open ? 0xFF5B8731 : 0xFFB0B0B0);
        gui.fill(x + PANEL_W - 40, y + 6, x + PANEL_W - 8, y + 7, COLOR_LINE);
        gui.drawString(this.font, Component.translatable("gui.akaishi.storage_link.open"),
                x + PANEL_W - 38, y + 7, open ? 0xFF2E7D32 : COLOR_TEXT, false);
    }

    private void drawStorageOverlay(GuiGraphics gui, int x, int y) {
        gui.fill(x + 4, y + 8, x + PANEL_W - 4, y + 72, COLOR_BG);
        gui.fill(x + 5, y + 9, x + PANEL_W - 5, y + 71, 0xFFA5A5A5);
        gui.drawString(this.font, Component.translatable(menu.linkState.nameKey), x + 8, y + 12, COLOR_TEXT, false);
        int pages = StorageLink.pageCount(menu.linkState);
        gui.drawString(this.font, (menu.linkState.page + 1) + "/" + pages, x + 8, y + 64, COLOR_TEXT, false);
        gui.drawString(this.font, "\u25C0", x + 112, y + 64,
                menu.linkState.canPagePrev() ? 0xFF2E7D32 : 0xFF8B8B8B, false);
        gui.drawString(this.font, "\u25B6", x + 130, y + 64,
                menu.linkState.canPageNext() ? 0xFF2E7D32 : 0xFF8B8B8B, false);
    }

    private void drawSlotBox(GuiGraphics gui, int x, int y) {
        gui.fill(x, y, x + 18, y + 18, COLOR_SLOT);
        gui.fill(x, y, x + 18, y + 1, COLOR_LINE);
        gui.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        gui.fill(x, y, x + 1, y + 18, COLOR_LINE);
        gui.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, 4, COLOR_TEXT, false);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, partialTick);
        this.renderTooltip(gui, mouseX, mouseY);

        // 存储浮层打开时：隐藏主 UI 悬停提示，避免叠在浮层上
        if (menu.linkState != null && menu.linkState.open) {
            return;
        }

        // 能量条悬停提示（数值信息）
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.life", menu.getLifeEnergy(), menu.getLifeMax()),
                    mouseX, mouseY);
        }
        // 重铸进度悬停提示
        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.trait_reforger.progress", menu.getProgress()),
                    mouseX, mouseY);
        }
        // 词条按钮悬停：显示目标词条名 + 稀有度 + 畸变标记
        int count = Math.min(menu.getTraitCount(), 4);
        for (int i = 0; i < count; i++) {
            int bx = this.leftPos + BTN_X0 + i * BTN_STEP;
            if (mouseX >= bx && mouseX < bx + BTN_W
                    && mouseY >= this.topPos + BTN_Y && mouseY < this.topPos + BTN_Y + BTN_H) {
                ItemStack organ = organStack();
                List<MutantTrait> mutations = AkaishiOrganItem.getMutations(organ);
                if (i < mutations.size()) {
                    MutantTrait trait = mutations.get(i);
                    gui.renderTooltip(this.font,
                            traitTooltip(trait).stream().map(Component::getVisualOrderText).toList(),
                            mouseX, mouseY);
                }
                break;
            }
        }
        // 升级槽悬停提示（速度/能量倍率随组件数量提升）
        if (isHovering(SPEED_SLOT_X, SPEED_SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.speed_slot", menu.getSpeedUpgradeCount(),
                            "x" + (1F + 0.125F * menu.getSpeedUpgradeCount())),
                    mouseX, mouseY);
        }
        if (isHovering(ENERGY_SLOT_X, ENERGY_SLOT_Y, 16, 16, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.upgrade.energy_slot", menu.getEnergyUpgradeCount(),
                            "x" + (1F + 0.5F * menu.getEnergyUpgradeCount())),
                    mouseX, mouseY);
        }
        // 存储按钮悬停提示
        if (menu.linkState != null
                && mouseX >= this.leftPos + PANEL_W - 40 && mouseX < this.leftPos + PANEL_W - 8
                && mouseY >= this.topPos + 6 && mouseY < this.topPos + 16) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.storage_link.tip",
                            Component.translatable(menu.linkState.nameKey)), mouseX, mouseY);
        }
    }

    /** 词条悬停提示：名称 + 稀有度（◆ 数量）+ 畸变警示 */
    private List<Component> traitTooltip(MutantTrait trait) {
        List<Component> tip = new java.util.ArrayList<>(2);
        tip.add(Component.translatable(trait.getNameKey()).copy()
                .withStyle(s -> s.withColor(trait.isDual() ? 0xFFC03030 : 0xFF2E7D32)));
        tip.add(Component.translatable("gui.akaishi.trait_reforger.rarity",
                "◆".repeat(Math.max(1, trait.getRarity())))
                .append(trait.isDual()
                        ? Component.translatable("gui.akaishi.trait_reforger.dual")
                        : Component.literal("")));
        return tip;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.linkState != null) {
            if (mouseX >= this.leftPos + PANEL_W - 40 && mouseX < this.leftPos + PANEL_W - 8
                    && mouseY >= this.topPos + 6 && mouseY < this.topPos + 16) {
                menu.linkState.open = !menu.linkState.open;
                return true;
            }
            if (menu.linkState.open) {
                if (mouseX >= this.leftPos + 112 && mouseX < this.leftPos + 126
                        && mouseY >= this.topPos + 64 && mouseY < this.topPos + 74) {
                    menu.linkState.flip(-1);
                    return true;
                }
                if (mouseX >= this.leftPos + 130 && mouseX < this.leftPos + 144
                        && mouseY >= this.topPos + 64 && mouseY < this.topPos + 74) {
                    menu.linkState.flip(1);
                    return true;
                }
                // 浮层打开：让位给浮层槽位点击
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        // 词条序号按钮：C2S 选择重铸目标（需方块坐标可用，且按钮在有效词条数内）
        if (button == 0 && menu.getBlockPos() != null) {
            int count = Math.min(menu.getTraitCount(), 4);
            for (int i = 0; i < count; i++) {
                int bx = this.leftPos + BTN_X0 + i * BTN_STEP;
                if (mouseX >= bx && mouseX < bx + BTN_W
                        && mouseY >= this.topPos + BTN_Y && mouseY < this.topPos + BTN_Y + BTN_H) {
                    AkaishiTraitReforgerSync.sendTarget(menu.getBlockPos(), i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
