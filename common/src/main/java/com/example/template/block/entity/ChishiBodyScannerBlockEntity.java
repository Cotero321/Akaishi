package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.life.body.IPlayerBodyState;
import com.example.template.life.body.PlayerBodyHelper;
import com.example.template.life.body.PlayerBodySync;
import com.example.template.menu.ChishiBodyScannerMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 躯体检查仪方块实体：纯展示面板，无容器、无 tick。
 * 右键打开体检界面时，将检查者本人的躯体状态（9 槽位 + 排斥值）经网络推送到客户端，
 * 由界面读取 ClientBodyData 渲染。
 */
public class ChishiBodyScannerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IDataCarrier {

    public ChishiBodyScannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_BODY_SCANNER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_body_scanner");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // 服务端打开界面时，向该玩家推送其躯体状态
        if (player instanceof ServerPlayer sp) {
            IPlayerBodyState state = PlayerBodyHelper.of(sp);
            if (state != null) {
                PlayerBodySync.sendToPlayer(sp, state);
            }
        }
        return new ChishiBodyScannerMenu(id, inv);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
