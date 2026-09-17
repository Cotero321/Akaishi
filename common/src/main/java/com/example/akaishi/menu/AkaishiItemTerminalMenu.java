package com.example.akaishi.menu;

import java.util.List;

import com.example.akaishi.block.entity.AkaishiItemTerminalBlockEntity;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.value.ItemTerminalFee;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 物品终端菜单：库页为「可滚动聚合列表」（AE2 网格口径）。
 * <p>
 * <b>槽位分侧</b>（对齐 AE2 {@code MEStorageMenu} + {@code RepoSlot}）：
 * 服务端只放玩家背包槽；库页 4×9 可视槽由客户端 {@code Screen#init} 追加在玩家槽之后，
 * 因此两侧玩家槽下标一致，而库内容不经原版槽同步、改由 {@link AkaishiItemTerminalSync} 快照下发。
 * <p>
 * <b>滚动纯客户端</b>：滚动行只是本机状态（AE2 滚动条同理），点击时把「选中条目 + 动作」发到服务端，
 * 服务端按物品重新定位条目，故不存在"两侧对不齐页"的问题。
 */
public class AkaishiItemTerminalMenu extends AbstractContainerMenu {

    // ===== 库页 / 面板几何（Menu 与 Screen 共用，避免两侧坐标错位） =====

    /** 库页列数 */
    public static final int COLUMNS = 9;
    /** 库页可视行数（AE2 每档滚轮滚 1 行，故 4 行对应 4 格步进） */
    public static final int ROWS = 4;
    /** 库页可视槽数 */
    public static final int VIEW_SLOTS = COLUMNS * ROWS;

    /** 面板尺寸 */
    public static final int PANEL_W = 184;
    public static final int PANEL_H = 229;
    /** 库页可视区左上角与行距（多出搜索框一行，故整体下移） */
    public static final int SLOT_X = 8;
    public static final int SLOT_Y = 58;
    public static final int SLOT_STEP = 18;
    /** 搜索框（标题 / IP 条之下、库区之上） */
    public static final int SEARCH_X = 8;
    public static final int SEARCH_Y = 36;
    public static final int SEARCH_W = 160;
    public static final int SEARCH_H = 16;
    /** 滚动条轨道（右侧竖列）：加宽以提升可辨识度 */
    public static final int SCROLLBAR_X = 173;
    public static final int SCROLLBAR_W = 8;
    /** 玩家背包首行 / 快捷栏 y */
    public static final int INV_TOP = 147;
    public static final int HOTBAR_Y = 205;

    /** 玩家背包槽数（3×9 主背包 + 1×9 快捷栏）；库页虚拟槽追加在其后 */
    public static final int PLAYER_SLOTS = 36;
    /** 主背包槽数：快速移动在「主背包 ↔ 快捷栏」间互转的边界 */
    private static final int MAIN_INV_SLOTS = 27;
    /** 视距校验：超过 8 格自动关闭菜单，防远程操作 */
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private final AkaishiItemTerminalBlockEntity terminal;
    private final ContainerData data;
    private final Player player;

    /** 服务端：已下发的库内容版本 / 成型状态（仅变化时重推，免每 tick 空包） */
    private int sentRevision = -1;
    private boolean sentFormed;
    /** 客户端：滚动行（0 起，纯本机状态） */
    private int scrollRow;
    /** 客户端：条目仓库（由 S2C 快照填充） */
    private List<AkaishiItemTerminalSync.Entry> entries = List.of();
    /** 客户端：搜索关键字（本机状态，不影响服务端） */
    private String searchQuery = "";
    /** 客户端：过滤后的可视条目（搜索为空时即原表） */
    private List<AkaishiItemTerminalSync.Entry> filtered = List.of();
    private int clientRevision = -1;
    private boolean clientFormed;
    /**
     * 「原版槽渲染这一趟」标记。
     * <p>
     * 1.20.1 的 {@code AbstractContainerScreen.renderSlot} 只在<b>空槽位</b>才走 {@code getNoItemIcon}
     * 分支（那是给空槽画背景图标用的），槽里有物品时照样 {@code renderItem} + {@code renderItemDecorations}，
     * 且这批几何要到<b>帧末最后一次 flush</b> 才落屏 —— 会把界面自绘的数字盖掉。
     * 因此这一趟让显示槽对原版"看起来是空的"（{@code getItem()} 返回空堆 ⇒ 原版只画我们的全透明图），
     * 图标与数量全部由界面自绘，顺序彻底自控（见 {@code AkaishiItemTerminalScreen#drawGrid}）。
     */
    private boolean vanillaRenderPass;

    public AkaishiItemTerminalMenu(int id, Inventory inv, AkaishiItemTerminalBlockEntity terminal) {
        super(ModMenus.CHISHI_ITEM_TERMINAL.get(), id);
        this.terminal = terminal;
        this.player = inv.player;
        // 未成型 / 方块实体缺失时用同规格空数据兜底：槽位与数据槽数量必须与正常路径一致，
        // 否则客户端索引会错位
        this.data = terminal != null
                ? terminal.data()
                : new SimpleContainerData(AkaishiItemTerminalBlockEntity.DATA_SLOTS);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new Slot(inv, col + row * COLUMNS + 9, SLOT_X + col * SLOT_STEP, INV_TOP + row * SLOT_STEP));
            }
        }
        for (int col = 0; col < COLUMNS; col++) {
            addSlot(new Slot(inv, col, SLOT_X + col * SLOT_STEP, HOTBAR_Y));
        }
        addDataSlots(this.data);
    }

    // ===== 库页条目仓库（客户端） =====

    /** 可视区下标 → 条目（越界返回 null = 空白格）；按搜索过滤后的列表取 */
    public AkaishiItemTerminalSync.Entry entryAt(int viewIndex) {
        int index = this.scrollRow * COLUMNS + viewIndex;
        return index >= 0 && index < this.filtered.size() ? this.filtered.get(index) : null;
    }

    /** 库页总行数（含末尾不足一行）：按过滤后的条目数计 */
    public int totalRows() {
        return (this.filtered.size() + COLUMNS - 1) / COLUMNS;
    }

    /** 客户端：当前搜索关键字（重开界面时用于恢复输入框内容） */
    public String searchQuery() {
        return this.searchQuery;
    }

    /** 客户端：设置搜索关键字并立即重算可视表 */
    public void setSearchQuery(String query) {
        this.searchQuery = query == null ? "" : query;
        applyFilter();
        // 过滤后行数变少，原滚动位置可能越界
        scrollTo(this.scrollRow);
    }

    /** 客户端：当前可视条目数（已按搜索过滤） */
    public int visibleEntryCount() {
        return this.filtered.size();
    }

    /** 客户端：当前可视条目的总件数（已按搜索过滤） */
    public long visibleItemCount() {
        long total = 0L;
        for (AkaishiItemTerminalSync.Entry entry : this.filtered) {
            total += entry.amount();
        }
        return total;
    }

    private void applyFilter() {
        this.filtered = ItemTerminalSearch.filter(this.entries, this.searchQuery);
    }

    /** 是否处于「原版槽渲染这一趟」（仅该趟内显示槽返回空堆，让原版不画物品） */
    public boolean vanillaRenderPass() {
        return this.vanillaRenderPass;
    }

    /** 由界面在 {@code super.render} 前后包夹设置（务必 try/finally） */
    public void setVanillaRenderPass(boolean value) {
        this.vanillaRenderPass = value;
    }

    /** 可滚动的最大行下标 */
    public int maxScrollRow() {
        return Math.max(0, totalRows() - ROWS);
    }

    public int scrollRow() {
        return this.scrollRow;
    }

    /** 客户端：滚动到指定行（钳制到合法范围） */
    public void scrollTo(int row) {
        this.scrollRow = Math.max(0, Math.min(maxScrollRow(), row));
    }

    /** 客户端：接收条目快照（乱序旧包直接丢弃） */
    public void acceptEntries(boolean formed, int revision, List<AkaishiItemTerminalSync.Entry> snapshot) {
        if (revision < this.clientRevision) {
            return;
        }
        this.clientRevision = revision;
        this.clientFormed = formed;
        this.entries = List.copyOf(snapshot);
        // 快照换了 ⇒ 过滤结果必须跟着重算，否则会拿着旧可视表渲染
        applyFilter();
        // 内容变少后原滚动位置可能越界，立即夹回，避免出现整屏空白
        scrollTo(this.scrollRow);
    }

    /** 客户端：最近一次快照是否对应成型中的终端 */
    public boolean snapshotFormed() {
        return this.clientFormed;
    }

    public AkaishiItemTerminalBlockEntity terminal() {
        return this.terminal;
    }

    // ===== 数据槽读数（服务端权威值，经数据槽同步到客户端） =====

    public boolean formed() {
        return this.data.get(AkaishiItemTerminalBlockEntity.DATA_FORMED) != 0;
    }

    public long usedIp() {
        return LongDataSlots.read(this.data, AkaishiItemTerminalBlockEntity.DATA_USED_LOW,
                AkaishiItemTerminalBlockEntity.DATA_USED_HIGH, AkaishiItemTerminalBlockEntity.DATA_USED_HIGH2,
                AkaishiItemTerminalBlockEntity.DATA_USED_HIGH3);
    }

    public long capacityIp() {
        return LongDataSlots.read(this.data, AkaishiItemTerminalBlockEntity.DATA_CAPACITY_LOW,
                AkaishiItemTerminalBlockEntity.DATA_CAPACITY_HIGH, AkaishiItemTerminalBlockEntity.DATA_CAPACITY_HIGH2,
                AkaishiItemTerminalBlockEntity.DATA_CAPACITY_HIGH3);
    }

    public long bufferedEnergy() {
        return LongDataSlots.read(this.data, AkaishiItemTerminalBlockEntity.DATA_BUFFER_LOW,
                AkaishiItemTerminalBlockEntity.DATA_BUFFER_HIGH, AkaishiItemTerminalBlockEntity.DATA_BUFFER_HIGH2,
                AkaishiItemTerminalBlockEntity.DATA_BUFFER_HIGH3);
    }

    public int unitCount() {
        return this.data.get(AkaishiItemTerminalBlockEntity.DATA_UNIT_COUNT);
    }

    /** 有效赤能源缓冲容量（服务端权威值：配置基准 + 缓冲扩展组件加成） */
    public long effectiveBufferCapacity() {
        return LongDataSlots.read(this.data, AkaishiItemTerminalBlockEntity.DATA_EFFECTIVE_BUFFER_LOW,
                AkaishiItemTerminalBlockEntity.DATA_EFFECTIVE_BUFFER_HIGH,
                AkaishiItemTerminalBlockEntity.DATA_EFFECTIVE_BUFFER_HIGH2,
                AkaishiItemTerminalBlockEntity.DATA_EFFECTIVE_BUFFER_HIGH3);
    }

    /** 生效的费率减免份数（0 ~ FEE_MODULE_MAX） */
    public int effectiveFeeModules() {
        return this.data.get(AkaishiItemTerminalBlockEntity.DATA_FEE_MODULES);
    }

    /**
     * 单笔可处理的最大 IP。
     * <p>
     * 用同步过来的「有效缓冲容量 + 减免份数」现算，而不是向终端 BE 取值 ——
     * 客户端那份 BE 是空壳（拿不到结构扫描结果），直接问它会永远显示配置默认值。
     */
    public long maxBatchIp(boolean deposit) {
        return ItemTerminalFee.maxIp(effectiveBufferCapacity(), deposit, effectiveFeeModules());
    }

    // ===== 服务端：库内容变化时重推快照 =====

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(this.player instanceof ServerPlayer serverPlayer) || this.terminal == null) {
            return;
        }
        int revision = this.terminal.contentRevision();
        boolean formed = this.terminal.isFormed();
        if (revision == this.sentRevision && formed == this.sentFormed) {
            return;
        }
        this.sentRevision = revision;
        this.sentFormed = formed;
        AkaishiItemTerminalSync.sendEntries(serverPlayer, this.containerId, formed, revision,
                formed ? AkaishiItemTerminalSync.snapshot(this.terminal) : List.of());
    }

    // ===== 槽位行为 =====

    /**
     * 库页是客户端只读虚拟槽（服务端不存在），因此快速移动只在玩家背包内做「主背包 ↔ 快捷栏」互转；
     * 与库的往来一律走光标点击（AE2 语义），不走原版快速移动。
     * <p>
     * 客户端直接拒绝：客户端槽位表里追加了 36 个虚拟库页槽，若在客户端对它们执行原版快速移动，
     * 会先把条目放进背包再清空"源堆"（虚拟槽清不掉），造成本地凭空复制。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide() || index < 0 || index >= PLAYER_SLOTS) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack result = current.copy();
        boolean moved = index < MAIN_INV_SLOTS
                ? this.moveItemStackTo(current, MAIN_INV_SLOTS, PLAYER_SLOTS, false)
                : this.moveItemStackTo(current, 0, MAIN_INV_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (current.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, current);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.terminal != null && !this.terminal.isRemoved()
                && player.distanceToSqr(this.terminal.getBlockPos().getCenter()) <= MAX_DISTANCE_SQR;
    }
}
