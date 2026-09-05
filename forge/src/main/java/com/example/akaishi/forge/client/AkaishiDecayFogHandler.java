package com.example.akaishi.forge.client;

import com.example.akaishi.decay.DecayClientAmbience;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 衰竭区域雾效渲染（仅客户端）：玩家身处衰竭区域时，随污染强度
 * 将雾色染成死地暗紫灰并收拢雾距，营造压抑的"污染群系"氛围。
 * 强度由 {@link DecayClientAmbience} 提供并平滑过渡，离开区域后自动还原。
 */
public final class AkaishiDecayFogHandler {

    public static final AkaishiDecayFogHandler INSTANCE = new AkaishiDecayFogHandler();

    /** 污染雾目标色（RGB 0~1）：暗紫灰，与衰竭土壤/木的视觉基调一致 */
    private static final float FOG_R = 0.16f;
    private static final float FOG_G = 0.145f;
    private static final float FOG_B = 0.20f;
    /** 满强度时雾距收拢比例（far 缩到 45%，near 缩到 30%） */
    private static final float FAR_SHRINK = 0.45f;
    private static final float NEAR_SHRINK = 0.30f;

    private AkaishiDecayFogHandler() {
    }

    @SubscribeEvent
    public void onRenderFog(ViewportEvent.RenderFog event) {
        // 仅地形雾生效；雾距修改必须取消事件才会被采用
        float level = DecayClientAmbience.current();
        if (level <= 0.01f || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        event.setNearPlaneDistance(event.getNearPlaneDistance() * (1f - NEAR_SHRINK * level));
        event.setFarPlaneDistance(event.getFarPlaneDistance() * (1f - FAR_SHRINK * level));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        float level = DecayClientAmbience.current();
        if (level <= 0.01f) {
            return;
        }
        event.setRed(event.getRed() + (FOG_R - event.getRed()) * level);
        event.setGreen(event.getGreen() + (FOG_G - event.getGreen()) * level);
        event.setBlue(event.getBlue() + (FOG_B - event.getBlue()) * level);
    }
}
