package com.example.akaishi.forge.value;

import com.example.akaishi.value.ValueReloadHooks;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * forge 侧估值内核事件挂载：把平台事件翻译为 common 层语义化钩子调用。
 *
 * <p>配方变更由 {@code ValueCache} 的内容指纹自动识别，故无需额外的配方同步事件。
 */
public final class AkaishiValueForgeEvents {

    public static final AkaishiValueForgeEvents INSTANCE = new AkaishiValueForgeEvents();

    private AkaishiValueForgeEvents() {
    }

    /** 数据包重载（含战利品表）：快照作废 + 掉落索引标脏，交后续 tick 分帧重建 */
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        ValueReloadHooks.onReload();
    }

    /** 服务端每 tick 推进掉落表扫描（分帧，不阻塞主线程） */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ValueReloadHooks.tick(event.getServer());
        }
    }

    /** 世界卸载（单人退出存档）：丢弃索引，避免跨存档残留旧价 */
    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            ValueReloadHooks.onServerStopped();
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        ValueReloadHooks.onServerStopped();
    }
}
