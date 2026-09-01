package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiTransformerBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 赤石变化器菜单（单输入单输出通用布局，见 {@link AkaishiSingleSlotMachineMenu}）。
 */
public class AkaishiTransformerMenu extends AkaishiSingleSlotMachineMenu {

    public AkaishiTransformerMenu(int id, Inventory inv, AkaishiTransformerBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiTransformerMenu(int id, Inventory inv, Container inventory, ContainerData data) {
        this(id, inv, inventory, data, new MachineUpgradeSlots());
    }

    AkaishiTransformerMenu(int id, Inventory inv, Container inventory, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_TRANSFORMER.get(), id, inventory, data, upgrades, inv);
    }
}
