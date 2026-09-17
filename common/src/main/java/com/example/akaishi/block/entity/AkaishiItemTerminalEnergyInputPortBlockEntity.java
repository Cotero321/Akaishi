package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.storage.IItemTerminalEnergyPort;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 物品终端赤能源接入口方块实体：纯汇口（仅管道注入，无手动界面）。
 * <p>
 * <b>不推送、只被抽</b>（D13 单一能量入口路径）：口只负责把管道供入的赤能源暂存在自身缓冲，
 * 终端在结算时经 {@link IItemTerminalEnergyPort} 主动抽走，故无需 ticker 与控制器坐标关联。
 * <p>
 * 破坏时缓冲随挖掘数据保留（{@link IDataCarrier}）。
 */
public class AkaishiItemTerminalEnergyInputPortBlockEntity extends BlockEntity
        implements IEnergyProvider, IDataCarrier, IItemTerminalEnergyPort {

    private final AkaishiEnergyStorage energy;

    public AkaishiItemTerminalEnergyInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ITEM_TERMINAL_ENERGY_INPUT.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE,
                ModConfig.itemTerminalEnergyPortBufferCapacity);
    }

    // ===== IEnergyProvider：纯汇（只允许管道注入，不向任何目标输出） =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        refreshCapacity();
        return energy;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    // ===== IItemTerminalEnergyPort：供终端在多口并联结算时抽取 =====

    @Override
    public long availableEnergy() {
        refreshCapacity();
        return energy.getEnergyStored();
    }

    @Override
    public long drainEnergy(long amount) {
        refreshCapacity();
        long drained = energy.extractEnergy(Math.max(0L, amount), false);
        if (drained > 0L) {
            setChanged();
        }
        return drained;
    }

    /**
     * 无 ticker（D13 单一入口路径）⇒ 改为访问时惰性同步配置容量，等价实现"配置热重载即时生效"，
     * 且不引入每 tick 开销。
     */
    private void refreshCapacity() {
        energy.setMaxEnergy(ModConfig.itemTerminalEnergyPortBufferCapacity);
    }

    // ===== NBT 持久化 =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
    }
}
