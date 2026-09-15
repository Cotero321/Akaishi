package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiLifeWirelessTerminalBlockEntity;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import com.example.akaishi.util.LongDataSlots;
import com.example.akaishi.wireless.WirelessFamily;
import com.example.akaishi.wireless.WirelessNetworkManager;
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
 * 无线生命便捷终端菜单（手持物品，只读遥控面板，赤版便捷终端的生命镜像）：
 * 无方块实体；服务端每 tick broadcastChanges 扫描玩家背包中的第一张身份卡（取第一张），
 * 按 {@link WirelessFamily#LIFE} 族反查授权该卡的在线生命终端并把其状态
 * （成型/储能/口统计/卡与终端短 ID）写入数据槽，随原版数据槽同步推送给客户端。
 * 手持终端不传输能量，仅作状态面板；与赤版便携终端共用身份卡，族隔离保证互不误命中。
 */
public class AkaishiLifeWirelessPortableTerminalMenu extends AbstractContainerMenu {

    public static final int DATA_STORED_LOW = 0;
    public static final int DATA_STORED_HIGH = 1;
    public static final int DATA_CAPACITY_LOW = 2;
    public static final int DATA_CAPACITY_HIGH = 3;
    public static final int DATA_FORMED = 4;
    public static final int DATA_INPUT_COUNT = 5;
    public static final int DATA_OUTPUT_COUNT = 6;
    public static final int DATA_CARD_HASH = 7;
    public static final int DATA_TERMINAL_HASH = 8;
    /** 卡短 ID 高 16 位（低 16 位见 {@link #DATA_CARD_HASH}）：数据槽仅 16 位有效，8 位 hex 短 ID 需拆 2 槽 */
    public static final int DATA_CARD_HASH_HIGH = 9;
    /** 终端短 ID 高 16 位（低 16 位见 {@link #DATA_TERMINAL_HASH}） */
    public static final int DATA_TERMINAL_HASH_HIGH = 10;
    /** 储能 64 位高段：终端储能/容量超过 2^31（生命串联器聚合 10.42 亿/台）时，低 32 位槽 0..3 无法承载，追加高 32 位槽 */
    public static final int DATA_STORED_HIGH2 = 11;
    public static final int DATA_STORED_HIGH3 = 12;
    public static final int DATA_CAPACITY_HIGH2 = 13;
    public static final int DATA_CAPACITY_HIGH3 = 14;
    public static final int DATA_SLOTS = 15;

    private final ContainerData data = new SimpleContainerData(DATA_SLOTS);
    private final Player player;

    public AkaishiLifeWirelessPortableTerminalMenu(int id, Inventory inv, Player player) {
        super(ModMenus.CHISHI_LIFE_WIRELESS_PORTABLE_TERMINAL.get(), id);
        this.player = player;
        // 198 高 GUI：玩家背包 3 行 y=124 起，快捷栏 y=180（与 akaishi_wireless_terminal.png 槽位图案对齐）
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

    /** 服务端刷新数据槽：取背包第一张身份卡，按生命族反查其授权终端的在线状态（无全图扫描） */
    private void refreshData(Level level) {
        ItemStack card = findCard(player);
        // 服务端逻辑：为背包新卡生成唯一卡号，保证能反查授权终端
        UUID cardUuid = card.isEmpty() ? null : AkaishiWirelessIdentityCardItem.ensureUuid(card);
        LongDataSlots.writeInt(data, DATA_CARD_HASH, DATA_CARD_HASH_HIGH,
                cardUuid == null ? 0 : (int) (cardUuid.getMostSignificantBits() >>> 32));

        UUID terminalId = WirelessNetworkManager.findTerminalForCard(cardUuid, WirelessFamily.LIFE);
        boolean formed = false;
        long stored = 0;
        long max = 0;
        int input = 0;
        int output = 0;
        if (terminalId != null) {
            LongDataSlots.writeInt(data, DATA_TERMINAL_HASH, DATA_TERMINAL_HASH_HIGH,
                    (int) (terminalId.getMostSignificantBits() >>> 32));
            WirelessNetworkManager.TerminalRef tr = WirelessNetworkManager.terminalOf(terminalId);
            if (tr != null && tr.dimension().equals(level.dimension())
                    && level.getBlockEntity(tr.pos()) instanceof AkaishiLifeWirelessTerminalBlockEntity t && t.isFormed()) {
                formed = true;
                stored = t.cachedStored();
                max = t.cachedMax();
                input = WirelessNetworkManager.inputCount(terminalId);
                output = WirelessNetworkManager.outputCount(terminalId);
            }
        } else {
            LongDataSlots.writeInt(data, DATA_TERMINAL_HASH, DATA_TERMINAL_HASH_HIGH, 0);
        }
        LongDataSlots.write(data, DATA_STORED_LOW, DATA_STORED_HIGH, DATA_STORED_HIGH2, DATA_STORED_HIGH3, stored);
        LongDataSlots.write(data, DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH, DATA_CAPACITY_HIGH2, DATA_CAPACITY_HIGH3, max);
        data.set(DATA_FORMED, formed ? 1 : 0);
        data.set(DATA_INPUT_COUNT, input);
        data.set(DATA_OUTPUT_COUNT, output);
    }

    /** 扫描玩家背包取第一张身份卡（未持有则空栈） */
    public static ItemStack findCard(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof AkaishiWirelessIdentityCardItem) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    public long getEnergy() {
        return LongDataSlots.read(data, DATA_STORED_LOW, DATA_STORED_HIGH, DATA_STORED_HIGH2, DATA_STORED_HIGH3);
    }

    public long getMaxEnergy() {
        return LongDataSlots.read(data, DATA_CAPACITY_LOW, DATA_CAPACITY_HIGH, DATA_CAPACITY_HIGH2, DATA_CAPACITY_HIGH3);
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

    /** 背包身份卡短 ID（8 位 hex；0=未持有卡；低/高 2 槽按 16 位段重组） */
    public int getCardHash() {
        return LongDataSlots.readInt(data, DATA_CARD_HASH, DATA_CARD_HASH_HIGH);
    }

    /** 认证终端短 ID（8 位 hex；0=未连接；低/高 2 槽按 16 位段重组） */
    public int getTerminalHash() {
        return LongDataSlots.readInt(data, DATA_TERMINAL_HASH, DATA_TERMINAL_HASH_HIGH);
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
