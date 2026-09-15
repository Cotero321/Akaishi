package com.example.akaishi.entity;

import com.example.akaishi.api.energy.ILifeEnergyReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 生命能量弹：由能量发射器射出，沿直线飞向绑定坐标，命中需能方块时注入能量后闪光消散。
 * <p>弹体携带发射器出膛时写入的能量（{@link #setPayload}），命中实现 {@link ILifeEnergyReceiver}
 * 的目标方块时调用其 {@code receiveLifeEnergy} 注入进度；若中途被其他方块挡住则仅消散、不注入。
 */
public class AkaishiLifeEnergyProjectileEntity extends Entity {

    private static final EntityDataAccessor<BlockPos> DATA_TARGET =
            SynchedEntityData.defineId(AkaishiLifeEnergyProjectileEntity.class, EntityDataSerializers.BLOCK_POS);
    /** 每 tick 飞行格数 */
    private static final double SPEED = 0.5D;
    /** 超时自毁（兜底，防止目标坐标异常时永久存活） */
    private static final int MAX_LIFETIME = 200;
    /** 判定"已抵达目标"的半径（格）：目标方块是实体方块，先撞面后到心，需留余量 */
    private static final double DELIVER_RADIUS = 1.0D;

    private int life;
    /** 携带的生命能量（由发射器写入，命中需能方块时注入并清零） */
    private long payload;

    public AkaishiLifeEnergyProjectileEntity(EntityType<? extends AkaishiLifeEnergyProjectileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    /** 绑定飞行目标（坐标不可变，跨端同步） */
    public void setTarget(BlockPos pos) {
        this.entityData.set(DATA_TARGET, pos.immutable());
    }

    public BlockPos getTarget() {
        return this.entityData.get(DATA_TARGET);
    }

    /** 设置本弹携带的能量（仅服务端有意义） */
    public void setPayload(long payload) {
        this.payload = Math.max(0L, payload);
    }

    public long getPayload() {
        return payload;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TARGET, BlockPos.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        // 客户端只负责展示（位置由服务端 tracker 插值同步），服务端负责推进
        // 拖尾一律交由渲染器的绿色彗尾承担，不再喷白色 END_ROD 粒子
        if (this.level().isClientSide) {
            return;
        }
        BlockPos target = getTarget();
        if (BlockPos.ZERO.equals(target) || ++this.life > MAX_LIFETIME) {
            this.burst();
            this.discard();
            return;
        }
        Vec3 delta = Vec3.atCenterOf(target).subtract(this.position());
        double distance = delta.length();
        if (distance <= SPEED) {
            this.deliver();
            this.burst();
            this.discard();
            return;
        }
        Vec3 step = delta.scale(SPEED / distance);
        this.setDeltaMovement(step);
        this.move(MoverType.SELF, step);
        // 被方块挡住即消散（目标坐标未必要暴露为可燃方块）
        if (this.horizontalCollision || this.verticalCollision) {
            // 撞上的若是目标方块本体（贴面距离中心 < 判定半径）才注入，被中途障碍物挡住则不注入
            if (Vec3.atCenterOf(target).distanceToSqr(this.position()) <= DELIVER_RADIUS * DELIVER_RADIUS) {
                this.deliver();
            }
            this.burst();
            this.discard();
        }
    }

    /** 向目标方块注入携带的能量（目标须实现 {@link ILifeEnergyReceiver}） */
    private void deliver() {
        if (payload <= 0 || level() == null) {
            return;
        }
        if (level().getBlockEntity(getTarget()) instanceof ILifeEnergyReceiver receiver) {
            receiver.receiveLifeEnergy(payload);
        }
        payload = 0;
    }

    /** 命中扩散（服务端广播粒子）：仅青绿能量点四散，不使用白色 END_ROD、也不整屏白闪 */
    private void burst() {
        if (this.level() instanceof ServerLevel server) {
            Vec3 p = this.position();
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 24, 0.35D, 0.35D, 0.35D, 0.02D);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
        this.payload = tag.getLong("Payload");
        if (tag.contains("Target")) {
            setTarget(BlockPos.of(tag.getLong("Target")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putLong("Payload", this.payload);
        tag.putLong("Target", getTarget().asLong());
    }
}
