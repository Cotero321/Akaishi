package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.life.mechanical.MechanicalMachineCosts;
import com.example.akaishi.sound.MachineHum;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.util.LongDataSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 机械改造三机共用基类：双能源（赤能 + 生命能）双进度池驱动 + 升级槽 + 第三方物流兼容。
 * <p>
 * 运行语义：仅在收到"制作"请求后，赤能与生命能各自累计进度（双进度池），两者都累计满各自费用时
 * 完成一次工艺并自动撤销请求（单次制作，不再无限循环）；速度升级按倍率提升每 tick 抽取量。
 * 数据布局：0/1 = 赤能（低/高 32 位），2/3 = 赤容量，4/5 = 生命能，6/7 = 生命容量，
 * 8 = 综合进度%，9 = 制作状态（bit0 请求中、bit1 可制作），10+ = 子类自定义。
 */
public abstract class AbstractMechanicalMachineBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IUpgradeableMachine, IDataCarrier {

    public static final int DATA_AKAISHI = 0;
    public static final int DATA_AKAISHI_MAX = 2;
    public static final int DATA_LIFE = 4;
    public static final int DATA_LIFE_MAX = 6;
    public static final int DATA_PROGRESS = 8;
    /** 制作状态数据槽：bit0=请求中（制作中），bit1=当前条件可制作 */
    public static final int DATA_CRAFT = 9;
    /** 子类自定义槽起始下标（也是基类数据槽总数，供菜单占位兜底引用） */
    public static final int DATA_EXTRA_BASE = 10;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage akaishi;
    private final AkaishiEnergyStorage life;
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    private final int slotCount;
    /** 赤能进度池（累计满 chishiCostTotal() 完成一次） */
    private long progressAkaishi;
    /** 生命能进度池（累计满 lifeCostTotal() 完成一次） */
    private long progressLife;
    /** 单次制作请求：为 true 时才累计进度，完成一次后自动清除 */
    private boolean craftRequested;
    /** 运转音播放器（延迟创建，音色由子类提供） */
    private MachineHum hum;

    protected AbstractMechanicalMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.slotCount = slotCount;
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, MechanicalMachineCosts.chishiCapacity());
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, MechanicalMachineCosts.lifeCapacity());
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(slotCount) {
            @Override
            public void setChanged() {
                super.setChanged();
                AbstractMechanicalMachineBlockEntity.this.setChanged();
            }
        };
        // 槽位布局：0~9 为基类字段（能量/容量各占低/高 2 槽），10+ 为子类自定义
        this.data = new SimpleContainerData(DATA_EXTRA_BASE + extraDataSlots());
    }

    /**
     * 请求制作一次（C2S 包调用，仅服务端）：条件满足且当前无请求时置位。
     * 完成后由 {@link #tickServer()} 自动清除，实现"点一次只做一次"。
     */
    public void requestCraft() {
        if (!craftRequested && canProcess()) {
            craftRequested = true;
            setChanged();
        }
    }

    // ==================== 子类契约 ====================

    /** 额外的 data 槽数（{@link #DATA_EXTRA_BASE} 之后留给子类） */
    protected abstract int extraDataSlots();

    /** 填充子类自定义 data 槽 */
    protected abstract void populateExtraData(SimpleContainerData data);

    /** 该机器的显示名（lang 键） */
    protected abstract Component getMachineName();

    /** 创建菜单 */
    protected abstract AbstractContainerMenu createMenuServer(int id, Inventory inv);

    /** 赤能总消耗（一次工艺的进度目标） */
    protected abstract long chishiCostTotal();

    /** 生命能总消耗（一次工艺的进度目标） */
    protected abstract long lifeCostTotal();

    /** 一次工艺的基础耗时（tick），速度升级在此基础上提升每 tick 推进量 */
    protected abstract int processTicksTotal();

    /** 工艺条件：原料齐备 + 输出可容纳；不满足时双进度清零 */
    protected abstract boolean canProcess();

    /** 双进度池均满时执行：消耗物品/产出（进度已在基类扣减） */
    protected abstract void onProgressCompleted();

    /** 本机运转音色（子类提供，基类统一负责播放节奏） */
    protected abstract RegistrySupplier<SoundEvent> humSound();

    /** 延迟创建运转音播放器（仅服务端 tick 调用，无并发问题） */
    private MachineHum hum() {
        if (hum == null) {
            hum = new MachineHum(humSound(), 0.4F, 1.0F);
        }
        return hum;
    }

    // ==================== 服务端 tick ====================

    public static void tick(Level level, BlockPos pos, BlockState state, AbstractMechanicalMachineBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 动态扩容：能量升级按倍率提升双能源容量
        akaishi.setMaxEnergy((long) (MechanicalMachineCosts.chishiCapacity() * getEnergyCapacityMultiplier()));
        life.setMaxEnergy((long) (MechanicalMachineCosts.lifeCapacity() * getEnergyCapacityMultiplier()));

        // 同步数据（能量为 long，拆低/高 32 位两槽）
        LongDataSlots.write(data, DATA_AKAISHI, DATA_AKAISHI + 1, akaishi.getEnergyStored());
        LongDataSlots.write(data, DATA_AKAISHI_MAX, DATA_AKAISHI_MAX + 1, akaishi.getMaxEnergy());
        LongDataSlots.write(data, DATA_LIFE, DATA_LIFE + 1, life.getEnergyStored());
        LongDataSlots.write(data, DATA_LIFE_MAX, DATA_LIFE_MAX + 1, life.getMaxEnergy());
        long aCost = Math.max(1, chishiCostTotal());
        long lCost = Math.max(1, lifeCostTotal());
        int pct = (int) Math.min(100, Math.min(progressAkaishi * 100 / aCost, progressLife * 100 / lCost));
        data.set(DATA_PROGRESS, pct);

        // 制作状态：bit0 请求中，bit1 当前可制作（供按钮置灰/进度显示）
        boolean ready = canProcess();
        // 条件中断（原料被取走/输出满）时撤销请求，避免条件恢复后自动续作
        if (craftRequested && !ready) {
            craftRequested = false;
            setChanged();
        }
        data.set(DATA_CRAFT, (craftRequested ? 1 : 0) | (ready ? 2 : 0));
        populateExtraData(data);

        // 单次制作：无请求或条件不满足则清零进度并等待下一次点击
        if (!craftRequested || !ready) {
            if (progressAkaishi != 0 || progressLife != 0) {
                progressAkaishi = 0;
                progressLife = 0;
                setChanged();
            }
            return;
        }

        // 以单次总成本和配置工时确定固定基数，速度升级按倍率提升每 tick 推进量。
        int ticks = Math.max(1, processTicksTotal());
        float speed = Math.max(0.0F, getSpeedMultiplier());
        long remainingA = Math.max(0L, aCost - progressAkaishi);
        long remainingL = Math.max(0L, lCost - progressLife);
        long extractA = extractionForTick(aCost, remainingA, ticks, speed);
        long extractL = extractionForTick(lCost, remainingL, ticks, speed);
        if (extractA > akaishi.getEnergyStored() || extractL > life.getEnergyStored()) {
            return;
        }

        boolean changed = false;
        if (extractA > 0) {
            akaishi.extractEnergy(extractA, false);
            progressAkaishi += extractA;
            changed = true;
        }
        if (extractL > 0) {
            life.extractEnergy(extractL, false);
            progressLife += extractL;
            changed = true;
        }
        // 实际推进时播放本机运转音
        if (extractA > 0 || extractL > 0) {
            hum().tick(level, worldPosition);
        }

        if (progressAkaishi >= aCost && progressLife >= lCost) {
            // 扣除最终进度前复核，避免物流在本 tick 改变库存时先耗能后产出失败。
            if (canProcess()) {
                progressAkaishi -= aCost;
                progressLife -= lCost;
                onProgressCompleted();
                // 单次制作完成：撤销请求，停止继续生产
                craftRequested = false;
            } else {
                progressAkaishi = 0;
                progressLife = 0;
                craftRequested = false;
            }
            changed = true;
        }
        if (changed) {
            setChanged();
        }
    }

    private static long extractionForTick(long totalCost, long remainingCost, int ticks, float speed) {
        if (totalCost <= 0 || remainingCost <= 0 || speed <= 0.0F) {
            return 0L;
        }
        long base = Math.max(1L, (totalCost + ticks - 1L) / ticks);
        long scaled = Math.max(1L, (long) Math.ceil(base * speed));
        return Math.min(remainingCost, scaled);
    }

    // ==================== 容器转发（供 Menu / IItemPipeDevice / 漏斗） ====================

    public Container inventory() {
        return inventory;
    }

    public SimpleContainerData data() {
        return data;
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
    public boolean canPlaceItem(int index, ItemStack stack) {
        return inventory.canPlaceItem(index, stack);
    }

    // ==================== 双能源：均为输入，不外输 ====================

    @Override
    public IEnergyStorage getEnergyStorage() {
        return akaishi;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        if (type == AkaishiEnergyType.INSTANCE) {
            return akaishi;
        }
        if (type == LifeEnergyType.INSTANCE) {
            return life;
        }
        return null;
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
        return type == AkaishiEnergyType.INSTANCE || type == LifeEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return false;
    }

    // ==================== 菜单提供 ====================

    @Override
    public Component getDisplayName() {
        return getMachineName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return createMenuServer(id, inv);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ==================== 持久化 ====================

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("AkaishiEnergy", akaishi.getEnergyStored());
        tag.putLong("LifeEnergy", life.getEnergyStored());
        tag.putLong("ProgressAkaishi", progressAkaishi);
        tag.putLong("ProgressLife", progressLife);
        tag.putBoolean("Crafting", craftRequested);
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
        saveExtraNbt(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        for (int i = 0; i < slotCount; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        akaishi.setEnergy(tag.getLong("AkaishiEnergy"));
        life.setEnergy(tag.getLong("LifeEnergy"));
        progressAkaishi = tag.getLong("ProgressAkaishi");
        progressLife = tag.getLong("ProgressLife");
        craftRequested = tag.getBoolean("Crafting");
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        loadExtraNbt(tag);
        NonNullList<ItemStack> items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < slotCount; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    /** 子类额外持久化（选择值等） */
    protected void saveExtraNbt(CompoundTag tag) {}

    /** 子类额外恢复 */
    protected void loadExtraNbt(CompoundTag tag) {}

    /** 掉落保留数据（能量/进度/升级/槽位），排除已随方块掉落的内部物品键 */
    @Override
    public String[] excludedKeys() {
        // 内部物品（含升级槽 "Items" 键）已随方块掉落，排除防止 loot 实体 + BlockEntityTag 双重掉落
        return new String[]{"Items"};
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }

    public long getCostAkaishiForDisplay() {
        return chishiCostTotal();
    }

    public long getCostLifeForDisplay() {
        return lifeCostTotal();
    }
}