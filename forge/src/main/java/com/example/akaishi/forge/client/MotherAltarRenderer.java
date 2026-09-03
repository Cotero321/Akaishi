package com.example.akaishi.forge.client;

import com.example.akaishi.block.entity.AkaishiMotherAltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 母神祭坛渲染器：把供奉物渲染于方块顶面之上——缓慢自转 + 轻微起伏，
 * 营造"献祭之物悬于母神之前"的氛围。客户端专用，渲染数据来自 BE 同步的 NBT。
 */
public class MotherAltarRenderer implements BlockEntityRenderer<AkaishiMotherAltarBlockEntity> {

    public MotherAltarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AkaishiMotherAltarBlockEntity altar, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack offering = altar.getOffering();
        if (offering.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // 祭坛顶台面（0.875 block）上方悬浮
        poseStack.translate(0.5D, 1.12D, 0.5D);
        long time = altar.getLevel() != null ? altar.getLevel().getGameTime() : 0L;
        float t = time + partialTick;
        // 缓慢自转
        poseStack.mulPose(Axis.YP.rotationDegrees((t * 1.4F) % 360.0F));
        // 轻微起伏
        poseStack.translate(0.0D, Math.sin(t * 0.05D) * 0.06D, 0.0D);
        poseStack.scale(0.6F, 0.6F, 0.6F);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            poseStack.popPose();
            return;
        }
        int light = altar.getLevel() != null
                ? LevelRenderer.getLightColor(altar.getLevel(), altar.getBlockPos().above())
                : packedLight;
        mc.getItemRenderer().renderStatic(offering, ItemDisplayContext.FIXED, light,
                packedOverlay, poseStack, bufferSource, mc.level, 0);
        poseStack.popPose();
    }
}
