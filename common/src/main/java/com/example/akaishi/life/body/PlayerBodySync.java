package com.example.akaishi.life.body;

import com.example.akaishi.AkaishiMod;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家躯体状态网络同步（S2C）：
 * 服务端打开躯体检查仪时，将玩家 9 槽位器官 + 排斥值打包推送到客户端缓存。
 * 仅使用 Architectury Networking，common 无平台差异。
 */
public final class PlayerBodySync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "player_body_state");

    private PlayerBodySync() {
    }

    /** 客户端注册接收器（AkaishiMod.init 的 Env.CLIENT 分支调用） */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CHANNEL, (buf, context) -> {
            // 网络线程接收，保留缓冲区并调度到客户端主线程应用（避免与界面渲染并发）
            buf.retain();
            Minecraft.getInstance().execute(() -> {
                try {
                    ClientBodyData.apply(buf);
                } finally {
                    buf.release();
                }
            });
        });
    }

    /** 服务端：向指定玩家发送其躯体状态（打开检查仪时调用） */
    public static void sendToPlayer(ServerPlayer player, IPlayerBodyState state) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        for (BodySlot slot : BodySlot.values()) {
            buf.writeUtf(slot.getId());
            ItemStack organ = state.getOrgan(slot);
            buf.writeBoolean(!organ.isEmpty());
            if (!organ.isEmpty()) {
                buf.writeItem(organ);
            }
            buf.writeInt(state.getRejection(slot));
        }
        NetworkManager.sendToPlayer(player, CHANNEL, buf);
    }
}
