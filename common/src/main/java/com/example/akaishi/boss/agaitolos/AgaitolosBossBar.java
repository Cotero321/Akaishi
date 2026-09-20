package com.example.akaishi.boss.agaitolos;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;

/**
 * 阿盖托洛丝的血条（独立成类：实体只做编排，进度刷新与参与者对账都收在这里）。
 * <p>
 * 参与者不由实体逐个 startSeenByPlayer/stopSeenByPlayer 维护，而是按固定周期主动对账：
 * 只给"客户端追踪半径内"的玩家显示，玩家看不到 BOSS 时也看不到血条，
 * 避免隔着半张地图一直挂着一条血条。
 */
public final class AgaitolosBossBar {

    /**
     * 参与者对账周期：20 tick（1 秒）。
     * 血条参与者的变化不需要逐 tick 精度，把遍历玩家列表摊到每秒一次就够，省掉每 tick 的 O(玩家数) 扫描。
     */
    private static final int RECONCILE_INTERVAL_TICKS = 20;

    private final ServerBossEvent event = new ServerBossEvent(
            Component.translatable("entity.akaishi.agaitolos"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);

    /** 距离下次参与者对账还剩多少 tick */
    private int reconcileCooldown;

    /** 是否仍然有效；remove() 后置 false，避免 die() 与 remove() 双调用重复回收 */
    private boolean active = true;

    /** 每个服务端 tick 由实体调用一次：刷新进度 + 按周期对账参与者 */
    public void tick(AgaitolosEntity boss) {
        if (!this.active) {
            return;
        }
        // 分母防零：属性尚未注册完成时 getMaxHealth() 可能为 0
        this.event.setProgress(Mth.clamp(boss.getHealth() / Math.max(1.0F, boss.getMaxHealth()), 0.0F, 1.0F));
        if (--this.reconcileCooldown > 0) {
            return;
        }
        this.reconcileCooldown = RECONCILE_INTERVAL_TICKS;
        this.reconcile(boss);
    }

    /**
     * 回收血条。死亡与实体移除（区块卸载 / 清场）都会走这里 —— 后者不会再 tick，
     * 不在这里摘掉的话血条会永久留在玩家屏幕上。
     */
    public void remove() {
        if (!this.active) {
            return;
        }
        this.active = false;
        this.event.removeAllPlayers();
        this.event.setVisible(false);
    }

    /** 参与者对账：只保留客户端追踪半径内的玩家 */
    private void reconcile(AgaitolosEntity boss) {
        // 用实体自身的 clientTrackingRange（区块）换算成格：显示范围与"玩家能否看见 BOSS"一致
        double range = boss.getType().clientTrackingRange() * 16.0D;
        double rangeSqr = range * range;
        for (Player player : boss.level().players()) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                continue;
            }
            boolean inRange = serverPlayer.distanceToSqr(boss) <= rangeSqr;
            boolean shown = this.event.getPlayers().contains(serverPlayer);
            if (inRange && !shown) {
                this.event.addPlayer(serverPlayer);
            } else if (!inRange && shown) {
                this.event.removePlayer(serverPlayer);
            }
        }
    }
}
