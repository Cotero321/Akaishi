package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiLifeBreederBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 生命培育器界面：生命能量条（绿）+ 培养进度条（黄）+ 器官/序列/结晶/产物槽。
 * 进度条悬停提示展示当前成功率（由序列纯度决定，最高 70%）与消耗。
 * 数据来自 {@link AkaishiLifeBreederMenu} 的 ContainerData。
 */
public class AkaishiLifeBreederScreen extends AbstractContainerScreen<AkaishiLifeBreederMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/akaishi_energy_cell.png");

    private static final int PANEL_W = 176;

    private static final int LIFE_BAR_X = 20, LIFE_BAR_Y = 16, BAR_W = 136, BAR_H = 8;
    /** 培养进度条区域（机器槽行下方空档；避开槽名行，下方留给状态行） */
    private static final int PROGRESS_X = 60, PROGRESS_Y = 60, PROGRESS_W = 56, PROGRESS_H = 8;
    /** 机器区槽数（升级槽 2 + 器官/序列/结晶/产物槽 4） */
    private static final int MACHINE_SLOTS = 6;
    /** 升级槽 GUI 位置（与 Menu 槽位坐标一致，产物槽右侧同行） */
    private static final int SPEED_SLOT_X = 134, SPEED_SLOT_Y = 30;
    private static final int ENERGY_SLOT_X = 152, ENERGY_SLOT_Y = 30;
    /** 菜单槽位索引（Menu 先加 2 升级槽，再按器官/序列/结晶/产物顺序加业务槽） */
    private static final int IDX_ORGAN = 2, IDX_SEQUENCE = 3, IDX_CRYSTAL = 4, IDX_OUTPUT = 5;
    /** 机器槽行下方布局：槽位底 y48 → 槽名 y49、进度条 y60、状态行 y70（与背包区 y84 不重叠） */
    private static final int CAPTION_Y = 49;
    private static final int STATUS_Y = 70;
    private static final int STATUS_X = 8;
    /** 状态行文案最大宽度 */
    private static final int STATUS_MAX_W = PANEL_W - STATUS_X - 6;

    public AkaishiLifeBreederScreen(AkaishiLifeBreederMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 大数值缩写：>=1M 百万，>=1K 千，否则原样输出 */
    private static String formatEnergy(long v) {
        if (v >= 1_000_000L) {
            return trim(v / 1.0e6) + "M";
        }
        if (v >= 1_000L) {
            return trim(v / 1.0e3) + "K";
        }
        return String.valueOf(v);
    }

    private static String trim(double d) {
        if (Math.abs(d - Math.round(d)) < 0.05) {
            return String.valueOf((long) Math.round(d));
        }
        return String.format(Locale.ROOT, "%.1f", d);
    }

    /** 状态行内容 + 颜色（金=培养中 / 绿=材料齐备 / 灰=等待缺料） */
    private record BreederStatus(Component text, int color) {
    }

    /** 在槽位下方居中绘制一行短说明（槽宽 18，超宽自动截断） */
    private void drawCaption(GuiGraphics gui, int slotX, int textY, String key) {
        Component c = Component.translatable(key);
        int w = Math.min(this.font.width(c), 18);
        gui.drawString(this.font, c, slotX + (18 - w) / 2, textY, 0xFF3F3F3F, false);
    }

    /** 汇总当前培养状态：运行中成功率 / 材料齐备 / 等待缺料原因（客户端按已同步槽位与数据判断） */
    private BreederStatus statusOf() {
        if (menu.getProgress() > 0) {
            return new BreederStatus(clip(
                    Component.translatable("gui.akaishi.life_breeder.status_run", menu.getSuccessRate())), 0xFF8B6F1E);
        }
        ItemStack organ = menu.slots.get(IDX_ORGAN).getItem();
        if (!(organ.getItem() instanceof AkaishiOrganItem) || !AkaishiOrganItem.canMutate(organ)) {
            return new BreederStatus(Component.translatable("gui.akaishi.life_breeder.need_organ"), 0xFF707070);
        }
        ItemStack seq = menu.slots.get(IDX_SEQUENCE).getItem();
        if (!(seq.getItem() instanceof AkaishiGeneSequenceItem)
                || menu.getPurity() < AkaishiLifeBreederBlockEntity.MIN_PURITY
                || AkaishiGeneSequenceItem.getGroup(seq) != AkaishiOrganItem.getSource(organ)) {
            return new BreederStatus(Component.translatable("gui.akaishi.life_breeder.need_seq"), 0xFF707070);
        }
        ItemStack crystal = menu.slots.get(IDX_CRYSTAL).getItem();
        if (!crystal.is(ModItems.exhaustedCrystal.get())
                || crystal.getCount() < ModConfig.lifeBreederCrystalCost) {
            return new BreederStatus(Component.translatable("gui.akaishi.life_breeder.need_crystal",
                    ModConfig.lifeBreederCrystalCost), 0xFF707070);
        }
        if (menu.getLifeEnergy() < ModConfig.lifeBreederLifeCost) {
            return new BreederStatus(Component.translatable("gui.akaishi.life_breeder.need_energy",
                    formatEnergy(ModConfig.lifeBreederLifeCost)), 0xFF707070);
        }
        if (!menu.slots.get(IDX_OUTPUT).getItem().isEmpty()) {
            return new BreederStatus(Component.translatable("gui.akaishi.life_breeder.need_output"), 0xFF707070);
        }
        return new BreederStatus(clip(Component.translatable("gui.akaishi.life_breeder.status_ready",
                menu.getSuccessRate(), menu.getPurity())), 0xFF2E7D32);
    }

    /** 状态行超宽截断（保留尾部不溢出面板） */
    private Component clip(Component text) {
        return this.font.width(text) <= STATUS_MAX_W
                ? text
                : Component.literal(this.font.plainSubstrByWidth(text.getString(), STATUS_MAX_W));
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // 存储联动浮层打开时：隐藏主 UI 内容（避免双 UI 叠加），仅保留背景 + 存储按钮 + 浮层
        if (menu.linkState != null && menu.linkState.open) {
            drawStorageButton(gui, x, y);
            drawStorageOverlay(gui, x, y);
            return;
        }

        // 机器槽位框（贴图无图形，自绘补齐；0/1=升级槽 2~5=器官/序列/结晶/产物槽）
        for (int i = 0; i < MACHINE_SLOTS; i++) {
            var slot = menu.slots.get(i);
            GuiWidgets.slotBox(gui, x + slot.x, y + slot.y);
        }
        // 机器业务槽说明（槽位下方一行，避免空槽无法辨认功能）
        drawCaption(gui, x + menu.slots.get(IDX_ORGAN).x, y + CAPTION_Y, "gui.akaishi.life_breeder.slot_organ");
        drawCaption(gui, x + menu.slots.get(IDX_SEQUENCE).x, y + CAPTION_Y, "gui.akaishi.life_breeder.slot_seq");
        drawCaption(gui, x + menu.slots.get(IDX_CRYSTAL).x, y + CAPTION_Y, "gui.akaishi.life_breeder.slot_crystal");
        drawCaption(gui, x + menu.slots.get(IDX_OUTPUT).x, y + CAPTION_Y, "gui.akaishi.life_breeder.slot_out");
        // 升级槽标签（槽位下方，避开上方能量条）
        gui.drawString(this.font, Component.translatable("gui.akaishi.upgrade.tag"),
                x + SPEED_SLOT_X, y + SPEED_SLOT_Y + 18, 0xFF707070, false);
        // 生命能量条（绿）
        GuiWidgets.track(gui, x + LIFE_BAR_X, y + LIFE_BAR_Y, BAR_W, BAR_H);
        long life = menu.getLifeEnergy();
        long cap = Math.max(1, menu.getLifeMax());
        int lifeWidth = (int) (BAR_W * Math.max(0, Math.min(life, cap)) / cap);
        if (lifeWidth > 0) {
            gui.fill(x + LIFE_BAR_X, y + LIFE_BAR_Y, x + LIFE_BAR_X + lifeWidth, y + LIFE_BAR_Y + BAR_H, 0xFF28B428);
        }
        // 培养进度条（黄）
        GuiWidgets.track(gui, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H);
        int progressWidth = (int) (PROGRESS_W * menu.getProgress() / 100.0F);
        if (progressWidth > 0) {
            gui.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + progressWidth, y + PROGRESS_Y + PROGRESS_H, 0xFFFFD030);
        }
        // 状态行：培养中成功率 / 材料齐备 / 缺料原因（颜色随状态，让机器功能与缺料一目了然）
        BreederStatus status = statusOf();
        gui.drawString(this.font, status.text(), x + STATUS_X, y + STATUS_Y, status.color(), false);
    }

    /** 右上角"存储"开关按钮 */
    private void drawStorageButton(GuiGraphics gui, int x, int y) {
        boolean open = menu.linkState.open;
        gui.fill(x + PANEL_W - 40, y + 6, x + PANEL_W - 8, y + 16, open ? 0xFF5B8731 : 0xFFB0B0B0);
        gui.drawString(this.font, Component.translatable("gui.akaishi.storage_link.open"),
                x + PANEL_W - 38, y + 7, open ? 0xFF2E7D32 : 0xFF3F3F3F, false);
    }

    /** 存储联动浮层：标题 + 18 槽位框 + 页码 + 翻页按钮 */
    private void drawStorageOverlay(GuiGraphics gui, int x, int y) {
        gui.fill(x + 4, y + 8, x + PANEL_W - 4, y + 72, 0xFFC6C6C6);
        gui.fill(x + 5, y + 9, x + PANEL_W - 5, y + 71, 0xFFA5A5A5);
        gui.drawString(this.font, Component.translatable(menu.linkState.nameKey), x + 8, y + 12, 0xFF3F3F3F, false);
        // 槽位框（两行九列）
        for (int i = 0; i < StorageLink.PAGE_SLOTS; i++) {
            int sx = x + StorageLink.SLOT_X + (i % 9) * 18;
            int sy = y + StorageLink.SLOT_Y + (i / 9) * 18;
            gui.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            gui.fill(sx, sy, sx + 18, sy + 1, 0xFF373737);
            gui.fill(sx, sy + 17, sx + 18, sy + 18, 0xFF373737);
            gui.fill(sx, sy, sx + 1, sy + 18, 0xFF373737);
            gui.fill(sx + 17, sy, sx + 18, sy + 18, 0xFF373737);
        }
        int pages = StorageLink.pageCount(menu.linkState);
        gui.drawString(this.font, (menu.linkState.page + 1) + "/" + pages, x + 8, y + 64, 0xFF3F3F3F, false);
        gui.drawString(this.font, "\u25C0", x + 112, y + 64,
                menu.linkState.canPagePrev() ? 0xFF2E7D32 : 0xFF8B8B8B, false);
        gui.drawString(this.font, "\u25B6", x + 130, y + 64,
                menu.linkState.canPageNext() ? 0xFF2E7D32 : 0xFF8B8B8B, false);
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        gui.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF3F3F3F, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && menu.linkState != null) {
            // 存储开关按钮
            if (mouseX >= this.leftPos + PANEL_W - 40 && mouseX < this.leftPos + PANEL_W - 8
                    && mouseY >= this.topPos + 6 && mouseY < this.topPos + 16) {
                menu.linkState.open = !menu.linkState.open;
                return true;
            }
            if (menu.linkState.open) {
                // 浮层翻页
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
                // 浮层打开：其余点击交给槽位处理（联动槽优先）
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

        // 悬停提示：能量条 / 进度条（含成功率与消耗）
        if (isHovering(LIFE_BAR_X, LIFE_BAR_Y, BAR_W, BAR_H, mouseX, mouseY)) {
            gui.renderTooltip(this.font,
                    Component.translatable("gui.akaishi.life",
                            formatEnergy(menu.getLifeEnergy()), formatEnergy(menu.getLifeMax())),
                    mouseX, mouseY);
        }
        if (isHovering(PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H, mouseX, mouseY)) {
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.akaishi.life_breeder.progress", menu.getProgress()));
            // 有合格序列（纯度达标）时才展示成功率与消耗，否则提示配方不全
            if (menu.getPurity() >= AkaishiLifeBreederBlockEntity.MIN_PURITY) {
                tip.add(Component.translatable("gui.akaishi.life_breeder.rate", menu.getSuccessRate()));
                tip.add(Component.translatable("gui.akaishi.life_breeder.cost",
                        ModConfig.lifeBreederCrystalCost,
                        formatEnergy(ModConfig.lifeBreederLifeCost)));
            } else {
                tip.add(Component.translatable("gui.akaishi.life_breeder.no_sequence"));
            }
            gui.renderComponentTooltip(this.font, tip, mouseX, mouseY);
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
}
