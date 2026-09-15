package com.example.akaishi.forge.life;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监守者 Boss 化（Forge 服务端，事件驱动，不修改原版实体）：
 * - Boss 血条：为世界中每个监守者挂紫色 BossBar，随生命实时刷新，离开世界/死亡即移除
 * - Boss 保护：免疫击退、免疫毒/凋零/饥饿/虚弱负面效果
 * 采用事件监听而非覆写 Warden，对原版/第三方生成的监守者同样生效，兼容性最佳。
 */
public final class WardenBossHandler {

    public static final WardenBossHandler INSTANCE = new WardenBossHandler();

    /** 实体的 BossBar 跟踪表（实体数量极少，无性能顾虑） */
    private static final Map<Entity, ServerBossEvent> BARS = new ConcurrentHashMap<>();

    private WardenBossHandler() {
    }

    /** 实体进入世界：服务端为监守者创建 BossBar 并对当前玩家可见 */
    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Warden warden)) {
            return;
        }
        broadcast(warden, barFor(warden));
    }

    /**
     * 实体离开世界：无论死亡、/kill 之外的直接移除（discard）、区块卸载还是换维度，
     * 都会走到 LevelCallback#onTrackingEnd 并发出本事件，据此立即回收血条。
     * 不能只依赖 tick/死亡事件：实体被直接移除后不再 tick，死亡事件也不触发，血条会永久残留在玩家屏幕上。
     */
    @SubscribeEvent
    public void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Warden warden)) {
            return;
        }
        removeBar(warden);
    }

    /** 实体每 tick：实时刷新血条进度，新上线玩家即时可见；实体消亡即清理 */
    @SubscribeEvent
    public void onTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Warden warden)) {
            return;
        }
        if (!warden.isAlive() || warden.isRemoved()) {
            removeBar(warden);
            return;
        }
        ServerBossEvent bar = barFor(warden); // 缺失即自愈重建（区块往返后仍在场）
        float max = warden.getMaxHealth();
        bar.setProgress(max > 0 ? Math.max(0.0F, warden.getHealth() / max) : 0.0F);
        broadcast(warden, bar);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Warden warden) {
            removeBar(warden);
        }
    }

    /** Boss 保护 1/2：免疫击退（Boss 应岿然不动） */
    @SubscribeEvent
    public void onKnockBackImmune(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Warden) {
            event.setCanceled(true);
        }
    }

    /** Boss 保护 2/2：免疫毒/凋零/饥饿/虚弱（不受负面药剂与效果干扰） */
    @SubscribeEvent
    public void onEffectImmune(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Warden)) {
            return;
        }
        MobEffect effect = event.getEffectInstance().getEffect();
        if (effect == MobEffects.POISON || effect == MobEffects.WITHER
                || effect == MobEffects.HUNGER || effect == MobEffects.WEAKNESS) {
            event.setCanceled(true);
        }
    }

    /** 取该监守者的血条，缺失则新建（重复创建由并发表保证等价于一次） */
    private static ServerBossEvent barFor(Warden warden) {
        return BARS.computeIfAbsent(warden, w -> {
            ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.minecraft.warden"),
                    BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            bar.setVisible(true);
            return bar;
        });
    }

    /** 让该监守者所在维度的全部玩家可见血条（addPlayer 内部去重，重复调用无副作用） */
    private static void broadcast(Warden warden, ServerBossEvent bar) {
        if (warden.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                bar.addPlayer(player);
            }
        }
    }

    private static void removeBar(Warden warden) {
        ServerBossEvent bar = BARS.remove(warden);
        if (bar != null) {
            bar.removeAllPlayers();
            bar.setVisible(false);
        }
    }
}
