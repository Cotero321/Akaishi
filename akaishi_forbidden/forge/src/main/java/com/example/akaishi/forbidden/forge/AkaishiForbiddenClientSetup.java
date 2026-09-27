package com.example.akaishi.forbidden.forge;

import com.example.akaishi.api.hud.AkaishiHudRegistry;
import com.example.akaishi.block.entity.AkaishiForbiddenBlockEntities;
import com.example.akaishi.entity.AkaishiForbiddenEntities;
import com.example.akaishi.forge.boss.agaitolos.AgaitolosBossBarOverlay;
import com.example.akaishi.forge.boss.agaitolos.AgaitolosMusicHandler;
import com.example.akaishi.forge.boss.agaitolos.AgaitolosRenderer;
import com.example.akaishi.forge.client.AkaishiDeepSeaFogHandler;
import com.example.akaishi.forge.client.AkaishiSanityVisionPostHandler;
import com.example.akaishi.forge.client.AkaishiUnnameableHandler;
import com.example.akaishi.forge.client.AkaishiUnnameableOverlay;
import com.example.akaishi.forge.client.AkaishiUnnameablePostHandler;
import com.example.akaishi.forge.client.MotherAltarRenderer;
import com.example.akaishi.forge.client.hud.AkaishiSanityHudElement;
import com.example.akaishi.forge.sanity.shadow.ShadowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

/**
 * 禁忌模块的客户端专属注册（P3b 随理智系统、P3c 随 BOSS 从本体 {@code AkaishiModForge} 迁入）。
 *
 * <p>单独成类是为了不在服务端加载任何客户端类型；由平台入口用
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 延时求值。
 */
public final class AkaishiForbiddenClientSetup {

    private AkaishiForbiddenClientSetup() {
    }

    /** 客户端注册：影怪 / BOSS 渲染器 + 理智 HUD 元素 + 深海视野锁定 + 低理智视野后处理 + BOSS 血条与战斗音乐 */
    public static void register() {
        // 影怪：GeckoLib 几何动画渲染 + 眼睛自发光图层（图层在渲染器构造器内注册）
        EntityRenderers.register(AkaishiForbiddenEntities.SHADOW.get(), ShadowRenderer::new);
        // 影怪的精神弹：复用原版凋零头渲染器（零新增贴图，见 ShadowBolt 类注释）
        EntityRenderers.register(AkaishiForbiddenEntities.SHADOW_BOLT.get(), WitherSkullRenderer::new);
        // 阿盖托洛丝：GeckoLib 几何动画渲染（阶段模型/动画 + 自发光图层）
        EntityRenderers.register(AkaishiForbiddenEntities.AGAITOLOS.get(), AgaitolosRenderer::new);
        // 阿盖托洛丝的远程弹体：复用原版凋零头渲染器（子类可被 EntityRenderer<WitherSkull> 直接渲染）
        EntityRenderers.register(AkaishiForbiddenEntities.AGAITOLOS_WITHER_SKULL.get(), WitherSkullRenderer::new);
        // 母神祭坛：方块实体渲染器（供奉物悬浮展示，P3d 随祭坛自本体迁入）
        BlockEntityRenderers.register(AkaishiForbiddenBlockEntities.CHISHI_MOTHER_ALTAR.get(), MotherAltarRenderer::new);
        // 统一 HUD 渲染层的元素（理智条）：登记到 api.hud 注册表，由本体 AkaishiHudLayer 统一解算与绘制
        AkaishiHudRegistry.register(new AkaishiSanityHudElement());
        // 深海视野锁定：主世界海洋群系 Y<0 时大幅收拢雾距并染深水色
        MinecraftForge.EVENT_BUS.register(AkaishiDeepSeaFogHandler.INSTANCE);
        // 低理智视野后处理：灰化 / 血丝 / 黑白（与不可名状后处理互斥，后者优先）
        MinecraftForge.EVENT_BUS.register(AkaishiSanityVisionPostHandler.INSTANCE);
        // 「不可名状」视野扭曲（相机滚转/抖动与 FOV 脉动）+ 整帧后处理（P3d 随不可名状自本体迁入）
        MinecraftForge.EVENT_BUS.register(AkaishiUnnameableHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AkaishiUnnameablePostHandler.INSTANCE);
        // 阿盖托洛丝战斗音乐：附近有存活 BOSS 时挂一条跟随它的循环位置音效（客户端 TickableSoundInstance）
        MinecraftForge.EVENT_BUS.register(AgaitolosMusicHandler.INSTANCE);
        // 阿盖托洛丝铭牌血条：锚在原版 boss 血条层（该层已空 —— 本 BOSS 不用 ServerBossEvent），
        // 挂上去即占据"原版血条的位置"。RegisterGuiOverlaysEvent 属 IModBusEvent，必须走 mod 事件总线。
        FMLJavaModLoadingContext.get().getModEventBus().addListener(
                (RegisterGuiOverlaysEvent event) -> {
                    // 「不可名状」边缘粗线 + 噪点 + 低语叠加层（P3d 自本体迁入）
                    event.registerAboveAll("unnameable_overlay", new AkaishiUnnameableOverlay());
                    event.registerAbove(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(),
                            "agaitolos_boss_bar", new AgaitolosBossBarOverlay());
                });
    }
}
