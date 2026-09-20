package com.example.akaishi.boss.agaitolos;

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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
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
 * {@code AgaitolosBossBarOverlay}）。原版 {@code ServerBossEvent}（旧 {@code AgaitolosBossBar}）已删除 ——
 * 它既画不出需求里的铭牌贴图与阶段换皮，留着还会与自定义血条<b>同时显示两条</b>，
 * 属于典型「看得到用不到」的残留。本类只负责把阶段/复活状态同步出去供客户端读取。
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
     * 恶怨倒转的冷却（tick）：300 = 15s，**在蓄力结束后**才开始计时。待调手感值 / P8 转配置项
     * <p>取值依据：规格未给冷却。召唤物会留在场上继续作战，冷却太短会让场上堆成一片凋零骷髅海；
     * 取 15s 后，"上一轮分队被清光 → 下一次起手"的最短间隔 = 15s（若被提前打断）到 12+15=27s（自然结束）。
     */
    public static final int MINION_COOLDOWN_TICKS = 300;

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

    /** 出场演出剩余 tick（服务端权威；客户端只需要"是否在出场"，同 respawning 的分工）。<b>不落盘</b>，理由见 {@link #readAdditionalSaveData} */
    private int introTicks;

    /**
     * 出场降临的目标 Y（服务端权威；开演时按 {@link AgaitolosMoveControl#hoverY} 定一次，之后不再重算）。
     * <p>整个过程不落盘、也不参与同步：它只是本段插值的临时基准，重进存档时演出已按"不重放"处理。
     */
    private double introDescentTargetY;

    public AgaitolosEntity(EntityType<? extends AgaitolosEntity> type, Level level) {
        super(type, level);
        // 低空飞行移动控制器。原版 Mob 没有 createMoveControl() 钩子（凋灵/幻翼也都是构造器里直接赋值），故在此替换
        this.moveControl = new AgaitolosMoveControl(this);
    }

    /** 基础属性；多玩家加成（§1.11）在 P3 之后再接（用户本轮确认暂不做） */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1444.0D)
                .add(Attributes.ATTACK_DAMAGE, 30.0D)
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

    /** 架势只由本类的 {@link #tickGuard()} 与 {@link #endGuard()} 开合，技能不自持计时 */
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

    /** 蓄力只由本类的 {@link #tickCharge()} / {@link #finishCharge()} 开合，技能不自持计时（同架势） */
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

    // ---------------------------------------------------------------- 冲锋 / 禁飞 / 封印状态

    /** 是否正在冲锋（俯冲位移中，服务端权威）：冲锋期间位移由本类直接写，{@code AgaitolosMoveControl} 全让位 */
    public boolean isDiving() {
        return this.diveTicks > 0;
    }

    /**
     * 是否被禁飞（被玩家格挡的惩罚）：禁飞期间不再悬停，改走带重力的原版 travel 落到地面。
     * <p>禁飞结束时无需任何额外处理：{@code AgaitolosMoveControl} ③ 的垂直悬停修正会自动把它升回
     * {@code HOVER_HEIGHT} 的常态高度。
     * <p><b>已知观感缺口（本轮不解决）</b>：现有 8 条 clip 里没有"落地待机"动画，禁飞期间仍会播
     * {@code idle_flight}（看起来还在飘）。彻底解决需要新增一条地面待机 clip，属建模任务。
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
     *   <li><b>吃封印</b>：俯冲镰扫（在 {@code tickDiveSweep} 里单独判）；后续"大招"——
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

    // ---------------------------------------------------------------- 存档

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_PHASE, getPhase().combatOrdinal());
        tag.putInt(NBT_RESPAWN_TICKS, this.respawnTicks);
        tag.putBoolean(NBT_INITIAL_RESPAWN_DONE, this.initialRespawnDone);
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // 缺键时 getInt 返回 0 ⇒ 序号 0 = 复活阶段，但 respawnTicks 也为 0 ⇒ 下面的 setRespawning 不会开无敌。
        // 新召唤（空 NBT）由 aiStep 的首次 tick 正常推进复活阶段，不依赖这里。
        this.setPhase(AgaitolosPhase.byCombatOrdinal(tag.getInt(NBT_PHASE)));
        this.respawnTicks = Math.max(0, tag.getInt(NBT_RESPAWN_TICKS));
        // 存档里若还在复活阶段，恢复无敌与倒计时：BOSS 不能靠读档跳过无敌期
        this.setRespawning(this.respawnTicks > 0);
        this.initialRespawnDone = tag.getBoolean(NBT_INITIAL_RESPAWN_DONE);
        // 缺键时 getInt 返回 0 ⇒ 冷却为 0（可立刻起手），属安全默认
        this.guardTicks = Math.max(0, tag.getInt(NBT_GUARD_TICKS));
        this.guardCooldownTicks = Math.max(0, tag.getInt(NBT_GUARD_COOLDOWN));
        // 架势随存档恢复；但剩余 tick 已为 0（异常存档）时不该继续举着，交给 tickGuard 下一 tick 收势
        this.setGuarding(tag.getBoolean(NBT_GUARDING) && this.guardTicks > 0);
        // 四个计时一律做下界保护：缺键 ⇒ 0（安全默认，等同"没有在冲锋/禁飞/封印/冷却"），
        // 异常存档里的负值也只当 0，绝不把状态搞成负数（负数会让 isDiving() 等判定失真）
        this.diveTicks = Math.max(0, tag.getInt(NBT_DIVE_TICKS));
        this.diveSweepCooldownTicks = Math.max(0, tag.getInt(NBT_DIVE_SWEEP_COOLDOWN));
        this.groundedTicks = Math.max(0, tag.getInt(NBT_GROUNDED_TICKS));
        this.scytheSealTicks = Math.max(0, tag.getInt(NBT_SCYTHE_SEAL_TICKS));
        // 蓄力 / 召唤冷却同做下界保护；蓄力随存档恢复，但剩余 tick 已为 0（异常存档）时不该继续举着，
        // 交给 tickCharge 下一 tick 收势
        this.chargeTicks = Math.max(0, tag.getInt(NBT_CHARGE_TICKS));
        this.minionCooldownTicks = Math.max(0, tag.getInt(NBT_MINION_COOLDOWN));
        // 下界保护：缺键 ⇒ 0（安全默认，等同"这一轮蓄力还没被打进任何伤害"）；
        // 异常存档里的负值也只当 0，否则负累计会让阈值判定永远差一截才算数
        this.chargeDamageTaken = Math.max(0.0F, tag.getFloat(NBT_CHARGE_DAMAGE));
        // 二阶段两招的冷却同做下界保护（缺键 ⇒ 0 = 可立刻起手，安全默认；异常负值也只当 0）
        this.blinkCooldownTicks = Math.max(0, tag.getInt(NBT_BLINK_COOLDOWN));
        this.kickCooldownTicks = Math.max(0, tag.getInt(NBT_KICK_COOLDOWN));
        this.setCharging(tag.getBoolean(NBT_CHARGING) && this.chargeTicks > 0);
        // 出场演出<b>刻意不落盘</b>（没有对应的 NBT 键），读回时一律复位成"没在出场"：
        // ① 它是首次召唤的一次性演出（initialRespawnDone 已落盘保证不重演），续播没有意义；
        // ② 续播还会错位 —— 降临的基准是"开演那一 tick 的悬停高度"，服务器存盘/区块卸载后这个基准已经丢了，
        //    若接着从半空插值，BOSS 会擦着地面或悬在半空落地（比直接复位更糟）。
        // 复位后由 AgaitolosMoveControl ③ 的悬停修正把 BOSS 收回常态高度，无需额外处理。
        this.introTicks = 0;
        this.setIntroPlaying(false);
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
        this.tickPenaltyTimers();
        // 蓄力必须排在格挡/冲锋之前：它要在本 tick 内先把 isCharging() 立起来，
        // 后面两条的起手闸才会因 !isCharging() 让位，状态互斥才成立
        this.tickCharge();
        this.tickGuard();
        this.tickDiveSweep();
        // 二阶段两招排在最后：它们的起手闸含 !isDiving()/!isGuarding()/!isCharging()，
        // 必须等上面三者在本 tick 先把状态立起来，才能保证"同一 tick 不会一边冲锋一边瞬移"
        //（顺序若反过来，本 tick 起手的冲锋/架势/蓄力会被这两招绕过，状态互斥就失效了）
        this.tickBlink();
        this.tickKick();
        // 出场演出排在<b>最末</b>：它要独占位移（写竖直分量），必须等所有可能改速度的状态都跑完；
        // 放在这里也保证"起手闸拦住的动作"先被判一遍，本 tick 不会既起手又降临
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
        double hoverY = AgaitolosMoveControl.hoverY(this.level(), this.getX(), this.getY(), this.getZ());
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
        this.enterRespawn();
    }

    /** 进入复活阶段：无敌 + 4s 内回满，倒计时归零时击飞周围玩家 */
    private void enterRespawn() {
        this.initialRespawnDone = true;
        this.respawnTicks = RESPAWN_DURATION_TICKS;
        this.setRespawning(true);
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
     * 格挡架势的状态推进（服务端权威，每 tick 一次）：冷却递减 → 架势倒计时 → 满足条件则起手。
     * <p>架势只是"状态"，判定与消费在别处：伤害归零见 {@link AgaitolosDamageRules#resolve} 的 ④ 闸，
     * 朝向/近战判定见 {@link AgaitolosGuardSkill#isBlocking}，动画见 {@code AgaitolosAnimations} 的 guard 控制器。
     */
    private void tickGuard() {
        if (this.guardCooldownTicks > 0) {
            --this.guardCooldownTicks;
        }
        if (this.isGuarding()) {
            // 复活阶段/死亡/出场演出必须立刻撤架势：无敌演出期间还举着格挡会与死亡/复活/降临姿势打架，也白吃一次免伤
            if (this.isRespawning() || this.isIntroPlaying() || this.isDeadOrDying() || --this.guardTicks <= 0) {
                this.endGuard();
            }
            return;
        }
        // 冲锋期间不做其它事：正在俯冲就不起手架势（否则"一边冲一边举盾"，语义与动画都会打架）
        if (this.guardCooldownTicks <= 0 && !this.isRespawning() && !this.isIntroPlaying() && !this.isDiving()
                && !this.isCharging() && this.shouldEnterGuard()) {
            this.setGuarding(true);
            this.guardTicks = AgaitolosGuardSkill.GUARD_DURATION_TICKS;
            // 架势要站定：先停掉寻路，否则 Goal 会继续下发目标点。
            // （水平加速另在 AgaitolosMoveControl 里掐断——travel 在 super.aiStep() 内已执行，此处清速度对当帧无效）
            this.getNavigation().stop();
        }
    }

    /**
     * 是否该起手格挡：有攻击目标 + 水平距离已进近战可达范围（= 马上要挨打）。触发条件为待调手感值 / P8 转配置项。
     * <p>距离口径复用 {@link #getMeleeAttackRangeSqr}（已按悬停高度放大），让"起手格挡"与"打得着它"用同一把尺子。
     */
    private boolean shouldEnterGuard() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        return deltaX * deltaX + deltaZ * deltaZ <= this.getMeleeAttackRangeSqr(target);
    }

    // ---------------------------------------------------------------- 恶怨倒转（一阶段：召唤分队 + 蓄力）

    /**
     * 「恶怨倒转」的状态推进（服务端权威，每 tick 一次）：起手判定 → 蓄力倒计时 → 收尾。
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
     * 起手闸：冷却中 / 与架势·冲锋·复活·死亡任一冲突 / 无存活目标，都不放。
     * 与架势、冲锋互斥是刻意的：三者共用同一批骨骼动画，同时成立会互相拉扯（见 {@code AgaitolosAnimations}）。
     */
    private void tickCharge() {
        if (this.isCharging()) {
            // 纯表现：蓄力每 tick 上报一次，节流在 AgaitolosFx 内部（每 2 tick 一帧）。
            // 放在收尾判断之前：本 tick 只要 isCharging() 仍成立就发一帧，末帧多发一次无副作用。
            AgaitolosFx.chargedOrb(this);
            // ① 死亡/复活/出场演出必须立刻收势（与 tickGuard 同一口径）：打断，不结算吸血
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
            return;
        }
        if (this.minionCooldownTicks > 0 || this.isGuarding() || this.isDiving()
                || this.isRespawning() || this.isIntroPlaying() || this.isDeadOrDying()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        // 先召唤再立状态：召唤失败（异常情况下 0 只落位）时下一 tick 就会因"全灭"立刻收势并进冷却，
        // 不会每 tick 空转起手
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
    }

    /**
     * 蓄力收尾（<b>期满结算</b>）：蓄力跑满 {@link #CHARGE_DURATION_TICKS} 自然结束时走这条 ——
     * 先做公共清理，再结算「恶怨倒转」的吸血（规格："如未取消便吸取周围小怪剩余血量造成 1/5 的魔法伤害"）。
     * <p>结算收在 {@link AgaitolosReversalSkill#drainMinions}：本类只表达"这一轮蓄力没被打断"这一语义，
     * 半径、伤害折算与归属筛选归技能持有（与 {@link AgaitolosGuardSkill#counterAttack} 同一分工）。
     */
    private void endChargeCompleted() {
        this.finishCharge();
        // 先收干净状态再结算：结算会杀死召唤物，期间 BOSS 不该还处于"蓄力中"（否则动画/出手闸会打架）
        AgaitolosReversalSkill.drainMinions(this);
    }

    /**
     * 蓄力收尾（<b>被打断</b>）：死亡 / 复活 / 承伤达阈值 / 召唤物全灭走这条，<b>不结算吸血</b>
     * （规格：蓄力被打断即"此状态被取消"，吸不到任何东西 —— 这正是玩家抢输出的收益）。
     */
    private void endChargeInterrupted() {
        this.finishCharge();
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
     * 俯冲镰扫的状态推进（服务端权威，每 tick 一次），<b>两段式</b>：
     * <ol>
     *   <li><b>冲锋中</b>（{@link #isDiving()}）→ {@link #tickDiveCharge()}：每 tick 朝目标高速推进，
     *       抵达或超时后收尾并结算一次横扫；</li>
     *   <li><b>未在冲锋</b> → 起手判定：满足"冷却结束 + 不在架势 + 不在复活/死亡 + 未被封印 +
     *       水平距离落在 [{@link #DIVE_MIN_RANGE}, {@link #DIVE_TRIGGER_RANGE}]"即进入冲锋，
     *       并<b>立即</b>播 {@code dive_sweep} 动画。</li>
     * </ol>
     * 位移本体在 {@link #tickDiveCharge()}（本类直接写 {@code deltaMovement}，{@code AgaitolosMoveControl} 全让位）；
     * 伤害结算与范围筛选全部收在 {@link AgaitolosDiveSweepSkill#perform(AgaitolosEntity)}，本方法只负责"何时放"。
     */
    private void tickDiveSweep() {
        if (this.diveSweepCooldownTicks > 0) {
            --this.diveSweepCooldownTicks;
        }
        // 死亡/复活/出场演出期间中断冲锋：不结算横扫（演出期间不该出手），也不让计时残留到下一条命
        if (this.isDeadOrDying() || this.isRespawning() || this.isIntroPlaying()) {
            this.diveTicks = 0;
            return;
        }
        if (this.isDiving()) {
            this.tickDiveCharge();
            return;
        }
        // 起手闸：冷却中 / 正举着架势 / 被玩家格挡后封印期内 / 出场演出中 —— "这一轮不放"的几种情况。
        // 与格挡架势互斥：架势是"这一轮放弃进攻"的取舍，不能一边举盾一边俯冲；
        // 封印期只封这一招，普攻与凋零头照常。横扫本身是当帧一次性结算，没有持续时间状态，故不存在反向重叠。
        if (this.diveSweepCooldownTicks > 0 || this.isGuarding() || this.isCharging() || this.isScytheSealed()
                || this.isIntroPlaying()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !isWithinDiveTriggerRange(target)) {
            return;
        }
        this.diveTicks = DIVE_MAX_TICKS;
        // 纯表现：冲锋起手即播。dive_sweep clip 本身覆盖「俯冲 + 横扫」两段，
        // 故冲锋收尾结算时<b>不再</b>重复触发（否则横扫段会被打断重播）。
        AgaitolosAnimations.playDiveSweep(this);
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
        if (AgaitolosDiveSweepSkill.perform(this)) {
            // 冷却按阶段折算（二阶段起更短）：倍率表见 AgaitolosPace，阶段一恒为原值
            this.diveSweepCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosDiveSweepSkill.SWEEP_COOLDOWN_TICKS);
        }
    }

    /** 冲锋瞄准点：目标<b>身体中部</b>（脚 + 半身高）——瞄脚会让 BOSS 一路贴地，瞄眼睛又会悬太高 */
    private static Vec3 diveAimPoint(LivingEntity target) {
        return new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }

    /**
     * 是否满足冲锋起手距离：与目标的<b>水平</b>距离落在 [{@link #DIVE_MIN_RANGE}, {@link #DIVE_TRIGGER_RANGE}]。
     * <p>用水平距离而非 3D 距离：悬停高度不该影响"够不够近"（沿用被替换掉的旧起手判定的口径）。
     * 超过上界交给飞行巡航接近；小于下界交给普攻/格挡 —— 贴脸再俯冲既无位移意义，也会把贴身战搅成连招。
     */
    private boolean isWithinDiveTriggerRange(LivingEntity target) {
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
     *       与 {@code tickCharge}、{@code tickGuard}、{@code tickDiveSweep} 的起手闸同款判据，
     *       共用同一批骨骼动画，同时成立会互相拉扯；</li>
     *   <li><b>技能封印</b>（{@link #isScytheSealed()}）：封印<b>只封"大招"</b>（俯冲镰扫，
     *       以及后续接入的三重投掷 / 天魔灾 / 投技），<b>不封瞬击与高速踢击</b>，
     *       故本方法<b>不含</b>封印判定；作用范围与理由见 {@link #isScytheSealed()} 的 javadoc。</li>
     * </ul>
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
     * 「瞬击」的状态推进（服务端权威，每 tick 一次），两段式与俯冲镰扫同构：
     * <ol>
     *   <li>冷却递减；</li>
     *   <li>起手闸通过后，仅在<b>不处于飞行状态</b>（{@link #isGrounded()}）时尝试起手，
     *       再交给 {@link AgaitolosBlinkSkill} 做距离判定、落点校验与瞬移。</li>
     * </ol>
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
     */
    private void tickBlink() {
        if (this.blinkCooldownTicks > 0) {
            --this.blinkCooldownTicks;
        }
        if (!this.canStartPhaseTwoSkill()) {
            return;
        }
        // 非飞行状态（= 禁飞窗口）是这一招的启用前提，规格原文即如此
        if (!this.isGrounded()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !AgaitolosBlinkSkill.canBlink(this, target)) {
            return;
        }
        if (AgaitolosBlinkSkill.perform(this, target)) {
            this.blinkCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosBlinkSkill.BLINK_COOLDOWN_TICKS);
        } else {
            this.blinkCooldownTicks = AgaitolosBlinkSkill.BLINK_FAILED_RETRY_TICKS;
        }
    }

    /**
     * 「高速踢击」的状态推进（服务端权威，每 tick 一次）：冷却递减 → 起手闸 → 目标与距离 → 交给技能结算。
     * <p>
     * <b>与瞬击恰相反，本招不限定飞行/地面</b>（规格："任何状态下可用"）：悬停、禁飞、任何高度都能起手；
     * 但仍受 {@link #canStartPhaseTwoSkill()} 的六项互斥约束（死亡/复活/架势/蓄力/冲锋/封印）
     * —— "任何状态"指的是空间状态，不是"可以一边蓄力一边踢"。
     * <p>
     * 目标与距离一律复用既有口径：目标取 {@code getTarget()}（唯一由 {@code NearestAttackableTargetGoal<Player>}
     * 选定），距离取 {@link #getMeleeAttackRangeSqr}（含悬停高度折算，与普攻/格挡同一把尺子）。
     * 出手即进冷却（被盾牌挡下也算"这一脚踢出去了"，与俯冲镰扫同一取舍），避免格挡成功时每 tick 空踢。
     */
    private void tickKick() {
        if (this.kickCooldownTicks > 0) {
            --this.kickCooldownTicks;
        }
        if (!this.canStartPhaseTwoSkill()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !AgaitolosKickSkill.isWithinKickRange(this, target)) {
            return;
        }
        if (AgaitolosKickSkill.perform(this, target)) {
            this.kickCooldownTicks = AgaitolosPace.scaledCooldown(this, AgaitolosKickSkill.KICK_COOLDOWN_TICKS);
        }
    }

    // ---------------------------------------------------------------- 受击 / 生命周期

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 格挡预判：与 resolve 的 ④ 闸走的是**同一**个 AgaitolosGuardSkill.isBlocking（§3.3「同一判定器」）。
        // 这里只负责"格挡成功 → 发动反击 + 收势"，伤害归零的口径仍由 resolve 决定（本分支直接短路，等价于归零）。
        // 必须在 resolve 之前判：counterAttack 会顺手收势，若先 resolve，判定会因架势已撤而落空。
        if (AgaitolosGuardSkill.isBlocking(this, source)) {
            AgaitolosGuardSkill.counterAttack(this, source);
            return false;
        }
        float resolved = AgaitolosDamageRules.resolve(this.level().getGameTime(), this.lastHurtGameTime,
                this.getMaxHealth(), this.isRespawning(), isReflectedSkull(source), this.isGuarding(), this,
                source, amount);
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
        }
        return applied;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
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
        if (this.deathTime >= DEATH_TICKS && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // 血条已整体移交客户端 overlay：实体离开客户端世界后 overlay 自然扫不到它，
        // 这里不再需要（也没有）任何服务端血条资源要回收。
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
        // 出场演出同理：这 4s 是"降临"，一律不起手（与所有技能起手闸同一口径，见各 tick* 的闸门）。
        if (this.isGuarding() || this.isCharging() || this.isIntroPlaying()) {
            return false;
        }
        // 普攻必须整套走 skill（物理 + 凋零 + 真实伤害），不能只留原版 Mob#doHurtTarget
        boolean hit = AgaitolosMeleeSkill.perform(this, target);
        if (hit) {
            // 纯表现：命中才播挥砍，不影响上面的结算结果
            AgaitolosAnimations.playAttack(this);
        }
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
     * 远程攻击（一阶段「召唤凋零头颅」）：由 {@link RangedAttackGoal} 按固定间隔调用。
     * <p>实际生成逻辑收在 {@link AgaitolosSkullSkill}，本类只做接口接线。
     *
     * @param velocity 原版的距离系数（0~1）；本招按固定初速发射，故未使用
     */
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        // 出场演出期间一律不起手（与 doHurtTarget 同一口径）：RangedAttackGoal 的间隔到点也会被这里挡下，
        // 不发射弹体、不播施法动作，避免"一边降临一边吐凋零头"
        if (this.isIntroPlaying()) {
            return;
        }
        AgaitolosSkullSkill.fire(this, target);
        // 纯表现：发射动作与弹体生成同刻触发
        AgaitolosAnimations.playCast(this);
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
     * <p>{@code MeleeAttackGoal} / {@code RangedAttackGoal} 走的是 {@code createPath(Entity)} / {@code moveTo(Entity)}，
     * 在飞行导航下照常成立（{@code canUpdatePath()} 恒真），故两条 Goal 无需改动。
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
     * 原版 {@code MeleeAttackGoal} 的判据是「脚对脚 3D 距离 ≤ {@code getMeleeAttackRangeSqr}」，
     * 而本 BOSS 悬停在玩家上方 {@link AgaitolosMoveControl#HOVER_HEIGHT} 格 —— 光是这段竖直差
     * 就已经顶破门槛（0.9 宽时门槛 ≈ 1.96 格，竖直差 2.0 即超出），
     * 结果就是<b>普攻永远触发不了、只剩远程输出</b>。
     * 这里把悬停高度加进可达距离，使「悬停在低空仍能下劈命中地面玩家」成立；
     * 用 {@link AgaitolosMoveControl#HOVER_HEIGHT} 而不是写死数字，将来调悬停高度时二者不会失配。
     */
    @Override
    public double getMeleeAttackRangeSqr(LivingEntity target) {
        double reachAllowance = AgaitolosMoveControl.HOVER_HEIGHT + 0.5D;
        return super.getMeleeAttackRangeSqr(target) + reachAllowance * reachAllowance;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        // 远程凋零头：60 tick = 3s 一发、24 格内可放（待调手感值 / P8 转配置项）。
        // 与优先级 2 的近战不冲突：目标脱离近战距离时 MeleeAttackGoal 不再可用，本条接管。
        this.goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0D, 60, 24.0F));
        // 观察距离 32 格；不加这条 BOSS 不会转头看人，观感很呆
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
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
