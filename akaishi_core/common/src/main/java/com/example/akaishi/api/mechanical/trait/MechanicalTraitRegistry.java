package com.example.akaishi.api.mechanical.trait;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 机械材料特性注册表。
 * <p>
 * 附属模组可在自己的初始化阶段调用 {@link #register} 新增特性，
 * 并按 ID 查询以便在 tooltip / 界面上展示。
 * 注册表线程安全；{@link #override} 与 {@link #unregister} 供开发期替换/调试。
 */
public final class MechanicalTraitRegistry {

    private static final ConcurrentMap<ResourceLocation, IMechanicalTrait> TRAITS = new ConcurrentHashMap<>();

    private MechanicalTraitRegistry() {
    }

    /** 注册一种特性；同一 ID 重复注册抛异常。 */
    public static void register(IMechanicalTrait trait) {
        validate(trait);
        if (TRAITS.putIfAbsent(idOf(trait), trait) != null) {
            throw new IllegalArgumentException("Mechanical trait already registered: " + trait.getId());
        }
    }

    /** 覆盖已注册（或新增）特性，供开发期替换使用。 */
    public static void override(IMechanicalTrait trait) {
        validate(trait);
        TRAITS.put(idOf(trait), trait);
    }

    /** 注销特性，返回被移除项；不存在返回 null。 */
    public static IMechanicalTrait unregister(ResourceLocation id) {
        return id == null ? null : TRAITS.remove(id);
    }

    public static IMechanicalTrait get(ResourceLocation id) {
        return id == null ? null : TRAITS.get(id);
    }

    /** 按字符串 ID 取特性（非法 ID 返回 null）。 */
    public static IMechanicalTrait get(String id) {
        ResourceLocation parsed = id == null ? null : ResourceLocation.tryParse(id);
        return parsed == null ? null : get(parsed);
    }

    /** 获取所有已注册特性的快照。 */
    public static Collection<IMechanicalTrait> getAll() {
        return List.copyOf(TRAITS.values());
    }

    /** 是否没有任何已注册特性（供处理器快速跳过）。 */
    public static boolean isEmpty() {
        return TRAITS.isEmpty();
    }

    /**
     * 宽松解析特性：完整 ID 精确命中 → {@code akaishi:} 命名空间回退（兼容裸 path），
     * 未知返回 null。供序列化与附属模组按 ID 查询使用。
     */
    public static IMechanicalTrait resolve(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String trimmed = rawId.trim();
        ResourceLocation direct = ResourceLocation.tryParse(trimmed);
        if (direct != null) {
            IMechanicalTrait trait = TRAITS.get(direct);
            if (trait != null) {
                return trait;
            }
        }
        // 裸 path 或第三方命名空间：回退到内置 akaishi 同名特性
        int colon = trimmed.indexOf(':');
        String path = colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
        ResourceLocation fallback = ResourceLocation.tryParse("akaishi:" + path.toLowerCase(Locale.ROOT));
        return fallback == null ? null : TRAITS.get(fallback);
    }

    private static ResourceLocation idOf(IMechanicalTrait trait) {
        return ResourceLocation.tryParse(trait.getId());
    }

    private static void validate(IMechanicalTrait trait) {
        if (trait == null || trait.getId() == null || idOf(trait) == null) {
            throw new IllegalArgumentException("Mechanical trait ID must be a valid namespaced resource location");
        }
        if (trait.getTranslationKey() == null || trait.getTranslationKey().isBlank()) {
            throw new IllegalArgumentException("Mechanical trait translation key must not be blank: " + trait.getId());
        }
        if (trait.maxLevel() < 1) {
            throw new IllegalArgumentException("Mechanical trait maxLevel must be >= 1: " + trait.getId());
        }
    }
}
