package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiWirelessInputPortBlockEntity;
import com.example.akaishi.wireless.IWirelessPortHost;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 无线赤能源端口菜单（输入口/输出口共用，两类页面：运行情况/传输情况）。
 * 无机器槽位；缓冲储能 + 绑定卡 + 认证状态经数据槽同步。
 * 页面切换由 Screen 本地状态互斥切换（互不重叠）；解绑走 clickMenuButton（服务端经 {@link IWirelessPortHost} 生效）。
 */
public class AkaishiWirelessPortMenu extends AbstractContainerMenu {

    /** 解绑身份卡（服务端执行） */
    public static final int BTN_UNBIND = 0;

    private final ContainerData data;
    private final IWirelessPortHost host;

    /** 服务端构造 */
    public AkaishiWirelessPortMenu(int id, Inventory inv, IWirelessPortHost host) {
        super(ModMenus.CHISHI_WIRELESS_PORT.get(), id);
        this.data = host.data();
        this.host = host;
        addPlayerSlots(inv);
        this.addDataSlots(data);
    }

    /** 客户端构造（数据经数据槽同步，无 host） */
    public AkaishiWirelessPortMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_WIRELESS_PORT.get(), id);
        this.data = data;
        this.host = null;
        addPlayerSlots(inv);
        this.addDataSlots(data);
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
        if (host == null || id != BTN_UNBIND) {
            return false;
        }
        host.unbind();
        return true;
    }

    public long getEnergy() {
        return ((long) data.get(AkaishiWirelessInputPortBlockEntity.DATA_STORED_HIGH) << 32)
                | (data.get(AkaishiWirelessInputPortBlockEntity.DATA_STORED_LOW) & 0xFFFFFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) data.get(AkaishiWirelessInputPortBlockEntity.DATA_CAPACITY_HIGH) << 32)
                | (data.get(AkaishiWirelessInputPortBlockEntity.DATA_CAPACITY_LOW) & 0xFFFFFFFFL);
    }

    /** 绑定卡短 ID（8 位 hex；0=未绑定） */
    public int getCardHash() {
        return data.get(AkaishiWirelessInputPortBlockEntity.DATA_CARD_HASH);
    }

    /** 认证终端短 ID（8 位 hex；0=未认证） */
    public int getTerminalHash() {
        return data.get(AkaishiWirelessInputPortBlockEntity.DATA_TERMINAL_HASH);
    }

    public boolean isAuthenticated() {
        return data.get(AkaishiWirelessInputPortBlockEntity.DATA_AUTHENTICATED) != 0;
    }

    /** 是否为输出口（经方向数据槽同步，客户端/服务端一致） */
    public boolean isOutput() {
        return data.get(AkaishiWirelessInputPortBlockEntity.DATA_IS_OUTPUT) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
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
