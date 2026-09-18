package com.example.akaishi.wireless;

import com.example.akaishi.api.security.AkaishiSecurityPermission;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
 * 终端（终端方块成型）以唯一 UUID 注册；每个终端维护在线输入口/输出口位置集合，并镜像终端侧
 * 权威的「归属者 + 权限表 + 默认权限条目」安全状态（见 {@link #updateSecurity}）。
 * 端口记录「绑定身份（玩家 UUID）+ 终端 ID」，判定一律走安全表
 * （{@link AkaishiSecurityPermission#INJECT} 输入口、{@link AkaishiSecurityPermission#EXTRACT} 输出口、
 * {@link AkaishiSecurityPermission#BUILD} 绑定）；无端口入口（便携终端）按身份经
 * {@link #findTerminalForIdentity} 反查，不再有「身份卡白名单」这一层。
 * <p>
 * 维度隔离：位置 key 携带维度（{@link PortKey}），跨维度口经终端跨维组件解锁后亦可注册。
 * 线程安全：全部集合用 ConcurrentHashMap / newKeySet；终端每 20 tick 惰性清理失效口（purge）。
 */
public final class WirelessNetworkManager {

    /** 终端位置引用：维度 + 坐标 */
    public record TerminalRef(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** 端口注册 key：维度 + 坐标（asLong），隔离不同维度相同坐标的口 */
    public record PortKey(ResourceKey<Level> dimension, long pos) {
    }

    /** 终端网络状态：位置心跳 + 在线输入/输出口集合 + 安全状态镜像 */
    public static final class TerminalEntry {
        public final UUID id;
        /** 终端方块位置；null = 该终端当前不在线 */
        public volatile TerminalRef ref;
        /** 终端心跳 tick（每 tick 更新，供超时兜底清理） */
        public volatile long seen;
        /** 在线输入口位置 */
        public final Set<PortKey> inputs = ConcurrentHashMap.newKeySet();
        /** 在线输出口位置 */
        public final Set<PortKey> outputs = ConcurrentHashMap.newKeySet();
        /** 所属网络族（赤能源 / 生命能量；默认 CHISHI，旧注册路径与占位条目行为不变） */
        public volatile WirelessFamily family = WirelessFamily.CHISHI;

        // ===== 安全状态（由终端安全页实时推送，见 updateSecurity） =====

        /** 归属者（放置终端的玩家，恒全权限）；null = 未知（控制台/结构生成） */
        public volatile UUID owner;
        /** 权限表：玩家 → 权限位掩码 */
        public final Map<UUID, Integer> playerPerms = new ConcurrentHashMap<>();
        /** 权限表玩家名（仅显示用；以 UUID 为准） */
        public final Map<UUID, String> playerNames = new ConcurrentHashMap<>();
        /** 是否登记了「默认权限条目」（未绑定身份的卡） */
        public volatile boolean hasDefaultEntry;
        /** 默认权限条目掩码 */
        public volatile int defaultPerms;

        TerminalEntry(UUID id) {
            this.id = id;
        }
    }

    /**
     * 终端推送安全状态（安全页/卡库变化时调用）。
     * <p>
     * 权限表与默认条目是<b>终端侧权威数据</b>，注册表只做镜像，供无关卡在手的入口（端口、便携终端）
     * 与客户端 UI 查询；判定一律走 {@link #hasPermission}。
     */
    public static void updateSecurity(UUID id, UUID owner, Map<UUID, Integer> perms, Map<UUID, String> names,
                                      boolean hasDefaultEntry, int defaultPerms) {
        TerminalEntry e = entry(id);
        e.owner = owner;
        e.playerPerms.clear();
        e.playerPerms.putAll(perms);
        e.playerNames.clear();
        e.playerNames.putAll(names);
        e.hasDefaultEntry = hasDefaultEntry;
        e.defaultPerms = defaultPerms & AkaishiSecurityPermission.ALL;
    }

    /** 安全系统是否已启用：登记了任何条目才算启用；一条都没有 = 未启用（全放行，AE2 口径） */
    public static boolean securityEnabled(UUID id) {
        TerminalEntry e = TERMINALS.get(id);
        return e != null && (!e.playerPerms.isEmpty() || e.hasDefaultEntry);
    }

    /** 终端归属者（GUI 显示/判定用）；未知返回 null */
    public static UUID ownerOf(UUID id) {
        TerminalEntry e = TERMINALS.get(id);
        return e == null ? null : e.owner;
    }

    /** 权限表快照（客户端 UI / 便利查询用） */
    public static TerminalEntry securityStateOf(UUID id) {
        return TERMINALS.get(id);
    }

    /**
     * 权限判定（镜像口径）：规则本体在 {@link TerminalSecurity#allows}，此处只取镜像字段。
     * <p>
     * 注册表里没有该终端（未注册 / 条目被清 / 已拆）⇒ <b>拒绝</b>（fail-closed）。
     * 先前这里返回 true（fail-open）：镜像缺失就一律放行，任何"注册表里查不到的 id"都能通过判定，
     * 是一颗随时会被踩到的暗雷 —— 宁可拒绝，也不要默认放行。
     */
    public static boolean hasPermission(UUID id, UUID player, AkaishiSecurityPermission perm) {
        TerminalEntry e = TERMINALS.get(id);
        if (e == null) {
            return false;
        }
        return TerminalSecurity.allows(e.owner, e.playerPerms, e.hasDefaultEntry, e.defaultPerms, player, perm);
    }

    /** 权限判定（玩家入口）：op（权限等级 4）放行，便于管理服例外操作 */
    public static boolean hasPermission(UUID id, Player player, AkaishiSecurityPermission perm) {
        if (player == null) {
            return true;
        }
        if (player.hasPermissions(4)) {
            return true;
        }
        return hasPermission(id, player.getUUID(), perm);
    }

    private static final Map<UUID, TerminalEntry> TERMINALS = new ConcurrentHashMap<>();

    private WirelessNetworkManager() {
    }

    /** 获取终端条目（不存在则创建占位） */
    public static TerminalEntry entry(UUID id) {
        return TERMINALS.computeIfAbsent(id, TerminalEntry::new);
    }

    /**
     * 按身份反查终端（旧「持身份卡右键绑定」兼容路径）：目标族内、该身份拥有指定权限的第一个在线终端。
     */
    public static UUID findTerminalForIdentity(UUID identity, WirelessFamily family,
            AkaishiSecurityPermission perm) {
        if (identity == null) {
            return null;
        }
        WirelessFamily f = family == null ? WirelessFamily.CHISHI : family;
        UUID found = null;
        for (TerminalEntry e : TERMINALS.values()) {
            if (e.family == f && e.ref != null && hasPermission(e.id, identity, perm)
                    && (found == null || e.id.toString().compareTo(found.toString()) < 0)) {
                found = e.id;
            }
        }
        return found;
    }

    /**
     * 可远程绑定的终端清单（端口 GUI 用）：目标族内、该玩家具备 {@link AkaishiSecurityPermission#BUILD}
     * 权限且当前在线的终端，按 ID 字符串排序 —— 客户端渲染顺序与服务端解算顺序一致。
     */
    public static java.util.List<TerminalEntry> bindableTerminals(WirelessFamily family, UUID player) {
        WirelessFamily f = family == null ? WirelessFamily.CHISHI : family;
        java.util.List<TerminalEntry> list = new java.util.ArrayList<>();
        for (TerminalEntry e : TERMINALS.values()) {
            if (e.family == f && e.ref != null
                    && hasPermission(e.id, player, AkaishiSecurityPermission.BUILD)) {
                list.add(e);
            }
        }
        list.sort(java.util.Comparator.comparing(e -> e.id.toString()));
        return list;
    }

    /** 终端短 ID 文本（GUI 显示用，8 位 hex，与终端方块实体的短号口径一致） */
    public static String shortId(UUID id) {
        return id == null ? "" : id.toString().substring(0, 8).toUpperCase();
    }

    /**
     * 终端每 tick 注册心跳与位置（默认赤能源族，旧调用路径不变） */
    public static void registerTerminal(UUID id, ResourceKey<Level> dimension, BlockPos pos, long gameTick) {
        registerTerminal(id, dimension, pos, gameTick, WirelessFamily.CHISHI);
    }

    /** 终端每 tick 注册心跳与位置，并声明所属网络族（生命终端注册走本重载） */
    public static void registerTerminal(UUID id, ResourceKey<Level> dimension, BlockPos pos, long gameTick,
                                        WirelessFamily family) {
        TerminalEntry e = entry(id);
        e.ref = new TerminalRef(dimension, pos.immutable());
        e.seen = gameTick;
        e.family = family == null ? WirelessFamily.CHISHI : family;
    }

    /** 终端被拆/结构失效：解除在线（口集合由 purge 兜底清理） */
    public static void unregisterTerminal(UUID id) {
        TerminalEntry e = TERMINALS.get(id);
        if (e != null) {
            e.ref = null;
        }
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
        BlockPos pos = BlockPos.of(key.pos());
        // 仅检查已加载区块，避免 getBlockEntity 触发未加载区块的同步加载/生成（跨维度周期性抖动）
        if (!((ServerLevel) target).isLoaded(pos)) {
            return false;
        }
        BlockEntity be = target.getBlockEntity(pos);
        return be != null && valid.test(be);
    }

    /** 在线终端总数（调试/统计用） */
    public static int terminalCount() {
        return (int) TERMINALS.values().stream().filter(e -> e.ref != null).count();
    }
}
