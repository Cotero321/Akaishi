package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiAutoCollectorBlock;
import com.example.akaishi.block.AkaishiCrystalClusterBlock;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiAutoCollectorMenu;
import com.example.akaishi.sound.MachineHum;
import com.example.akaishi.sound.ModSounds;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.util.LongDataSlots;
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
public class AkaishiAutoCollectorBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    /** 内部存储槽数（箱子一样：27 格） */
    public static final int STORAGE_SIZE = 27;
    /** 能量缓冲容量 */
    public static final int MAX_ENERGY = 50000;
    /** 与 Menu 同步的数据槽（能量/容量上限 5 万×能量升级倍率，超 short ±32767，各拆低/高两槽） */
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_CAPACITY = 2;
    public static final int DATA_CAPACITY_HIGH = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_STATUS = 5;
    public static final int DATA_SLOTS = 6;
    public static final int DATA_STATUS_IDLE = 0;    // 范围内无水晶簇，待机
    public static final int DATA_STATUS_NO_ENERGY = 1; // 能量不足，暂停
    public static final int DATA_STATUS_WORKING = 2; // 正在收集

    private final AkaishiAutoCollectorBlock.CollectorTier tier;
    private final AkaishiEnergyStorage energy;
    /** 运转音播放器（本机音色） */
    private final MachineHum hum = new MachineHum(ModSounds.AUTO_COLLECTOR_HUM, 0.4F, 1.0F);
    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 当前收集进度（tick），满 {@code tier.workTicks} 收获一次 */
    private int progressTicks;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;

    public AkaishiAutoCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_AUTO_COLLECTOR.get(), pos, state);
        this.tier = state.getBlock() instanceof AkaishiAutoCollectorBlock block
                ? block.tier() : AkaishiAutoCollectorBlock.CollectorTier.BASIC;
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, MAX_ENERGY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(STORAGE_SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiAutoCollectorBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiAutoCollectorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 动态扩容：能量升级组件生效时按倍率提升能量上限
        energy.setMaxEnergy((long) (MAX_ENERGY * getEnergyCapacityMultiplier()));
        // 状态判定：0=待机（无目标） 1=能量不足 2=工作中
        // 运行能耗 = 基础耗能 × 速度升级耗能倍率（判定与扣费口径一致）
        long energyCost = (long) (tier.energyCost * getEnergyCostMultiplier());
        BlockPos cluster = findCluster();
        int status;
        if (cluster == null) {
            status = DATA_STATUS_IDLE;
            progressTicks = 0;
            speedAccum = 0;
        } else if (energy.getEnergyStored() < energyCost) {
            status = DATA_STATUS_NO_ENERGY;
        } else {
            status = DATA_STATUS_WORKING;
            energy.extractEnergy(energyCost, false);
            hum.tick(level, worldPosition);
            // 速度升级：每级 +100%，8 级封顶 8 倍速（小数余量累积避免截断）
            speedAccum += getSpeedMultiplier();
            int delta = (int) speedAccum;
            if (delta > 0) {
                speedAccum -= delta;
                progressTicks += delta;
            }
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
        // 能量/容量拆两槽同步（容量随能量升级最高 25 万，超 short）
        LongDataSlots.write(data, DATA_ENERGY, DATA_ENERGY_HIGH, energy.getEnergyStored());
        LongDataSlots.write(data, DATA_CAPACITY, DATA_CAPACITY_HIGH, energy.getMaxEnergy());
        data.set(DATA_PROGRESS, (int) (progressTicks * 100L / tier.workTicks));
        data.set(DATA_STATUS, status);
    }

    /** 在 3×3×3 范围内查找第一颗水晶簇 */
    private BlockPos findCluster() {
        int half = tier.range / 2;
        for (int dx = -half; dx <= half; dx++) {
            for (int dy = -half; dy <= half; dy++) {
                for (int dz = -half; dz <= half; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() instanceof AkaishiCrystalClusterBlock) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /** 将精华堆叠入容器，成功返回 true（任一槽可容纳即整体成功） */
    private boolean addEssence(int count) {
        ItemStack essence = new ItemStack(ModItems.akaishiEssence.get(), count);
        // 先尝试合并进已有堆叠，再放入空槽
        for (int i = 0; i < STORAGE_SIZE; i++) {
            ItemStack slot = inventory.getItem(i);
            if (!slot.isEmpty() && slot.is(ModItems.akaishiEssence.get())
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

    public AkaishiEnergyStorage energy() {
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
            case BASIC -> Component.translatable("block.akaishi.akaishi_collector_basic");
            case MEDIUM -> Component.translatable("block.akaishi.akaishi_collector_medium");
            case ADVANCED -> Component.translatable("block.akaishi.akaishi_collector_advanced");
            case ULTIMATE -> Component.translatable("block.akaishi.akaishi_collector_ultimate");
        };
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiAutoCollectorMenu(id, inv, this);
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
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
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
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        NonNullList<ItemStack> items = NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < STORAGE_SIZE; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
