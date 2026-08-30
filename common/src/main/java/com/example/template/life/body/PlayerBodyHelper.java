package com.example.template.life.body;

import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.Function;

/**
 * 玩家躯体状态静态访问 API：
 * common 通过本类读取玩家 capability，平台侧（Forge）在启动时注入 provider。
 * 避免 common 直接依赖 Forge Capability，保持跨平台可移植。
 */
public final class PlayerBodyHelper {

    private static Function<Player, IPlayerBodyState> provider = player -> null;

    private PlayerBodyHelper() {
    }

    /** 平台侧注入 capability 获取器（Forge 启动时调用一次） */
    public static void registerProvider(Function<Player, IPlayerBodyState> provider) {
        PlayerBodyHelper.provider = Objects.requireNonNull(provider);
    }

    /** 获取玩家躯体状态（未挂载 capability 时为 null，调用方需判空） */
    public static IPlayerBodyState of(Player player) {
        return provider.apply(player);
    }
}
