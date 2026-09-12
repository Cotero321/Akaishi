package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiLifeActivatorBlockEntity;
import com.example.akaishi.util.LongDataSlots;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命活化器菜单：无机器槽位（纯液体转化），仅玩家背包 + 14 个数据槽同步。
 * 数据槽：0/1=生命能量 2/3=生命容量 4/5=输入量 6/7=输入容量
 * 8/9=输出量 10/11=输出容量 12/13=累计活化量（long 拆低/高 32 位双槽）。
 */
public class AkaishiLifeActivatorMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public AkaishiLifeActivatorMenu(int id, Inventory inv, AkaishiLifeActivatorBlockEntity be) {
        this(id, inv, be.data());
    }

    public AkaishiLifeActivatorMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_LIFE_ACTIVATOR.get(), id);
        this.data = data;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public long getLifeEnergy() {
        return LongDataSlots.read(data, AkaishiLifeActivatorBlockEntity.DATA_LIFE_ENERGY,
                AkaishiLifeActivatorBlockEntity.DATA_LIFE_ENERGY_HIGH);
    }

    public long getLifeMax() {
        return LongDataSlots.read(data, AkaishiLifeActivatorBlockEntity.DATA_LIFE_CAPACITY,
                AkaishiLifeActivatorBlockEntity.DATA_LIFE_CAPACITY_HIGH);
    }

    public long getInAmount() {
        return LongDataSlots.read(data, AkaishiLifeActivatorBlockEntity.DATA_IN_AMOUNT,
                AkaishiLifeActivatorBlockEntity.DATA_IN_AMOUNT_HIGH);
    }

    public long getInMax() {
        return LongDataSlots.read(data, AkaishiLifeActivatorBlockEntity.DATA_IN_CAPACITY,
                AkaishiLifeActivatorBlockEntity.DATA_IN_CAPACITY_HIGH);
    }

    public long getOutAmount() {
        return LongDataSlots.read(data, AkaishiLifeActivatorBlockEntity.DATA_OUT_AMOUNT,
                AkaishiLifeActivatorBlockEntity.DATA_OUT_AMOUNT_HIGH);
    }

    public long getOutMax() {
        return LongDataSlots.read(data, AkaishiLifeActivatorBlockEntity.DATA_OUT_CAPACITY,
                AkaishiLifeActivatorBlockEntity.DATA_OUT_CAPACITY_HIGH);
    }

    /** 累计活化量（mb）：槽 DATA_PROCESSED_LOW/HIGH 为低/高 16 位段 */
    public long getProcessed() {
        return LongDataSlots.read(data, AkaishiLifeActivatorBlockEntity.DATA_PROCESSED_LOW,
                AkaishiLifeActivatorBlockEntity.DATA_PROCESSED_HIGH);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // 无机器槽位，物品仅在玩家背包间移动
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
