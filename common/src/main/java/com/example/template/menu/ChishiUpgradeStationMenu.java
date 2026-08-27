package com.example.template.menu;

import com.example.template.block.entity.ChishiUpgradeStationBlockEntity;
import com.example.template.item.ChishiUpgradeHelper;
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
 * 赤红升级台菜单：输入（赤石装备）+ 输入（升级模板）+ 输出（升级后装备）。
 * 通过 clickMenuButton 处理客户端按钮：0-5 选择升级类型，100 执行升级（服务端生效）。
 */
public class ChishiUpgradeStationMenu extends AbstractContainerMenu {

    /** 执行升级的按钮 id */
    public static final int BUTTON_EXECUTE = 100;

    private final Container container;
    private final ContainerData data;
    private final ChishiUpgradeStationBlockEntity be;

    public ChishiUpgradeStationMenu(int id, Inventory inv, ChishiUpgradeStationBlockEntity be) {
        super(ModMenus.CHISHI_UPGRADE_STATION.get(), id);
        this.container = be != null ? be.inventory() : new SimpleContainer(ChishiUpgradeStationBlockEntity.SLOT_COUNT);
        this.data = be != null ? be.data() : new SimpleContainerData(5);
        this.be = be;

        // 方块槽：装备 / 模板输入左侧，输出右侧
        addSlot(new Slot(container, ChishiUpgradeStationBlockEntity.INPUT_GEAR_SLOT, 44, 30));
        addSlot(new Slot(container, ChishiUpgradeStationBlockEntity.INPUT_TEMPLATE_SLOT, 62, 30));
        addSlot(new Slot(container, ChishiUpgradeStationBlockEntity.OUTPUT_SLOT, 116, 30));

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

    public int getEnergy() {
        return data.get(0);
    }

    public int getMaxEnergy() {
        return data.get(1);
    }

    /** 当前选择的升级类型序号 */
    public int getSelectedType() {
        return data.get(2);
    }

    /** 装备剩余升级槽位 */
    public int getSlots() {
        return data.get(3);
    }

    /** 输入槽是否放入了赤石装备 */
    public boolean hasGear() {
        return data.get(4) == 1;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null) {
            return false;
        }
        if (id >= 0 && id < ChishiUpgradeHelper.SpecialAbility.values().length) {
            // 选择特殊能力
            be.setSelectedType(id);
            return true;
        }
        if (id == BUTTON_EXECUTE) {
            // 执行升级（服务端校验并消耗）
            be.tryUpgrade();
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
            if (index < ChishiUpgradeStationBlockEntity.SLOT_COUNT) {
                if (!this.moveItemStackTo(current, ChishiUpgradeStationBlockEntity.SLOT_COUNT,
                        ChishiUpgradeStationBlockEntity.SLOT_COUNT + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(current, 0, ChishiUpgradeStationBlockEntity.SLOT_COUNT, false)) {
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
