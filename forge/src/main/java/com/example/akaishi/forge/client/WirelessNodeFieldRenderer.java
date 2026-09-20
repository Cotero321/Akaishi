package com.example.akaishi.forge.client;

import com.example.akaishi.block.AkaishiMiniMatrixNetworkNodeBlock;
import com.example.akaishi.block.entity.AkaishiMiniMatrixNetworkNodeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 网络节点子场域屏障渲染器：节点<b>被矩阵申领</b>时，把它那 1 区块子场域画成四面透明蓝光墙。
 * <p>
 * <b>为什么节点要有自己的渲染器</b>：矩阵终端的渲染器只能画自己所在区块附近的场域
 * （方块实体渲染器的裁剪距离是相对玩家与<b>该方块实体</b>的），而节点按新口径
 * <b>不限距离</b>放置 ⇒ 远处的子场域必须由节点自己所在区块渲染，否则玩家看不到场域延伸到哪里。
 * <p>
 * <b>绘制例程复用</b> {@link WirelessFieldRenderer#renderField}：子场域与主场域只是中心/半径不同，
 * 几何与贴图必须一模一样（同一套场域被看成两种东西就是缺陷）。
 * <p>
 * <b>可见性</b>与主场域同口径（{@link WirelessFieldRenderer#visibleToLocalPlayer}）：
 * 归属者由申领的矩阵终端写入节点方块实体、随同步标签下发。
 */
public class WirelessNodeFieldRenderer implements BlockEntityRenderer<AkaishiMiniMatrixNetworkNodeBlockEntity> {

    /** 节点子场域半径（区块）：需求口径「让专用网络节点附加无线场域，半径 1 区块」 */
    private static final int RADIUS_CHUNKS = 1;

    public WirelessNodeFieldRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() {
        return WirelessFieldRenderer.VIEW_DISTANCE;
    }

    @Override
    public void render(AkaishiMiniMatrixNetworkNodeBlockEntity be, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        // 未申领的节点没有场域：状态由申领方写在方块状态上，客户端随方块更新包即时拿到
        BlockState state = be.getBlockState();
        if (!state.hasProperty(AkaishiMiniMatrixNetworkNodeBlock.ACTIVE)
                || !state.getValue(AkaishiMiniMatrixNetworkNodeBlock.ACTIVE)) {
            return;
        }
        // 每个节点各自带一个屏障开关（默认开）：关掉只影响这一个节点，也顺带解决
        // "释放时区块未加载导致 ACTIVE 残留"的观感问题 —— 玩家可以把这个节点的屏障关掉
        if (!be.barrierEnabled()) {
            return;
        }
        if (!WirelessFieldRenderer.visibleToLocalPlayer(be.claimantId(), be.claimantName())) {
            return;
        }
        WirelessFieldRenderer.renderField(level, poseStack, buffers, be.getBlockPos(), RADIUS_CHUNKS);
    }
}
