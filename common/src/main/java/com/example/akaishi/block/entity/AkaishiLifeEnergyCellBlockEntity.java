package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.block.AkaishiLifeEnergyCellBlock;
import com.example.akaishi.block.AkaishiLifeEnergyCellSerializerBlock;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyCellTier;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.menu.AkaishiLifeEnergyCellMenu;
import com.example.akaishi.util.LongDataSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 生命能量储存器方块实体：纯生命能量存储（双向缓冲，可充可放）。
 * 容量由方块等级 {@link LifeEnergyCellTier} 决定；三档共用同一方块实体类型。
 * 作为生命储存串联器外壳时，能量访问自动代理中心串联器的聚合存储。
 * 独立简洁界面：数据槽 0/1=生命能量低/高位，2/3=容量低/高位（long 拆分，防超 int 截断）。
 */
public class AkaishiLifeEnergyCellBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, IDataCarrier {

    /** 本方块等级（决定容量） */
    private final LifeEnergyCellTier tier;
    private final AkaishiEnergyStorage energy;
    /** 数据缓存：生命能量/容量各拆低/高位两个 int 槽（共 4 槽） */
    private final SimpleContainerData data;

    public AkaishiLifeEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_ENERGY_CELL.get(), pos, state);
        this.tier = state.getBlock() instanceof AkaishiLifeEnergyCellBlock cellBlock
                ? cellBlock.getTier()
                : LifeEnergyCellTier.BASIC;
        this.energy = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, tier.capacity);
        this.data = new SimpleContainerData(4);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeEnergyCellBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        LongDataSlots.write(data, 0, 1, energy.getEnergyStored());
        LongDataSlots.write(data, 2, 3, energy.getMaxEnergy());
    }

    public ContainerData data() {
        return data;
    }

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        // 作为串联器外壳时：代理中心串联器的聚合存储（中心被 26 台储存器包围，管道只能经外壳接入整体结构）
        AkaishiLifeEnergyCellSerializerBlockEntity center = findSerializerCenter();
        if (center != null) {
            return center.getEnergyStorage();
        }
        return energy;
    }

    /** 在自身为中心的 3×3×3 范围内查找成型中的生命储存串联器主方块 */
    public AkaishiLifeEnergyCellSerializerBlockEntity findSerializerCenter() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof AkaishiLifeEnergyCellSerializerBlock && s.getValue(AkaishiLifeEnergyCellSerializerBlock.FORMED)) {
                        if (level.getBlockEntity(p) instanceof AkaishiLifeEnergyCellSerializerBlockEntity be) {
                            return be;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        // 只暴露生命能量存储（赤管道通过类型匹配自动跳过本方块）
        IEnergyStorage storage = getEnergyStorage();
        return type == LifeEnergyType.INSTANCE && storage != null && storage.getType() == type ? storage : null;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(switch (tier) {
            case BASIC -> "block.akaishi.akaishi_life_energy_cell";
            case ADVANCED -> "block.akaishi.akaishi_life_energy_cell_advanced";
            case SUPER -> "block.akaishi.akaishi_life_energy_cell_super";
            default -> "block.akaishi.akaishi_life_energy_cell";
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifeEnergyCellMenu(id, inv, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("LifeEnergy", energy.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("LifeEnergy"));
    }
}
