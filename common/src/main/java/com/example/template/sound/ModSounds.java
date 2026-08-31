package com.example.template.sound;

import com.example.template.TemplateMod;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.RegistrarManager;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * 模组音效注册表。音效资源位于 assets/template_mod/sounds/*.ogg（自合成），
 * 由 sounds.json 关联事件名。注册时机与方块一致：注册事件期间延迟求值。
 */
public final class ModSounds {

    private ModSounds() {
    }

    /** 反应堆燃烧运转（循环） */
    public static final RegistrySupplier<SoundEvent> REACTOR_HUM = reg("reactor_hum");
    /** 反应堆高温警告 */
    public static final RegistrySupplier<SoundEvent> REACTOR_WARN = reg("reactor_warn");
    /** 反应堆爆炸 */
    public static final RegistrySupplier<SoundEvent> REACTOR_EXPLOSION = reg("reactor_explosion");
    /** 通用机器运转（循环） */
    public static final RegistrySupplier<SoundEvent> MACHINE_HUM = reg("machine_hum");
    /** 生命活化器液体活化（循环） */
    public static final RegistrySupplier<SoundEvent> ACTIVATOR_BUBBLE = reg("activator_bubble");
    /** 多方块结构成型 */
    public static final RegistrySupplier<SoundEvent> MULTIBLOCK_ACTIVATE = reg("multiblock_activate");
    /** 衰竭区域泄漏警示 */
    public static final RegistrySupplier<SoundEvent> DECAY_LEAK = reg("decay_leak");

    /** 强制类加载：确保 SoundEvent 在注册事件前完成注册（游戏启动阶段由 TemplateMod.init 调用） */
    public static void touch() {
    }

    private static RegistrySupplier<SoundEvent> reg(String name) {
        Registrar<SoundEvent> registrar = RegistrarManager.get(TemplateMod.MOD_ID).get(Registries.SOUND_EVENT);
        ResourceLocation id = new ResourceLocation(TemplateMod.MOD_ID, name);
        return registrar.register(id, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
