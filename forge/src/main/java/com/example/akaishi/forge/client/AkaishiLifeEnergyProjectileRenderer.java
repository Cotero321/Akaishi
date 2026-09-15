package com.example.akaishi.forge.client;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.entity.AkaishiLifeEnergyProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 生命能量弹渲染器：纯彗尾表现——沿飞行方向由亮到透明渐隐的窄带拖尾，
 * 不带亮核球，传达"能量被输送"而非单点闪光的观感（参照 Botania 发射器 burst 的拖尾）。
 */
public class AkaishiLifeEnergyProjectileRenderer extends EntityRenderer<AkaishiLifeEnergyProjectileEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/entity/life_energy_projectile.png");
    /** 拖尾长度（格） */
    private static final double TRAIL_LENGTH = 4.0D;
    /** 外层柔光晕半宽（格）——最宽最淡，营造能量外溢的发光边 */
    private static final float TRAIL_GLOW_WIDTH = 0.16F;
    /** 主束半宽（格） */
    private static final float TRAIL_HALF_WIDTH = 0.09F;
    /** 高亮内芯半宽（格）——最细最亮，构成光束的"骨" */
    private static final float TRAIL_CORE_WIDTH = 0.035F;

    public AkaishiLifeEnergyProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AkaishiLifeEnergyProjectileEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 沿飞行方向（由当前位指向目标）构建拖尾；方向退化时退回竖直
        BlockPos target = entity.getTarget();
        Vec3 delta = BlockPos.ZERO.equals(target)
                ? Vec3.ZERO
                : Vec3.atCenterOf(target).subtract(entity.position());
        Vec3 dir = delta.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 1.0D, 0.0D) : delta.normalize();
        Vector3f look = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        Vec3 forward = new Vec3(look.x(), look.y(), look.z());
        Vec3 rightUnit = dir.cross(forward);
        if (rightUnit.lengthSqr() < 1.0E-6D) {
            rightUnit = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        rightUnit = rightUnit.normalize();

        poseStack.pushPose();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        int light = 0xF000F0;

        // 三层绿色光束叠加：外层柔光晕（宽而淡）→ 主束 → 高亮内芯（细而亮），构成饱满的发光柱
        trailLayer(consumer, matrix, dir, rightUnit, TRAIL_GLOW_WIDTH, 70, light);
        trailLayer(consumer, matrix, dir, rightUnit, TRAIL_HALF_WIDTH, 190, light);
        trailLayer(consumer, matrix, dir, rightUnit, TRAIL_CORE_WIDTH, 255, light);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** 绘制一条由弹体位置沿 -dir 延伸 {@link #TRAIL_LENGTH} 的窄带（近端 alphaHead、尾端透明） */
    private static void trailLayer(VertexConsumer consumer, Matrix4f matrix, Vec3 dir, Vec3 rightUnit,
                                   float halfWidth, int alphaHead, int light) {
        Vec3 r = rightUnit.scale(halfWidth);
        Vec3 tail = dir.scale(-TRAIL_LENGTH);
        quad(consumer, matrix,
                new Vec3(-r.x, -r.y, -r.z), new Vec3(r.x, r.y, r.z),
                new Vec3(tail.x + r.x, tail.y + r.y, tail.z + r.z),
                new Vec3(tail.x - r.x, tail.y - r.y, tail.z - r.z),
                alphaHead, 0, light);
    }

    /** 以 4 个世界坐标（相对实体原点）构建一个四边形，顶点色 alpha 依次为 a0..a3 */
    private static void quad(VertexConsumer consumer, Matrix4f matrix,
                             Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int a0, int a3, int light) {
        consumer.vertex(matrix, (float) p0.x, (float) p0.y, (float) p0.z).color(255, 255, 255, a0 / 255.0F)
                .uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, (float) p1.x, (float) p1.y, (float) p1.z).color(255, 255, 255, a0 / 255.0F)
                .uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, (float) p2.x, (float) p2.y, (float) p2.z).color(255, 255, 255, a3 / 255.0F)
                .uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
        consumer.vertex(matrix, (float) p3.x, (float) p3.y, (float) p3.z).color(255, 255, 255, a3 / 255.0F)
                .uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0.0F, 0.0F, 1.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(AkaishiLifeEnergyProjectileEntity entity) {
        return TEXTURE;
    }
}
