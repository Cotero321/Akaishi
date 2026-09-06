package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiItemReconstructorMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 物品重构仪方块实体（仅服务端驱动逻辑）。
 * 以衰竭结晶为代价嬗变物品：原料 + 衰竭结晶 → 产物（配方表见 {@link #RECIPES}）。
 * 每 tick 消耗 1 结晶与固定赤能源累计进度，进度满（=配方代价结晶数）结算产出 1 个产物；
 * 更换原料自动重置进度，防止错配。
 */
public class AkaishiItemReconstructorBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    /** 重构配方：原料 → (产物, 结晶代价) */
    public record ReconstructRecipe(Item output, int crystalCost) {
    }

    /** 配方表（硬编码；结晶代价越低嬗变越划算，高阶产物代价越高） */
    public static final Map<Item, ReconstructRecipe> RECIPES = Map.ofEntries(
            Map.entry(Items.SKELETON_SKULL, new ReconstructRecipe(Items.WITHER_SKELETON_SKULL, 32)),
            Map.entry(Items.WITHER_SKELETON_SKULL, new ReconstructRecipe(Items.NETHER_STAR, 32)),
            Map.entry(Items.GOLDEN_APPLE, new ReconstructRecipe(Items.ENCHANTED_GOLDEN_APPLE, 16)),
            Map.entry(Items.AMETHYST_SHARD, new ReconstructRecipe(Items.ECHO_SHARD, 8)),
            Map.entry(Items.DEEPSLATE, new ReconstructRecipe(Items.REINFORCED_DEEPSLATE, 8)),
            Map.entry(Items.QUARTZ, new ReconstructRecipe(Items.PRISMARINE_SHARD, 16)),
            Map.entry(Items.LEATHER, new ReconstructRecipe(Items.PHANTOM_MEMBRANE, 16)),
            Map.entry(Items.PRISMARINE_SHARD, new ReconstructRecipe(Items.SCUTE, 16)),
            Map.entry(Items.ROTTEN_FLESH, new ReconstructRecipe(Items.RABBIT_FOOT, 32)),
            Map.entry(Items.END_CRYSTAL, new ReconstructRecipe(Items.DRAGON_BREATH, 32)),
            Map.entry(Items.SNOWBALL, new ReconstructRecipe(Items.GHAST_TEAR, 32))
    );

    // ===== 数据槽 =====
    public static final int DATA_SLOTS = 5;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    /** 当前批次进度（已消耗结晶数） */
    public static final int DATA_PROGRESS = 2;
    /** 当前配方所需结晶总数（无配方为 0） */
    public static final int DATA_REQUIRED = 3;
    /** 结晶槽存量 */
    public static final int DATA_CRYSTALS = 4;

    private final SimpleContainerData data;
    private final AkaishiEnergyStorage energy;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 物品槽：0=原料，1=衰竭结晶（代价），2=产物 */
    private final SimpleContainer inventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            AkaishiItemReconstructorBlockEntity.this.setChanged();
        }
    };
    /** 当前配方对应的原料（null=空/未初始化；更换原料时重置进度） */
    private Item currentInput;
    private int progress;

    public AkaishiItemReconstructorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ITEM_RECONSTRUCTOR.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.reconstructorEnergyCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiItemReconstructorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容能量缓冲（倍率变化时自动夹取）
        energy.setMaxEnergy((long) (ModConfig.reconstructorEnergyCapacity * getEnergyCapacityMultiplier()));
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_ENERGY_CAPACITY, (int) energy.getMaxEnergy());
        data.set(DATA_CRYSTALS, inventory.getItem(1).getCount());

        ItemStack inputStack = inventory.getItem(0);
        Item input = inputStack.getItem();
        ReconstructRecipe recipe = RECIPES.get(input);
        // 更换原料 → 重置进度（防止跨配方错配）
        if (currentInput == null) {
            currentInput = input;
        } else if (input != currentInput) {
            progress = 0;
            currentInput = input;
        }
        data.set(DATA_REQUIRED, recipe == null ? 0 : recipe.crystalCost());
        data.set(DATA_PROGRESS, progress);

        if (recipe == null || inputStack.isEmpty()) {
            return; // 无配方，静默等待
        }
        ItemStack crystalStack = inventory.getItem(1);
        if (crystalStack.isEmpty() || !crystalStack.is(ModItems.exhaustedCrystal.get())) {
            return; // 结晶槽为空或非衰竭结晶
        }
        // 机器升级：速度升级每 tick 多处理若干子步（每子步消耗 1 结晶 + 能量）
        int steps = (int) getSpeedMultiplier();
        for (int s = 0; s < steps && !crystalStack.isEmpty()
                && energy.getEnergyStored() >= ModConfig.reconstructorCostPerCrystal; s++) {
            if (!canFitOutput(recipe.output())) {
                break; // 产物槽不可容纳 → 暂停等待腾出
            }
            // 每子步：消耗 1 结晶 + 能量，进度 +1；满代价结算 1 产物
            crystalStack.shrink(1);
            energy.extractEnergy(ModConfig.reconstructorCostPerCrystal, false);
            progress++;
            if (progress >= recipe.crystalCost()) {
                inputStack.shrink(1);
                addOutput(recipe.output());
                progress = 0;
            }
            setChanged();
        }
    }

    private boolean canFitOutput(Item output) {
        ItemStack cur = inventory.getItem(2);
        return cur.isEmpty() || (cur.is(output) && cur.getCount() < cur.getMaxStackSize());
    }

    private void addOutput(Item output) {
        ItemStack cur = inventory.getItem(2);
        if (cur.isEmpty()) {
            inventory.setItem(2, new ItemStack(output));
        } else if (cur.is(output)) {
            cur.grow(1);
            inventory.setItem(2, cur);
        }
    }

    public SimpleContainer inventory() {
        return inventory;
    }

    // ===== IItemPipeDevice：第三方物流向原料槽/结晶槽供料，从产物槽取料（仅输出侧开放抽取） =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{0, 1}; // 0=原料，1=衰竭结晶代价
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{2}; // 2=产物
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

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_item_reconstructor");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiItemReconstructorMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    public ContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：只接收赤能源（驱动重构），不对外输出 =====

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

    // ===== NBT =====

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
        // 机器升级槽（独立 NBT key）
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        inventory.fromTag(tag.getList("Inventory", 10));
        progress = tag.getInt("Progress");
        // 机器升级槽恢复（旧档无该 key 时保持空）
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        // 校验进度与当前原料配方匹配：原料空 / 无配方 / 进度超代价上限（跨配方错配）→ 清零，防拆除换料白嫖
        ReconstructRecipe recipe = RECIPES.get(inventory.getItem(0).getItem());
        if (recipe == null || progress >= recipe.crystalCost()) {
            progress = 0;
        }
        // 重载后由原料槽重新初始化 currentInput（同配方进度已校验保留，不重置）
        currentInput = null;
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
