package com.example.akaishi.menu;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.wireless.TerminalSecurity;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 终端安全页网络包。
 * <p>
 * <b>S2C 快照</b>：权限表（身份 / 显示名 / 权限位）+ 归属者名 + 默认条目，仅供界面渲染；
 * 服务端每 tick 比对 {@link TerminalSecurity#revision()}，变化才推，避免空包。
 * <p>
 * <b>C2S 动作</b>：登记 / 移除 / 勾选权限。勾选按<b>目标身份 UUID</b>下发而非行号 ——
 * 行号会随权限表增删而错位，用 UUID 才能保证"点谁改谁"（服务端仍校验 SECURITY 权限）。
 */
public final class AkaishiTerminalSecuritySync {

    /** S2C：权限表快照 */
    public static final ResourceLocation SNAPSHOT_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "terminal_security");
    /** C2S：安全页动作 */
    public static final ResourceLocation ACTION_CHANNEL =
            new ResourceLocation(AkaishiMod.MOD_ID, "terminal_security_action");

    /** 归属者名 / 身份显示名长度上限（收发同口径；原版 readUtf/writeUtf 默认放到 32767，太宽） */
    private static final int MAX_NAME = 64;

    /** 登记：把安全页卡槽里的身份卡写入权限表（卡未绑定身份 ⇒ 写默认条目） */
    public static final byte ACTION_REGISTER = 0;
    /** 移除：按卡槽里那张卡的身份（或未绑定 ⇒ 默认条目）移除登记 */
    public static final byte ACTION_REMOVE = 1;
    /** 勾选：切换某身份（null = 默认条目）的单个权限位 */
    public static final byte ACTION_TOGGLE = 2;

    /** 客户端只读条目：身份 + 显示名 + 权限位 */
    public record Entry(UUID player, String name, int perms) {
    }

    /**
     * 安全页网络接收方：三个终端菜单各自实现（赤能源无线 / 生命无线 / 物品终端）。
     * <p>
     * 收发两端都只认这个接口 + 容器 ID —— 早期版本写死 {@code instanceof AkaishiWirelessTerminalMenu}，
     * 导致生命无线终端（另一个菜单类）收不到快照、点了也没反应。
     */
    public interface Target {
        /** S2C：接收权限表快照（渲染只读） */
        void acceptSecurity(String ownerName, boolean hasDefault, int defaultPerms, List<Entry> entries);

        /** C2S：执行一次安全页动作（登记 / 移除 / 勾选） */
        void applySecurityAction(Player actor, byte action, UUID target, int permOrdinal);
    }

    private AkaishiTerminalSecuritySync() {
    }

    // ===== C2S：安全页动作 =====

    /** 服务端注册接收器（AkaishiMod.init 调用） */
    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ACTION_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            byte action = buf.readByte();
            UUID target = buf.readBoolean() ? buf.readUUID() : null;
            byte permOrdinal = buf.readByte();
            context.queue(() -> {
                var player = context.getPlayer();
                if (player == null) {
                    return;
                }
                // 校验：必须是打开中的终端菜单（实现 Target）且容器 ID 一致，拦截伪造包
                if (player.containerMenu instanceof Target actionTarget
                        && player.containerMenu.containerId == containerId) {
                    actionTarget.applySecurityAction(player, action, target, permOrdinal);
                }
            });
        });
    }

    /** 客户端：发送一次安全页动作 */
    public static void sendAction(int containerId, byte action, UUID target, int permOrdinal) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeByte(action);
        buf.writeBoolean(target != null);
        if (target != null) {
            buf.writeUUID(target);
        }
        buf.writeByte(permOrdinal);
        NetworkManager.sendToServer(ACTION_CHANNEL, buf);
    }

    // ===== S2C：权限表快照 =====

    /** 客户端注册接收器（AkaishiMod.init 的客户端分支调用） */
    public static void registerClient() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SNAPSHOT_CHANNEL, (buf, context) -> {
            int containerId = buf.readInt();
            String ownerName = buf.readUtf(MAX_NAME);
            boolean hasDefault = buf.readBoolean();
            int defaultPerms = buf.readVarInt();
            int size = buf.readVarInt();
            // 条数钳到服务端口径上限：解码端不信任对端给的 size（畸形包会让客户端预分配爆内存）
            int count = Math.min(size, TerminalSecurity.MAX_ENTRIES);
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                UUID player = buf.readUUID();
                String name = buf.readUtf(MAX_NAME);
                int perms = buf.readVarInt();
                entries.add(new Entry(player, name, perms));
            }
            // 网络线程只解码，落地回客户端主线程，避免与渲染线程并发读写
            Minecraft.getInstance().execute(() -> {
                var player = Minecraft.getInstance().player;
                if (player != null && player.containerMenu instanceof Target snapshotTarget
                        && player.containerMenu.containerId == containerId) {
                    snapshotTarget.acceptSecurity(ownerName, hasDefault, defaultPerms, entries);
                }
            });
        });
    }

    /** 服务端：推送一次权限表快照（权限表变化时调用） */
    public static void sendSnapshot(ServerPlayer player, int containerId, TerminalSecurity security) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(containerId);
        buf.writeUtf(security.ownerName(), MAX_NAME);
        buf.writeBoolean(security.hasDefaultEntry());
        buf.writeVarInt(security.defaultPerms());
        buf.writeVarInt(security.entryCount());
        for (Map.Entry<UUID, Integer> entry : security.entries().entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(security.displayName(entry.getKey()), MAX_NAME);
            buf.writeVarInt(entry.getValue());
        }
        NetworkManager.sendToPlayer(player, SNAPSHOT_CHANNEL, buf);
    }
}
