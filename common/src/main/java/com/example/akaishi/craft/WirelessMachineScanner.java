package com.example.akaishi.craft;

import com.example.akaishi.api.recipe.IMachineProcessKind;
import com.example.akaishi.api.recipe.IProcessSource;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.wireless.WirelessFieldManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 场域内「可接收机器」扫描：列出装了无线接收升级、且确实落在无线场域里的机器。
 * <p>
 * <b>为什么是"按需扫描"而不是常驻注册表</b>：场域半径最大 3 区块（7×7 = 49 区块），
 * 要让机器主动登记就得改动二十多个机器方块实体的 tick —— 侵入面太大；
 * 而这份列表只在需要时用，按需遍历已加载区块的方块实体即可，调用方再做节流与缓存。
 * <p>
 * <b>准入三连</b>：① 方块实体实现 {@link IUpgradeableMachine}；② {@code hasWirelessReceiver()} 为真；
 * ③ {@link WirelessFieldManager#inField} 命中。三者缺一不可 ——
 * 少任何一条都会出现"看得见但送不进去"的假目标。
 * <p>
 * 另有 {@link #coverage} 收集<b>工序供给</b>（自家机台自述 + 接入器认可的第三方机器）。
 */
public final class WirelessMachineScanner {

    /** 单次扫描返回的机器数上限（界面只列前几台，多余的不必算） */
    public static final int MAX_RESULTS = 32;

    private WirelessMachineScanner() {
    }

    /**
     * 扫描以 {@code center} 为心、{@code radiusChunks} 个区块半径内的可接收机器，按距离由近到远。
     * <p>
     * 只遍历已加载的区块（未加载的区块本来也没有运行中的机器实体可用）。
     */
    public static List<BlockPos> scan(ServerLevel level, BlockPos center, int radiusChunks) {
        List<BlockPos> found = new ArrayList<>();
        forEachReceiver(level, center, radiusChunks, (pos, be) -> found.add(pos));
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found.size() > MAX_RESULTS ? List.copyOf(found.subList(0, MAX_RESULTS)) : List.copyOf(found);
    }

    /**
     * 扫一次场域，汇总<b>工序供给</b>（见 {@link ProcessCoverage}）。
     * <p>一次遍历同时回答三件事：哪些工序族已被覆盖、是否存在已认可的第三方机器、
     * 以及每族机台各自装了什么升级 —— 分几次扫会得到几个可能不一致的快照。
     */
    public static ProcessCoverage coverage(ServerLevel level, BlockPos center, int radiusChunks) {
        Set<RecipeType<?>> kinds = new HashSet<>();
        Map<RecipeType<?>, List<MachineSpec>> machines = new HashMap<>();
        int[] thirdParty = {0}; // 已认可的第三方机器台数（每个接入器代表 1 台）
        forEachInField(level, center, radiusChunks, (pos, be) -> {
            if (!WirelessFieldManager.inField(level, pos)) {
                return; // 场域外的方块与网络无关（半径内不等于在场域内）
            }
            if (be instanceof IUpgradeableMachine machine && machine.hasWirelessReceiver()
                    && be instanceof IMachineProcessKind self) {
                RecipeType<?> kind = self.processKind();
                if (kind != null) {
                    kinds.add(kind);
                    // 机台明细：升级件直接读它的升级槽（速度影响耗时/每 tick 能耗，能量只影响容量）
                    MachineUpgradeSlots slots = machine.getUpgradeSlots();
                    machines.computeIfAbsent(kind, key -> new ArrayList<>(2))
                            .add(MachineSpec.of(slots.getSpeedCount(), slots.getEnergyCount()));
                }
            }
            if (be instanceof IProcessSource source && source.isRecognized()) {
                thirdParty[0]++; // 一个接入器 = 一台已认可的第三方机器（虚拟加工按台数分担）
                kinds.addAll(source.providedProcesses());
            }
        });
        Map<RecipeType<?>, List<MachineSpec>> frozen = new HashMap<>(machines.size());
        machines.forEach((kind, specs) -> frozen.put(kind, List.copyOf(specs)));
        return new ProcessCoverage(Set.copyOf(kinds), thirdParty[0] > 0, Map.copyOf(frozen), thirdParty[0]);
    }

    /**
     * 遍历场域内的「可接收机器」。<b>准入三连</b>：① 实现 {@link IUpgradeableMachine}；
     * ② {@code hasWirelessReceiver()} 为真；③ {@link WirelessFieldManager#inField} 命中。
     * 三者缺一不可 —— 少任何一条都会出现"看得见但送不进去"的假目标。
     */
    private static void forEachReceiver(ServerLevel level, BlockPos center, int radiusChunks,
            BiConsumer<BlockPos, BlockEntity> visitor) {
        forEachInField(level, center, radiusChunks, (pos, be) -> {
            if (!(be instanceof IUpgradeableMachine machine) || !machine.hasWirelessReceiver()) {
                return;
            }
            if (!WirelessFieldManager.inField(level, pos)) {
                return;
            }
            visitor.accept(pos, be);
        });
    }

    /**
     * 按区块遍历半径内已加载区块的方块实体（<b>不做任何准入判定</b>，由调用方自行筛选）。
     * <p>未加载的区块直接跳过：里面即使有机器也没有运行中的方块实体。
     * <p><b>对同族开放</b>：真机加工的选机（{@code craft.exec.EndpointFinder}）也要按同一片场域遍历，
     * 复用这里可以保证"准入看到的机器"与"执行找得到的机器"是同一批。
     */
    public static void forEachInField(ServerLevel level, BlockPos center, int radiusChunks,
            BiConsumer<BlockPos, BlockEntity> visitor) {
        if (radiusChunks <= 0) {
            return;
        }
        int radius = Math.min(radiusChunks, WirelessFieldManager.MAX_RADIUS_CHUNKS);
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            for (int cz = centerChunkZ - radius; cz <= centerChunkZ + radius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                    BlockEntity be = entry.getValue();
                    if (be.isRemoved()) {
                        continue;
                    }
                    visitor.accept(entry.getKey().immutable(), be);
                }
            }
        }
    }
}
