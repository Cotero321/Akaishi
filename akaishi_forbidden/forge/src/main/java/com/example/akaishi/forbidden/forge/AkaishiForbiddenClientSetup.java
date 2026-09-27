package com.example.akaishi.forbidden.forge;

import com.example.akaishi.api.hud.AkaishiHudRegistry;
import com.example.akaishi.entity.AkaishiForbiddenEntities;
import com.example.akaishi.forge.client.AkaishiDeepSeaFogHandler;
import com.example.akaishi.forge.client.AkaishiSanityVisionPostHandler;
import com.example.akaishi.forge.client.hud.AkaishiSanityHudElement;
import com.example.akaishi.forge.sanity.shadow.ShadowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraftforge.common.MinecraftForge;

/**
 * 禁忌模块的客户端专属注册（P3b 随理智系统从本体 {@code AkaishiModForge} 迁入）。
 *
 * <p>单独成类是为了不在服务端加载任何客户端类型；由平台入口用
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 延时求值。
 */
public final class AkaishiForbiddenClientSetup {

    private AkaishiForbiddenClientSetup() {
    }

    /** 客户端注册：影怪渲染器 + 理智 HUD 元素 + 深海视野锁定 + 低理智视野后处理 */
    public static void register() {
        // 影怪：GeckoLib 几何动画渲染 + 眼睛自发光图层（图层在渲染器构造器内注册）
        EntityRenderers.register(AkaishiForbiddenEntities.SHADOW.get(), ShadowRenderer::new);
        // 影怪的精神弹：复用原版凋零头渲染器（零新增贴图，见 ShadowBolt 类注释）
        EntityRenderers.register(AkaishiForbiddenEntities.SHADOW_BOLT.get(), WitherSkullRenderer::new);
        // 统一 HUD 渲染层的元素（理智条）：登记到 api.hud 注册表，由本体 AkaishiHudLayer 统一解算与绘制
        AkaishiHudRegistry.register(new AkaishiSanityHudElement());
        // 深海视野锁定：主世界海洋群系 Y<0 时大幅收拢雾距并染深水色
        MinecraftForge.EVENT_BUS.register(AkaishiDeepSeaFogHandler.INSTANCE);
        // 低理智视野后处理：灰化 / 血丝 / 黑白（与不可名状后处理互斥，后者优先）
        MinecraftForge.EVENT_BUS.register(AkaishiSanityVisionPostHandler.INSTANCE);
    }
}
