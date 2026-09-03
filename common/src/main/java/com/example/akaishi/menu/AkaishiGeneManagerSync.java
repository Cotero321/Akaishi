package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.body.PlayerBodySync;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 基因管理器操作网络包（C2S）：负载 = 动作字节 + 来源 id。
 * - 卸载基因（ACTION_UNLOAD）：移除该来源基因强化；若该基因正被突破激活则突破一并结束。
 * - 结束突破（ACTION_END_BT）：仅结束当前激活的突破强化（基因保留，可再次激活）。
 * 服务端校验操作者本人正打开基因管理器后执行，并立即回推躯体状态刷新界面。
 */
public final class AkaishiGeneManagerSync {

    public static final ResourceLocation CHANNEL = new ResourceLocation(AkaishiMod.MOD_ID, "gene_manager_unload");

    /** 动作：卸载一种基因型 */
    public static final byte ACTION_UNLOAD = 1;
    /** 动作：提前结束当前突破强化 */
    public static final byte ACTION_END_BT = 2;

    private AkaishiGeneManagerSync() {
    }

    /** 服务端注册接收器（AkaishiMod.init 调用） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CHANNEL, (buf, context) -> {
            byte action = buf.readByte();
            // 来源 id 限制 256 字符，防恶意大包占内存（生物注册名远短于此）
            String entityId = buf.readUtf(256);
            // 调度到服务端主线程，避免与身体状态并发读写
            context.queue(() -> {
                // 仅允许操作者本人、且正打开基因管理器时操作（只影响自己，防止误触）
                if (!(context.getPlayer() instanceof ServerPlayer sp)
                        || !(sp.containerMenu instanceof AkaishiGeneManagerMenu)) {
                    return;
                }
                IPlayerBodyState state = PlayerBodyHelper.of(sp);
                if (state == null) {
                    return;
                }
                if (action == ACTION_UNLOAD) {
                    // 卸载的基因正被突破激活 → 突破一并结束（removeGene 内联动清除）
                    boolean wasBtOnThis = state.isBreakthroughActive(entityId);
                    if (state.removeGene(entityId)) {
                        if (wasBtOnThis) {
                            sp.displayClientMessage(
                                    Component.translatable("message.akaishi.gene.bt_interrupted"), true);
                        }
                        // 卸载成功：回推最新躯体状态刷新界面
                        PlayerBodySync.sendToPlayer(sp, state);
                    }
                } else if (action == ACTION_END_BT) {
                    // 提前结束突破：仅结束激活、保留基因（卸载/到期语义之外的主动终止）
                    if (state.hasActiveBreakthrough() && state.endBreakthrough()) {
                        sp.displayClientMessage(
                                Component.translatable("message.akaishi.gene.bt_ended"), true);
                        PlayerBodySync.sendToPlayer(sp, state);
                    }
                }
            });
        });
    }

    /** 客户端：发送卸载请求（entityId = 生物来源 id） */
    public static void sendUnload(String entityId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(ACTION_UNLOAD);
        buf.writeUtf(entityId);
        NetworkManager.sendToServer(CHANNEL, buf);
    }

    /** 客户端：发送提前结束突破请求 */
    public static void sendEndBreakthrough() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(ACTION_END_BT);
        buf.writeUtf("");
        NetworkManager.sendToServer(CHANNEL, buf);
    }
}
