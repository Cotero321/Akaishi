package com.example.akaishi.entity;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.sanity.shadow.ShadowBolt;
import com.example.akaishi.sanity.shadow.ShadowEntity;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

/**
 * 禁忌模块·实体类型注册（P3b 随理智系统从本体迁入，注册 id 与迁前逐字一致）。
 *
 * <p>影怪与精神弹属理智系统，故归本模块；未安装本模块时二者根本不注册。
 * 侵入式强转仍收敛在 {@link #entity(String, Supplier)} 一处（与本体 ModEntities 同范式）。
 */
public final class AkaishiForbiddenEntities {

    /**
     * 影怪（低理智幻影）。
     * <p><b>MobCategory 取 MISC 的理由</b>：影怪由理智系统自建生成器投放，<b>不参与原版刷怪体系</b> ——
     * MISC 的数量上限是 -1（不占 mobcap）、不参与自然刷怪与难度判定；而 MONSTER 会把它登记成"怪物"，
     * 让 {@code /kill @e[type=monster]} 之类的原版选择器与刷怪统计把它算进去。
     * 真正的隔离来自"它不继承 {@code Mob}"（见 ShadowEntity 类注释）：衰减区的转化与死寂禁刷都以
     * {@code instanceof Mob} 为门，因此两条都不作用到影怪身上。
     */
    public static RegistrySupplier<EntityType<ShadowEntity>> SHADOW;

    /** 影怪的远程弹体：精神弹（继承原版 WitherSkull，只结算一发精神伤害；渲染复用原版凋零头渲染器） */
    public static RegistrySupplier<EntityType<ShadowBolt>> SHADOW_BOLT;

    private AkaishiForbiddenEntities() {
    }

    public static void register() {
        SHADOW = entity("shadow",
                () -> EntityType.Builder
                        .<ShadowEntity>of(ShadowEntity::new, MobCategory.MISC)
                        // 碰撞箱按模型稿实测值定（geo 单位 → 格，1u = 1/16 格）：
                        //   躯干/头/双臂的极值为 x∈[-5.65, 5.65]、y∈[0.8, 28.8]、z∈[-2.1, 2.1]，
                        //   即 0.71 宽 × 1.75 高 × 0.26 深，脚底离地 0.05 格（是一张"薄片人影"）。
                        //   取 0.7 × 1.8：与实测同量级并留少量余量；深度不参与原版碰撞箱（宽/高二值）。
                        //   模型若有改动，这里与 ShadowEntity 的近战距离常量要一起看。
                        .sized(0.7F, 1.8F)
                        // 影怪会瞬移：追踪范围略大、同步间隔取 2，避免瞬移后位姿抖动
                        .clientTrackingRange(10)
                        .updateInterval(2)
                        .build("shadow"));

        SHADOW_BOLT = entity("shadow_bolt",
                () -> EntityType.Builder
                        .<ShadowBolt>of(ShadowBolt::new, MobCategory.MISC)
                        // 与凋零头同尺寸，直接复用原版 WitherSkullRenderer 的模型与贴图
                        .sized(0.3125F, 0.3125F)
                        .clientTrackingRange(8)
                        .updateInterval(2)
                        .build("shadow_bolt"));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Entity> RegistrySupplier<EntityType<T>> entity(String id, Supplier<EntityType<T>> factory) {
        return (RegistrySupplier<EntityType<T>>) (Object) RegistrarManager
                .get(AkaishiMod.MOD_ID).get(Registries.ENTITY_TYPE)
                .register(new ResourceLocation(AkaishiMod.MOD_ID, id), factory);
    }
}
