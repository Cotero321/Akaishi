package com.example.akaishi.wireless;

import com.example.akaishi.api.miniature.IMiniatureChipView;
import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.api.storage.IItemTerminalHost;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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
 * 心跳超时即视为离线（拆方块/崩溃残留不会留下幽灵条目）。
 * <p>
 * 权限口径与端口一致：绑定清单只列出该玩家具备 {@link AkaishiSecurityPermission#BUILD} 的已加载终端；
 * 真正的传输判定在端口 tick 里用终端自身的 {@code TerminalSecurity} 复核（INJECT / EXTRACT）。
 */
public final class ItemTerminalRegistry {

    /** 心跳超时（tick）：终端方块被拆/未加载后多久视为离线 */
    private static final long TIMEOUT_TICKS = 40;

    /** 在线条目：终端 ID → 位置快照（心跳刷新） */
    private static final Map<UUID, Live> TERMINALS = new ConcurrentHashMap<>();

    private ItemTerminalRegistry() {
    }

    /** 一条在线记录（不可变；心跳整条替换，天然线程安全） */
    private record Live(ResourceKey<Level> dimension, BlockPos pos, String ownerName, long tick) {
    }

    /** 终端心跳（服务端每 tick 由其 {@code serverTick} 调用） */
    public static void heartbeat(Level level, UUID terminalId, BlockPos pos, String ownerName) {
        if (level == null || level.isClientSide || terminalId == null || pos == null) {
            return;
        }
        TERMINALS.put(terminalId, new Live(level.dimension(), pos.immutable(),
                ownerName == null ? "" : ownerName, level.getGameTime()));
    }

    /** 注销（方块被拆 / 终端被微缩时调用，幂等） */
    public static void unregister(UUID terminalId) {
        if (terminalId != null) {
            TERMINALS.remove(terminalId);
        }
    }

    /**
     * 可绑定终端清单（储存口 GUI 用）：<b>当前维度内</b>、已加载、且该玩家具备「布局」权限的物品终端。
     * 顺带清理超时条目。
     */
    public static List<Entry> bindableTerminals(ServerLevel level, Player player) {
        List<Entry> result = new ArrayList<>();
        if (level == null) {
            return result;
        }
        long now = level.getGameTime();
        boolean op = player != null && player.hasPermissions(4);
        UUID viewer = player == null ? null : player.getUUID();
        List<Map.Entry<UUID, Live>> stale = new ArrayList<>();
        for (Map.Entry<UUID, Live> entry : TERMINALS.entrySet()) {
            Live live = entry.getValue();
            if (now - live.tick() > TIMEOUT_TICKS) {
                stale.add(entry);
                continue;
            }
            if (!level.dimension().equals(live.dimension())) {
                continue;
            }
            BlockPos pos = live.pos();
            // 未加载区块跳过：绑定清单只列可立即使用的终端
            if (!level.isLoaded(pos)) {
                continue;
            }
            IItemTerminalHost terminal = hostAt(level, pos);
            if (terminal == null) {
                continue;
            }
            if (!op && !terminal.security().check(viewer, AkaishiSecurityPermission.BUILD)) {
                continue;
            }
            result.add(new Entry(entry.getKey(), pos, terminal.security().ownerName()));
        }
        for (Map.Entry<UUID, Live> dead : stale) {
            // 值比对式删除：期间若刚被心跳刷新（新的 Live 实例）则保留，不做误删
            TERMINALS.remove(dead.getKey(), dead.getValue());
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

    /** 位置快照（GUI 显示「已绑定终端：维度 x,y,z」；未注册返回 null） */
    public static Snapshot locate(UUID terminalId) {
        Live live = terminalId == null ? null : TERMINALS.get(terminalId);
        return live == null ? null : new Snapshot(live.dimension(), live.pos(), live.ownerName());
    }

    /** 清单条目：终端 ID + 坐标 + 归属者名（GUI 显示用；坐标仅供显示，绑定一律用 ID） */
    public record Entry(UUID terminalId, BlockPos pos, String ownerName) {
    }

    /** 位置快照（维度 + 坐标 + 归属者名） */
    public record Snapshot(ResourceKey<Level> dimension, BlockPos pos, String ownerName) {
    }
}
