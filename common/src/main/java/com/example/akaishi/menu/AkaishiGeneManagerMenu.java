package com.example.akaishi.menu;

import com.example.akaishi.block.entity.AkaishiGeneManagerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 基因管理器菜单：纯展示面板，无任何容器槽位。
 * 玩家躯体状态经 S2C 同步包推送到客户端缓存，由 Screen 直接渲染。
 * 持有方块坐标，stillValid 校验方块仍存在且在 8 格交互范围内（服务端逐 tick 校验）。
 */
public class AkaishiGeneManagerMenu extends AbstractContainerMenu {

    /** 菜单绑定方块坐标（客户端从开包 extraData 读取；服务端由方块实体传入） */
    private final BlockPos pos;

    /** 客户端反序列化用（Buffer 中读取坐标） */
    public AkaishiGeneManagerMenu(int id, Inventory playerInv, BlockPos pos) {
        super(ModMenus.CHISHI_GENE_MANAGER.get(), id);
        this.pos = pos;
    }

    @Override
    public boolean stillValid(Player player) {
        // 方块已拆除：失效自动关界面；超出 8 格交互范围同样失效
        if (!(player.level().getBlockEntity(pos) instanceof AkaishiGeneManagerBlockEntity)) {
            return false;
        }
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
