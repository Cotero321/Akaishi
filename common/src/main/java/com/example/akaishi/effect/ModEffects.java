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
 * 目前仅注册"衰变"效果（衰竭区域减益）。
 */
public final class ModEffects {

    /** 衰变：衰竭区域内每秒造成 2×(等级+1) 点魔法伤害（亡灵免疫） */
    public static RegistrySupplier<MobEffect> DECAY;

    private ModEffects() {
    }

    /** 由 {@link AkaishiMod#init()} 在 mod 事件总线内调用（注册表冻结前） */
    public static void register() {
        Registrar<MobEffect> registrar = RegistrarManager.get(AkaishiMod.MOD_ID).get(Registries.MOB_EFFECT);
        DECAY = registrar.register(new ResourceLocation(AkaishiMod.MOD_ID, "decay"), DecayEffect::new);
    }
}
