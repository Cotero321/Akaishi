package com.example.akaishi.craft;

import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.wireless.WirelessFieldManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 场域内「可接收机器」扫描：列出装了无线接收升级、且确实落在无线场域里的机器。
 * <p>
 * <b>为什么是"按需扫描"而不是常驻注册表</b>：场域半径最大 3 区块（7×7 = 49 区块），
 * 要让机器主动登记就得改动二十多个机器方块实体的 tick —— 侵入面太大；
 * 而这份列表只在玩家打开派发页时用，按需遍历已加载区块的方块实体即可，
 * 调用方（菜单）再做节流与缓存。
 * <p>
 * <b>准入三连</b>：① 方块实体实现 {@link IUpgradeableMachine}；② {@code hasWirelessReceiver()} 为真；
 * ③ {@link WirelessFieldManager#inField} 命中。三者缺一不可 ——
 * 少任何一条都会出现"看得见但送不进去"的假目标。
 */
public final class WirelessMachineScanner {

    /** 单次扫描返回的机器数上限（界面只列前几台，多余的不必算） */
    public static final int MAX_RESULTS = 32;

    private WirelessMachineScanner() {
    }

    /**
     * 扫描以 {@code center} 为心、{@code radiusChunks} 个区块半径内的可接收机器，按距离由近到远。
     * <p>
     * 只遍历已加载的区块（未加载的区块本来也没有运行中的机器实体可派发）。
     */
    public static List<BlockPos> scan(ServerLevel level, BlockPos center, int radiusChunks) {
        if (radiusChunks <= 0) {
            return List.of();
        }
        int radius = Math.min(radiusChunks, WirelessFieldManager.MAX_RADIUS_CHUNKS);
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        List<BlockPos> found = new ArrayList<>();
        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            for (int cz = centerChunkZ - radius; cz <= centerChunkZ + radius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue; // 未加载：无运行中的机器
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = entry.getValue();
                    if (be.isRemoved() || !(be instanceof IUpgradeableMachine machine)) {
                        continue;
                    }
                    if (!machine.hasWirelessReceiver()) {
                        continue;
                    }
                    BlockPos pos = entry.getKey();
                    if (!WirelessFieldManager.inField(level, pos)) {
                        continue;
                    }
                    found.add(pos.immutable());
                }
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found.size() > MAX_RESULTS ? List.copyOf(found.subList(0, MAX_RESULTS)) : List.copyOf(found);
    }
}
