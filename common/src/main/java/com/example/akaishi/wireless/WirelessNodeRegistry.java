package com.example.akaishi.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无线网络节点登记表（服务端静态表）：只记「哪些坐标上有节点方块」，不持有任何逻辑。
 * <p>
 * 登记/注销由<b>节点方块实体</b>的生命周期驱动（{@code setLevel}/{@code setRemoved}）：
 * 区块加载即重新登记、区块卸载即注销 —— 于是服务器重启后世界里的节点会自动回到表里，
 * 不需要玩家拆掉重放；方块自身的 {@code onPlace}/{@code onRemove} 仍同步登记与注销（幂等互补）。
 * <p>
 * 按 {@link MinecraftServer} <b>实例分表</b>：同一 JVM 换世界（单人多次进出存档）时旧实例整组丢弃，
 * 旧坐标不会残留在新世界里；服务器停止时由平台层钩子调用 {@link #clearServer} 显式释放。
 * <p>
 * 只对被申领为「场域节点」的坐标挂弱加载票据（见 {@link WirelessFieldManager}），
 * 未申领的节点方块不产生任何加载开销。
 */
public final class WirelessNodeRegistry {

    private record Key(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private static final Map<MinecraftServer, Set<Key>> NODES = new ConcurrentHashMap<>();

    private WirelessNodeRegistry() {
    }

    /** 节点放置 / 区块加载时登记（服务端） */
    public static void register(ServerLevel level, BlockPos pos) {
        // 兜底清理（与 WirelessFieldManager.refresh 同款）：异常停机没走平台钩子时，顺手丢掉已停止的
        // 实例分组，否则旧 server 会被这张静态表长期钉住（原先只有 ServerStoppedEvent 一处释放点）
        NODES.keySet().removeIf(server -> server != level.getServer() && !server.isRunning());
        NODES.computeIfAbsent(level.getServer(), server -> ConcurrentHashMap.newKeySet())
                .add(new Key(level.dimension(), pos.immutable()));
    }

    /** 节点被拆 / 区块卸载时注销 */
    public static void unregister(ServerLevel level, BlockPos pos) {
        Set<Key> nodes = NODES.get(level.getServer());
        if (nodes != null) {
            nodes.remove(new Key(level.dimension(), pos));
        }
    }

    /**
     * 取该维度已登记的节点坐标快照（调用方负责复核方块类型与距离）。
     * 返回副本，避免申领过程中的并发结构变更。
     */
    public static List<BlockPos> nodesIn(ServerLevel level) {
        Set<Key> nodes = NODES.get(level.getServer());
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        List<BlockPos> result = new ArrayList<>(nodes.size());
        for (Key key : nodes) {
            if (key.dimension().equals(level.dimension())) {
                result.add(key.pos());
            }
        }
        return result;
    }

    /** 服务器停止时整组丢弃：静态表不再钉住已结束的 server/level（平台层钩子调用） */
    public static void clearServer(MinecraftServer server) {
        if (server != null) {
            NODES.remove(server);
        }
    }
}
