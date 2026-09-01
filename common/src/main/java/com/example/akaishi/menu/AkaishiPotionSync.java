package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiPotionTableBlockEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 药剂台模板选择网络包（C2S）：
 * 客户端点击「永久/突破」模板按钮时发送，服务端写入当前制作类型。
 */
public final class AkaishiPotionSync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "potion_table_select");

    private AkaishiPotionSync() {
    }

    /** 服务端注册接收器（AkaishiMod.init 调用，客户端注册无害） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> {
            BlockPos pos = buf.readBlockPos();
            int templateIndex = buf.readInt();
            context.queue(() -> {
                Player player = context.getPlayer();
                if (player != null && player.level().getBlockEntity(pos) instanceof AkaishiPotionTableBlockEntity be) {
                    be.selectTemplate(player, templateIndex);
                }
            });
        });
    }

    /** 客户端：发送模板选择（PotionRegistry.all() 索引） */
    public static void sendSelect(BlockPos pos, int templateIndex) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeInt(templateIndex);
        NetworkManager.sendToServer(CHANNEL, buf);
    }
}
