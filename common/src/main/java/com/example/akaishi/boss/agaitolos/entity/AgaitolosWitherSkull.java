package com.example.akaishi.boss.agaitolos.entity;

import com.example.akaishi.boss.agaitolos.AgaitolosCombat;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 阿盖托洛丝的远程弹体：凋零头颅（设计文档 §0「远程攻击」/§1 第 20 条）。
 * <p>
 * 继承原版 {@link WitherSkull} 复用其飞行、渲染、与方块/实体碰撞管线，<b>只替换命中结算</b>：
 * 原版是 8 点凋零/魔法伤害 + 凋零 II 10s，与本 BOSS 规格（5 点爆炸 + 凋零 III 2s + 2 点真实伤害）不符。
 * <p>
 * 伤害数值常量放在本类（消费端）而非发射器，避免弹体与 skill 互相引用。
 * <p>
 * <b>两处对原版的刻意偏离</b>（均由 1.20.1 字节码实测支撑，见各自方法注释）：
 * <ol>
 *   <li><b>可被近战打回</b>：原版 {@code WitherSkull#isPickable()} 与 {@code WitherSkull#hurt(...)}
 *       都无条件返回 false，弹体永远不会被玩家命中；而带偏转逻辑的
 *       {@code AbstractHurtingProjectile#hurt} 被 {@code WitherSkull#hurt} 整段覆盖，
 *       子类也无法经 {@code super} 调到。若不打开这两个口子，规格的
 *       「如反弹至 BOSS 则 BOSS 承受此伤害」永远不可达，故本类覆写二者还原偏转语义。
 *       偏转时 {@code setOwner(偏转者)} 会把 owner 从 BOSS 换成玩家；此后本弹体的伤害源
 *       （{@code directEntity=本弹体 / causingEntity=owner}）恰好满足
 *       {@code AgaitolosEntity#hurt} 内的判据
 *       {@code source.getDirectEntity() instanceof AgaitolosWitherSkull && source.getEntity() instanceof Player}，
 *       于是命中 {@code AgaitolosDamageRules#resolve} 的 ⑤ 自伤通道（跳过 ⑥ 减伤与 ⑦ 锁伤）。</li>
 *   <li><b>不破坏地形</b>：原版 {@code WitherSkull#onHit} 无条件调用
 *       {@code level().explode(this, x, y, z, 1.0F, false, Level.ExplosionInteraction.MOB)}；
 *       注意原版 {@code setDangerous(false)} <b>并不能阻止爆炸破坏地形</b>
 *       （它只改 {@code getInertia()} 与火方块上的阻爆），故本类整个覆写 {@code onHit} 不生成爆炸。</li>
 * </ol>
 */
public class AgaitolosWitherSkull extends WitherSkull {

    /** 命中爆炸伤害 5 点。P8 转配置项 */
    public static final float BURST_DAMAGE = 5.0F;

    /** 命中附加的真实伤害 2 点（无视护甲/抗性/附魔）。P8 转配置项 */
    public static final float TRUE_DAMAGE = 2.0F;

    /** 凋零持续 40 tick = 2s。P8 转配置项 */
    public static final int WITHER_DURATION_TICKS = 40;

    /** 凋零等级放大器 2 = 凋零 III（amplifier 从 0 起算）。P8 转配置项 */
    public static final int WITHER_AMPLIFIER = 2;

    public AgaitolosWitherSkull(EntityType<? extends WitherSkull> type, Level level) {
        super(type, level);
    }

    /**
     * 成为射线可命中的目标。原版 {@code WitherSkull#isPickable()} 字节码是 {@code iconst_0; ireturn}（恒 false），
     * 而玩家近战的取靶（实体射线）与 {@code Player#attack} → {@code Entity#hurt} 这条链都以可命中为目标前提，
     * 不开则玩家永远打不到这一发头，规格的反弹玩法不可达。
     * <p>副作用：{@code Entity#canBeHitByProjectile()} = {@code isAlive() && isPickable()}，
     * 因此本弹体自此也会被其他弹体判定为可命中目标，这是可偏转设计的必然代价。</p>
     */
    @Override
    public boolean isPickable() {
        return true;
    }

    /**
     * 近战打回。原版 {@code WitherSkull#hurt(...)} 恒 {@code return false}，把 {@code AbstractHurtingProjectile#hurt}
     * 的偏转逻辑整段吃掉，故本方法按该父类<b>字节码语义</b>重写（实测指令顺序：
     * {@code isInvulnerableTo → markHurt → source.getEntity() → 非客户端则 getLookAngle → setDeltaMovement →
     * xPower/yPower/zPower = look * 0.1D（三个字段均为 public double，可直接写）→ setOwner(偏转者)}，
     * 有实体来源返回 true，否则 false）：
     * <p>与原版仅两点差异：① 客户端直接 {@code return false}，状态改动只在服务端做，避免两端分叉；
     * ② 偏转者必须是<b>本弹体可命中的生物</b>。1.20.1 实测不存在 {@code Entity#canAttack(Entity)}
     * （{@code Entity} 上只有 {@code isAttackable/skipAttackInteraction}），
     * {@code LivingEntity#canAttack(LivingEntity)} 只接受生物、{@code canAttackType} 恒返回 true，
     * 对非生物的弹体都不可用，故用 {@code Projectile#canHitEntity}
     * （{@code target.canBeHitByProjectile() && 不与 owner 同乘载具}）做等价筛除，挡掉旁观者/已死亡目标。
     * <p>{@code setOwner(偏转者)} 是整条反弹链的关键：owner 由 BOSS 换成玩家后，
     * {@link #onHitEntity} 构造的伤害源 {@code causingEntity} 才变成玩家，
     * {@code AgaitolosEntity} 侧才会把它识别为「被反弹」并放行进 ⑤ 自伤通道。
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) {
            return false;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (!(source.getEntity() instanceof LivingEntity deflector) || !this.canHitEntity(deflector)) {
            return false;
        }
        this.markHurt();
        Vec3 look = deflector.getLookAngle();
        this.setDeltaMovement(look);
        this.xPower = look.x * 0.1D;
        this.yPower = look.y * 0.1D;
        this.zPower = look.z * 0.1D;
        this.setOwner(deflector);
        return true;
    }

    /**
     * 命中分派（<b>刻意不调用 super</b>）。原版 {@code WitherSkull#onHit} 字节码为
     * 「{@code super.onHit(result)}（继承链上落到 {@code Projectile#onHit} 的 ENTITY/BLOCK 分派）
     * → {@code if (!level().isClientSide) { level().explode(this, getX(), getY(), getZ(), 1.0F, false,
     * Level.ExplosionInteraction.MOB); discard(); } }」——即<b>爆炸是 WitherSkull 自己在 onHit 里无条件生成的</b>，
     * {@code setDangerous(false)} 挡不住；调 super 就等于保留破坏地形的爆炸。规格要求「不可破坏地形」，
     * 故此处自建分派：实体命中交给 {@link #onHitEntity}，方块命中不做任何事（不爆炸、不做方块交互），
     * 最后服务端 discard。
     * <p>特例：加 {@code isPickable()=true} 后，刚生成的头可能与发射者（owner = BOSS 自己）重叠并被判定命中自身，
     * 此时直接返回、<b>既不结算也不 discard</b>，让弹体正常飞出，避免贴脸自毁。
     */
    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) result;
            if (entityHit.getEntity() == this.getOwner()) {
                return;
            }
            this.onHitEntity(entityHit);
        }
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        // 每一段伤害的伤害源都必须同时带 directEntity = 本弹体、causingEntity = owner，两个理由缺一不可：
        //   ① BOSS 侧靠 source.getDirectEntity() instanceof AgaitolosWitherSkull 识别「这发头是被反弹回来的」；
        //   ② 真实伤害段若不带 causingEntity，source.getEntity() 为 null，会被 BOSS 受击管线第 ① 步
        //      （非玩家来源免伤）整段挡掉，规格要求的「反弹至 BOSS 则 BOSS 承受此伤害」就落不了地。
        // 故不能走 level.damageSources().explosion(null, owner)：explosion 的 directEntity 会退化为 null，理由①失效。
        Holder<DamageType> explosionHolder = living.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.EXPLOSION);
        DamageSource explosionSource = new DamageSource(explosionHolder, this, owner);

        // ① 5 点爆炸伤害（只借爆炸伤害类型，不生成真正的爆炸 —— 不破坏地形）
        living.hurt(explosionSource, BURST_DAMAGE);
        // ② 凋零 III 2s；施加者取 owner 保证击杀归属，owner 非生物（理论上不会）时用弹体自身兜底。
        //    1.20.1 常量名是 MobEffects.WITHER。目标若是本 BOSS，canBeAffected=false 会自动挡掉，无需特判。
        living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_DURATION_TICKS, WITHER_AMPLIFIER),
                owner instanceof LivingEntity livingOwner ? livingOwner : this);
        // ③ 2 点真实伤害（同样携带 direct + causing，被反弹回 BOSS 时才会被自伤通道吃下）
        living.hurt(AgaitolosCombat.trueDamage(living.level(), this, owner), TRUE_DAMAGE);
    }
}
