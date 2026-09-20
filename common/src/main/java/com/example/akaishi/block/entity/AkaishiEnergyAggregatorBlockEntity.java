package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.api.recipe.IMachineProcessKind;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.MachineProcessEnergy;
import com.example.akaishi.craft.recipe.AkaishiEnergyProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiEnergyAggregatorMenu;
import com.example.akaishi.sound.MachineHum;
import com.example.akaishi.sound.ModSounds;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import com.example.akaishi.util.LongDataSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 赤石能量聚合器：消耗赤能源聚合出赤石锭，或逐级升级赤石水晶母岩。
 * 配方数据位于 {@code data/akaishi/recipes/aggregating/*.json}（物品 + 赤能源 → 物品）；
 * 赤能源只进不出（由管道供能）。
 */
public class AkaishiEnergyAggregatorBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IEnergyProvider, Container, IItemPipeDevice, IUpgradeableMachine,
        IMachineProcessKind, IDataCarrier {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;

    private final AkaishiEnergyStorage energy;
    private final SimpleContainer inventory;
    /** 机器升级槽（速度 / 能量 / 无线接收各一格） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 同步数据：0/1=能量低/高，2/3=容量低/高，4=进度%，5/6=当前配方单次消耗低/高 */
    private final SimpleContainerData data;

    /** 运转音播放器（本机音色） */
    private final MachineHum hum = new MachineHum(ModSounds.ENERGY_AGGREGATOR_HUM, 0.4F, 1.0F);

    // ===== 数据槽索引（能量/容量/消耗为 long，各占低/高 32 位两槽）=====
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_CAPACITY = 2;
    public static final int DATA_CAPACITY_HIGH = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_COST = 5;
    public static final int DATA_COST_HIGH = 6;
    public static final int DATA_SLOTS = 7;

    public AkaishiEnergyAggregatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_AGGREGATOR.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.energyAggregatorEnergyCapacity);
        this.inventory = new SimpleContainer(SLOT_COUNT);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiEnergyAggregatorBlockEntity be) {
        be.tickServer();
    }

    /** 机台自述工序族（供场域调度判断"这道机械工序有没有机台能跑"） */
    @Override
    public RecipeType<?> processKind() {
        return AkaishiRecipeTypes.AGGREGATING.get();
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }

    /** 当前输入物品匹配的配方（数据包配方），无匹配返回 null */
    @Nullable
    public AkaishiEnergyProcessRecipe currentRecipe() {
        return AkaishiMachineRecipeIndex.find(level, AkaishiRecipeTypes.AGGREGATING.get(), inventory.getItem(INPUT_SLOT));
    }

    /**
     * 单次聚合的赤能源消耗。
     * <p>口径唯一来源：{@link MachineProcessEnergy#costPerRun}（与虚拟加工的成本显示同源，
     * 否则会出现"界面报的价"与"机器实收"不一致）。
     */
    private static long energyCost(@Nullable AkaishiEnergyProcessRecipe recipe) {
        return MachineProcessEnergy.costPerRun(recipe).chishi();
    }

    private void tickServer() {
        // 能量升级扩容（与其它机器同口径：配置基准 × 容量倍率）
        energy.setMaxEnergy((long) (ModConfig.energyAggregatorEnergyCapacity * getEnergyCapacityMultiplier()));
        // 速度升级 = 每 tick 可连续聚合的次数。本机没有"每 tick 推进的进度条"（充能满即产出），
        // 时间维度只能落在并行度上；单次成本固定，故不套用耗能倍率（套上就是纯惩罚，不合理）
        AkaishiEnergyProcessRecipe recipe = currentRecipe();
        int parallel = 1 + upgradeSlots.getSpeedCount();
        for (int i = 0; i < parallel && recipe != null && canProcess(recipe); i++) {
            process(recipe);
        }
        long stored = energy.getEnergyStored();
        long currentCost = energyCost(recipe);
        // long 拆高低 32 位（SimpleContainerData 仅支持 int）
        LongDataSlots.write(data, DATA_ENERGY, DATA_ENERGY_HIGH, stored);
        LongDataSlots.write(data, DATA_CAPACITY, DATA_CAPACITY_HIGH, energy.getMaxEnergy());
        // 进度 = 当次聚合的充能进度（能量 / 当前配方消耗，long 计算防溢出）；成本为 0 时视为已充满
        data.set(DATA_PROGRESS, currentCost <= 0L ? 100 : (int) Math.min(100, stored * 100L / currentCost));
        LongDataSlots.write(data, DATA_COST, DATA_COST_HIGH, currentCost);
    }

    /** 条件：能量足够 + 输入匹配配方 + 输出可容纳产物 */
    private boolean canProcess(AkaishiEnergyProcessRecipe recipe) {
        if (energy.getEnergyStored() < energyCost(recipe)) {
            return false;
        }
        ItemStack result = recipe.result();
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        return output.isEmpty() || (output.is(result.getItem())
                && output.getCount() + result.getCount() <= result.getMaxStackSize());
    }

    /** 执行聚合：消耗能量 + 按配方的单格消耗量扣除输入 → 产出配方产物 */
    private void process(AkaishiEnergyProcessRecipe recipe) {
        energy.extractEnergy(energyCost(recipe), false);
        hum.tick(level, worldPosition);
        inventory.removeItem(INPUT_SLOT, recipe.inputCount());
        ItemStack result = recipe.result();
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setItem(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }
        setChanged();
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        return type == AkaishiEnergyType.INSTANCE ? energy : null;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == AkaishiEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return false;
    }

    public ContainerData data() {
        return data;
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_energy_aggregator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiEnergyAggregatorMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ---- Container（原版容器接口，AE2 存储总线 / Mekanism 物流管道可直接访问槽位） ----

    // ---- IItemPipeDevice：物品管道精准对接——原料进 0 号输入槽，产物从 1 号输出槽取走 ----

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{OUTPUT_SLOT};
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
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.put("Inventory", inventory.createTag());
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    // ===== IDataCarrier：物品不随方块下落，其余 NBT（能量/升级）保留在掉落物 =====

    /** 与单槽族机器同一口径：{@code Inventory} 不写进掉落物 */
    @Override
    public String[] excludedKeys() {
        return new String[]{"Inventory"};
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        inventory.fromTag(tag.getList("Inventory", net.minecraft.nbt.Tag.TAG_COMPOUND));
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
    }
}
