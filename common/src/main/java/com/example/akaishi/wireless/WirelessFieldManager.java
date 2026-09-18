package com.example.akaishi.wireless;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无线场域服务（服务端权威的静态表）。
 * <p>
 * 微缩矩阵终端按内腔「无线场域升级」数量注册场域，机器侧每 N tick 自查
 * {@link #inField(ServerLevel, BlockPos)} 决定是否参与无线调度 ——
 * <b>不做"终端每 tick 推给机器"</b>（半径 3 区块 = 49 区块遍历不可行，见设计记忆 §15.2 口径 4）。
 * <p>
 * <b>弱加载</b>：照 {@code AkaishiItemTerminalBlockEntity#updateChunkLoad} 范式用
 * {@link TicketType#PORTAL} 挂「中心区块 + 半径」的区域票据；参数变化先摘旧票再挂新票，
 * 场域归零 / 结构破损 / 终端被拆即刻释放，防票据泄漏。
 * <p>
 * <b>身份与残留</b>：键 = （维度, 方块坐标）而非 UUID（矩阵无 terminalId，位置即身份）；
 * 整张表按 {@link MinecraftServer} <b>实例分组</b>保存且只存坐标不存 {@code ServerLevel}，
 * 于是服务器一停就不再有静态强引用钉住旧世界；平台层在服务器停止时调用
 * {@link #clearServer} 显式整组丢弃。
 * <p>
 * <b>持有者</b>：每个中心记「谁在申领」（{@link Field#owners}）。同一节点可能被多台终端申领，
 * 释放只摘掉<b>该持有者</b>的占位，占位清空才真正摘票 —— 否则 A 终端拆掉会把 B 的票一起摘走。
 * <p>
 * 线程：读写都发生在服务端主线程；表用 {@link ConcurrentHashMap} 仅为满足项目并发规范。
 */
public final class WirelessFieldManager {

    /**
     * 场域半径硬上限（区块）：需求口径的场域升级最大 3 级 ⇒ 半径 3 区块（7×7 = 49 区块）。
     * 再叠加时会先被这里钳住，避免升级组合出不可承受的弱加载面积。
     */
    public static final int MAX_RADIUS_CHUNKS = 3;

    /** 一条场域记录（不含 Level 引用：Level 由所属 server + 维度反查） */
    public static final class Field {
        private final BlockPos center;
        /** 持有者坐标 → 其请求半径（区块）。同一持有者重复申领只覆盖自己的条目 */
        private final Map<BlockPos, Integer> owners = new ConcurrentHashMap<>();
        /** 当前生效半径 = 各持有者请求的最大值 */
        private volatile int radiusChunks;
        /** 已挂票据的锚点（null = 当前未挂票），摘票时必须以同一锚点与半径反挂 */
        @Nullable
        private ChunkPos ticketCenter;
        private int ticketRadius;

        private Field(BlockPos center) {
            this.center = center;
        }

        public BlockPos center() {
            return center;
        }

        public int radiusChunks() {
            return radiusChunks;
        }
    }

    /** 键：维度 + 中心坐标（BlockPos 不可变且自带 equals/hashCode） */
    private record Key(ResourceKey<Level> dimension, BlockPos center) {
    }

    private static final Map<MinecraftServer, Map<Key, Field>> FIELDS = new ConcurrentHashMap<>();

    private WirelessFieldManager() {
    }

    /**
     * 注册或刷新一条场域（{@code owner} = 申领方坐标：终端自身或终端为节点挂的子场域）。
     * <p>
     * {@code radiusChunks <= 0} 等价于 {@link #release}（该持有者退出申领）；
     * 重复施加同一持有者同一参数的票不会叠加。
     */
    public static void refresh(ServerLevel level, BlockPos center, int radiusChunks, BlockPos owner) {
        // 兜底清理：平台层 ServerStoppedEvent 钩子是主释放点，异常停机时靠这里顺手丢掉已停止的实例分组
        FIELDS.keySet().removeIf(server -> server != level.getServer() && !server.isRunning());
        int radius = Math.min(radiusChunks, MAX_RADIUS_CHUNKS);
        if (radius <= 0) {
            release(level, center, owner);
            return;
        }
        Key key = new Key(level.dimension(), center.immutable());
        Field field = fieldsOf(level.getServer()).computeIfAbsent(key, k -> new Field(k.center()));
        field.owners.put(owner.immutable(), radius);
        int effective = maxRadius(field);
        if (effective != field.radiusChunks) {
            // 半径变化：先把旧票摘掉，否则旧票会永远留在 chunkSource 里
            detach(level, field);
            field.radiusChunks = effective;
        }
        attach(level, field);
    }

    /**
     * 释放<b>某持有者</b>的占位（场域归零 / 结构破损 / 该终端放弃该节点）；
     * 占位清空才真正摘票并移除记录 —— 多台终端申领同一节点时互不干扰。
     */
    public static void release(ServerLevel level, BlockPos center, BlockPos owner) {
        Map<Key, Field> fields = FIELDS.get(level.getServer());
        if (fields == null) {
            return;
        }
        Key key = new Key(level.dimension(), center.immutable());
        Field field = fields.get(key);
        if (field == null) {
            return;
        }
        field.owners.remove(owner.immutable());
        if (field.owners.isEmpty()) {
            fields.remove(key, field);
            detach(level, field);
            return;
        }
        int effective = maxRadius(field);
        if (effective != field.radiusChunks) {
            detach(level, field);
            field.radiusChunks = effective;
            attach(level, field);
        }
    }

    /**
     * 释放整条场域（不看持有者）：节点方块被拆 / 被替换时调用 ——
     * 载体都没了，全部持有者的子场域必须一次摘净。
     */
    public static void releaseAll(ServerLevel level, BlockPos center) {
        Map<Key, Field> fields = FIELDS.get(level.getServer());
        if (fields == null) {
            return;
        }
        Field field = fields.remove(new Key(level.dimension(), center.immutable()));
        if (field != null) {
            detach(level, field);
        }
    }

    /** 服务器停止时整组丢弃：静态表不再钉住已结束的 server/level（平台层钩子调用） */
    public static void clearServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        Map<Key, Field> fields = FIELDS.remove(server);
        if (fields == null) {
            return;
        }
        for (Map.Entry<Key, Field> entry : fields.entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey().dimension());
            if (level != null) {
                detach(level, entry.getValue());
            }
        }
    }

    /** 某坐标是否落在任一场域内（机器侧自查入口） */
    public static boolean inField(ServerLevel level, BlockPos pos) {
        return fieldOf(level, pos) != null;
    }

    /** 命中的场域；不在任何场域内返回 null */
    @Nullable
    public static Field fieldOf(ServerLevel level, BlockPos pos) {
        Map<Key, Field> fields = FIELDS.get(level.getServer());
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        ChunkPos target = new ChunkPos(pos);
        for (Field field : fields.values()) {
            ChunkPos center = new ChunkPos(field.center);
            int dx = Math.abs(target.x - center.x);
            int dz = Math.abs(target.z - center.z);
            // 场域按区块切比雪夫距离判定（方形范围），与玩家直觉的"多少区块远"一致
            if (Math.max(dx, dz) <= field.radiusChunks) {
                return field;
            }
        }
        return null;
    }

    /** 当前登记的场域总数（调试/命令用） */
    public static int fieldCount() {
        int total = 0;
        for (Map<Key, Field> fields : FIELDS.values()) {
            total += fields.size();
        }
        return total;
    }

    // ===== 内部 =====

    private static Map<Key, Field> fieldsOf(MinecraftServer server) {
        return FIELDS.computeIfAbsent(server, s -> new ConcurrentHashMap<>());
    }

    private static int maxRadius(Field field) {
        int max = 0;
        for (int radius : field.owners.values()) {
            max = Math.max(max, radius);
        }
        return max;
    }

    private static void attach(ServerLevel level, Field field) {
        ChunkPos cp = new ChunkPos(field.center);
        // 幂等：同一 owner 重复施加同一票不会叠加
        level.getChunkSource().addRegionTicket(TicketType.PORTAL, cp, field.radiusChunks,
                cp.getWorldPosition());
        field.ticketCenter = cp;
        field.ticketRadius = field.radiusChunks;
    }

    private static void detach(ServerLevel level, Field field) {
        if (field.ticketCenter == null) {
            return;
        }
        level.getChunkSource().removeRegionTicket(TicketType.PORTAL, field.ticketCenter,
                field.ticketRadius, field.ticketCenter.getWorldPosition());
        field.ticketCenter = null;
    }
}
