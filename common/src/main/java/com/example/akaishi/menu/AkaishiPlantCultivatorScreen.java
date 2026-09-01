package com.example.akaishi.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤石植物培养机界面（通用单输入单输出自绘布局，见 {@link AkaishiSingleSlotMachineScreen}）。
 */
public class AkaishiPlantCultivatorScreen extends AkaishiSingleSlotMachineScreen<AkaishiPlantCultivatorMenu> {

    public AkaishiPlantCultivatorScreen(AkaishiPlantCultivatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}
