package com.example.akaishi.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 躯体检查仪菜单：纯展示面板，无任何容器槽位（不需要在界面内操作物品）。
 * 玩家躯体状态经 S2C 同步包推送到客户端缓存，由 Screen 直接渲染。
 */
public class AkaishiBodyScannerMenu extends AbstractContainerMenu {

    public AkaishiBodyScannerMenu(int id, Inventory playerInv) {
        super(ModMenus.CHISHI_BODY_SCANNER.get(), id);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
