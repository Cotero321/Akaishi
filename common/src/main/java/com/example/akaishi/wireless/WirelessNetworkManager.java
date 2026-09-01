package com.example.akaishi.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 无线赤能源终端网络注册表（全局静态，替代原频道注册表）。
 * <p>
 * 终端（终端方块成型）以唯一 UUID 注册；每个终端维护授权卡集合（安全卡认证页管理）
 * 与在线输入口/输出口位置集合。输入/输出口绑定身份卡后，按卡 UUID 反查终端注册表
 * —— 找到授权该卡的成型终端即认证成功并注册上线（参考 Flux 网络「认证即连」）。
 * <p>
 * 维度隔离：位置 key 携带维度（{@link PortKey}），跨维度口经终端跨维组件解锁后亦可注册。
 * 线程安全：全部集合用 ConcurrentHashMap / newKeySet；终端每 20 tick 惰性清理失效口（purge）。
 */
public final class WirelessNetworkManager {

    /** 终端可授权身份卡上限 */
    public static final int MAX_AUTHORIZED_CARDS = 8;

    /** 终端位置引用：维度 + 坐标 */
    public record TerminalRef(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** 端口注册 key：维度 + 坐标（asLong），隔离不同维度相同坐标的口 */
    public record PortKey(ResourceKey<Level> dimension, long pos) {
    }

    /** 终端网络状态：位置心跳 + 授权卡集合 + 在线输入/输出口集合 */
    public static final class TerminalEntry {
        public final UUID id;
        /** 终端方块位置；null = 该终端当前不在线 */
        public volatile TerminalRef ref;
        /** 终端心跳 tick（每 tick 更新，供超时兜底清理） */
        public volatile long seen;
        /** 授权身份卡集合（安全卡认证页管理，最多 {@link #MAX_AUTHORIZED_CARDS} 张） */
        public final Set<UUID> authorizedCards = ConcurrentHashMap.newKeySet();
        /** 在线输入口位置 */
        public final Set<PortKey> inputs = ConcurrentHashMap.newKeySet();
        /** 在线输出口位置 */
        public final Set<PortKey> outputs = ConcurrentHashMap.newKeySet();

        TerminalEntry(UUID id) {
            this.id = id;
        }
    }

    private static final Map<UUID, TerminalEntry> TERMINALS = new ConcurrentHashMap<>();

    private WirelessNetworkManager() {
    }

    /** 获取终端条目（不存在则创建占位） */
    public static TerminalEntry entry(UUID id) {
        return TERMINALS.computeIfAbsent(id, TerminalEntry::new);
    }

    /** 终端每 tick 注册心跳与位置 */
    public static void registerTerminal(UUID id, ResourceKey<Level> dimension, BlockPos pos, long gameTick) {
        TerminalEntry e = entry(id);
        e.ref = new TerminalRef(dimension, pos.immutable());
        e.seen = gameTick;
    }

    /** 终端被拆/结构失效：解除在线（授权卡与口集合由 purge 兜底清理） */
    public static void unregisterTerminal(UUID id) {
        TerminalEntry e = TERMINALS.get(id);
        if (e != null) {
            e.ref = null;
        }
    }

    /** 添加授权卡（超过上限返回 false） */
    public static boolean addAuthorizedCard(UUID id, UUID card) {
        TerminalEntry e = entry(id);
        if (e.authorizedCards.size() >= MAX_AUTHORIZED_CARDS) {
            return false;
        }
        return e.authorizedCards.add(card);
    }

    /** 移除授权卡 */
    public static void removeAuthorizedCard(UUID id, UUID card) {
        TerminalEntry e = TERMINALS.get(id);
        if (e != null) {
            e.authorizedCards.remove(card);
        }
    }

    /** 该卡是否已被指定终端授权 */
    public static boolean isAuthorized(UUID id, UUID card) {
        TerminalEntry e = TERMINALS.get(id);
        return e != null && e.authorizedCards.contains(card);
    }

    /** 已授权卡数量（GUI 显示用） */
    public static int authorizedCount(UUID id) {
        TerminalEntry e = TERMINALS.get(id);
        return e == null ? 0 : e.authorizedCards.size();
    }

    /** 该终端在线位置；离线返回 null */
    public static TerminalRef terminalOf(UUID id) {
        TerminalEntry e = TERMINALS.get(id);
        return e == null ? null : e.ref;
    }

    /** 终端条目（口/便携终端读取在线口集合等） */
    public static TerminalEntry entryOf(UUID id) {
        return TERMINALS.get(id);
    }

    /** 在线输入口数量 */
    public static int inputCount(UUID id) {
        TerminalEntry e = TERMINALS.get(id);
        return e == null ? 0 : e.inputs.size();
    }

    /** 在线输出口数量 */
    public static int outputCount(UUID id) {
        TerminalEntry e = TERMINALS.get(id);
        return e == null ? 0 : e.outputs.size();
    }

    /**
     * 认证查询：口绑定卡反查授权该卡的在线终端（参考 MEK 同卡配对）。
     * 遍历注册表取首个匹配的在线终端；无则返回 null。
     */
    public static UUID findTerminalForCard(UUID card) {
        if (card == null) {
            return null;
        }
        for (TerminalEntry e : TERMINALS.values()) {
            if (e.ref != null && e.authorizedCards.contains(card)) {
                return e.id;
            }
        }
        return null;
    }

    /** 输入口/输出口注册上线（口认证成功后每 tick 幂等调用） */
    public static void registerPort(UUID terminalId, ResourceKey<Level> dimension, BlockPos pos, boolean isInput) {
        TerminalEntry e = entry(terminalId);
        PortKey key = new PortKey(dimension, pos.asLong());
        if (isInput) {
            e.inputs.add(key);
        } else {
            e.outputs.add(key);
        }
    }

    /** 输入口/输出口下线 */
    public static void unregisterPort(UUID terminalId, ResourceKey<Level> dimension, BlockPos pos, boolean isInput) {
        TerminalEntry e = TERMINALS.get(terminalId);
        if (e == null) {
            return;
        }
        PortKey key = new PortKey(dimension, pos.asLong());
        if (isInput) {
            e.inputs.remove(key);
        } else {
            e.outputs.remove(key);
        }
    }

    /**
     * 惰性清理失效口（跨维度）：按各口所在维度经服务器查询方块实体，
     * 被拆/换绑/卡被撤销授权的口位从集合剔除。由终端控制器每 20 tick 调用，
     * 防止 onRemove 遗漏导致的口位悬空（计数虚高 + 区块 ticket 泄漏）。
     */
    public static void purge(UUID id, MinecraftServer server, Predicate<BlockEntity> validInput,
                             Predicate<BlockEntity> validOutput) {
        TerminalEntry e = TERMINALS.get(id);
        if (e == null) {
            return;
        }
        e.inputs.removeIf(key -> !portAlive(server, key, validInput));
        e.outputs.removeIf(key -> !portAlive(server, key, validOutput));
    }

    /** 口位是否存活：目标维度已加载且该位置存在合法口实体 */
    private static boolean portAlive(MinecraftServer server, PortKey key, Predicate<BlockEntity> valid) {
        Level target = server.getLevel(key.dimension());
        if (target == null) {
            return false;
        }
        BlockEntity be = target.getBlockEntity(BlockPos.of(key.pos()));
        return be != null && valid.test(be);
    }

    /** 在线终端总数（调试/统计用） */
    public static int terminalCount() {
        return (int) TERMINALS.values().stream().filter(e -> e.ref != null).count();
    }
}
