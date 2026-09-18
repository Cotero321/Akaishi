package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 端口「远程绑定」网络包（输入口/输出口共用）。
 * <p>
 * <b>S2C 快照</b>：可绑定终端清单（终端 ID + 短号 + 归属者名）+ 当前已绑定终端的短号（行高亮用）。
 * 服务端每 tick 比对内容变化才推，避免空包。
 * <p>
 * <b>C2S 动作</b>：绑定 / 解绑。绑定按<b>终端 UUID</b>下发（不用行号 —— 清单会随终端上线/下线变动），
 * 服务端仍校验「布局(BUILD)」权限。
 */
public final class AkaishiPortBindingSync {

    /** S2C：可绑定终端清单快照 */
    public static final ResourceLocation SNAPSHOT_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "port_binding");
    /** C2S：绑定 / 解绑动作 */
    public static final ResourceLocation ACTION_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "port_binding_action");

    /** 绑定到指定终端 */
    public static final byte ACTION_BIND = 0;
    /** 解绑当前终端 */
    public static final byte ACTION_UNBIND = 1;

    /** 客户端只读条目：终端 ID + 短号 + 归属者名 */
    public record Entry(UUID terminalId, String shortId, String ownerName) {
    }

    /** 收发方：两个端口菜单各自实现（勿按具体菜单类判类型，见安全页那次的教训） */
    public interface Target {
        /** S2C：接收清单快照（渲染只读） */
        void acceptBinding(List<Entry> entries, String boundShortId);

        /** C2S：执行一次绑定 / 解绑 */
        void applyBindingAction(Player actor, byte action, UUID terminalId);
    }

    private AkaishiPortBindingSync() {
    }

    // ===== C2S：绑定 / 解绑 =====

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ACTION_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            byte action = buf.readByte();
            UUID terminalId = buf.readBoolean() ? buf.readUUID() : null;
            context.queue(() -> {
                Player player = context.getPlayer();
                if (player != null && player.containerMenu instanceof Target target
                        && player.containerMenu.containerId == containerId) {
                    target.applyBindingAction(player, action, terminalId);
                }
            });
        });
    }

    public static void sendAction(int containerId, byte action, UUID terminalId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeByte(action);
        buf.writeBoolean(terminalId != null);
        if (terminalId != null) {
            buf.writeUUID(terminalId);
        }
        NetworkManager.sendToServer(ACTION_CHANNEL, buf);
    }

    // ===== S2C：清单快照 =====

    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SNAPSHOT_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            String boundShortId = buf.readUtf();
            int size = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                UUID terminalId = buf.readUUID();
                String shortId = buf.readUtf();
                String ownerName = buf.readUtf();
                entries.add(new Entry(terminalId, shortId, ownerName));
            }
            // 网络线程只解码，落地回客户端主线程，避免与渲染线程并发读写
            Minecraft.getInstance().execute(() -> {
                Player player = Minecraft.getInstance().player;
                if (player != null && player.containerMenu instanceof Target target
                        && player.containerMenu.containerId == containerId) {
                    target.acceptBinding(entries, boundShortId);
                }
            });
        });
    }

    public static void sendSnapshot(ServerPlayer player, int containerId, List<Entry> entries, String boundShortId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeUtf(boundShortId == null ? "" : boundShortId);
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUUID(entry.terminalId());
            buf.writeUtf(entry.shortId());
            buf.writeUtf(entry.ownerName());
        }
        NetworkManager.sendToPlayer(player, SNAPSHOT_CHANNEL, buf);
    }
}
