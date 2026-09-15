package com.example.akaishi.entity;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

/**
 * 实体类型注册。
 * 侵入式强转统一收敛在 {@link #entity(String, Supplier)} 一处（与 ModBlockEntities 一致）。
 */
public final class ModEntities {

    /** 生命能量弹（发射器射出的可视化弹体） */
    public static RegistrySupplier<EntityType<AkaishiLifeEnergyProjectileEntity>> LIFE_ENERGY_PROJECTILE;

    private ModEntities() {
    }

    public static void register() {
        LIFE_ENERGY_PROJECTILE = entity("life_energy_projectile",
                () -> EntityType.Builder
                        .<AkaishiLifeEnergyProjectileEntity>of(AkaishiLifeEnergyProjectileEntity::new, MobCategory.MISC)
                        .sized(0.4F, 0.4F)
                        .clientTrackingRange(8)
                        .updateInterval(1)
                        .build("life_energy_projectile"));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> RegistrySupplier<EntityType<T>> entity(String id, Supplier<EntityType<T>> factory) {
        return (RegistrySupplier<EntityType<T>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.ENTITY_TYPE)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
