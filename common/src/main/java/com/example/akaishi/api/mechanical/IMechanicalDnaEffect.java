package com.example.akaishi.api.mechanical;

import net.minecraft.resources.ResourceLocation;

/** 可扩展机械 DNA 特殊效果契约。 */
public interface IMechanicalDnaEffect {
    /** 稳定的命名空间 ID，例如 akaishi:critical_boost。 */
    String getId();

    /** 展示用本地化键。 */
    String getTranslationKey();

    /** 解析后的命名空间 ID；非法 ID 返回 null（供注册表与处理钩子索引）。 */
    default ResourceLocation id() {
        return ResourceLocation.tryParse(getId());
    }
}
