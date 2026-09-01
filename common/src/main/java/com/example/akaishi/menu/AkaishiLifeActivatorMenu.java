package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiLifeActivatorBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命活化器菜单：无机器槽位（纯液体转化），仅玩家背包 + 7 个数据槽同步。
 * 数据槽：0/1=生命能量/容量 2/3=输入 4/5=输出 6=累计活化量。
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
        return data.get(AkaishiLifeActivatorBlockEntity.DATA_LIFE_ENERGY);
    }

    public long getLifeMax() {
        return data.get(AkaishiLifeActivatorBlockEntity.DATA_LIFE_CAPACITY);
    }

    public long getInAmount() {
        return data.get(AkaishiLifeActivatorBlockEntity.DATA_IN_AMOUNT);
    }

    public long getInMax() {
        return data.get(AkaishiLifeActivatorBlockEntity.DATA_IN_CAPACITY);
    }

    public long getOutAmount() {
        return data.get(AkaishiLifeActivatorBlockEntity.DATA_OUT_AMOUNT);
    }

    public long getOutMax() {
        return data.get(AkaishiLifeActivatorBlockEntity.DATA_OUT_CAPACITY);
    }

    /** 累计活化量（mb）：低/高 32 位双槽重组为 long，无精度损失 */
    public long getProcessed() {
        long low = data.get(AkaishiLifeActivatorBlockEntity.DATA_PROCESSED_LOW) & 0xFFFFFFFFL;
        long high = (long) data.get(AkaishiLifeActivatorBlockEntity.DATA_PROCESSED_HIGH) << 32;
        return high | low;
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
