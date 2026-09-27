package com.example.akaishi.forbidden;

import com.example.akaishi.item.AkaishiCodexItems;
import com.example.akaishi.menu.AkaishiCodexMenuRegs;
import com.example.akaishi.menu.AkaishiCodexSync;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

/**
 * 赤石禁忌（Akaishi Forbidden）的通用入口。
 *
 * <p><b>P3a</b>：禁忌秘典（物品 / 创造栏「禁忌」/ 菜单与界面 / 网络包）已自本体迁入本模块；
 * 未安装本模块时这些内容一律不注册（注册命名空间仍为三模块共用的 {@code akaishi:}，
 * 其余禁忌内容（母神祭坛 / 生命融合 / BOSS / 理智系统）在后续阶段迁入）。
 */
public final class AkaishiForbiddenMod {
    /** 模组 ID，需与禁忌模块 mods.toml 中的 modId 保持一致 */
    public static final String MOD_ID = "akaishi_forbidden";

    private AkaishiForbiddenMod() {
    }

    /** 通用初始化入口，由各平台加载器的入口类调用 */
    public static void init() {
        // 禁忌秘典：物品 → 创造栏 → 菜单类型（+ 客户端 screen factory）
        AkaishiCodexItems.register();
        AkaishiForbiddenCreativeTabs.register();
        AkaishiCodexMenuRegs.register();
        // 秘典网络包：C2S 研究请求（服务端注册；客户端注册也无害，与本体同口径）
        AkaishiCodexSync.register();
        // 秘典进度快照接收器：仅客户端注册
        if (Platform.getEnvironment() == Env.CLIENT) {
            AkaishiCodexSync.registerClient();
        }
    }
}
