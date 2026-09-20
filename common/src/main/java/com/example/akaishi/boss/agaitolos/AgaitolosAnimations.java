package com.example.akaishi.boss.agaitolos;

import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

/**
 * 阿盖托洛丝的动画名常量与控制器注册。
 * <p>
 * 常量必须与 {@code assets/akaishi/animations/entity/agaitolos_stage1.animation.json} 中的 clip 名逐字一致，
 * 写错只会在实机渲染时抛 GeckoLibException，编译期查不出来。
 * <p>
 * GeckoLib 每个控制器同一时刻只能播一条 clip，故按用途拆成六个：
 * <ul>
 *   <li>{@link #CONTROLLER_MAIN} —— 常驻飞行待机；死亡、格挡架势、蓄力或出场演出时停播，把骨骼让给对应控制器</li>
 *   <li>{@link #CONTROLLER_ACTION} —— 触发式动作（普攻 / 召唤凋零头 / 受击），由服务端 {@code triggerAnim} 派发</li>
 *   <li>{@link #CONTROLLER_DEATH} —— 死亡动画，用 GeckoLib 推荐的「状态轮询」而非触发器</li>
 *   <li>{@link #CONTROLLER_GUARD} —— 格挡架势，同为状态轮询式（架势是状态，不是一次性动作）</li>
 *   <li>{@link #CONTROLLER_CHARGE} —— 恶怨倒转蓄力，同为状态轮询式（蓄力是状态，不是一次性动作）</li>
 *   <li>{@link #CONTROLLER_INTRO} —— 出场演出，同为状态轮询式（出场是 4s 的状态，不是一次性动作）</li>
 * </ul>
 * 动画名与控制器名都收在本类，实体侧只调语义方法（{@link #playAttack} 等），不硬写字符串。
 */
public final class AgaitolosAnimations {

    /** 常驻飞行待机（loop 4s） */
    public static final String IDLE_FLIGHT = "animation.agaitolos.idle_flight";
    /** 普攻挥舞 */
    public static final String ATTACK_SLASH = "animation.agaitolos.attack_slash";
    /** 召唤凋零头颅 */
    public static final String CAST_SKULL = "animation.agaitolos.cast_skull";
    /** 俯冲镰扫 */
    public static final String DIVE_SWEEP = "animation.agaitolos.dive_sweep";
    /** 蓄力（loop 2s） */
    public static final String CHARGE = "animation.agaitolos.charge";
    /** 格挡架势 */
    public static final String GUARD = "animation.agaitolos.guard";
    /** 受击 */
    public static final String HURT = "animation.agaitolos.hurt";
    /** 死亡 */
    public static final String DEATH = "animation.agaitolos.death";
    /**
     * 出场演出（PLAY_ONCE，4.0s = 80 tick）。
     * <p>⚠ 这条 clip 由建模任务产出，<b>名字必须逐字为 {@code animation.agaitolos.intro}</b>：
     * 写错只会在实机渲染时抛 GeckoLibException，编译期查不出来（同本类顶部说明）。
     */
    public static final String INTRO = "animation.agaitolos.intro";

    /** 主控制器名：常驻待机 */
    public static final String CONTROLLER_MAIN = "main";
    /** 动作控制器名：触发式动画（普攻 / 召唤 / 受击）共用 */
    public static final String CONTROLLER_ACTION = "action";
    /** 死亡控制器名：状态轮询式 */
    public static final String CONTROLLER_DEATH = "death";
    /** 格挡控制器名：状态轮询式（架势由 DATA_GUARDING 同步，两端各自判定） */
    public static final String CONTROLLER_GUARD = "guard";
    /** 蓄力控制器名：状态轮询式（蓄力由 DATA_CHARGING 同步，两端各自判定） */
    public static final String CONTROLLER_CHARGE = "charge";
    /** 出场控制器名：状态轮询式（出场由 DATA_INTRO 同步，两端各自判定） */
    public static final String CONTROLLER_INTRO = "intro";

    /** 控制器过渡时长（tick）：避免姿态硬切 */
    public static final int TRANSITION_TICKS = 5;

    /** 动作控制器过渡时长：动作本身只有 0.5~1.2s，过渡必须更短，否则首尾糊成一团 */
    public static final int ACTION_TRANSITION_TICKS = 2;

    /**
     * 触发键名。刻意与 clip 同名：{@code triggerAnim} 传错名字时不会报错、只会静默不播，
     * 同名时「动画没出来」与「json 里没这条 clip」能一眼对上。
     */
    private static final String TRIGGER_ATTACK = "attack_slash";
    private static final String TRIGGER_CAST = "cast_skull";
    private static final String TRIGGER_DIVE_SWEEP = "dive_sweep";
    private static final String TRIGGER_HURT = "hurt";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(IDLE_FLIGHT);
    // 四条动作 clip 在 json 里 loop 均为缺省（PLAY_ONCE）：thenPlay 播完自停，控制器随即交还给状态回调
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay(ATTACK_SLASH);
    private static final RawAnimation CAST_ANIM = RawAnimation.begin().thenPlay(CAST_SKULL);
    private static final RawAnimation DIVE_SWEEP_ANIM = RawAnimation.begin().thenPlay(DIVE_SWEEP);
    private static final RawAnimation HURT_ANIM = RawAnimation.begin().thenPlay(HURT);
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlay(DEATH);
    // guard 在 json 里是 1.2s 非循环 clip ⇒ 与其同步的 GUARD_DURATION_TICKS=24，thenPlay 正好播完一次
    private static final RawAnimation GUARD_ANIM = RawAnimation.begin().thenPlay(GUARD);
    // charge 在 json 里 loop=true（2s 循环）⇒ 必须用 thenLoop 而非 thenPlay，
    // 否则一个循环播完就会停在末帧，而蓄力要持续 12s
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenLoop(CHARGE);
    // 出场在 json 里是 PLAY_ONCE（4.0s = INTRO_DURATION_TICKS=80 tick，逐字对齐）
    // ⇒ 必须用 thenPlay：控制器与节拍都靠 DATA_INTRO 轮询，播完一遍刚好落在实体清状态那一 tick
    private static final RawAnimation INTRO_ANIM = RawAnimation.begin().thenPlay(INTRO);

    private AgaitolosAnimations() {
    }

    // ---------------------------------------------------------------- 语义播放入口

    /** 近战普攻命中：播一次挥砍。仅服务端调用，由 GeckoLib 自动同步给追踪客户端 */
    public static void playAttack(AgaitolosEntity boss) {
        boss.triggerAnim(CONTROLLER_ACTION, TRIGGER_ATTACK);
    }

    /** 发射凋零头颅：播一次施法。仅服务端调用，同步方式同上 */
    public static void playCast(AgaitolosEntity boss) {
        boss.triggerAnim(CONTROLLER_ACTION, TRIGGER_CAST);
    }

    /** 受击真正吃下伤害：播一次受击。仅服务端调用，同步方式同上 */
    public static void playHurt(AgaitolosEntity boss) {
        boss.triggerAnim(CONTROLLER_ACTION, TRIGGER_HURT);
    }

    /** 俯冲镰扫结算到人：播一次横扫（1.5s 非循环 clip）。仅服务端调用，同步方式同上 */
    public static void playDiveSweep(AgaitolosEntity boss) {
        boss.triggerAnim(CONTROLLER_ACTION, TRIGGER_DIVE_SWEEP);
    }

    // ---------------------------------------------------------------- 控制器注册

    /**
     * 注册动画控制器。
     * <p>
     * 死亡动画走「状态轮询」而非触发器：这是 GeckoLib 自己的推荐做法
     * （见 4.4.9 {@code DefaultAnimations#genericDeathController}：{@code isDeadOrDying() ? setAndContinue(DIE) : STOP}，transition 0）。
     * 好处是死亡状态由 {@code LivingEntity#isDeadOrDying()}（= 生命值 ≤ 0，原版本就同步）驱动，
     * 服务端/客户端各自都能判定，不需要额外的触发同步，也不需要改 renderer。
     */
    public static void registerControllers(AgaitolosEntity entity, AnimatableManager.ControllerRegistrar controllers) {
        // 常驻飞行待机；死亡 / 格挡架势 / 蓄力 / 出场演出时主动 STOP —— 多个控制器会同时驱动同一批骨骼，
        // 不停播会和死亡姿势 / 格挡架势 / 蓄力姿势 / 出场姿势互相拉扯（共用同一批骨骼，必须靠状态互斥）。
        // 出场也要列在这里：出场那 4s 一直在播，若 MAIN 不停，待机与降临动作会抢同一批骨骼。
        controllers.add(new AnimationController<>(entity, CONTROLLER_MAIN, TRANSITION_TICKS,
                state -> state.getAnimatable().isDeadOrDying() || state.getAnimatable().isGuarding()
                        || state.getAnimatable().isCharging() || state.getAnimatable().isIntroPlaying()
                        ? PlayState.STOP
                        : state.setAndContinue(IDLE)));

        // 动作控制器：状态回调只在「没有触发动画在播」时才会走到这里，此时无事可做 ⇒ STOP 等下一次 triggerAnim。
        // 不能返回 CONTINUE，否则控制器永远停不下来，触发式动画结束后会卡在最后一帧。
        controllers.add(new AnimationController<>(entity, CONTROLLER_ACTION, ACTION_TRANSITION_TICKS,
                state -> PlayState.STOP)
                .triggerableAnim(TRIGGER_ATTACK, ATTACK_ANIM)
                .triggerableAnim(TRIGGER_CAST, CAST_ANIM)
                .triggerableAnim(TRIGGER_DIVE_SWEEP, DIVE_SWEEP_ANIM)
                .triggerableAnim(TRIGGER_HURT, HURT_ANIM));

        // 死亡：transition 照上游取 0，死亡瞬间不需要过渡
        controllers.add(new AnimationController<>(entity, CONTROLLER_DEATH, 0,
                state -> state.getAnimatable().isDeadOrDying() ? state.setAndContinue(DEATH_ANIM) : PlayState.STOP));

        // 格挡架势：与死亡同理走「状态轮询」而不是触发器 —— 架势是持续状态（由 DATA_GUARDING 同步），
        // 两端各自轮询即可，不需要一次性的触发同步，实体侧也就不用加 playGuard 入口。
        // 过渡取动作级的 2 tick：姿态只有 24 tick，过渡太长会把起手/收手糊掉。
        controllers.add(new AnimationController<>(entity, CONTROLLER_GUARD, ACTION_TRANSITION_TICKS,
                state -> state.getAnimatable().isGuarding() ? state.setAndContinue(GUARD_ANIM) : PlayState.STOP));

        // 蓄力（恶怨倒转）：与 guard 同款「状态轮询」——蓄力是持续状态（由 DATA_CHARGING 同步），
        // 两端各自轮询即可，不需要一次性的触发同步，实体侧也就不用加 playCharge 入口。
        // 过渡同样取动作级的 2 tick：蓄力起手要利落，过渡太长会把"猛地举起球"糊掉。
        controllers.add(new AnimationController<>(entity, CONTROLLER_CHARGE, ACTION_TRANSITION_TICKS,
                state -> state.getAnimatable().isCharging() ? state.setAndContinue(CHARGE_ANIM) : PlayState.STOP));

        // 出场：与 guard/charge 同款「状态轮询」——出场是<b>持续状态</b>（固定 4s，由 DATA_INTRO 同步），
        // 不是"某刻触发一次"的动作，两端各自轮询即可，实体侧也就不需要 playIntro 触发入口。
        // 过渡取 0（而非动作级的 2）：召唤那一刻 idle 才刚起，没有任何既存姿态值得混合；
        // 用 2 tick 反而会把"从空中降临"的头两帧糊在待机姿态上，与召唤瞬间的粒子爆发错开。
        controllers.add(new AnimationController<>(entity, CONTROLLER_INTRO, 0,
                state -> state.getAnimatable().isIntroPlaying() ? state.setAndContinue(INTRO_ANIM) : PlayState.STOP));
    }
}
