package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiCatalystBlock;
import com.example.akaishi.block.AkaishiGeodeBlock;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiCatalystMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 赤石催化器方块实体：每 tick 消耗赤能源，对范围内每个母岩执行一次"催化生长尝试"，
 * 成功率 = 等级效率（20%-50%），与母岩自身随机 tick 叠加，大幅提升水晶簇产出。
 * GUI 数据槽：0=能量，1=容量，2=工作标志（1=能量充足正在催化）。
 */
public class AkaishiCatalystBlockEntity extends BlockEntity implements IEnergyProvider, ExtendedMenuProvider, IDataCarrier {

    /** 能量缓冲容量（终极 625/tick，可缓冲 80 tick，需管道持续供能） */
    public static final int MAX_ENERGY = 50000;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_WORKING = 2;
    public static final int DATA_SLOTS = 3;

    private final AkaishiCatalystBlock.CatalystTier tier;
    private final AkaishiEnergyStorage energy;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    /** 当前是否在催化（能量充足） */
    private boolean working;

    public AkaishiCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_CATALYST.get(), pos, state);
        this.tier = state.getBlock() instanceof AkaishiCatalystBlock block
                ? block.tier() : AkaishiCatalystBlock.CatalystTier.BASIC;
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, MAX_ENERGY);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiCatalystBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // 同步 GUI：能量/容量/工作标志
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_CAPACITY, MAX_ENERGY);
        data.set(DATA_WORKING, working ? 1 : 0);

        if (energy.getEnergyStored() < tier.energyCost) {
            working = false; // 能量不足，催化暂停
            return;
        }
        working = true;
        energy.extractEnergy(tier.energyCost, false);
        int half = tier.range / 2;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                for (int dz = -half; dz <= half; dz++) {
                    m.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    if (level.getBlockState(m).getBlock() instanceof AkaishiGeodeBlock) {
                        AkaishiGeodeBlock.tryGrow(serverLevel, m.immutable(), level.random, tier.efficiency / 100.0F);
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

    public ContainerData data() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return switch (tier) {
            case BASIC -> Component.translatable("block.akaishi.akaishi_catalyst_basic");
            case MEDIUM -> Component.translatable("block.akaishi.akaishi_catalyst_medium");
            case ADVANCED -> Component.translatable("block.akaishi.akaishi_catalyst_advanced");
            case ULTIMATE -> Component.translatable("block.akaishi.akaishi_catalyst_ultimate");
        };
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiCatalystMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
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
