package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.api.recipe.IMachineProcessKind;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.IAkaishiMachineRecipe;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.sound.MachineHum;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.util.LongDataSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 单输入单输出处理机器抽象基类（赤石植物培养机/压缩机/打粉机/变化器共用）。
 * 统一实现：配方表（物品→物品）、进度推进（速度升级倍率，浮点余量防截断）、
 * 每 tick 能量消耗、升级槽、数据槽同步与 NBT 持久化。
 * 子类仅需提供配方表、能量容量/耗时/能耗与菜单构造。
 */
public abstract class AkaishiSingleSlotMachineBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier, IUpgradeableMachine,
        IMachineProcessKind {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    // ===== 数据槽（Menu 同步）=====
    // 能量/容量为 long，各占低/高 32 位两槽（SimpleContainerData 仅支持 int，直接强转会溢出）
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_CAPACITY = 2;
    public static final int DATA_CAPACITY_HIGH = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_REQUIRED = 5;
    public static final int DATA_SLOTS = 6;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage energy;
    protected final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 当前配方对应输入物品（更换时重置进度，防跨配方错配） */
    private Item currentInput;
    protected int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;
    /** 运转音播放器（延迟创建，音色由子类提供） */
    private MachineHum hum;

    protected AkaishiSingleSlotMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, baseCapacity());
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiSingleSlotMachineBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    /** 本机配方类型（数据包配方，见 {@code data/akaishi/recipes/<机器>/}） */
    protected abstract RecipeType<AkaishiItemProcessRecipe> recipeType();

    /** 机台自述工序族（供场域调度判断"这道机械工序有没有机台能跑"） */
    @Override
    public RecipeType<?> processKind() {
        return recipeType();
    }

    /** 能量缓冲基础容量（能量升级按倍率扩容） */
    protected abstract long baseCapacity();

    /** 单次加工基础耗时（tick，速度升级缩短） */
    protected abstract int ticks();

    /** 每 tick 消耗的赤能源 */
    protected abstract long energyPerTick();

    /** 本机运转音色（子类提供，每台机器独立音色） */
    protected abstract RegistrySupplier<SoundEvent> humSound();

    /** 惰性创建运转音播放器（首次工作时初始化，避免构造期访问子类字段） */
    private MachineHum hum() {
        if (hum == null) {
            hum = new MachineHum(humSound(), 0.4F, 1.0F);
        }
        return hum;
    }

    /** 子类创建具体菜单（供 createMenu 委托） */
    protected abstract AbstractContainerMenu createMenuInstance(int id, Inventory inv);

    /** 服务端 tick 入口（由子类 Block 的 getTicker 绑定） */
    protected void tickServer() {
        // 能量升级动态扩容（倍率变化实时生效，容量缩小时自动夹取）
        energy.setMaxEnergy((long) (baseCapacity() * getEnergyCapacityMultiplier()));
        LongDataSlots.write(data, DATA_ENERGY, DATA_ENERGY_HIGH, energy.getEnergyStored());
        LongDataSlots.write(data, DATA_CAPACITY, DATA_CAPACITY_HIGH, energy.getMaxEnergy());

        ItemStack inputStack = inventory.getItem(SLOT_INPUT);
        Item input = inputStack.getItem();
        IAkaishiMachineRecipe recipe = AkaishiMachineRecipeIndex.find(level, recipeType(), inputStack);
        // 更换原料 → 重置进度（防止跨配方错配）
        if (currentInput == null) {
            currentInput = input;
        } else if (input != currentInput) {
            progress = 0;
            speedAccum = 0;
            currentInput = input;
        }
        data.set(DATA_REQUIRED, recipe == null ? 0 : ticks());
        data.set(DATA_PROGRESS, progress);

        // 无配方 / 输入不足 / 输出不可容纳 / 能量不足 → 待机
        // （运行能耗 = energyPerTick × 配置 [machine] costMultiplier × 速度升级耗能倍率，判定与扣费口径一致）
        long perTick = (long) (energyPerTick() * ModConfig.machineCostMultiplier * getEnergyCostMultiplier());
        // 输入保留型配方（培养机）只看"有没有料"，不看数量
        int have = recipe != null && !recipe.consumeInput() ? 1 : inputStack.getCount();
        if (recipe == null || inputStack.isEmpty() || have < recipe.inputCount()
                || !canFitOutput(recipe.result().getItem()) || energy.getEnergyStored() < perTick) {
            return;
        }
        // 推进：每 tick 扣能量，进度按速度倍率累加（小数余量防截断）
        energy.extractEnergy(perTick, false);
        hum().tick(level, worldPosition);
        speedAccum += getSpeedMultiplier();
        int delta = (int) speedAccum;
        if (delta > 0) {
            speedAccum -= delta;
            progress += delta;
        }
        if (progress >= ticks()) {
            progress = 0;
            speedAccum = 0;
            if (recipe.consumeInput()) {
                inputStack.shrink(recipe.inputCount());
            }
            addOutput(recipe.result().getItem(), recipe.result().getCount());
        }
        setChanged();
    }

    private boolean canFitOutput(Item output) {
        ItemStack cur = inventory.getItem(SLOT_OUTPUT);
        return cur.isEmpty() || (cur.is(output) && cur.getCount() < cur.getMaxStackSize());
    }

    private void addOutput(Item output, int count) {
        ItemStack cur = inventory.getItem(SLOT_OUTPUT);
        if (cur.isEmpty()) {
            inventory.setItem(SLOT_OUTPUT, new ItemStack(output, count));
        } else if (cur.is(output)) {
            cur.grow(count);
            inventory.setItem(SLOT_OUTPUT, cur);
        }
    }

    public Container inventory() {
        return inventory;
    }

    // ===== IItemPipeDevice：第三方物流向输入槽供料、从输出槽取料（方向与 GUI 一致） =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SLOT_INPUT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{SLOT_OUTPUT};
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
        return true;
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    public ContainerData data() {
        return data;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block." + nameKey());
    }

    /** 方块翻译 key（block.akaishi.<id>） */
    protected abstract String nameKey();

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return createMenuInstance(id, inv);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== IEnergyProvider：只接收赤能源（纯消耗型）=====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == AkaishiEnergyType.INSTANCE;
    }

    // ===== IDataCarrier：物品随方块掉落，其余 NBT（能量/进度/升级）保留在掉落物 =====

    @Override
    public String[] excludedKeys() {
        return new String[]{"Inventory"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.put("Inventory", inventory.createTag());
        tag.putInt("Progress", progress);
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        inventory.fromTag(tag.getList("Inventory", 10));
        progress = tag.getInt("Progress");
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        // 进度超上限（跨配方错配）→ 清零；配方是否存在由下一个 tick 查数据包配方判定
        if (progress >= ticks()) {
            progress = 0;
        }
        speedAccum = 0;
        currentInput = null;
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
