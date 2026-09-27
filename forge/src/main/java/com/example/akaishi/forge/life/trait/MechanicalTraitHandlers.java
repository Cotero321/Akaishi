package com.example.akaishi.forge.life.trait;

/**
 * 材料特性处理器注册入口（forge 侧）。
 * <p>
 * 由 {@code AkaishiModForge} 在通用初始化之后调用一次：
 * <ul>
 *   <li>注册落在 7 个通用钩子上的处理器（生存 5 / 攻击 6 / 机动 2）；</li>
 *   <li>{@code AkaishiMechanicalTraitEvents} 承载需平台事件的 4 个特性（净化滤芯 / 掘进 / 探矿 / 数据收割），
 *       由平台单独注册到游戏总线。</li>
 * </ul>
 * 未注册处理器的特性只展示、不生效、不报错（企划 §3）。
 */
public final class MechanicalTraitHandlers {

    private MechanicalTraitHandlers() {
    }

    /** 注册全部内置特性处理器；重复调用会因注册表检测到重复而抛异常（仅调用一次）。 */
    public static void register() {
        SurvivalTraitHandlers.register();
        AttackTraitHandlers.register();
        MobilityTraitHandlers.register();
    }
}
