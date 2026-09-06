package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.MutantTrait;
import com.example.akaishi.life.sample.SampleGroup;
import com.example.akaishi.life.sequence.AkaishiGeneSequenceItem;
import com.example.akaishi.menu.AkaishiLifeBreederMenu;
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
 * 生命培育器方块实体（仅服务端驱动逻辑）。
 * 以基因序列为培养基、衰竭结晶为催化剂对器官施加随机基因突变（双刃剑）：
 * - 输入器官必须非原生、已定型且未达词条承载上限（与序列同基因来源组才可培养）
 * - 成功率由序列纯度决定：纯度 25 → 35%、纯度 100 → 70% 线性插值（封顶 70%）
 * - 培养成功：消耗材料与原器官，返回器官副本并附加 1 条随机突变词条；
 *   失败：仅消耗材料（结晶/序列/能量），输入器官保留，可补料重试
 * 槽位：0=器官输入，1=基因序列，2=衰竭结晶，3=产物输出。
 */
public class AkaishiLifeBreederBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    /** 单次培养消耗的生命能量（消耗减半：原 120K） */
    public static final long LIFE_COST = 60_000L;
    /** 单次培养消耗的衰竭结晶数量（消耗减半：原 4） */
    public static final int CRYSTAL_COST = 2;
    /** 生命能量缓冲容量（够 2 次培养） */
    public static final long LIFE_CAPACITY = 120_000L;
    /** 单次培养耗时（tick，50 秒） */
    public static final int PROGRESS_TICKS = 1000;
    /** 成功率下限（纯度 25）与上限（纯度 100，封顶 70%） */
    public static final float MIN_SUCCESS_RATE = 0.35F;
    public static final float MAX_SUCCESS_RATE = 0.70F;
    /** 序列纯度的有效插值下限（与基因分析台解构门槛一致） */
    public static final int MIN_PURITY = 25;

    public static final int ORGAN_SLOT = 0;
    public static final int SEQUENCE_SLOT = 1;
    public static final int CRYSTAL_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int SLOT_COUNT = 4;
    /** Menu 同步数据槽：0/1=生命能量/容量 2=培养进度百分比 3=序列纯度 */
    public static final int DATA_SLOTS = 4;
    public static final int DATA_PROGRESS = 2;
    /** 序列纯度同步槽：客户端据此实时计算成功率展示（物品槽同步在结算消耗后可能滞后） */
    public static final int DATA_PURITY = 3;

    /** 成功率 = 纯度 25→35%、纯度 100→70% 线性插值；纯度越纯越接近 70% 封顶 */
    public static float successRate(int purity) {
        float t = (float) (Math.max(MIN_PURITY, Math.min(100, purity)) - MIN_PURITY) / (100 - MIN_PURITY);
        return MIN_SUCCESS_RATE + t * (MAX_SUCCESS_RATE - MIN_SUCCESS_RATE);
    }

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage life;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    private int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;

    public AkaishiLifeBreederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_LIFE_BREEDER.get(), pos, state);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiLifeBreederBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiLifeBreederBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容生命能量缓冲（倍率变化时自动夹取）
        life.setMaxEnergy((long) (LIFE_CAPACITY * getEnergyCapacityMultiplier()));
        data.set(0, (int) life.getEnergyStored());
        data.set(1, (int) life.getMaxEnergy());
        // 同步当前序列纯度（供客户端实时计算成功率，序列为空时置 0）
        ItemStack sequence = inventory.getItem(SEQUENCE_SLOT);
        data.set(DATA_PURITY, sequence.getItem() instanceof AkaishiGeneSequenceItem
                ? AkaishiGeneSequenceItem.getPurity(sequence) : 0);

        boolean changed = false;
        if (canProcess()) {
            // 机器升级：速度升级提升每 tick 培养进度（+1 → 每级 +12.5%，8 级 2 倍速；小数余量累积避免截断）
            speedAccum += getSpeedMultiplier();
            int delta = (int) speedAccum;
            if (delta > 0) {
                speedAccum -= delta;
                progress += delta;
            }
            if (progress >= PROGRESS_TICKS) {
                progress = 0;
                settle();
            }
            changed = true;
        } else {
            progress = 0;
            speedAccum = 0;
        }
        // 进度数据槽在状态变更后写入：停机/完成当帧即同步新值，避免残留旧进度
        data.set(2, progress * 100 / PROGRESS_TICKS);
        if (changed) {
            setChanged();
        }
    }

    /** 培养条件：可突变器官 + 同组序列 + 足够结晶/能量 + 产物槽可容纳 */
    private boolean canProcess() {
        ItemStack organ = inventory.getItem(ORGAN_SLOT);
        if (organ.isEmpty() || !(organ.getItem() instanceof AkaishiOrganItem) || !AkaishiOrganItem.canMutate(organ)) {
            return false;
        }
        ItemStack sequence = inventory.getItem(SEQUENCE_SLOT);
        if (sequence.isEmpty() || !(sequence.getItem() instanceof AkaishiGeneSequenceItem)) {
            return false;
        }
        // 同组催化：序列基因来源分组必须与器官一致（异源序列无法引导定向突变）
        SampleGroup organGroup = AkaishiOrganItem.getSource(organ);
        if (organGroup == null || AkaishiGeneSequenceItem.getGroup(sequence) != organGroup) {
            return false;
        }
        if (!inventory.getItem(CRYSTAL_SLOT).is(ModItems.exhaustedCrystal.get())
                || inventory.getItem(CRYSTAL_SLOT).getCount() < CRYSTAL_COST) {
            return false;
        }
        if (life.getEnergyStored() < LIFE_COST) {
            return false;
        }
        // 产物槽必须为空：突变器官 NBT 与既有物品不同，无法合并堆叠
        return inventory.getItem(OUTPUT_SLOT).isEmpty();
    }

    /** 培养结算：成败均消耗结晶 + 序列 + 能量；成功消耗原器官产出突变副本，失败器官保留可重试 */
    private void settle() {
        ItemStack organ = inventory.getItem(ORGAN_SLOT);
        ItemStack sequence = inventory.getItem(SEQUENCE_SLOT);
        int purity = AkaishiGeneSequenceItem.getPurity(sequence);

        life.extractEnergy(LIFE_COST, false);
        inventory.getItem(CRYSTAL_SLOT).shrink(CRYSTAL_COST);
        sequence.shrink(1);

        // 成功：消耗原器官并产出副本 + 随机突变词条（稀有度由纯度解锁）；失败：仅材料损失，器官留在输入槽
        if (level.random.nextFloat() < successRate(purity)) {
            ItemStack result = organ.copy();
            // 排除器官已携带词条，避免同词条重复占用承载上限
            MutantTrait trait = MutantTrait.roll(level.random, MutantTrait.maxRarity(purity),
                    AkaishiOrganItem.getMutations(result), AkaishiOrganItem.slotOf(result));
            if (trait != null) {
                AkaishiOrganItem.addMutation(result, trait);
            }
            inventory.setItem(OUTPUT_SLOT, result);
            organ.shrink(1);
        }
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== Container：漏斗 / 物品管道直接读写 =====

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

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        // 自动化（漏斗/投掷器等）入口过滤：输入槽按类型收料，产物槽只出不进（防垃圾卡死输出）
        return switch (index) {
            case ORGAN_SLOT -> stack.getItem() instanceof AkaishiOrganItem && AkaishiOrganItem.canMutate(stack);
            case SEQUENCE_SLOT -> stack.getItem() instanceof AkaishiGeneSequenceItem;
            case CRYSTAL_SLOT -> stack.is(ModItems.exhaustedCrystal.get());
            default -> false;
        };
    }

    // ===== IDataCarrier：物品随方块破坏实体掉落，从掉落物 NBT 排除（防放置后复制） =====

    @Override
    public String[] excludedKeys() {
        return new String[]{"Items"};
    }

    // ===== IItemPipeDevice：器官/序列/结晶输入，产物输出 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{ORGAN_SLOT, SEQUENCE_SLOT, CRYSTAL_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    // ===== IEnergyProvider：仅生命能量输入，不对外输出 =====

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
        return Component.translatable("block.akaishi.akaishi_life_breeder");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiLifeBreederMenu(id, inv, this);
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
        // 机器升级槽（独立 NBT key，避免与物品槽 "Items" 冲突）
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
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
        // 机器升级槽恢复（旧档无该 key 时保持空）
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
