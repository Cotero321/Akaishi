package com.example.akaishi.block.entity;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiLifePurifierMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
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
 * 生命能量提纯器方块实体（仅服务端驱动逻辑）。
 * 双能量输入：赤能源（驱动）+ 生命能量（原料）。
 * 运行：每 tick 抽取最多 1M 赤能源，累计满 10M 时消耗 1000 生命能量
 * 固化出 1 个生命能量固态物（约 10 tick/次）。
 * 槽位：0=输出（生命能量固态物）。
 */
public class AkaishiLifePurifierBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IUpgradeableMachine {

    /** 每 tick 赤能源输入率（需求速率） */
    public static final long CHISHI_RATE = 1_000_000L;
    /** 完成一次固化所需赤能源总量 */
    public static final long TOTAL_COST = 10_000_000L;
    /** 完成一次固化消耗的生命能量 */
    public static final long LIFE_COST = 1000L;
    /** 赤能源缓冲容量（够 2 次固化积累） */
    public static final long CHISHI_CAPACITY = 20_000_000L;
    /** 生命能量缓冲容量（够 5 次固化） */
    public static final long LIFE_CAPACITY = 5000L;

    public static final int OUTPUT_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    /** Menu 同步数据槽：0/1=赤能量/赤容量 2/3=生命能量/生命容量 4=固化进度百分比 */
    public static final int DATA_SLOTS = 5;
    public static final int DATA_PROGRESS = 4;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage akaishi;
    private final AkaishiEnergyStorage life;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 已投入的赤能源（能量池模式，满 {@link #TOTAL_COST} 完成一次） */
    private long progressEnergy;

    public AkaishiLifePurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_PURIFIER.get(), pos, state);
        this.akaishi = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, CHISHI_CAPACITY);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiLifePurifierBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifePurifierBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 动态扩容：能量升级组件生效时按倍率提升赤能源缓冲上限（生命能量为原料槽保持固定）
        akaishi.setMaxEnergy((long) (CHISHI_CAPACITY * getEnergyCapacityMultiplier()));
        // 同步数据到 GUI（Menu 的 broadcastChanges 据此下发客户端）
        data.set(0, (int) akaishi.getEnergyStored());
        data.set(1, (int) akaishi.getMaxEnergy());
        data.set(2, (int) life.getEnergyStored());
        data.set(3, (int) life.getMaxEnergy());
        data.set(4, (int) (progressEnergy * 100 / TOTAL_COST));

        boolean changed = false;
        // 原料（生命能量）与输出满足条件时投入赤能源推进进度；赤能源不足时进度暂停不清零
        if (canProcess()) {
            // 速度升级：每 tick 赤能源抽取率按倍率提升（总耗不变，提速消耗更快）
            long extract = Math.min((long) (CHISHI_RATE * getSpeedMultiplier()), akaishi.getEnergyStored());
            if (extract > 0) {
                akaishi.extractEnergy(extract, false);
                progressEnergy += extract;
                if (progressEnergy >= TOTAL_COST) {
                    progressEnergy -= TOTAL_COST;
                    life.extractEnergy(LIFE_COST, false);
                    ItemStack out = inventory.getItem(OUTPUT_SLOT);
                    if (out.isEmpty()) {
                        inventory.setItem(OUTPUT_SLOT, new ItemStack(ModItems.akaishiLifeEssenceSolid.get()));
                    } else {
                        out.grow(1);
                    }
                }
                changed = true;
            }
        } else {
            // 生命能量不足或输出已满：重置进度
            progressEnergy = 0;
        }
        if (changed) {
            setChanged();
        }
    }

    /** 固化条件：生命能量充足 + 输出可容纳（赤能源检查在 tick 内做，不足时暂停） */
    private boolean canProcess() {
        if (life.getEnergyStored() < LIFE_COST) {
            return false;
        }
        ItemStack out = inventory.getItem(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(ModItems.akaishiLifeEssenceSolid.get()) && out.getCount() < out.getMaxStackSize();
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== IItemPipeDevice：输出槽只允许管道抽取固态物（本机无输入槽） =====

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    // ===== Container：使漏斗 / AE2 存储总线 / 物品管道可直接读写输出槽 =====

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

    // ===== IEnergyProvider：赤能源与生命能量均为输入（驱动 + 原料），不向外输出 =====

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

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_life_purifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifePurifierMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("AkaishiEnergy", akaishi.getEnergyStored());
        tag.putLong("LifeEnergy", life.getEnergyStored());
        tag.putLong("ProgressEnergy", progressEnergy);
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 复用既有存储实例恢复，避免重建导致外部引用失效
        akaishi.setEnergy(tag.getLong("AkaishiEnergy"));
        life.setEnergy(tag.getLong("LifeEnergy"));
        progressEnergy = tag.getLong("ProgressEnergy");
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
