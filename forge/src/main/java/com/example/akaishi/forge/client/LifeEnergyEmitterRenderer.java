package com.example.akaishi.forge.client;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiLifeEnergyEmitterBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 生命能量发射器渲染器（参照 Botania 魔力发射器：克制、非刺眼，但整机连续瞄准）：
 * ① 方块本体 {@link net.minecraft.world.level.block.RenderShape#INVISIBLE}，整机由本渲染器绘制，
 *    以方块中心为轴心做 look-at 旋转，突破 BlockState 只有 6 向的限制，实现 360° 平滑对准目标；
 * ② 仅"发射瞬间"由炮口沿目标方向拉出一道低透明度窄能量流，随时间缓慢淡出，平时不显示
 *    （避免常亮信标柱的刺眼观感）。
 * <p>
 * 模型正面（炮口）= north 面 → 方块局部 -Z 轴，故把局部 -Z 轴对齐瞄准向量即完成整机转向。
 */
public class LifeEnergyEmitterRenderer implements BlockEntityRenderer<AkaishiLifeEnergyEmitterBlockEntity> {

    private static final ResourceLocation CORE_TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/entity/life_energy_projectile.png");
    /** 发射后能量流持续渲染的刻数（仅此窗口内有传输观感，平时不显示；拉长窗口以获得更长的拖尾） */
    private static final int FIRE_FLOW_TICKS = 24;
    /** 能量流颜色（青绿，与生命能量族识别色一致） */
    private static final float[] FLOW_COLOR = {0.18F, 0.90F, 0.60F};
    /** 能量流半宽（格）——刻意收窄，避免形成粗亮光柱 */
    private static final float FLOW_HALF_WIDTH = 0.07F;
    /** 炮口核心相对方块中心沿瞄准方向的外移量（格） */
    private static final double CORE_OFFSET = 0.45D;
    /** 方块局部中心 */
    private static final Vec3 LOCAL_CENTER = new Vec3(0.5D, 0.5D, 0.5D);
    /** 模型炮口轴（方块局部空间的 north 面法线，即 -Z），整机旋转的基准向量 */
    private static final Vector3f MUZZLE_AXIS = new Vector3f(0.0F, 0.0F, -1.0F);

    public LifeEnergyEmitterRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AkaishiLifeEnergyEmitterBlockEntity emitter, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (emitter.getLevel() == null) {
            return;
        }
        Vec3 aim = emitter.getAimDirection();
        float time = emitter.getLevel().getGameTime() + partialTick;

        // 整机连续朝向渲染（炮口/光束方向同一来源，保证两者一致）
        // 注：本体是不透明满块，方块自身位置光照恒为 0，必须改用相邻位置光照，否则整机全黑
        renderRotatedModel(emitter, aim, poseStack, bufferSource, resolveModelLight(emitter), packedOverlay);

        Vec3 muzzle = muzzleLocal(aim);

        BlockPos target = emitter.getTarget();
        double rangeSqr = (double) AkaishiLifeEnergyEmitterBlockEntity.RANGE * AkaishiLifeEnergyEmitterBlockEntity.RANGE;
        if (target == null || emitter.getBlockPos().distSqr(target) > rangeSqr) {
            return;
        }
        long lastFire = emitter.getLastFireTick();
        if (lastFire == AkaishiLifeEnergyEmitterBlockEntity.NO_FIRE) {
            return;
        }
        float elapsed = time - lastFire;
        if (elapsed < 0.0F || elapsed > FIRE_FLOW_TICKS) {
            return;
        }
        Vec3 end = Vec3.atCenterOf(target).subtract(Vec3.atLowerCornerOf(emitter.getBlockPos()));
        // 亮度曲线：前段保持较亮、末段快速收尾（1→0 的二次缓出），观感上是"拖尾"而非一闪即灭
        float t = Mth.clamp(elapsed / FIRE_FLOW_TICKS, 0.0F, 1.0F);
        renderFireFlow(poseStack, bufferSource, muzzle, end, 1.0F - t * t);
    }

    /** 绘制整机模型：以方块中心为轴心把局部炮口轴(-Z)旋转到瞄准向量方向 */
    private void renderRotatedModel(AkaishiLifeEnergyEmitterBlockEntity emitter, Vec3 aim, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(LOCAL_CENTER.x, LOCAL_CENTER.y, LOCAL_CENTER.z);
        poseStack.mulPose(aimRotation(aim));
        poseStack.translate(-LOCAL_CENTER.x, -LOCAL_CENTER.y, -LOCAL_CENTER.z);
        renderBlockModel(emitter.getBlockState(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /**
     * 直接绘制方块模型。
     * 因本体 {@code RenderShape = INVISIBLE}，{@code BlockRenderDispatcher.renderSingleBlock} 会提前返回而不绘制，
     * 故此处复刻其 MODEL 分支（取烘焙模型 → 按渲染类型逐层 renderModel），保持与常规方块一致的观感。
     */
    private static void renderBlockModel(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        ModelBlockRenderer renderer = dispatcher.getModelRenderer();
        for (RenderType type : model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY)) {
            renderer.renderModel(poseStack.last(),
                    bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(type, false)),
                    state, model, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay, ModelData.EMPTY, type);
        }
    }

    /**
     * 计算整机模型应使用的光照。
     * 本体为不透明满块，BER 传入的 packedLight 取自方块自身位置（光照无法进入块内部，恒为 0），
     * 若直接透传会导致整机全黑；故改采样六个相邻位置，取天光/块光各自最大值再打包。
     */
    private static int resolveModelLight(AkaishiLifeEnergyEmitterBlockEntity emitter) {
        var level = emitter.getLevel();
        if (level == null) {
            return 0xF000F0;
        }
        BlockPos pos = emitter.getBlockPos();
        int sky = 0;
        int block = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            int s = level.getBrightness(LightLayer.SKY, neighbor);
            if (s > sky) {
                sky = s;
            }
            int b = level.getBrightness(LightLayer.BLOCK, neighbor);
            if (b > block) {
                block = b;
            }
        }
        return LightTexture.pack(block, sky);
    }

    /** 求把局部炮口轴(-Z)对齐到瞄准向量的旋转四元数（含共线退化处理） */
    private static Quaternionf aimRotation(Vec3 aim) {
        Vector3f to = new Vector3f((float) aim.x, (float) aim.y, (float) aim.z);
        if (to.lengthSquared() < 1.0E-6F) {
            return new Quaternionf();
        }
        to.normalize();
        Vector3f axis = new Vector3f(MUZZLE_AXIS).cross(to);
        float dot = MUZZLE_AXIS.dot(to);
        if (axis.lengthSquared() < 1.0E-6F) {
            // 与炮口轴共线：同向无需旋转；反向绕 Y 轴转 180°
            return dot > 0.0F
                    ? new Quaternionf()
                    : new Quaternionf(new AxisAngle4f((float) Math.PI, 0.0F, 1.0F, 0.0F));
        }
        axis.normalize();
        float angle = (float) Math.acos(Mth.clamp(dot, -1.0F, 1.0F));
        return new Quaternionf(new AxisAngle4f(angle, axis.x, axis.y, axis.z));
    }

    /** 炮口局部坐标：方块中心沿连续瞄准方向外移 {@link #CORE_OFFSET} */
    private static Vec3 muzzleLocal(Vec3 aim) {
        return LOCAL_CENTER.add(aim.scale(CORE_OFFSET));
    }

    /** 发射瞬间的窄能量流：由炮口沿瞄准方向指向目标，出膛端稍亮、目标端渐隐，整体随 fade 快速淡出 */
    private void renderFireFlow(PoseStack poseStack, MultiBufferSource bufferSource,
                                Vec3 start, Vec3 end, float fade) {
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 0.05D) {
            return;
        }
        Vec3 dir = delta.scale(1.0D / length);
        Vector3f look = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        Vec3 forward = new Vec3(look.x(), look.y(), look.z());
        Vec3 rightUnit = dir.cross(forward);
        if (rightUnit.lengthSqr() < 1.0E-6D) {
            rightUnit = dir.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }
        rightUnit = rightUnit.normalize();

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucentEmissive(CORE_TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        int light = 0xF000F0;
        float r = FLOW_COLOR[0];
        float g = FLOW_COLOR[1];
        float b = FLOW_COLOR[2];
        // 外层柔光晕：更宽更淡，营造能量流外溢的发光边
        beamQuad(consumer, matrix, start, end, rightUnit.scale(FLOW_HALF_WIDTH * 2.4F), r, g, b,
                (int) (70.0F * fade), 0, 0.0F, 1.0F, light);
        // 主束：出膛端亮、目标端近乎透明，形成"被输送走"的口感
        beamQuad(consumer, matrix, start, end, rightUnit.scale(FLOW_HALF_WIDTH), r, g, b,
                (int) (165.0F * fade), (int) (18.0F * fade), 0.0F, 1.0F, light);
        // 炮口短促起爆段：强化"能量从炮口射出"的方向感
        Vec3 kickEnd = start.add(dir.scale(Math.min(0.6D, length)));
        beamQuad(consumer, matrix, start, kickEnd, rightUnit.scale(FLOW_HALF_WIDTH), r, g, b,
                (int) (215.0F * fade), 0, 0.0F, 0.25F, light);
    }

    /** 沿 a→b 的一条相机朝向窄带（a 端 alphaA、b 端 alphaB） */
    private static void beamQuad(VertexConsumer consumer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 right,
                                 float r, float g, float b2, int alphaA, int alphaB,
                                 float vA, float vB, int light) {
        float aA = alphaA / 255.0F;
        float aB = alphaB / 255.0F;
        consumer.vertex(matrix, (float) (a.x - right.x), (float) (a.y - right.y), (float) (a.z - right.z))
                .color(r, g, b2, aA).uv(0.0F, vA).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, (float) (a.x + right.x), (float) (a.y + right.y), (float) (a.z + right.z))
                .color(r, g, b2, aA).uv(1.0F, vA).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, (float) (b.x + right.x), (float) (b.y + right.y), (float) (b.z + right.z))
                .color(r, g, b2, aB).uv(1.0F, vB).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
        consumer.vertex(matrix, (float) (b.x - right.x), (float) (b.y - right.y), (float) (b.z - right.z))
                .color(r, g, b2, aB).uv(0.0F, vB).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(0.0F, 1.0F, 0.0F).endVertex();
    }
}
