package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 储存无线输入/输出口「远程绑定」网络包（输入口/输出口共用）。
 * <p>
 * <b>S2C 快照</b>：可绑定物品终端清单（终端 ID + 维度 + 坐标 + 归属者名）+ 当前已绑定终端的标签（行高亮用）。
 * 服务端每 tick 比对内容变化才推，避免空包。
 * <p>
 * <b>C2S 动作</b>：绑定 / 解绑。绑定按<b>终端 ID</b>下发（清单会随终端上下线变动，不用行号，
 * 也不能用坐标：终端搬动 / 被微缩后坐标就变了）；服务端按终端本地权威安全表复核「布局(BUILD)」权限。
 * <p>
 * 过滤网不走本类：9 格已是真槽位，内容由原版 menu 槽同步下发（无自定义包）。
 */
public final class AkaishiItemPortBindingSync {

    /** S2C：可绑定终端清单快照 */
    public static final ResourceLocation SNAPSHOT_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "item_port_binding");
    /** C2S：绑定 / 解绑动作 */
    public static final ResourceLocation ACTION_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "item_port_binding_action");

    /** 绑定到指定终端 */
    public static final byte ACTION_BIND = 0;
    /** 解绑当前终端 */
    public static final byte ACTION_UNBIND = 1;

    /** 客户端只读条目：终端 ID + 维度 + 坐标（后两者仅供显示）+ 归属者名 */
    public record Entry(UUID terminalId, ResourceLocation dimension, BlockPos pos, String ownerName) {
    }

    /** 收发方：口菜单实现（勿按具体菜单类判类型，见安全页那次的教训） */
    public interface Target {
        /** S2C：接收清单快照（渲染只读） */
        void acceptBinding(List<Entry> entries, String boundLabel);

        /** C2S：执行一次绑定（terminalId 非空）/ 解绑（terminalId 传 null） */
        void applyBindingAction(Player actor, byte action, UUID terminalId);
    }

    private AkaishiItemPortBindingSync() {
    }

    /** 行/绑定标签（与 {@code AkaishiItemPortBlockEntity#boundTargetLabel()} 同格式）：「维度 path x,y,z」 */
    public static String label(ResourceLocation dimension, BlockPos pos) {
        return dimension.getPath() + ' ' + pos.getX() + ',' + pos.getY() + ',' + pos.getZ();
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

    /** 客户端发起绑定（terminalId 非空）或解绑（传 null） */
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
            String boundLabel = buf.readUtf();
            int size = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                // 维度按 ResourceLocation 字符串收发；解析失败的脏条目直接丢弃，不牵连整包
                UUID terminalId = buf.readUUID();
                ResourceLocation dimension = ResourceLocation.tryParse(buf.readUtf());
                BlockPos pos = buf.readBlockPos();
                String ownerName = buf.readUtf();
                if (dimension != null) {
                    entries.add(new Entry(terminalId, dimension, pos, ownerName));
                }
            }
            // 网络线程只解码，落地回客户端主线程，避免与渲染线程并发读写
            Minecraft.getInstance().execute(() -> {
                Player player = Minecraft.getInstance().player;
                if (player != null && player.containerMenu instanceof Target target
                        && player.containerMenu.containerId == containerId) {
                    target.acceptBinding(entries, boundLabel);
                }
            });
        });
    }

    public static void sendSnapshot(ServerPlayer player, int containerId, List<Entry> entries, String boundLabel) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeUtf(boundLabel == null ? "" : boundLabel);
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUUID(entry.terminalId());
            buf.writeUtf(entry.dimension().toString());
            buf.writeBlockPos(entry.pos());
            buf.writeUtf(entry.ownerName());
        }
        NetworkManager.sendToPlayer(player, SNAPSHOT_CHANNEL, buf);
    }
}
