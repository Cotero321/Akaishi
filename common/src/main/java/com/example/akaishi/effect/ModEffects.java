package com.example.akaishi.effect;

import com.example.akaishi.AkaishiMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

/**
 * 模组自定义状态效果注册表。
 * 目前注册"衰变"（衰竭区域减益）、"不可名状"（黑山羊母神仪式减益）
 * 与"凋亡"（阿盖托洛丝阶段三替换凋零的上位减益）。
 * <p>理智恢复效果（{@code akaishi:sanity_restore}）随理智系统迁往 {@code akaishi_forbidden}，
 * 由该模块自行注册（id 不变）。
 */
public final class ModEffects {

    /** 衰变：衰竭区域内每秒造成 2×(等级+1) 点魔法伤害（亡灵免疫） */
    public static RegistrySupplier<MobEffect> DECAY;

    /** 不可名状：黑山羊母神仪式进行中与完成瞬间施加，仅驱动客户端画面/音效表现 */
    public static RegistrySupplier<MobEffect> UNNAMEABLE;

    /**
     * 凋亡：阿盖托洛丝阶段三的减益（周期掉血 + 降低治疗）。
     * <p>施加侧唯一入口见 {@code com.example.akaishi.boss.agaitolos.AgaitolosDoom}
     * （阶段一/二仍施加原版凋零，阶段三改施加本效果）。
     */
    public static RegistrySupplier<MobEffect> DOOM;

    private ModEffects() {
    }

    /** 由 {@link AkaishiMod#init()} 在 mod 事件总线内调用（注册表冻结前） */
    public static void register() {
        Registrar<MobEffect> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.MOB_EFFECT);
        DECAY = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "decay"), DecayEffect::new);
        UNNAMEABLE = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "unnameable"), UnnameableEffect::new);
        DOOM = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "doom"), DoomEffect::new);
    }
}
