package com.example.akaishi.api.mechanical;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 机械 DNA 效果执行钩子注册表：把「效果 ID」映射到「运行时逻辑」。 */
public final class MechanicalEffectHandlerRegistry {

    private static final ConcurrentMap<ResourceLocation, IMechanicalDnaEffectHandler> HANDLERS = new ConcurrentHashMap<>();

    private MechanicalEffectHandlerRegistry() {
    }

    /** 绑定效果 ID 与执行钩子；同一 ID 重复注册抛异常。 */
    public static void register(String effectId, IMechanicalDnaEffectHandler handler) {
        ResourceLocation id = parseId(effectId);
        if (HANDLERS.putIfAbsent(id, requireHandler(handler)) != null) {
            throw new IllegalArgumentException("Mechanical DNA effect handler already registered: " + effectId);
        }
    }

    /** 绑定/覆盖效果 ID 的执行钩子（开发期替换用）。 */
    public static void override(String effectId, IMechanicalDnaEffectHandler handler) {
        HANDLERS.put(parseId(effectId), requireHandler(handler));
    }

    /** 注销执行钩子，返回被移除的钩子；不存在返回 null。 */
    public static IMechanicalDnaEffectHandler unregister(ResourceLocation effectId) {
        return effectId == null ? null : HANDLERS.remove(effectId);
    }

    public static IMechanicalDnaEffectHandler get(ResourceLocation effectId) {
        return effectId == null ? null : HANDLERS.get(effectId);
    }

    /** 按字符串 ID 取钩子（非法 ID 返回 null）。 */
    public static IMechanicalDnaEffectHandler get(String effectId) {
        return effectId == null ? null : get(ResourceLocation.tryParse(effectId));
    }

    /** 按效果实例取钩子（效果非法或无钩子返回 null）。 */
    public static IMechanicalDnaEffectHandler get(IMechanicalDnaEffect effect) {
        if (effect == null) {
            return null;
        }
        IMechanicalDnaEffectHandler handler = get(effect.id());
        return handler != null ? handler : get(effect.getId());
    }

    public static Collection<IMechanicalDnaEffectHandler> getAll() {
        return List.copyOf(HANDLERS.values());
    }

    /** 是否没有任何已注册钩子（供主处理器快速跳过分发）。 */
    public static boolean isEmpty() {
        return HANDLERS.isEmpty();
    }

    private static ResourceLocation parseId(String effectId) {
        ResourceLocation id = effectId == null ? null : ResourceLocation.tryParse(effectId);
        if (id == null) {
            throw new IllegalArgumentException("Mechanical DNA effect handler ID must be a valid resource location");
        }
        return id;
    }

    private static IMechanicalDnaEffectHandler requireHandler(IMechanicalDnaEffectHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Mechanical DNA effect handler must not be null");
        }
        return handler;
    }
}
