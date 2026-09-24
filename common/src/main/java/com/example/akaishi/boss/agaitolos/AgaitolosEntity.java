package com.example.akaishi.boss.agaitolos;

import com.example.akaishi.boss.agaitolos.arena.NetherPrisonArena;
import com.example.akaishi.boss.agaitolos.entity.AgaitolosWitherSkull;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosBlinkSkill;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosDiveSweepSkill;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosGuardSkill;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosKickSkill;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosMeleeSkill;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosMinionSkill;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosReversalSkill;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosSkullSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.UUID;

/**
 * 阿盖托洛丝【下界本源】实体主体。
 * <p>
 * P1 交付骨架（属性 + 受击管线 + GeckoLib 可见渲染），P3 在本类上补<b>编排</b>：
 * 阶段机（阈值判定 → 推进）、复活阶段（无敌 + 回血 + 结束击飞）；
 * P5 再补<b>二阶段两招</b>（瞬击 / 高速踢击）与「二阶段比一阶段更快速」的阶段节奏倍率
 * （倍率表收在 {@link AgaitolosPace}，本类只接线）。
 * 阶段独立成类（{@link AgaitolosPhase}），本类只做编排，不把逻辑堆成巨型类。
 * <p>
 * <b>血条不由本类持有</b>：显示职责整体移交客户端自定义 overlay（forge 侧
 * {@code AgaitolosBossBarOverlay}）。原版 {@code ServerBossEvent} 血条路线（早期那个只做参与者对账的
 * 服务端血条类）已整体删除，本目录下不再有任何服务端血条代码 ——
 * 它既画不出需求里的铭牌贴图与阶段换皮，留着还会与自定义血条<b>同时显示两条</b>，
 * 属于典型「看得到用不到」的残留。本类只负责把阶段/复活状态同步出去供客户端读取。
 * <p>
 * <b>2026-09-21 补：战斗决策层与"不还手"根因修复</b>
 * <ul>
 *   <li><b>根因</b>：原版 {@code MeleeAttackGoal} 判可达距离时只用自己的 {@code getBbWidth()}²
 *       （实测字节码，不读 {@link #getMeleeAttackRangeSqr}），而本 BOSS 常态悬停在玩家上方 2 格 ⇒
 *       光竖直差就 4.0 &gt; 3.84，<b>近战一次都挥不出</b>；同时远程的 {@code RangedAttackGoal}
 *       与近战 Goal 抢同一组 Flag、优先级又更低 ⇒ 被永久饿死。两者叠加 = 玩家贴脸打它、它只会飘着。
 *       修法见 {@link AgaitolosMeleeAttackGoal}（近战判据）+ {@code AgaitolosSkillDirector}（远程改由决策层放）。</li>
 *   <li><b>决策层</b>：起手判定从各 {@code tickXxx} 的顺次闸整体上移到 {@link AgaitolosSkillDirector}
 *       （距离分层 + 权重随机 + 全局动作锁 + 阶段/玩家状态修正）；本类只保留<b>进行中状态</b>
 *       （{@code tickChargeState}/{@code tickGuardState}/{@code tickDiveState}）与<b>执行入口</b>
 *       （{@code startGuard}/{@code startCharge}/{@code startDiveSweep}/{@code startBlink}/{@code startKick}）。</li>
 *   <li><b>报复与脱战</b>：{@link #hurt} 一进来就 {@link #retaliate}（被打即锁定攻击者）+
 *       目标选择器新增 {@code HurtByTargetGoal}；脱战低空巡逻、卡住重寻路都在决策层里。</li>
 * </ul>
 * <p>
 * <b>2026-09-21 补（第二轮：生存实测四项反馈）</b>
 * <ul>
 *   <li><b>召唤太频繁</b>：{@link #MINION_COOLDOWN_TICKS} 300 → 900（阶段分化仍走 {@link AgaitolosPace}，
 *       见该常量的改前/改后表）；叠加"整队回收"后场上不再叠罗汉。</li>
 *   <li><b>召唤物内斗</b>：{@code AgaitolosMinionSkill} 用原版 Team 把 BOSS 与召唤物放进同一支队伍
 *       （原版唯一的友军判定），并让 BOSS 自己的凋零头不再命中自家召唤物。</li>
 *   <li><b>技能结束召唤物不消失</b>：四个出口统一调 {@code AgaitolosMinionSkill#dismissAll}
 *       —— {@link #endChargeCompleted()} / {@link #endChargeInterrupted()} / {@link #enterRespawn()} /
 *        {@link #die} 与 {@link #remove}（区块卸载那一支由召唤物自身的看门狗
 *       {@code AgaitolosMinion} 兜）。</li>
 *   <li><b>多待在地上</b>：新增<b>地面档</b> {@link #isPerched()}（近身缠斗时贴地，最短 3s、最迟 10s 后升空），
 *       高度消费在 {@code AgaitolosMoveControl#PERCH_HEIGHT}，切换节律见 {@link #tickPerchState()}。</li>
 *   <li><b>贴地不再播飞行待机（第三轮）</b>：地面档三件套 clip（{@code land} / {@code idle_ground} /
 *       {@code takeoff}）已接入动画 MAIN 控制器，客户端靠同步状态 {@link #getPerchState()} 分支，
 *       见 {@code AgaitolosAnimations#groundAwareIdle}。</li>
 * </ul>
 * <p>
 * <b>2026-09-21 补（阶段三三项"无新动画"内容）</b>
 * <ul>
 *   <li><b>凋亡</b>：阶段三起，BOSS 的三处"上凋零"全部改施加 {@code akaishi:doom}
 *       （统一入口 {@link AgaitolosDoom}，读凋零的结算也一并认凋亡），效果定义见 {@code DoomEffect}；</li>
 *   <li><b>免疫远程</b>：见 {@link #isProjectileImmune()} 与 {@link AgaitolosDamageRules#resolve} 的第 ⑥ 闸
 *       （弹射物完全免伤，但"被玩家打回来的凋零头"走自伤通道、不受此闸约束）；</li>
 *   <li><b>超低空悬停</b>：阶段三空中档降到 {@code AgaitolosMoveControl#PHASE_3_HOVER_HEIGHT}（2.0 → 1.0），
 *       近战可达尺子仍按<b>最大</b>空中档折算（见 {@link #getMeleeAttackRangeSqr}）。</li>
 * </ul>
 * <p>
 * <b>2026-09-21 补（阶段三五招：真有 clip 了）</b>
 * <ul>
 *   <li><b>五条新 clip</b>（{@code triple_throw} / {@code grab_sweep} / {@code grab_smash} /
 *       {@code calamity_cast} / {@code bombard}）接进既有控制器：四条一次性动作复用 ACTION 触发式，
 *       循环的 {@code bombard} 另开第七个<b>状态轮询</b>控制器（同步位 {@code DATA_BOMBARDING}）；</li>
 *   <li><b>状态机收在 {@code AgaitolosPhaseThreeState}</b>：三连出手节拍 / 高空轰炸 /
 *       抓取态（踩住）/ 劈击 / 施法，本类只保留同步位、转发入口与每 tick 一次推进；</li>
 *   <li><b>起手仍归决策层</b>：五招以"仅阶段三 + 吃大招封印 + 各自的距离层"接进
 *       {@code AgaitolosSkillDirector} 的权重表，保命招硬闸与距离分层一字未动；</li>
 *   <li><b>精神伤害改写</b>（天魔＊灾）：唯一口径在 {@code AgaitolosPsychic}，
 *       玩家侧持久标记落在既有 capability（死亡/换维度/重启都不丢）。</li>
 * </ul>
 */
public class AgaitolosEntity extends Monster implements GeoEntity, RangedAttackMob {

    /** 同步数据：当前阶段序号（0/1/2 = PHASE_1/2/3）；客户端 P7 起据此选模型与动画 */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.INT);

    /** 同步数据：是否处于复活阶段；客户端据此切演出（P7）与模型复位 */
    private static final EntityDataAccessor<Boolean> DATA_RESPAWNING =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.BOOLEAN);

    /** 同步数据：是否处于格挡架势；动画的 guard 控制器两端各自轮询它（无需额外触发同步） */
    private static final EntityDataAccessor<Boolean> DATA_GUARDING =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.BOOLEAN);

    /** 同步数据：是否处于「恶怨倒转」蓄力；动画的 charge 控制器两端各自轮询它（同 DATA_GUARDING 的分工） */
    private static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * 同步数据：是否正在播出场演出。
     * <p>与架势 / 蓄力同属<b>状态轮询式</b>，故走同步数据而不是一次性触发同步：出场是一个持续 4s 的状态，
     * 两端各自读它来驱动动画控制器（见 {@code AgaitolosAnimations#CONTROLLER_INTRO}），
     * 服务端结算（位移/闸门）也读同一份真源，不需要额外的触发包。
     */
    private static final EntityDataAccessor<Boolean> DATA_INTRO =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * 同步数据：<b>地面档展示状态</b>（四态，见 {@link #PERCH_STATE_AIRBORNE} 等常量）。
     * <p>
     * 与架势 / 蓄力 / 出场同属<b>状态轮询式</b>：状态由服务端写、客户端读（{@link #getPerchState()}），
     * 动画的 MAIN 控制器两端各自轮询同一份真源（见 {@code AgaitolosAnimations#groundAwareIdle}），
     * 不需要一次性的触发同步，实体侧也就不必加 playXxx 入口。
     * <p>
     * <b>为什么不直接同步 {@link #isPerched()} 那个布尔</b>：高度档布尔只够驱动位移（空中 2 格 / 贴地 0 格），
     * 而动画还要知道"正在落地过渡还是正在升空过渡"才能播 land / takeoff 两条 0.6s clip；
     * 用四态 int 一个 accessor 同时表达"稳态/过渡"与"方向"，且整轮节律里只写 4 次（见 {@link #tickPerchState()}）。
     */
    private static final EntityDataAccessor<Integer> DATA_PERCH_STATE =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.INT);

    /**
     * 同步数据：是否正在「饱和轰炸」（阶段三）。
     * <p>与架势 / 蓄力 / 出场同属<b>状态轮询式</b>：轰炸是一段最长 {@code BOMBARD_DURATION_TICKS} 的持续状态，
     * 客户端动画控制器轮询它来驱动<b>那条 loop clip</b>（见 {@code AgaitolosAnimations#CONTROLLER_BOMBARD}）
     * —— 触发式播放对循环 clip 没有停止口，故只能走状态。
     * <p>写方只有一个：{@code AgaitolosPhaseThreeState}（起手立、期满/中止撤），故两端不会读到中间态。
     */
    private static final EntityDataAccessor<Boolean> DATA_BOMBARDING =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.BOOLEAN);

    /** 复活阶段时长：4s = 80 tick（用户拍板；期间回满生命，结束时击飞周围玩家） */
    public static final int RESPAWN_DURATION_TICKS = 80;

    /** 复活结束的击飞半径（格）——待调手感值 */
    public static final double RESPAWN_KNOCKBACK_RADIUS = 12.0D;

    /** 复活结束的水平击飞力度——待调手感值 */
    public static final double RESPAWN_KNOCKBACK_HORIZONTAL = 1.2D;

    /** 复活结束的垂直上抛力度——待调手感值 */
    public static final double RESPAWN_KNOCKBACK_VERTICAL = 0.9D;

    /** 俯冲镰扫的冲锋起手最大水平距离（格）：更远就交给飞行巡航接近。待调手感值 / P8 转配置项 */
    public static final double DIVE_TRIGGER_RANGE = 16.0D;

    /** 俯冲镰扫的冲锋起手最小水平距离（格）：太近就不俯冲，交给普攻/格挡。待调手感值 / P8 转配置项 */
    public static final double DIVE_MIN_RANGE = 3.0D;

    /** 冲锋最长持续（tick）：1s。到点无论是否抵达都收尾并结算一次横扫，避免冲锋把其它行为锁死。待调手感值 / P8 转配置项 */
    public static final int DIVE_MAX_TICKS = 20;

    /**
     * 冲锋速度（格/tick）：1.2。
     * <p>取值依据：规格要求"<b>快速</b>飞行至玩家处"，起手距离上限 16 格，需在 1 秒（20 tick）内跨越十几格
     * ⇒ 均速需 ≥ 16 / 20 = 0.8 格/tick；取 1.2 留出末段转向与绕障余量（16 格约 13 tick 抵达），
     * 又不至于瞬移（≈24 格/s，快于末影珍珠但可目视追踪）。待调手感值 / P8 转配置项
     */
    public static final double DIVE_SPEED = 1.2D;

    /** 冲锋抵达判定：与目标点的水平距离 ≤ 该值（格）即视为"已到玩家处"。待调手感值 / P8 转配置项 */
    public static final double DIVE_ARRIVE_DISTANCE = 1.0D;

    /** 被玩家格挡成功后的禁飞时长（tick）：30s（规格明确）。待调手感值 / P8 转配置项 */
    public static final int GROUNDED_DURATION_TICKS = 600;

    /**
     * 被玩家格挡成功后的镰扫封印时长（tick）：与禁飞同窗 30s。
     * <p>刻意做成<b>限时</b>而非永久：设计文档 §0 规定「二阶段拥有一阶段所有技能」，
     * 若永久封印，后续阶段会永久少掉"俯冲镰扫"这一招、破坏阶段契约；
     * 限时封印既保留"格挡成功有实际收益"的惩罚语义，又不至于让后续阶段少一招。待调手感值 / P8 转配置项
     */
    public static final int SCYTHE_SEAL_TICKS = 600;

    /** 恶怨倒转的蓄力时长（tick）：240 = 12s（规格明确：蓄力状态存在 12s） */
    public static final int CHARGE_DURATION_TICKS = 240;

    /**
     * 出场压血比例：25% —— <b>待调手感值 / P8 转配置项</b>。
     * <p>取值依据：规格 §0 要求「召唤之后进入复活阶段快速回复血量」，但实体生成时血量本来就是满的，
     * 复活阶段的每 tick {@code heal} 会被原版按最大生命夹取，画面上<b>什么都看不到</b>。
     * 故开场先把血量压到该比例，再交给复活阶段在 4s 内回满 —— 这段"快速回复"才真正可见。
     * 只压这一次（见 {@link #aiStep}）：50% / 25% 处的常规阶段推进不压血，保持原有口径。
     */
    public static final float INTRO_HEALTH_RATIO = 0.25F;

    // ---------------------------------------------------------------- 出场演出编排（首次召唤，0~80t）

    /**
     * 出场演出总时长（tick）：<b>严格等于复活阶段</b>（{@link #RESPAWN_DURATION_TICKS} = 80 = 4.0s）。
     * <p>为什么直接引用而不是再写一个 80：两者必须同窗——首次召唤那一 tick 同时进复活阶段与出场演出，
     * 演出结束时血量刚回满、复活的无敌与击飞也刚好收尾，玩家看到的是"BOSS 从空中降临并回满血"这一件事。
     * 各写一份常量迟早会漂移成"降临完了还在回血"或"血回满了还在空中"。
     * <p>同时也是 {@code animation.agaitolos.intro} clip 的长度（4.0s）：改 clip 长度要同步改这里。待调手感值 / P8 转配置项
     */
    public static final int INTRO_DURATION_TICKS = RESPAWN_DURATION_TICKS;

    /** 出场 ① 段（粒子柱）的结束 tick：0~20t。待调手感值 / P8 转配置项 */
    public static final int INTRO_PILLAR_END_TICKS = 20;

    /**
     * 出场 ② 段（降临位移）的时长（tick）：0~40t 从"悬停高度上方 {@link #INTRO_DESCENT_HEIGHT} 格"降到悬停高度。
     * <p>这个常量同时是 ③ 段的起点（落地冲击环在降临完成那一 tick 起爆）——刻意只留一份分界，
     * 两处各写 40 迟早会漂移成"还没落地就起环"。待调手感值 / P8 转配置项
     */
    public static final int INTRO_DESCENT_TICKS = 40;

    /** 出场 ③ 段（落地冲击环）的结束 tick：40~60t（起点复用 {@link #INTRO_DESCENT_TICKS}）。待调手感值 / P8 转配置项 */
    public static final int INTRO_SHOCK_END_TICKS = 60;

    /**
     * 降临起点相对悬停高度的抬升量（格）：3.5。
     * <p>起点 = 本 tick 的<b>真实悬停高度</b> + 该值（不是"当前坐标 + 该值"）：悬停高度由
     * {@link AgaitolosMoveControl#hoverY} 唯一计算，这样 40t 内落到的终点就是 BOSS 之后该待的位置，
     * 演出结束时 MoveControl 接手不会再有"落地又弹一下"的二次修正。待调手感值 / P8 转配置项
     */
    public static final double INTRO_DESCENT_HEIGHT = 3.5D;

    /**
     * 死亡演出时长（tick）：50 = 2.5s，<b>与 death clip 时长对齐 —— 改 clip 长度要同步改这里</b>。
     * <p>原版 {@code LivingEntity#tickDeath()} 在 {@code deathTime >= 20}（1s）就 {@code remove(KILLED)}，
     * 2.5s 的死亡动画只能看到前 40%；本类把收尾门槛抬到本常量（见 {@link #tickDeath()}）。待调手感值 / P8 转配置项
     */
    public static final int DEATH_TICKS = 50;

    /**
     * 蓄力自发光（GLOWING）的药水时长：蓄力时长 + 2 的余量。
     * <p>刻意比蓄力长一点：药水与蓄力同刻起算、每 tick 递减，留余量才不会在蓄力最后几帧提前熄灭；
     * 蓄力被打断或自然结束都由 {@link #finishCharge()} 主动摘除，不依赖自然过期。
     */
    private static final int CHARGE_GLOW_DURATION_TICKS = CHARGE_DURATION_TICKS + 2;

    /**
     * 「恶怨倒转」被打断所需的<b>累计承伤比例</b>：6%（阈值 = {@code getMaxHealth() × 该比例}，
     * 1444 血时 ≈ 87 点）。<b>待调手感值 / P8 转配置项</b>
     * <p>
     * <b>为什么是 6%（用户拍板：单人必须能打断）</b>：本 BOSS 受击冷却只有 4 tick
     * （{@code AgaitolosDamageRules.HURT_COOLDOWN_TICKS}），12s 内理论上有 240 / 4 = 60 个命中窗口，
     * 单次落地还要过 60% 减伤（{@code DAMAGE_MULTIPLIER}），下界合金剑 + 锋利 V（原始 11）落地约 4.4、
     * 跳跃暴击约 6.6；但真正卡住单人的是<b>玩家攻击速度</b>（1.6 ⇒ 12.5 tick/刀）：
     * 240 / 12.5 ≈ 19 刀 × 6.6 ≈ <b>125 点</b> 才是单人 12s 的实际上限（装备差些或要走位则更低）。
     * <p>
     * ⇒ 阈值取 6%（≈87，约 7 次落地）让单人<b>稳定可达</b>，同时给走位与躲小怪留约 30% 余量；
     * 若取 2%（≈29）则一碰就断、蓄力形同虚设。历史取值 25%（≈361）是"团队阈值"（单人打不断），
     * 已按用户口径废弃。若要回调：0.04F（≈58）更易、0.09F（≈130）更难。
     * <p>
     * <b>已知代价</b>：多人同场时几乎必然被打断 —— 这是"单人可打断"口径的必然结果，不是缺陷。
     */
    public static final float CHARGE_BREAK_DAMAGE_RATIO = 0.06F;

    /**
     * 恶怨倒转的冷却（tick）：900 = 45s，**在蓄力结束后**才开始计时。<b>待调手感值 / P8 转配置项</b>
     * <p>
     * <b>2026-09-21 由 300（15s）上调到 900（45s）</b>，理由（用户实测反馈"召唤怪物太频繁了"，两条根因）：
     * <ol>
     *   <li><b>基准太短</b>：本招在"蓄力 12s（{@link #CHARGE_DURATION_TICKS}）之后"才起算冷却，
     *       故改前一轮完整的间隔只有 12+15 = 27s（阶段二三经 {@link AgaitolosPace#cooldownScale} 再缩到
     *       23.25s / 21s）——<b>每 20 秒就往场上多塞 10 只凋零骷髅</b>，观感必然是"一直在招"。</li>
     *   <li><b>旧分队不消失（已同时修）</b>：技能结束不清场时，冷却到点就能在旧分队头上再招一支，
     *       场上会叠成 20、30 只（问题叠加放大）。清场落点见 {@code AgaitolosMinionSkill#dismissAll}。</li>
     * </ol>
     * <p>
     * <b>阶段差异化沿用既有唯一倍率表</b>：本常量仍走 {@link AgaitolosPace#scaledCooldown}，
     * 不另立"召唤专用节奏"，故与决策层的出手节拍（{@code AgaitolosSkillDirector#beatTicksFor}）同源、不会互相打架：
     * <table border="1">
     *   <caption>改前 / 改后</caption>
     *   <tr><th>阶段</th><th>冷却系数</th><th>改前冷却</th><th>改前整轮间隔</th><th>改后冷却</th><th>改后整轮间隔</th></tr>
     *   <tr><td>一</td><td>1.00</td><td>15.0s</td><td>27.0s</td><td>45.0s</td><td><b>57.0s</b></td></tr>
     *   <tr><td>二</td><td>0.75</td><td>11.25s</td><td>23.25s</td><td>33.75s</td><td><b>45.75s</b></td></tr>
     *   <tr><td>三</td><td>0.60</td><td>9.0s</td><td>21.0s</td><td>27.0s</td><td><b>39.0s</b></td></tr>
     * </table>
     * 即"一阶段最少、二/三阶段略多"（用户口径），且每一轮都给玩家留足清场与喘息窗口。
     * 若要回调：600 ⇒ 一阶段整轮 42s（更凶），1200 ⇒ 66s（更松）。
     */
    public static final int MINION_COOLDOWN_TICKS = 900;

    /**
     * 主动索敌锁定后、<b>失去视线仍继续追击</b>的容忍时长（tick）：200 = 10s。<b>待调手感值 / P8 转配置项</b>
     * <p>取值依据：它由 {@code TargetGoal#setUnseenMemoryTicks} 消费（原版默认 60 = 3s）。
     * 60 tick 太短——玩家绕一根柱子、或者跳下一个平台，BOSS 就会"跟丢"并停在原地；
     * 而本 BOSS 是飞行单位、本来就该咬得住。10s 足够玩家做一次完整的走位拉扯，
     * 又不至于让它隔着半个牢狱死追（超过 {@code FOLLOW_RANGE} 仍会正常脱锁）。
     * <p>注意：这一项只影响"<b>锁定之后</b>能不能看不见"，<b>索敌</b>本身仍要求视线
     * （{@code TargetingConditions#checkLineOfSight} 默认为真，且不随 mustSee 改变）；
     * "被打就锁定"那条路径由 {@code HurtByTargetGoal} 负责，它内部固定 300 tick。
     */
    public static final int TARGET_UNSEEN_MEMORY_TICKS = 200;

    private static final String NBT_PHASE = "AgaitolosPhase";
    private static final String NBT_RESPAWN_TICKS = "AgaitolosRespawnTicks";
    private static final String NBT_INITIAL_RESPAWN_DONE = "AgaitolosInitialRespawnDone";
    private static final String NBT_GUARDING = "AgaitolosGuarding";
    private static final String NBT_GUARD_TICKS = "AgaitolosGuardTicks";
    private static final String NBT_GUARD_COOLDOWN = "AgaitolosGuardCooldown";
    private static final String NBT_DIVE_TICKS = "AgaitolosDiveTicks";
    private static final String NBT_DIVE_SWEEP_COOLDOWN = "AgaitolosDiveSweepCooldown";
    private static final String NBT_GROUNDED_TICKS = "AgaitolosGroundedTicks";
    private static final String NBT_SCYTHE_SEAL_TICKS = "AgaitolosScytheSealTicks";
    private static final String NBT_CHARGING = "AgaitolosCharging";
    private static final String NBT_CHARGE_TICKS = "AgaitolosChargeTicks";
    private static final String NBT_MINION_COOLDOWN = "AgaitolosMinionCooldown";
    private static final String NBT_CHARGE_DAMAGE = "AgaitolosChargeDamage";
    private static final String NBT_BLINK_COOLDOWN = "AgaitolosBlinkCooldown";
    private static final String NBT_KICK_COOLDOWN = "AgaitolosKickCooldown";
    private static final String NBT_MELEE_COOLDOWN = "AgaitolosMeleeCooldown";
    private static final String NBT_RANGED_COOLDOWN = "AgaitolosRangedCooldown";
    /** 已定案的「场内人数」（缩放用），见 {@link AgaitolosScaling} */
    private static final String NBT_PLAYER_COUNT = "AgaitolosPlayerCount";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** 上次成功受击的游戏刻。初值取远小于当前刻的负数，保证首次受击不被 0.2s 冷却拦下 */
    private long lastHurtGameTime = -100L;

    /** 复活阶段剩余 tick（服务端权威；不参与同步，客户端只需要"是否在复活"） */
    private int respawnTicks;

    /**
     * 「召唤即进入复活阶段」这一演出是否已发生（落盘）。
     * 必须持久化：否则读档后 {@code tickCount} 归零会重演一次满血复活 + 全场击飞。
     */
    private boolean initialRespawnDone;

    /** 格挡架势剩余 tick（服务端权威；客户端只需要"是否在架势"，同 respawning 的分工） */
    private int guardTicks;

    /** 架势结束后的冷却剩余 tick（服务端权威，不参与同步） */
    private int guardCooldownTicks;

    /** 俯冲镰扫的冷却剩余 tick（服务端权威，不参与同步；已落盘，见 {@link #NBT_DIVE_SWEEP_COOLDOWN}） */
    private int diveSweepCooldownTicks;

    /** 冲锋剩余 tick（服务端权威，不参与同步）：> 0 即"正在俯冲位移"，见 {@link #isDiving()} */
    private int diveTicks;

    /** 禁飞剩余 tick（服务端权威，不参与同步）：被玩家格挡成功后授予，见 {@link #applyGrounded(int)} */
    private int groundedTicks;

    /** 镰扫封印剩余 tick（服务端权威，不参与同步）：封印期内不再起手俯冲镰扫 */
    private int scytheSealTicks;

    /** 「恶怨倒转」蓄力剩余 tick（服务端权威；客户端只需要"是否在蓄力"，同 guard 的分工） */
    private int chargeTicks;

    /** 「恶怨倒转」冷却剩余 tick（服务端权威，不参与同步；蓄力结束才开始计时） */
    private int minionCooldownTicks;

    /**
     * 「恶怨倒转」蓄力期间累计吃下的伤害（服务端权威，不参与同步）：达到
     * {@code getMaxHealth() × }{@link #CHARGE_BREAK_DAMAGE_RATIO} 即视为"足量的伤害"、打断蓄力。
     * <p>只在伤害<b>真正落地</b>的分支累加（见 {@link #hurt} 的 {@code if (applied)}），
     * 被格挡 / 被免伤（非玩家来源、复活无敌）/ 被 0.2s 冷却挡下的部分一律不计入。
     */
    private float chargeDamageTaken;

    /** 瞬击（二阶段）冷却剩余 tick（服务端权威，不参与同步；已落盘，同俯冲镰扫） */
    private int blinkCooldownTicks;

    /** 高速踢击（二阶段）冷却剩余 tick（服务端权威，不参与同步；已落盘，同上） */
    private int kickCooldownTicks;

    /**
     * 普攻间隔剩余 tick（服务端权威，不参与同步；已落盘）。
     * <p>
     * <b>为什么实体必须显式持有它</b>：本轮之前这个间隔藏在原版 {@code MeleeAttackGoal} 的
     * {@code attackInterval}（实测 = 20 tick）里。现在普攻的出手闸收进 {@link AgaitolosSkillDirector}
     * 与 {@link #doHurtTarget}（决策层也会直接调 {@code doHurtTarget}），若不在实体侧也留一份，
     * 决策层会每个节拍都放普攻、把普攻速度凭空翻倍。
     * <p>基准值取 {@link AgaitolosMeleeSkill#MELEE_INTERVAL_TICKS}（与原版同值 20），
     * 再按阶段折算（{@code AgaitolosPace#scaledCooldown}）⇒ 二、三阶段的普攻更密，与"二阶段更快速"同向。
     */
    private int meleeCooldownTicks;

    /**
     * 远程凋零头冷却剩余 tick（服务端权威，不参与同步；已落盘）。
     * <p>基准值取 {@link AgaitolosSkullSkill#SKULL_COOLDOWN_TICKS}（= 原 {@code RangedAttackGoal} 的
     * 60 tick 间隔字面量，随该 Goal 一起搬进技能类），按阶段折算。
     */
    private int rangedCooldownTicks;

    /**
     * 已定案的「场内人数」n（服务端权威，不参与同步；<b>落盘</b>）。
     * <p>
     * <b>只在两个时刻重算</b>（用户拍板：不做实时跟随）：① BOSS 入场（首次 summon 分支）；
     * ② 每次进阶段（{@link #advancePhase}）。玩家中途进出<b>不</b>改动本值 ——
     * 否则血量上限会随进出忽大忽小，BossBar 与"每失去一半血进阶段"的节奏全部失真。
     * <p>必须落盘：区块卸载/重启会重建实体，不落盘则"已按 3 人算好的血量上限"会跟着实体的
     * 属性修饰符一起读回、而伤害系数退回 1.0 —— 两条口径立刻分叉（血量按 3 人、伤害按 1 人）。
     */
    private int scaledPlayerCount = 1;

    /**
     * 战斗决策层：<b>选招的唯一入口</b>（"这一拍放哪一招"）。
     * <p>
     * <b>为什么用字段初始化而不是在 {@link #registerGoals()} 里 new 一个传给 Goal</b>：
     * {@code registerGoals()} 是由 {@code Mob} 的<b>构造器</b>回调的，早于本类的字段初始化，
     * 那时任何"构造时注入"的写法拿到的都是 {@code null}（且只会在运行期某次空指针时才暴露）。
     * 本类与决策层的交互全部走方法调用（{@link #aiStep} 主动 tick、Goal 不持有它），故无此风险。
     */
    private final AgaitolosSkillDirector skillDirector = new AgaitolosSkillDirector();

    /**
     * 阶段三五招的状态机（三重投掷 / 饱和轰炸 / 投技①② / 天魔＊灾）。
     * <p>与 {@link #skillDirector} 同一分工：本类不把它们的计时塞进自己的字段，
     * 只每 tick 调一次 {@code phaseThree.tick(this)} 并把起手入口转发出去
     * （理由与"这些字段为什么单独一个类"见 {@code AgaitolosPhaseThreeState} 的类注释）。
     */
    private final AgaitolosPhaseThreeState phaseThree = new AgaitolosPhaseThreeState();

    /** 出场演出剩余 tick（服务端权威；客户端只需要"是否在出场"，同 respawning 的分工）。<b>不落盘</b>，理由见 {@link #readAdditionalSaveData} */
    private int introTicks;

    /**
     * 出场降临的目标 Y（服务端权威；开演时按 {@link AgaitolosMoveControl#hoverY} 定一次，之后不再重算）。
     * <p>整个过程不落盘、也不参与同步：它只是本段插值的临时基准，重进存档时演出已按"不重放"处理。
     */
    private double introDescentTargetY;

    /**
     * 是否处于<b>地面档</b>（服务端权威；不落盘）。
     * <p>档位的唯一消费点是 {@code AgaitolosMoveControl} ③（垂直目标高度），故这个布尔本身不需要同步：
     * 位置由原版实体同步，高度已经体现在坐标里。客户端动画要用的"展示状态"另走 {@link #DATA_PERCH_STATE}
     * （多带"过渡中/朝哪个方向"，见 {@link #getPerchState()}）。
     * <p>不落盘：读档/区块卸载后从"空中档"重新开始（最多 4s 后再落地），不会出现"读档后还记得
     * 上一轮站在地上"这种无法解释的行为，与决策层的战术记忆同一口径。
     */
    private boolean perched;

    /** 已在地面档停留的 tick 数（迟滞用；进入地面档时归零） */
    private int perchedTicks;

    /** 已离开地面档的 tick 数（节律用；<b>封顶</b>在 {@link #PERCH_AIRBORNE_MIN_TICKS}，故不会溢出） */
    private int airborneTicks;

    /**
     * 落地 / 升空过渡的剩余 tick（只在 {@link #PERCH_STATE_LANDING} / {@link #PERCH_STATE_TAKEOFF} 期间递减）。
     * <p><b>两个消费点</b>：
     * <ol>
     *   <li>动画展示：过渡态 → 稳态的切换点（闸门）；</li>
     *   <li><b>升降位移的匀速分母</b>（2026-09-21 对齐）：{@code AgaitolosMoveControl} ③-a 在窗口内改用
     *       "剩余高度 ÷ 剩余 tick" 的匀速收敛，使"位置到位"与 12t 的过渡 clip <b>严格同刻</b>结束。
     *       稳态下的高度收敛（③-b 的比例 + 限速）不读它。</li>
     * </ol>
     */
    private int perchTransitionTicks;

    public AgaitolosEntity(EntityType<? extends AgaitolosEntity> type, Level level) {
        super(type, level);
        // 低空飞行移动控制器。原版 Mob 没有 createMoveControl() 钩子（凋灵/幻翼也都是构造器里直接赋值），故在此替换
        this.moveControl = new AgaitolosMoveControl(this);
    }

    /**
     * 基础属性。
     * <p>两个基础值取 {@link AgaitolosScaling} 的常量（单一真源）：随在场玩家数增强时，
     * 缩放量 = {@code 基础值 × (缩放系数 − 1)}，故基础值必须与缩放算式同源，否则同一份加成会被算成两个数。
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, AgaitolosScaling.BASE_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, AgaitolosScaling.BASE_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    // ---------------------------------------------------------------- 同步数据

    @Override
    protected void defineSynchedData() {
        // 必须保留父类（Entity/LivingEntity/Mob）已定义的数据项
        super.defineSynchedData();
        // 默认 = PHASE_1 的序号（复活阶段由 aiStep 的首次 tick 推进去，见下）
        this.entityData.define(DATA_PHASE, AgaitolosPhase.PHASE_1.combatOrdinal());
        this.entityData.define(DATA_RESPAWNING, Boolean.FALSE);
        this.entityData.define(DATA_GUARDING, Boolean.FALSE);
        this.entityData.define(DATA_CHARGING, Boolean.FALSE);
        // 出场默认 false：读档重建的实体不从存档恢复演出（见 readAdditionalSaveData），
        // 只有"首次召唤分支"会把它立起来
        this.entityData.define(DATA_INTRO, Boolean.FALSE);
        // 地面档展示状态默认 = 空中档（不落盘，理由同 perched 字段：读档/区块卸载后从空中档重新开始）
        this.entityData.define(DATA_PERCH_STATE, PERCH_STATE_AIRBORNE);
        // 轰炸默认 false：读档重建的实体不从存档恢复"正在轰炸"（进行中状态一律不落盘，见 AgaitolosPhaseThreeState）
        this.entityData.define(DATA_BOMBARDING, Boolean.FALSE);
    }

    /** 当前阶段（服务端读权威值，客户端读同步值） */
    public AgaitolosPhase getPhase() {
        return AgaitolosPhase.byCombatOrdinal(this.entityData.get(DATA_PHASE));
    }

    /** 切换阶段（P4~P6 解锁招式、P7 换模型时按阶段分支） */
    public void setPhase(AgaitolosPhase phase) {
        this.entityData.set(DATA_PHASE, phase.combatOrdinal());
    }

    /** 是否处于复活阶段：全程无敌 + 快速回血，结束瞬间击飞周围玩家 */
    public boolean isRespawning() {
        return this.entityData.get(DATA_RESPAWNING);
    }

    /** 复活阶段只由本类的阶段机开合，外部（技能）只读 */
    private void setRespawning(boolean respawning) {
        this.entityData.set(DATA_RESPAWNING, respawning);
    }

    /** 是否处于格挡架势（服务端读权威值，客户端读同步值）：动画与受击判定两端共用 */
    public boolean isGuarding() {
        return this.entityData.get(DATA_GUARDING);
    }

    /** 架势由决策层经 {@link #startGuard(LivingEntity)} 开启、由 {@link #tickGuardState()} 与 {@link #endGuard()} 收合，技能不自持计时 */
    private void setGuarding(boolean guarding) {
        this.entityData.set(DATA_GUARDING, guarding);
    }

    /**
     * 立即收势：结束架势并进入冷却。
     * <p>只给格挡反击用（{@link AgaitolosGuardSkill#counterAttack}）：状态（剩余 tick / 冷却）归实体自己持有，
     * 技能只表达"这次反击把架势打断了"这一语义，不去碰实体的计数字段。
     */
    public void endGuard() {
        this.guardTicks = 0;
        // 冷却按阶段折算（二阶段起更短，即"更频繁地摆架势"）：倍率表见 AgaitolosPace
        this.guardCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosGuardSkill.GUARD_COOLDOWN_TICKS);
        this.setGuarding(false);
    }

    /** 是否处于「恶怨倒转」蓄力（服务端读权威值，客户端读同步值）：动画与出手判定两端共用 */
    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING);
    }

    /** 蓄力由决策层经 {@link #startCharge(LivingEntity)} 开启、由 {@link #tickChargeState()} / {@link #finishCharge()} 收合（同架势） */
    private void setCharging(boolean charging) {
        this.entityData.set(DATA_CHARGING, charging);
    }

    /** 是否正在播出场演出（服务端读权威值，客户端读同步值）：动画与"一律不起手"的闸门两端共用 */
    public boolean isIntroPlaying() {
        return this.entityData.get(DATA_INTRO);
    }

    /** 出场只由本类的 {@link #startIntro()} / {@link #tickIntro()} 开合，技能不自持计时（同架势/蓄力） */
    private void setIntroPlaying(boolean intro) {
        this.entityData.set(DATA_INTRO, intro);
    }

    /**
     * 是否正在「饱和轰炸」（阶段三）：服务端读权威值，客户端读同步值。
     * <p>三处消费：动画的 bombard 控制器（两端各自轮询）、MAIN 控制器的互斥停播、以及
     * {@code AgaitolosMoveControl} 的"高空档"（{@code BOMBARD_HOVER_HEIGHT}）。
     * <p><b>与 {@code AgaitolosPhaseThreeState} 的分工</b>：本方法只表达"这一刻是不是在轰炸"，
     * 计时/节拍/冷却全在状态机那一侧（与 {@code isGuarding()} ↔ {@code tickGuardState()} 同款分工）。
     */
    public boolean isBombarding() {
        return this.entityData.get(DATA_BOMBARDING);
    }

    /** 轰炸同步位只由 {@code AgaitolosPhaseThreeState} 开合（起手立、期满/整段中止撤），技能不自持计时 */
    void setBombarding(boolean bombarding) {
        this.entityData.set(DATA_BOMBARDING, bombarding);
    }

    // ---------------------------------------------------------------- 冲锋 / 禁飞 / 封印状态

    /** 是否正在冲锋（俯冲位移中，服务端权威）：冲锋期间位移由本类直接写，{@code AgaitolosMoveControl} 全让位 */
    public boolean isDiving() {
        return this.diveTicks > 0;
    }

    /**
     * 是否被禁飞（被玩家格挡的惩罚）：禁飞期间不再悬停，改走带重力的原版 travel 落到地面。
     * <p>禁飞结束时无需任何额外处理：{@code AgaitolosMoveControl} ③ 的垂直修正会自动把它升回
     * 当前高度档（空中档取本阶段的高度，见 {@code AgaitolosMoveControl#airborneHeight}；若此刻正处地面档
     * {@link #isPerched()} 则维持贴地）。
     * <p><b>禁飞期的动画（2026-09-21 补）</b>：禁飞只是"不给垂直分量、交给重力"，落地位置随机，
     * 故没有（也不该有）专门的禁飞姿态 —— 它自己不驱动动画。实际观感由地面档接管：
     * 玩家刚挡下横扫、人就贴在旁边，禁飞落地的 BOSS 会在 {@link #PERCH_AIRBORNE_MIN_TICKS}（4s）内
     * 被 {@link #tickPerchState()} 判为可贴地并切进地面档，此后就播 {@code idle_ground} 而不再是
     * {@code idle_flight}（地面档动画见 {@link #isPerched()}）。禁飞期若目标恰好不在近战可达内，
     * 则这段时间仍会播飞行待机 —— 属"没有禁飞专属姿态"的已知取舍，不是失效。
     */
    public boolean isGrounded() {
        return this.groundedTicks > 0;
    }

    /**
     * 镰扫是否处于封印期（被玩家格挡的惩罚）。
     * <p>
     * <b>封印的作用范围（2026-09-20 用户拍板）</b>：封印<b>只封"大招"</b>，不封基础手段。
     * 原因是禁飞与封印由 {@link #onSweepBlocked()} 同刻授予、且同为 600 tick，
     * 而瞬击的唯一起手条件恰恰是"不处于飞行状态"（= 禁飞窗口）—— 若封印也盖住瞬击，
     * 它的起手窗口会被完全覆盖、一次也放不出来，规格原文「如 BOSS 不处于飞行状态时便可以使用」
     * 就成了死条文。
     * <ul>
     *   <li><b>吃封印</b>：俯冲镰扫（决策层打分与 {@link #startDiveSweep(LivingEntity)} 复校里各判一次）；后续"大招"——
     *       三重投掷 / 天魔灾 / 投技 —— 接入时请走含封印的起手闸；</li>
     *   <li><b>不吃封印</b>：瞬击（惩罚期的位移补偿，本身不造成伤害）、
     *       高速踢击（规格原文"<b>任何状态下可用</b>"，"任何状态"含封印期）。</li>
     * </ul>
     * ⚠ 待用户确认：恶怨倒转<b>也未被封印</b>（既有口径即如此），是否要把它纳入"大招"名单。
     */
    public boolean isScytheSealed() {
        return this.scytheSealTicks > 0;
    }

    /**
     * 授予禁飞。幂等：重复调用只刷新计时（取较大值，不会把已更长的窗口缩短），无累积副作用
     * —— 一次横扫可能被多个玩家同时格挡，逐人上报会重复调用。
     */
    private void applyGrounded(int ticks) {
        this.groundedTicks = Math.max(this.groundedTicks, ticks);
    }

    /**
     * 横扫被玩家格挡成功后的惩罚入口（由 {@link AgaitolosDiveSweepSkill#perform} 逐人上报）。
     * <p>状态与计时归实体自己持有，技能只上报"这一击被格挡了"（与 {@link #endGuard()} 同一手法，
     * 技能不碰实体的计数字段）。规格：本次攻击无效 + 解除飞行状态 30s + 封印该技能。
     * <p>幂等：重复调用只是刷新计时，无累积副作用（横扫可能被多个玩家同时格挡）。
     */
    public void onSweepBlocked() {
        // 打断正在进行的冲锋：被挡下后不该继续冲向对方。
        // 正常流程里 perform 由冲锋收尾处调用、此刻 diveTicks 已为 0，此处属防御性归零。
        this.diveTicks = 0;
        this.applyGrounded(GROUNDED_DURATION_TICKS);
        this.scytheSealTicks = SCYTHE_SEAL_TICKS;
    }

    /** 禁飞 / 封印 / 召唤冷却的服务端计时递减（每 tick 一次，不为负） */
    private void tickPenaltyTimers() {
        if (this.groundedTicks > 0) {
            --this.groundedTicks;
        }
        if (this.scytheSealTicks > 0) {
            --this.scytheSealTicks;
        }
        if (this.minionCooldownTicks > 0) {
            --this.minionCooldownTicks;
        }
    }

    /**
     * 招式冷却的服务端计时递减（每 tick 一次，不为负）。
     * <p>
     * <b>为什么集中在一个方法里</b>：这些计数原先各自藏在 {@code tickGuard}/{@code tickDiveSweep}/
     * {@code tickBlink}/{@code tickKick} 内部，起手判定被移交给 {@link AgaitolosSkillDirector} 之后，
     * 递减若继续留在那几条方法里，就会出现"决策层先读冷却、状态推进后递减"的顺序依赖
     * （同一 tick 内读到的可能是还没减的旧值）。集中到一处、由 {@link #aiStep} 在决策层之前调用，
     * 顺序就只有一种。
     * <p>与 {@link #tickPenaltyTimers()} 的分工：那一条是<b>玩家争取来的惩罚窗口</b>（禁飞/封印/召唤冷却），
     * 这一条是<b>BOSS 自己的出手节奏</b>（架势/横扫/瞬击/踢击/普攻/远程）。
     */
    private void tickActionCooldowns() {
        if (this.guardCooldownTicks > 0) {
            --this.guardCooldownTicks;
        }
        if (this.diveSweepCooldownTicks > 0) {
            --this.diveSweepCooldownTicks;
        }
        if (this.blinkCooldownTicks > 0) {
            --this.blinkCooldownTicks;
        }
        if (this.kickCooldownTicks > 0) {
            --this.kickCooldownTicks;
        }
        if (this.meleeCooldownTicks > 0) {
            --this.meleeCooldownTicks;
        }
        if (this.rangedCooldownTicks > 0) {
            --this.rangedCooldownTicks;
        }
        // 阶段三五招的冷却也收在这一个调用点（口径同上：所有冷却都在决策层读之前统一递减一次）。
        // 它们<b>逐招写在本类会再长 5 个字段</b>，故计时器归 AgaitolosPhaseThreeState 持有，
        // 但"什么时候减"仍只有这一个入口 —— 顺序依赖问题与上面五招完全同款地不存在。
        this.phaseThree.tickCooldowns();
    }

    /**
     * 只读探针：某一招的剩余冷却（供 {@link AgaitolosSkillDirector} 判断"这一拍选不选它"）。
     * <p><b>只读不写</b>：计时的持有与递减仍在本类（{@link #tickActionCooldowns()}），
     * 决策层只消费状态——"状态归实体持有"这条既有分工不因为引入决策层而改变，
     * 否则又会退化成"两个地方都能改冷却"的两套口径。
     */
    int remainingCooldown(AgaitolosSkillDirector.Move move) {
        switch (move) {
            case MELEE:
                return this.meleeCooldownTicks;
            case RANGED:
                return this.rangedCooldownTicks;
            case DIVE_SWEEP:
                return this.diveSweepCooldownTicks;
            case GUARD:
                return this.guardCooldownTicks;
            case REVERSAL:
                return this.minionCooldownTicks;
            case BLINK:
                return this.blinkCooldownTicks;
            case KICK:
                return this.kickCooldownTicks;
            // 阶段三五招：计时器归 AgaitolosPhaseThreeState 持有，这里只是只读转发
            // （默认分支的 Integer.MAX_VALUE 会把这些招判成"永远在冷却中"，
            //  漏一个 case 就表现为"某一招永远不出现"，故五招必须逐一列出）
            case TRIPLE_THROW:
            case BOMBARD:
            case GRAB_SWEEP:
            case GRAB_SMASH:
            case CALAMITY:
                return this.phaseThree.remainingCooldown(move);
            default:
                return Integer.MAX_VALUE;
        }
    }

    // ---------------------------------------------------------------- 地面档（"多待在地上"，2026-09-21 补）

    /**
     * 地面档的<b>最短停留</b>（tick）：60 = 3s。<b>待调手感值 / P8 转配置项</b>
     * <p>作用 = 迟滞：落地后 3s 内无论目标怎么走都不起飞，避免"目标一抖动就升降"的每 tick 抖动。
     */
    public static final int PERCH_MIN_TICKS = 60;

    /**
     * 地面档的<b>最长停留</b>（tick）：200 = 10s。<b>待调手感值 / P8 转配置项</b>
     * <p>作用 = 强制节律：到点必升空一次，保证"落地 ↔ 升空"是一个玩家看得见的循环，
     * 而不是"一旦贴脸就永远趴在地上"（那会丢掉本 BOSS 的飞行辨识度，也会让部分需要空间的招失去意义）。
     * <p>同时也是"贴地若被地形卡住"的自解出口：最多 10s 就升空脱离（见 {@code AgaitolosMoveControl#PERCH_HEIGHT}）。
     */
    public static final int PERCH_MAX_TICKS = 200;

    /**
     * 地面档之后的<b>最短空中停留</b>（tick）：80 = 4s。<b>待调手感值 / P8 转配置项</b>
     * <p>作用 = 节律的下半段：升空后至少飞 4s 才允许再落地，避免"落-起-落"高频抖动。
     * <p>本常量同时是 {@link #airborneTicks} 的封顶值（判据只需"是否已满"，不必真计时，故不会溢出）。
     */
    public static final int PERCH_AIRBORNE_MIN_TICKS = 80;

    /**
     * 地面档的<b>脱离距离</b>（格）：4.5。<b>待调手感值 / P8 转配置项</b>
     * <p>
     * 与进入条件（{@code isTargetWithinMeleeReach}，含悬停折算约 3.18 格）刻意留出差值，
     * 形成迟滞区间：目标在 3.18~4.5 格之间来回走位时档位不变，避免在阈值上反复横跳。
     */
    public static final double PERCH_RELEASE_RANGE = 4.5D;

    /** 地面档展示状态：<b>空中档</b>（纯飞行待机，MAIN 播 idle_flight） */
    public static final int PERCH_STATE_AIRBORNE = 0;

    /** 地面档展示状态：<b>落地过渡</b>（12t，MAIN 播 land，播完接 idle_ground） */
    public static final int PERCH_STATE_LANDING = 1;

    /** 地面档展示状态：<b>贴地稳态</b>（MAIN 播 idle_ground 循环） */
    public static final int PERCH_STATE_PERCHED = 2;

    /** 地面档展示状态：<b>升空过渡</b>（12t，MAIN 播 takeoff，播完接 idle_flight） */
    public static final int PERCH_STATE_TAKEOFF = 3;

    /**
     * 落地过渡窗口（tick）：12 = 0.6s，与 clip {@code animation.agaitolos.land} <b>逐字对齐</b>。
     * <p>改 clip 时长必须同步改这里：窗口是"过渡态 → 贴地稳态"的切换点，短了会把 land 掐掉一截、
     * 长了会让 land 播完停在末帧（末帧 == idle_ground 首帧，观感上仍不跳，但会显得落地后僵一下）。<b>待调手感值</b>
     */
    public static final int LAND_TRANSITION_TICKS = 12;

    /** 升空过渡窗口（tick）：12 = 0.6s，与 clip {@code animation.agaitolos.takeoff} 逐字对齐（口径同上） */
    public static final int TAKEOFF_TRANSITION_TICKS = 12;

    /**
     * 是否处于地面档（近身缠斗时贴地）。<b>服务端权威</b>，唯一消费点是
     * {@code AgaitolosMoveControl} ③ 的垂直目标高度（空中档 vs 地面档）。
     * <p>
     * <b>切换节律（全部由 {@link #tickPerchState()} 推进，这里只列口径）</b>：
     * <ol>
     *   <li><b>落地</b>：已在空中满 {@link #PERCH_AIRBORNE_MIN_TICKS}（4s）<b>且</b>目标进入近战可达
     *       （复用 {@code isTargetWithinMeleeReach} —— 与普攻/格挡/踢击同一把尺子，不另立距离口径）；</li>
     *   <li><b>保持</b>：至少 {@link #PERCH_MIN_TICKS}（3s），之后满足任一即<b>升空</b>：
     *       ① 目标超出 {@link #PERCH_RELEASE_RANGE}（4.5 格）或失去目标；
     *       ② 已待满 {@link #PERCH_MAX_TICKS}（10s）；</li>
     *   <li><b>升降过程</b>：不做瞬移 —— {@code AgaitolosMoveControl} 仍走"按高度差收敛 + 限速"，
     *       2 格高度差约 10~16 tick 走完，与落地/升空过渡 clip 的 12t 大体同步（见下）。</li>
     * </ol>
     * <b>与 {@link #isGrounded()}（被格挡后的禁飞惩罚）是两个概念，刻意不合并</b>：禁飞是"不写垂直分量、
     * 交给重力"，地面档是"垂直收敛到更低的档位"；两者可以叠加（禁飞期本来就在地上，地面档让惩罚结束后
     * 不要立刻弹回空中）。混用会让"瞬击只在禁飞窗口可用"这条规则连带被改坏。
     * <p>
     * <b>为什么落地位不缩短近战可达尺子</b>：{@link #getMeleeAttackRangeSqr} 仍按最大悬停高度
     * （{@code AgaitolosMoveControl.HOVER_HEIGHT}）折算。落地后这把尺子等于放宽了约 2 格，
     * 属"保守侧"——刚修好的还手能力不会因为落地而变弱（宁可多够 2 格，也不要出现"落地后反倒打不着"）。
     * 若实机觉得落地后够得太远，再改成按当前档位折算。
     * <p>
     * <b>动画（2026-09-21 已接入）</b>：地面档不再是"人站在地上、姿态还在飘"——MAIN 控制器按
     * 同步状态 {@link #getPerchState()} 分支，落地过渡播 {@code land}、贴地播 {@code idle_ground}、
     * 升空过渡播 {@code takeoff}（常量与分支见 {@code AgaitolosAnimations}）。
     * 本布尔仍只管"高度档"，不参与动画判定（动画需要四态，见 {@link #getPerchState()}）。
     */
    public boolean isPerched() {
        return this.perched;
    }

    /**
     * 地面档的<b>展示状态</b>（服务端写、客户端读；动画 MAIN 控制器轮询它，见 {@code AgaitolosAnimations#groundAwareIdle}）。
     * <p>
     * 四态：{@link #PERCH_STATE_AIRBORNE} / {@link #PERCH_STATE_LANDING} / {@link #PERCH_STATE_PERCHED}
     * / {@link #PERCH_STATE_TAKEOFF}。
     * <p>
     * <b>为什么是四态 int 而不是"布尔 + 过渡标志"</b>：动画需要在两个方向上各播一条过渡 clip，
     * 而"过渡中"与"朝哪个方向"合起来正好是 4 个互斥取值；用一个 INT 一次带走，
     * 客户端只需一个 accessor 与一次比较（省掉第二个同步字段，也省掉"两字段不一致"的可能）。
     * 与 {@link #isPerched()} 的关系是纯派生：LANDING/PERCHED ⇒ perched 为 true（高度已朝地面收敛），
     * AIRBORNE/TAKEOFF ⇒ perched 为 false（高度已朝空中收敛）——两条边在同一 tick 一起翻，
     * 故过渡 clip 与升降位移是同刻起跑的（见 {@link #tickPerchState()}）。
     */
    public int getPerchState() {
        return this.entityData.get(DATA_PERCH_STATE);
    }

    /**
     * 只读探针：过渡窗口的剩余 tick（0 = 不在过渡中）。
     * <p>消费点只有 {@code AgaitolosMoveControl} ③-a（当匀速收敛的分母）。
     * 节律（进入 / 递减 / 退出）仍由本类独占（{@link #tickPerchState()}），控制器只读 ——
     * 与 {@link #isPerched()}（高度档）和 {@link #getPerchState()}（展示状态）的既有分工一致。
     * <p>无需同步：只有服务端控制器读它（客户端只读 {@link #getPerchState()} 那条四态数据去选 clip）。
     */
    int getPerchTransitionTicks() {
        return this.perchTransitionTicks;
    }

    /**
     * 写地面档展示状态。<b>只在值时显式判等后写</b>：整轮节律只翻 4 次
     * （LANDING → PERCHED → TAKEOFF → AIRBORNE），不存在每 tick 反复 set 造成的无谓同步量；
     * 判等也让"同步量"这件事不依赖原版 {@code SynchedEntityData#set} 对同值是否短路。
     */
    private void setPerchState(int state) {
        if (this.getPerchState() == state) {
            return;
        }
        this.entityData.set(DATA_PERCH_STATE, state);
    }

    /**
     * 地面档的状态推进（服务端权威，每 tick 一次）：<b>过渡窗口 + 落地条件 + 三段迟滞 + 强制升空</b>。
     * <p>
     * 只动"高度档位"一个布尔、展示状态一个 int 与三个计数，不碰任何招式状态（不占决策层的全局动作锁、
     * 不影响冷却），故与 {@link AgaitolosSkillDirector} 的仲裁完全正交：落地期间照样能普攻 / 格挡 / 蓄力 / 被踢击，
     * 也照样能被决策层派招（俯冲的位移由 {@code tickDiveCharge} 独占，地面档不参与）。
     * <p>
     * 演出与死亡期间整段跳过：那几段的位移由 {@code tickIntro} / 回复演出独占，
     * {@code AgaitolosMoveControl} 也已整体让位，此时切档没有任何意义（只会让计数白白推进）。
     * <p>
     * <b>本方法只在服务端跑</b>：唯一调用点 {@link #aiStep()} 在客户端已提前 return，
     * 故对同步数据 {@code DATA_PERCH_STATE} 的写入天然是"服务端权威"（客户端只读不写）。
     */
    private void tickPerchState() {
        if (this.isDeadOrDying() || this.isRespawning() || this.isIntroPlaying()) {
            return;
        }
        // ① 过渡窗口（落地 / 升空各 12t，与对应 clip 等长）：到期即切到稳态，供动画把"过渡 clip → 稳态循环"接上。
        //    与下面的档位判定互不干扰：窗口只影响展示状态，高度档（perched）在过渡的第一 tick 就已经翻好了。
        //    只在过渡态里递减（稳态下计数器不参与，也就不会长跑到负值）
        int perchState = this.getPerchState();
        if ((perchState == PERCH_STATE_LANDING || perchState == PERCH_STATE_TAKEOFF)
                && --this.perchTransitionTicks <= 0) {
            this.setPerchState(perchState == PERCH_STATE_LANDING ? PERCH_STATE_PERCHED : PERCH_STATE_AIRBORNE);
        }
        LivingEntity target = this.getTarget();
        if (this.perched) {
            // 地面档：先满最短停留（迟滞），再判"目标走远"或"待太久"
            if (++this.perchedTicks < PERCH_MIN_TICKS) {
                return;
            }
            // 抓取态（投技①）期间<b>不升空</b>：那一招的语义是"把玩家踩在脚下"，
            // 若恰好卡在 PERCH_MAX_TICKS 的升空点上，就会把"踩住"变成"拎着人上天"，
            // 而且松手时玩家会被留在半空吃一段下落伤害 —— 与"压制"的语义相反。
            // 抓取最长只有 HOLD_TICKS(22t)，远小于 PERCH_MAX_TICKS 的量级，故这点延迟肉眼不可见。
            if (this.phaseThree.isHoldingTarget()) {
                return;
            }
            boolean targetLeft = target == null || !target.isAlive()
                    || this.distanceTo(target) > PERCH_RELEASE_RANGE;
            if (targetLeft || this.perchedTicks >= PERCH_MAX_TICKS) {
                this.perched = false;
                this.perchedTicks = 0;
                this.airborneTicks = 0;
                // 高度档与展示状态同刻翻：升空过渡 clip 与"朝空中档收敛"是同一 tick 起跑的两件事
                this.perchTransitionTicks = TAKEOFF_TRANSITION_TICKS;
                this.setPerchState(PERCH_STATE_TAKEOFF);
            }
            return;
        }
        // 空中档：先攒满最短空中停留（保证升空是一段可感知的节律，而不是刚起飞又落），再判能否落地。
        // airborneTicks 封顶在阈值上：后半段判据只需"是否已满"，不需要真实时长，也就不存在溢出
        if (this.airborneTicks < PERCH_AIRBORNE_MIN_TICKS) {
            ++this.airborneTicks;
            return;
        }
        if (target != null && target.isAlive() && this.isTargetWithinMeleeReach(target)) {
            this.perched = true;
            this.perchedTicks = 0;
            this.perchTransitionTicks = LAND_TRANSITION_TICKS;
            this.setPerchState(PERCH_STATE_LANDING);
        }
    }

    // ---------------------------------------------------------------- 存档

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_PHASE, getPhase().combatOrdinal());
        tag.putInt(NBT_RESPAWN_TICKS, this.respawnTicks);
        tag.putBoolean(NBT_INITIAL_RESPAWN_DONE, this.initialRespawnDone);
        // 已定案的场内人数一并落盘（理由见字段注释）：它决定伤害缩放系数，而血量上限是以属性修饰符
        // 的形式独立落盘的 ⇒ 两者必须同时落盘，否则读档后会出现"血量按 3 人、伤害按 1 人"的分叉
        tag.putInt(NBT_PLAYER_COUNT, this.scaledPlayerCount);
        // 架势与冷却一并落盘（理由同 DATA_RESPAWNING）：读档/区块卸载会重建实体，
        // 不落盘则"正举着格挡"的 BOSS 读档后白送一次免伤，冷却也会被重置成可立刻连挡。
        tag.putBoolean(NBT_GUARDING, this.isGuarding());
        tag.putInt(NBT_GUARD_TICKS, this.guardTicks);
        tag.putInt(NBT_GUARD_COOLDOWN, this.guardCooldownTicks);
        // 冲锋 / 禁飞 / 封印 / 横扫冷却同理落盘：不落盘则"正在俯冲的 BOSS"读档后会停在半空，
        // 玩家辛苦格挡换来的 30s 禁飞与封印也会被读档洗掉。
        tag.putInt(NBT_DIVE_TICKS, this.diveTicks);
        tag.putInt(NBT_DIVE_SWEEP_COOLDOWN, this.diveSweepCooldownTicks);
        tag.putInt(NBT_GROUNDED_TICKS, this.groundedTicks);
        tag.putInt(NBT_SCYTHE_SEAL_TICKS, this.scytheSealTicks);
        // 蓄力与召唤冷却同理落盘：不落盘则"正举着球蓄力的 BOSS"读档后会白送一次 12s 蓄力，
        // 冷却也会被重置成可立刻再招一支分队。
        tag.putBoolean(NBT_CHARGING, this.isCharging());
        tag.putInt(NBT_CHARGE_TICKS, this.chargeTicks);
        tag.putInt(NBT_MINION_COOLDOWN, this.minionCooldownTicks);
        // 打断阈值的累计承伤也要落盘：不落盘则"玩家已经打进一半阈值的蓄力"被读档洗成从零开始，
        // 等于用读档白嫖一次续命（区块卸载/重启都会重走 readAdditionalSaveData）
        tag.putFloat(NBT_CHARGE_DAMAGE, this.chargeDamageTaken);
        // 二阶段两招的冷却同理落盘：不落盘则读档会白送一次瞬击/踢击（且与"冷却已过"无法区分）
        tag.putInt(NBT_BLINK_COOLDOWN, this.blinkCooldownTicks);
        tag.putInt(NBT_KICK_COOLDOWN, this.kickCooldownTicks);
        // 普攻/远程的出手间隔同理落盘：不落盘则读档会白送一次贴脸连击（普攻间隔原本藏在原版 Goal 里，
        // 读档本来也会被重置；现在由实体持有，就把它一并按同一口径处理，不留特例）
        tag.putInt(NBT_MELEE_COOLDOWN, this.meleeCooldownTicks);
        tag.putInt(NBT_RANGED_COOLDOWN, this.rangedCooldownTicks);
        // 阶段三五招：只落冷却（口径同上 —— 不落盘则读档会白送一次大招）；
        // 进行中的状态（三连节拍 / 轰炸 / 抓取 / 劈击 / 施法）**刻意不落盘**，
        // 理由见 AgaitolosPhaseThreeState 的类注释（抓取态续播要凭一个可能失效的 UUID 去控人，风险远大于收益）
        this.phaseThree.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // 缺键时 getInt 返回 0，而 0 在 AgaitolosPhase 里是 RESPAWN（不是 PHASE_1）——
        // 复活阶段<b>永远不会由阶段机产生</b>（enterRespawn 只动 respawning 布尔，不动阶段字段），
        // 所以存档里出现 0 只可能是"键不存在"（典型场景：/summon 生成时原版会把命令 NBT 整个 load 一遍，
        // 那里没有本键）。若照搬 getInt 的 0，BOSS 会永久停在 RESPAWN：
        // checkPhaseAdvance 对它取不到阈值（-1）直接 return ⇒ <b>永远进不了二/三阶段</b>
        // （表现：招式永远只有一阶段那五招、模型不换、节奏不加快）。
        // 故这里显式区分"键不存在"与"值为 0"：缺键一律当 PHASE_1（新实体的正确初值）。
        this.setPhase(tag.contains(NBT_PHASE)
                ? AgaitolosPhase.byCombatOrdinal(tag.getInt(NBT_PHASE))
                : AgaitolosPhase.PHASE_1);
        this.respawnTicks = Math.max(0, tag.getInt(NBT_RESPAWN_TICKS));
        // 存档里若还在复活阶段，恢复无敌与倒计时：BOSS 不能靠读档跳过无敌期
        this.setRespawning(this.respawnTicks > 0);
        this.initialRespawnDone = tag.getBoolean(NBT_INITIAL_RESPAWN_DONE);
        // 场内人数（缩放用）：缺键一律当 1（= 单人，与改动前逐位一致）。
        // 读回后<b>重挂一次</b>缩放修饰符而不是只赋值：applyScalingFor 是幂等的（摘旧挂新），
        // 且 super.readAdditionalSaveData 已把属性块（含永久修饰符）与当前血量读完 ⇒ 这里既保留
        // "血量按比例不变"，又顺手把"旧存档缺修饰符 / 属性被外部工具改过"的坏数据纠正回与人数一致。
        this.applyScalingFor(tag.contains(NBT_PLAYER_COUNT) ? tag.getInt(NBT_PLAYER_COUNT) : 1);
        // 缺键时 getInt 返回 0 ⇒ 冷却为 0（可立刻起手），属安全默认
        this.guardTicks = Math.max(0, tag.getInt(NBT_GUARD_TICKS));
        this.guardCooldownTicks = Math.max(0, tag.getInt(NBT_GUARD_COOLDOWN));
        // 架势随存档恢复；但剩余 tick 已为 0（异常存档）时不该继续举着，交给 tickGuardState 下一 tick 收势
        this.setGuarding(tag.getBoolean(NBT_GUARDING) && this.guardTicks > 0);
        // 四个计时一律做下界保护：缺键 ⇒ 0（安全默认，等同"没有在冲锋/禁飞/封印/冷却"），
        // 异常存档里的负值也只当 0，绝不把状态搞成负数（负数会让 isDiving() 等判定失真）
        this.diveTicks = Math.max(0, tag.getInt(NBT_DIVE_TICKS));
        this.diveSweepCooldownTicks = Math.max(0, tag.getInt(NBT_DIVE_SWEEP_COOLDOWN));
        this.groundedTicks = Math.max(0, tag.getInt(NBT_GROUNDED_TICKS));
        this.scytheSealTicks = Math.max(0, tag.getInt(NBT_SCYTHE_SEAL_TICKS));
        // 蓄力 / 召唤冷却同做下界保护；蓄力随存档恢复，但剩余 tick 已为 0（异常存档）时不该继续举着，
        // 交给 tickChargeState 下一 tick 收势
        this.chargeTicks = Math.max(0, tag.getInt(NBT_CHARGE_TICKS));
        this.minionCooldownTicks = Math.max(0, tag.getInt(NBT_MINION_COOLDOWN));
        // 下界保护：缺键 ⇒ 0（安全默认，等同"这一轮蓄力还没被打进任何伤害"）；
        // 异常存档里的负值也只当 0，否则负累计会让阈值判定永远差一截才算数
        this.chargeDamageTaken = Math.max(0.0F, tag.getFloat(NBT_CHARGE_DAMAGE));
        // 二阶段两招的冷却同做下界保护（缺键 ⇒ 0 = 可立刻起手，安全默认；异常负值也只当 0）
        this.blinkCooldownTicks = Math.max(0, tag.getInt(NBT_BLINK_COOLDOWN));
        this.kickCooldownTicks = Math.max(0, tag.getInt(NBT_KICK_COOLDOWN));
        // 普攻/远程间隔同做下界保护（口径与上面四招完全一致：缺键 ⇒ 0 = 可立刻出手）
        this.meleeCooldownTicks = Math.max(0, tag.getInt(NBT_MELEE_COOLDOWN));
        this.rangedCooldownTicks = Math.max(0, tag.getInt(NBT_RANGED_COOLDOWN));
        this.setCharging(tag.getBoolean(NBT_CHARGING) && this.chargeTicks > 0);
        // 阶段三五招：只读冷却（缺键 ⇒ 0 = 可立刻起手，安全默认；异常负值也只当 0，由状态机内部钳制）。
        // 进行中的状态一律复位成"没在演"：bombard 的同步位由 defineSynchedData 的默认 false 保证，
        // 抓取态则因为"本类不再逐 tick 钉人"而天然消失 —— 这就是它最硬的一条松手出口。
        this.phaseThree.load(tag);
        // 出场演出<b>刻意不落盘</b>（没有对应的 NBT 键），读回时一律复位成"没在出场"：
        // ① 它是首次召唤的一次性演出（initialRespawnDone 已落盘保证不重演），续播没有意义；
        // ② 续播还会错位 —— 降临的基准是"开演那一 tick 的悬停高度"，服务器存盘/区块卸载后这个基准已经丢了，
        //    若接着从半空插值，BOSS 会擦着地面或悬在半空落地（比直接复位更糟）。
        // 复位后由 AgaitolosMoveControl ③ 的悬停修正把 BOSS 收回常态高度，无需额外处理。
        this.introTicks = 0;
        this.setIntroPlaying(false);
    }

    // ---------------------------------------------------------------- 随在场玩家数增强（§0 第 71 行 / §1 定案表第 11 条）

    /**
     * 生命缩放修饰符的 UUID。
     * <p>做成 {@code ADDITION} 的<b>永久</b>修饰符而不是直接改基础值：{@code AttributeInstance#save()} 会写
     * 基础值 + 永久修饰符两者，但走修饰符才能让"重算"变成幂等的"摘旧的、挂新的"（见
     * {@link #setScalingBonus}），基础值则始终是规格里的 1444 / 30 这两个数，读起来不会自相矛盾。
     */
    private static final UUID UUID_SCALE_HEALTH = UUID.fromString("a1c2e3f4-0b17-4c58-9d6e-2f3a4b5c6d71");

    /** 攻击力缩放修饰符的 UUID（理由同 {@link #UUID_SCALE_HEALTH}） */
    private static final UUID UUID_SCALE_ATTACK = UUID.fromString("a1c2e3f4-0b17-4c58-9d6e-2f3a4b5c6d72");

    /**
     * <b>重算</b>场内人数并按它缩放生命与攻击力（服务端；只在入场与每次进阶段两个点调用）。
     * <p>调用点见 {@code aiStep} 的首次召唤分支与 {@link #advancePhase}；<b>不做</b>每 tick 跟随，
     * 理由见 {@link #scaledPlayerCount}。
     */
    private void applyPlayerCountScaling() {
        this.applyScalingFor(AgaitolosScaling.countPresentPlayers(this));
    }

    /**
     * 按给定人数缩放（幂等）。
     * <p>
     * <b>为什么按"比例"保留当前血量而不是按绝对值</b>：入场那一路刚把血量压到
     * {@code INTRO_HEALTH_RATIO}（见 {@code aiStep}），若直接抬高上限，压在 25% 的血会变成
     * "1877 上限里的 361（19%）"，出场演出的回血曲线立刻走形；按比例搬移则"压到 25%"的语义不变。
     * 阶段推进那一路本来就紧接 {@link #enterRespawn} 的回满，故这条口径对它同样无害。
     *
     * @param players 已定案的场内人数（未钳制，由 {@link AgaitolosScaling} 内部钳制）
     */
    private void applyScalingFor(int players) {
        this.scaledPlayerCount = AgaitolosScaling.clampPlayerCount(players);
        float healthRatio = this.getMaxHealth() > 0.0F ? this.getHealth() / this.getMaxHealth() : 1.0F;
        setScalingBonus(Attributes.MAX_HEALTH, UUID_SCALE_HEALTH, "Agaitolos player scaling health",
                AgaitolosScaling.BASE_HEALTH * (AgaitolosScaling.healthScale(this.scaledPlayerCount) - 1.0D));
        setScalingBonus(Attributes.ATTACK_DAMAGE, UUID_SCALE_ATTACK, "Agaitolos player scaling attack",
                AgaitolosScaling.BASE_ATTACK_DAMAGE * (AgaitolosScaling.damageScale(this.scaledPlayerCount) - 1.0D));
        // 上限变了：按旧比例搬回当前血量（setHealth 内部按新上限夹取，故缩小时也不会超上限）
        this.setHealth(this.getMaxHealth() * healthRatio);
    }

    /**
     * 幂等地设置一条属性加成：先按 UUID 摘掉旧值，再按需挂新值。
     * <p>用 {@code addPermanentModifier} 而非 {@code addTransientModifier}：前者会被
     * {@code AttributeInstance#save()} 序列化 ⇒ 区块卸载/读档后加成<b>不丢</b>
     * （原版 {@code Mob#finalizeSpawn} 的刷怪加成正是这么做的）。
     * <p>加成为 0（单人局）时<b>不挂</b>：少一条无意义的修饰符，也让存档里的属性块保持干净。
     */
    private void setScalingBonus(Attribute attribute, UUID id, String name, double bonus) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (bonus > 1.0E-6D) {
            instance.addPermanentModifier(new AttributeModifier(id, name, bonus, AttributeModifier.Operation.ADDITION));
        }
    }

    /** 已定案的场内人数 n（落盘；客户端读到的是默认 1 —— 本值只在服务端被消费） */
    public int getScaledPlayerCount() {
        return this.scaledPlayerCount;
    }

    /** 生命缩放系数（1 人 = 1.0）：供血量上限的读法与调试用 */
    public double getHealthScale() {
        return AgaitolosScaling.healthScale(this.scaledPlayerCount);
    }

    /**
     * <b>固定数值伤害</b>的缩放系数（1 人 = 1.0）：供普攻真实段与凋零头弹体读取。
     * <p>百分比最大生命的招式（俯冲镰扫/踢击/投技）<b>不</b>读它，理由见 {@link AgaitolosScaling} 的类注释。
     */
    public double getDamageScale() {
        return AgaitolosScaling.damageScale(this.scaledPlayerCount);
    }

    // ---------------------------------------------------------------- 阶段机 / 复活阶段

    @Override
    public void aiStep() {
        super.aiStep();
        // 阶段机是服务端权威，客户端只消费同步数据（血条已在客户端 overlay 侧自绘，本类不再持有）
        if (this.level().isClientSide()) {
            return;
        }
        // 规格（设计文档 §1 第 14 条）：召唤之后先进复活阶段快速回复血量。
        // 只在实例的最初 tick 触发一次；initialRespawnDone 落盘，避免读档重演。
        if (this.tickCount <= 1 && !this.initialRespawnDone) {
            // P2 接入点：此处之后还要铺下界牢狱场地，并在复活结束后对牢狱内生物施放凋零 III
            // 先把血量压到 INTRO_HEALTH_RATIO 再进复活阶段：满血进场时复活阶段的回血在画面上完全不可见
            // （详见 INTRO_HEALTH_RATIO 的注释）。只此一处压血，阶段推进触发的复活不压。
            this.setHealth(this.getMaxHealth() * INTRO_HEALTH_RATIO);
            this.enterRespawn();
            // 铺下界牢狱场地（分级施工/快照落盘/重启自愈全在 NetherPrisonArena 内，实体侧只留这一个入口）。
            // 必须在 startIntro() 之前调用：场地中心取"召唤点"（blockPosition），
            // 而出场演出第一件事就是把 BOSS 抬到悬停高度上方，放在后面会把中心抬高 3.5 格。
            NetherPrisonArena.begin(this);
            // 随在场玩家数增强：<b>入场即定案一次</b>（§0 第 71 行 / §1 第 11 条，用户拍板不做实时跟随）。
            // 必须排在 begin 之后：牢狱记录此刻已建立 ⇒ 人数判据走"牢狱 box"这条正路
            // （排在 begin 之前只能退化为半径兜底，还会把刚压好的 25% 血算成另一套上限）。
            // 也必须排在 startIntro 之前：出场降临的目标高度按新的悬停几何算，不受本次缩放影响，但
            // setHealth 的基准（getMaxHealth）必须已经是缩放后的值，否则开场血线走形
            this.applyPlayerCountScaling();
            // 纯表现：出场瞬间的一次性青蓝爆发（识别色同族），不参与任何结算
            AgaitolosFx.introBurst(this);
            // 出场演出（三段编排）与复活阶段同刻开演、同窗结束：开演那一刻就把 BOSS 抬到悬停高度上方，
            // 之后 40t 降临、40~60t 落地冲击。只在此分支起手 ⇒ 阶段推进触发的复活不会重放（见 tickIntro）
            this.startIntro();
        }
        // 复活阶段内不再判阶段阈值，否则刚进 PHASE_2 就会被残留的低血量直接推到 PHASE_3
        if (this.isRespawning()) {
            this.tickRespawn();
        } else {
            this.checkPhaseAdvance();
        }
        // ---- 计时（与"选招"无关，一律无条件推进）----
        // 惩罚计时（禁飞/封印/召唤冷却）与招式冷却分开：前者是玩家争取来的窗口，后者是 BOSS 自己的节奏，
        // 混在一个方法里改一处很容易把另一处的口径带偏（历史上"改一招忘一招"的常见来源）。
        this.tickPenaltyTimers();
        this.tickActionCooldowns();
        // ---- 进行中状态（只推进"已经在演"的那一招，不再承担起手判定）----
        // 起手判定已整体移交 AgaitolosSkillDirector：这三条只剩倒计时、打断出口与冲锋位移，
        // 顺序不再需要"谁先立状态"这种约定（状态互斥由决策层的全局动作锁保证，比原先的顺次闸更硬）
        this.tickChargeState();
        this.tickGuardState();
        this.tickDiveState();
        // ---- 阶段三五招的进行中状态（三重投掷节拍 / 高空轰炸 / 抓取态 / 劈击 / 施法）----
        // 与上面三条同一档：只推进"已经在演"的那一招，起手判定归决策层；
        // 放在决策层之前，保证"本 tick 刚结束的状态"已释放全局动作锁，决策层不会白等一拍。
        // 抓取态（投技①）的钉住动作也在这里逐 tick 执行，故它不持有任何玩家侧状态。
        this.phaseThree.tick(this);
        // ---- 地面档：只切"垂直高度档位"（空中 2 格 / 贴地 0 格），不碰任何招式状态 ----
        // 放在状态推进之后、决策层之前：本 tick 切出来的档位会被下一 tick 的
        // AgaitolosMoveControl ③ 读到（moveControl.tick 在 super.aiStep() 内已跑过，故当帧不生效，
        // 这与"实体写 deltaMovement、下一帧 travel 消费"是同一个半拍延迟，肉眼不可见）
        this.tickPerchState();
        // ---- 战斗决策层：这一拍放哪一招（唯一入口）----
        // 放在所有状态推进之后：本 tick 刚结束的招已释放动作锁，决策层能立刻看到最新状态；
        // 也保证"某招在本 tick 被中断"时不会再被决策层当成"正在演"而白等一拍
        this.skillDirector.tick(this);
        // 出场演出排在<b>最末</b>：它要独占位移（写竖直分量），必须等所有可能改速度的状态都跑完；
        // 放在这里也保证"被决策层拦住的动作"先被判一遍，本 tick 不会既起手又降临
        this.tickIntro();
    }

    // ---------------------------------------------------------------- 出场演出编排（首次召唤，0~80t）

    /**
     * 出场演出的<b>服务端编排</b>（唯一入口，见 {@link #aiStep} 的首次召唤分支），三段：
     * <ol>
     *   <li><b>0~{@link #INTRO_PILLAR_END_TICKS}t 粒子柱</b>：BOSS 周身青蓝光焰（{@link AgaitolosFx#introPillar}）；</li>
     *   <li><b>0~{@link #INTRO_DESCENT_TICKS}t 降临位移</b>：从"悬停高度 + {@link #INTRO_DESCENT_HEIGHT}"逐 tick 插值降到悬停高度；</li>
     *   <li><b>{@link #INTRO_DESCENT_TICKS}~{@link #INTRO_SHOCK_END_TICKS}t 落地冲击环</b>：向外扩散的粒子环 + 地面尘
     *       （{@link AgaitolosFx#introShockRing}）。</li>
     * </ol>
     * 全段由 {@link #isIntroPlaying()}（同步数据 DATA_INTRO）对外表达，客户端动画控制器轮询同一个状态。
     * 数值一律为待调手感值 / P8 转配置项。
     * <p>
     * <b>为什么是"逐 tick 插值速度"而不是直接 {@code setPos}</b>：与 {@code tickDiveCharge} 同一手法 ——
     * 写 {@code deltaMovement} 后由 {@code travel} 走 {@code move(MoverType.SELF, …)}，位移会<b>经过碰撞</b>
     * （撞到地形就停下），而 {@code setPos} 是无碰撞的瞬移，降临途中可能把 BOSS 塞进方块里。
     * 代价只是"本 tick 写的速度由下一 tick 的 travel 消费"这半拍延迟，与冲锋完全一致、肉眼不可见。
     * <p>
     * <b>与 {@link AgaitolosMoveControl} 的共存</b>：MoveControl 在出场期间<b>整体让位</b>
     * （见其 {@code tick()} ⓪'），故本方法写的竖直速度不会被悬停修正改写、水平也不会被导航加速。
     * 纯按"剩余距离 / 剩余 tick"递推 ⇒ 每 tick 步长恒等于 {@code INTRO_DESCENT_HEIGHT / INTRO_DESCENT_TICKS}
     * （3.5/40 = 0.0875 格/tick，匀速、无抖），且 40 tick 内<b>精确</b>落在目标高度上（最后一步剩余距离恰好一步走完）。
     */
    private void tickIntro() {
        if (!this.isIntroPlaying()) {
            return;
        }
        // 已演出 tick 数（0 起）：起手那一 tick 为 0，与两段常量的时间窗口径一致
        int introAge = INTRO_DURATION_TICKS - this.introTicks;
        AgaitolosFx.introPillar(this, introAge);
        // ② 降临：40t 内线性收敛；40t 之后必须继续写竖直 0（见下）
        double motionY = 0.0D;
        if (introAge < INTRO_DESCENT_TICKS) {
            int remaining = INTRO_DESCENT_TICKS - introAge;
            motionY = (this.introDescentTargetY - this.getY()) / remaining;
        }
        // 水平恒 0：演出要原地降临（MoveControl 已让位，这里把残余水平速度也一并清掉，避免被击退/惯性带偏）。
        // 竖直在 ② 段之后仍写 0 是<b>必须</b>的：travel 会把上一 tick 的速度乘 0.91 留到下一 tick，
        // 若 40t 后撒手不管，末段那 0.0875 的残余速度会继续下沉（几何级数合计约 0.8 格），
        // 观感是"落地后又沉一下，随后被悬停修正拉回"。待调手感值 / P8 转配置项
        this.setDeltaMovement(0.0D, motionY, 0.0D);
        AgaitolosFx.introShockRing(this, introAge);
        if (--this.introTicks <= 0) {
            // 只出场一次：计时期满即撤状态，之后（含阶段推进触发的复活）不会再回到这里
            this.setIntroPlaying(false);
        }
    }

    /**
     * 开演：把 BOSS 抬到"悬停高度 + {@link #INTRO_DESCENT_HEIGHT}"作为降临起点，并立起状态与计时。
     * <p>
     * 目标高度取 {@link AgaitolosMoveControl#hoverY}（唯一的悬停高度算法，MoveControl 每 tick 用的是同一份），
     * 不用"当前坐标 + 3.5"：召唤坐标未必正好在悬停位（例如被指令放到地面、或第一 tick 只被修正了 0.2 格），
     * 用真实悬停高度才能保证 40t 后落点就是它之后该待的位置，演出结束不出现二次升降。
     * <p>
     * 抬升用 {@code setPos} 直接落位（同"落点"语义，不需要 {@code teleportTo} 的乘客/朝向处理）：
     * 一次性瞬抬 3.5 格由原版位置包下发给客户端，客户端会插值过去，读作"闪现在空中"。
     */
    private void startIntro() {
        // 目标高度取「本阶段真正的空中档高度」（AgaitolosMoveControl#airborneHeight）：
        // 与 MoveControl ③ 每 tick 维持的高度是同一个出口，演出结束时落点就是它之后该待的位置。
        // 出场必然发生在阶段一（首次召唤那一 tick），但这里刻意按阶段取而不是写死 HOVER_HEIGHT ——
        // 阶段三降高之后，两处若各写一份，将来任何"阶段切换后再演出"的场景都会漂移出一次二次升降。
        double hoverY = AgaitolosMoveControl.hoverY(this.level(), this.getX(), this.getY(), this.getZ(),
                AgaitolosMoveControl.airborneHeight(this.getPhase()));
        // 虚空/深井里扫不到地面（NaN）时不强行算高度，退化为"原地不动"，演出其余两段照常
        this.introDescentTargetY = Double.isNaN(hoverY) ? this.getY() : hoverY;
        this.setPos(this.getX(), this.introDescentTargetY + INTRO_DESCENT_HEIGHT, this.getZ());
        // 清掉召唤瞬间可能残留的速度：起点必须是干净的，否则 ② 的匀速插值会被叠加上一段漂移
        this.setDeltaMovement(Vec3.ZERO);
        this.introTicks = INTRO_DURATION_TICKS;
        this.setIntroPlaying(true);
    }

    /** 阶段阈值判定：生命比例跌破本阶段的下一阶段门槛即推进 */
    private void checkPhaseAdvance() {
        // 已死亡 ⇒ 阶段机必须停摆。
        // 漏这道的后果不是"少进一个阶段"而是不可逆的僵尸态：血量归零时 0 <= 阈值 恒成立 ⇒ advancePhase ⇒
        // enterRespawn 开始回血 ⇒ isDeadOrDying() 变回 false ⇒ LivingEntity#tick 的
        // 「isDeadOrDying() && shouldTickDeath()」不再成立 ⇒ tickDeath 停摆 ⇒ 实体**永远不被移除**，
        // 表现为"死亡动画冻在半途 + 血条满格 + 打不死"。
        // 当前该路径**尚不可达**（单次伤害被锁伤 max(24, 5%maxHealth) 卡在 72.2，无法从 >50% 一刀到 0；
        // /kill 又被受击管线第 ① 步『非玩家来源免伤』挡下），但它是一颗引信：任何放开伤害上限的改动都会踩爆。
        if (this.isDeadOrDying()) {
            return;
        }
        AgaitolosPhase phase = getPhase();
        double threshold = phase.healthThresholdRatio();
        // healthThresholdRatio() 用 -1 表示"无阈值"：PHASE_3 与 RESPAWN 都走这条，不可能再推进
        if (threshold < 0.0D) {
            return;
        }
        // 用 <=：规格"每失去一半的生命值进入下一阶段"，恰好在门槛上也算进入
        if (this.getHealth() / this.getMaxHealth() <= threshold) {
            this.advancePhase(phase.nextCombatPhase());
        }
    }

    /** 推进阶段：落阶段 + 进复活阶段。换模型（P7）与解锁招式（P4~P6）后续在此接入 */
    private void advancePhase(AgaitolosPhase next) {
        this.setPhase(next);
        // 随在场玩家数增强：<b>每次进阶段重算一次</b>（§1 第 11 条，用户拍板的口径 —— 玩家中途进出
        // 只在阶段边界生效）。放在 enterRespawn 之前：复活阶段会把血量回满，上限必须先定案，
        // 回血的分母（getMaxHealth() / RESPAWN_DURATION_TICKS）才是新上限的 1/80。
        // 同时这里也是"阶段阈值跟着上限走"的另一半：阈值本就读 当前 上限的比例（AgaitolosPhase#healthThresholdRatio），
        // 上限一涨，二/三阶段的门槛立刻同步上涨（见该枚举的 javadoc）。
        this.applyPlayerCountScaling();
        this.enterRespawn();
    }

    /**
     * 进入复活阶段：无敌 + 4s 内回满，倒计时归零时击飞周围玩家。
     * <p>
     * <b>同时整队回收召唤物</b>（首次召唤与阶段推进两条入口都经这里）：复活是"无敌 + 回血 + 4s 演出"的
     * 场景边界 —— 这段窗口里留在场上的小怪只会单方面追打玩家（BOSS 无敌、又不该让"召唤物还能打"
     * 成为复活的附带伤害）。阶段推进必然先把蓄力打断（{@code tickChargeState} 的 ① 闸会走到
     * {@code endChargeInterrupted}，那里已经清过一次），这里是同一口径的显式化：
     * <b>"进入复活 ⇒ 场上无召唤物"是一条不变量，不依赖调用顺序</b>。
     * <p>首次召唤那一 tick 也走本方法，此时场上本来就没有召唤物，{@code dismissAll} 是空操作
     * （代价只是一次 64 格实体查询，一次性开销）。
     */
    private void enterRespawn() {
        this.initialRespawnDone = true;
        this.respawnTicks = RESPAWN_DURATION_TICKS;
        this.setRespawning(true);
        AgaitolosMinionSkill.dismissAll(this);
    }

    /** 复活阶段每 tick：回血并递减倒计时，归零则收尾 */
    private void tickRespawn() {
        if (--this.respawnTicks > 0) {
            // 每 tick 回 maxHealth/80：4s 恰好回满；heal 内部按最大生命夹取，不会溢出
            this.heal(this.getMaxHealth() / RESPAWN_DURATION_TICKS);
            return;
        }
        this.heal(this.getMaxHealth());
        this.setRespawning(false);
        this.knockbackNearbyPlayers();
        // 规格 §0：复活后对下界牢狱内的所有生物（含玩家）施加凋零 III 持续 5s。
        // 挂在这里而不是"首次召唤"分支：复活阶段有两个入口（首次召唤 / 阶段推进），两处都要表现
        NetherPrisonArena.applyRespawnWither(this);
    }

    /** 复活结束的击飞：半径内玩家被推离 BOSS 并上抛 */
    private void knockbackNearbyPlayers() {
        double radiusSqr = RESPAWN_KNOCKBACK_RADIUS * RESPAWN_KNOCKBACK_RADIUS;
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(RESPAWN_KNOCKBACK_RADIUS))) {
            if (player.distanceToSqr(this) > radiusSqr) {
                continue; // 取到的是方盒，按球半径再筛一遍
            }
            Vec3 offset = player.position().subtract(this.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            // 玩家恰好位于 BOSS 正下方时水平向量无法归一化，退化为纯上抛
            Vec3 direction = horizontal.lengthSqr() > 1.0E-6D ? horizontal.normalize() : Vec3.ZERO;
            player.setDeltaMovement(direction.x * RESPAWN_KNOCKBACK_HORIZONTAL,
                    RESPAWN_KNOCKBACK_VERTICAL,
                    direction.z * RESPAWN_KNOCKBACK_HORIZONTAL);
            // 服务端改速度不会自动下发，置 hurtMarked 让原版把速度包发给该玩家
            player.hurtMarked = true;
        }
    }

    // ---------------------------------------------------------------- 格挡架势（一阶段）

    /**
     * 格挡架势的<b>进行中状态</b>推进（服务端权威，每 tick 一次）：架势倒计时 + 强制收势出口 + 姿态朝向对齐。
     * <p>起手判定已移交 {@link AgaitolosSkillDirector}：<b>"该不该架"由决策层的保命招硬闸确定性判定</b>
     * （{@code shouldGuardForced}，不参与权重掷骰 —— 2026-09-21 修正：交回掷骰会让"该架的时候多半不架"），
     * 本方法只剩两件事：①"已经在举的架势怎么结束"（倒计时归零、或撞上复活/出场/死亡演出必须立刻收势）；
     * ② 守住架势方向 —— 每 tick 把头按回起手锁定的身体朝向（2026-09-21 补，理由见方法内注释）。
     * <p>架势只是"状态"，判定与消费在别处：伤害归零见 {@link AgaitolosDamageRules#resolve} 的 ④ 闸，
     * 朝向/近战判定见 {@link AgaitolosGuardSkill#isBlocking}，动画见 {@code AgaitolosAnimations} 的 guard 控制器。
     */
    private void tickGuardState() {
        if (!this.isGuarding()) {
            return;
        }
        // 复活阶段/死亡/出场演出必须立刻撤架势：无敌演出期间还举着格挡会与死亡/复活/降临姿势打架，也白吃一次免伤
        if (this.isRespawning() || this.isIntroPlaying() || this.isDeadOrDying() || --this.guardTicks <= 0) {
            this.endGuard();
        } else {
            // 姿态朝向对齐：把头部朝向按回身体朝向（身体 yaw 见 startGuard 的起手锁定）。
            // 原版 LookControl 每 tick 都把 yHeadRot 转向当前目标，而客户端渲染的身体朝向
            // （BodyRotationControl 跟随 yHeadRot）因此会跟着玩家转过去 —— 不对齐就会出现
            // "身子已经转向玩家、但格挡锥仍锁在起手方向"的所见非所得错位（用户反馈的"明明在架盾却挡不住"）。
            // 对齐后：渲染朝向 == 格挡锥方向（AgaitolosGuardSkill#isWithinFrontArc 读 yRot）
            // == 护盾纹方向（AgaitolosActionFx#guardAura 也读 look 的水平分量）。
            // 另一侧保证在 AgaitolosMoveControl：它在架势期间跳过身体 yaw 的转向（见其 tick() ⓪''），
            // 故这里的锁定不会每 tick 被"转向目标"覆盖掉 —— 两处合起来才是"整个架势朝向不变"。
            // 代价是架势期间头部不再追人，这恰是我们要的语义：举镰是一段"有方向的承诺"，
            // 玩家能看到它锁定了哪一侧，从那一侧之外绕过去就能打穿。
            this.setYHeadRot(this.getYRot());
        }
        // 纯表现：架势仍在的每 tick 刷一帧正面护盾纹；放在撤架势判定<b>之后</b>，
        // 本 tick 刚放下的架势不会再亮一帧，避免"盾已收还亮着"
        AgaitolosActionFx.guardAura(this);
    }

    /**
     * 执行入口：起手格挡架势（由 {@link AgaitolosSkillDirector} 在"保命招硬闸命中格挡"时调用）。
     * <p>前置复校（冷却/距离）与决策层的硬闸判据同源（{@code AgaitolosSkillDirector#shouldGuardForced}）：
     * 决策层据此"必定"起手，这里再挡一次，保证"即使将来有人在别处直接调用本方法"也不会绕过冷却。
     * <p>
     * <b>起手瞬间锁定朝向（2026-09-21 补，2026-09-21 二轮复核后保留）</b>：把身体朝向对准目标，格挡锥才有意义。
     * 姿势角度的判定读的是 {@link #getYRot()} 的水平分量（见
     * {@link AgaitolosGuardSkill#isWithinFrontArc}）。身体 yaw 自本轮起由
     * {@code AgaitolosMoveControl} 每 tick 朝目标限速转动（根因修复，字节码证据见该类 javadoc），
     * 故"起手那一刻的朝向"通常已经≈目标方向；但这里的显式对准<b>必须保留</b>，两条理由：
     * <ol>
     *   <li><b>硬保证</b>：转速是限速的（{@code MAX_YAW_SPEED_DEGREES} 度/tick），起手前一刻可能还差几度；
     *       架势锥只有 120°（半角 60°），"差几度"不该由运气决定 ⇒ 起手这一帧直接对准，锥心零误差；</li>
     *   <li><b>可惩罚面</b>：{@code AgaitolosMoveControl#tick()} ⓪'' 在<b>架势期间跳过转向</b>，
     *       故这里的锁定在整个 24t 架势里保持不变 —— 玩家仍能从锁定方向之外绕过去打穿（不是 360° 无死角）。</li>
     * </ol>
     * 写 {@code yHeadRot} 的理由同 {@code AgaitolosBlinkSkill#faceTarget}：渲染的头部朝向读它，
     * 不写会看到"身子转了、头还在看原来的方向"。
     *
     * @return 是否真的起手（冷却中/距离不满足/已被别的状态占用时为 false，此时不消耗全局节拍）
     */
    boolean startGuard(LivingEntity target) {
        if (this.guardCooldownTicks > 0 || this.isRespawning() || this.isIntroPlaying() || this.isDiving()
                || this.isCharging() || !this.isTargetWithinGuardRange(target)) {
            return false;
        }
        this.setGuarding(true);
        this.guardTicks = AgaitolosGuardSkill.GUARD_DURATION_TICKS;
        // 朝向锁定：身体 + 头部同写（俯仰不锁 —— 它由 LookControl 按目标眼睛高度逐 tick 给，
        // 不影响水平面的格挡锥，也让"低头看着脚下的人"这一幕保留）
        float yaw = this.yawTowards(target);
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        // 架势要站定：先停掉寻路，否则 Goal 会继续下发目标点。
        // （水平加速另在 AgaitolosMoveControl 里掐断——travel 在 super.aiStep() 内已执行，此处清速度对当帧无效）
        this.getNavigation().stop();
        return true;
    }

    /**
     * 水平朝向目标所需的 yaw（度）。公式与 {@code AgaitolosBlinkSkill#faceTarget} 同源
     * （原版惯例：{@code atan2(dz, dx)} 转角度后 − 90°；MC 里 yaw 0 = 朝 +Z、−90 = 朝 +X）。
     * <p>水平位移为 0（目标恰在正上/正下方）时得 {@code atan2(0, 0) = 0} ⇒ 一个<b>确定</b>但任意的角；
     * 该情形下"是否正面"由 {@link AgaitolosGuardSkill#isWithinFrontArc} 的退化分支直接判为正面，
     * 不依赖这里的取值。
     */
    private float yawTowards(LivingEntity target) {
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        return (float) (Mth.atan2(deltaZ, deltaX) * (180.0D / Math.PI)) - 90.0F;
    }

    /**
     * 只读探针：是否该起手格挡 —— 目标存活 + 水平距离已进近战可达范围（= 马上要挨打）。
     * <p>距离口径复用 {@link #getMeleeAttackRangeSqr}（已按悬停高度放大），让"起手格挡"与"打得着它"用同一把尺子；
     * 判据本身仍是<b>水平</b>距离² 与那个 3D 可达² 比较（沿用既有口径，不擅自改成 3D —— 那会改变起手时机）。
     * <p>供 {@link AgaitolosSkillDirector} 打分使用（本类不自持"要不要格挡"的判断，只提供探针）。
     */
    boolean isTargetWithinGuardRange(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        return deltaX * deltaX + deltaZ * deltaZ <= this.getMeleeAttackRangeSqr(target);
    }

    /**
     * 只读探针：目标是否已进<b>近战可达距离</b>（3D，含悬停高度折算）。
     * <p>
     * 与 {@code AgaitolosMeleeAttackGoal} 用的是<b>同一个</b> {@link #getMeleeAttackRangeSqr}：
     * 决策层用它判断"这一拍要不要选普攻"，Goal 用它判断"这一刀能不能挥出去"，
     * 两处若各算一套，就会出现"打分说打得着、挥出去落空"或反过来的空拍。
     */
    boolean isTargetWithinMeleeReach(LivingEntity target) {
        return target != null && target.isAlive()
                && this.distanceToSqr(target) <= this.getMeleeAttackRangeSqr(target);
    }

    // ---------------------------------------------------------------- 恶怨倒转（一阶段：召唤分队 + 蓄力）

    /**
     * 「恶怨倒转」的<b>进行中状态</b>推进（服务端权威，每 tick 一次）：蓄力倒计时 → 四条取消条件 → 收尾。
     * <p>
     * 规格：召唤凋零骷髅分队（见 {@link AgaitolosMinionSkill#summon}）后进入蓄力状态 12s；
     * 期间造成<b>足量的伤害</b>则取消此状态；<b>未取消</b>则吸取周围小怪剩余血量造成 1/5 的魔法伤害（范围 20 格）。
     * <p>
     * <b>四条取消条件与"是否结算"的分流</b>（这是本招唯二的口径，改一处必须同时看另一处）：
     * <ul>
     *   <li>死亡 / 复活演出 → {@link #endChargeInterrupted()}（不结算）；</li>
     *   <li>累计承伤 ≥ {@code getMaxHealth() × }{@link #CHARGE_BREAK_DAMAGE_RATIO} → {@link #endChargeInterrupted()}（不结算）；</li>
     *   <li>召唤物全部阵亡 → {@link #endChargeInterrupted()}（不结算）；</li>
     *   <li>蓄力跑满 {@link #CHARGE_DURATION_TICKS} → {@link #endChargeCompleted()}（<b>唯一</b>会结算吸血的出口）。</li>
     * </ul>
     * 拆成两个语义入口而不是给公共清理加布尔参数：这样"哪条路径会吸血"由<b>方法名</b>直接表达，
     * 只有 {@code endChargeCompleted} 里写着 {@code drainMinions}，被打断路径物理上无从误触发。
     * <p>
     * <b>起手已不在本方法</b>：见 {@link #startCharge(LivingEntity)}（由 {@link AgaitolosSkillDirector} 调用）。
     */
    private void tickChargeState() {
        if (!this.isCharging()) {
            return;
        }
        // 纯表现：蓄力每 tick 上报一次，节流在 AgaitolosFx 内部（每 2 tick 一帧）。
        // 放在收尾判断之前：本 tick 只要 isCharging() 仍成立就发一帧，末帧多发一次无副作用。
        AgaitolosFx.chargedOrb(this);
        // ① 死亡/复活/出场演出必须立刻收势（与 tickGuardState 同一口径）：打断，不结算吸血
        if (this.isDeadOrDying() || this.isRespawning() || this.isIntroPlaying()) {
            this.endChargeInterrupted();
            return;
        }
        // ② 打断阈值（规格："如期间造成足量的伤害便取消此状态"）。
        //    阈值 = 最大生命 × CHARGE_BREAK_DAMAGE_RATIO（1444 × 6% ≈ 87），累计值由 hurt() 在
        //    伤害真正落地时累加、起手时归零。打断 ⇒ 不结算吸血（"取消此状态"即整招作废）。
        //    取值口径与算式见 CHARGE_BREAK_DAMAGE_RATIO 的 javadoc（6% 是为了让单人也能打断）。
        if (this.chargeDamageTaken >= this.getMaxHealth() * CHARGE_BREAK_DAMAGE_RATIO) {
            this.endChargeInterrupted();
            return;
        }
        // ③ 分队全灭：提前收势，不结算（没有小怪可吸，本就不该给伤害）。
        //    判"全灭"用 ALIVE_CHECK_RADIUS（64）而不是吸血结算的 20：召唤物追出去很远也算活着，
        //    否则会把"追敌中"误判成"已阵亡"、蓄力被提前打断。
        if (AgaitolosMinionSkill.findLivingMinions(this, AgaitolosMinionSkill.ALIVE_CHECK_RADIUS).isEmpty()) {
            this.endChargeInterrupted();
            return;
        }
        // ④ 期满（自然结束）：规格"如未取消便吸取周围小怪剩余血量造成 1/5 魔法伤害"的唯一触发点。
        //    倒计时放在最后递减：上面三条一旦判定成立就直接返回，此处不必再管剩余 tick（endCharge* 会清零）
        if (--this.chargeTicks <= 0) {
            this.endChargeCompleted();
        }
    }

    /**
     * 执行入口：起手「恶怨倒转」（由 {@link AgaitolosSkillDirector} 在"这一拍选中召唤"时调用）。
     * <p>
     * 与其它招的互斥不再写在这里：决策层的全局动作锁已经保证"同一时刻只允许一招在演"
     * （起手前会检查 {@code isDiving()/isGuarding()/isCharging()/复活/演出}）。
     * 本方法只保留两条<b>本招专属</b>的前置：召唤冷却未过、目标有效 —— 前者是规格给它的 15s 冷却，
     * 后者是"召唤物要共享谁的目标"（见 {@link AgaitolosMinionSkill#summon}）。
     * <p>先召唤再立状态：召唤失败（异常情况下 0 只落位）时下一 tick 就会因"全灭"立刻收势并进冷却，
     * 不会每 tick 空转起手。
     *
     * @return 是否真的起手（冷却中/目标无效时为 false，此时不消耗全局节拍）
     */
    boolean startCharge(LivingEntity target) {
        if (this.minionCooldownTicks > 0 || target == null || !target.isAlive()
                || this.isRespawning() || this.isIntroPlaying() || this.isDeadOrDying()) {
            return false;
        }
        AgaitolosMinionSkill.summon(this);
        this.chargeTicks = CHARGE_DURATION_TICKS;
        // 上一轮的承伤累计必须归零后再起手：否则"被打断过的那一轮"的累计会残留，
        // 下一轮起手第一 tick 就立刻再判成打断、连蓄力都立不起来
        this.chargeDamageTaken = 0.0F;
        this.setCharging(true);
        // 蓄力自发光（§1 第 22 条）：给 BOSS 自己挂 GLOWING，起手加、finishCharge() 摘。
        // 靠"自施药水"而非 setGlowingTag：发光是要随状态自动消失的临时状态，药水自带计时，
        // 且 GLOWING 走原版实体描边通道（LevelRenderer 对任何实体都生效，不需要改 renderer）。
        // 注意副作用：描边色由队伍决定，无队伍时为白色（不是紫色）；发光轮廓会穿墙可见。
        // 参数 (duration, amplifier=0, ambient=false, visible=false, showIcon=false)：不要药水气泡粒子，
        // 只要轮廓（气泡会被误读成"中毒"一类状态）。
        this.addEffect(new MobEffectInstance(MobEffects.GLOWING, CHARGE_GLOW_DURATION_TICKS, 0, false, false, false));
        return true;
    }

    /**
     * 蓄力收尾（<b>期满结算</b>）：蓄力跑满 {@link #CHARGE_DURATION_TICKS} 自然结束时走这条 ——
     * 先做公共清理，再结算「恶怨倒转」的吸血（规格："如未取消便吸取周围小怪剩余血量造成 1/5 的魔法伤害"）。
     * <p>结算收在 {@link AgaitolosReversalSkill#drainMinions}：本类只表达"这一轮蓄力没被打断"这一语义，
     * 半径、伤害折算与归属筛选归技能持有（与 {@link AgaitolosGuardSkill#counterAttack} 同一分工）。
     * <p><b>三步顺序不可调换</b>：{@code finishCharge}（撤蓄力状态，动画/出手闸先回常态）→
     * {@code drainMinions}（按规格吸血，内部 kill 掉 20 格内的）→ {@code dismissAll}（收尾清场）。
     * 若把清场提到吸血之前，吸血会把"已经消失的小怪"当成 0 血来算 ⇒ 这一招直接空放。
     */
    private void endChargeCompleted() {
        this.finishCharge();
        // 先收干净状态再结算：结算会杀死召唤物，期间 BOSS 不该还处于"蓄力中"（否则动画/出手闸会打架）
        AgaitolosReversalSkill.drainMinions(this);
        // 收尾清场：把 20 格外的漏网者、以及结算瞬间新追近的残余一并回收（口径见 dismissAll 的 javadoc）
        AgaitolosMinionSkill.dismissAll(this);
    }

    /**
     * 蓄力收尾（<b>被打断</b>）：死亡 / 复活 / 承伤达阈值 / 召唤物全灭走这条，<b>不结算吸血</b>
     * （规格：蓄力被打断即"此状态被取消"，吸不到任何东西 —— 这正是玩家抢输出的收益）。
     * <p>同时<b>整队回收</b>：打断的收益是"这一招白放"，不该变成"留下一整队小怪继续追着玩家打"
     * —— 旧口径只收状态不清场，正是用户实测到的"释放完技能之后怪物不会消失"。
     */
    private void endChargeInterrupted() {
        this.finishCharge();
        AgaitolosMinionSkill.dismissAll(this);
    }

    /**
     * 蓄力收尾的公共部分：清计时、清承伤累计、置冷却、撤状态。
     * <p>两个语义入口（{@link #endChargeCompleted()} / {@link #endChargeInterrupted()}）共用本方法，
     * 保证"只要不在蓄力就必须在冷却"这条不变量不会因分流而漏掉一边。
     */
    private void finishCharge() {
        this.chargeTicks = 0;
        // 承伤累计随收尾一起清：两个出口都清，下一轮起手（tickCharge 起手处也清一次）不会残留
        this.chargeDamageTaken = 0.0F;
        // 冷却按阶段折算（二阶段起更短）：倍率表见 AgaitolosPace
        this.minionCooldownTicks = AgaitolosPace.scaledCooldown(this, MINION_COOLDOWN_TICKS);
        this.setCharging(false);
        // 自发光随状态一起摘：否则会出现"球已经收了，人还亮着"的错位
        this.removeEffect(MobEffects.GLOWING);
    }

    // ---------------------------------------------------------------- 俯冲镰扫（一阶段：位移 + 惩罚）

    /**
     * 俯冲镰扫的<b>进行中状态</b>推进（服务端权威，每 tick 一次）：冲锋位移 → 抵达/超时收尾结算。
     * <p>
     * 两段式里的"冲锋中"这一段（{@link #isDiving()} → {@link #tickDiveCharge()}）留在这里；
     * <b>起手判定已移交</b> {@link AgaitolosSkillDirector}（"这一拍选不选俯冲"由决策层掷骰决定，
     * 冷却/封印/水平距离这三项也由它打分），执行入口见 {@link #startDiveSweep(LivingEntity)}。
     * <p>
     * 位移本体在 {@link #tickDiveCharge()}（本类直接写 {@code deltaMovement}，{@code AgaitolosMoveControl} 全让位）；
     * 伤害结算与范围筛选全部收在 {@link AgaitolosDiveSweepSkill#perform(AgaitolosEntity)}。
     */
    private void tickDiveState() {
        // 死亡/复活/出场演出期间中断冲锋：不结算横扫（演出期间不该出手），也不让计时残留到下一条命
        if (this.isDeadOrDying() || this.isRespawning() || this.isIntroPlaying()) {
            this.diveTicks = 0;
            return;
        }
        if (this.isDiving()) {
            this.tickDiveCharge();
        }
    }

    /**
     * 执行入口：起手俯冲镰扫（由 {@link AgaitolosSkillDirector} 在"这一拍选中俯冲"时调用）。
     * <p>前置复校与决策层打分同源：冷却未过 / 正举着架势 / 蓄力中 / 被格挡后封印期内 /
     * 水平距离不在 [{@link #DIVE_MIN_RANGE}, {@link #DIVE_TRIGGER_RANGE}] —— 任一成立都不起手。
     * 封印期只封这一招（普攻与凋零头照常），作用范围见 {@link #isScytheSealed()} 的 javadoc。
     *
     * @return 是否真的起手（被上面任一条挡下时为 false，此时不消耗全局节拍）
     */
    boolean startDiveSweep(LivingEntity target) {
        if (this.diveSweepCooldownTicks > 0 || this.isGuarding() || this.isCharging() || this.isScytheSealed()
                || this.isRespawning() || this.isIntroPlaying()
                || target == null || !target.isAlive() || !this.isWithinDiveTriggerRange(target)) {
            return false;
        }
        this.diveTicks = DIVE_MAX_TICKS;
        // 纯表现：冲锋起手即播。dive_sweep clip 本身覆盖「俯冲 + 横扫」两段，
        // 故冲锋收尾结算时<b>不再</b>重复触发（否则横扫段会被打断重播）。
        AgaitolosAnimations.playDiveSweep(this);
        return true;
    }

    /**
     * 冲锋中每 tick：朝目标点（身体中部）高速推进。
     * <p>水平与垂直<b>都</b>朝目标收敛：只修水平的话，常态悬停的 2 格竖直差永远消不掉，
     * "抵达"判定不可能成立，冲锋会次次超时。垂直修正只在冲锋期间做，不破坏常态悬停
     * （常态悬停仍由 {@code AgaitolosMoveControl} ③ 负责，且它在冲锋期间整体让位）。
     * <p>写入时机：本方法在 {@code aiStep} 的 {@code super.aiStep()} 之后执行，而 {@code travel} 在
     * {@code super.aiStep()} 内部、{@code MoveControl#tick} 之后 —— 即本 tick 写的速度由下一 tick 的
     * {@code travel} 消费，与 {@code AgaitolosMoveControl} 的写入时机同层，不存在额外的"晚一帧"偏差。
     */
    private void tickDiveCharge() {
        --this.diveTicks;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.diveTicks <= 0) {
            // 目标丢失/目标死亡/超时：原地收尾并结算一次横扫。
            // 超时是"被地形挡住或目标一直跑"的兜底，保证冲锋不会永久锁住状态机。
            this.finishDive();
            return;
        }
        Vec3 aim = diveAimPoint(target);
        // 纯表现：只在"真的在推进"的 tick 拖尾（上面的收尾分支已 return，故起手/收尾帧不会拖一条假尾）
        AgaitolosActionFx.diveTrail(this);
        double deltaX = aim.x - this.getX();
        double deltaY = aim.y - this.getY();
        double deltaZ = aim.z - this.getZ();
        double horizontalSqr = deltaX * deltaX + deltaZ * deltaZ;
        if (horizontalSqr <= DIVE_ARRIVE_DISTANCE * DIVE_ARRIVE_DISTANCE) {
            this.finishDive();
            return;
        }
        // 按 3D 方向归一化后乘速度：竖直分量也在内，故水平速度会随俯仰角自然减小。
        // 速度再乘阶段倍率（AgaitolosPace）：规格"二阶段比一阶段更加快速"，阶段一恒为 1.0 不受影响
        double length = Math.sqrt(horizontalSqr + deltaY * deltaY);
        // length > DIVE_ARRIVE_DISTANCE > 0，不会除零
        double scale = DIVE_SPEED * AgaitolosPace.moveSpeed(this) / length;
        this.setDeltaMovement(deltaX * scale, deltaY * scale, deltaZ * scale);
    }

    /** 冲锋收尾：清零计时并结算一次横扫；有人落入范围才进冷却（口径沿用上一轮：被格挡也算"这一刀挥出去了"） */
    private void finishDive() {
        this.diveTicks = 0;
        boolean swept = AgaitolosDiveSweepSkill.perform(this);
        // 纯表现：挥刀这一帧先落特效，再判冷却 —— 结算结果只决定"空挥还是命中"（特效强度），
        // 不影响冷却口径（仍是 swept 决定），故顺序只关乎观感：让刀光与伤害同帧出现
        AgaitolosActionFx.diveSweepImpact(this, swept);
        if (swept) {
            // 冷却按阶段折算（二阶段起更短）：倍率表见 AgaitolosPace，阶段一恒为原值
            this.diveSweepCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosDiveSweepSkill.SWEEP_COOLDOWN_TICKS);
        }
    }

    /** 冲锋瞄准点：目标<b>身体中部</b>（脚 + 半身高）——瞄脚会让 BOSS 一路贴地，瞄眼睛又会悬太高 */
    private static Vec3 diveAimPoint(LivingEntity target) {
        return new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }

    /**
     * 只读探针：是否满足冲锋起手距离 —— 与目标的<b>水平</b>距离落在 [{@link #DIVE_MIN_RANGE}, {@link #DIVE_TRIGGER_RANGE}]。
     * <p>用水平距离而非 3D 距离：悬停高度不该影响"够不够近"（沿用被替换掉的旧起手判定的口径）。
     * 超过上界交给飞行巡航接近；小于下界交给普攻/格挡 —— 贴脸再俯冲既无位移意义，也会把贴身战搅成连招。
     * <p>供 {@link AgaitolosSkillDirector} 打分与 {@link #startDiveSweep(LivingEntity)} 复校共用（同一口径，不复制第二份）。
     */
    boolean isWithinDiveTriggerRange(LivingEntity target) {
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        double distanceSqr = deltaX * deltaX + deltaZ * deltaZ;
        return distanceSqr >= DIVE_MIN_RANGE * DIVE_MIN_RANGE && distanceSqr <= DIVE_TRIGGER_RANGE * DIVE_TRIGGER_RANGE;
    }

    // ---------------------------------------------------------------- 二阶段两招（瞬击 / 高速踢击）

    /**
     * 二阶段两招共用的起手闸（服务端权威）。
     * <p>
     * <b>为什么两招共用一条判断</b>：它们的互斥条件完全相同，分开写就会出现"改一招忘一招"；
     * 与 {@link AgaitolosDamageRules} / {@link AgaitolosGuardSkill} 的收口思路一致。
     * <p>
     * 三段判据：
     * <ul>
     *   <li><b>阶段门</b>：两招都是规格里的二阶段招式（设计文档 §0 阶段二 / §5 的 P5），
     *       阶段一起手会破坏"二阶段才解锁"的契约；</li>
     *   <li><b>状态互斥</b>：死亡 / 复活演出 / 出场演出 / 架势 / 蓄力 / 冲锋任一成立都不起手 ——
     *       与 {@link AgaitolosSkillDirector} 的全局动作锁同款判据（该锁已把这几项统一收口，
     *       不再需要各招各写一份互斥列表，历史上"改一招忘一招"正是这么来的）；</li>
     *   <li><b>技能封印</b>（{@link #isScytheSealed()}）：封印<b>只封"大招"</b>（俯冲镰扫，
     *       以及后续接入的三重投掷 / 天魔灾 / 投技），<b>不封瞬击与高速踢击</b>，
     *       故本方法<b>不含</b>封印判定；作用范围与理由见 {@link #isScytheSealed()} 的 javadoc。</li>
     * </ul>
     * <p>
     * <b>与决策层的关系（本轮新增）</b>：起手的主闸已上移到 {@link AgaitolosSkillDirector}
     * （它用全局动作锁保证"同一时刻只允许一招在演"，并把阶段门与封印口径一并纳入打分）。
     * 本方法保留为<b>执行入口自己的复校</b>：{@code startBlink}/{@code startKick} 在被决策层调用时仍会走一遍，
     * 这样"将来有人在别处直接调用这两个入口"也不会绕过互斥条件（防御性重复，不是两份口径——
     * 判据本身只有这一处实现）。
     */
    private boolean canStartPhaseTwoSkill() {
        return this.isPhaseTwoOrLater() && !this.isDeadOrDying() && !this.isRespawning()
                && !this.isIntroPlaying() && !this.isGuarding() && !this.isCharging() && !this.isDiving();
    }

    /** 是否已进入二阶段（含三阶段）：二阶段招式的阶段门 */
    private boolean isPhaseTwoOrLater() {
        return getPhase().combatOrdinal() >= AgaitolosPhase.PHASE_2.combatOrdinal();
    }

    /**
     * 是否处于「免疫远程」的阶段（<b>阶段三专属</b>，规格："BOSS 免疫远程攻击"）。
     * <p>
     * 判据用 {@code combatOrdinal() >= PHASE_3}（与 {@link #isPhaseTwoOrLater()} 同一写法）：
     * 复活阶段序数为 0，天然不在此列 —— 那一段的免伤由受击管线第 ② 步（复活无敌）负责，
     * 两条口径不重叠。
     * <p>
     * <b>唯一消费点</b>是 {@link AgaitolosDamageRules#resolve} 的第 ⑥ 闸（弹射物伤害归零）。
     * 本方法只回答"这个阶段要不要免疫"，"什么算弹射物"的口径在
     * {@link AgaitolosDamageRules#isProjectileDamage} 一处，不在这里复制第二份。
     */
    public boolean isProjectileImmune() {
        return getPhase().combatOrdinal() >= AgaitolosPhase.PHASE_3.combatOrdinal();
    }

    /**
     * 执行入口：起手「瞬击」（由 {@link AgaitolosSkillDirector} 在"这一拍选中瞬击"时调用）；
     * 两段式里的"冷却"由 {@link #tickActionCooldowns()} 统一递减，本方法只剩落点判定与瞬移。
     * <p>
     * <b>"不飞行"为什么只能是禁飞窗口</b>：本 BOSS 常态低空悬停（{@code AgaitolosMoveControl} 恒定维持高度），
     * 唯一的落地状态就是被玩家格挡俯冲镰扫后授予的 30s 禁飞（{@link #onSweepBlocked()}）。
     * 于是瞬击天然是一招"<b>惩罚期的补偿手段</b>"：禁飞期间够不到目标，靠绕后瞬移把距离拉回近战范围
     * （详见 {@link AgaitolosBlinkSkill} 的类注释）。
     * <p>
     * <b>与技能封印的关系（2026-09-20 用户拍板：封印不覆盖瞬击）</b>：禁飞与封印由
     * {@code onSweepBlocked} <b>同刻授予、且同为 600 tick</b>，若瞬击也受封印约束，它的唯一起手窗口
     * 就会被完全覆盖、一次也放不出来。故瞬击走 {@link #canStartPhaseTwoSkill()}（<b>不含</b>封印），
     * 与踢击同属"不吃封印"的基础手段；作用范围见 {@link #isScytheSealed()} 的 javadoc。
     * <p>
     * 冷却落点两分支：<b>成功进完整冷却</b>（{@link AgaitolosBlinkSkill#BLINK_COOLDOWN_TICKS}，
     * 按阶段折算）；<b>落点校验失败只给短重试窗口</b>（{@link AgaitolosBlinkSkill#BLINK_FAILED_RETRY_TICKS}）
     * —— 失败不传送、不改朝向、不扣完整冷却，只是别每 tick 重扫方块。
     *
     * @return 是否真的起手（阶段未开放/状态冲突/不在地面/落点校验失败时为 false）
     */
    boolean startBlink(LivingEntity target) {
        if (!this.canStartPhaseTwoSkill() || !this.isGrounded()
                || target == null || !target.isAlive() || !AgaitolosBlinkSkill.canBlink(this, target)) {
            return false;
        }
        if (AgaitolosBlinkSkill.perform(this, target)) {
            this.blinkCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosBlinkSkill.BLINK_COOLDOWN_TICKS);
            return true;
        }
        // 落点校验没过：只吃短重试窗口，且<b>不算起手</b>（决策层据此只给一个短重试节拍，不空等整拍）
        this.blinkCooldownTicks = AgaitolosBlinkSkill.BLINK_FAILED_RETRY_TICKS;
        return false;
    }

    /**
     * 执行入口：起手「高速踢击」（由 {@link AgaitolosSkillDirector} 在"这一拍选中踢击"时调用）。
     * <p>
     * <b>与瞬击恰相反，本招不限定飞行/地面</b>（规格："任何状态下可用"）：悬停、禁飞、任何高度都能起手；
     * 但仍受 {@link #canStartPhaseTwoSkill()} 的六项互斥约束（死亡/复活/架势/蓄力/冲锋/封印）
     * —— "任何状态"指的是空间状态，不是"可以一边蓄力一边踢"。
     * <p>
     * 目标与距离一律复用既有口径：距离取 {@link #getMeleeAttackRangeSqr}（含悬停高度折算，
     * 与普攻/格挡同一把尺子）。出手即进冷却（被盾牌挡下也算"这一脚踢出去了"，与俯冲镰扫同一取舍），
     * 避免格挡成功时每 tick 空踢。
     *
     * @return 是否真的起手（阶段未开放/状态冲突/距离不够时为 false）
     */
    boolean startKick(LivingEntity target) {
        if (!this.canStartPhaseTwoSkill() || target == null || !target.isAlive()
                || !AgaitolosKickSkill.isWithinKickRange(this, target)) {
            return false;
        }
        if (AgaitolosKickSkill.perform(this, target)) {
            this.kickCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosKickSkill.KICK_COOLDOWN_TICKS);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- 阶段三五招（2026-09-21 补）

    /**
     * 只读探针：阶段三五招是否有招在演（全局动作锁的一段）。
     * <p>两个消费点：{@code AgaitolosSkillDirector#isBusy}（决定"这一拍还派不派新招"）与
     * {@link #doHurtTarget}（拦住原版 {@code MeleeAttackGoal} 那条旁路）。
     * <p>计时与节拍的持有者在 {@code AgaitolosPhaseThreeState}，本方法只是转发
     * （与 {@code isGuarding()} ↔ {@code tickGuardState()} 同款分工）。
     */
    boolean isPhaseThreeBusy() {
        return this.phaseThree.isBusy();
    }

    /**
     * 执行入口：起手「三重投掷」（由 {@link AgaitolosSkillDirector} 在"这一拍选中三重投掷"时调用）。
     * <p>本类只转发给状态机，起手复校（冷却 / 封印 / 状态互斥 / 阶段三 / 空中 / 射程）在
     * {@code AgaitolosPhaseThreeState#startTripleThrow} 里，节拍与 clip 的对齐关系见
     * {@code AgaitolosTripleThrowSkill}（三次出手落在 clip 的 6 / 12 / 18 tick）。
     *
     * @return 是否真的起手（未过复校时为 false，此时不消耗全局节拍）
     */
    boolean startTripleThrow(LivingEntity target) {
        return this.phaseThree.startTripleThrow(this, target);
    }

    /**
     * 执行入口：起手「饱和轰炸」（阶段三高空投弹，唯一一条循环 clip 驱动的持续状态）。
     * <p>起手成功后同步位 {@code DATA_BOMBARDING} 立起（动画与高空档都靠它），期满或整段中止时撤销。
     * <p>「被反弹则自伤」不需要本招额外接线：投出的仍是自家凋零头，玩家打回后 owner 变玩家，
     * 命中 BOSS 时自然落进 {@code AgaitolosDamageRules} 的 ⑤ 自伤通道（详见 {@code AgaitolosBombardSkill}）。
     */
    boolean startBombard(LivingEntity target) {
        return this.phaseThree.startBombard(this, target);
    }

    /**
     * 执行入口：起手「投技①（踩住 + 镰扫）」（阶段三、BOSS 在地面档、目标贴地且贴身）。
     * <p>抓取态的四条解除出口与"锁位移 + 跟随"的实现在状态机与 {@code AgaitolosGrabSkill} 里，
     * 本类只转发（<b>抓取不往玩家侧写任何状态</b>，故不可能永久锁人）。
     */
    boolean startGrabSweep(LivingEntity target) {
        return this.phaseThree.startGrabSweep(this, target);
    }

    /**
     * 执行入口：起手「投技②（抓摔 + 劈击）」。
     * <p>clip 播到 0.6s 时结算 20% 最大生命的真实伤害；本入口与状态机不碰任何客户端状态，
     * 也不改变任何伤害/冷却口径。（2026-09-24：原「BOSS 特写」相机表现已彻底删除。）
     */
    boolean startGrabSmash(LivingEntity target) {
        return this.phaseThree.startGrabSmash(this, target);
    }

    /**
     * 执行入口：起手「天魔＊灾」（阶段三，24 格内隔空锁定）。
     * <p>0.5s 处落地"不可名状 + 精神伤害改写"，改写口径（含"此后永久"与玩家侧落盘）唯一收在
     * {@code AgaitolosPsychic}；本入口不重复任何一条那条口径。
     */
    boolean startCalamity(LivingEntity target) {
        return this.phaseThree.startCalamity(this, target);
    }

    // ---------------------------------------------------------------- 受击 / 生命周期

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 受击报复排在一切结算之前：<b>"你打我了"这件事必须与"这一下打没打动"完全解耦</b>。
        // 若放在下面几条 return 之后，那么"被格挡 / 复活无敌 / 0.2s 冷却内"的这一次受击就不会触发报复——
        // 而玩家在无敌期狂点鼠标恰恰是最容易观察到"它不还手"的场景。
        this.retaliate(source);
        // 格挡预判：与 resolve 的 ④ 闸走的是**同一**个 AgaitolosGuardSkill.isBlocking（§3.3「同一判定器」）。
        // 这里只负责"格挡成功 → 发动反击 + 收势"，伤害归零的口径仍由 resolve 决定（本分支直接短路，等价于归零）。
        // 必须在 resolve 之前判：counterAttack 会顺手收势，若先 resolve，判定会因架势已撤而落空。
        if (AgaitolosGuardSkill.isBlocking(this, source)) {
            AgaitolosGuardSkill.counterAttack(this, source);
            // 纯表现：格挡成功的火花落在"伤害来源"那一侧（取 source 的位置，理由见 AgaitolosActionFx#guardSpark）。
            // 放在 counterAttack 之后无副作用：它只收架势与推人，不动 source
            AgaitolosActionFx.guardSpark(this, source);
            return false;
        }
        float resolved = AgaitolosDamageRules.resolve(this.level().getGameTime(), this.lastHurtGameTime,
                this.getMaxHealth(), this.isRespawning(), isReflectedSkull(source), this.isGuarding(),
                this.isProjectileImmune(), this, source, amount);
        if (resolved <= 0.0F) {
            return false;
        }
        this.lastHurtGameTime = this.level().getGameTime();
        // 原版 LivingEntity#hurt 在 invulnerableTime > 10 时走"只结算增量伤害"分支，会吞掉 10 tick 内的后续伤害；
        // 这与本 BOSS「每 0.2s 只吃一次」的规格冲突，故清零，让自定义 4 tick 冷却成为唯一闸门。
        this.invulnerableTime = 0;
        boolean applied = super.hurt(source, resolved);
        if (applied) {
            // 「恶怨倒转」的打断阈值累计：只统计<b>真正落地</b>的伤害，故放在 super.hurt 返回 true 之后。
            // 上面三条 return 已经排除了「被格挡（直接短路）/ 被免伤（非玩家来源、复活无敌）/ 被 0.2s 冷却」
            // 三类分支，再叠加这里的 applied 闸，阈值绝不会被"没打到 BOSS 身上"的伤害推进。
            // 只在蓄力中累加：非蓄力期不写这个字段，避免无意义的字段变动（起手时也会显式归零）。
            if (this.isCharging()) {
                this.chargeDamageTaken += resolved;
            }
            // 纯表现：伤害真正落地才播受击动作；不参与结算，也不改返回值
            AgaitolosAnimations.playHurt(this);
            // 纯表现：受击反馈粒子与受击动作同刻（同一个 applied 闸内，没真吃到伤害就不冒）
            AgaitolosActionFx.hurtFeedback(this);
        }
        return applied;
    }

    /**
     * 受击报复：把伤害归属者（玩家）锁成当前目标。
     * <p>
     * <b>为什么必须显式做这一步</b>：本类此前<b>从不主动 setTarget</b>，唯一的目标来源是
     * {@code NearestAttackableTargetGoal<Player>} 的主动索敌（mustSee=true ⇒ 索敌要求视线）。
     * 于是"从背后打、隔着掩体打、刚进场时打"都可能落在"没有目标"的状态里：
     * BOSS 既不追也不还手，玩家看到的就是"我打它，它不理我"。
     * 配套的 {@code HurtByTargetGoal}（见 {@link #registerGoals()}）读的是 {@code getLastHurtByMob}，
     * 而那条字段由 {@code LivingEntity#hurt} 写——只有伤害<b>真正落地</b>时才写；
     * 复活无敌 / 0.2s 冷却 / 被格挡这几条早期 return 都会绕过它，故这里独立补一道。
     * <p>
     * <b>不覆盖已有目标</b>：正在打的人不该因为旁边有人蹭了一下就换目标
     * （那属于 {@code HurtByTargetGoal} 的职责，它按"最后打我的人"排序，同样不会乱换）。
     * 这里只补"当前没有有效目标"这一种情况。
     */
    private void retaliate(DamageSource source) {
        if (!(source.getEntity() instanceof Player player) || !player.isAlive()) {
            return;
        }
        LivingEntity current = this.getTarget();
        if (current == null || !current.isAlive()) {
            this.setTarget(player);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // 整队回收召唤物：BOSS 一死，分队就没有存在理由（它们是这一招的产物，不是野外怪）。
        // 放 die() 而不是等 tickDeath 收尾：死亡演出还有 2.5s（DEATH_TICKS），
        // 若等到那时才清，玩家会看到"BOSS 已经倒了，小怪还在打人"这半段错位。
        AgaitolosMinionSkill.dismissAll(this);
        // 下界牢狱开始还原（设计 §4.4 的"BOSS 死亡 ⇒ 场地还原"）：只置状态、不动方块，
        // 逐格还原由 NetherPrisonArena 的分批节拍做，死亡演出（2.5s）与还原可以并行
        NetherPrisonArena.end(this);
        // 纯表现：死亡瞬间的一次性青蓝爆发。只做表现、不干扰结算与回收。
        // die() 每次死亡只会进来一次：原版 LivingEntity#hurt 在 isDeadOrDying() 时直接 return false，
        // 且 LivingEntity#die 自身有 !this.dead 闸，故不会重复爆发。
        AgaitolosFx.deathBurst(this);
    }

    /**
     * 死亡演出：把 death clip（2.5s）放完再走原版收尾。
     * <p>
     * 原版在 {@code deathTime >= 20}（1s）就 {@code remove(KILLED)}，2.5s 的死亡动画只能看到前 40%；
     * 这里把收尾门槛抬到 {@link #DEATH_TICKS}，其余逐行照抄 {@code LivingEntity#tickDeath}
     * （{@code broadcastEntityEvent((byte) 60)} + {@code remove(KILLED)}），<b>不另开收尾路径</b>。
     * <p>血条无需在此回收：它已是客户端 overlay，实体从客户端世界消失后血条自然不再绘制。
     * <p>
     * <b>尸体姿态安全性（已实测源码）</b>：本 BOSS 由 GeckoLib 的 {@code GeoEntityRenderer} 渲染，
     * 它继承的是 {@code EntityRenderer} 而<b>不是</b> {@code LivingEntityRenderer}，
     * 死亡倾倒角这段代码根本不参与；即便参与，{@code LivingEntityRenderer#setupRotations} 里
     * {@code f = Mth.sqrt((deathTime + partialTicks - 1) / 20 * 1.6); if (f > 1.0f) f = 1.0f;}
     * 也已把倾倒系数钳在 1.0（deathTime ≈ 14 即到顶），更长的 deathTime 不会让尸体继续翻转。
     */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        // 纯表现：死亡演出期间逐帧外逸的灵魂（服务端限定在 AgaitolosActionFx 内兜底）。
        // 挂在演出计时上而不是 die()：die() 只有一帧，而"灵魂外逸上升"是一段需要逐帧推进的过程
        AgaitolosActionFx.deathSoulRise(this, this.deathTime);
        if (this.deathTime >= DEATH_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    /**
     * 实体被"从世界移除"（死亡收尾 {@code KILLED} / {@code discard} / 换维度）时整队回收召唤物。
     * <p>
     * <b>为什么挂在 {@code remove} 而不是 {@code setRemoved}</b>（实测 1.20.1 字节码）：
     * {@code Entity#setRemoved} 是 <b>final</b>（编译期就会报"无法覆盖 final 方法"），
     * 而 {@code Entity#remove(reason)} 内部正是 {@code setRemoved(reason)} + {@code invalidateCaps()}、
     * {@code LivingEntity#remove} 再补 {@code brain.clearMemories()} ⇒ 覆写 {@code remove} 就覆盖了
     * 所有走 {@code remove}/{@code discard} 的路径。
     * <p>
     * <b>区块卸载这一支不在这里</b>：{@code PersistentEntitySectionManager#unloadEntity(EntityAccess)}
     * 直接调 {@code setRemoved(UNLOADED_TO_CHUNK)}，<b>不经过 {@code remove}</b>，而 {@code setRemoved}
     * 又是 final（无钩子可挂）。故那一种情形改由<b>召唤物自带的看门狗</b>兜底：
     * {@code AgaitolosMinionWatchdogGoal} 每 tick 查一次"主人还在不在、还在不在蓄力"，主人被卸载即自我消散；
     * 即便看门狗也随着召唤物的区块一起卸载（重载后变回原版凋零骷髅、丢掉这个 Goal），
     * BOSS 自身的蓄力状态是落盘的，重载后蓄力照常推进并在 ≤12s 内走到收尾清场，不会留下永久孤儿。
     * <p>{@code UNLOADED_WITH_PLAYER} 被排除：那一条发生在整维度回收（关服/删维度）的时刻，
     * 世界正在拆，此刻遍历并 discard 其它实体没有收益、只有风险。
     * <p><b>幂等</b>：没有存活召唤物时只是一次 64 格查询 + 一次裁判空队，可被多个出口重复调用。
     */
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // 血条已整体移交客户端 overlay：实体离开客户端世界后 overlay 自然扫不到它，
        // 这里不再需要（也没有）任何服务端血条资源要回收。
        if (!this.level().isClientSide() && reason != RemovalReason.UNLOADED_WITH_PLAYER) {
            AgaitolosMinionSkill.dismissAll(this);
            // /kill 直接走 remove(KILLED)（不经 die），换维度/普通 discard 也走这里 ⇒ 场地还原挂在此处才全覆盖。
            // 幂等：die() 已经置过还原态时这次是空操作；区块卸载（UNLOADED_TO_CHUNK）不经 remove，
            // 那一条由 NetherPrisonArena 的 ACTIVE 看门狗兜底（设计 §4.4 同样要求"区块卸载也要能还"）
            NetherPrisonArena.end(this);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        // 规格：不受负面效果影响。
        // 白名单：只放行 GLOWING。蓄力自发光（§1 第 22 条）靠"自施 GLOWING"实现，而 addEffect 的第一道闸
        // 就是本方法 —— 一律 false 会把自己的发光一并挡掉，故必须显式放行这一项。
        // 判据用 == MobEffects.GLOWING（药水是注册单例），只此一项；其余（尤其负面）仍然一律拒绝，
        // 不做"按 beneficial/harmful 分类放行"的模糊判断，将来要放行新东西必须在此显式列出。
        // 附带效果：光灵箭等外部来源的 GLOWING 也会命中，BOSS 会被标记发光（无害，且与自发光同语义）。
        return effect.getEffect() == MobEffects.GLOWING;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // 规格：免疫击退
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
        // BOSS 不自然消失（配合 removeWhenFarAway=false）
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // 架势期间不出手：一手格挡一手打人观感很怪，也会让"格挡 = 这一轮放弃进攻"的取舍失效。
        // 蓄力（恶怨倒转）同理：规格要求"手握紫色球往天上举"，这一轮整体让位给召唤与蓄力，
        // 出手既与 charge 姿势打架，也会让"蓄力是一个可被打断的窗口"这一代价语义失效。
        // 出场演出同理：这 4s 是"降临"，一律不起手（与所有技能起手闸同一口径，见各 tickXxx 的闸门）。
        // 复活阶段同理（本轮补齐）：那是"回血 + 无敌"的恢复窗口，招式的起手已被决策层整体禁掉，
        // 只剩原版 MeleeAttackGoal 会直接调本方法这条旁路——补上这一项，"复活期间不出手"才没有漏洞
        //（与 isRespawning 期间所有 tickXxxState 都会中断同一口径）。
        // 冲锋同理（本轮补）：俯冲镰扫的收尾处会结算一次横扫，冲锋途中再补一记普攻等于一招两段伤害，
        // 也与决策层"同一时刻只允许一招在演"的契约冲突。
        // 阶段三五招同理（2026-09-21 补）：它们各自是一段有节拍的持续状态（三连出手 / 投弹 / 踩住 / 施法），
        // 期间补一记普攻既与动作 clip 抢骨骼，也等于"一边施法一边打人"。决策层已不会派普攻，
        // 这里补的是原版 MeleeAttackGoal 那条旁路（它只认自己的可达判据）。
        if (this.isGuarding() || this.isCharging() || this.isIntroPlaying() || this.isRespawning() || this.isDiving()
                || this.phaseThree.isBusy()) {
            return false;
        }
        // 普攻间隔（本轮新收口在此）：原版 {@code MeleeAttackGoal} 的 20 tick 间隔只作用于它自己那条路径，
        // 而 {@link AgaitolosSkillDirector} 也会直接调本方法 ⇒ 两边必须共用<b>这一个</b>字段，
        // 否则普攻频率会被叠成两倍（决策层每拍一次 + Goal 自己每 20 tick 一次）。
        if (this.meleeCooldownTicks > 0) {
            return false;
        }
        // 普攻必须整套走 skill（物理 + 凋零 + 真实伤害），不能只留原版 Mob#doHurtTarget
        boolean hit = AgaitolosMeleeSkill.perform(this, target);
        if (hit) {
            // 纯表现：命中才播挥砍，不影响上面的结算结果
            AgaitolosAnimations.playAttack(this);
            // 纯表现：斩击弧与挥砍动作同刻（同样只在 hit 分支内 —— 空挥没有刃痕）
            AgaitolosActionFx.meleeSlash(this);
        }
        // 无论命中与否都起算间隔：挥空也是"这一刀挥出去了"（与横扫"被格挡也算挥出去"同一取舍），
        // 否则目标走位躲开会让 BOSS 每 tick 补刀。基准 20 tick（原版同值）再按阶段折算。
        this.meleeCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosMeleeSkill.MELEE_INTERVAL_TICKS);
        return hit;
    }

    /**
     * 只执行原版物理一击，供 {@link AgaitolosMeleeSkill} 拼装普攻使用。
     * <p>必须由子类暴露成方法：{@code super.doHurtTarget} 只能在子类内部调用。
     */
    public boolean doHurtTargetPhysical(Entity target) {
        return super.doHurtTarget(target);
    }

    /**
     * 远程攻击（一阶段「召唤凋零头颅」）：<b>由 {@link AgaitolosSkillDirector} 在"这一拍选中远程"时直接调用</b>。
     * <p>原先它由原版 {@code RangedAttackGoal} 按固定间隔调用，但该 Goal 与 {@code MeleeAttackGoal}
     * 抢同一组 Flag（MOVE + LOOK）、优先级又更低，实测<b>一次都放不出来</b>（详见 {@link #registerGoals()}）。
     * 现在节奏由决策层给（见 {@link #rangedCooldownTicks}），本方法只负责"把这一发打出去"。
     * <p>实际生成逻辑收在 {@link AgaitolosSkullSkill}，本类只做接口接线。
     *
     * @param velocity 原版的距离系数（0~1）；本招按固定初速发射，故未使用
     */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        // 出场演出期间一律不起手（与 doHurtTarget 同一口径）：即便被外部直接调用，也不发射弹体、不播施法动作，
        // 避免"一边降临一边吐凋零头"
        if (this.isIntroPlaying() || target == null || !target.isAlive()) {
            return;
        }
        AgaitolosSkullSkill.fire(this, target);
        // 纯表现：发射动作与弹体生成同刻触发
        AgaitolosAnimations.playCast(this);
        // 纯表现：掌心聚集 + 离手弹道（一个入口含两段，理由见 AgaitolosActionFx#skullCast）。
        // 放在 fire 之后：弹体已是既成事实，粒子只做注脚，不会出现"有特效没弹体"
        AgaitolosActionFx.skullCast(this, target);
        // 发射即起算冷却：基准 60 tick（原 RangedAttackGoal 的间隔）再按阶段折算
        this.rangedCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosSkullSkill.SKULL_COOLDOWN_TICKS);
    }

    /**
     * 判定这次受击是否来自「被玩家打回来的自家凋零头」（规格：反弹则 BOSS 承受且无视减伤/锁伤）。
     * <p>
     * 本 BOSS 射出的头其 owner 是 BOSS 自己；只有该弹体被玩家反弹后 owner 才变成玩家。
     * 因此「直接伤害实体是本凋零头 + 伤害归属实体是玩家」等价于「这发头被反弹回来」。
     * <p>只做判据、不做结算：伤害的免伤/减伤仍<b>唯一</b>走 {@link AgaitolosDamageRules#resolve}。
     */
    private static boolean isReflectedSkull(DamageSource source) {
        return source.getDirectEntity() instanceof AgaitolosWitherSkull
                && source.getEntity() instanceof Player;
    }

    // ---------------------------------------------------------------- 低空飞行（P4-c）

    /**
     * 飞行寻路：替换 {@link Monster} 默认的 {@code GroundPathNavigation}。
     * <p>配置照抄原版凋灵（同为 Monster 系飞行怪）：不开门、可浮水；不再调 {@code setCanPassDoors(true)}，
     * 因为 {@code FlyingPathNavigation#createPathFinder} 已默认开启。
     * {@code AgaitolosMeleeAttackGoal} / 决策层的脱困重寻路走的是 {@code createPath(Entity)} / {@code moveTo(Entity)}，
     * 在飞行导航下照常成立（{@code canUpdatePath()} 恒真），故近战 Goal 无需改动导航。
     */
    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    /**
     * 飞行移动：整体替换 {@code LivingEntity#travel}，去掉其中的重力项，只保留水/岩浆阻力与空气摩擦。
     * <p>蓝本 = 原版 {@code FlyingMob#travel}（恶魂、幻翼靠它做到不受重力）。本类继承自 {@link Monster} 无法复用该类，故照抄其行为。
     * <p><b>被格挡后的禁飞例外</b>：{@link #isGrounded()} 时交回原版 {@code super.travel}，让重力把 BOSS 拉向地面。
     * 走哪一层已核对 1.20.1 源码：{@code Mob} / {@code PathfinderMob} / {@code Monster} <b>都没有</b>覆写 {@code travel}
     * （只有 {@code LivingEntity} 声明），故这里的 {@code super.travel} 直达 {@code LivingEntity#travel}；
     * 其"普通移动"分支（非水/岩浆/鞘翅）里重力就在这一行：
     * <pre>
     *   d2 = handleRelativeFrictionAndCalculateMovement(travelVector, f2).y;
     *   ...
     *   else if (!this.isNoGravity()) { d2 -= d0; }   // d0 = 重力属性值（forge 的 ENTITY_GRAVITY，默认 0.08）
     *   this.setDeltaMovement(vec35.x * f3, d2 * 0.98, vec35.z * f3);
     * </pre>
     * 本类未设 {@code noGravity}，故该项确实生效，实体每 tick 净增约 -0.08 的竖直速度并不断累积直到落地。
     * 水平方向照旧消费 {@code AgaitolosMoveControl} 写入的速度（{@code travelVector} 对本实体恒为 0，Mob 不产生移动输入）。
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isGrounded()) {
            super.travel(travelVector);
            return;
        }
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
            } else {
                // 空中摩擦固定 0.91（取原版 Allay#travel 的写法）：
                // FlyingMob 会再查一遍脚下方块摩擦，但那个重载 getFriction(Level,BlockPos,Entity) 只存在于 forge 侧，
                // common 编译面没有；且本 BOSS 的位移完全由 AgaitolosMoveControl 改写 deltaMovement 驱动，
                // travelVector 恒为 0，贴地/空中的输入系数差异无实际影响。
                this.moveRelative(0.02F, travelVector);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.91F));
            }
        }
        this.calculateEntityAnimation(false);
    }

    /**
     * 不吃摔落伤害：直接否决摔落结算。
     * <p>不清空 {@code checkFallDamage}（{@code FlyingMob} 的路子）：那条路径还兼管水中状态与落地粒子/音效，
     * 本类继承自 {@link Monster}，保留原版链、只否决伤害更安全。
     */
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /**
     * 近战可达距离：把常态悬停高度折算进来。
     * <p>
     * <b>⚠ 原注释里的一个错误假设（已按字节码实测修正，也是"打它不还手"的根因）</b>：
     * 这里曾写着"原版 {@code MeleeAttackGoal} 的判据是 3D 距离 ≤ {@code getMeleeAttackRangeSqr}"——
     * <b>不成立</b>。实测 Forge 1.20.1-47.3.0 的 {@code MeleeAttackGoal#getAttackReachSqr} 是：
     * <pre>
     *   return this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F + target.getBbWidth();
     * </pre>
     * 它<b>根本不读本方法</b>。于是本方法当时的"修复"完全落空：0.9 宽时原版门槛 = 1.8² + 0.6 = 3.84（≈1.96 格），
     * 而本 BOSS 悬停在玩家脚上 {@link AgaitolosMoveControl#HOVER_HEIGHT}=2.0 格，光竖直差就是 2.0² = 4.0 &gt; 3.84
     * ⇒ <b>普攻永远触发不了</b>（远程又被同 Flag 的 Goal 饿死，故表现是"完全不还手"）。
     * <p>
     * 真正的落地方式是让近战 Goal 走本方法：见 {@link AgaitolosMeleeAttackGoal}（只改那一处判据）。
     * 本方法因此有三个消费方，口径必须只有这一份：近战 Goal 的出手判定、
     * 决策层 {@link AgaitolosSkillDirector} 的普攻打分、以及 {@link AgaitolosKickSkill} 的踢击可达。
     * <p>
     * <b>折算基准取「最大空中档高度」而不是"当前阶段的高度"（2026-09-21 阶段三降高时复核）</b>：
     * 本式用的是 {@link AgaitolosMoveControl#HOVER_HEIGHT}（= 一/二阶段的常态高度 2.0），
     * 而阶段三的实际空中档是 {@link AgaitolosMoveControl#PHASE_3_HOVER_HEIGHT}（1.0）、
     * 地面档更是 0.0 ⇒ 这把尺子在任何阶段都<b>不小于</b>该阶段的实际所需，属保守侧：
     * <ul>
     *   <li>阶段三：实际竖直差只有 1.0，而折算项按 2.0 给 ⇒ 多让约 1 格水平余量 ——
     *       不会出现"降了高度反而打不着"，最多是"够得着一点点就挥刀"；</li>
     *   <li>地面档：竖直差 0 ⇒ 同理偏宽松。这正是 {@link #isPerched()} 注释里写明的既有取舍
     *       （宁可多够 2 格，也不要出现"落地后反倒打不着"）。</li>
     * </ul>
     * 若实机觉得阶段三"够得太远"，改法是把本式换成按当前档位折算（读
     * {@code AgaitolosMoveControl#airborneHeight(getPhase())} / {@link #isPerched()})，
     * <b>而不是</b>去动 {@code HOVER_HEIGHT} —— 后者会连带改掉一、二阶段的悬停手感。
     */
    @Override
    public double getMeleeAttackRangeSqr(LivingEntity target) {
        double reachAllowance = AgaitolosMoveControl.HOVER_HEIGHT + 0.5D;
        return super.getMeleeAttackRangeSqr(target) + reachAllowance * reachAllowance;
    }

    @Override
    protected void registerGoals() {
        // 近战 + 追击。必须用本项目的子类：原版 MeleeAttackGoal 按自己算的 getBbWidth()² 判可达距离，
        // 不读 getMeleeAttackRangeSqr，而本 BOSS 悬停在玩家上方 2.0 格 ⇒ 竖直差一项就顶破门槛、
        // 贴到脸上也挥不出一刀（"玩家打它却不还手"的直接原因，证据与推导见 AgaitolosMeleeAttackGoal 注释）。
        this.goalSelector.addGoal(2, new AgaitolosMeleeAttackGoal(this, 1.0D, true));
        // 远程凋零头（60 tick = 3s 一发、24 格内可放）<b>不再用原版 RangedAttackGoal</b>：
        // 它与 MeleeAttackGoal 抢同一组 Flag（实测两者都 setFlags(MOVE, LOOK)），而 MeleeAttackGoal 优先级 2 更高、
        // 且它的 canUse 只要求"有存活目标"⇒ 永远成立 ⇒ 优先级 3 的远程 Goal 被<b>永久饿死、一次都放不出来</b>
        //（原注释写的"目标脱离近战距离时本条接管"不成立：MeleeAttackGoal 不会因为距离远就停）。
        // 现在远程与普攻一起由 AgaitolosSkillDirector 决定"这一拍放哪个"，节奏常量已搬进 AgaitolosSkullSkill。
        // 观察距离 32 格；不加这条 BOSS 不会转头看人，观感很呆
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        // 受击报复：优先级 0（高于主动索敌）。未被打时它的 canUse 恒假，不影响正常索敌。
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        // 主动索敌：mustSee=true ⇒ <b>索敌</b>要求视线（逃不掉，TargetingConditions 的 checkLineOfSight 默认为真），
        // 但它同时决定"锁定后能容忍多久看不见"——TargetGoal 的 unseenTicks/unseenMemoryTicks 机制。
        // 显式调长该记忆时长，让玩家绕柱子、跳下平台时 BOSS 不会立刻放弃追击。
        NearestAttackableTargetGoal<Player> nearestPlayer = new NearestAttackableTargetGoal<>(this, Player.class, true);
        nearestPlayer.setUnseenMemoryTicks(TARGET_UNSEEN_MEMORY_TICKS);
        this.targetSelector.addGoal(1, nearestPlayer);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AgaitolosAnimations.registerControllers(this, controllers);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
