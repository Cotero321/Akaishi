package com.example.akaishi.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * 赤石压缩机界面（通用单输入单输出自绘布局，见 {@link AkaishiSingleSlotMachineScreen}）。
 */
public class AkaishiCompressorScreen extends AkaishiSingleSlotMachineScreen<AkaishiCompressorMenu> {

    public AkaishiCompressorScreen(AkaishiCompressorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }
}
