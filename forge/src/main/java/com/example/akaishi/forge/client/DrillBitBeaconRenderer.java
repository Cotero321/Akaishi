package com.example.akaishi.forge.client;

import com.example.akaishi.block.entity.AkaishiMinerDrillBitBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 钻机钻头渲染器：所属矿机结构成型时，从钻头底面向正下方打出贯穿的信标光束
 * （模拟向地底钻探的光柱）。仅客户端，成型状态直接读控制器 FORMED。
 * 复用原版 BeaconRenderer 光束：高度传负值即可让光束沿 Y 负方向延伸。
 */
public class DrillBitBeaconRenderer implements BlockEntityRenderer<AkaishiMinerDrillBitBlockEntity> {

    /** 光束颜色（青白，模拟钻探能量） */
    private static final float[] BEAM_COLOR = {0.45F, 0.80F, 1.0F};
    /** 向下的光束长度（方块数） */
    private static final int BEAM_DOWN_LENGTH = 48;

    public DrillBitBeaconRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AkaishiMinerDrillBitBlockEntity drill, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (drill.getLevel() == null || !drill.isPartOfFormedStructure()) {
            return;
        }
        poseStack.pushPose();
        // 从钻头底面（局部 y=0）向下投射：height 为负使光束朝下方延伸
        BeaconRenderer.renderBeaconBeam(poseStack, bufferSource,
                new net.minecraft.resources.ResourceLocation("textures/entity/beacon_beam.png"),
                partialTick, 1.0F, drill.getLevel().getGameTime(), 0, -BEAM_DOWN_LENGTH,
                BEAM_COLOR, 0.2F, 0.25F);
        poseStack.popPose();
    }
}
