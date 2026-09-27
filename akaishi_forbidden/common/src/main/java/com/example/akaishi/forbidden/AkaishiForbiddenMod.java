package com.example.akaishi.forbidden;

/**
 * 赤石禁忌（Akaishi Forbidden）的通用入口。
 *
 * <p>P1 阶段为空壳：只承载 modId 常量与空初始化入口。禁忌内容（母神祭坛 / 生命融合 /
 * 秘典 / BOSS 阿盖托洛丝 / 理智系统）在 P3 迁入，且**只有安装本模块时才会注册**。
 */
public final class AkaishiForbiddenMod {
    /** 模组 ID，需与禁忌模块 mods.toml 中的 modId 保持一致 */
    public static final String MOD_ID = "akaishi_forbidden";

    private AkaishiForbiddenMod() {
    }

    /** 通用初始化入口，由各平台加载器的入口类调用（P1 暂为空实现） */
    public static void init() {
        // P3 起在此注册禁忌内容
    }
}
