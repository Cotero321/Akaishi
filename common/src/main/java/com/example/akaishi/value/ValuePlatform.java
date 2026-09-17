package com.example.akaishi.value;

import net.minecraft.server.MinecraftServer;

/**
 * 平台桥：common 层不直接引用加载器 API，由 forge 侧在启动时注入实现。
 *
 * <p>说明：architectury 9.2.14 的 common 构件内不含 {@code @ExpectPlatform} 注解
 * （已核验 jar 无 {@code dev.architectury.injectables} 包），故改用可注入桥接，
 * 同样满足依赖倒置，且不依赖注解处理器。
 */
public final class ValuePlatform {

    /** 平台实现：只承载 common 拿不到的加载器状态 */
    public interface Bridge {
        /** 当前服务端；客户端主菜单 / 数据包加载前返回 null */
        MinecraftServer server();
    }

    private static final Bridge EMPTY = () -> null;

    private static volatile Bridge bridge = EMPTY;

    private ValuePlatform() {
    }

    public static void setBridge(Bridge impl) {
        bridge = impl == null ? EMPTY : impl;
    }

    /** 当前服务端，可能为 null（调用方需自行兜底） */
    public static MinecraftServer server() {
        return bridge.server();
    }
}
