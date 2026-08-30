package com.example.template.menu;

import com.example.template.TemplateMod;
import com.example.template.block.entity.ChishiSurgeryBlockEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 手术仓手术开始网络包（C2S）：
 * 客户端点击「移植/摘除」按钮时发送，服务端校验后开始手术进度。
 * 仅使用 Architectury Networking，common 无平台差异。
 */
public final class ChishiSurgerySync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(TemplateMod.MOD_ID, "surgery_start");

    private ChishiSurgerySync() {
    }

    /** 服务端注册接收器（TemplateMod.init 调用，客户端注册无害） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> {
            BlockPos pos = buf.readBlockPos();
            int type = buf.readInt();
            int slotIndex = buf.readInt();
            // 调度到服务端主线程，避免并发访问方块实体
            context.queue(() -> {
                Player player = context.getPlayer();
                if (player != null && player.level().getBlockEntity(pos) instanceof ChishiSurgeryBlockEntity be) {
                    be.startOperation(player, type, slotIndex);
                }
            });
        });
    }

    /** 客户端：发送手术开始（type=1 移植 / 2 摘除，slotIndex=BodySlot.values() 索引） */
    public static void sendStart(BlockPos pos, int type, int slotIndex) {
        // 当前 Architectury 版本 sendToServer 签名为 (channel, FriendlyByteBuf)
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeInt(type);
        buf.writeInt(slotIndex);
        NetworkManager.sendToServer(CHANNEL, buf);
    }
}
