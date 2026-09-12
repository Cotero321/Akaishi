package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiCatalystBlockEntity;
import com.example.akaishi.util.LongDataSlots;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赤石催化器菜单：无机器槽位，仅玩家背包 + 数据槽（能量/容量/工作标志）。
 * 数据经原版 ContainerData 同步，GUI 据此显示工作状态与能量条。
 */
public class AkaishiCatalystMenu extends AbstractContainerMenu {

    private final ContainerData data;

    /** 服务端/客户端通用构造（数据槽随网络同步） */
    public AkaishiCatalystMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_CATALYST.get(), id);
        this.data = data;
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

    /** 当前能量（50000 > short，拆两槽重组） */
    public int getEnergy() {
        return (int) LongDataSlots.read(data, AkaishiCatalystBlockEntity.DATA_ENERGY,
                AkaishiCatalystBlockEntity.DATA_ENERGY_HIGH);
    }

    /** 能量容量（50000 > short，拆两槽重组） */
    public int getEnergyCapacity() {
        return (int) LongDataSlots.read(data, AkaishiCatalystBlockEntity.DATA_CAPACITY,
                AkaishiCatalystBlockEntity.DATA_CAPACITY_HIGH);
    }

    /** 是否正在催化（能量充足） */
    public boolean isWorking() {
        return data.get(AkaishiCatalystBlockEntity.DATA_WORKING) == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 无机器槽位，仅玩家背包，无需快速移动
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
