package com.example.akaishi.life.altar;

import net.minecraft.world.entity.player.Player;

/**
 * 母神祭坛·仪式完成的对外回调（P3b 倒置层）。
 *
 * <p><b>为什么需要它</b>：仪式完成时理智系统要记一次"首见"（{@code mother_altar} 条目），
 * 但理智系统已按三模块定型迁往 {@code akaishi_forbidden}，本体不可反向依赖它。
 * 故在仪式侧只暴露一个"仪式完成了"的语义事件，由可选内容模块自行安装实现
 * （与项目既有的 {@code ForbiddenSetHooks} / {@code ItemAccessHolder} 同一注入范式）。
 *
 * <p>未安装实现时为空操作，不影响仪式本身。
 */
public final class AltarRitualHooks {

    /** 仪式完成回调 */
    @FunctionalInterface
    public interface RitualCompleted {
        void onRitualCompleted(Player player);
    }

    private static volatile RitualCompleted completed = player -> {
    };

    private AltarRitualHooks() {
    }

    /** 由内容模块注入实现（null 忽略） */
    public static void install(RitualCompleted impl) {
        if (impl != null) {
            completed = impl;
        }
    }

    /** 广播"母神祭坛仪式完成"（由祭坛仪式在成功结算后调用） */
    public static void ritualCompleted(Player player) {
        if (player != null) {
            completed.onRitualCompleted(player);
        }
    }
}
