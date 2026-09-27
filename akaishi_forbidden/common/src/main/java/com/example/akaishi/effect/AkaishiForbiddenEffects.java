package com.example.akaishi.effect;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/**
 * 禁忌模块·状态效果注册（P3b 随理智系统从本体 {@code ModEffects} 迁入）。
 *
 * <p>「理智恢复」是理智回复药水的载体效果，随理智系统归本模块；
 * 注册 id 与迁前逐字一致：{@code akaishi:sanity_restore}（命名空间仍为三模块共用的 {@code akaishi:}）。
 * 其余效果（衰变 / 不可名状 / 凋亡）留在本体 {@code ModEffects}。
 */
public final class AkaishiForbiddenEffects {

    /**
     * 理智恢复：酿造药水的载体效果（I 级共回 7 SAN / II 级共回 10 SAN，在 5s 窗口内逐 tick 分摊）。
     * <p>施加侧唯一入口是原版酿造链路（药水 id 见 {@code SanityRestorePotions}），
     * 数值与分摊口径全在 {@link SanityRestoreEffect}。
     */
    public static RegistrySupplier<MobEffect> SANITY_RESTORE;

    private AkaishiForbiddenEffects() {
    }

    /** 由 {@code AkaishiForbiddenMod.init()} 调用（注册表冻结前） */
    public static void register() {
        Registrar<MobEffect> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.MOB_EFFECT);
        SANITY_RESTORE = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "sanity_restore"),
                SanityRestoreEffect::new);
    }
}
