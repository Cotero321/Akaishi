package com.example.akaishi.forge.boss.agaitolos;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 阿盖托洛丝阶段一 Geo 模型（几何 / 贴图 / 动画三资源定位）。
 * <p>
 * GeckoLib 的客户端渲染类只能放 forge 模块：common 是纯 Architectury 编译面，
 * 只通过 modCompileOnly 取接口 API，不承担渲染实现。
 */
public class AgaitolosModel extends GeoModel<AgaitolosEntity> {

    private static final ResourceLocation MODEL = new ResourceLocation(AkaishiMod.MOD_ID, "geo/entity/agaitolos_stage1.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(AkaishiMod.MOD_ID, "textures/entity/agaitolos_stage1.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(AkaishiMod.MOD_ID, "animations/entity/agaitolos_stage1.animation.json");

    @Override
    public ResourceLocation getModelResource(AgaitolosEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(AgaitolosEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(AgaitolosEntity animatable) {
        return ANIMATION;
    }
}
