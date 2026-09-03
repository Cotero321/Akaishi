package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiLifeStructBlockEntity;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 生命结构台目标槽位选择网络包（C2S）：
 * 客户端界面点击 9 个槽位按钮时发送，服务端写入方块实体的目标槽位。
 * 仅使用 Architectury Networking，common 无平台差异。
 */
public final class AkaishiLifeStructSync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "life_struct_select");

    private AkaishiLifeStructSync() {
    }

    /** 服务端注册接收器（AkaishiMod.init 调用，客户端注册无害） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> {
            BlockPos pos = buf.readBlockPos();
            int slotIndex = buf.readInt();
            // 调度到服务端主线程，避免并发访问方块实体
            context.queue(() -> {
                Player player = context.getPlayer();
                // 必须打开对应机器菜单且坐标一致（参照手术台惯例），拦截非本机/远距离篡改包
                if (player != null
                        && player.containerMenu instanceof AkaishiLifeStructMenu menu
                        && pos.equals(menu.getBlockPos())
                        && player.level().getBlockEntity(pos) instanceof AkaishiLifeStructBlockEntity be) {
                    be.setTargetSlot(slotIndex);
                }
            });
        });
    }

    /** 客户端：发送目标槽位选择（BodySlot.values() 索引） */
    public static void sendSelect(BlockPos pos, int slotIndex) {
        // 当前 Architectury 版本 sendToServer 签名为 (channel, FriendlyByteBuf)
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeInt(slotIndex);
        NetworkManager.sendToServer(CHANNEL, buf);
    }
}
