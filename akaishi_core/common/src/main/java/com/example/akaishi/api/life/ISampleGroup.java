package com.example.akaishi.api.life;

import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

/** 可扩展生命样本分组契约。 */
public interface ISampleGroup {
    String getId();
    int getBasePurity();
    int getBaseCollectRate();
    int getCompatMin();
    int getCompatMax();
    double getRejectionFactor();
    String getNameKey();
    int getQualityTier();
    Predicate<LivingEntity> matcher();
}
