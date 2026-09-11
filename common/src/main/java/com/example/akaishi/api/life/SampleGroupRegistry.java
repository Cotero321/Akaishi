package com.example.akaishi.api.life;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 生命样本分组注册表，供主模组和附属模组共享。 */
public final class SampleGroupRegistry {
    private static final ConcurrentMap<ResourceLocation, ISampleGroup> GROUPS = new ConcurrentHashMap<>();

    private SampleGroupRegistry() {
    }

    public static void register(ISampleGroup group) {
        validate(group);
        if (GROUPS.putIfAbsent(idOf(group), group) != null) {
            throw new IllegalArgumentException("Sample group already registered: " + group.getId());
        }
    }

    public static void override(ISampleGroup group) {
        validate(group);
        GROUPS.put(idOf(group), group);
    }

    public static ISampleGroup get(ResourceLocation id) {
        return id == null ? null : GROUPS.get(id);
    }

    public static ISampleGroup get(String id) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(id);
        return resourceLocation == null ? null : get(resourceLocation);
    }

    public static ISampleGroup unregister(ResourceLocation id) {
        return id == null ? null : GROUPS.remove(id);
    }

    public static Collection<ISampleGroup> getAll() {
        return List.copyOf(GROUPS.values());
    }

    /** 按注册顺序无法保证时，调用方应通过内置优先级先行匹配。 */
    public static ISampleGroup find(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        for (ISampleGroup group : GROUPS.values()) {
            if (group.matcher() != null && group.matcher().test(entity)) {
                return group;
            }
        }
        return null;
    }

    private static ResourceLocation idOf(ISampleGroup group) {
        return ResourceLocation.tryParse(group.getId());
    }

    private static void validate(ISampleGroup group) {
        if (group == null || group.getId() == null || idOf(group) == null) {
            throw new IllegalArgumentException("Sample group ID must be a valid namespaced resource location");
        }
        if (group.matcher() == null) {
            throw new IllegalArgumentException("Sample group matcher must not be null: " + group.getId());
        }
    }
}
