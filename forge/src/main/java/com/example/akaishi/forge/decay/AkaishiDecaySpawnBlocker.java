package com.example.akaishi.forge.decay;

import com.example.akaishi.decay.DecayZoneManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 衰竭区域死寂：区域内禁止生物生成。
 * - FinalizeSpawn 置 DENY（生成路径拦截，覆盖自然/刷怪笼/生成蛋）；
 * - EntityJoinLevel 兜底取消（实体加入世界前硬取消）。
 * 区域腐化转化产物（凋零骷髅/僵尸马/骷髅马/僵尸村民，几乎不会自然生成）放行以保留转化机制。
 */
public enum AkaishiDecaySpawnBlocker {
    INSTANCE;

    @SubscribeEvent
    public void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (isInDecayZone(event.getEntity())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        // 区域腐化转化产物放行（保留转化机制）
        if (mob instanceof WitherSkeleton || mob instanceof ZombieHorse
                || mob instanceof SkeletonHorse || mob instanceof ZombieVillager) {
            return;
        }
        if (isInDecayZone(mob)) {
            event.setCanceled(true);
        }
    }

    /** 位置是否处于任一衰竭区域内（同维度） */
    private static boolean isInDecayZone(Entity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return DecayZoneManager.intensityAt(serverLevel, entity.blockPosition()) > 0f;
    }
}
