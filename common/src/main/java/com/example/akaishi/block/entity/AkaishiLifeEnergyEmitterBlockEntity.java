package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.energy.ILifeEnergyReceiver;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.entity.AkaishiLifeEnergyProjectileEntity;
import com.example.akaishi.entity.ModEntities;
import com.example.akaishi.menu.AkaishiLifeEnergyEmitterMenu;
import com.example.akaishi.util.LongDataSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 能量发射器方块实体：主动从相邻生命能量源抽能蓄满后，朝绑定坐标射出生命能量弹。
 * - 抽能优先级：相邻存储设备（储存器/串联器等）优先，管道次选；
 * - 仅当目标方块仍需要生命能量（{@link ILifeEnergyReceiver#needsLifeEnergy()}）时才抽能蓄力与发射，
 *   目标已满时自动停手，避免能量白白积累与无效发射；
 * - 缓存蓄满（{@link #CAPACITY}）且目标在 {@link #RANGE} 内时发射，发射清空缓存；
 * - 实现 IEnergyProvider：对生命能量管道表现为 sink（可输入、不可输出），从而被管道灌能；
 * - 绑定坐标与能量经方块实体 NBT 同步客户端（BER 渲染头部朝向与蓄能光效依赖）。
 */
public class AkaishiLifeEnergyEmitterBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IEnergyProvider, IDataCarrier {

    /** 内部缓存容量（蓄满即发射） */
    public static final long CAPACITY = 8000L;
    /** 每 tick 最多抽取量 */
    private static final long PULL_PER_TICK = 200L;
    /** 发射冷却（20 tick = 1 秒）：无论蓄能多快，两次发射至少间隔 1 秒 */
    private static final int FIRE_INTERVAL_TICKS = 20;
    /** 有效射程（格） */
    public static final int RANGE = 32;

    private static final String TAG_ENERGY = "LifeEnergy";
    private static final String TAG_TARGET = "Target";
    private static final String TAG_LAST_FIRE = "LastFire";
    /** 未绑定目标坐标的哨兵值 */
    private static final long NO_TARGET = Long.MIN_VALUE;
    /** 尚无发射记录的哨兵值（配合 lastFireTick） */
    public static final long NO_FIRE = Long.MIN_VALUE;
    /** 渲染数据同步节流（每 10 tick 推送一次） */
    private static final int SYNC_INTERVAL = 10;
    /** 炮口相对方块中心的外移距离（世界格），用于弹体出膛点 */
    private static final double MUZZLE_OFFSET = 0.6D;
    /** 未绑定目标时的默认瞄准方向（模型正面 = north 面 → -Z） */
    private static final Vec3 DEFAULT_AIM = new Vec3(0.0D, 0.0D, -1.0D);

    private final AkaishiEnergyStorage energy = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, CAPACITY);
    /** 数据缓存：能量/容量各拆低/高位两个 int 槽 */
    private final SimpleContainerData data = new SimpleContainerData(4);

    private BlockPos target;
    private int syncTimer;
    /** 最近一次发射的游戏刻（{@link #NO_FIRE} 表示尚未发射），供客户端渲染瞬时能量流 */
    private long lastFireTick = NO_FIRE;

    public AkaishiLifeEnergyEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_ENERGY_EMITTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeEnergyEmitterBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        LongDataSlots.write(data, 0, 1, energy.getEnergyStored());
        LongDataSlots.write(data, 2, 3, energy.getMaxEnergy());
        // 仅当目标方块仍需要能量时才抽能蓄力并发射：目标已满/不存在时不再无谓积累
        if (targetNeedsEnergy()) {
            pullFromNeighbors();
            if (energy.getEnergyStored() >= CAPACITY && !onCooldown()) {
                fire();
            }
        }
        if (++syncTimer % SYNC_INTERVAL == 0) {
            sync();
        }
    }

    /** 目标方块是否需要生命能量：未绑定 / 目标非接收器 / 接收器已满 均为 false */
    private boolean targetNeedsEnergy() {
        if (level == null || target == null) {
            return false;
        }
        return level.getBlockEntity(target) instanceof ILifeEnergyReceiver receiver && receiver.needsLifeEnergy();
    }

    /** 是否处于发射冷却中：距上次发射不足 {@link #FIRE_INTERVAL_TICKS} 刻 */
    private boolean onCooldown() {
        // NO_FIRE 时须短路，否则 gameTime - Long.MIN_VALUE 会溢出为负数导致永远判定在冷却
        if (level == null || lastFireTick == NO_FIRE) {
            return false;
        }
        return level.getGameTime() - lastFireTick < FIRE_INTERVAL_TICKS;
    }

    /** 从相邻能源抽能：先存储设备，后管道 */
    private void pullFromNeighbors() {
        if (level == null || energy.getEnergyStored() >= CAPACITY) {
            return;
        }
        for (int pass = 0; pass < 2; pass++) {
            for (Direction dir : Direction.values()) {
                if (energy.getEnergyStored() >= CAPACITY) {
                    return;
                }
                BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
                if (!(neighbor instanceof IEnergyProvider provider)) {
                    continue;
                }
                // pass 0：非管道（存储设备）；pass 1：管道
                if ((neighbor instanceof AkaishiEnergyPipeBlockEntity) == (pass == 0)) {
                    continue;
                }
                if (!provider.canOutputEnergy(LifeEnergyType.INSTANCE)) {
                    continue;
                }
                IEnergyStorage storage = provider.getEnergyStorage(LifeEnergyType.INSTANCE);
                if (storage == null) {
                    continue;
                }
                long need = CAPACITY - energy.getEnergyStored();
                long pulled = storage.extractEnergy(Math.min(need, PULL_PER_TICK), false);
                if (pulled > 0) {
                    energy.addEnergy(pulled, false);
                }
            }
        }
    }

    /** 蓄满后朝绑定坐标射出能量弹 */
    private void fire() {
        if (level == null || level.isClientSide || target == null) {
            return;
        }
        if (worldPosition.distSqr(target) > (double) RANGE * RANGE) {
            return;
        }
        AkaishiLifeEnergyProjectileEntity projectile =
                new AkaishiLifeEnergyProjectileEntity(ModEntities.LIFE_ENERGY_PROJECTILE.get(), level);
        projectile.setTarget(target);
        projectile.setPayload(CAPACITY);
        Vec3 muzzle = muzzlePos();
        projectile.setPos(muzzle.x, muzzle.y, muzzle.z);
        level.addFreshEntity(projectile);
        energy.extractEnergy(CAPACITY, false);
        // 记录发射时刻，供 BER 判断"仅发射瞬间"渲染能量流
        lastFireTick = level.getGameTime();
        sync();
    }

    /** 炮口出膛点：方块中心沿连续瞄准方向外移 {@link #MUZZLE_OFFSET} 格 */
    private Vec3 muzzlePos() {
        return Vec3.atCenterOf(worldPosition).add(getAimDirection().scale(MUZZLE_OFFSET));
    }

    /**
     * 瞄准方向（方块中心 → 目标，单位向量）；未绑定或目标与中心重合时回退默认朝北。
     * 供弹体出膛点与客户端 BER 的整机 look-at 旋转共用，保证"模型朝向 / 出膛点 / 光束"三者一致。
     */
    public Vec3 getAimDirection() {
        if (target == null) {
            return DEFAULT_AIM;
        }
        Vec3 delta = Vec3.atCenterOf(target).subtract(Vec3.atCenterOf(worldPosition));
        double length = delta.length();
        return length < 1.0E-6D ? DEFAULT_AIM : delta.scale(1.0D / length);
    }

    public ContainerData data() {
        return data;
    }

    /** 绑定/解绑发射目标（仅服务端调用） */
    public void setTarget(BlockPos pos) {
        this.target = pos == null ? null : pos.immutable();
        sync();
    }

    public BlockPos getTarget() {
        return target;
    }

    /** 最近一次发射的游戏刻（{@link #NO_FIRE} 表示尚未发射），供客户端 BER 渲染瞬时能量流 */
    public long getLastFireTick() {
        return lastFireTick;
    }

    /** 数据变更 → 落盘 + 广播方块实体数据包（BER 渲染需客户端持有最新朝向/能量） */
    private void sync() {
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
            for (ServerPlayer player : serverLevel.players()) {
                player.connection.send(packet);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE ? energy : null;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        // 发射器只向弹体输出，不向管道回供
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_energy_emitter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifeEnergyEmitterMenu(id, inv, data, target);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        buf.writeBoolean(target != null);
        buf.writeBlockPos(target == null ? BlockPos.ZERO : target);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // 始终写入键（含未绑定哨兵），避免空 tag 被数据包转成 null 导致客户端不同步
        tag.putLong(TAG_ENERGY, energy.getEnergyStored());
        tag.putLong(TAG_TARGET, target == null ? NO_TARGET : target.asLong());
        tag.putLong(TAG_LAST_FIRE, lastFireTick);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong(TAG_ENERGY));
        long packed = tag.getLong(TAG_TARGET);
        target = packed == NO_TARGET ? null : BlockPos.of(packed);
        lastFireTick = tag.contains(TAG_LAST_FIRE) ? tag.getLong(TAG_LAST_FIRE) : NO_FIRE;
    }
}
