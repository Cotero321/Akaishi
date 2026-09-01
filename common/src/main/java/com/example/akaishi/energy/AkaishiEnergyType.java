package com.example.akaishi.energy;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.api.energy.IEnergyType;
import net.minecraft.resources.ResourceLocation;

/**
 * 赤石能量：赤石矿簇蕴含的天然能量，阶段二引入的第一种能量类型。
 * 通过燃烧赤石晶/粗制赤石块获取，供赤石提纯器等机器消耗。
 */
public final class AkaishiEnergyType implements IEnergyType {

    public static final ResourceLocation ID = new ResourceLocation(AkaishiMod.MOD_ID, "akaishi_energy");
    /** 能量条显示色（赤红） */
    public static final int COLOR = 0xFF3B30;

    public static final AkaishiEnergyType INSTANCE = new AkaishiEnergyType();

    private AkaishiEnergyType() {
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public int getBaseMax() {
        return 10000;
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
        return "energy.akaishi.akaishi";
    }
}
