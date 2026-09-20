package com.example.akaishi.entity;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import com.example.akaishi.boss.agaitolos.entity.AgaitolosWitherSkull;
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

    /** 阿盖托洛丝【下界本源】BOSS 实体；碰撞箱 0.9×2.2，依据见 register() 内注释 */
    public static RegistrySupplier<EntityType<AgaitolosEntity>> AGAITOLOS;

    /** 阿盖托洛丝的远程弹体：凋零头颅（继承原版 WitherSkull，只替换命中结算） */
    public static RegistrySupplier<EntityType<AgaitolosWitherSkull>> AGAITOLOS_WITHER_SKULL;

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

        AGAITOLOS = entity("agaitolos",
                () -> EntityType.Builder
                        .<AgaitolosEntity>of(AgaitolosEntity::new, MobCategory.MONSTER)
                        // 碰撞箱按当前一阶段模型实测值定（设计文档 §6.2b 的 1.6×2.9 依据的是已被替换的旧 v3 模型「本体 3.44 格」，已作废）：
                        //   实测躯干核心（躯干+腿+裙甲+双臂）宽 0.78、最高点（冠饰）y=2.07、脚底 y=0.14。
                        //   取 0.9 宽：覆盖整个本体并留少量余量；两侧浮空恶魔头（x±0.90）与翅膀（翼展 2.54）
                        //   是悬浮装饰件，不算本体，故意留在盒外，避免玩家在身侧空气里打中它。
                        //   取 2.2 高：覆盖脚底到冠饰顶并留余量；镰刀举起时到 2.35，但手持武器不参与碰撞箱。
                        .sized(0.9F, 2.2F)
                        // 大型 BOSS：加大追踪半径、降低位置同步间隔，避免高机动时画面抖动
                        .clientTrackingRange(12)
                        .updateInterval(3)
                        .build("agaitolos"));

        AGAITOLOS_WITHER_SKULL = entity("agaitolos_wither_skull",
                () -> EntityType.Builder
                        .<AgaitolosWitherSkull>of(AgaitolosWitherSkull::new, MobCategory.MISC)
                        // 尺寸与追踪参数逐项对齐原版 EntityType.WITHER_SKULL（5/16 格、范围 4、间隔 10）
                        .sized(0.3125F, 0.3125F)
                        .clientTrackingRange(4)
                        .updateInterval(10)
                        .build("agaitolos_wither_skull"));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> RegistrySupplier<EntityType<T>> entity(String id, Supplier<EntityType<T>> factory) {
        return (RegistrySupplier<EntityType<T>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.ENTITY_TYPE)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
