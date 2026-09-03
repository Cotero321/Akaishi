package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiTraitReforgerBlockEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 词条重铸仪目标词条选择网络包（C2S）：
 * 客户端点击词条序号按钮时发送，服务端写入当前重铸目标（仅打开该菜单的玩家可改）。
 */
public final class AkaishiTraitReforgerSync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "trait_reforger_select");

    private AkaishiTraitReforgerSync() {
    }

    /** 服务端注册接收器（AkaishiMod.init 调用，客户端注册无害） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> {
            BlockPos pos = buf.readBlockPos();
            int targetIndex = buf.readInt();
            context.queue(() -> {
                Player player = context.getPlayer();
                // 仅允许已打开该菜单的玩家修改目标，防远程篡改
                if (player != null && player.containerMenu instanceof AkaishiTraitReforgerMenu
                        && player.level().getBlockEntity(pos) instanceof AkaishiTraitReforgerBlockEntity be) {
                    be.selectTarget(targetIndex);
                }
            });
        });
    }

    /** 客户端：发送目标词条序号选择 */
    public static void sendTarget(BlockPos pos, int targetIndex) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeInt(targetIndex);
        NetworkManager.sendToServer(CHANNEL, buf);
    }
}
