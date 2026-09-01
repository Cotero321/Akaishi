package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiWirelessTerminalBlockEntity;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import com.example.akaishi.wireless.WirelessNetworkManager;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 无线赤能源终端菜单（终端方块主界面，四个互斥页面：运行情况/能量储存/安全卡认证/能量传输）。
 * 无机器存储槽，仅 1 个「安全卡认证」页的授权槽（只接受身份卡）；
 * 储能/容量（long 4 槽）+ 口统计 + 授权卡数 + 组件状态经数据槽同步。
 * 页面切换为 Screen 本地状态（互不重叠）；授权/移除授权走 clickMenuButton（服务端经方块实体生效）。
 */
public class AkaishiWirelessTerminalMenu extends AbstractContainerMenu {

    // ===== 页面（Screen 本地状态，此处仅定义常量供安全方块直达页使用） =====
    public static final int PAGE_RUN = 0;
    public static final int PAGE_ENERGY = 1;
    public static final int PAGE_SECURITY = 2;
    public static final int PAGE_TRANSFER = 3;

    // ===== 服务端按钮 =====
    /** 授权：把授权槽中的身份卡加入白名单 */
    public static final int BTN_AUTHORIZE = 0;
    /** 移除授权：把授权槽中的身份卡移出白名单 */
    public static final int BTN_REVOKE = 1;

    /** 授权槽在 menu 的 slot 索引 */
    public static final int CARD_SLOT_INDEX = 0;
    /** 授权槽界面坐标（198 高 GUI，与切页按钮区错开） */
    public static final int CARD_SLOT_X = 62;
    public static final int CARD_SLOT_Y = 46;

    private final SimpleContainer cardInv;
    private final ContainerData data;
    private final AkaishiWirelessTerminalBlockEntity be;
    /** 初始页面（安全方块/终端方块经网络缓冲传入，Screen 打开时定位） */
    private int initialPage;

    /** 服务端构造：持有方块实体（授权/移除授权在此生效） */
    public AkaishiWirelessTerminalMenu(int id, Inventory inv, AkaishiWirelessTerminalBlockEntity be) {
        super(ModMenus.CHISHI_WIRELESS_TERMINAL.get(), id);
        this.be = be;
        this.data = be.data();
        this.cardInv = new SimpleContainer(1);
        addCardSlot();
        addPlayerSlots(inv);
        this.addDataSlots(data);
    }

    /** 客户端构造：仅数据槽同步（授权槽不可操作） */
    public AkaishiWirelessTerminalMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_WIRELESS_TERMINAL.get(), id);
        this.be = null;
        this.data = data;
        this.cardInv = new SimpleContainer(1);
        addCardSlot();
        addPlayerSlots(inv);
        this.addDataSlots(data);
    }

    /** 授权槽：仅允许放入身份卡；isActive 由 Screen 按页面切换（非安全页隐藏槽） */
    private void addCardSlot() {
        this.addSlot(new Slot(cardInv, 0, CARD_SLOT_X, CARD_SLOT_Y) {
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
            this.addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) {
            return false;
        }
        ItemStack card = cardInv.getItem(0);
        if (card.getItem() instanceof AkaishiWirelessIdentityCardItem) {
            if (id == BTN_AUTHORIZE) {
                be.authorizeCard(AkaishiWirelessIdentityCardItem.uuidOf(card));
                return true;
            }
            if (id == BTN_REVOKE) {
                be.revokeCard(AkaishiWirelessIdentityCardItem.uuidOf(card));
                return true;
            }
        }
        return false;
    }

    // ===== 数据槽读取 =====

    public long getEnergy() {
        return ((long) data.get(AkaishiWirelessTerminalBlockEntity.DATA_STORED_HIGH) << 32)
                | (data.get(AkaishiWirelessTerminalBlockEntity.DATA_STORED_LOW) & 0xFFFFFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) data.get(AkaishiWirelessTerminalBlockEntity.DATA_CAPACITY_HIGH) << 32)
                | (data.get(AkaishiWirelessTerminalBlockEntity.DATA_CAPACITY_LOW) & 0xFFFFFFFFL);
    }

    public boolean isFormed() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_FORMED) == 1;
    }

    public int getInputCount() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_INPUT_COUNT);
    }

    public int getOutputCount() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_OUTPUT_COUNT);
    }

    public int getBoundSerializers() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_BOUND_SERIALIZERS);
    }

    public int getAuthorizedCount() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_AUTHORIZED);
    }

    /** 跨维度是否已解锁（内腔含终端跨维组件） */
    public boolean isCrossDim() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_CROSS_DIM) == 1;
    }

    /** 区块加载是否已启用（内腔含区块加载构架） */
    public boolean isChunkLoad() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_CHUNK_LOAD) == 1;
    }

    /** 区块加载范围是否已扩展为 3×3（内腔含区块加载扩展组件） */
    public boolean isChunkRange() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_CHUNK_RANGE) == 1;
    }

    /** 当前弱加载区块数（区块加载构架生效时 >0） */
    public int getChunkLoaded() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_CHUNK_LOADED);
    }

    /** 区块加载能量税是否因能量不足而停用（网络区块加载已关闭） */
    public boolean isChunkTaxDisabled() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_TAX_DISABLED) == 1;
    }

    /** 内腔输入损耗抑制组件数量 */
    public int inputLossModules() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_INPUT_LOSS);
    }

    /** 内腔输出损耗抑制组件数量 */
    public int outputLossModules() {
        return data.get(AkaishiWirelessTerminalBlockEntity.DATA_OUTPUT_LOSS);
    }

    /** 终端短 ID（8 位 hex，与身份卡 ID 同格式） */
    public String getTerminalShortId() {
        return String.format("%08X", data.get(AkaishiWirelessTerminalBlockEntity.DATA_TERMINAL_ID));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
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

    /** 方块实体缺失兜底（客户端空数据构造） */
    public static AkaishiWirelessTerminalMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiWirelessTerminalMenu(id, inv,
                new SimpleContainerData(AkaishiWirelessTerminalBlockEntity.DATA_SLOTS));
    }

    /** 打开时的初始页面（客户端经网络缓冲设置） */
    public void setInitialPage(int page) {
        this.initialPage = page;
    }

    public int getInitialPage() {
        return initialPage;
    }

    /** 已授权卡上限（GUI 显示 x/8） */
    public int maxAuthorized() {
        return WirelessNetworkManager.MAX_AUTHORIZED_CARDS;
    }
}
