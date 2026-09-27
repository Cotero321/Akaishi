package com.example.akaishi.api.mechanical.trait;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 机械材料特性执行钩子注册表：把「特性 ID」映射到「运行时逻辑」。 */
public final class MechanicalTraitHandlerRegistry {

    private static final ConcurrentMap<ResourceLocation, IMechanicalTraitHandler> HANDLERS = new ConcurrentHashMap<>();

    private MechanicalTraitHandlerRegistry() {
    }

    /** 绑定特性 ID 与执行钩子；同一 ID 重复注册抛异常。 */
    public static void register(String traitId, IMechanicalTraitHandler handler) {
        ResourceLocation id = parseId(traitId);
        if (HANDLERS.putIfAbsent(id, requireHandler(handler)) != null) {
            throw new IllegalArgumentException("Mechanical trait handler already registered: " + traitId);
        }
    }

    /** 绑定/覆盖特性 ID 的执行钩子（开发期替换用）。 */
    public static void override(String traitId, IMechanicalTraitHandler handler) {
        HANDLERS.put(parseId(traitId), requireHandler(handler));
    }

    /** 注销执行钩子，返回被移除的钩子；不存在返回 null。 */
    public static IMechanicalTraitHandler unregister(ResourceLocation traitId) {
        return traitId == null ? null : HANDLERS.remove(traitId);
    }

    public static IMechanicalTraitHandler get(ResourceLocation traitId) {
        return traitId == null ? null : HANDLERS.get(traitId);
    }

    /** 按字符串 ID 取钩子（非法 ID 返回 null）。 */
    public static IMechanicalTraitHandler get(String traitId) {
        return traitId == null ? null : get(ResourceLocation.tryParse(traitId));
    }

    /** 按特性实例取钩子（特性非法或无钩子返回 null）。 */
    public static IMechanicalTraitHandler get(IMechanicalTrait trait) {
        if (trait == null) {
            return null;
        }
        IMechanicalTraitHandler handler = get(trait.id());
        return handler != null ? handler : get(trait.getId());
    }

    public static Collection<IMechanicalTraitHandler> getAll() {
        return List.copyOf(HANDLERS.values());
    }

    /** 是否没有任何已注册钩子（供主处理器快速跳过分发）。 */
    public static boolean isEmpty() {
        return HANDLERS.isEmpty();
    }

    private static ResourceLocation parseId(String traitId) {
        ResourceLocation id = traitId == null ? null : ResourceLocation.tryParse(traitId);
        if (id == null) {
            throw new IllegalArgumentException("Mechanical trait handler ID must be a valid resource location");
        }
        return id;
    }

    private static IMechanicalTraitHandler requireHandler(IMechanicalTraitHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Mechanical trait handler must not be null");
        }
        return handler;
    }
}
