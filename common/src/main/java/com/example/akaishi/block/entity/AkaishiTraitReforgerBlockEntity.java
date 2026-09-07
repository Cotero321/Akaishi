package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.MutantTrait;
import com.example.akaishi.menu.AkaishiTraitReforgerMenu;
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

import java.util.List;

/**
 * 词条重铸仪方块实体（仅服务端驱动逻辑）。
 * 以衰竭结晶为代价、生命能量为动力，将器官上指定的第 N 条突变词条
 * 原位替换为同稀有度档的新随机词条（确定性：消耗达标必成，不损毁器官）：
 * - 输入器官必须非原生、已定型且至少携带 1 条突变词条
 * - 重铸范围限定"目标词条的稀有度档"，且排除器官已携带的全部词条（含目标本身）
 * - 无候选时无法启动（UI 提示），避免无效消耗
 * 槽位：0=器官输入，1=衰竭结晶，2=产物输出。
 */
public class AkaishiTraitReforgerBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    public static final int ORGAN_SLOT = 0;
    public static final int CRYSTAL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    /** Menu 同步数据槽：0/1=生命能量/容量 2=重铸进度% 3=词条总数 4=目标词条序号 */
    public static final int DATA_SLOTS = 5;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_COUNT = 3;
    public static final int DATA_TARGET = 4;

    /** 衰竭结晶消耗 = 稀有度 × 基数（1/2/3 档 → 2/4/6） */
    public static int crystalCost(int rarity) {
        return Math.max(1, rarity) * ModConfig.traitReforgerCrystalPerRarity;
    }

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage life;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    private int progress;
    /** 当前重铸目标词条序号（0-based，随器官变化自动钳制；NBT 持久化） */
    private int targetIndex;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;

    public AkaishiTraitReforgerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_TRAIT_REFORGER.get(), pos, state);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, ModConfig.traitReforgerLifeCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiTraitReforgerBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiTraitReforgerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容生命能量缓冲（倍率变化时自动夹取）
        life.setMaxEnergy((long) (ModConfig.traitReforgerLifeCapacity * getEnergyCapacityMultiplier()));
        data.set(0, (int) life.getEnergyStored());
        data.set(1, (int) life.getMaxEnergy());

        // 词条总数与目标序号随器官状态自愈：器官换掉/词条数缩水时钳制目标
        ItemStack organ = inventory.getItem(ORGAN_SLOT);
        int count = organ.getItem() instanceof AkaishiOrganItem
                ? AkaishiOrganItem.getMutations(organ).size() : 0;
        data.set(DATA_COUNT, count);
        if (targetIndex >= count) {
            targetIndex = Math.max(0, count - 1);
        }
        data.set(DATA_TARGET, targetIndex);

        boolean changed = false;
        if (canProcess()) {
            // 机器升级：速度升级提升每 tick 重铸进度（+1 → 每级 +12.5%，8 级 2 倍速；小数余量累积避免截断）
            speedAccum += getSpeedMultiplier();
            int delta = (int) speedAccum;
            if (delta > 0) {
                speedAccum -= delta;
                progress += delta;
            }
            if (progress >= ModConfig.traitReforgerProcessTicks) {
                progress = 0;
                settle();
            }
            changed = true;
        } else {
            progress = 0;
            speedAccum = 0;
        }
        // 进度数据槽在状态变更后写入：停机/完成当帧即同步新值，避免残留旧进度
        data.set(DATA_PROGRESS, progress * 100 / ModConfig.traitReforgerProcessTicks);
        if (changed) {
            setChanged();
        }
    }

    /** 当前目标词条（无有效器官/越界返回 null） */
    private MutantTrait targetTrait() {
        ItemStack organ = inventory.getItem(ORGAN_SLOT);
        if (!(organ.getItem() instanceof AkaishiOrganItem)) {
            return null;
        }
        List<MutantTrait> mutations = AkaishiOrganItem.getMutations(organ);
        return targetIndex >= 0 && targetIndex < mutations.size() ? mutations.get(targetIndex) : null;
    }

    /** 重铸条件：可重铸器官（≥1 词条且目标档有候选）+ 足够结晶/能量 + 产物槽可容纳 */
    private boolean canProcess() {
        ItemStack organ = inventory.getItem(ORGAN_SLOT);
        if (organ.isEmpty() || !(organ.getItem() instanceof AkaishiOrganItem)
                || AkaishiOrganItem.isNative(organ)) {
            return false;
        }
        MutantTrait old = targetTrait();
        if (old == null) {
            return false;
        }
        // 候选不足（该稀有度池内的其他词条已全部携带）时无法启动，避免无效消耗
        List<MutantTrait> mutations = AkaishiOrganItem.getMutations(organ);
        if (!MutantTrait.hasCandidates(old.getRarity(), mutations)) {
            return false;
        }
        int cost = crystalCost(old.getRarity());
        if (!inventory.getItem(CRYSTAL_SLOT).is(ModItems.exhaustedCrystal.get())
                || inventory.getItem(CRYSTAL_SLOT).getCount() < cost) {
            return false;
        }
        if (life.getEnergyStored() < ModConfig.traitReforgerLifeCost) {
            return false;
        }
        // 产物槽必须为空：重铸后器官 NBT 与既有物品不同，无法合并堆叠
        return inventory.getItem(OUTPUT_SLOT).isEmpty();
    }

    /** 重铸结算：确定性成功（消耗达标必成），原位替换目标词条为同档新词条 */
    private void settle() {
        ItemStack organ = inventory.getItem(ORGAN_SLOT);
        MutantTrait old = targetTrait();
        if (old == null) {
            return;
        }
        int cost = crystalCost(old.getRarity());
        life.extractEnergy(ModConfig.traitReforgerLifeCost, false);
        inventory.getItem(CRYSTAL_SLOT).shrink(cost);

        ItemStack result = organ.copy();
        // roll 已由 canProcess 的候选校验保证非空（排除全部已携带词条，新词条必不同于旧词条）
        MutantTrait fresh = MutantTrait.roll(level.random, old.getRarity(),
                AkaishiOrganItem.getMutations(result), AkaishiOrganItem.slotOf(organ));
        if (fresh != null) {
            AkaishiOrganItem.replaceMutation(result, targetIndex, fresh);
        }
        inventory.setItem(OUTPUT_SLOT, result);
        organ.shrink(1);
    }

    /** C2S 目标词条选择入口：服务端权威校验序号后写入（越界/加工中重置进度） */
    public void selectTarget(int index) {
        if (level != null && level.isClientSide) {
            return;
        }
        ItemStack organ = inventory.getItem(ORGAN_SLOT);
        int count = organ.getItem() instanceof AkaishiOrganItem
                ? AkaishiOrganItem.getMutations(organ).size() : 0;
        if (index < 0 || index >= count) {
            return;
        }
        progress = 0;
        speedAccum = 0;
        targetIndex = index;
        setChanged();
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
            case ORGAN_SLOT -> stack.getItem() instanceof AkaishiOrganItem
                    && !AkaishiOrganItem.isNative(stack)
                    && AkaishiOrganItem.getMutations(stack).size() > 0;
            case CRYSTAL_SLOT -> stack.is(ModItems.exhaustedCrystal.get());
            default -> false;
        };
    }

    // ===== IDataCarrier：物品随方块破坏实体掉落，从掉落物 NBT 排除（防放置后复制） =====

    @Override
    public String[] excludedKeys() {
        return new String[]{"Items"};
    }

    // ===== IItemPipeDevice：器官/结晶输入，产物输出 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{ORGAN_SLOT, CRYSTAL_SLOT};
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
        return Component.translatable("block.akaishi.akaishi_trait_reforger");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiTraitReforgerMenu(id, inv, this);
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
        tag.putInt("TargetIndex", targetIndex);
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
        targetIndex = tag.getInt("TargetIndex");
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
