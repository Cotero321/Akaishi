package com.example.akaishi.api.miniature;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微缩终端适配器注册表（类型 id → 适配器）。
 * <p>
 * 通用层只经此表按 NBT 里的类型 id 取适配器，不认识任何具体终端族；
 * 各族在 {@code register()} 阶段自注册（与项目其它注册表同范式）。
 * <p>
 * <b>只对终端开放</b>：非终端机器不注册，微缩机制自然对其无效。
 */
public final class MiniatureTerminalRegistry {

    /** NBT 键：微缩方块内的终端类型 id */
    public static final String TAG_TYPE = "TerminalType";
    /** NBT 键：微缩方块内的终端唯一 ID（端口/无线设备寻址用） */
    public static final String TAG_TERMINAL_ID = "TerminalId";
    /** NBT 键：终端族自有数据（各族自解释） */
    public static final String TAG_PAYLOAD = "Payload";

    private static final Map<ResourceLocation, MiniatureTerminalAdapter> ADAPTERS = new ConcurrentHashMap<>();

    private MiniatureTerminalRegistry() {
    }

    /** 注册一支适配器（同 id 重复注册直接覆盖，便于开发期热改） */
    public static void register(MiniatureTerminalAdapter adapter) {
        if (adapter != null && adapter.typeId() != null) {
            ADAPTERS.put(adapter.typeId(), adapter);
        }
    }

    /** 按类型 id 取适配器（未注册返回 null） */
    public static MiniatureTerminalAdapter adapter(ResourceLocation typeId) {
        return typeId == null ? null : ADAPTERS.get(typeId);
    }

    /** 按存盘数据取适配器（脏数据 / 未注册族返回 null） */
    public static MiniatureTerminalAdapter adapterOf(CompoundTag tag) {
        if (tag == null || !tag.contains(TAG_TYPE)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(TAG_TYPE));
        return adapter(id);
    }
}
