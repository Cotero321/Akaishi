package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiMinerControllerBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 赤石矿机控制器菜单：产物暂存 6 格（只读）+ 玩家背包 + 挖矿模式切换按钮。
 * 数据槽：0=能量 1=容量 2=进度 3=总耗时 4=成型 5=速度升级 6=时运升级 7=储能升级 8=挖矿模式（0 正常 / 1 精准）。
 */
public class AkaishiMinerControllerMenu extends AbstractContainerMenu {

    /** 挖矿模式切换按钮 id（正常 / 精准） */
    public static final int BTN_MODE_NORMAL = 1;
    public static final int BTN_MODE_PRECISE = 2;

    private final Container container;
    private final ContainerData data;
    /** 服务端菜单持有的控制器 BE（客户端菜单为 null；clickMenuButton 只在服务端触发） */
    private AkaishiMinerControllerBlockEntity be;

    public AkaishiMinerControllerMenu(int id, Inventory inv, AkaishiMinerControllerBlockEntity be) {
        this(id, inv, be.inventory(), be.data());
        this.be = be;
    }

    public AkaishiMinerControllerMenu(int id, Inventory inv, Container container, ContainerData data) {
        super(ModMenus.CHISHI_MINER_CONTROLLER.get(), id);
        this.container = container;
        this.data = data;

        // 产物暂存 3×2（只读，等待推送转口）
        int[] cols = {26, 44, 62};
        int[] rows = {40, 58};
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < cols.length; c++) {
                addSlot(new OutputSlot(container, r * cols.length + c, cols[c], rows[r]));
            }
        }

        // 玩家背包 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
            }
        }
        // 快捷栏 1×9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 180));
        }

        addDataSlots(data);
    }

    /** 只读产物槽（防止玩家取出/放入，产物由控制器自动推送转口） */
    private static class OutputSlot extends Slot {
        OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    public int getEnergy() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_ENERGY);
    }

    public int getCapacity() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_CAPACITY);
    }

    public int getProgress() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_PROGRESS);
    }

    public int getRequired() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_REQUIRED);
    }

    /** 结构是否成型 */
    public boolean isFormed() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_FORMED) == 1;
    }

    public int getSpeedCount() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_SPEED);
    }

    public int getFortuneCount() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_FORTUNE);
    }

    public int getStorageCount() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_STORAGE);
    }

    /** 当前是否为精准模式（客户端数据槽同步值） */
    public boolean isPrecise() {
        return data.get(AkaishiMinerControllerBlockEntity.DATA_MODE) == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) {
            return false;
        }
        if (id == BTN_MODE_NORMAL) {
            be.setPreciseMode(false);
            return true;
        }
        if (id == BTN_MODE_PRECISE) {
            be.setPreciseMode(true);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();
            int machineEnd = AkaishiMinerControllerBlockEntity.OUTPUT_SLOTS;
            if (index < machineEnd) {
                if (!this.moveItemStackTo(current, machineEnd, machineEnd + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, machineEnd, false)) {
                    return ItemStack.EMPTY;
                }
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

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
