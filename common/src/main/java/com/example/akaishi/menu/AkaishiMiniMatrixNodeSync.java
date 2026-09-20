package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiMiniMatrixNetworkNodeBlockEntity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.architectury.networking.NetworkManager;

import io.netty.buffer.Unpooled;

import java.util.UUID;

/**
 * 网络节点界面 → 服务端的开关包（C2S 单向，「节点屏障」开/关）。
 * <p>
 * <b>为什么不用 S2C</b>：节点方块实体自带同步标签，服务端改完 {@code setBarrierEnabled} 内部就会
 * {@code sendBlockUpdated} 把新的开关值推给客户端（渲染端与界面共用同一份数据），无需另做快照包。
 * <p>
 * <b>权限口径</b>：开关只影响这一个节点的屏障显示，但仍可能被用来"关掉别人的场域可视化"，
 * 故只放行三种人 —— op4、未申领节点、申领方本人；其余给一条 actionbar 回执（不是静默失败）。
 */
public final class AkaishiMiniMatrixNodeSync {

    public static final ResourceLocation TOGGLE_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "mini_matrix_node_toggle");

    private AkaishiMiniMatrixNodeSync() {
    }

    /** 服务端注册接收器（AkaishiMod.init 调用） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TOGGLE_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            boolean enabled = buf.readBoolean();
            context.queue(() -> {
                var player = context.getPlayer();
                if (player == null) {
                    return;
                }
                // 校验：必须是打开中的节点菜单且容器 ID 一致，拦截伪造包
                if (player.containerMenu instanceof AkaishiMiniMatrixNodeMenu menu
                        && menu.containerId == containerId) {
                    AkaishiMiniMatrixNetworkNodeBlockEntity node = menu.node();
                    if (node == null || node.isRemoved()) {
                        return;
                    }
                    UUID claimant = node.claimantId();
                    if (!player.hasPermissions(4) && claimant != null && !claimant.equals(player.getUUID())) {
                        player.displayClientMessage(
                                Component.translatable("message.akaishi.matrix.node.denied"), true);
                        return;
                    }
                    node.setBarrierEnabled(enabled);
                }
            });
        });
    }

    /** 客户端：发送一次开关（传"目标状态"） */
    public static void sendToggle(int containerId, boolean enabled) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeBoolean(enabled);
        NetworkManager.sendToServer(TOGGLE_CHANNEL, buf);
    }
}
