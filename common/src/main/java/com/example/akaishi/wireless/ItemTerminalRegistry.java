package com.example.akaishi.wireless;

import com.example.akaishi.api.miniature.IMiniatureChipView;
import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.api.storage.IItemTerminalHost;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物品终端（储存终端）注册表：给「储存无线输入/输出口」提供<b>可绑定终端清单</b>与按 ID 解析。
 * <p>
 * 与 {@link WirelessNetworkManager} 的区别：物品终端<b>不参与无线能量网络</b>（没有网络能量中枢语义，
 * 安全表本地权威），所以不做网络注册、不做端口清单维护；但<b>寻址口径与无线族一致</b>——
 * 用终端自身的唯一 ID（{@code terminalId}，BE 首次生成并 NBT 持久化）+ 心跳定位，
 * 而不是「维度 + 坐标」：坐标会随方块搬动/微缩而变，ID 不会。
 * <p>
 * <b>权威口径（本类唯一事实来源）</b>：一条记录是否"活着"，以<b>世界</b>为准 ——
 * 记录的坐标处必须存在宿主，且该宿主<b>自报的 ID 与记录 key 严格相等</b>（与 {@link #resolve} 同口径）。
 * 任一条不满足即判失效并<b>确定性摘除</b>（由 {@link #serverTick} 每 {@link #SWEEP_INTERVAL_TICKS} tick
 * 主动清扫，不再依赖"有人开界面才清理"）。历史上只有超时/已加载/宿主非空三道过滤，缺少 ID 校验，
 * 于是"记录过期而坐标被另一台终端占用"时清单会列出并不存在的终端（幽灵条目）。
 * <p>
 * 心跳超时即视为离线（拆方块/崩溃残留不会留下幽灵条目）；同一 ID 出现在两个坐标
 * （复制出的同 ID 终端）时<b>先注册者优先</b>并记日志，避免两台互相覆盖造成绑定目标抖动。
 */
public final class ItemTerminalRegistry {

    /** 排障日志：只在"同 ID 出现在两个坐标"这类异常路径上打点 */
    private static final Logger LOGGER = LogManager.getLogger("akaishi.itemterminal");

    /** 心跳超时（tick）：终端方块被拆/未加载后多久视为离线 */
    private static final long TIMEOUT_TICKS = 40;

    /** 失效条目主动清扫间隔（tick）；待调手感值 */
    private static final long SWEEP_INTERVAL_TICKS = 40;

    /** 同 ID 冲突日志限流（tick，同一 ID 两次记录的最小间隔）；待调手感值 */
    private static final long CONFLICT_LOG_INTERVAL_TICKS = 200;

    /** 在线条目：终端 ID → 位置快照（心跳刷新） */
    private static final Map<UUID, Live> TERMINALS = new ConcurrentHashMap<>();

    /** 冲突日志限流表：终端 ID → 上次记录时的游戏时间 */
    private static final Map<UUID, Long> CONFLICT_LOGGED = new ConcurrentHashMap<>();

    private ItemTerminalRegistry() {
    }

    /** 一条在线记录（不可变；心跳整条替换，天然线程安全） */
    private record Live(ResourceKey<Level> dimension, BlockPos pos, String ownerName, Component name, long tick) {
    }

    /**
     * 终端心跳（服务端每 tick 由其 {@code serverTick} 调用）。
     * <p>
     * 同一 ID 从<b>不同坐标</b>上报（复制出的同 ID 终端）时<b>保留先注册者的坐标</b>，
     * 只刷新心跳时间 —— 否则两台会逐 tick 互相覆盖坐标，绑定目标随之抖动。冲突会限流记 WARN 日志。
     */
    public static void heartbeat(Level level, UUID terminalId, BlockPos pos, String ownerName, Component name) {
        if (level == null || level.isClientSide || terminalId == null || pos == null) {
            return;
        }
        BlockPos at = pos.immutable();
        ResourceKey<Level> dimension = level.dimension();
        String owner = ownerName == null ? "" : ownerName;
        Component display = name == null ? Component.empty() : name;
        long now = level.getGameTime();
        TERMINALS.compute(terminalId, (id, old) -> {
            if (old == null) {
                return new Live(dimension, at, owner, display, now);
            }
            if (!old.pos().equals(at) || !old.dimension().equals(dimension)) {
                logConflict(id, old, dimension, at, now);
                return new Live(old.dimension(), old.pos(), old.ownerName(), old.name(), now);
            }
            return new Live(dimension, at, owner, display, now);
        });
    }

    /** 同 ID 双坐标冲突日志（限流；只记录、不改变"先注册者优先"的择一结果） */
    private static void logConflict(UUID id, Live kept, ResourceKey<Level> dimension, BlockPos at, long now) {
        Long last = CONFLICT_LOGGED.get(id);
        if (last != null && now - last < CONFLICT_LOG_INTERVAL_TICKS) {
            return;
        }
        CONFLICT_LOGGED.put(id, now);
        LOGGER.warn("[akaishi] duplicate item terminal id {}: keeping first registered {} {} and ignoring {} {}",
                id, kept.dimension().location(), kept.pos(), dimension.location(), at);
    }

    /** 注销（方块被拆 / 终端被微缩时调用，幂等） */
    public static void unregister(UUID terminalId) {
        if (terminalId != null) {
            TERMINALS.remove(terminalId);
            CONFLICT_LOGGED.remove(terminalId);
        }
    }

    /**
     * 服务端每 tick 驱动（Architectury {@code TickEvent.SERVER_LEVEL_POST}，每个维度各调一次）：
     * 按 {@link #SWEEP_INTERVAL_TICKS} 节拍清扫失效条目。
     * <p>
     * 这是"记录失效即确定性清除"的唯一保证点 —— 关闭界面、无人查看时同样生效。
     */
    public static void serverTick(ServerLevel level) {
        if (level == null || TERMINALS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        if (now % SWEEP_INTERVAL_TICKS != 0L) {
            return;
        }
        for (Map.Entry<UUID, Live> entry : TERMINALS.entrySet()) {
            Live live = entry.getValue();
            if (!level.dimension().equals(live.dimension())) {
                continue;
            }
            if (isDead(entry.getKey(), live, level, now)) {
                TERMINALS.remove(entry.getKey(), live);
            }
        }
    }

    /**
     * 记录是否已失效：
     * <ul>
     *   <li><b>超时</b>：心跳停了（方块被拆、区块卸载、崩溃残留）；</li>
     *   <li><b>游戏时间回退</b>：记录里的 tick 晚于当前（同 JVM 换存档 / 换世界；静态表不会跨世界清空，
     *       此时"差值"为负、超时判据永远不成立，必须显式判死）；</li>
     *   <li><b>坐标已被顶替</b>：区块已加载，但该坐标的宿主自报 ID 与记录 key 不等（含宿主消失）。</li>
     * </ul>
     */
    private static boolean isDead(UUID terminalId, Live live, ServerLevel level, long now) {
        if (now - live.tick() > TIMEOUT_TICKS || live.tick() > now) {
            return true;
        }
        if (!level.isLoaded(live.pos())) {
            return false; // 区块未加载：无法判定宿主，交由超时兜底，不误删
        }
        return !terminalId.equals(hostIdAt(level, live.pos()));
    }

    /**
     * 可绑定终端清单（储存口 GUI 用）：<b>当前维度内</b>、已加载、已成型、且该玩家具备「布局」权限的物品终端。
     * <p>
     * 顺带清除本轮命中的失效条目（清单侧即时生效；长期保证由 {@link #serverTick} 负责）。
     * <b>未成型终端不列出</b>：宿主存在但结构不完整时，端口一件也搬不动（无储存单元、缓冲不汇聚），
     * 列出来就是"看得到、连得上、用不到"。
     */
    public static List<Entry> bindableTerminals(ServerLevel level, Player player) {
        List<Entry> result = new ArrayList<>();
        if (level == null) {
            return result;
        }
        long now = level.getGameTime();
        boolean op = player != null && player.hasPermissions(4);
        UUID viewer = player == null ? null : player.getUUID();
        List<Map.Entry<UUID, Live>> dead = new ArrayList<>();
        for (Map.Entry<UUID, Live> entry : TERMINALS.entrySet()) {
            Live live = entry.getValue();
            if (!level.dimension().equals(live.dimension())) {
                continue; // 只列本维度；别的维度残留由 serverTick 轮到该维度时清扫
            }
            if (isDead(entry.getKey(), live, level, now)) {
                dead.add(entry);
                continue;
            }
            BlockPos pos = live.pos();
            if (!level.isLoaded(pos)) {
                continue; // 未加载区块跳过：绑定清单只列可立即使用的终端
            }
            IItemTerminalHost terminal = hostAt(level, pos);
            // 世界权威：坐标处必须有宿主，且宿主自报 ID 必须等于条目 key（与 resolve 同口径）
            if (terminal == null || !entry.getKey().equals(terminal.terminalId())) {
                dead.add(entry);
                continue;
            }
            if (!terminal.isFormed()) {
                continue; // 未成型不接受存取，不进清单
            }
            if (!op && !terminal.security().check(viewer, AkaishiSecurityPermission.BUILD)) {
                continue;
            }
            result.add(new Entry(entry.getKey(), pos, terminal.security().ownerName(),
                    displayNameAt(level, pos, live.name()), shortId(entry.getKey())));
        }
        for (Map.Entry<UUID, Live> record : dead) {
            // 值比对式删除：期间若刚被心跳刷新（新的 Live 实例）则保留，不做误删
            TERMINALS.remove(record.getKey(), record.getValue());
        }
        result.sort((a, b) -> {
            int byHeight = Integer.compare(b.pos().getY(), a.pos().getY());
            return byHeight != 0 ? byHeight : Integer.compare(a.ownerName().hashCode(), b.ownerName().hashCode());
        });
        return result;
    }

    /**
     * 按终端 ID 解析（跨维度）：未注册 / 已超时 / 目标区块未加载 / 该位置已换成别的终端
     * —— 一律返回 null。位置与 ID 双重校验，避免「终端搬走后新方块占用旧坐标」被误认。
     * <p>
     * 返回接口而非具体类：微缩后的终端同样是「物品终端宿主」，端口与搬运引擎无需区分形态。
     * <p>
     * <b>刻意不校验 {@link IItemTerminalHost#isFormed()}</b>：未成型也要能解析出宿主，
     * 端口才能把失败原因区分成「未成型」而不是「目标不存在」。
     */
    public static IItemTerminalHost resolve(MinecraftServer server, UUID terminalId) {
        if (server == null || terminalId == null) {
            return null;
        }
        Live live = TERMINALS.get(terminalId);
        if (live == null) {
            return null;
        }
        ServerLevel level = server.getLevel(live.dimension());
        if (level == null || !level.isLoaded(live.pos())) {
            return null;
        }
        IItemTerminalHost terminal = hostAt(level, live.pos());
        return terminal != null && terminalId.equals(terminal.terminalId()) ? terminal : null;
    }

    /**
     * 取该位置的「物品终端宿主」。
     * <p>
     * <b>不能只按方块实体接口判</b>：微缩件本身不是 {@link IItemTerminalHost}
     * （库与落账能力在族状态里，见 {@code IMiniatureChipView#itemHost()}），
     * 只按接口判会把微缩后的终端当成"没有终端" —— 绑定清单里消失、已绑定的口也搬不动。
     */
    @Nullable
    private static IItemTerminalHost hostAt(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IItemTerminalHost host) {
            return host;
        }
        return be instanceof IMiniatureChipView chip ? chip.itemHost() : null;
    }

    /** 该坐标宿主的自报 ID（无宿主为 null）；权威判据用 */
    @Nullable
    private static UUID hostIdAt(Level level, BlockPos pos) {
        IItemTerminalHost host = hostAt(level, pos);
        return host == null ? null : host.terminalId();
    }

    /**
     * 终端显示名：优先自定义名，其次默认显示名。
     * <p>
     * 取方块实体实现的 {@link MenuProvider#getDisplayName()}（物品终端与微缩芯片都经它给出名称：
     * 微缩芯片由适配器返回<b>族名</b>）；实现 {@code Nameable} 的方块实体在其中自会优先返回自定义名。
     * 只实现 {@link BlockEntity} 的宿主退回方块描述名。读不到方块实体时退回心跳缓存的名字。
     */
    private static Component displayNameAt(Level level, BlockPos pos, Component fallback) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return fallback == null ? Component.empty() : fallback;
        }
        if (be instanceof MenuProvider provider) {
            return provider.getDisplayName();
        }
        return Component.translatable(be.getBlockState().getBlock().getDescriptionId());
    }

    /** 位置快照（GUI 显示「已绑定终端：名称 · 短号 · 维度 x,y,z」；未注册返回 null） */
    public static Snapshot locate(UUID terminalId) {
        Live live = terminalId == null ? null : TERMINALS.get(terminalId);
        return live == null ? null
                : new Snapshot(live.dimension(), live.pos(), live.ownerName(), live.name(), shortId(terminalId));
    }

    /** 终端短号（8 位 hex）：口径与储存口界面的绑定身份短号一致（UUID 高 32 位） */
    public static String shortId(UUID terminalId) {
        return terminalId == null ? "--------"
                : String.format("%08X", (int) (terminalId.getMostSignificantBits() >>> 32));
    }

    /**
     * 清单条目：终端 ID + 坐标 + 归属者名 + 显示名 + 短号。
     * <p>
     * 坐标仅供显示与悬停提示；绑定一律用 ID（终端搬动/微缩后坐标会变，ID 不会）。
     */
    public record Entry(UUID terminalId, BlockPos pos, String ownerName, Component name, String shortId) {

        /** 附属模组用法：只关心 ID / 坐标 / 归属者时（名称取空、短号按 ID 现算）；工程内部一律用 5 参构造。 */
        public Entry(UUID terminalId, BlockPos pos, String ownerName) {
            this(terminalId, pos, ownerName, Component.empty(), ItemTerminalRegistry.shortId(terminalId));
        }
    }

    /** 位置快照（维度 + 坐标 + 归属者名 + 显示名 + 短号） */
    public record Snapshot(ResourceKey<Level> dimension, BlockPos pos, String ownerName, Component name,
            String shortId) {
    }
}
