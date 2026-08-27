package com.example.template.energy;

import com.example.template.TemplateMod;
import com.example.template.api.energy.IEnergyType;
import net.minecraft.resources.ResourceLocation;

/**
 * 生命能量：由生命聚合转换器消耗赤能源聚合而成的高级能量类型。
 * 通过生命能量管道单独传输，供生命体系设备使用。
 */
public final class LifeEnergyType implements IEnergyType {

    public static final ResourceLocation ID = new ResourceLocation(TemplateMod.MOD_ID, "life_energy");
    /** 能量条显示色（生命绿） */
    public static final int COLOR = 0x3BFF3B;

    public static final LifeEnergyType INSTANCE = new LifeEnergyType();

    private LifeEnergyType() {
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public int getBaseMax() {
        return 1000;
    }

    @Override
    public float getRegenPerSecond() {
        return 0F;
    }

    @Override
    public int getColor() {
        return COLOR;
    }

    @Override
    public String getTranslationKey() {
        return "energy.template_mod.life";
    }
}
