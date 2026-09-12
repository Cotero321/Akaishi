package com.example.akaishi.menu;

import com.example.akaishi.util.LongDataSlots;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 聚变能量输出口菜单：无机器槽位，仅玩家背包 + 能量/容量数据展示。
 * 能量与容量为 long，各拆 4 个 int 数据槽同步（0..3=能量 4 段，4..7=容量 4 段）。
 * 容量配置达 200 亿（超 2^32），必须用 4 槽版避免高位截断。
 */
public class AkaishiFusionEnergyOutputMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public AkaishiFusionEnergyOutputMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_FUSION_ENERGY_OUTPUT.get(), id);
        this.data = data;

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    /** 缓冲能量（槽 0..3 为 4 个 16 位段） */
    public long getEnergy() {
        return LongDataSlots.read(data, 0, 1, 2, 3);
    }

    /** 缓冲容量（槽 4..7 为 4 个 16 位段） */
    public long getMaxEnergy() {
        return LongDataSlots.read(data, 4, 5, 6, 7);
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

    /** 供无方块实体兜底时使用的空菜单（数据全 0） */
    public static AkaishiFusionEnergyOutputMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiFusionEnergyOutputMenu(id, inv, new SimpleContainerData(8));
    }
}
