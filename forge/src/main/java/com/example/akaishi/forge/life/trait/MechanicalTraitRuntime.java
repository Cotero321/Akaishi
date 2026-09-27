package com.example.akaishi.forge.life.trait;

import net.minecraft.world.effect.MobEffect;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 材料特性的「按玩家瞬时状态」（不落盘）：连击层数 / 持续移动计时 / 相位冷却 / 负面效果提前清除截止刻。
 * <p>
 * 仅在服务端主线程读写；使用 {@link ConcurrentHashMap} 兜底并发安全，
 * 玩家登出时由 {@link AkaishiMechanicalTraitEvents} 清理，避免长期钉住 UUID。
 */
final class MechanicalTraitRuntime {

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private MechanicalTraitRuntime() {
    }

    static State of(UUID playerId) {
        return STATES.computeIfAbsent(playerId, k -> new State());
    }

    /** 仅查询（不创建）：无状态返回 null，供每 tick 清理避免无谓分配。 */
    static State peek(UUID playerId) {
        return STATES.get(playerId);
    }

    static void clear(UUID playerId) {
        STATES.remove(playerId);
    }

    /** 单个玩家的瞬时状态容器。 */
    static final class State {
        /** 连击驱动：当前连击层数与上次命中刻 */
        int comboHits;
        long comboLastTick;
        /** 动能释放：上次命中刻（用于判定"冲刺首击"） */
        long lastAttackTick;
        /** 动能增幅：持续移动计时与上次累计行走距离 */
        int movingTicks;
        float lastWalkDist;
        /** 相位步频：下次可触发的游戏刻 */
        long phaseReadyTick;
        /** 净化滤芯：负面效果 → 提前移除截止刻 */
        final Map<MobEffect, Long> debuffDeadlines = new HashMap<>();
    }
}
