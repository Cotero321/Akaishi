package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.block.ChishiCatalystBlock;
import com.example.template.block.ChishiGeodeBlock;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 赤石催化器方块实体：每 tick 消耗赤能源，对范围内每个母岩执行一次"催化生长尝试"，
 * 成功率 = 等级效率（20%-50%），与母岩自身随机 tick 叠加，大幅提升水晶簇产出。
 */
public class ChishiCatalystBlockEntity extends BlockEntity implements IEnergyProvider {

    /** 能量缓冲容量（终极 625/tick，可缓冲 80 tick，需管道持续供能） */
    public static final int MAX_ENERGY = 50000;

    private final ChishiCatalystBlock.CatalystTier tier;
    private final ChishiEnergyStorage energy;

    public ChishiCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_CATALYST.get(), pos, state);
        this.tier = state.getBlock() instanceof ChishiCatalystBlock block
                ? block.tier() : ChishiCatalystBlock.CatalystTier.BASIC;
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, MAX_ENERGY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiCatalystBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (energy.getEnergyStored() < tier.energyCost) {
            return; // 能量不足，催化暂停
        }
        energy.extractEnergy(tier.energyCost, false);
        int half = tier.range / 2;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                for (int dz = -half; dz <= half; dz++) {
                    m.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    if (level.getBlockState(m).getBlock() instanceof ChishiGeodeBlock) {
                        ChishiGeodeBlock.tryGrow(serverLevel, m.immutable(), level.random, tier.efficiency / 100.0F);
                    }
                }
            }
        }
        setChanged();
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    /** 纯消耗型机器：只接收管道输入的赤能源，不向外输出 */
    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

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
