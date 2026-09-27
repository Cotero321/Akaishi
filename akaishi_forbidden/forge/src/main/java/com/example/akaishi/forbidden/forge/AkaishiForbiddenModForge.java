package com.example.akaishi.forbidden.forge;

import com.example.akaishi.forbidden.AkaishiForbiddenMod;
import net.minecraftforge.fml.common.Mod;

/**
 * 赤石禁忌的 Forge 平台入口（mods.toml 中 modId = akaishi_forbidden 的实现类）。
 *
 * <p>P1 阶段仅完成构造与通用初始化转发，不注册任何游戏内容，也不引入 Architectury 运行时依赖
 * （避免与本体 jar 内嵌的 architectury-forge 形成重复装载）；P3 迁入禁忌内容时再接入 Architectury 总线。
 */
@Mod(AkaishiForbiddenMod.MOD_ID)
public final class AkaishiForbiddenModForge {

    public AkaishiForbiddenModForge() {
        AkaishiForbiddenMod.init();
    }
}
