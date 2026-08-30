package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.block.ChishiEnergyCellBlock;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.energy.EnergyCellTier;
import com.example.template.item.ChishiPortableEnergyCell;
import com.example.template.menu.ChishiEnergyCellMenu;
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
public class ChishiEnergyCellBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, IDataCarrier {

    /** data 布局：0/1=能量低/高位，2/3=容量低/高位，4/5=便携单元能量低/高位，6/7=便携单元容量低/高位 */
    public static final int DATA_SIZE = 8;

    /** 数据缓存：long 能量拆 4 个 int 槽同步（0=能量低位，1=能量高位，2=容量低位，3=容量高位） */
    private final SimpleContainerData data;
    private final ChishiEnergyStorage energy;
    /** 本方块等级（容量/传输速率） */
    private final EnergyCellTier tier;
    /** 便携单元充能槽（1 格，只放便捷赤能源储存单元） */
    private final SimpleContainer cellSlot = new SimpleContainer(1);

    public ChishiEnergyCellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_CELL.get(), pos, state);
        this.tier = ((ChishiEnergyCellBlock) state.getBlock()).getTier();
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, tier.capacity);
        this.data = new SimpleContainerData(DATA_SIZE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiEnergyCellBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 为便携单元充能（速率取方块与便携单元传输速率中的较小者）
        chargePortableCell();
        // 同步当前能量与容量到 GUI：long 拆 4 个 int 槽，Menu 侧重组（Menu 的 broadcastChanges 负责下发）
        long stored = energy.getEnergyStored();
        long max = energy.getMaxEnergy();
        data.set(0, (int) stored);
        data.set(1, (int) (stored >>> 32));
        data.set(2, (int) max);
        data.set(3, (int) (max >>> 32));
        ItemStack cellStack = cellSlot.getItem(0);
        long cellStored = cellStack.getItem() instanceof ChishiPortableEnergyCell portable
                ? portable.getEnergyStored(cellStack) : 0;
        long cellMax = cellStack.getItem() instanceof ChishiPortableEnergyCell portable2
                ? portable2.getMaxEnergy() : 0;
        data.set(4, (int) cellStored);
        data.set(5, (int) (cellStored >>> 32));
        data.set(6, (int) cellMax);
        data.set(7, (int) (cellMax >>> 32));
    }

    /** 方块能量 → 便携单元注入（作为串联器外壳时经代理读取中心存储） */
    private void chargePortableCell() {
        ItemStack cellStack = cellSlot.getItem(0);
        if (!(cellStack.getItem() instanceof ChishiPortableEnergyCell portable)) {
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
        ChishiEnergyCellSerializerBlockEntity center = findSerializerCenter();
        if (center != null) {
            return center.getEnergyStorage();
        }
        return energy;
    }

    /** 在自身为中心的 3×3×3 范围内查找成型中的储存串联器主方块 */
    public ChishiEnergyCellSerializerBlockEntity findSerializerCenter() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() instanceof com.example.template.block.ChishiEnergyCellSerializerBlock
                            && level.getBlockState(p).getValue(com.example.template.block.ChishiEnergyCellSerializerBlock.FORMED)) {
                        if (level.getBlockEntity(p) instanceof ChishiEnergyCellSerializerBlockEntity be) {
                            return be;
                        }
                    }
                }
            }
        }
        return null;
    }

    public ChishiEnergyStorage energy() {
        return energy;
    }

    @Override
    public Component getDisplayName() {
        EnergyCellTier tier = ((ChishiEnergyCellBlock) getBlockState().getBlock()).getTier();
        return Component.translatable(switch (tier) {
            case BASIC -> "block.template_mod.chishi_energy_cell_basic";
            case ADVANCED -> "block.template_mod.chishi_energy_cell_advanced";
            case SUPER -> "block.template_mod.chishi_energy_cell_super";
            default -> "block.template_mod.chishi_energy_cell_basic";
        });
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiEnergyCellMenu(id, inv, cellSlot, data);
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
