package com.example.template.menu;

import com.example.template.block.entity.ChishiWirelessTerminalBlockEntity;
import com.example.template.item.ChishiWirelessIdentityCardItem;
import com.example.template.wireless.WirelessNetworkManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 无线能源便捷终端菜单（手持物品，只读遥控面板，参考 AE2 无线终端）：
 * 无方块实体；服务端每 tick broadcastChanges 扫描玩家背包中的身份卡（取第一张），
 * 反查授权该卡的在线终端并把其状态（成型/储能/口统计/卡与终端短 ID）写入数据槽，
 * 随原版数据槽同步推送给客户端。手持终端不传输能量，仅作状态面板。
 */
public class ChishiWirelessPortableTerminalMenu extends AbstractContainerMenu {

    public static final int DATA_STORED_LOW = 0;
    public static final int DATA_STORED_HIGH = 1;
    public static final int DATA_CAPACITY_LOW = 2;
    public static final int DATA_CAPACITY_HIGH = 3;
    public static final int DATA_FORMED = 4;
    public static final int DATA_INPUT_COUNT = 5;
    public static final int DATA_OUTPUT_COUNT = 6;
    public static final int DATA_CARD_HASH = 7;
    public static final int DATA_TERMINAL_HASH = 8;
    public static final int DATA_SLOTS = 9;

    private final ContainerData data = new SimpleContainerData(DATA_SLOTS);
    private final Player player;

    public ChishiWirelessPortableTerminalMenu(int id, Inventory inv, Player player) {
        super(ModMenus.CHISHI_WIRELESS_PORTABLE_TERMINAL.get(), id);
        this.player = player;
        // 198 高 GUI：玩家背包 3 行 y=124 起，快捷栏 y=180（与 chishi_wireless_terminal.png 槽位图案对齐）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }
        this.addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!player.level().isClientSide) {
            refreshData(player.level());
        }
    }

    /** 服务端刷新数据槽：取背包第一张身份卡，反查其授权终端的在线状态（无全图扫描） */
    private void refreshData(Level level) {
        ItemStack card = findCard(player);
        UUID cardUuid = card.isEmpty() ? null : ChishiWirelessIdentityCardItem.uuidOf(card);
        data.set(DATA_CARD_HASH, cardUuid == null ? 0 : (int) (cardUuid.getMostSignificantBits() >>> 32));

        UUID terminalId = WirelessNetworkManager.findTerminalForCard(cardUuid);
        boolean formed = false;
        long stored = 0;
        long max = 0;
        int input = 0;
        int output = 0;
        if (terminalId != null) {
            data.set(DATA_TERMINAL_HASH, (int) (terminalId.getMostSignificantBits() >>> 32));
            WirelessNetworkManager.TerminalRef tr = WirelessNetworkManager.terminalOf(terminalId);
            if (tr != null && tr.dimension().equals(level.dimension())
                    && level.getBlockEntity(tr.pos()) instanceof ChishiWirelessTerminalBlockEntity t && t.isFormed()) {
                formed = true;
                stored = t.cachedStored();
                max = t.cachedMax();
                input = WirelessNetworkManager.inputCount(terminalId);
                output = WirelessNetworkManager.outputCount(terminalId);
            }
        } else {
            data.set(DATA_TERMINAL_HASH, 0);
        }
        data.set(DATA_STORED_LOW, (int) stored);
        data.set(DATA_STORED_HIGH, (int) (stored >>> 32));
        data.set(DATA_CAPACITY_LOW, (int) max);
        data.set(DATA_CAPACITY_HIGH, (int) (max >>> 32));
        data.set(DATA_FORMED, formed ? 1 : 0);
        data.set(DATA_INPUT_COUNT, input);
        data.set(DATA_OUTPUT_COUNT, output);
    }

    /** 扫描玩家背包取第一张身份卡（未持有则空栈） */
    public static ItemStack findCard(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof ChishiWirelessIdentityCardItem) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    public long getEnergy() {
        return ((long) data.get(DATA_STORED_HIGH) << 32) | (data.get(DATA_STORED_LOW) & 0xFFFFFFFFL);
    }

    public long getMaxEnergy() {
        return ((long) data.get(DATA_CAPACITY_HIGH) << 32) | (data.get(DATA_CAPACITY_LOW) & 0xFFFFFFFFL);
    }

    public boolean isFormed() {
        return data.get(DATA_FORMED) == 1;
    }

    public int getInputCount() {
        return data.get(DATA_INPUT_COUNT);
    }

    public int getOutputCount() {
        return data.get(DATA_OUTPUT_COUNT);
    }

    /** 背包身份卡短 ID（8 位 hex；0=未持有卡） */
    public int getCardHash() {
        return data.get(DATA_CARD_HASH);
    }

    /** 认证终端短 ID（8 位 hex；0=未连接） */
    public int getTerminalHash() {
        return data.get(DATA_TERMINAL_HASH);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
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
