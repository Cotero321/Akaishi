package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 储存无线输入/输出口「远程绑定」网络包（输入口/输出口共用）。
 * <p>
 * <b>S2C 快照</b>：可绑定物品终端清单（终端 ID + 维度 + 坐标 + 归属者名 + <b>显示名</b> + <b>短号</b>）
 * + 当前已绑定目标的结构化描述（{@link BoundTarget}：名称 / 短号 / 位置 / 是否仍存活）。
 * 服务端每 tick 比对内容变化才推，避免空包。
 * <p>
 * <b>C2S 动作</b>：绑定 / 解绑。绑定按<b>终端 ID</b>下发（清单会随终端上下线变动，不用行号，
 * 也不能用坐标：终端搬动 / 被微缩后坐标就变了）；服务端按终端本地权威安全表复核「布局(BUILD)」权限。
 * <p>
 * <b>名称为何走 JSON</b>：名称是 {@code Component}（可翻译），若在服务端就 {@code getString()}
 * 会按<b>服务端语言</b>固定成字符串，客户端切到中文也还是英文；故按原版
 * {@code Component.Serializer.toJson/fromJson} 原样过网，由客户端本地化渲染。
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

    /** 客户端只读条目：终端 ID + 维度 + 坐标（后两者仅供显示/悬停）+ 归属者名 + 显示名 + 短号 */
    public record Entry(UUID terminalId, ResourceLocation dimension, BlockPos pos, String ownerName,
            Component name, String shortId) {
    }

    /**
     * 已绑定目标（客户端只读）：名称 + 短号 + 位置文本 + 是否仍存活。
     * <p>
     * <b>为什么要有"存活"标志</b>：绑定关系是 NBT 持久化的，终端被拆/坍缩后再拆芯片时绑定仍在，
     * 界面若只显示缓存坐标，就会把"已经不存在的终端"画成一条正常绑定（幽灵行）。
     * 现在由服务端每 tick 判定并把结论下发，客户端照实渲染「已绑定（目标已失效）」。
     *
     * @param posText 「维度 x,y,z」文本；空串 = 位置未知
     */
    public record BoundTarget(UUID terminalId, Component name, String shortId, String posText, boolean live) {
    }

    /** 收发方：口菜单实现（勿按具体菜单类判类型，见安全页那次的教训） */
    public interface Target {
        /** S2C：接收清单快照 + 已绑定目标（渲染只读；未绑定传 null） */
        void acceptBinding(List<Entry> entries, @Nullable BoundTarget boundTarget);

        /** C2S：执行一次绑定（terminalId 非空）/ 解绑（terminalId 传 null） */
        void applyBindingAction(Player actor, byte action, UUID terminalId);
    }

    private AkaishiItemPortBindingSync() {
    }

    /** 位置文本（悬停提示用）：「维度 path x,y,z」 */
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
            boolean bound = buf.readBoolean();
            BoundTarget boundTarget = null;
            if (bound) {
                UUID terminalId = buf.readUUID();
                Component name = readComponent(buf.readUtf());
                String shortId = buf.readUtf();
                String posText = buf.readUtf();
                boolean live = buf.readBoolean();
                boundTarget = new BoundTarget(terminalId, name, shortId, posText, live);
            }
            int size = buf.readVarInt();
            List<Entry> entries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                // 维度按 ResourceLocation 字符串收发；解析失败的脏条目直接丢弃，不牵连整包
                UUID terminalId = buf.readUUID();
                ResourceLocation dimension = ResourceLocation.tryParse(buf.readUtf());
                BlockPos pos = buf.readBlockPos();
                String ownerName = buf.readUtf();
                Component name = readComponent(buf.readUtf());
                String shortId = buf.readUtf();
                if (dimension != null) {
                    entries.add(new Entry(terminalId, dimension, pos, ownerName, name, shortId));
                }
            }
            BoundTarget decodedTarget = boundTarget;
            // 网络线程只解码，落地回客户端主线程，避免与渲染线程并发读写
            Minecraft.getInstance().execute(() -> {
                Player player = Minecraft.getInstance().player;
                if (player != null && player.containerMenu instanceof Target target
                        && player.containerMenu.containerId == containerId) {
                    target.acceptBinding(entries, decodedTarget);
                }
            });
        });
    }

    public static void sendSnapshot(ServerPlayer player, int containerId, List<Entry> entries,
            @Nullable BoundTarget boundTarget) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeBoolean(boundTarget != null);
        if (boundTarget != null) {
            buf.writeUUID(boundTarget.terminalId());
            buf.writeUtf(toJson(boundTarget.name()));
            buf.writeUtf(boundTarget.shortId());
            buf.writeUtf(boundTarget.posText());
            buf.writeBoolean(boundTarget.live());
        }
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUUID(entry.terminalId());
            buf.writeUtf(entry.dimension().toString());
            buf.writeBlockPos(entry.pos());
            buf.writeUtf(entry.ownerName());
            buf.writeUtf(toJson(entry.name()));
            buf.writeUtf(entry.shortId());
        }
        NetworkManager.sendToPlayer(player, SNAPSHOT_CHANNEL, buf);
    }

    /** 名称组件 → JSON（空名写成空串，客户端按"未知终端"兜底） */
    private static String toJson(@Nullable Component name) {
        return name == null || name.getString().isEmpty() ? "" : Component.Serializer.toJson(name);
    }

    /** JSON → 名称组件；脏数据不牵连整包（退回空组件，客户端显示"未知终端"） */
    private static Component readComponent(String json) {
        if (json == null || json.isEmpty()) {
            return Component.empty();
        }
        try {
            return Component.Serializer.fromJson(json);
        } catch (RuntimeException e) {
            return Component.empty();
        }
    }
}
