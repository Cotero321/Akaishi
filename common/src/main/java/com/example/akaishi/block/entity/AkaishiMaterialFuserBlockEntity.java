package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.api.recipe.IMachineProcessKind;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiEnergyProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiMachineRecipeIndex;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiMaterialFuserMenu;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 材料融合器：<b>两种原料 + 赤能源 → 一件机械材料</b>（单方块形态）。
 *
 * <p>配方数据位于 {@code data/akaishi/recipes/fusing/*.json}（{@code akaishi:fusing} 类型，
 * 复用 {@link AkaishiEnergyProcessRecipe}：{@code ingredient/input_count} 为基底、
 * {@code ingredient2/input_count2} 为辅料，{@code energy} 为单次总耗）。
 *
 * <p><b>两个输入槽不区分槽序</b>：管道 / 无线端点投料并不保证先 A 后 B（
 * {@code OwnMachineEndpoint} 按物品逐个塞、顺序由执行器决定），若按固定槽序匹配会漏命中；
 * 故匹配时两格全排列比较，命中后记住"A 在哪格、B 在哪格"再按格扣料。
 *
 * <p><b>能量口径</b>：仅赤能源，只进不出；单次加工总耗 = 配方 {@code energy}（每件 1M），
 * 按 {@link #BASE_TICKS} 摊平为每 tick 抽取额（速度升级加快进度并按耗能倍率抬高功率，
 * 与 {@code MachineProcessEnergy} 同源）。
 */
public class AkaishiMaterialFuserBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier, IUpgradeableMachine,
        IMachineProcessKind {

    /** 基底输入槽（配方 {@code ingredient}） */
    public static final int SLOT_INPUT_A = 0;
    /** 辅料输入槽（配方 {@code ingredient2}） */
    public static final int SLOT_INPUT_B = 1;
    /** 产物输出槽（只读） */
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_COUNT = 3;

    /** 能量缓冲基础容量（待调手感值：够连续加工若干次） */
    public static final long BASE_CAPACITY = 10_000_000L;
    /** 单次加工基础耗时 tick（待调手感值：200t = 10s） */
    public static final int BASE_TICKS = 200;
    /** 配方未声明 {@code energy} 时的单次总耗兜底（待调手感值：与用户口径每件 1M 一致） */
    public static final long DEFAULT_ENERGY_PER_CRAFT = 1_000_000L;

    // ===== 数据槽（能量/容量/成本为 long，各占低/高 16 位两槽，见 LongDataSlots） =====
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_CAPACITY = 2;
    public static final int DATA_CAPACITY_HIGH = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_REQUIRED = 5;
    public static final int DATA_COST = 6;
    public static final int DATA_COST_HIGH = 7;
    public static final int DATA_SLOTS = 8;

    private final AkaishiEnergyStorage energy;
    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    protected final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 运转音播放器（复用能量机器音色，避免新增音效资源） */
    private final MachineHum hum = new MachineHum(ModSounds.ENERGY_AGGREGATOR_HUM, 0.4F, 1.0F);

    /** 当前命中的配方与槽位分配（变更即重置进度，防跨配方错配） */
    @Nullable
    private AkaishiEnergyProcessRecipe currentRecipe;
    private int currentSlotA = -1;
    private int currentSlotB = -1;
    protected int progress;
    /** 速度升级小数余量（避免 (int) 截断使低速升级无效） */
    private float speedAccum;

    /** 配方命中结果：配方 + 两个原料所在的容器槽（单原料配方 bSlot = -1） */
    private record Match(AkaishiEnergyProcessRecipe recipe, int aSlot, int bSlot) {
    }

    public AkaishiMaterialFuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MATERIAL_FUSER.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, BASE_CAPACITY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiMaterialFuserBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiMaterialFuserBlockEntity be) {
        be.tickServer();
    }

    @Override
    public RecipeType<?> processKind() {
        return AkaishiRecipeTypes.FUSING.get();
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }

    // ===== 加工主循环 =====

    private void tickServer() {
        // 能量升级动态扩容（与其它机器同口径）
        energy.setMaxEnergy((long) (BASE_CAPACITY * getEnergyCapacityMultiplier()));
        LongDataSlots.write(data, DATA_ENERGY, DATA_ENERGY_HIGH, energy.getEnergyStored());
        LongDataSlots.write(data, DATA_CAPACITY, DATA_CAPACITY_HIGH, energy.getMaxEnergy());
        data.set(DATA_REQUIRED, BASE_TICKS);

        Match match = findMatch();
        if (match == null) {
            reset();
            data.set(DATA_PROGRESS, 0);
            LongDataSlots.write(data, DATA_COST, DATA_COST_HIGH, 0L);
            return;
        }
        AkaishiEnergyProcessRecipe recipe = match.recipe();
        // 换配方 / 换槽分配 → 进度清零
        if (currentRecipe != recipe || currentSlotA != match.aSlot() || currentSlotB != match.bSlot()) {
            progress = 0;
            speedAccum = 0;
            currentRecipe = recipe;
            currentSlotA = match.aSlot();
            currentSlotB = match.bSlot();
        }
        long perTickBase = perTickBase(recipe);
        // 每 tick 抽取额 = 基础额 × [machine] costMultiplier × 速度升级耗能倍率（与 MachineProcessEnergy 同源）
        long perTick = (long) (perTickBase * ModConfig.machineCostMultiplier) * (long) getEnergyCostMultiplier();
        data.set(DATA_PROGRESS, progress);
        LongDataSlots.write(data, DATA_COST, DATA_COST_HIGH, perTickBase * BASE_TICKS);

        // 输出不可容纳 / 能量不足 → 待机（不扣料、不推进）
        if (!canFitOutput(recipe.result().getItem())
                || perTick <= 0L || energy.getEnergyStored() < perTick) {
            setChanged();
            return;
        }
        energy.extractEnergy(perTick, false);
        hum.tick(level, worldPosition);
        speedAccum += getSpeedMultiplier();
        int delta = (int) speedAccum;
        if (delta > 0) {
            speedAccum -= delta;
            progress += delta;
        }
        if (progress >= BASE_TICKS) {
            progress = 0;
            speedAccum = 0;
            inventory.removeItem(match.aSlot(), recipe.inputCount());
            if (recipe.ingredient2() != null && match.bSlot() >= 0) {
                inventory.removeItem(match.bSlot(), recipe.inputCount2());
            }
            addOutput(recipe.result());
        }
        setChanged();
    }

    /** 单次加工总耗按基础耗时摊平（配方 energy 优先，缺省兜底） */
    private static long perTickBase(AkaishiEnergyProcessRecipe recipe) {
        long total = recipe.energy() > 0L ? recipe.energy() : DEFAULT_ENERGY_PER_CRAFT;
        return Math.max(1L, (total + BASE_TICKS - 1L) / BASE_TICKS);
    }

    private void reset() {
        progress = 0;
        speedAccum = 0;
        currentRecipe = null;
        currentSlotA = -1;
        currentSlotB = -1;
    }

    /**
     * 在 {@code akaishi:fusing} 配方表里找命中：两格全排列比较（不区分槽序），
     * 命中后记住 A/B 各自落在哪一格。
     */
    @Nullable
    private Match findMatch() {
        if (level == null) {
            return null;
        }
        ItemStack s0 = inventory.getItem(SLOT_INPUT_A);
        ItemStack s1 = inventory.getItem(SLOT_INPUT_B);
        for (AkaishiEnergyProcessRecipe recipe
                : AkaishiMachineRecipeIndex.all(level.getRecipeManager(), AkaishiRecipeTypes.FUSING.get())) {
            Ingredient ingA = recipe.ingredient();
            if (ingA == null) {
                continue;
            }
            Ingredient ingB = recipe.ingredient2();
            if (ingB == null) {
                // 单原料兜底：任一格命中即可（本机现有配方均为双原料）
                if (fits(s0, ingA, recipe.inputCount())) {
                    return new Match(recipe, SLOT_INPUT_A, -1);
                }
                if (fits(s1, ingA, recipe.inputCount())) {
                    return new Match(recipe, SLOT_INPUT_B, -1);
                }
                continue;
            }
            if (fits(s0, ingA, recipe.inputCount()) && fits(s1, ingB, recipe.inputCount2())) {
                return new Match(recipe, SLOT_INPUT_A, SLOT_INPUT_B);
            }
            if (fits(s1, ingA, recipe.inputCount()) && fits(s0, ingB, recipe.inputCount2())) {
                return new Match(recipe, SLOT_INPUT_B, SLOT_INPUT_A);
            }
        }
        return null;
    }

    private static boolean fits(ItemStack stack, Ingredient ingredient, int need) {
        return !stack.isEmpty() && stack.getCount() >= need && ingredient.test(stack);
    }

    private boolean canFitOutput(Item output) {
        ItemStack cur = inventory.getItem(SLOT_OUTPUT);
        return cur.isEmpty() || (cur.is(output) && cur.getCount() < cur.getMaxStackSize());
    }

    private void addOutput(ItemStack result) {
        ItemStack cur = inventory.getItem(SLOT_OUTPUT);
        if (cur.isEmpty()) {
            inventory.setItem(SLOT_OUTPUT, result.copy());
        } else if (cur.is(result.getItem())) {
            cur.grow(result.getCount());
            inventory.setItem(SLOT_OUTPUT, cur);
        }
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== IItemPipeDevice：两格输入供料、一格输出取料（第三方物流与自研端点同源） =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SLOT_INPUT_A, SLOT_INPUT_B};
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
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // ===== IEnergyProvider：只接收赤能源（纯消耗型） =====

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

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_material_fuser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiMaterialFuserMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
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
        if (progress >= BASE_TICKS) {
            progress = 0; // 跨配方错配或口径变更 → 清零，交由下一 tick 重新判定
        }
        speedAccum = 0;
        reset();
    }
}
