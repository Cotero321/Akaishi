package com.example.akaishi.boss.agaitolos;

import com.example.akaishi.boss.agaitolos.entity.AgaitolosWitherSkull;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosMeleeSkill;
import com.example.akaishi.boss.agaitolos.skill.AgaitolosSkullSkill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
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
 * 阶段机（阈值判定 → 推进）、复活阶段（无敌 + 回血 + 结束击飞）、BossBar 生命周期。
 * 血条与阶段各自独立成类（{@link AgaitolosBossBar} / {@link AgaitolosPhase}），本类只做编排，
 * 不把逻辑堆成巨型类。
 */
public class AgaitolosEntity extends Monster implements GeoEntity, RangedAttackMob {

    /** 同步数据：当前阶段序号（0/1/2 = PHASE_1/2/3）；客户端 P7 起据此选模型与动画 */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.INT);

    /** 同步数据：是否处于复活阶段；客户端据此切演出（P7）与模型复位 */
    private static final EntityDataAccessor<Boolean> DATA_RESPAWNING =
            SynchedEntityData.defineId(AgaitolosEntity.class, EntityDataSerializers.BOOLEAN);

    /** 复活阶段时长：4s = 80 tick（用户拍板；期间回满生命，结束时击飞周围玩家） */
    public static final int RESPAWN_DURATION_TICKS = 80;

    /** 复活结束的击飞半径（格）——待调手感值 */
    public static final double RESPAWN_KNOCKBACK_RADIUS = 12.0D;

    /** 复活结束的水平击飞力度——待调手感值 */
    public static final double RESPAWN_KNOCKBACK_HORIZONTAL = 1.2D;

    /** 复活结束的垂直上抛力度——待调手感值 */
    public static final double RESPAWN_KNOCKBACK_VERTICAL = 0.9D;

    private static final String NBT_PHASE = "AgaitolosPhase";
    private static final String NBT_RESPAWN_TICKS = "AgaitolosRespawnTicks";
    private static final String NBT_INITIAL_RESPAWN_DONE = "AgaitolosInitialRespawnDone";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final AgaitolosBossBar bossBar = new AgaitolosBossBar();

    /** 上次成功受击的游戏刻。初值取远小于当前刻的负数，保证首次受击不被 0.2s 冷却拦下 */
    private long lastHurtGameTime = -100L;

    /** 复活阶段剩余 tick（服务端权威；不参与同步，客户端只需要"是否在复活"） */
    private int respawnTicks;

    /**
     * 「召唤即进入复活阶段」这一演出是否已发生（落盘）。
     * 必须持久化：否则读档后 {@code tickCount} 归零会重演一次满血复活 + 全场击飞。
     */
    private boolean initialRespawnDone;

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

    // ---------------------------------------------------------------- 存档

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(NBT_PHASE, getPhase().combatOrdinal());
        tag.putInt(NBT_RESPAWN_TICKS, this.respawnTicks);
        tag.putBoolean(NBT_INITIAL_RESPAWN_DONE, this.initialRespawnDone);
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
    }

    // ---------------------------------------------------------------- 阶段机 / 复活阶段

    @Override
    public void aiStep() {
        super.aiStep();
        // 阶段机与血条都是服务端权威，客户端只消费同步数据
        if (this.level().isClientSide()) {
            return;
        }
        // 规格（设计文档 §1 第 14 条）：召唤之后先进复活阶段快速回复血量。
        // 只在实例的最初 tick 触发一次；initialRespawnDone 落盘，避免读档重演。
        if (this.tickCount <= 1 && !this.initialRespawnDone) {
            // P2 接入点：此处之后还要铺下界牢狱场地，并在复活结束后对牢狱内生物施放凋零 III
            this.enterRespawn();
        }
        // 复活阶段内不再判阶段阈值，否则刚进 PHASE_2 就会被残留的低血量直接推到 PHASE_3
        if (this.isRespawning()) {
            this.tickRespawn();
        } else {
            this.checkPhaseAdvance();
        }
        this.bossBar.tick(this);
    }

    /** 阶段阈值判定：生命比例跌破本阶段的下一阶段门槛即推进 */
    private void checkPhaseAdvance() {
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

    // ---------------------------------------------------------------- 受击 / 生命周期

    @Override
    public boolean hurt(DamageSource source, float amount) {
        float resolved = AgaitolosDamageRules.resolve(this.level().getGameTime(), this.lastHurtGameTime,
                this.getMaxHealth(), this.isRespawning(), isReflectedSkull(source), source, amount);
        if (resolved <= 0.0F) {
            return false;
        }
        this.lastHurtGameTime = this.level().getGameTime();
        // 原版 LivingEntity#hurt 在 invulnerableTime > 10 时走"只结算增量伤害"分支，会吞掉 10 tick 内的后续伤害；
        // 这与本 BOSS「每 0.2s 只吃一次」的规格冲突，故清零，让自定义 4 tick 冷却成为唯一闸门。
        this.invulnerableTime = 0;
        boolean applied = super.hurt(source, resolved);
        if (applied) {
            // 纯表现：伤害真正落地才播受击动作；不参与结算，也不改返回值
            AgaitolosAnimations.playHurt(this);
        }
        return applied;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.bossBar.remove();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // 实体被直接移除（区块卸载 / 清场）时不会再 tick，必须在此回收血条，否则血条永久留在玩家屏幕上
        this.bossBar.remove();
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        // 规格：不受负面效果影响。后续若需要自施效果（阶段增益/发光标记），在此加白名单后再放行。
        return false;
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
     */
    @Override
    public void travel(Vec3 travelVector) {
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
