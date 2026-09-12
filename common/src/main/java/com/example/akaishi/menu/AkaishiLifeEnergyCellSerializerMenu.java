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
 * 生命储存串联器菜单：无机器槽位，仅展示总生命能量/总容量与结构状态。
 * 数据槽：0/1=总能量低/高位，2/3=总容量低/高位，4=结构状态（1=成型）。
 * 玩家背包槽位（3 行 + 快捷栏）供常规交互。
 */
public class AkaishiLifeEnergyCellSerializerMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public AkaishiLifeEnergyCellSerializerMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_LIFE_ENERGY_CELL_SERIALIZER.get(), id);
        this.data = data;

        // 玩家背包 3 行 × 9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        this.addDataSlots(data);
    }

    /** 当前总储量（槽 0/1 为低/高 16 位段） */
    public long getEnergy() {
        return LongDataSlots.read(data, 0, 1);
    }

    /** 总容量上限（槽 2/3 为低/高 16 位段） */
    public long getMaxEnergy() {
        return LongDataSlots.read(data, 2, 3);
    }

    /** 结构是否成型 */
    public boolean isFormed() {
        return data.get(4) == 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 无机器槽位，仅背包内部移动
        return ItemStack.EMPTY;
    }

    /** 供无方块实体兜底时使用的空菜单（数据全 0） */
    public static AkaishiLifeEnergyCellSerializerMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiLifeEnergyCellSerializerMenu(id, inv, new SimpleContainerData(5));
    }
}
