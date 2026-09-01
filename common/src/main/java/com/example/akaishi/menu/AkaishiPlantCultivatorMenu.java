package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiPlantCultivatorBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 赤石植物培养机菜单（单输入单输出通用布局，见 {@link AkaishiSingleSlotMachineMenu}）。
 */
public class AkaishiPlantCultivatorMenu extends AkaishiSingleSlotMachineMenu {

    public AkaishiPlantCultivatorMenu(int id, Inventory inv, AkaishiPlantCultivatorBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiPlantCultivatorMenu(int id, Inventory inv, Container inventory, ContainerData data) {
        this(id, inv, inventory, data, new MachineUpgradeSlots());
    }

    AkaishiPlantCultivatorMenu(int id, Inventory inv, Container inventory, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_PLANT_CULTIVATOR.get(), id, inventory, data, upgrades, inv);
    }
}
