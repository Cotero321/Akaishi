package com.example.akaishi.value;

import net.minecraft.server.MinecraftServer;

/**
 * 估值内核的重载钩子：平台层把「数据包重载 / 服务端停止 / tick」翻译成这里的方法调用，
 * 保证「配方变更 → 快照失效」「战利品表变更 → 掉落索引重建」的时序正确。
 *
 * <p>common 层不直接依赖平台事件，仅暴露语义化入口（依赖倒置）。
 */
public final class ValueReloadHooks {

    private ValueReloadHooks() {
    }

    /** 数据包 / 配置重载：作废估值快照，并标记掉落索引待重建 */
    public static void onReload() {
        ValueCache.invalidate();
        LootValueIndex.markDirty();
    }

    /** 服务端停止 / 世界卸载：彻底丢弃缓存与索引，避免跨存档残留旧价 */
    public static void onServerStopped() {
        ValueCache.invalidate();
        LootValueIndex.clear();
    }

    /** 服务端每 tick 推进掉落索引扫描（由平台 tick 事件驱动） */
    public static void tick(MinecraftServer server) {
        LootValueIndex.tick(server);
    }
}
