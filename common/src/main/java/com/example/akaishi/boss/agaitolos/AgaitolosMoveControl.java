package com.example.akaishi.boss.agaitolos;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 阿盖托洛丝的低空飞行移动控制器：<b>低空悬停巡航 + 直线追击</b>，刻意不做寻路/避障（设计文档明确不追求）。
 * <p>
 * 蓝本 = 原版 {@code Ghast.GhastMoveControl}（飞行怪直接改写 {@code deltaMovement} 的最小做法）。
 * 每 tick 只做三件事：
 * <ol>
 *   <li><b>水平</b>：导航（{@code MeleeAttackGoal} / {@code RangedAttackGoal} 经 {@code FlyingPathNavigation}）
 *       下发移动指令时朝目标点的水平方向加速，加速度按 {@code speedModifier} 缩放；</li>
 *   <li><b>垂直</b>：无论有无指令都恒定维持「脚下地面顶面 + {@link #HOVER_HEIGHT}」的悬停高度；</li>
 *   <li>把结果写回 {@code deltaMovement}，由 {@code AgaitolosEntity#travel} 消费（该 travel 不含重力项）。</li>
 * </ol>
 * 刻意不消费 {@code wantedY}：本 BOSS 常态低空悬停、不俯冲，高度完全由脚下地形决定。
 */
public class AgaitolosMoveControl extends MoveControl {

    /** 悬停高度（格）：以「脚下地面顶面」为基准的抬升量。手感值，待实机调 / P8 转配置项 */
    public static final double HOVER_HEIGHT = 2.0D;

    /** 水平加速度（格/tick，再乘 speedModifier 后作用于水平速度）——待调手感值 */
    private static final double HORIZONTAL_ACCELERATION = 0.05D;

    /** 水平速度上限（格/tick）：空气阻力只有 0.91，不夹住会越飞越快——待调手感值 */
    private static final double MAX_HORIZONTAL_SPEED = 0.25D;

    /** 到导航目标点的水平死区（格）：进入后不再加速，避免在目标点上方来回抖 */
    private static final double HORIZONTAL_DEAD_ZONE = 0.5D;

    /** 高度差 → 垂直速度的比例系数（再经 {@link #MAX_VERTICAL_SPEED} 限速） */
    private static final double VERTICAL_GAIN = 0.25D;

    /** 垂直速度上限（格/tick）：限制上升/下降速率，避免瞬移感 */
    private static final double MAX_VERTICAL_SPEED = 0.2D;

    /** 高度差死区（格）：差量小于此值即视为已在悬停位，垂直速度归零以抑制抖动 */
    private static final double VERTICAL_DEAD_ZONE = 0.05D;

    /** 向下探测脚下地面的最大格数；范围内找不到地面（虚空）则本 tick 不做垂直修正 */
    private static final int GROUND_SCAN_RANGE = 16;

    public AgaitolosMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        Vec3 motion = this.mob.getDeltaMovement();
        double motionX = motion.x;
        double motionZ = motion.z;

        // ① 水平追击：导航每 tick 都会重新下发 setWantedPosition，故这里只按"当前是否有指令"处理，不把 operation 改回 WAIT。
        //    用 hasWanted() 而不是直接比 Operation：Operation 是 MoveControl 的 protected 嵌套枚举，跨包不便引用。
        if (this.hasWanted()) {
            double deltaX = this.wantedX - this.mob.getX();
            double deltaZ = this.wantedZ - this.mob.getZ();
            double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (horizontal > HORIZONTAL_DEAD_ZONE) {
                double acceleration = HORIZONTAL_ACCELERATION * this.speedModifier;
                motionX += deltaX / horizontal * acceleration;
                motionZ += deltaZ / horizontal * acceleration;
            }
        }

        // ② 水平限速（加速度 + 空气阻力的平衡速度会超过上限，这里夹住）
        double horizontalSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontalSpeed > MAX_HORIZONTAL_SPEED) {
            double scale = MAX_HORIZONTAL_SPEED / horizontalSpeed;
            motionX *= scale;
            motionZ *= scale;
        }

        // ③ 垂直悬停：无移动指令时也执行（否则会随空气阻力缓慢丢失高度）
        double motionY = 0.0D;
        double hoverY = this.hoverY();
        if (!Double.isNaN(hoverY)) {
            double offset = hoverY - this.mob.getY();
            if (Math.abs(offset) > VERTICAL_DEAD_ZONE) {
                motionY = Mth.clamp(offset * VERTICAL_GAIN, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);
            }
        }

        this.mob.setDeltaMovement(motionX, motionY, motionZ);
    }

    /**
     * 计算本 tick 的悬停目标 Y（实体脚部坐标）。
     * <p>
     * 从脚部所在方块向下限程扫描，取第一个「有碰撞形状或含流体」的方块作为脚下地面
     * （判据与原版 {@code Heightmap.Types.MOTION_BLOCKING_NO_LEAVES} 同一口径），目标高度 = 该方块顶面 Y + {@link #HOVER_HEIGHT}。
     * <p>
     * 用向下扫描而不是 {@code Level#getHeight(Heightmap.Types, int, int)}：后者取的是整列最高遮挡方块，
     * 在地下/洞穴里会返回地表 Y，把 BOSS 往天上拉。
     *
     * @return 悬停目标 Y；{@link #GROUND_SCAN_RANGE} 格内无地面（虚空/深井）时返回 {@link Double#NaN}，表示本 tick 不修正高度
     */
    private double hoverY() {
        Level level = this.mob.level();
        int x = Mth.floor(this.mob.getX());
        int z = Mth.floor(this.mob.getZ());
        int lowestY = Math.max(level.getMinBuildHeight(), Mth.floor(this.mob.getY()) - GROUND_SCAN_RANGE);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = Mth.floor(this.mob.getY()); y >= lowestY; --y) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            // 无碰撞（空气、草、火把…）且不含流体 ⇒ 不是地面，继续往下找
            if (state.getCollisionShape(level, cursor).isEmpty() && state.getFluidState().isEmpty()) {
                continue;
            }
            return (y + 1) + HOVER_HEIGHT; // 该方块顶面 = y + 1
        }
        return Double.NaN;
    }
}
