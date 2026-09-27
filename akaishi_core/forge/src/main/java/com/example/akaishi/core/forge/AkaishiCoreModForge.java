package com.example.akaishi.core.forge;

import com.example.akaishi.core.AkaishiCoreMod;
import net.minecraftforge.fml.common.Mod;

/**
 * 赤石核心的 Forge 平台入口（mods.toml 中 modId = akaishi_core 的实现类）。
 *
 * <p>P1 阶段仅完成构造与通用初始化转发，不注册任何游戏内容，也不引入 Architectury 运行时依赖
 * （避免与本体 jar 内嵌的 architectury-forge 形成重复装载）；P2 抽取 API 时再接入 Architectury 总线。
 */
@Mod(AkaishiCoreMod.MOD_ID)
public final class AkaishiCoreModForge {

    public AkaishiCoreModForge() {
        AkaishiCoreMod.init();
    }
}
