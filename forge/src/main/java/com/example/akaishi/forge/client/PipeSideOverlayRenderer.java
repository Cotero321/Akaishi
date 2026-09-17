package com.example.akaishi.forge.client;

import com.example.akaishi.block.entity.AkaishiPipeControl;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Matrix4f;

/**
 * 管道方向标识渲染器（参考 Mekanism 的 ConnectionType 几何表达）：
 * 在管臂「朝设备那一端」按方向类型套一层几何：
 * ① 模式 1（`MODE_OUTPUT`，界面显示「输入」）：臂端伸出细线头（等宽细杆 + 外端端子头）；
 * ② 模式 2（`MODE_INPUT`，界面显示「输出」）：臂端向设备张开成喇叭口，观感为"把东西送进机械"；
 * ③ 正常：不额外渲染，保持原管臂。
 * <p>
 * 注意：形态与模式的配对由用户实机裁定 —— 喇叭口归属界面显示「输出」的那一面（模式 2），
 * 线头归属显示「输入」的那一面（模式 1）；文字键（{@code message.akaishi.pipe_mode.*}）不随之改动。
 * <p>
 * 方向类型存于方块实体（{@link AkaishiPipeControl}），不进方块状态，避免变体爆炸；
 * 贴图直接取方块模型的 particle 图标，自动覆盖全部管道等级，无需硬编码等级映射。
 * 碰撞/选择箱仍由 {@code PipeShapes} 维持原臂形状，标识仅作视觉提示。
 */
public class PipeSideOverlayRenderer implements BlockEntityRenderer<BlockEntity> {

    /**
     * 标识占用的轴向范围：自核心外 5/16 到管臂外端前 1/32（避开与相邻方块面共面产生的 Z-Fighting）。
     * 必须占据整段管臂，否则锥面轴向过短（<1px）看不出形状。
     */
    private static final float MARK_INNER_DIST = 5.0F / 16.0F;
    private static final float MARK_OUTER_DIST = 0.5F - 1.0F / 32.0F;
    /**
     * 喇叭口外沿半宽（10px 宽）：模式 2 标识明显外凸于 6px 管臂。
     * BER 只能在方块模型上「叠加」几何，任何整体落在管臂内部（半宽 ≤3/16）的形状都不可见，
     * 因此标识必须有一截半宽大于管臂（3/16）。
     */
    private static final float MOUTH_HALF = 5.0F / 16.0F;
    /** 收口半宽（7px 宽）：喇叭口内沿，仍略大于管臂半宽，避免与管臂侧面共面导致闪烁 */
    private static final float NARROW_HALF = 3.5F / 16.0F;
    /**
     * 线头细杆半宽（7px 宽）：模式 1 的"线"本体。
     * 下限受 BER 叠加渲染限制 —— 半宽 ≤ 管臂 3/16（6px 宽）会被管臂完全遮挡而看不见，
     * 故取 3.5/16（比管臂外凸 0.5px），既保证可见又避免与管臂侧面共面产生 Z-Fighting。
     */
    private static final float WIRE_HALF = 3.5F / 16.0F;
    /** 线头端子半宽（7.5px 宽）：外端一小截放大 0.5px，作为"线头"的辨识点 */
    private static final float WIRE_TIP_HALF = 3.75F / 16.0F;
    /** 线杆与端子头的分界比例：外端 30% 长度为端子头 */
    private static final float WIRE_TIP_SPLIT = 0.7F;

    /** 复用顶点缓冲：内 4 角 + 外 4 角，各含 xyz（渲染线程单线程调用，无并发问题） */
    private final float[] corners = new float[24];

    public PipeSideOverlayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!(be instanceof AkaishiPipeControl control) || be.getLevel() == null) {
            return;
        }
        BlockState state = be.getBlockState();

        // 先判定是否存在需要绘制标识的面，全部为正常/未连接时直接跳过（省去贴图与缓冲开销）
        boolean any = false;
        for (Direction dir : Direction.values()) {
            if (needMark(control, state, dir)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                .getParticleIcon(state);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        Matrix4f matrix = poseStack.last().pose();

        for (Direction dir : Direction.values()) {
            if (!needMark(control, state, dir)) {
                continue;
            }
            // 模式 2（界面显示「输出」）= 朝设备张开的喇叭口；模式 1（界面显示「输入」）= 探向设备的细线头。
            // 两者都有一截半宽超出 6px 管臂，保证 BER 叠加渲染下必然可见。
            int mode = control.getSideMode(dir);
            if (mode == AkaishiPipeControl.MODE_INPUT) {
                renderTip(consumer, matrix, dir, MARK_INNER_DIST, MARK_OUTER_DIST,
                        NARROW_HALF, MOUTH_HALF, sprite, packedLight);
            } else {
                renderWire(consumer, matrix, dir, sprite, packedLight);
            }
        }
    }

    /** 该面是否需要绘制标识：方向类型非正常，且该面确实连着设备（断开或未连接时不画，避免悬空锥体） */
    private static boolean needMark(AkaishiPipeControl control, BlockState state, Direction dir) {
        if (control.getSideMode(dir) == AkaishiPipeControl.MODE_NORMAL) {
            return false;
        }
        if (control.isDisconnected(dir)) {
            return false;
        }
        // 连接态由方块状态的布尔属性表达（属性名与 Direction#getName 一致）
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty bool && bool.getName().equals(dir.getName())) {
                return state.getValue(bool);
            }
        }
        return false;
    }

    /**
     * 绘制"线头"标识（模式 1）：等宽细杆占内侧 70%，外端 30% 放大成端子头。
     * 杆与端子都比 6px 管臂外凸 0.5px 以上，保证 BER 叠加渲染下轮廓可见。
     */
    private void renderWire(VertexConsumer consumer, Matrix4f matrix, Direction dir,
                            TextureAtlasSprite sprite, int light) {
        float splitDist = MARK_INNER_DIST + (MARK_OUTER_DIST - MARK_INNER_DIST) * WIRE_TIP_SPLIT;
        renderTip(consumer, matrix, dir, MARK_INNER_DIST, splitDist, WIRE_HALF, WIRE_HALF, sprite, light);
        renderTip(consumer, matrix, dir, splitDist, MARK_OUTER_DIST, WIRE_HALF, WIRE_TIP_HALF, sprite, light);
    }

    /**
     * 绘制单面标识的一段四棱台：由「靠近核心的截面」过渡到「靠近设备的截面」+ 朝设备侧封口。
     * 内侧半宽小于外侧 = 喇叭口（模式 2）；两段等宽细杆 + 末端放大 = 线头（模式 1）。
     */
    private void renderTip(VertexConsumer consumer, Matrix4f matrix, Direction dir, float innerDist,
                           float outerDist, float innerHalf, float outerHalf,
                           TextureAtlasSprite sprite, int light) {
        float nx = dir.getStepX();
        float ny = dir.getStepY();
        float nz = dir.getStepZ();

        // 取与法线正交的横截面基向量：u = ref × n，v = n × u
        float rx;
        float ry;
        float rz;
        if (Math.abs(ny) > 0.5F) {
            rx = 1.0F;
            ry = 0.0F;
            rz = 0.0F;
        } else {
            rx = 0.0F;
            ry = 1.0F;
            rz = 0.0F;
        }
        float ux = ry * nz - rz * ny;
        float uy = rz * nx - rx * nz;
        float uz = rx * ny - ry * nx;
        float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;
        float vx = ny * uz - nz * uy;
        float vy = nz * ux - nx * uz;
        float vz = nx * uy - ny * ux;

        float span = outerDist - innerDist;
        float cx = 0.5F + nx * innerDist;
        float cy = 0.5F + ny * innerDist;
        float cz = 0.5F + nz * innerDist;
        fillCorners(0, cx, cy, cz, ux, uy, uz, vx, vy, vz, innerHalf);
        fillCorners(12, cx + nx * span, cy + ny * span, cz + nz * span, ux, uy, uz, vx, vy, vz, outerHalf);

        float u0 = sprite.getU(0.0F);
        float u1 = sprite.getU(1.0F);
        float v0 = sprite.getV(0.0F);
        float v1 = sprite.getV(1.0F);

        // 四侧面：内截面第 i 角 → 外截面对应角，绕一周闭合
        for (int i = 0; i < 4; i++) {
            int a = i * 3;
            int b = ((i + 1) % 4) * 3;
            quad(consumer, matrix, a, b, 12 + b, 12 + a, u0, u1, v0, v1, light);
        }
        // 外封口（朝设备）：给出锥/喇叭的端面
        quad(consumer, matrix, 12, 15, 18, 21, u0, u1, v0, v1, light);
    }

    /** 写入一段横截面的四个角：0:(-u,-v) 1:(+u,-v) 2:(+u,+v) 3:(-u,+v) */
    private void fillCorners(int offset, float cx, float cy, float cz, float ux, float uy, float uz,
                             float vx, float vy, float vz, float half) {
        corners[offset] = cx - ux * half - vx * half;
        corners[offset + 1] = cy - uy * half - vy * half;
        corners[offset + 2] = cz - uz * half - vz * half;
        corners[offset + 3] = cx + ux * half - vx * half;
        corners[offset + 4] = cy + uy * half - vy * half;
        corners[offset + 5] = cz + uz * half - vz * half;
        corners[offset + 6] = cx + ux * half + vx * half;
        corners[offset + 7] = cy + uy * half + vy * half;
        corners[offset + 8] = cz + uz * half + vz * half;
        corners[offset + 9] = cx - ux * half + vx * half;
        corners[offset + 10] = cy - uy * half + vy * half;
        corners[offset + 11] = cz - uz * half + vz * half;
    }

    /** 按顶点下标（corners 数组中的 xyz 偏移）绘制一个四边形，UV 取贴图整幅 */
    private void quad(VertexConsumer consumer, Matrix4f matrix, int i0, int i1, int i2, int i3,
                      float u0, float u1, float v0, float v1, int light) {
        vertex(consumer, matrix, i0, u0, v0, light);
        vertex(consumer, matrix, i1, u1, v0, light);
        vertex(consumer, matrix, i2, u1, v1, light);
        vertex(consumer, matrix, i3, u0, v1, light);
    }

    private void vertex(VertexConsumer consumer, Matrix4f matrix, int index, float u, float v, int light) {
        consumer.vertex(matrix, corners[index], corners[index + 1], corners[index + 2])
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
