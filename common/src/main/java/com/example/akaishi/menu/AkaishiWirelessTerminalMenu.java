package com.example.akaishi.menu;

import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.api.storage.IWirelessTerminalHost;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.wireless.TerminalSecurity;
import com.example.akaishi.wireless.WirelessNetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 无线赤能源终端菜单（终端方块主界面，四个互斥页面：运行情况/能量储存/安全卡认证/能量传输）。
 * 无机器存储槽，仅 1 个「安全卡认证」页的授权槽（只接受身份卡）；
 * 储能/容量（long 4 槽）+ 口统计 + 授权卡数 + 组件状态经数据槽同步。
 * 页面切换为 Screen 本地状态（互不重叠）；授权/移除授权走 clickMenuButton（服务端经方块实体生效）。
 */
public class AkaishiWirelessTerminalMenu extends AbstractContainerMenu
        implements SecurityPage.Source, AkaishiTerminalSecuritySync.Target {

    // ===== 页面（Screen 本地状态，此处仅定义常量供安全方块直达页使用） =====
    public static final int PAGE_RUN = 0;
    public static final int PAGE_ENERGY = 1;
    public static final int PAGE_SECURITY = 2;
    public static final int PAGE_TRANSFER = 3;

    /** 授权槽在 menu 的 slot 索引 */
    public static final int CARD_SLOT_INDEX = 0;
    // 授权槽界面坐标统一由 SecurityPage 提供（三个终端共用同一套安全页版式）

    private final SimpleContainer cardInv;
    private final ContainerData data;
    private final IWirelessTerminalHost host;
    private final Player player;
    /** 初始页面（安全方块/终端方块经网络缓冲传入，Screen 打开时定位） */
    private int initialPage;

    // ===== 安全页：权限表快照（客户端渲染只读；S2C 包填充） =====

    private String securityOwnerName = "";
    private boolean securityHasDefault;
    private int securityDefaultPerms;
    private List<AkaishiTerminalSecuritySync.Entry> securityEntries = List.of();
    /** 服务端：已推送的权限表版本（仅变化时重推，免每 tick 空包） */
    private int securitySentRevision = -1;

    /** 服务端构造：持有终端宿主（授权/移除授权在此生效；方块实体与微缩件都实现该接口） */
    public AkaishiWirelessTerminalMenu(int id, Inventory inv, IWirelessTerminalHost host) {
        super(ModMenus.CHISHI_WIRELESS_TERMINAL.get(), id);
        this.host = host;
        this.player = inv.player;
        this.data = host.data();
        this.cardInv = new SimpleContainer(1);
        addCardSlot();
        addPlayerSlots(inv);
        this.addDataSlots(data);
    }

    /** 客户端构造：仅数据槽同步（授权槽不可操作） */
    public AkaishiWirelessTerminalMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_WIRELESS_TERMINAL.get(), id);
        this.host = null;
        this.player = inv.player;
        this.data = data;
        this.cardInv = new SimpleContainer(1);
        addCardSlot();
        addPlayerSlots(inv);
        this.addDataSlots(data);
    }

    /** 授权槽：仅允许放入身份卡；isActive 由 Screen 按页面切换（非安全页隐藏槽） */
    private void addCardSlot() {
        this.addSlot(new Slot(cardInv, 0, SecurityPage.CARD_SLOT_X, SecurityPage.CARD_SLOT_Y) {
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

    /** 授权槽是否激活（仅安全认证页激活；客户端 Screen 每帧同步） */
    private boolean securitySlotActive = true;

    public void setSecuritySlotActive(boolean active) {
        this.securitySlotActive = active;
    }

    private void addPlayerSlots(Inventory inv) {
        // 198 高 GUI：玩家背包 3 行下移至 y=124 起，快捷栏 y=180（与 akaishi_wireless_terminal.png 槽位图案对齐）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            // 快捷栏：与背包同网格（x=8+18c / y=180），贴图槽框同网格绘制
            this.addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
    }

    // ===== 安全页：服务端动作（由 C2S 包驱动） =====

    /** 每 tick 比对权限表版本，变化才推快照（免空包） */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (host == null || !(this.player instanceof ServerPlayer serverPlayer)
                || serverPlayer.containerMenu != this) {
            return;
        }
        TerminalSecurity security = host.security();
        if (security.revision() != this.securitySentRevision) {
            this.securitySentRevision = security.revision();
            AkaishiTerminalSecuritySync.sendSnapshot(serverPlayer, this.containerId, security);
        }
    }

    /**
     * 服务端执行一次安全页动作（由 C2S 包驱动）——实现集中在 {@link SecurityPage}，
     * 两个无线终端菜单共用，避免重复。
     */
    public void applySecurityAction(Player actor, byte action, UUID target, int permOrdinal) {
        if (host == null) {
            return;
        }
        SecurityPage.applyAction(host.security(),
                (checkPlayer, perm) -> WirelessNetworkManager.hasPermission(host.terminalId(), checkPlayer, perm),
                actor, cardInv.getItem(0), action, target, permOrdinal);
    }

    // ===== 安全页：客户端快照访问器 =====

    /** 客户端：接收权限表快照（渲染只读） */
    public void acceptSecurity(String ownerName, boolean hasDefault, int defaultPerms,
            List<AkaishiTerminalSecuritySync.Entry> entries) {
        this.securityOwnerName = ownerName == null ? "" : ownerName;
        this.securityHasDefault = hasDefault;
        this.securityDefaultPerms = defaultPerms;
        this.securityEntries = List.copyOf(entries);
    }

    /** 终端归属者名（客户端快照；未知为空串） */
    public String securityOwnerName() {
        return securityOwnerName;
    }

    /** 权限表条目（客户端快照，顺序与服务端一致） */
    public List<AkaishiTerminalSecuritySync.Entry> securityEntries() {
        return securityEntries;
    }

    /** 是否登记了默认权限条目 */
    public boolean securityHasDefault() {
        return securityHasDefault;
    }

    /** 默认权限条目掩码 */
    public int securityDefaultPerms() {
        return securityDefaultPerms;
    }

    /** 是否已启用安全（登记了任何条目）——供界面提示"未启用 = 全部放行" */
    public boolean securityEnabled() {
        return !securityEntries.isEmpty() || securityHasDefault;
    }

    // ===== 数据槽读取 =====

    public long getEnergy() {
        return LongDataSlots.read(data, IWirelessTerminalHost.DATA_STORED_LOW,
                IWirelessTerminalHost.DATA_STORED_HIGH,
                IWirelessTerminalHost.DATA_STORED_HIGH2,
                IWirelessTerminalHost.DATA_STORED_HIGH3);
    }

    public long getMaxEnergy() {
        return LongDataSlots.read(data, IWirelessTerminalHost.DATA_CAPACITY_LOW,
                IWirelessTerminalHost.DATA_CAPACITY_HIGH,
                IWirelessTerminalHost.DATA_CAPACITY_HIGH2,
                IWirelessTerminalHost.DATA_CAPACITY_HIGH3);
    }

    public boolean isFormed() {
        return data.get(IWirelessTerminalHost.DATA_FORMED) == 1;
    }

    public int getInputCount() {
        return data.get(IWirelessTerminalHost.DATA_INPUT_COUNT);
    }

    public int getOutputCount() {
        return data.get(IWirelessTerminalHost.DATA_OUTPUT_COUNT);
    }

    public int getBoundSerializers() {
        return data.get(IWirelessTerminalHost.DATA_BOUND_SERIALIZERS);
    }

    /** 跨维度是否已解锁（内腔含终端跨维组件） */
    public boolean isCrossDim() {
        return data.get(IWirelessTerminalHost.DATA_CROSS_DIM) == 1;
    }

    /** 区块加载是否已启用（内腔含区块加载构架） */
    public boolean isChunkLoad() {
        return data.get(IWirelessTerminalHost.DATA_CHUNK_LOAD) == 1;
    }

    /** 区块加载范围是否已扩展为 3×3（内腔含区块加载扩展组件） */
    public boolean isChunkRange() {
        return data.get(IWirelessTerminalHost.DATA_CHUNK_RANGE) == 1;
    }

    /** 当前弱加载区块数（区块加载构架生效时 >0） */
    public int getChunkLoaded() {
        return data.get(IWirelessTerminalHost.DATA_CHUNK_LOADED);
    }

    /** 内腔输入损耗抑制组件数量 */
    public int inputLossModules() {
        return data.get(IWirelessTerminalHost.DATA_INPUT_LOSS);
    }

    /** 内腔输出损耗抑制组件数量 */
    public int outputLossModules() {
        return data.get(IWirelessTerminalHost.DATA_OUTPUT_LOSS);
    }

    /** 终端短 ID（8 位 hex，与身份卡 ID 同格式；低/高 2 槽按 16 位段重组） */
    public String getTerminalShortId() {
        return String.format("%08X", LongDataSlots.readInt(data,
                IWirelessTerminalHost.DATA_TERMINAL_ID,
                IWirelessTerminalHost.DATA_TERMINAL_ID_HIGH));
    }

    @Override
    public boolean stillValid(Player player) {
        // 8 格内才有效：与物品终端同口径（安全页能改权限，更不能远距离操作）
        return host == null || player.distanceToSqr(host.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 授权槽是 Menu 内瞬时容器（非方块实体物品栏）：关闭 GUI 时未点「授权」的卡必须返还玩家，防物品丢失
        ItemStack card = cardInv.removeItemNoUpdate(0);
        if (!card.isEmpty() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.getInventory().placeItemBackInInventory(card);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // slot 0 = 授权槽（不参与自动搬运）；1..27 背包 ↔ 28..36 热栏
        if (index == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < 28) {
                if (!this.moveItemStackTo(current, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 1, 28, false)) {
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
        }
        return result;
    }

    /** 宿主缺失兜底（客户端空数据构造） */
    public static AkaishiWirelessTerminalMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiWirelessTerminalMenu(id, inv,
                new SimpleContainerData(IWirelessTerminalHost.DATA_SLOTS));
    }

    /** 打开时的初始页面（客户端经网络缓冲设置） */
    public void setInitialPage(int page) {
        this.initialPage = page;
    }

    public int getInitialPage() {
        return initialPage;
    }
}
