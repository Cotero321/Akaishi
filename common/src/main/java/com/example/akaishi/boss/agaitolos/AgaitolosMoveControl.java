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
 * 刻意不消费 {@code wantedY}：常态低空悬停、不俯冲，高度完全由脚下地形决定
 * （俯冲镰扫的冲锋位移不在这里，见下）。
 * <p><b>三类例外</b>：
 * <ul>
 *   <li>冲锋中（{@code boss.isDiving()}）：整体让位，水平与垂直都不写，位移由 {@code AgaitolosEntity#tickDiveCharge} 驱动；</li>
 *   <li>禁飞中（{@code boss.isGrounded()}）：只做水平，垂直分量原样保留（交给 {@code LivingEntity#travel} 的重力把 BOSS 拉下地面）；</li>
 *   <li>格挡架势（{@code boss.isGuarding()}）：水平归零站定，垂直悬停照常；</li>
 *   <li>蓄力（{@code boss.isCharging()}，恶怨倒转）：同样水平归零站定、垂直悬停照常
 *       —— 理由与架势相同（"举球蓄力要站定"，且蓄力姿势不该一边飘一边放）。</li>
 * </ul>
 */
public class AgaitolosMoveControl extends MoveControl {

    /** 悬停高度（格）：以「脚下地面顶面」为基准的抬升量。手感值，待实机调 / P8 转配置项 */
    public static final double HOVER_HEIGHT = 2.0D;

    /** 水平加速度（格/tick，再乘 speedModifier 与阶段倍率后作用于水平速度）——待调手感值 */
    private static final double HORIZONTAL_ACCELERATION = 0.05D;

    /** 水平速度上限（格/tick）：空气阻力只有 0.91，不夹住会越飞越快；同样乘阶段倍率——待调手感值 */
    private static final double MAX_HORIZONTAL_SPEED = 0.25D;

    /** 到导航目标点的水平死区（格）：进入后不再加速，避免在目标点上方来回抖 */
    private static final double HORIZONTAL_DEAD_ZONE = 0.5D;

    /** 高度差 → 垂直速度的比例系数（再经 {@link #MAX_VERTICAL_SPEED} 限速） */
    private static final double VERTICAL_GAIN = 0.25D;

    /** 垂直速度上限（格/tick）：限制上升/下降速率，避免瞬移感；乘阶段倍率后阶段二三升降更快——待调手感值 */
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
        // ⓪ 冲锋中：<b>水平与垂直都完全不写</b>，整体让位。
        //    冲锋期间 deltaMovement 由 AgaitolosEntity#tickDiveCharge 每 tick 直接写（朝目标 3D 方向 × DIVE_SPEED）。
        //    若这里继续写：水平会被"导航目标点死区 + 限速"抹平，垂直会被 ③ 的悬停修正拉回 HOVER_HEIGHT，
        //    结果是俯冲永远贴近不了地面目标、抵达判定次次超时。
        //    与既有"架势掐断水平"同理，必须在控制器层让位 —— 本 tick 的 travel 在随后执行，实体侧改写来不及。
        if (isDiving()) {
            return;
        }

        Vec3 motion = this.mob.getDeltaMovement();
        double motionX = motion.x;
        double motionZ = motion.z;

        // ① 水平追击：导航每 tick 都会重新下发 setWantedPosition，故这里只按"当前是否有指令"处理，不把 operation 改回 WAIT。
        //    用 hasWanted() 而不是直接比 Operation：Operation 是 MoveControl 的 protected 嵌套枚举，跨包不便引用。
        //    格挡架势 / 蓄力例外：两者都要站定，既不产生水平加速、也抹掉残留惯性（垂直悬停照常，见 ③）。
        //    必须在这一层改而不能在实体侧改：MoveControl#tick 由 Mob#serverAiStep 调用、位于 travel 之前，
        //    而 travel 已经在 super.aiStep() 内部执行过，实体侧 post-super 清速度对当帧无效。
        if (isGuarding() || isCharging()) {
            motionX = 0.0D;
            motionZ = 0.0D;
        } else if (this.hasWanted()) {
            double deltaX = this.wantedX - this.mob.getX();
            double deltaZ = this.wantedZ - this.mob.getZ();
            double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (horizontal > HORIZONTAL_DEAD_ZONE) {
                double acceleration = HORIZONTAL_ACCELERATION * this.speedModifier * pace();
                motionX += deltaX / horizontal * acceleration;
                motionZ += deltaZ / horizontal * acceleration;
            }
        }

        // ② 水平限速（加速度 + 空气阻力的平衡速度会超过上限，这里夹住）；上限同样乘阶段倍率
        double maxHorizontalSpeed = MAX_HORIZONTAL_SPEED * pace();
        double horizontalSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        if (horizontalSpeed > maxHorizontalSpeed) {
            double scale = maxHorizontalSpeed / horizontalSpeed;
            motionX *= scale;
            motionZ *= scale;
        }

        // ③ 垂直悬停：无移动指令时也执行（否则会随空气阻力缓慢丢失高度）
        double motionY;
        if (isGrounded()) {
            // 禁飞（被格挡惩罚）：<b>不写垂直分量</b>，原样保留 travel 里累积的重力速度，让 BOSS 自然下坠。
            // 这里绝不能写 0：本控制器在 travel 之前执行、且每 tick 都会 setDeltaMovement，
            // 写 0 会把上一 tick 累积的下坠速度清零 ⇒ 重力永远积不起来，BOSS 会一直挂在半空，禁飞形同虚设。
            motionY = motion.y;
        } else {
            motionY = 0.0D;
            double hoverY = this.hoverY();
            if (!Double.isNaN(hoverY)) {
                double offset = hoverY - this.mob.getY();
                if (Math.abs(offset) > VERTICAL_DEAD_ZONE) {
                    double maxVerticalSpeed = MAX_VERTICAL_SPEED * pace();
                    motionY = Mth.clamp(offset * VERTICAL_GAIN, -maxVerticalSpeed, maxVerticalSpeed);
                }
            }
        }

        this.mob.setDeltaMovement(motionX, motionY, motionZ);
    }

    /**
     * 所属生物是否正在俯冲冲锋。为 true 时本控制器整体让位（见 {@link #tick()} ⓪）。
     * <p>{@code instanceof} 仅作类型兜底（防将来被复用到别的生物上），同 {@link #isGuarding()}。
     */
    private boolean isDiving() {
        return this.mob instanceof AgaitolosEntity boss && boss.isDiving();
    }

    /**
     * 所属生物是否被禁飞（被玩家格挡后 30s）。为 true 时停止悬停修正，交给重力。
     * <p>禁飞结束无需额外处理：本方法转 false 后 ③ 会照常把 BOSS 收敛回
     * 「脚下地面顶面 + {@link #HOVER_HEIGHT}」，即自动升回常态悬停高度。
     */
    private boolean isGrounded() {
        return this.mob instanceof AgaitolosEntity boss && boss.isGrounded();
    }

    /**
     * 所属生物是否正在摆格挡架势。
     * <p>本控制器只装配给 {@link AgaitolosEntity}，{@code instanceof} 仅作类型兜底（防将来被复用到别的生物上）。
     */
    private boolean isGuarding() {
        return this.mob instanceof AgaitolosEntity boss && boss.isGuarding();
    }

    /**
     * 所属生物是否正在「恶怨倒转」蓄力。为 true 时水平归零站定（垂直悬停照常）。
     * <p>与 {@link #isGuarding()} 同一机制、同一理由：蓄力要举球站定，不能一边飘一边蓄。
     * <p>{@code instanceof} 仅作类型兜底（防将来被复用到别的生物上）。
     */
    private boolean isCharging() {
        return this.mob instanceof AgaitolosEntity boss && boss.isCharging();
    }

    /**
     * 当前阶段的移动速度倍率（{@link AgaitolosPace#moveSpeed(AgaitolosEntity)}）：
     * 规格"二阶段比一阶段更加快速"的<b>唯一</b>落点之一（另一处是冲锋速度，见 {@code AgaitolosEntity#tickDiveCharge}）。
     * <p>阶段一与复活阶段恒为 1.0 ⇒ 乘算后与原值逐位相同，一阶段手感不受影响。
     * <p>{@code instanceof} 兜底：非本 BOSS 生物回退 1.0（将来若复用本控制器不会莫名提速）。
     */
    private double pace() {
        return this.mob instanceof AgaitolosEntity boss ? AgaitolosPace.moveSpeed(boss) : 1.0D;
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
