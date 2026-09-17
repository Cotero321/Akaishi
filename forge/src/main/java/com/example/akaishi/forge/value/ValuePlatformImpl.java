package com.example.akaishi.forge.value;

import com.example.akaishi.value.ValuePlatform;

import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * forge 侧估值平台桥接：把「当前服务端」的实现注入 common 层的 {@link ValuePlatform}。
 *
 * <p>用 {@link ServerLifecycleHooks#getCurrentServer()} 而非事件缓存，避免依赖事件时序。
 */
public final class ValuePlatformImpl {

    private ValuePlatformImpl() {
    }

    /** 在模组构造阶段调用一次即可 */
    public static void install() {
        ValuePlatform.setBridge(ServerLifecycleHooks::getCurrentServer);
    }
}
