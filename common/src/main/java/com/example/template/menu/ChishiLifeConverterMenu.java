package com.example.template.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 生命转换菜单（生命聚合转换器 / 生命转换架构共用）：
 * 无机器槽位，展示赤能源（输入缓冲）与生命能量（输出缓冲）+ 结构状态。
 * 数据槽：0/1=赤能源/赤容量，2/3=生命能量/生命容量，4=结构状态（1=成型）。
 */
public class ChishiLifeConverterMenu extends AbstractContainerMenu {

    private static final Container EMPTY = new SimpleContainer(0);

    private final ContainerData data;

    public ChishiLifeConverterMenu(int id, Inventory playerInv, ContainerData data) {
        super(ModMenus.CHISHI_LIFE_CONVERTER.get(), id);
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

    /** 当前赤能源储量 */
    public long getChishiEnergy() {
        return data.get(0);
    }

    /** 赤能源容量上限 */
    public long getChishiMax() {
        return data.get(1);
    }

    /** 当前生命能量储量 */
    public long getLifeEnergy() {
        return data.get(2);
    }

    /** 生命能量容量上限 */
    public long getLifeMax() {
        return data.get(3);
    }

    /** 结构是否成型（生命转换架构） */
    public boolean isFormed() {
        return data.get(4) == 1;
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
    public static ChishiLifeConverterMenu emptyMenu(int id, Inventory inv) {
        return new ChishiLifeConverterMenu(id, inv, new SimpleContainerData(5));
    }
}
