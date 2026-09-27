package com.example.akaishi.api.energy;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 能量类型注册表：集合模组可注册任意多种能量（赤石/魔力/奥术等）。
 * 线程安全，附属模组可通过本注册表查询或扩展能量类型。
 */
public final class EnergyTypeRegistry {

    private static final ConcurrentMap<ResourceLocation, IEnergyType> TYPES = new ConcurrentHashMap<>();

    private EnergyTypeRegistry() {
    }

    /** 注册能量类型，重复 ID 以首次注册为准 */
    public static void register(IEnergyType type) {
        TYPES.putIfAbsent(type.getId(), type);
    }

    /** 按 ID 查询能量类型，不存在返回 null */
    public static IEnergyType get(ResourceLocation id) {
        return TYPES.get(id);
    }

    /** 全部已注册能量类型 */
    public static Collection<IEnergyType> getAll() {
        return TYPES.values();
    }
}
