package com.example.akaishi.api.mechanical.trait;

import net.minecraft.resources.ResourceLocation;

/**
 * 可扩展的机械材料特性契约。
 * <p>
 * 特性是「挂在机械材料上」的能力标签，与 {@code IMechanicalDnaEffect}（DNA 特殊效果）
 * 互相独立、可同时存在：DNA 效果挂在 DNA 来源上，材料特性挂在材料上。
 * <p>
 * <b>等级</b>：每个特性能被叠到多个等级，等级由「同一器官内 4 个部件中使用该材料的次数」
 * 决定（{@code clamp(次数, 1, 4)}），上限见 {@link #maxLevel()}。
 * 附属模组只需实现本接口并注册到 {@link MechanicalTraitRegistry}；
 * 若希望特性真正产生效果，还需为它实现 {@link IMechanicalTraitHandler}
 * 并注册到 {@link MechanicalTraitHandlerRegistry}（未注册处理器的特性只展示、不生效、不报错）。
 */
public interface IMechanicalTrait {

    /** 稳定的命名空间 ID，例如 {@code akaishi:self_repair}。 */
    String getId();

    /** 展示用本地化键（特性名），例如 {@code mechanical.trait.akaishi.self_repair}。 */
    String getTranslationKey();

    /**
     * 指定等级的效果描述本地化键。
     * <p>
     * 默认约定为「特性名键 + .desc + 等级」，即 {@code mechanical.trait.akaishi.self_repair.desc3}；
     * 附属模组可覆写本方法自定义命名。
     *
     * @param level 等级（1 起）
     */
    default String descriptionKey(int level) {
        return getTranslationKey() + ".desc" + level;
    }

    /** 等级上限（用户口径为四级），默认 4。 */
    default int maxLevel() {
        return 4;
    }

    /** 解析后的命名空间 ID；非法 ID 返回 null（供注册表与处理钩子索引）。 */
    default ResourceLocation id() {
        return ResourceLocation.tryParse(getId());
    }
}
