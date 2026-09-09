package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiExhaustedBarrelBlockEntity;
import com.example.akaishi.util.LongDataSlots;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 衰竭保存桶菜单：无机器槽位，仅玩家背包 + 液体量/容量数据。
 * 数据槽：0/1=液体量 2/3=容量（long 各占高低两槽）。
 */
public class AkaishiExhaustedBarrelMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public AkaishiExhaustedBarrelMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_EXHAUSTED_BARREL.get(), id);
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

    /** 当前液体储量（mb） */
    public long getFluidAmount() {
        return LongDataSlots.read(data, AkaishiExhaustedBarrelBlockEntity.DATA_AMOUNT,
                AkaishiExhaustedBarrelBlockEntity.DATA_AMOUNT_HIGH);
    }

    /** 液体容量上限（mb） */
    public long getFluidMax() {
        return LongDataSlots.read(data, AkaishiExhaustedBarrelBlockEntity.DATA_CAPACITY,
                AkaishiExhaustedBarrelBlockEntity.DATA_CAPACITY_HIGH);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** 供无方块实体兜底时使用的空菜单（数据全 0） */
    public static AkaishiExhaustedBarrelMenu emptyMenu(int id, Inventory inv) {
        return new AkaishiExhaustedBarrelMenu(id, inv, new SimpleContainerData(AkaishiExhaustedBarrelBlockEntity.DATA_SLOTS));
    }
}
