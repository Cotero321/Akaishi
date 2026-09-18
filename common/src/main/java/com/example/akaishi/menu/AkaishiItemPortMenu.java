package com.example.akaishi.menu;

import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.block.entity.AkaishiItemPortBlockEntity;
import com.example.akaishi.api.storage.IItemTerminalHost;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.wireless.ItemTerminalRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 储存无线输入/输出口菜单（输入口/输出口共用，两页：运行 / 绑定）。
 * 方向/绑定态/上次搬运件数/绑定身份短号经数据槽同步；
 * 绑定清单走 {@link AkaishiItemPortBindingSync} 的 S2C 快照（服务端算，客户端只读）。
 * <p>
 * <b>过滤网</b>：9 格真槽位（{@code slots} 下标 36..44，玩家槽之后），背后挂
 * {@link ItemPortFilterContainer} —— 服务端直读写方块实体的过滤数组（单一数据源），
 * 客户端为本机镜像，内容随原版 menu 槽同步自动下发。取放全走原版点击管线：
 * 光标拿物品点入 = 放 1 个进过滤槽，点已配置的槽 = 取回过滤物。
 */
public class AkaishiItemPortMenu extends AbstractContainerMenu implements AkaishiItemPortBindingSync.Target {

    /** 解绑当前终端（服务端执行） */
    public static final int BTN_UNBIND = 0;

    // ===== 运行页过滤网几何（3×3 真槽位；界面自绘框与槽位坐标同源，天然对齐） =====
    /** 网格外框左上角（cell 原点；槽位 = cell + 1，留给原版槽框的 1px 边） */
    public static final int FILTER_X = 110;
    public static final int FILTER_Y = 48;
    /** 单格边长（原版 18×18 = 16 物品区 + 双侧 1px） */
    public static final int FILTER_CELL = 18;
    /** 格间距 */
    public static final int FILTER_GAP = 2;
    /** 列数（3×3） */
    public static final int FILTER_COLS = 3;
    /** 网格外框宽（含格间距） */
    public static final int FILTER_W = FILTER_COLS * FILTER_CELL + (FILTER_COLS - 1) * FILTER_GAP;
    /** 过滤槽在 {@code slots} 里的首下标（玩家 36 槽之后；不占用、不重排任何 DATA_* 下标） */
    public static final int FILTER_SLOT_START = 36;

    /** 界面有效距离（8 格）的平方：与物品终端同口径 */
    private static final double MAX_DISTANCE_SQR = 64.0D;

    private final ContainerData data;
    /** 服务端为方块实体；客户端兜底路径为 null（数据仍经数据槽同步） */
    private final AkaishiItemPortBlockEntity host;
    private final Player player;
    /** 绑定页快照（客户端渲染只读） */
    private List<AkaishiItemPortBindingSync.Entry> bindingEntries = List.of();
    /** 已绑定终端标签（客户端；空串=未绑定） */
    private String boundLabel = "";
    /** 服务端：已推送的清单签名（内容变化才推） */
    private String sentBindingSignature = "";
    /** 过滤网 9 格容器：直读写方块实体过滤数组（客户端镜像承接原版槽同步） */
    private final ItemPortFilterContainer filterContainer;
    /** 过滤槽是否激活（只在运行页激活；绑定页隐藏，免得点进看不见的格子） */
    private boolean filterSlotsActive = true;

    /** 第 slot 格的槽位 x（= {@code Slot#x}；界面据此画框，两处同源不会错位） */
    public static int filterSlotX(int slot) {
        return FILTER_X + (slot % FILTER_COLS) * (FILTER_CELL + FILTER_GAP) + 1;
    }

    /** 第 slot 格的槽位 y（= {@code Slot#y}） */
    public static int filterSlotY(int slot) {
        return FILTER_Y + (slot / FILTER_COLS) * (FILTER_CELL + FILTER_GAP) + 1;
    }

    /** 服务端构造 */
    public AkaishiItemPortMenu(int id, Inventory inv, AkaishiItemPortBlockEntity host) {
        super(ModMenus.CHISHI_ITEM_PORT.get(), id);
        this.data = host.data();
        this.host = host;
        this.player = inv.player;
        this.filterContainer = new ItemPortFilterContainer(host);
        addPlayerSlots(inv);
        addFilterSlots();
        this.addDataSlots(data);
    }

    /** 客户端兜底构造（方块实体缺失；数据经数据槽同步，过滤槽走本机镜像容器） */
    public AkaishiItemPortMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_ITEM_PORT.get(), id);
        this.data = data;
        this.host = null;
        this.player = inv.player;
        this.filterContainer = new ItemPortFilterContainer(null);
        addPlayerSlots(inv);
        addFilterSlots();
        this.addDataSlots(data);
    }

    // ===== 绑定页：服务端推送 + 动作 =====

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (host == null || !(this.player instanceof ServerPlayer serverPlayer)
                || serverPlayer.containerMenu != this) {
            return;
        }
        List<AkaishiItemPortBindingSync.Entry> entries = buildBindingEntries(serverPlayer);
        String bound = host.boundTargetLabel();
        // 清单内容 + 当前绑定 一起做签名：任一变化（含终端上下线）即重推
        String signature = bound + '|' + entries.size() + '|' + entries.hashCode();
        if (!signature.equals(this.sentBindingSignature)) {
            this.sentBindingSignature = signature;
            AkaishiItemPortBindingSync.sendSnapshot(serverPlayer, this.containerId, entries, bound);
        }
        // 过滤网不再走自定义包：9 格真槽位由 super.broadcastChanges() 的原版槽同步接管
    }

    /** 可绑定清单：该玩家具备「布局」权限且已加载的同维度物品终端（服务端算，客户端只读） */
    private List<AkaishiItemPortBindingSync.Entry> buildBindingEntries(ServerPlayer serverPlayer) {
        if (!(serverPlayer.level() instanceof ServerLevel level)) {
            return List.of();
        }
        ResourceLocation dimension = level.dimension().location();
        List<AkaishiItemPortBindingSync.Entry> entries = new ArrayList<>();
        for (ItemTerminalRegistry.Entry entry : ItemTerminalRegistry.bindableTerminals(level, serverPlayer)) {
            entries.add(new AkaishiItemPortBindingSync.Entry(entry.terminalId(), dimension,
                    entry.pos(), entry.ownerName() == null ? "" : entry.ownerName()));
        }
        return entries;
    }

    /** 客户端：接收清单快照 */
    @Override
    public void acceptBinding(List<AkaishiItemPortBindingSync.Entry> entries, String boundLabel) {
        this.bindingEntries = List.copyOf(entries);
        this.boundLabel = boundLabel == null ? "" : boundLabel;
    }

    /** 服务端：绑定 / 解绑（绑定复核目标终端「布局」权限，op 等级 4 放行；解绑只看本口绑定态） */
    @Override
    public void applyBindingAction(Player actor, byte action, UUID terminalId) {
        if (host == null || actor == null) {
            return;
        }
        if (!mayReconfigure(actor)) {
            actor.displayClientMessage(Component.translatable("message.akaishi.item_port.not_owner"), true);
            return;
        }
        if (action == AkaishiItemPortBindingSync.ACTION_UNBIND) {
            host.unbind();
            actor.displayClientMessage(Component.translatable("message.akaishi.item_port.unbound"), true);
            return;
        }
        if (terminalId == null || actor.getServer() == null) {
            return;
        }
        IItemTerminalHost terminal = ItemTerminalRegistry.resolve(actor.getServer(), terminalId);
        // 权限复核：终端本地权威安全表（未登记条目时全放行）+ op 放行
        if (terminal == null || (!actor.hasPermissions(4)
                && !terminal.security().check(actor.getUUID(), AkaishiSecurityPermission.BUILD))) {
            actor.displayClientMessage(Component.translatable("message.akaishi.item_port.denied"), true);
            return;
        }
        host.bindTerminal(terminalId, actor.getUUID());
        actor.displayClientMessage(Component.translatable("message.akaishi.item_port.bound",
                host.boundTargetLabel()), true);
    }

    // ===== 运行页：过滤网（AE2 总线配置槽口径的真槽位） =====

    /**
     * 过滤槽内容（= 过滤条件；全空 = 全通）。
     * 服务端直读方块实体过滤数组，客户端读原版槽同步下来的镜像 —— 只有一个数据源，不存在脱节。
     */
    public ItemStack filterSlot(int slot) {
        return filterContainer.getItem(slot);
    }

    /** 过滤槽是否激活（界面按页面切换：只在运行页激活） */
    public boolean filterSlotsActive() {
        return this.filterSlotsActive;
    }

    /** 由界面按页面切换（绑定页置 false：否则隐藏的格子仍会被点击命中） */
    public void setFilterSlotsActive(boolean active) {
        this.filterSlotsActive = active;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 过滤物常驻方块实体过滤数组（NBT 持久化，破坏方块随掉落物带走），正常路径无需返还；
        // 唯方块实体缺失（跨维度/超距兜底构造）时数据无处安放，才把槽内过滤物还回玩家背包，防丢失
        if (host != null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        for (int slot = 0; slot < AkaishiItemPortBlockEntity.FILTER_SLOTS; slot++) {
            ItemStack stack = filterContainer.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) {
                serverPlayer.getInventory().placeItemBackInInventory(stack);
            }
        }
    }

    /** 绑定页：清单（客户端只读） */
    public List<AkaishiItemPortBindingSync.Entry> bindingEntries() {
        return bindingEntries;
    }

    /** 绑定页/运行页：已绑定终端标签（客户端；空串=未绑定） */
    public String boundLabel() {
        return boundLabel;
    }

    private void addPlayerSlots(Inventory inv) {
        // 198 高 GUI：玩家背包 3 行自 y=124 起，快捷栏 y=180
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
    }

    /**
     * 过滤网 9 格真槽位（占 {@code slots} 下标 {@value #FILTER_SLOT_START}..44，挂在玩家 36 槽之后，
     * 玩家槽下标与 {@code quickMoveStack} 的 0..35 口径不受影响）。
     */
    private void addFilterSlots() {
        for (int slot = 0; slot < AkaishiItemPortBlockEntity.FILTER_SLOTS; slot++) {
            this.addSlot(new ItemPortFilterSlot(this, filterContainer, slot,
                    filterSlotX(slot), filterSlotY(slot)));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (host == null || id != BTN_UNBIND || !mayReconfigure(player)) {
            return false;
        }
        host.unbind();
        return true;
    }

    /**
     * 是否有权改绑/解绑本口。
     * <p>
     * 端口本身没有"归属者"字段，绑定身份就是归属凭据：未绑定时谁都能先占位（目标终端另有 BUILD 校验），
     * 一旦绑上，就只有 <b>op / 原绑定者本人 / 对当前绑定终端有「布局」权限的人</b>能改绑或解绑 ——
     * 否则任何路人右键一次就能把别人的物流改投到自己终端，或直接解绑让别人的口失效。
     */
    private boolean mayReconfigure(Player actor) {
        if (host == null || actor == null) {
            return false;
        }
        if (!host.isBound() || actor.hasPermissions(4)) {
            return true;
        }
        if (actor.getUUID().equals(host.boundIdentityView())) {
            return true;
        }
        if (actor.getServer() == null) {
            return false;
        }
        IItemTerminalHost bound = ItemTerminalRegistry.resolve(actor.getServer(), host.boundTerminalIdView());
        return bound != null && bound.security().check(actor.getUUID(), AkaishiSecurityPermission.BUILD);
    }

    /** 是否已绑定终端（数据槽同步） */
    public boolean isBound() {
        return data.get(AkaishiItemPortBlockEntity.DATA_BOUND) != 0;
    }

    /** 是否为输出口（数据槽同步；客户端/服务端一致） */
    public boolean isOutput() {
        return data.get(AkaishiItemPortBlockEntity.DATA_IS_OUTPUT) != 0;
    }

    /** 上次搬运件数（数据槽同步） */
    public int lastMoved() {
        return data.get(AkaishiItemPortBlockEntity.DATA_LAST_MOVED);
    }

    /** 上次搬运尝试是否因赤能源不足整批跳过（数据槽同步；运行页红字告警） */
    public boolean energyShort() {
        return data.get(AkaishiItemPortBlockEntity.DATA_ENERGY_SHORT) != 0;
    }

    /** 绑定身份短 ID（8 位 hex；0=未绑定；低/高 2 槽按 16 位段重组） */
    public int identityHash() {
        return LongDataSlots.readInt(data, AkaishiItemPortBlockEntity.DATA_IDENTITY_HASH,
                AkaishiItemPortBlockEntity.DATA_IDENTITY_HASH_HIGH);
    }

    @Override
    public boolean stillValid(Player player) {
        // 与物品终端同口径的距离约束：否则走远/传送后界面仍可用，过滤网与绑定关系可被远程改写
        return this.host != null && !this.host.isRemoved()
                && player.distanceToSqr(this.host.getBlockPos().getCenter()) <= MAX_DISTANCE_SQR;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 背包行 ↔ 快捷栏互搬；过滤槽（下标 ≥ FILTER_SLOT_START）不参与 Shift 搬运：
        // 取回/放置只走普通点击，避免 Shift 批量操作把配置好的过滤网顺手清空
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem() && index < FILTER_SLOT_START) {
            ItemStack current = slot.getItem();
            result = current.copy();
            if (index < 27) {
                if (!this.moveItemStackTo(current, 27, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 0, 27, false)) {
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
}
