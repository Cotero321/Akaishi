package com.example.template.api.energy;

import net.minecraft.resources.ResourceLocation;

/**
 * 能量类型定义：集合模组中每种能量（魔力/奥术/龙息等）为一个类型。
 * 附属模组可实现或直接使用默认实现注册新能量。
 */
public interface IEnergyType {
    /** 能量唯一 ID，如 arcane、dragon 等 */
    ResourceLocation getId();

    /** 基础上限（玩家可通过道具提升，存于玩家数据中） */
    int getBaseMax();

    /** 每秒自然回复量 */
    float getRegenPerSecond();

    /** HUD 显示颜色（RGB） */
    int getColor();

    /** 显示名称的翻译键 */
    String getTranslationKey();
}
