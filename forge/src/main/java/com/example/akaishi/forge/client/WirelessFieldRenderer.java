package com.example.akaishi.forge.client;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.block.entity.AkaishiMiniMatrixTerminalBlockEntity;
import com.example.akaishi.config.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * 无线场域屏障渲染器：把微缩矩阵终端的场域范围画成<b>四面透明蓝光墙</b>。
 * <p>
 * <b>为什么用方块实体渲染器而不是世界渲染事件</b>：场域参数（内腔「无线场域升级」数量）
 * 客户端本来就拿得到 —— 方块实体在客户端也会跑一次只读重扫
 * （见 {@code AkaishiMiniMatrixTerminalBlockEntity#clientTick}），
 * 因此不需要新增同步包，也不依赖平台特有的世界渲染事件；
 * 渲染器只在本区块被渲染时触发，远处矩阵自然不会被画，无需额外距离判断。
 * <p>
 * <b>几何</b>：场域按<b>区块切比雪夫距离</b>界定（与 {@code WirelessFieldManager.inField} 同一口径），
 * 故墙体落在区块边界上、四角精确对齐。竖直方向取整根世界高度 ——
 * 屏障只在水平方向表达"覆盖范围"，不试图圈出一个盒子。
 * <p>
 * <b>双面绘制</b>：站在场域内外都要能看到墙，故每面按正反两种绕序各画一次，
 * 不依赖 {@link RenderType#entityTranslucent} 是否开启背面剔除。
 * 贴图按<b>方块</b>为周期平铺（UV = 方块数），故任意半径下场域网格密度一致。
 */
public class WirelessFieldRenderer implements BlockEntityRenderer<AkaishiMiniMatrixTerminalBlockEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/entity/wireless_field.png");

    /** 场域可能延伸到矩阵外 3 区块（48 格），渲染距离取 128 保证在场域内任意位置都能看到墙 */
    public static final int VIEW_DISTANCE = 128;

    /** 蓝色染色（贴图本身近似白 + 自带 alpha，最终色 = 贴图 × 此色） */
    private static final float TINT_R = 0.45F;
    private static final float TINT_G = 0.75F;
    private static final float TINT_B = 1.0F;
    private static final float TINT_A = 1.0F;

    public WirelessFieldRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    @Override
    public void render(AkaishiMiniMatrixTerminalBlockEntity be, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        int radius = be.fieldRadiusChunks();
        if (level == null || radius <= 0 || !visibleToLocalPlayer(be.fieldOwnerId(), be.fieldOwnerName())) {
            return;
        }
        renderField(level, poseStack, buffers, be.getBlockPos(), radius);
    }

    /**
     * 画一片场域（区块切比雪夫范围）的四面透明蓝光墙。
     * <p>
     * 抽成静态例程供<b>矩阵主场域</b>与<b>网络节点子场域</b>共用：两者只有"中心与半径"不同，
     * 几何、贴图、双面绕序必须完全一致，否则同一套场域会被看成两种东西。
     *
     * @param origin 顶点坐标的相对原点（对应 PoseStack 已平移到该方块的坐标系）
     */
    public static void renderField(Level level, PoseStack poseStack, MultiBufferSource buffers,
            BlockPos origin, int radiusChunks) {
        // 场域以中心所在区块为准，故墙体落在区块边界上
        ChunkPos center = new ChunkPos(origin);
        float x0 = (center.x - radiusChunks) * 16.0F - origin.getX();
        float x1 = (center.x + radiusChunks + 1) * 16.0F - origin.getX();
        float z0 = (center.z - radiusChunks) * 16.0F - origin.getZ();
        float z1 = (center.z + radiusChunks + 1) * 16.0F - origin.getZ();
        float y0 = level.getMinBuildHeight() - origin.getY();
        float y1 = level.getMaxBuildHeight() - origin.getY();

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        float uWidth = (x1 - x0) / 16.0F;
        float uDepth = (z1 - z0) / 16.0F;
        float vHeight = (y1 - y0) / 16.0F;

        // 四面墙：法线朝外，四角按 (左上, 右上, 右下, 左下) 给
        wall(consumer, matrix, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, uWidth, vHeight);
        wall(consumer, matrix, x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, uWidth, vHeight);
        wall(consumer, matrix, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, uDepth, vHeight);
        wall(consumer, matrix, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, uDepth, vHeight);
    }

    /**
     * 可见性过滤（设计记忆 §15.2 口径 2）：默认<b>所有人可见</b>（场域是环境提示），
     * 配置开启后只对归属者本人与其同队可见。
     * <p>
     * 同队判定用<b>归属者名字</b>比对玩家队伍成员表（{@link Team#getPlayers()} 返回名字），
     * 因此归属者离线时也能判定 —— 这也是同步归属者名而不只同步 UUID 的原因。
     * <p>
     * 主场域（矩阵终端）与子场域（网络节点）共用本判定：节点侧写的是<b>申领它的矩阵终端</b>的归属者。
     */
    public static boolean visibleToLocalPlayer(@Nullable UUID owner, @Nullable String ownerName) {
        if (!ModConfig.wirelessFieldOwnerOnly) {
            return true;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (owner != null && owner.equals(player.getUUID())) {
            return true;
        }
        Team team = player.getTeam();
        return team != null && ownerName != null && team.getPlayers().contains(ownerName);
    }

    /** 一面墙：正反两种绕序各画一次（场域内外都可见） */
    private static void wall(VertexConsumer consumer, Matrix4f matrix,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            float uMax, float vMax) {
        quad(consumer, matrix, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, uMax, vMax);
        quad(consumer, matrix, dx, dy, dz, cx, cy, cz, bx, by, bz, ax, ay, az, uMax, vMax);
    }

    /** 一个四边形：UV 以方块为周期（左上 (0,0)、右下 (uMax,vMax)） */
    private static void quad(VertexConsumer consumer, Matrix4f matrix,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz,
            float uMax, float vMax) {
        vertex(consumer, matrix, ax, ay, az, 0.0F, 0.0F);
        vertex(consumer, matrix, bx, by, bz, uMax, 0.0F);
        vertex(consumer, matrix, cx, cy, cz, uMax, vMax);
        vertex(consumer, matrix, dx, dy, dz, 0.0F, vMax);
    }

    /** 屏障不受光照/雾影响（自发光观感），故用满亮 + 无叠加层 */
    private static void vertex(VertexConsumer consumer, Matrix4f matrix,
            float x, float y, float z, float u, float v) {
        consumer.vertex(matrix, x, y, z)
                .color(TINT_R, TINT_G, TINT_B, TINT_A)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
