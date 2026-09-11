package com.example.akaishi.api.mechanical;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 机械 DNA 效果注册表。 */
public final class MechanicalEffectRegistry {

    /** 「无效果」的规范 ID；注册表以该 ID 表示无效果，反序列化时应跳过。 */
    public static final String NONE_ID = "akaishi:none";

    private static final ConcurrentMap<ResourceLocation, IMechanicalDnaEffect> EFFECTS = new ConcurrentHashMap<>();

    private MechanicalEffectRegistry() {
    }

    /** 判断效果是否为「无效果」（null 或规范 NONE）。 */
    public static boolean isNone(IMechanicalDnaEffect effect) {
        return effect == null || NONE_ID.equals(effect.getId());
    }

    public static void register(IMechanicalDnaEffect effect) {
        validate(effect);
        if (EFFECTS.putIfAbsent(idOf(effect), effect) != null) {
            throw new IllegalArgumentException("Mechanical DNA effect already registered: " + effect.getId());
        }
    }

    public static void override(IMechanicalDnaEffect effect) {
        validate(effect);
        EFFECTS.put(idOf(effect), effect);
    }

    public static IMechanicalDnaEffect get(ResourceLocation id) {
        return id == null ? null : EFFECTS.get(id);
    }

    public static IMechanicalDnaEffect get(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed == null ? null : get(parsed);
    }

    public static IMechanicalDnaEffect unregister(ResourceLocation id) {
        return id == null ? null : EFFECTS.remove(id);
    }

    public static Collection<IMechanicalDnaEffect> getAll() {
        return List.copyOf(EFFECTS.values());
    }

    /**
     * 宽松解析效果：完整 ID 精确命中 → {@code akaishi:} 命名空间回退（兼容内置模板与裸 path），
     * 未知返回 null。供 NBT 反序列化与附属模组按 ID 查询使用。
     */
    public static IMechanicalDnaEffect resolve(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String trimmed = rawId.trim();
        ResourceLocation direct = ResourceLocation.tryParse(trimmed);
        if (direct != null) {
            IMechanicalDnaEffect effect = EFFECTS.get(direct);
            if (effect != null) {
                return effect;
            }
        }
        // 裸 path 或第三方命名空间：回退到内置 akaishi 同名效果
        int colon = trimmed.indexOf(':');
        String path = colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
        ResourceLocation fallback = ResourceLocation.tryParse("akaishi:" + path.toLowerCase(Locale.ROOT));
        return fallback == null ? null : EFFECTS.get(fallback);
    }

    private static ResourceLocation idOf(IMechanicalDnaEffect effect) {
        return ResourceLocation.tryParse(effect.getId());
    }

    private static void validate(IMechanicalDnaEffect effect) {
        if (effect == null || effect.getId() == null || idOf(effect) == null) {
            throw new IllegalArgumentException("Mechanical DNA effect ID must be a valid namespaced resource location");
        }
        if (effect.getTranslationKey() == null || effect.getTranslationKey().isBlank()) {
            throw new IllegalArgumentException("Mechanical DNA effect translation key must not be blank: " + effect.getId());
        }
    }
}
