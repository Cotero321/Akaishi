package com.example.akaishi.menu;

import java.util.List;
import java.util.UUID;

import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.value.ItemTerminalFee;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
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
public class AkaishiItemTerminalMenu extends AbstractContainerMenu
        implements SecurityPage.Source, AkaishiTerminalSecuritySync.Target {

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

    private final IItemTerminalHost terminal;
    private final ContainerData data;
    private final Player player;
    /** 安全页授权槽（瞬时容器：关闭界面时未登记的卡返还玩家，防物品丢失） */
    private final SimpleContainer cardInv = new SimpleContainer(1);
    /** 授权槽是否激活（仅安全页激活；客户端 Screen 每帧同步） */
    private boolean securitySlotActive;

    // ===== 安全页：权限表快照（客户端渲染只读；S2C 包填充） =====

    private String securityOwnerName = "";
    private boolean securityHasDefault;
    private int securityDefaultPerms;
    private List<AkaishiTerminalSecuritySync.Entry> securityEntries = List.of();
    /** 服务端：已推送的权限表版本（仅变化时重推，免每 tick 空包） */
    private int securitySentRevision = -1;

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

    public AkaishiItemTerminalMenu(int id, Inventory inv, IItemTerminalHost terminal) {
        super(ModMenus.CHISHI_ITEM_TERMINAL.get(), id);
        this.terminal = terminal;
        this.player = inv.player;
        // 未成型 / 方块实体缺失时用同规格空数据兜底：槽位与数据槽数量必须与正常路径一致，
        // 否则客户端索引会错位
        this.data = terminal != null
                ? terminal.data()
                : new SimpleContainerData(IItemTerminalHost.DATA_SLOTS);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new Slot(inv, col + row * COLUMNS + 9, SLOT_X + col * SLOT_STEP, INV_TOP + row * SLOT_STEP));
            }
        }
        for (int col = 0; col < COLUMNS; col++) {
            addSlot(new Slot(inv, col, SLOT_X + col * SLOT_STEP, HOTBAR_Y));
        }
        // 安全页授权槽挂在玩家槽之后（下标 36）：玩家槽下标保持 0..35，quickMoveStack 不受影响；
        // 库页虚拟槽由 Screen#init 追加在其后
        addCardSlot();
        addDataSlots(this.data);
    }

    /** 安全页授权槽：仅允许放入身份卡；isActive 由 Screen 按页面切换（非安全页隐藏槽） */
    private void addCardSlot() {
        this.addSlot(new Slot(cardInv, 0, SecurityPage.CARD_SLOT_X, SecurityPage.CARD_SLOT_Y + SEC_PAGE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof AkaishiWirelessIdentityCardItem;
            }

            @Override
            public boolean isActive() {
                return securitySlotActive;
            }
        });
    }

    /** 安全页整体下移量：物品终端面板更高（背包在 y=147），且 y=17..35 是 IP 条与 IP 文本行 */
    public static final int SEC_PAGE_Y = 10;

    /** 授权槽是否激活（仅安全页激活） */
    public void setSecuritySlotActive(boolean active) {
        this.securitySlotActive = active;
    }

    // ===== 安全页：服务端动作 + 客户端快照（SecurityPage.Source 实现） =====

    /**
     * 服务端执行一次安全页动作。安全校验走<b>本地权威权限表</b>：
     * 物品终端不参与无线网络注册表，没有可查的镜像。
     */
    public void applySecurityAction(Player actor, byte action, UUID target, int permOrdinal) {
        if (terminal == null) {
            return;
        }
        SecurityPage.applyAction(terminal.security(),
                (checkPlayer, perm) -> SecurityPage.checkLocal(terminal.security(), checkPlayer, perm),
                actor, cardInv.getItem(0), action, target, permOrdinal);
    }

    /** 客户端：接收权限表快照（渲染只读） */
    public void acceptSecurity(String ownerName, boolean hasDefault, int defaultPerms,
            List<AkaishiTerminalSecuritySync.Entry> entries) {
        this.securityOwnerName = ownerName == null ? "" : ownerName;
        this.securityHasDefault = hasDefault;
        this.securityDefaultPerms = defaultPerms;
        this.securityEntries = List.copyOf(entries);
    }

    @Override
    public String securityOwnerName() {
        return securityOwnerName;
    }

    @Override
    public List<AkaishiTerminalSecuritySync.Entry> securityEntries() {
        return securityEntries;
    }

    @Override
    public boolean securityHasDefault() {
        return securityHasDefault;
    }

    @Override
    public int securityDefaultPerms() {
        return securityDefaultPerms;
    }

    @Override
    public boolean securityEnabled() {
        return !securityEntries.isEmpty() || securityHasDefault;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 授权槽是 Menu 内瞬时容器（非方块实体物品栏）：关闭界面时未登记的卡必须返还玩家，防物品丢失
        ItemStack card = cardInv.removeItemNoUpdate(0);
        if (!card.isEmpty() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getInventory().placeItemBackInInventory(card);
        }
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

    public IItemTerminalHost terminal() {
        return this.terminal;
    }

    // ===== 数据槽读数（服务端权威值，经数据槽同步到客户端） =====

    public boolean formed() {
        return this.data.get(IItemTerminalHost.DATA_FORMED) != 0;
    }

    public long usedIp() {
        return LongDataSlots.read(this.data, IItemTerminalHost.DATA_USED_LOW,
                IItemTerminalHost.DATA_USED_HIGH, IItemTerminalHost.DATA_USED_HIGH2,
                IItemTerminalHost.DATA_USED_HIGH3);
    }

    public long capacityIp() {
        return LongDataSlots.read(this.data, IItemTerminalHost.DATA_CAPACITY_LOW,
                IItemTerminalHost.DATA_CAPACITY_HIGH, IItemTerminalHost.DATA_CAPACITY_HIGH2,
                IItemTerminalHost.DATA_CAPACITY_HIGH3);
    }

    public long bufferedEnergy() {
        return LongDataSlots.read(this.data, IItemTerminalHost.DATA_BUFFER_LOW,
                IItemTerminalHost.DATA_BUFFER_HIGH, IItemTerminalHost.DATA_BUFFER_HIGH2,
                IItemTerminalHost.DATA_BUFFER_HIGH3);
    }

    public int unitCount() {
        return this.data.get(IItemTerminalHost.DATA_UNIT_COUNT);
    }

    /** 有效赤能源缓冲容量（服务端权威值：配置基准 + 缓冲扩展组件加成） */
    public long effectiveBufferCapacity() {
        return LongDataSlots.read(this.data, IItemTerminalHost.DATA_EFFECTIVE_BUFFER_LOW,
                IItemTerminalHost.DATA_EFFECTIVE_BUFFER_HIGH,
                IItemTerminalHost.DATA_EFFECTIVE_BUFFER_HIGH2,
                IItemTerminalHost.DATA_EFFECTIVE_BUFFER_HIGH3);
    }

    /** 生效的费率减免份数（0 ~ FEE_MODULE_MAX） */
    public int effectiveFeeModules() {
        return this.data.get(IItemTerminalHost.DATA_FEE_MODULES);
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
        // 安全页权限表：与库内容版本相互独立，变化才推快照（免每 tick 空包）
        var security = this.terminal.security();
        if (security.revision() != this.securitySentRevision) {
            this.securitySentRevision = security.revision();
            AkaishiTerminalSecuritySync.sendSnapshot(serverPlayer, this.containerId, security);
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
