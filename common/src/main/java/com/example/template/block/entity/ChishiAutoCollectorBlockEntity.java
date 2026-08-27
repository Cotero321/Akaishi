package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.item.IItemPipeDevice;
import com.example.template.block.ChishiAutoCollectorBlock;
import com.example.template.block.ChishiCrystalClusterBlock;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.item.ModItems;
import com.example.template.menu.ChishiAutoCollectorMenu;
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
 * 自动收集器方块实体：每 tick 消耗赤能源，累积到工作阈值后自动收获
 * 3×3×3 范围内一颗水晶簇，将产物（赤石精华 1-2 个）存入内部 27 槽容器。
 * 实现 Container 接口：漏斗可抽取，AE2 存储总线 / Mekanism 物流管道可直接读写。
 */
public class ChishiAutoCollectorBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container, IItemPipeDevice {

    /** 内部存储槽数（箱子一样：27 格） */
    public static final int STORAGE_SIZE = 27;
    /** 能量缓冲容量 */
    public static final int MAX_ENERGY = 50000;
    /** 与 Menu 同步的数据槽数量（0=能量 1=容量 2=收集进度%） */
    public static final int DATA_SLOTS = 3;

    private final ChishiAutoCollectorBlock.CollectorTier tier;
    private final ChishiEnergyStorage energy;
    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    /** 当前收集进度（tick），满 {@code tier.workTicks} 收获一次 */
    private int progressTicks;

    public ChishiAutoCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_AUTO_COLLECTOR.get(), pos, state);
        this.tier = state.getBlock() instanceof ChishiAutoCollectorBlock block
                ? block.tier() : ChishiAutoCollectorBlock.CollectorTier.BASIC;
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, MAX_ENERGY);
        this.inventory = new SimpleContainer(STORAGE_SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiAutoCollectorBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiAutoCollectorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(0, (int) energy.getEnergyStored());
        data.set(1, MAX_ENERGY);
        data.set(2, (int) (progressTicks * 100L / tier.workTicks));

        // 范围内无水晶簇：空闲不耗能，进度归零
        BlockPos cluster = findCluster();
        if (cluster == null) {
            progressTicks = 0;
            return;
        }
        if (energy.getEnergyStored() < tier.energyCost) {
            return; // 能量不足，收集暂停
        }
        energy.extractEnergy(tier.energyCost, false);
        progressTicks++;
        if (progressTicks >= tier.workTicks) {
            progressTicks = 0;
            // 收获：精华入容器成功才移除方块（容器满则不破坏，方块保留）
            int count = 1 + level.random.nextInt(2); // 1-2 个，与原版战利品表一致
            if (addEssence(count)) {
                level.removeBlock(cluster, false);
            }
        }
        setChanged();
    }

    /** 在 3×3×3 范围内查找第一颗水晶簇 */
    private BlockPos findCluster() {
        int half = tier.range / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                for (int dz = -half; dz <= half; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() instanceof ChishiCrystalClusterBlock) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /** 将精华堆叠入容器，成功返回 true（任一槽可容纳即整体成功） */
    private boolean addEssence(int count) {
        ItemStack essence = new ItemStack(ModItems.chishiEssence.get(), count);
        // 先尝试合并进已有堆叠，再放入空槽
        for (int i = 0; i < STORAGE_SIZE; i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && slot.is(ModItems.chishiEssence.get())
                    && slot.getCount() + count <= slot.getMaxStackSize()) {
                slot.grow(count);
                return true;
            }
        }
        for (int i = 0; i < STORAGE_SIZE; i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, essence);
                return true;
            }
        }
        return false; // 容器已满
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    public ChishiEnergyStorage energy() {
        return energy;
    }

    // ===== Container：使漏斗 / AE2 存储总线 / Mekanism 物流管道能直接访问存储空间 =====

    // ===== IItemPipeDevice：收集器 27 槽全量输入输出，管道可自动供料与取走精华 =====

    @Override
    public int[] getPipeInputSlots() {
        return allStorageSlots();
    }

    @Override
    public int[] getPipeOutputSlots() {
        return allStorageSlots();
    }

    private int[] allStorageSlots() {
        int[] slots = new int[STORAGE_SIZE];
        for (int i = 0; i < STORAGE_SIZE; i++) {
            slots[i] = i;
        }
        return slots;
    }

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
        return inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
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
    public Component getDisplayName() {
        return switch (tier) {
            case BASIC -> Component.translatable("block.template_mod.chishi_collector_basic");
            case MEDIUM -> Component.translatable("block.template_mod.chishi_collector_medium");
            case ADVANCED -> Component.translatable("block.template_mod.chishi_collector_advanced");
            case ULTIMATE -> Component.translatable("block.template_mod.chishi_collector_ultimate");
        };
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiAutoCollectorMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("ProgressTicks", progressTicks);
        NonNullList<ItemStack> items = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < STORAGE_SIZE; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        progressTicks = tag.getInt("ProgressTicks");
        NonNullList<ItemStack> items = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < STORAGE_SIZE; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
