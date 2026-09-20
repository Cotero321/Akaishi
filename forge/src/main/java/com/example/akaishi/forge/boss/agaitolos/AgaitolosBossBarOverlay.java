package com.example.akaishi.forge.boss.agaitolos;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.boss.agaitolos.AgaitolosEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

/**
 * 阿盖托洛丝的<b>自定义铭牌式血条</b>（仅客户端 HUD），替换原版 {@code ServerBossEvent} 血条。
 * <p>
 * <b>为什么整套搬到客户端</b>：原版血条是「服务端下发事件 → 客户端用固定 182×5 的 bars.png 画」，
 * 形状、配色、阶段表现都改不了。这里改成纯客户端 overlay：<b>不占一条服务端 BossEvent，
 * 但按需求的铭牌贴图自绘</b>，并且按阶段换图集 —— 原版事件做不到「同一条血条换皮」。
 * <p>
 * <b>为什么不需要服务端参与</b>：客户端世界里的实体是服务端按 {@code clientTrackingRange}
 * 同步过来的，玩家身边没有该实体时，客户端根本没有这个对象 —— 于是「扫到了实体」与
 * 「玩家看得见它」天然等价，不需要再写一套参与者对账（旧 {@code AgaitolosBossBar} 那一套）。
 * <p>
 * <b>数据来源</b>：阶段（{@code DATA_PHASE}）与复活（{@code DATA_RESPAWNING}）是同步数据，
 * 客户端可直接读；血量/最大血量走原版同步，也直接读。本类<b>只读不写</b>，不产生任何包。
 * <p>
 * <b>叠放偏移表（三张图集几何同源，故只有这一套常量）</b>：坐标基准 {@code (px, py)} = 铭牌左上角。
 * <pre>
 *   顺序 区域        源矩形(图集内)                绘制到(相对 px,py)
 *   1    plate      (0,0,256,64)                 (px,        py)
 *   2    fill       (0,64,W,16) W=⌊240×血量比⌋    (px+8,      py+24)
 *   3    rim        (0,80,240,16)                (px+8,      py+24)   ← 最上层压边
 *   4    endL       (0,96,32,32)                 (px-32,     py+16)
 *   5    endR       (32,96,32,32)                (px+256,    py+16)
 *   6    crown      (64,96,64,32)                (px+96,     py-28)
 *   7    pendant    (128,96,64,32)               (px+96,     py+60)
 *   8    四角件     (0/32/64/96,128,32,32)        (px,py)(px+224,py)(px,py+32)(px+224,py+32)
 * </pre>
 * 内嵌轨道可用区在 plate 内为 x 8..247（宽 240）、y 24..39（高 16）；plate 内 y 5..21 是留给
 * 代码写名字的净区，贴图在那里没有元素，故文字可以直接叠在 plate 上。
 */
public final class AgaitolosBossBarOverlay implements IGuiOverlay {

    /** 图集尺寸（三张都是 256×256），作为 blit 的 textureWidth/textureHeight */
    private static final int TEX_SIZE = 256;

    /**
     * 阶段 → 图集。下标 = {@code combatOrdinal - 1}（1/2/3 分别对应一/二/三阶段）。
     * 阶段是同步数据，客户端直接读，不需要任何额外包。
     */
    private static final ResourceLocation[] PHASE_TEXTURES = {
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/agaitolos_bar_phase1.png"),
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/agaitolos_bar_phase2.png"),
            new ResourceLocation(AkaishiMod.MOD_ID, "textures/gui/agaitolos_bar_phase3.png"),
    };

    // ---------------------------------------------------------------- 图集区域（源）
    private static final int PLATE_U = 0;
    private static final int PLATE_V = 0;
    private static final int PLATE_W = 256;
    private static final int PLATE_H = 64;

    /** 内嵌轨道在 plate 内的可用区（也是 rim 的源宽高 / fill 的最大宽度） */
    private static final int TRACK_OFFSET_X = 8;
    private static final int TRACK_OFFSET_Y = 24;
    private static final int TRACK_W = 240;
    private static final int TRACK_H = 16;

    private static final int FILL_V = 64;
    private static final int RIM_V = 80;

    private static final int DECOR_V = 96;
    private static final int CORNER_V = 128;
    private static final int DECOR_SIZE = 32;

    /** 左右端件的目标 x（相对 px）：endL 在 px-32、endR 在 px+256（源 u 分别为 0 / 32） */
    private static final int END_L_X = -32;
    private static final int END_R_X = 256;
    /** 上下挂件（冠饰 / 坠饰）的目标 x，两者同 x（相对 px） */
    private static final int PENDANT_X = 96;

    // ---------------------------------------------------------------- 布局（相对 px, py）
    /** 铭牌整体占据的逻辑宽度：endL 从 px-32 起，到 endR 的 px+288 止 = 320 */
    private static final int BAR_LEFT = END_L_X;
    private static final int BAR_RIGHT = END_R_X + DECOR_SIZE;
    private static final int BAR_WIDTH = BAR_RIGHT - BAR_LEFT;
    /** 铭牌中心相对 px 的偏移：把 (BAR_LEFT, BAR_RIGHT) 的中点对齐屏幕中线 */
    private static final int BAR_CENTER = (BAR_LEFT + BAR_RIGHT) / 2;

    /** 端件的目标 y（相对 py） */
    private static final int END_Y = 16;
    /** 冠饰的目标 y（相对 py）：贴图在 plate 上方 */
    private static final int CROWN_Y = -28;
    /** 坠饰的目标 y（相对 py） */
    private static final int PENDANT_Y = 60;

    /**
     * 铭牌最低点相对 py 的偏移：坠饰底边 y+60+32。顶边为冠饰的 {@link #CROWN_Y}（y-28）。
     * 两者一起构成 120px 的纵向占位，兜底缩放按它算。
     */
    private static final int BAR_BOTTOM = PENDANT_Y + DECOR_SIZE;

    /**
     * 铭牌左上角的屏幕 y（逻辑值，未缩放）。
     * <p>取值依据：冠饰要落在 py-28 处，故 py 至少 28；留 4px 余量避开屏幕最上沿
     * （F3 调试文字第一行 / 原版顶部 HUD 的边距）取 32 —— 此时冠饰 y = 4..36、坠饰 y = 92..124，
     * 整条 120px 高、320px 宽，落在屏幕顶部中央，与原版 Boss 血条（y=12 起、宽 182）同区但不重叠自身。
     */
    private static final int PLATE_Y = 32;

    /** 名称净区（plate 内 y 5..21）在屏幕上的文字基线 y（相对 py）：(17 - 9) / 2 = 4 ⇒ py+5+4 */
    private static final int NAME_OFFSET_Y = 9;

    /** 兜底缩放的左右留白（逻辑像素）：缩到刚好不贴边 */
    private static final int EDGE_PADDING = 6;

    // ---------------------------------------------------------------- 目标选取
    /**
     * 客户端扫描半径（格）。取 96 = 6 区块，<b>依据</b>：
     * <ul>
     *   <li>本实体 {@code clientTrackingRange(12)} = 12 区块 = 192 格，即客户端最多能在 192 格内看到它；
     *       扫 96 格只会"漏掉 96~192 格之间那只"——那个距离上 BOSS 已是一个被雾遮住的小点，
     *       给它挂一条 320px 宽的大铭牌反而是误导（也与原版血条"隔半张地图还挂着一条"的观感一致地收敛）。</li>
     *   <li>反过来，任何被扫到的实体一定已在客户端世界里 = 已被服务端追踪，不会出现
     *       "玩家看不见 BOSS 却还挂着血条"。</li>
     *   <li>性能：96 格 = 12×12 个区块段，比按 192 扫少 4 倍的遍历量。逐帧一次，无缓存。</li>
     * </ul>
     */
    private static final double SCAN_RADIUS = 96.0D;

    // ---------------------------------------------------------------- 复活脉动（待调手感值）
    /** 复活/无敌期间的基础不透明度 */
    private static final float RESPAWN_ALPHA = 0.45F;
    /** 复活/无敌期间的正弦脉动幅度（±） */
    private static final float RESPAWN_PULSE_AMPLITUDE = 0.15F;
    /** 复活/无敌期间一个完整脉动周期的毫秒数 */
    private static final float RESPAWN_PULSE_PERIOD_MS = 1200.0F;

    /** 名称与百分比字色（仅 RGB，浅色、暗底可读；alpha 由复活脉动在绘制时按位或入高 8 位） */
    private static final int NAME_RGB = 0x00FFFFFF;
    private static final int PERCENT_RGB = 0x00E6F7FF;

    /**
     * 名称净区可用宽度（逻辑像素）。
     * <p>推算依据：plate 右上 x224..246 是留给百分比的角窗，百分比最宽（{@code "100%"}）
     * 时左缘落在 px+223；名称居中于 px+128，故单侧可用 223-128 = 95、双侧 190，再留 4px 余量取 186。
     * 超过这个宽度就退回只画名称 —— 宁可少画一段，也不让两段文字互相压字。
     */
    private static final int NAME_ZONE_WIDTH = 186;

    /** 名称与副标题的分隔符 */
    private static final String NAME_SEPARATOR = " · ";

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        // F1 隐藏 HUD 时一并隐藏（与原版 BOSS_EVENT_PROGRESS overlay 的 hideGui 门控保持一致）
        if (player == null || minecraft.options.hideGui) {
            return;
        }
        AgaitolosEntity boss = findNearest(player);
        // 血量归零 = 正在播死亡演出：此刻不该再挂一条血条（否则"血条满格却打不动"的误导，与旧实现同理）
        if (boss == null || boss.getHealth() <= 0.0F) {
            return;
        }

        // 阶段 → 图集。combatOrdinal 是 1/2/3，落 0（异常存档里的 RESPAWN）时钳到一阶段图，不越界不崩
        int textureIndex = Mth.clamp(boss.getPhase().combatOrdinal() - 1, 0, PHASE_TEXTURES.length - 1);
        ResourceLocation texture = PHASE_TEXTURES[textureIndex];

        float healthRatio = Mth.clamp(boss.getHealth() / Math.max(1.0F, boss.getMaxHealth()), 0.0F, 1.0F);
        // 比例 > 0 时至少 1px：否则剩最后几滴血（240 × 0.07% < 1）时轨道看起来像"已经空了"
        int fillWidth = healthRatio > 0.0F ? Math.max(1, (int) (TRACK_W * healthRatio)) : 0;

        // 逻辑坐标（未缩放）：把铭牌中心对齐屏幕中线
        int px = screenWidth / 2 - BAR_CENTER;
        int py = PLATE_Y;

        // 兜底缩放系数：先按"含留白能否容纳 320×120"算，再交给 PoseStack。
        // 不把系数揉进坐标常量：坐标保持可读的逻辑值，缩放只影响矩阵，
        // 于是 fill 宽度、文字居中、端件偏移全都仍是同一套换算，不会因缩放而错位。
        float fitWidth = (screenWidth - 2.0F * EDGE_PADDING) / (float) BAR_WIDTH;
        float fitHeight = (screenHeight - 2.0F * EDGE_PADDING) / (float) (PLATE_Y + BAR_BOTTOM);
        float scale = Math.min(1.0F, Math.min(fitWidth, fitHeight));

        // 复活/无敌阶段：降低不透明度 + 正弦脉动，表达"打不动"
        float alpha = 1.0F;
        if (boss.isRespawning()) {
            float progress = (Util.getMillis() % (long) RESPAWN_PULSE_PERIOD_MS) / RESPAWN_PULSE_PERIOD_MS;
            float pulse = Mth.sin(progress * Mth.TWO_PI);
            alpha = Mth.clamp(RESPAWN_ALPHA + RESPAWN_PULSE_AMPLITUDE * pulse, 0.05F, 1.0F);
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();
        if (scale != 1.0F) {
            // 以「屏幕中竖线 + 屏幕顶边」为不动点缩放：水平仍居中，垂直向上收，冠饰不会缩出屏幕
            pose.translate(screenWidth / 2.0F, 0.0F, 0.0F);
            pose.scale(scale, scale, 1.0F);
            pose.translate(-screenWidth / 2.0F, 0.0F, 0.0F);
        }

        // 贴图统一降透明度：GuiGraphics#setColor 走 RenderSystem.setShaderColor，
        // 而 GuiGraphics 的 blit 是"设 shader → BufferUploader.drawWithShader"立即绘制，
        // 因此这里设的 ColorModulator 对本段每一张图立即生效（不像文字那样缓入 bufferSource 延后 flush）。
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        drawBar(graphics, texture, px, py, fillWidth);
        // 立刻恢复全局 shader 颜色：文字是缓入 bufferSource、在 GUI 帧末才 flush 的，
        // 若把低透明度留到那时，会被叠加进文字的 ColorModulator（双重变暗）并污染后续 HUD。
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        drawText(minecraft.font, graphics, px, py, healthRatio, alpha);

        pose.popPose();
    }

    /** 单只最近目标：本帧找不到（或没有）就整条不画 */
    private static AgaitolosEntity findNearest(LocalPlayer player) {
        Level level = player.level();
        List<AgaitolosEntity> candidates = level.getEntitiesOfClass(AgaitolosEntity.class,
                player.getBoundingBox().inflate(SCAN_RADIUS));
        AgaitolosEntity nearest = null;
        double nearestSqr = Double.MAX_VALUE;
        for (AgaitolosEntity candidate : candidates) {
            double distanceSqr = candidate.distanceToSqr(player);
            if (distanceSqr < nearestSqr) {
                nearestSqr = distanceSqr;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /**
     * 严格按契约表叠放：plate → fill → rim → 两端件 → 冠饰 → 坠饰 → 四角件。
     * <p>rim 必须压在 fill 之上（fill 只是轨道内的进度色块，rim 是它的描边/压边）。
     */
    private static void drawBar(GuiGraphics graphics, ResourceLocation texture, int px, int py, int fillWidth) {
        // 1) 铭牌底板
        graphics.blit(texture, px, py, (float) PLATE_U, (float) PLATE_V, PLATE_W, PLATE_H, TEX_SIZE, TEX_SIZE);
        // 2) 进度填充：源 u 从 0 起、宽 = fillWidth，绘制宽度同为 fillWidth（1:1 不拉伸）
        if (fillWidth > 0) {
            graphics.blit(texture, px + TRACK_OFFSET_X, py + TRACK_OFFSET_Y,
                    0.0F, (float) FILL_V, fillWidth, TRACK_H, TEX_SIZE, TEX_SIZE);
        }
        // 3) 压边（最上层）
        graphics.blit(texture, px + TRACK_OFFSET_X, py + TRACK_OFFSET_Y,
                0.0F, (float) RIM_V, TRACK_W, TRACK_H, TEX_SIZE, TEX_SIZE);
        // 4) 左右端件
        graphics.blit(texture, px + END_L_X, py + END_Y, 0.0F, (float) DECOR_V, DECOR_SIZE, DECOR_SIZE, TEX_SIZE, TEX_SIZE);
        graphics.blit(texture, px + END_R_X, py + END_Y, 32.0F, (float) DECOR_V, DECOR_SIZE, DECOR_SIZE, TEX_SIZE, TEX_SIZE);
        // 5) 冠饰（上）与坠饰（下）
        graphics.blit(texture, px + PENDANT_X, py + CROWN_Y, 64.0F, (float) DECOR_V, 64, DECOR_SIZE, TEX_SIZE, TEX_SIZE);
        graphics.blit(texture, px + PENDANT_X, py + PENDANT_Y, 128.0F, (float) DECOR_V, 64, DECOR_SIZE, TEX_SIZE, TEX_SIZE);
        // 6) 四角件：源 x 依次 0/32/64/96（同一行），目标 (0,0)(224,0)(0,32)(224,32)
        for (int i = 0; i < 4; i++) {
            int cornerX = (i % 2 == 0) ? 0 : 224;
            int cornerY = (i < 2) ? 0 : DECOR_SIZE;
            graphics.blit(texture, px + cornerX, py + cornerY,
                    (float) (i * DECOR_SIZE), (float) CORNER_V, DECOR_SIZE, DECOR_SIZE, TEX_SIZE, TEX_SIZE);
        }
    }

    /**
     * 名称与血量百分比（用原版字体，不依赖贴图）。
     * <p>透明度<b>写进颜色 int 的 alpha 通道</b>而不是靠 shader 颜色：文字是缓入 bufferSource
     * 的，flush 时机在 GUI 帧末，那时 shader 颜色已被恢复成 1,1,1,1；而文字渲染类型带
     * TRANSLUCENT_TRANSPARENCY，顶点色 alpha 一定生效，是可靠做法。
     */
    private static void drawText(Font font, GuiGraphics graphics,
                                 int px, int py, float healthRatio, float alpha) {
        int alphaBits = Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24;
        // 名称：居中于 plate（plate 中心 = px + 128），落在净区 py+5..py+21 的中线上。
        // 副标题键 boss.akaishi.agaitolos.subtitle 是既有双语键、此前全项目无人引用（孤儿键），
        // 这里用「名称 · 副标题」单行形式消费掉；超宽则退回只画名称（贴图净区不动）
        Component name = Component.translatable("entity.akaishi.agaitolos");
        Component fullTitle = name.copy()
                .append(NAME_SEPARATOR)
                .append(Component.translatable("boss.akaishi.agaitolos.subtitle"));
        Component title = font.width(fullTitle) <= NAME_ZONE_WIDTH ? fullTitle : name;
        graphics.drawCenteredString(font, title, px + PLATE_W / 2, py + NAME_OFFSET_Y, alphaBits | NAME_RGB);
        // 百分比：右对齐到轨道右端内侧（px+8+240-1），用 ceil 保证"活着就不会显示 0%"
        String percent = Mth.ceil(healthRatio * 100.0F) + "%";
        int percentX = px + TRACK_OFFSET_X + TRACK_W - 1 - font.width(percent);
        graphics.drawString(font, percent, percentX, py + NAME_OFFSET_Y, alphaBits | PERCENT_RGB);
    }
}
