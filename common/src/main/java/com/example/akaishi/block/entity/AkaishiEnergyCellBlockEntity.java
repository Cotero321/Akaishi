package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiEnergyCellBlock;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.EnergyCellTier;
import com.example.akaishi.item.AkaishiPortableEnergyCell;
import com.example.akaishi.menu.AkaishiEnergyCellMenu;
import com.example.akaishi.util.LongDataSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 赤能源储存单元方块实体：纯能量存储，容量与传输速率由方块等级决定。
 * 额外提供 1 个便携单元充能槽：放入便捷赤能源储存单元后自动注入能量。
 * 服务端驱动 tick 同步能量数据，客户端经 Menu 展示。
 */
public class AkaishiEnergyCellBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, IDataCarrier {

    /** data 布局（64 位 long 拆 4 槽，支持超级单元 200 亿容量）：
     *  0..3=方块能量，4..7=方块容量，8..11=便携单元能量，12..15=便携单元容量 */
    public static final int DATA_SIZE = 16;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 4;
    public static final int DATA_CELL_ENERGY = 8;
    public static final int DATA_CELL_CAPACITY = 12;

    /** 数据缓存：long 能量拆 4 个 int 槽同步（0=能量低位，1=能量高位，2=容量低位，3=容量高位） */
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage energy;
    /** 本方块等级（容量/传输速率） */
    private final EnergyCellTier tier;
    /** 便携单元充能槽（1 格，只放便捷赤能源储存单元） */
    private final SimpleContainer cellSlot = new SimpleContainer(1);

    public AkaishiEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_CELL.get(), pos, state);
        this.tier = ((AkaishiEnergyCellBlock) state.getBlock()).getTier();
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, tier.capacity);
        this.data = new SimpleContainerData(DATA_SIZE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiEnergyCellBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 为便携单元充能（速率取方块与便携单元传输速率中的较小者）
        chargePortableCell();
        // 同步当前能量与容量到 GUI：long 拆 4 槽（64 位），Menu 侧重组（Menu 的 broadcastChanges 负责下发）
        long stored = energy.getEnergyStored();
        long max = energy.getMaxEnergy();
        LongDataSlots.write(data, DATA_ENERGY, DATA_ENERGY + 1, DATA_ENERGY + 2, DATA_ENERGY + 3, stored);
        LongDataSlots.write(data, DATA_CAPACITY, DATA_CAPACITY + 1, DATA_CAPACITY + 2, DATA_CAPACITY + 3, max);
        ItemStack cellStack = cellSlot.getItem(0);
        long cellStored = cellStack.getItem() instanceof AkaishiPortableEnergyCell portable
                ? portable.getEnergyStored(cellStack) : 0;
        long cellMax = cellStack.getItem() instanceof AkaishiPortableEnergyCell portable2
                ? portable2.getMaxEnergy() : 0;
        LongDataSlots.write(data, DATA_CELL_ENERGY, DATA_CELL_ENERGY + 1, DATA_CELL_ENERGY + 2, DATA_CELL_ENERGY + 3, cellStored);
        LongDataSlots.write(data, DATA_CELL_CAPACITY, DATA_CELL_CAPACITY + 1, DATA_CELL_CAPACITY + 2, DATA_CELL_CAPACITY + 3, cellMax);
    }

    /** 方块能量 → 便携单元注入（作为串联器外壳时经代理读取中心存储） */
    private void chargePortableCell() {
        ItemStack cellStack = cellSlot.getItem(0);
        if (!(cellStack.getItem() instanceof AkaishiPortableEnergyCell portable)) {
            return;
        }
        IEnergyStorage source = getEnergyStorage();
        if (source == null) {
            return;
        }
        long stored = source.getEnergyStored();
        if (stored <= 0) {
            return;
        }
        long need = portable.getMaxEnergy() - portable.getEnergyStored(cellStack);
        if (need <= 0) {
            return;
        }
        long rate = Math.min(tier.transferRate, portable.tier.transferRate);
        long extracted = source.extractEnergy(Math.min(need, Math.min(stored, rate)), false);
        if (extracted > 0) {
            portable.addEnergy(cellStack, extracted, false);
            setChanged();
        }
    }

    public ContainerData data() {
        return data;
    }

    /** 便携单元充能槽容器（供 Menu 添加槽位） */
    public SimpleContainer cellSlot() {
        return cellSlot;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        // 作为串联器外壳时：代理中心串联器的聚合存储（中心被 26 个单元包围，管道只能经外壳接入整体结构）
        AkaishiEnergyCellSerializerBlockEntity center = findSerializerCenter();
        if (center != null) {
            return center.getEnergyStorage();
        }
        return energy;
    }

    /** 在自身为中心的 3×3×3 范围内查找成型中的储存串联器主方块 */
    public AkaishiEnergyCellSerializerBlockEntity findSerializerCenter() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() instanceof com.example.akaishi.block.AkaishiEnergyCellSerializerBlock
                            && level.getBlockState(p).getValue(com.example.akaishi.block.AkaishiEnergyCellSerializerBlock.FORMED)) {
                        if (level.getBlockEntity(p) instanceof AkaishiEnergyCellSerializerBlockEntity be) {
                            return be;
                        }
                    }
                }
            }
        }
        return null;
    }

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    @Override
    public Component getDisplayName() {
        EnergyCellTier tier = ((AkaishiEnergyCellBlock) getBlockState().getBlock()).getTier();
        return Component.translatable(switch (tier) {
            case BASIC -> "block.akaishi.akaishi_energy_cell_basic";
            case ADVANCED -> "block.akaishi.akaishi_energy_cell_advanced";
            case SUPER -> "block.akaishi.akaishi_energy_cell_super";
            default -> "block.akaishi.akaishi_energy_cell_basic";
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiEnergyCellMenu(id, inv, cellSlot, data);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.put("CellSlot", cellSlot.createTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        cellSlot.fromTag(tag.getList("CellSlot", net.minecraft.nbt.Tag.TAG_COMPOUND));
    }
}
