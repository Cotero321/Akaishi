package com.example.akaishi.boss.agaitolos;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 阿盖托洛丝的表现层：出场爆发、死亡爆发、蓄力光球三处纯粒子演出。
 * <p>
 * <b>为什么单独成类</b>：粒子只跟"什么时候放、放成什么样"有关，与状态机、伤害口径、
 * 阶段推进毫无耦合。塞进 {@code AgaitolosEntity} 会让本就承担编排的实体继续变胖，
 * 也让以后调手感（换个粒子/改个数量）必须去改实体。这里只暴露三个语义入口，
 * 实体侧各调一行，两侧互不牵制。
 * <p>
 * <b>为什么全部只在服务端做</b>：
 * <ol>
 *   <li>触发时机来自服务端状态机（{@code aiStep} 的蓄力分支 / {@code die()}），客户端没有这些时刻；</li>
 *   <li>服务端 {@code sendParticles} 会自动逐玩家下发 {@code ClientboundLevelParticlesPacket}，
 *       不用自己写同步；客户端 {@code addParticle} 则只有本机看得见，多人局里是"主机独享特效"。</li>
 * </ol>
 * 因此每个入口都用 {@code boss.level() instanceof ServerLevel level} 兜底，非服务端直接空转 ——
 * 这层兜底不是防御性冗余：少了它，客户端调用会在强转处抛 {@code ClassCastException}。
 * <p>
 * 所有常量均为<b>待调手感值</b>（P8 转配置项）。位置的绝对精度受限于服务端拿不到骨骼坐标，
 * 一律按碰撞箱 + 朝向做近似（详见 {@link #orbPosition}）。
 */
public final class AgaitolosFx {

    /** 蓄力光球的发射节流间隔（tick）：2 —— 每 2 tick 一簇，约 10 簇/s。待调手感值 / P8 转配置项 */
    public static final int CHARGE_ORB_INTERVAL_TICKS = 2;

    /** 光球每簇粒子数：3。待调手感值 / P8 转配置项 */
    public static final int CHARGE_ORB_COUNT = 3;

    /** 光球粒子的扩散速度（格）：0.16。待调手感值 / P8 转配置项 */
    public static final double CHARGE_ORB_SPREAD = 0.16D;

    /** 光球粒子的初速度：0.01（几乎原地，靠 PORTAL 自身的上飘体现"能量在聚集"）。待调手感值 / P8 转配置项 */
    public static final double CHARGE_ORB_SPEED = 0.01D;

    /** 光球高度 = 身高 × 该比例：0.95（身高 2.2 ⇒ 约 2.09 格，头顶略上，像举在头前的手上）。待调手感值 / P8 转配置项 */
    public static final double CHARGE_ORB_HEIGHT_RATIO = 0.95D;

    /** 光球沿朝向的前移距离（格）：0.45（贴着头前，不至于飘进胸口里）。待调手感值 / P8 转配置项 */
    public static final double CHARGE_ORB_FORWARD = 0.45D;

    /** 汇聚粒子相对光球中心的下沉量（格）：1.2（让球心略低于脚，视觉上像从地面被吸上来）。待调手感值 / P8 转配置项 */
    public static final double CHARGE_ORB_CONVERGE_DROP = 1.2D;

    /** 汇聚粒子的扩散速度（格）：0.35。待调手感值 / P8 转配置项 */
    public static final double CHARGE_ORB_CONVERGE_SPREAD = 0.35D;

    /** 汇聚粒子每簇数量：2。待调手感值 / P8 转配置项 */
    public static final int CHARGE_ORB_CONVERGE_COUNT = 2;

    /** 出场爆发的粒子数：60。待调手感值 / P8 转配置项 */
    public static final int INTRO_BURST_COUNT = 60;

    /** 出场爆发的扩散速度（格）：1.4。待调手感值 / P8 转配置项 */
    public static final double INTRO_BURST_SPREAD = 1.4D;

    /** 出场爆发的初速度：0.06。待调手感值 / P8 转配置项 */
    public static final double INTRO_BURST_SPEED = 0.06D;

    /** 死亡爆发的粒子数：160。待调手感值 / P8 转配置项 */
    public static final int DEATH_BURST_COUNT = 160;

    /** 死亡爆发的扩散速度（格）：3.0。待调手感值 / P8 转配置项 */
    public static final double DEATH_BURST_SPREAD = 3.0D;

    /** 死亡爆发的初速度：0.09。待调手感值 / P8 转配置项 */
    public static final double DEATH_BURST_SPEED = 0.09D;

    /** 爆发粒子的高度 = 身高 × 该比例：0.6（躯干中部，而不是脚底或头顶）。待调手感值 / P8 转配置项 */
    public static final double BURST_HEIGHT_RATIO = 0.6D;

    private AgaitolosFx() {
    }

    /**
     * 「恶怨倒转」蓄力期间的紫光球（由 {@code AgaitolosEntity#tickCharge} 每 tick 调用）。
     * <p>
     * <b>节流放在这里而不是实体里</b>：{@code boss.tickCount % CHARGE_ORB_INTERVAL_TICKS} 是无状态判据，
     * 本类因此不需要自持任何计时字段/实例 —— 工具类保持无状态，重进存档也不会出现"计时器没复位"的错位。
     * <p>
     * <b>为什么选 PORTAL</b>：它是终界/末影人那类紫罗兰旋涡点，慢速外扩 + 缓慢上飘、不瞬间消失，
     * 是原版粒子里最像"悬停的紫色能量球"的；{@code DRAGON_BREATH} 虽是淡紫软雾但寿命短、会淡出，
     * 单独用来做球心会显得虚，所以留给下方那一簇"能量汇聚"（下沉 {@link #CHARGE_ORB_CONVERGE_DROP} 格），
     * 两簇叠加才有"球心实、外围在往里收"的层次。
     */
    public static void chargedOrb(AgaitolosEntity boss) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        if (boss.tickCount % CHARGE_ORB_INTERVAL_TICKS != 0) {
            return;
        }
        Vec3 orb = orbPosition(boss);
        level.sendParticles(ParticleTypes.PORTAL, orb.x, orb.y, orb.z, CHARGE_ORB_COUNT,
                CHARGE_ORB_SPREAD, CHARGE_ORB_SPREAD, CHARGE_ORB_SPREAD, CHARGE_ORB_SPEED);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, orb.x, orb.y - CHARGE_ORB_CONVERGE_DROP, orb.z,
                CHARGE_ORB_CONVERGE_COUNT, CHARGE_ORB_CONVERGE_SPREAD, CHARGE_ORB_CONVERGE_SPREAD,
                CHARGE_ORB_CONVERGE_SPREAD, CHARGE_ORB_SPEED);
    }

    /**
     * 出场爆发（首次进入复活阶段那一瞬间调一次）。
     * <p>
     * 存在的意义是让"快速回复血量"这件事<b>被看见</b>：首次进复活前血量被压到 25%
     * （{@code AgaitolosEntity#INTRO_HEALTH_RATIO}），随后 80 tick 回满 —— 这段过程若没有任何表现，
     * 玩家只会觉得"血怎么自己涨了"。爆发粒子充当那次压血/回血的视觉锚点。
     */
    public static void introBurst(AgaitolosEntity boss) {
        burst(boss, INTRO_BURST_COUNT, INTRO_BURST_SPREAD, INTRO_BURST_SPEED);
    }

    /**
     * 死亡爆发（{@code die()} 里调一次）。
     * <p>数量与扩散刻意远大于出场（160 / 3.0 对 60 / 1.4），让"这一战结束了"有明确的量级差。
     */
    public static void deathBurst(AgaitolosEntity boss) {
        burst(boss, DEATH_BURST_COUNT, DEATH_BURST_SPREAD, DEATH_BURST_SPEED);
    }

    /**
     * 爆发粒子的共用实现。
     * <p>
     * <b>为什么选 SOUL_FIRE_FLAME</b>：青蓝焰点，与 BOSS 的识别色 #4FE3D0 同族，
     * 死亡/出场的"灵魂外泄"观感直接成型；用普通 FLAME 会变成橙红，与识别色撞车。
     * <p>
     * 位置取碰撞箱躯干中部（身高 × {@link #BURST_HEIGHT_RATIO}）而不是脚底：
     * 粒子若从脚底炸开会像"脚下冒泡"，从躯干炸开才像"体内迸出来"。
     */
    private static void burst(AgaitolosEntity boss, int count, double spread, double speed) {
        if (!(boss.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(),
                boss.getY() + boss.getBbHeight() * BURST_HEIGHT_RATIO, boss.getZ(),
                count, spread, spread, spread, speed);
    }

    /**
     * 蓄力光球的世界坐标（近似）。
     * <p>
     * <b>为什么是近似</b>：粒子由服务端下发，而服务端<b>拿不到 GeckoLib 的骨骼坐标</b>
     * （骨骼只在客户端渲染阶段存在）。故只能按"碰撞箱 + 视线朝向"推算：
     * 高度取身高比例 0.95（略高于头顶，像举在头前的手上），再沿水平朝向前移 0.45 格，
     * <b>竖直方向不随俯仰移动</b> —— 若跟着视线上下跑，俯冲时球会甩到脚下或头顶，反而更假。
     * <p>
     * 因此模型比例若与"身高 2.2 格"不符，光球会偏进头里或偏到空处，
     * 需要实机微调 {@link #CHARGE_ORB_HEIGHT_RATIO} 与 {@link #CHARGE_ORB_FORWARD}。
     */
    private static Vec3 orbPosition(AgaitolosEntity boss) {
        Vec3 look = boss.getLookAngle();
        return new Vec3(
                boss.getX() + look.x * CHARGE_ORB_FORWARD,
                boss.getY() + boss.getBbHeight() * CHARGE_ORB_HEIGHT_RATIO,
                boss.getZ() + look.z * CHARGE_ORB_FORWARD);
    }
}
