package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiCompressorBlockEntity;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 赤石压缩机菜单（单输入单输出通用布局，见 {@link AkaishiSingleSlotMachineMenu}）。
 */
public class AkaishiCompressorMenu extends AkaishiSingleSlotMachineMenu {

    public AkaishiCompressorMenu(int id, Inventory inv, AkaishiCompressorBlockEntity be) {
        this(id, inv, be.inventory(), be.data(), be.getUpgradeSlots());
    }

    public AkaishiCompressorMenu(int id, Inventory inv, Container inventory, ContainerData data) {
        this(id, inv, inventory, data, new MachineUpgradeSlots());
    }

    AkaishiCompressorMenu(int id, Inventory inv, Container inventory, ContainerData data, Container upgrades) {
        super(ModMenus.CHISHI_COMPRESSOR.get(), id, inventory, data, upgrades, inv);
    }
}
