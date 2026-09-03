package com.example.akaishi.menu;

import com.example.akaishi.api.storage.IStorageVault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 机器 × 存储库联动工具（统一机制，本项目多机器复用）。
 * - 机器打开界面时检测 3×3×3 范围内匹配类型的 IStorageVault（样本库/器官储藏库/药剂库）
 * - 命中则向菜单注入一组分页联动槽（LinkedVaultSlot，18 格/页），界面侧弹浮层存取
 * - 客户端本地翻页/开关状态存于 StorageLinkState，无需网络包；
 *   服务端与客户端在 Menu 构造时分别扫描（同世界状态，结果一致）
 *
 * 浮层统一坐标：标题 y=12，槽位 y=24-60（两行九列），翻页按钮 y=64。
 */
public final class StorageLink {

    /** 联动检测范围（曼哈顿/欧氏 3 格） */
    public static final int RADIUS = 3;
    /** 每页槽位数（浮层两行九列） */
    public static final int PAGE_SLOTS = 18;
    /** 浮层槽位区坐标 */
    public static final int SLOT_X = 8, SLOT_Y = 24;

    private StorageLink() {
    }

    /**
     * 尝试为菜单建立联动（匹配任意类型存储库）：范围内存在存储库则注入联动槽并返回状态，否则返回 null。
     * 调用时机：Menu 构造末尾（服务端与客户端同一工厂，结果一致）。
     * slotAdder 由菜单自身传入（this::addSlot，addSlot 为 AbstractContainerMenu 保护方法）。
     */
    @Nullable
    public static StorageLinkState tryLink(Consumer<Slot> slotAdder, Level level, @Nullable BlockPos center) {
        return tryLink(slotAdder, level, center, vault -> true);
    }

    /**
     * 尝试为菜单建立联动（仅匹配 filter 认可的存储库）：
     * 机器按自身需求过滤库类型（如分析台只连样本库），避免范围存在多库时误连。
     * filter 需两侧一致：客户端与服务端同用 {@link IStorageVault} 类型判断，结果一致。
     */
    @Nullable
    public static StorageLinkState tryLink(Consumer<Slot> slotAdder, Level level, @Nullable BlockPos center,
                                           Predicate<IStorageVault> filter) {
        if (center == null) {
            return null;
        }
        IStorageVault vault = findNearby(level, center, filter);
        if (vault == null) {
            return null;
        }
        StorageLinkState state = new StorageLinkState();
        state.storage = vault.getVaultContainer();
        state.nameKey = vault.getVaultNameKey();
        Container storage = state.storage;
        for (int i = 0; i < PAGE_SLOTS; i++) {
            slotAdder.accept(new LinkedVaultSlot(state, storage, i,
                    SLOT_X + (i % 9) * 18, SLOT_Y + (i / 9) * 18));
        }
        return state;
    }

    /** 扫描中心 3 格范围内最近的 IStorageVault（无则 null） */
    @Nullable
    public static IStorageVault findNearby(Level level, BlockPos center) {
        return findNearby(level, center, vault -> true);
    }

    /** 扫描中心 3 格范围内最近的、满足 filter 的 IStorageVault（无则 null） */
    @Nullable
    public static IStorageVault findNearby(Level level, BlockPos center, Predicate<IStorageVault> filter) {
        IStorageVault best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos min = center.offset(-RADIUS, -RADIUS, -RADIUS);
        BlockPos max = center.offset(RADIUS, RADIUS, RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IStorageVault vault && filter.test(vault)) {
                double dist = pos.distSqr(center);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = vault;
                }
            }
        }
        return best;
    }

    /** 联动存储分页总数（每页 18 格） */
    public static int pageCount(StorageLinkState state) {
        if (state.storage == null) {
            return 0;
        }
        return (state.storage.getContainerSize() + PAGE_SLOTS - 1) / PAGE_SLOTS;
    }
}
