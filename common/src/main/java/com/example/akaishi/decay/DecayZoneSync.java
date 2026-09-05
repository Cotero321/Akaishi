package com.example.akaishi.decay;

import com.example.akaishi.AkaishiMod;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 衰竭区域污染强度同步（S2C）：伪群系氛围的数据通道。
 * <p>
 * 服务端周期性（每 20 tick）计算每个在线玩家所处位置的污染强度（0~1），
 * 离散为 16 级发送；客户端接收后写入 {@link DecayClientAmbience} 供雾渲染平滑过渡。
 * 玩家不在任何区域时服务端发送 0 级，使离开区域后氛围自动消退。
 */
public final class DecayZoneSync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "decay_zone_state");
    /** 强度同步周期（tick）：1 秒一跳，兼顾平滑与包开销 */
    private static final int SYNC_INTERVAL = 20;
    /** 强度离散层级数（0~16，0=不在区域） */
    private static final int LEVEL_STEPS = 16;

    private DecayZoneSync() {
    }

    /** 服务端每个维度每 tick 调用（由 AkaishiMod.init 的 SERVER_LEVEL_POST 驱动） */
    public static void serverTick(ServerLevel level) {
        if (level.getGameTime() % SYNC_INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            float intensity = DecayZoneManager.intensityAt(level, player.blockPosition());
            buf.writeByte((int) Math.min(LEVEL_STEPS, Math.round(intensity * LEVEL_STEPS)));
            NetworkManager.sendToPlayer(player, CHANNEL, buf);
        }
    }

    /** 客户端注册接收器（AkaishiMod.init 的 Env.CLIENT 分支调用，仅客户端执行） */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, CHANNEL, (buf, context) -> {
            // 网络线程读取字节后调度到主线程更新状态（无缓冲区引用遗留）
            int level = buf.readUnsignedByte();
            Minecraft.getInstance().execute(() -> DecayClientAmbience.setTarget(level / (float) LEVEL_STEPS));
        });
    }
}
