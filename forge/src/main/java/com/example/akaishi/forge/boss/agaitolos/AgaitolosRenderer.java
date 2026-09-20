package com.example.akaishi.forge.boss.agaitolos;

import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * 阿盖托洛丝实体渲染器。
 * <p>
 * P1 只做最简接线：不加附加图层、不做阶段换模/换贴图、不改缩放与阴影，
 * 这些属于 P7 的渲染表现阶段（按 RULES §7 先图后码）。
 */
public class AgaitolosRenderer extends GeoEntityRenderer<AgaitolosEntity> {

    public AgaitolosRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AgaitolosModel());
    }
}
