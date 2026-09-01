package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiPulverizerBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 赤石打粉机菜单（单输入单输出通用布局，见 {@link AkaishiSingleSlotMachineMenu}）。
 */
public class AkaishiPulverizerMenu extends AkaishiSingleSlotMachineMenu {

    public AkaishiPulverizerMenu(int id, Inventory inv, AkaishiPulverizerBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiPulverizerMenu(int id, Inventory inv, Container inventory, ContainerData data) {
        this(id, inv, inventory, data, new MachineUpgradeSlots());
    }

    AkaishiPulverizerMenu(int id, Inventory inv, Container inventory, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_PULVERIZER.get(), id, inventory, data, upgrades, inv);
    }
}
