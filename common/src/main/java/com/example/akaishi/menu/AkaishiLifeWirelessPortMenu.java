package com.example.akaishi.menu;

import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.block.entity.AkaishiLifeWirelessInputPortBlockEntity;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.wireless.IWirelessPortHost;
import com.example.akaishi.wireless.WirelessFamily;
import com.example.akaishi.wireless.WirelessNetworkManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 生命无线端口菜单（输入口/输出口共用，两类页面：运行情况/传输情况，生命能量版）。
 * 无机器槽位；缓冲储能 + 绑定卡 + 认证状态经数据槽同步。
 * 页面切换由 Screen 本地状态互斥切换（互不重叠）；解绑走 clickMenuButton（服务端经 {@link IWirelessPortHost} 生效）。
 */
public class AkaishiLifeWirelessPortMenu extends AbstractContainerMenu implements AkaishiPortBindingSync.Target {

    /** 解绑身份卡（服务端执行） */
    public static final int BTN_UNBIND = 0;

    private final ContainerData data;
    private final IWirelessPortHost host;
    private final Player player;
    /** 绑定页快照（客户端渲染只读） */
    private List<AkaishiPortBindingSync.Entry> bindingEntries = List.of();
    private String boundShortId = "";
    /** 服务端：已推送的清单签名（内容变化才推） */
    private String sentBindingSignature = "";

    /** 服务端构造 */
    public AkaishiLifeWirelessPortMenu(int id, Inventory inv, IWirelessPortHost host) {
        super(ModMenus.CHISHI_LIFE_WIRELESS_PORT.get(), id);
        this.data = host.data();
        this.host = host;
        this.player = inv.player;
        addPlayerSlots(inv);
        this.addDataSlots(data);
    }

    /** 客户端构造（数据经数据槽同步，无 host） */
    public AkaishiLifeWirelessPortMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_LIFE_WIRELESS_PORT.get(), id);
        this.data = data;
        this.host = null;
        this.player = inv.player;
        addPlayerSlots(inv);
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
        List<AkaishiPortBindingSync.Entry> entries = buildBindingEntries();
        String bound = host.boundTerminalShortId();
        // 清单内容 + 当前绑定 一起做签名：任一变化（含终端上下线）即重推
        String signature = bound + '|' + entries.size() + '|' + entries.hashCode();
        if (!signature.equals(this.sentBindingSignature)) {
            this.sentBindingSignature = signature;
            AkaishiPortBindingSync.sendSnapshot(serverPlayer, this.containerId, entries, bound);
        }
    }

    /** 可绑定清单：该玩家具备「布局」权限且在线的同族终端（服务端算，客户端只读） */
    private List<AkaishiPortBindingSync.Entry> buildBindingEntries() {
        List<AkaishiPortBindingSync.Entry> entries = new ArrayList<>();
        for (WirelessNetworkManager.TerminalEntry entry : WirelessNetworkManager.bindableTerminals(
                WirelessFamily.LIFE, this.player.getUUID())) {
            entries.add(new AkaishiPortBindingSync.Entry(entry.id, WirelessNetworkManager.shortId(entry.id),
                    entry.owner == null ? "" : WirelessNetworkManager.shortId(entry.owner)));
        }
        return entries;
    }

    /** 客户端：接收清单快照 */
    @Override
    public void acceptBinding(List<AkaishiPortBindingSync.Entry> entries, String bound) {
        this.bindingEntries = List.copyOf(entries);
        this.boundShortId = bound == null ? "" : bound;
    }

    /** 服务端：绑定 / 解绑（绑定要求目标终端「布局」权限；解绑只看本口归属） */
    @Override
    public void applyBindingAction(Player actor, byte action, UUID terminalId) {
        if (host == null || actor == null) {
            return;
        }
        if (!mayReconfigure(actor)) {
            actor.displayClientMessage(Component.translatable("message.akaishi.wireless.port.not_owner"), true);
            return;
        }
        if (action == AkaishiPortBindingSync.ACTION_UNBIND) {
            host.unbind();
            actor.displayClientMessage(Component.translatable("message.akaishi.wireless.port.unbound"), true);
            return;
        }
        if (terminalId == null) {
            return;
        }
        if (!WirelessNetworkManager.hasPermission(terminalId, actor, AkaishiSecurityPermission.BUILD)) {
            actor.displayClientMessage(Component.translatable("message.akaishi.wireless.port.denied"), true);
            return;
        }
        host.bindTerminal(terminalId, actor.getUUID());
        actor.displayClientMessage(Component.translatable("message.akaishi.wireless.port.bound",
                WirelessNetworkManager.shortId(terminalId)), true);
    }

    /** 绑定页：清单（客户端） */
    public List<AkaishiPortBindingSync.Entry> bindingEntries() {
        return bindingEntries;
    }

    /** 绑定页：当前绑定终端短号（客户端；空串=未绑定） */
    public String boundShortId() {
        return boundShortId;
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
        if (host == null || id != BTN_UNBIND || !mayReconfigure(player)) {
            return false;
        }
        host.unbind();
        return true;
    }

    /**
     * 是否有权改绑 / 解绑本口（服务端判定）。
     * <p>
     * 端口没有独立归属字段，<b>绑定者本人即归属凭据</b>：未绑定时谁都能先占位（目标终端另有「布局」校验），
     * 一旦绑上，就只有 op / 原绑定者本人 / 对当前绑定终端有「布局」权限的人能改绑或解绑。
     */
    private boolean mayReconfigure(Player actor) {
        if (host == null || actor == null) {
            return false;
        }
        if (host.bindingIdentity() == null || actor.hasPermissions(4)) {
            return true;
        }
        if (actor.getUUID().equals(host.bindingIdentity())) {
            return true;
        }
        UUID bound = host.boundTerminalId();
        return bound != null && WirelessNetworkManager.hasPermission(bound, actor.getUUID(),
                AkaishiSecurityPermission.BUILD);
    }

    public long getEnergy() {
        return LongDataSlots.read(data, AkaishiLifeWirelessInputPortBlockEntity.DATA_STORED_LOW,
                AkaishiLifeWirelessInputPortBlockEntity.DATA_STORED_HIGH);
    }

    public long getMaxEnergy() {
        return LongDataSlots.read(data, AkaishiLifeWirelessInputPortBlockEntity.DATA_CAPACITY_LOW,
                AkaishiLifeWirelessInputPortBlockEntity.DATA_CAPACITY_HIGH);
    }

    /** 绑定卡短 ID（8 位 hex；0=未绑定；低/高 2 槽按 16 位段重组） */
    public int getCardHash() {
        return LongDataSlots.readInt(data, AkaishiLifeWirelessInputPortBlockEntity.DATA_CARD_HASH,
                AkaishiLifeWirelessInputPortBlockEntity.DATA_CARD_HASH_HIGH);
    }

    /** 认证终端短 ID（8 位 hex；0=未认证；低/高 2 槽按 16 位段重组） */
    public int getTerminalHash() {
        return LongDataSlots.readInt(data, AkaishiLifeWirelessInputPortBlockEntity.DATA_TERMINAL_HASH,
                AkaishiLifeWirelessInputPortBlockEntity.DATA_TERMINAL_HASH_HIGH);
    }

    public boolean isAuthenticated() {
        return data.get(AkaishiLifeWirelessInputPortBlockEntity.DATA_AUTHENTICATED) != 0;
    }

    /** 是否为输出口（经方向数据槽同步，客户端/服务端一致） */
    public boolean isOutput() {
        return data.get(AkaishiLifeWirelessInputPortBlockEntity.DATA_IS_OUTPUT) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        // 8 格内才有效：与物品终端/储存口同口径，避免走远或传送后仍能远程改写绑定关系
        return host == null || player.distanceToSqr(host.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 无机器槽：仅处理背包行 ↔ 快捷栏
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
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
