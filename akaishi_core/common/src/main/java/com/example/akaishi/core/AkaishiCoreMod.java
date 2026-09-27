package com.example.akaishi.core;

/**
 * 赤石核心（Akaishi Core）的通用入口。
 *
 * <p>本类在 P1 阶段只承载 modId 常量与空的初始化入口，等 P2 抽 API 时再填充实际内容。
 * 三模块共用 {@code akaishi:} 注册命名空间，故此处不再单独持有命名空间常量。
 */
public final class AkaishiCoreMod {
    /** 模组 ID，需与核心模块 mods.toml 中的 modId 保持一致 */
    public static final String MOD_ID = "akaishi_core";

    private AkaishiCoreMod() {
    }

    /** 通用初始化入口，由各平台加载器的入口类调用（P1 暂为空实现） */
    public static void init() {
        // P2 起在此注册核心 API / 事件框架
    }
}
