package com.example.template.menu;

import com.example.template.block.entity.ChishiFuelMixerBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 燃料混合器菜单：无机器槽位（纯液体调和），仅玩家背包 + 9 个数据槽同步。
 * 数据槽：0/1=赤能量/容量 2/3=输入1 4/5=输入2 6/7=输出 8=混合进度。
 */
public class ChishiFuelMixerMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public ChishiFuelMixerMenu(int id, Inventory inv, ChishiFuelMixerBlockEntity be) {
        this(id, inv, be.data());
    }

    public ChishiFuelMixerMenu(int id, Inventory inv, ContainerData data) {
        super(ModMenus.CHISHI_FUEL_MIXER.get(), id);
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

    public long getChishiEnergy() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_CHISHI_ENERGY);
    }

    public long getChishiMax() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_CHISHI_CAPACITY);
    }

    public long getIn1Amount() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_IN1_AMOUNT);
    }

    public long getIn1Max() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_IN1_CAPACITY);
    }

    public long getIn2Amount() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_IN2_AMOUNT);
    }

    public long getIn2Max() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_IN2_CAPACITY);
    }

    public long getOutAmount() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_OUT_AMOUNT);
    }

    public long getOutMax() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_OUT_CAPACITY);
    }

    /** 混合进度（0-100） */
    public int getProgress() {
        return data.get(ChishiFuelMixerBlockEntity.DATA_PROGRESS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 无机器槽位，直接返回（物品仅在玩家背包间移动）
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
