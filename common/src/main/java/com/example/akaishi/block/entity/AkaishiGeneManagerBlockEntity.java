package com.example.akaishi.block.entity;

import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.body.PlayerBodySync;
import com.example.akaishi.menu.AkaishiGeneManagerMenu;
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
 * 基因管理器方块实体：纯管理面板，无容器、无 tick。
 * 右键打开界面时，将使用者本人的基因强化列表（来源 → 适配加成）经网络推送到客户端；
 * 卸载操作经 C2S（GeneManagerSync）处理。
 */
public class AkaishiGeneManagerBlockEntity extends BlockEntity implements ExtendedMenuProvider {

    public AkaishiGeneManagerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_GENE_MANAGER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_gene_manager");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // 服务端打开界面时，向该玩家推送其躯体状态（含基因强化）
        if (player instanceof ServerPlayer sp) {
            IPlayerBodyState state = PlayerBodyHelper.of(sp);
            if (state != null) {
                PlayerBodySync.sendToPlayer(sp, state);
            }
        }
        return new AkaishiGeneManagerMenu(id, inv, worldPosition);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
