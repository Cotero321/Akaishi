package com.example.akaishi.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 微缩矩阵终端界面 · 加工详情页协作类（纯机械搬迁自 {@link AkaishiMiniMatrixTerminalScreen}）。
 * <p>
 * 承接详情页（点加工列表格子进入）的成块渲染与账目状态：物品贴图 + 合成方式（直接材料格）
 * + 成本 + 数量输入联动 + 开始加工；工序来源悬停、账目匹配（{@code planFor}）与
 * 数量请求节流（{@code refreshDetailPlan}）也在本类。
 * <p>
 * 详情页布局常量（DETAIL_*）真源落在本类；材料格距与列表页共用
 * {@link MiniMatrixCraftList#CRAFT_CELL}（同名别名，方法体与原实现逐位一致）。
 */
final class MiniMatrixCraftDetail {

    // 加工详情页（点格子进入）：贴图 + 合成方式（直接材料格）+ 成本 + 数量 + 开始加工
    // （DETAIL_AMOUNT_* / DETAIL_BTN_H / DETAIL_OP_Y 为包私有：宿主 init 的数量框创建按原坐标引用同一真源）
    private static final int DETAIL_TOP = 30;
    static final int DETAIL_BTN_H = 12;
    private static final int DETAIL_BACK_X = 8;
    private static final int DETAIL_BACK_W = 32;
    private static final int DETAIL_ICON_X = 46;
    private static final int DETAIL_NAME_X = 68;
    private static final int DETAIL_RECIPE_Y = 52;
    private static final int DETAIL_GRID_X = 9;
    private static final int DETAIL_GRID_Y = 62;
    private static final int DETAIL_COLS = 8;
    /** 材料格上限（两行 × 8 列） */
    private static final int DETAIL_MATERIALS = 16;
    private static final int DETAIL_COST_Y = 100;
    static final int DETAIL_OP_Y = 109;
    static final int DETAIL_AMOUNT_X = 40;
    static final int DETAIL_AMOUNT_W = 48;
    private static final int DETAIL_START_X = 100;
    private static final int DETAIL_START_W = 68;
    /** 单次加工的数量上限（与 {@code VirtualCraftPlanner.MAX_TARGET_COUNT} 同口径，防手输大数把服务端算爆） */
    static final int DETAIL_AMOUNT_MAX = 9999;

    /** 材料格距与加工列表页同一口径（别名引用，真源见 {@link MiniMatrixCraftList#CRAFT_CELL}） */
    private static final int CRAFT_CELL = MiniMatrixCraftList.CRAFT_CELL;

    /** 文本配色别名（真源见 {@link MiniMatrixTerminalSections}） */
    private static final int TEXT = MiniMatrixTerminalSections.TEXT;
    private static final int TEXT_DIM = MiniMatrixTerminalSections.TEXT_DIM;
    private static final int TEXT_RED = MiniMatrixTerminalSections.TEXT_RED;
    /** 面板文本可用宽度（真源见 {@link MiniMatrixTerminalSections#CONTENT_W}） */
    private static final int CONTENT_W = MiniMatrixTerminalSections.CONTENT_W;

    /** 宿主界面（访问 menu / font / leftPos / topPos / amountBox） */
    private final AkaishiMiniMatrixTerminalScreen scr;
    /** 加工列表页（读查询串） */
    private final MiniMatrixCraftList list;

    /** 详情页正在看的物品（null = 在加工列表页） */
    private Item detailItem;
    /** 当前详情是哪一次数量请求回来的（用于判断输入框里的数量变了要不要重算） */
    private int detailFetchedAmount = -1;
    /** 上一次见到的账（对象身份）：用于识别"账被目录刷新作废"，从而重取一次 */
    @Nullable
    private AkaishiMatrixCraftSync.PlanView lastSeenPlan;

    MiniMatrixCraftDetail(AkaishiMiniMatrixTerminalScreen scr, MiniMatrixCraftList list) {
        this.scr = scr;
        this.list = list;
    }

    /** 是否正处在详情页（宿主的渲染/按键/点击分派据此分流） */
    boolean hasDetail() {
        return detailItem != null;
    }

    /**
     * 详情页数量与"已取回的数量"不一致时，向服务端重取一次账（按新数量）。
     * <p>数量输入框的软节流：每 tick 最多发一次"按新数量重算"的请求（宿主 containerTick 调用）。
     */
    void refreshDetailPlan() {
        if (detailItem == null) {
            return;
        }
        // 目录刷新会把账作废（{@code acceptCraftCatalog} 清 craftPlan）：检测到"原本有账、现在没了"
        // 就重取一次，否则详情页会一直停在"正在规划…"。用对象身份判断，避免每 tick 空发请求。
        AkaishiMatrixCraftSync.PlanView plan = scr.menu().craftPlan();
        if (plan != lastSeenPlan) {
            lastSeenPlan = plan;
            if (plan == null) {
                detailFetchedAmount = -1;
            }
        }
        int amount = detailAmount();
        if (amount == detailFetchedAmount) {
            return;
        }
        detailFetchedAmount = amount;
        AkaishiMatrixCraftSync.sendAction(scr.menu().containerId, AkaishiMatrixCraftSync.ACTION_SELECT,
                list.query(), new ItemStack(detailItem, amount));
    }

    /** 详情页当前数量（输入框非法值一律回落到 1） */
    private int detailAmount() {
        if (scr.amountBox == null) {
            return 1;
        }
        String text = scr.amountBox.getValue().trim();
        if (text.isEmpty()) {
            return 1;
        }
        try {
            return Math.max(1, Math.min(Integer.parseInt(text), DETAIL_AMOUNT_MAX));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** 打开物品详情页：先按数量 1 取一次账（异步），数量框可直接改 */
    void openCraftDetail(Item item) {
        detailItem = item;
        detailFetchedAmount = 1;
        if (scr.amountBox != null) {
            scr.amountBox.setValue("1");
            // 全选：直接敲数字即可替换，不必先删掉这个 1
            scr.amountBox.setCursorPosition(0);
            scr.amountBox.setHighlightPos(1);
        }
        AkaishiMatrixCraftSync.sendAction(scr.menu().containerId, AkaishiMatrixCraftSync.ACTION_SELECT,
                list.query(), new ItemStack(item, 1));
    }

    /** 回到加工列表 */
    void closeCraftDetail() {
        detailItem = null;
        detailFetchedAmount = -1;
        if (scr.amountBox != null) {
            scr.amountBox.setFocused(false);
        }
    }

    /** 详情页点击：返回 / 开始加工（材料格只做展示，说明走悬停）。返回是否已消费这次点击 */
    boolean handleCraftDetailClick(double mouseX, double mouseY) {
        if (AkaishiMiniMatrixTerminalScreen.isIn(scr.leftPos() + DETAIL_BACK_X, scr.topPos() + DETAIL_TOP,
                DETAIL_BACK_W, DETAIL_BTN_H, mouseX, mouseY)) {
            closeCraftDetail();
            return true;
        }
        if (AkaishiMiniMatrixTerminalScreen.isIn(scr.leftPos() + DETAIL_START_X, scr.topPos() + DETAIL_OP_Y,
                DETAIL_START_W, DETAIL_BTN_H, mouseX, mouseY)) {
            // 数量随输入框走：服务端按该数量重新规划、扣料、入库。
            // 必须与绘制端同判 affordable：置灰的按钮若能点，命中区就与视觉状态不一致（服务端会拒，但体验是坏的）
            AkaishiMatrixCraftSync.PlanView ready = planFor(detailItem, detailAmount());
            if (ready != null && ready.affordable()) {
                AkaishiMatrixCraftSync.sendAction(scr.menu().containerId, AkaishiMatrixCraftSync.ACTION_START,
                        list.query(), new ItemStack(detailItem, detailAmount()));
                closeCraftDetail();
            }
            return true;
        }
        return false;
    }

    /**
     * 加工详情页：物品贴图 + 合成方式（直接材料格，缺的标红）+ 成本 + 数量 + 开始加工。
     * <p>
     * 只画"最上面那一层"的直接材料（玩家看得懂"要 3 木板 + 2 木棍"）；其余细节走悬停提示。
     */
    void renderCraftDetail(GuiGraphics gui) {
        GuiWidgets.buttonText(gui, scr.font(), DETAIL_BACK_X, DETAIL_TOP, DETAIL_BACK_W, DETAIL_BTN_H,
                Component.translatable("gui.akaishi.matrix.craft.detail.back"), true);
        ItemStack icon = new ItemStack(detailItem);
        GuiWidgets.slotBox(gui, DETAIL_ICON_X, DETAIL_TOP);
        gui.renderItem(icon, DETAIL_ICON_X, DETAIL_TOP);
        gui.drawString(scr.font(),
                scr.font().plainSubstrByWidth(icon.getHoverName().getString(), 168 - DETAIL_NAME_X),
                DETAIL_NAME_X, DETAIL_TOP + 4, TEXT, false);
        gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.craft.detail.recipe"),
                8, DETAIL_RECIPE_Y, TEXT_DIM, false);
        // 账必须与"当前物品 + 当前件数"都对得上：旧包（上一件、或上一个数量）一律不显示
        AkaishiMatrixCraftSync.PlanView plan = planFor(detailItem, detailFetchedAmount);
        if (plan == null) {
            // "算不出来"与"还没算完"分开说，否则玩家会一直等一个永远不会来的结果
            gui.drawString(scr.font(), Component.translatable(planUnavailable(detailFetchedAmount)
                            ? "gui.akaishi.matrix.craft.unresolvable"
                            : "message.akaishi.matrix.craft.planning"),
                    8, DETAIL_GRID_Y + 4, TEXT_DIM, false);
            return;
        }
        List<AkaishiMatrixCraftSync.LeafView> materials = plan.leaves();
        for (int i = 0; i < materials.size() && i < DETAIL_MATERIALS; i++) {
            int cellX = DETAIL_GRID_X + (i % DETAIL_COLS) * CRAFT_CELL;
            int cellY = DETAIL_GRID_Y + (i / DETAIL_COLS) * CRAFT_CELL;
            AkaishiMatrixCraftSync.LeafView material = materials.get(i);
            GuiWidgets.slotBox(gui, cellX, cellY);
            gui.renderItem(material.stack(), cellX, cellY);
            GuiWidgets.amountLabel(gui, scr.font(), cellX, cellY, Long.toString(material.count()));
            if (!material.enough()) {
                gui.fill(cellX, cellY, cellX + 16, cellY + 16, 0x66FF2020); // 缺料：红罩
            }
        }
        // 成本行：三个数都走统一缩写（加上机器能耗/耗时后量级可达百万级，原样输出会撑出面板）。
        // 取整方向按 EnergyFormat 的既有约定：能量是<b>应付量</b>（向上，不误导少备能量）、
        // 材料 IP 是只读占用量（向下，绝不暗示还有余量）、耗时是中性的读数。
        // 行尾按"最硬的拦路条件"替换（保证整行不超出面板宽度）：
        // 缺机台 > 要付生命能量（纯能量配方没有材料，材料 IP 恒为 0）> 材料 IP。
        String seconds = EnergyFormat.format(plan.ticks() / 20L);
        String chishi = EnergyFormat.formatCeil(plan.energy());
        Component costLine;
        if (plan.machineMissing()) {
            costLine = Component.translatable("gui.akaishi.matrix.craft.detail.cost_machine", seconds, chishi);
        } else if (plan.powerMissing()) {
            // 机台在场但拿不到能量（终端缺「操控」/「联动」）：真机加工下机台转不起来，与缺机台同为硬拦路
            costLine = Component.translatable("gui.akaishi.matrix.craft.detail.cost_power", seconds, chishi);
        } else if (plan.lifeEnergy() > 0L) {
            costLine = Component.translatable("gui.akaishi.matrix.craft.detail.cost_life", seconds, chishi,
                    EnergyFormat.formatCeil(plan.lifeEnergy()));
        } else {
            costLine = Component.translatable("gui.akaishi.matrix.craft.detail.cost", seconds, chishi,
                    EnergyFormat.formatFloor(plan.materialIp()));
        }
        // 必须按内容宽度截断：中文模板约 116px 尚可，英文模板（"%s s · energy %s · material IP %s"）本身已约 152px，
        // 再加三个缩写值必然超出 CONTENT_W(160)。原先注释写了"保证不超宽"，但这里其实没有任何宽度约束。
        gui.drawString(scr.font(), scr.font().plainSubstrByWidth(costLine.getString(), CONTENT_W),
                8, DETAIL_COST_Y, plan.affordable() ? TEXT : TEXT_RED, false);
        gui.drawString(scr.font(), Component.translatable("gui.akaishi.matrix.craft.detail.amount"),
                8, DETAIL_OP_Y + 3, TEXT_DIM, false);
        GuiWidgets.buttonText(gui, scr.font(), DETAIL_START_X, DETAIL_OP_Y, DETAIL_START_W, DETAIL_BTN_H,
                Component.translatable("gui.akaishi.matrix.craft.detail.start"), plan.affordable());
    }

    /** 详情页悬停：贴图格 → 物品名；材料格 → 材料名 + 需要数量 + 够/缺 */
    void renderCraftDetailTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        AkaishiMatrixCraftSync.PlanView plan = planFor(detailItem, detailFetchedAmount);
        if (AkaishiMiniMatrixTerminalScreen.isIn(scr.leftPos() + DETAIL_ICON_X, scr.topPos() + DETAIL_TOP,
                16, 16, mouseX, mouseY)) {
            lines.add(new ItemStack(detailItem).getHoverName());
        } else if (plan != null && plan.machineMissing()
                && AkaishiMiniMatrixTerminalScreen.isIn(scr.leftPos() + 8, scr.topPos() + DETAIL_COST_Y,
                        160, 9, mouseX, mouseY)) {
            // 行尾那三个字（缺机台）说不清要怎么办，悬停给完整说法
            lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.machine_missing"));
        } else if (plan != null && plan.powerMissing()
                && AkaishiMiniMatrixTerminalScreen.isIn(scr.leftPos() + 8, scr.topPos() + DETAIL_COST_Y,
                        160, 9, mouseX, mouseY)) {
            lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.power_missing"));
        } else if (plan != null && AkaishiMiniMatrixTerminalScreen.isIn(scr.leftPos() + 8,
                scr.topPos() + DETAIL_RECIPE_Y - 1, CONTENT_W, 10, mouseX, mouseY)) {
            // 工序来源：这条订单到底由谁提供（自研机台族 / 第三方已声明到方块 / 第三方粗粒度）
            lines.addAll(processLines(plan));
        } else if (plan != null) {
            int relX = (int) mouseX - scr.leftPos() - DETAIL_GRID_X;
            int relY = (int) mouseY - scr.topPos() - DETAIL_GRID_Y;
            if (relX < 0 || relY < 0) {
                return;
            }
            int col = relX / CRAFT_CELL;
            int index = (relY / CRAFT_CELL) * DETAIL_COLS + col;
            List<AkaishiMatrixCraftSync.LeafView> materials = plan.leaves();
            if (col >= DETAIL_COLS || index >= Math.min(materials.size(), DETAIL_MATERIALS)) {
                return;
            }
            AkaishiMatrixCraftSync.LeafView material = materials.get(index);
            lines.add(material.stack().getHoverName());
            lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.material_line", material.count()));
            lines.add(Component.translatable(material.enough()
                    ? "gui.akaishi.matrix.craft.detail.material_ok"
                    : "gui.akaishi.matrix.craft.detail.material_missing"));
        } else {
            return;
        }
        gui.renderComponentTooltip(scr.font(), lines, mouseX, mouseY);
    }

    /**
     * 「这条工序由谁提供」的悬停文本：标题 + 逐条来源。不需要机台（纯原版配方）时给一句说明。
     */
    private List<Component> processLines(AkaishiMatrixCraftSync.PlanView plan) {
        List<Component> lines = new ArrayList<>();
        if (plan.processes().isEmpty()) {
            lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.source.none"));
            return lines;
        }
        lines.add(Component.translatable("gui.akaishi.matrix.craft.detail.source.header"));
        for (AkaishiMatrixCraftSync.ProcessView process : plan.processes()) {
            Component name = processName(process.processId());
            lines.add(switch (process.tier()) {
                case 0 -> Component.translatable("gui.akaishi.matrix.craft.detail.source.own", name);
                case 1 -> Component.translatable("gui.akaishi.matrix.craft.detail.source.declared", name,
                        ownerNames(process));
                default -> Component.translatable("gui.akaishi.matrix.craft.detail.source.generic", name);
            });
        }
        return lines;
    }

    /**
     * 工序显示名：自研族取 {@code gui.akaishi.process.<配方类型 path>}（= 机器族名，玩家认机器不认配方 id），
     * 第三方则直接用配方类型 id。缺键时回落 id —— 绝不显示裸 key。
     */
    private static Component processName(String processId) {
        ResourceLocation id = ResourceLocation.tryParse(processId);
        if (id == null) {
            return Component.literal(processId);
        }
        return Component.translatableWithFallback("gui.akaishi.process." + id.getPath(), processId);
    }

    /** 已声明工序的提供方块名（在客户端翻成当前语言；方块不存在时退回 id 本身） */
    private static String ownerNames(AkaishiMatrixCraftSync.ProcessView process) {
        List<String> names = new ArrayList<>(process.owners().size());
        for (String ownerId : process.owners()) {
            ResourceLocation id = ResourceLocation.tryParse(ownerId);
            Block block = id == null ? null : BuiltInRegistries.BLOCK.get(id);
            names.add(block == null || block == Blocks.AIR ? ownerId : block.getName().getString());
        }
        return String.join(" / ", names);
    }

    /**
     * 详情页当前该显示的那份账：<b>物品 + 件数</b>都要对得上，否则返回 null。
     * <p>
     * 只有物品对得上是不够的：玩家把数量从 1 改成 8 时，先前那个"按 1 件算"的包可能后到，
     * 显示就会被永久卡在旧账上（现象：有时显示不正确）。件数是随包回显的请求件数。
     */
    @Nullable
    private AkaishiMatrixCraftSync.PlanView planFor(Item item, int amount) {
        AkaishiMatrixCraftSync.PlanView plan = scr.menu().craftPlan();
        return plan != null && !plan.target().isEmpty() && plan.target().getItem() == item
                && plan.amount() == amount ? plan : null;
    }

    /**
     * 该件数是否"已经算过、但规划不出来"：服务端把空 target 的账连同件数一起回显。
     * <p>
     * 与"还没有回包"必须分开：前者要明说"做不了"，后者才显示"正在规划…"。
     */
    private boolean planUnavailable(int amount) {
        AkaishiMatrixCraftSync.PlanView plan = scr.menu().craftPlan();
        return plan != null && plan.target().isEmpty() && plan.amount() == amount;
    }
}
