package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.api.item.IItemPipeDevice;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.LifeEnergyType;
import com.example.template.item.ModItems;
import com.example.template.life.sample.ChishiLifeSampleItem;
import com.example.template.life.sequence.ChishiGeneSequenceItem;
import com.example.template.menu.ChishiGeneAnalyzerMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
 * 生命分析台方块实体（仅服务端驱动逻辑）。
 * 对纯度 25 及以上的生命样本解构为基因序列片段：
 * - 纯度不足的样本无法放入输入槽（GUI/管道均拒绝）
 * - 解构耗时 5 秒，消耗 5000 生命能量，成功率随纯度线性插值（25→70%，100→95%，失败损失样本）
 * - 基因序列保留样本纯度，供结构台造器官时按纯度分配完整度
 * 槽位：0=输入（纯度 ≥25 的生命样本），1=输出（基因序列片段）。
 */
public class ChishiGeneAnalyzerBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier {

    /** 单次解构消耗的生命能量 */
    public static final long LIFE_COST = 5000L;
    /** 生命能量缓冲容量（够 2 次解构） */
    public static final long LIFE_CAPACITY = 10_000L;
    /** 解构耗时（tick） */
    public static final int PROGRESS_TICKS = 100;
    /** 解构成功率下限（纯度 25）与上限（纯度 100），随纯度线性插值 */
    public static final float MIN_SUCCESS_RATE = 0.70F;
    public static final float MAX_SUCCESS_RATE = 0.95F;
    /** 可解构的最低样本纯度 */
    public static final int MIN_PURITY = 25;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    /** Menu 同步数据槽：0/1=生命能量/容量 2=解构进度百分比 */
    public static final int DATA_SLOTS = 3;
    public static final int DATA_PROGRESS = 2;

    /** 按样本纯度插值解构成功率：纯度 25 → 70%，纯度 100 → 95%，区间内线性 */
    public static float successRate(int purity) {
        float t = (float) (purity - MIN_PURITY) / (100 - MIN_PURITY);
        t = Math.max(0, Math.min(1, t));
        return MIN_SUCCESS_RATE + t * (MAX_SUCCESS_RATE - MIN_SUCCESS_RATE);
    }

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final ChishiEnergyStorage life;
    /** 当前解构进度（tick） */
    private int progress;

    public ChishiGeneAnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_GENE_ANALYZER.get(), pos, state);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiGeneAnalyzerBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiGeneAnalyzerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(0, (int) life.getEnergyStored());
        data.set(1, (int) life.getMaxEnergy());
        data.set(2, progress * 100 / PROGRESS_TICKS);

        boolean changed = false;
        if (canProcess()) {
            progress++;
            if (progress >= PROGRESS_TICKS) {
                progress = 0;
                life.extractEnergy(LIFE_COST, false);
                // 无论成败样本都消耗；先取 NBT 再扣减
                ItemStack sample = inventory.getItem(INPUT_SLOT);
                ItemStack sequence = ChishiGeneSequenceItem.createFromSample(sample);
                sample.shrink(1);
                // 成功率判定：按样本纯度插值，失败则样本流失无产出
                if (level.random.nextFloat() < successRate(ChishiLifeSampleItem.getPurity(sample))) {
                    ItemStack out = inventory.getItem(OUTPUT_SLOT);
                    if (out.isEmpty()) {
                        inventory.setItem(OUTPUT_SLOT, sequence);
                    } else {
                        out.grow(1);
                    }
                }
            }
            changed = true;
        } else {
            progress = 0;
        }
        if (changed) {
            setChanged();
        }
    }

    /** 解构条件：纯度 ≥25 样本在位 + 生命能量充足 + 输出可容纳 */
    private boolean canProcess() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input.isEmpty() || !(input.getItem() instanceof ChishiLifeSampleItem)
                || ChishiLifeSampleItem.getPurity(input) < MIN_PURITY) {
            return false;
        }
        if (life.getEnergyStored() < LIFE_COST) {
            return false;
        }
        ItemStack out = inventory.getItem(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(ModItems.geneSequence.get()) && out.getCount() < out.getMaxStackSize();
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== Container：使漏斗 / AE2 存储总线 / 物品管道可直接读写槽位 =====

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return inventory.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return inventory.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return inventory.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        inventory.setItem(index, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // ===== IItemPipeDevice：输入槽收样本，输出槽出序列片段 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    // ===== IEnergyProvider：仅生命能量输入 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return life;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE ? life : null;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_gene_analyzer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiGeneAnalyzerMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("LifeEnergy", life.getEnergyStored());
        tag.putInt("Progress", progress);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        life.setEnergy(tag.getLong("LifeEnergy"));
        progress = tag.getInt("Progress");
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
