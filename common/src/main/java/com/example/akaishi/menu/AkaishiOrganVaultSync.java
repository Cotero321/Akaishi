package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * 器官储藏库页选择网络包（C2S）：
 * 页槽通过 VaultSlot 映射到"当前选中页"，若仅客户端切页而服务端不知情，
 * 服务端所有放置/取出都会落到第 0 页，导致存取错乱。
 * 本包把选中页同步给服务端菜单，保证两侧页状态一致。
 */
public final class AkaishiOrganVaultSync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "organ_vault_page");

    private AkaishiOrganVaultSync() {
    }

    /** 服务端注册接收器（AkaishiMod.init 调用，客户端注册无害） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            int page = buf.readInt();
            context.queue(() -> {
                var player = context.getPlayer();
                // 校验：必须是打开中的器官储藏库菜单且容器 ID 一致，拦截伪造包
                if (player != null
                        && player.containerMenu instanceof AkaishiOrganVaultMenu menu
                        && menu.containerId == containerId) {
                    menu.setCurrentPage(page);
                    // 立即把新页槽内容推给客户端（服务端槽位映射已切换）
                    menu.broadcastChanges();
                }
            });
        });
    }

    /** 客户端：发送页选择（0 ~ PAGE_COUNT-1） */
    public static void sendPageSelect(int containerId, int page) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeInt(page);
        NetworkManager.sendToServer(CHANNEL, buf);
    }
}
