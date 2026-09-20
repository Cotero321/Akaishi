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
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiActivatedFractionatorMenu;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 活化分馏器方块实体（仅服务端驱动逻辑）。
 * 将活化结晶深度拆分：1 个活化结晶 → 1 个对应活化成分（主）+ 1 个衰竭结晶（副）。
 * 每次加工耗时 {@link ModConfig#fractionatorProcessTicks} tick、消耗赤能源一次结清；
 * 输出槽只读（防止杂物卡死机器）。
 * 换料/取空输入槽会清零进度，防止跨配方错配白嫖半程进度。
 *
 * <p><b>配方来自数据包</b>（{@code data/akaishi/recipes/fractionating/*.json}，类型
 * {@code akaishi:fractionating}）：输入匹配、主产物与副产全部由配方描述，
 * 机器侧不再硬编码"结晶 ↔ 成分"对照表 —— 那条表以前是机器与 JEI 各维护一份。
 * <p>本机自述工序族（{@link IMachineProcessKind}），使虚拟加工能要求"场域内真有分馏器"。
 */
public class AkaishiActivatedFractionatorBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier, IUpgradeableMachine,
        IMachineProcessKind {

    // ===== 数据槽 =====
    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_HIGH = 1;
    public static final int DATA_ENERGY_CAPACITY = 2;
    public static final int DATA_ENERGY_CAPACITY_HIGH = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_SLOTS = 5;

    private final SimpleContainerData data;
    private final AkaishiEnergyStorage energy;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 输入槽（0=活化结晶，7 种任一） */
    private final SimpleContainer input;
    /** 输出槽（0=活化成分，1=衰竭结晶） */
    private final SimpleContainer output = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            AkaishiActivatedFractionatorBlockEntity.this.setChanged();
        }
    };
    /** 当前加工进度（tick，满 {@link ModConfig#fractionatorProcessTicks} 结算一次） */
    private int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;
    /** 当前生效的配方（数据包提供；换配方即清零进度，防跨配方白嫖半程） */
    @Nullable
    private AkaishiItemProcessRecipe currentRecipe;
    /** 运转音播放器（本机音色） */
    private final MachineHum hum = new MachineHum(ModSounds.ACTIVATED_FRACTIONATOR_HUM, 0.4F, 1.0F);

    public AkaishiActivatedFractionatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ACTIVATED_FRACTIONATOR.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, ModConfig.fractionatorEnergyCapacity);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.input = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiActivatedFractionatorBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiActivatedFractionatorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 机器升级：能量升级动态扩容能量缓冲（倍率变化时自动夹取）
        energy.setMaxEnergy((long) (ModConfig.fractionatorEnergyCapacity * getEnergyCapacityMultiplier()));
        LongDataSlots.write(data, DATA_ENERGY, DATA_ENERGY_HIGH, energy.getEnergyStored());
        LongDataSlots.write(data, DATA_ENERGY_CAPACITY, DATA_ENERGY_CAPACITY_HIGH, energy.getMaxEnergy());
        data.set(DATA_PROGRESS, progress);

        ItemStack inputStack = input.getItem(0);
        // 配方来自数据包：按输入栈匹配（组内线性匹配，故标签原料也能命中）
        AkaishiItemProcessRecipe recipe = AkaishiMachineRecipeIndex.find(level,
                AkaishiRecipeTypes.FRACTIONATING.get(), inputStack);
        if (recipe == null) {
            // 无有效输入 → 清零进度（换料/取空均在此兜底）
            progress = 0;
            speedAccum = 0;
            currentRecipe = null;
            return;
        }
        Item main = recipe.result().getItem();
        Item byproduct = recipe.byproduct().isEmpty() ? null : recipe.byproduct().getItem();
        // 换配方防御：配方种类变化 → 清零进度，防止跨配方白嫖半程
        if (recipe != currentRecipe) {
            progress = 0;
            speedAccum = 0;
            currentRecipe = recipe;
        }
        // 单次加工耗能 = 基础 × 速度升级耗能倍率（封顶 4×）
        long craftCost = (long) (ModConfig.fractionatorCostPerCraft * getEnergyCostMultiplier());
        // tick 前检查能量足够才扣费（能量不足 → 暂停，进度保持）
        if (energy.getEnergyStored() < craftCost) {
            return;
        }
        // 产物槽不可容纳（加工中满仓）→ 暂停等待腾出
        if (progress < ModConfig.fractionatorProcessTicks && !canFit(main, byproduct)) {
            return;
        }
        // 机器升级：速度升级提升每 tick 加工进度（每级 +100%，8 级 8 倍速；小数余量累积避免截断）
        speedAccum += getSpeedMultiplier();
        int delta = (int) speedAccum;
        if (delta > 0) {
            speedAccum -= delta;
            progress += delta;
            hum.tick(level, worldPosition);
        }
        if (progress >= ModConfig.fractionatorProcessTicks) {
            if (canFit(main, byproduct)) {
                progress = 0;
                inputStack.shrink(recipe.inputCount());
                energy.extractEnergy(craftCost, false);
                addOutput(0, main);
                if (byproduct != null) {
                    addOutput(1, byproduct);
                }
            }
            // 满进度但产物槽满 → 保持满值，等待腾出后下 tick 结算
        }
        setChanged();
    }

    /** 本机自述工序族：虚拟加工据此要求场域内确有分馏器（见 {@link IMachineProcessKind}） */
    @Override
    public RecipeType<?> processKind() {
        return AkaishiRecipeTypes.FRACTIONATING.get();
    }

    private boolean canFit(int slot, Item item) {
        ItemStack cur = output.getItem(slot);
        return cur.isEmpty() || (cur.is(item) && cur.getCount() < cur.getMaxStackSize());
    }

    /** 主产物与副产的槽位是否都容得下（副产为 null 表示本配方无副产） */
    private boolean canFit(Item main, @Nullable Item byproduct) {
        return canFit(0, main) && (byproduct == null || canFit(1, byproduct));
    }

    private void addOutput(int slot, Item item) {
        ItemStack cur = output.getItem(slot);
        if (cur.isEmpty()) {
            output.setItem(slot, new ItemStack(item));
        } else if (cur.is(item)) {
            cur.grow(1);
            output.setItem(slot, cur);
        }
    }

    public SimpleContainer inputContainer() {
        return input;
    }

    public SimpleContainer outputContainer() {
        return output;
    }

    // ===== IItemPipeDevice：对外虚拟槽 0=输入、1/2=两个输出（组合容器视图，方向与 GUI 一致） =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{0};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{1, 2};
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty() && output.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        if (index == 0) {
            return input.getItem(0);
        }
        return index >= 1 && index <= 2 ? output.getItem(index - 1) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index == 0) {
            return input.removeItem(0, count);
        }
        return index >= 1 && index <= 2 ? output.removeItem(index - 1, count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index == 0) {
            return input.removeItemNoUpdate(0);
        }
        return index >= 1 && index <= 2 ? output.removeItemNoUpdate(index - 1) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0) {
            input.setItem(0, stack);
        } else if (index >= 1 && index <= 2) {
            output.setItem(index - 1, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        input.clearContent();
        output.clearContent();
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_activated_fractionator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AkaishiActivatedFractionatorMenu(containerId, inventory, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    public ContainerData data() {
        return data;
    }

    // ===== IEnergyProvider：只接收赤能源（驱动分馏），不对外输出 =====

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
        return new String[]{"Output"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.put("Input", input.createTag());
        tag.put("Output", output.createTag());
        // 机器升级槽（独立 NBT key）
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        progress = tag.getInt("Progress");
        input.fromTag(tag.getList("Input", 10));
        output.fromTag(tag.getList("Output", 10));
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
