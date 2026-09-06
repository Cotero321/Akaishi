package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiCrystalBlocks;
import com.example.akaishi.block.AkaishiMatrixBlocks;
import com.example.akaishi.block.AkaishiPurifierEnergyInputPortBlock;
import com.example.akaishi.block.AkaishiPurifierItemInputPortBlock;
import com.example.akaishi.block.AkaishiPurifierItemOutputPortBlock;
import com.example.akaishi.block.AkaishiPurifierMatrixCasingBlock;
import com.example.akaishi.block.AkaishiPurifierMatrixControllerBlock;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiPurifierMatrixControllerMenu;
import com.example.akaishi.multiblock.MatrixStructure;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 提纯矩阵控制器：类反应堆式矩阵主方块（3×3×3）。
 * 结构成型后消耗赤能源集中提纯：粗制赤石块→1 精华，赤石水晶块→4 精华。
 * 能量经能量输入口（或直接管道）注入，原料/产物经物品输入/输出口流转。
 * 数据槽：0=能量，1=进度百分比，2=结构状态。
 */
public class AkaishiPurifierMatrixControllerBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IEnergyProvider, Container, IDataCarrier, IUpgradeableMachine {

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    public static final int DATA_SLOTS = 3;

    /** 最大能量存储（与旧提纯器一致） */
    public static final int MAX_ENERGY = 10000;
    /** 单次提纯所需总能量 */
    public static final long TOTAL_COST = 500L;
    /** 成型后每 tick 提纯消耗能量（30 倍速度，总耗不变） */
    public static final long RATE_FORMED = 150L;

    private final SimpleContainer inventory;
    private final SimpleContainerData data = new SimpleContainerData(DATA_SLOTS);
    private final AkaishiEnergyStorage energy;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 已投入提纯能量（能量池模式，满 TOTAL_COST 完成一次） */
    private long progressEnergy;
    /** 最近一次成型的箱体范围（解除端口关联时使用） */
    private BlockPos boxMin, boxMax;

    public AkaishiPurifierMatrixControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PURIFIER_MATRIX_CONTROLLER.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, MAX_ENERGY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiPurifierMatrixControllerBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPurifierMatrixControllerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = false;
        // 动态扩容：能量升级组件生效时按倍率提升能量上限
        energy.setMaxEnergy((long) (MAX_ENERGY * getEnergyCapacityMultiplier()));
        data.set(0, (int) energy.getEnergyStored());

        boolean formed = getBlockState().getValue(AkaishiPurifierMatrixControllerBlock.FORMED);
        // 成型后每 10 tick 校验一次即可（结构变化不频繁，减少扫描开销）
        boolean checked = !formed || ++structureTick % 10 == 0;
        MatrixStructure.Result scan = checked
                ? MatrixStructure.scan(level, worldPosition, 3, this::isWall)
                : null;
        boolean valid = checked ? scan != null : formed;
        if (formed != valid) {
            setFormed(valid, scan);
            if (valid) {
                boxMin = scan.min;
                boxMax = scan.max;
            } else {
                boxMin = null;
                boxMax = null;
            }
            formed = valid;
            changed = true;
        }
        data.set(2, formed ? 1 : 0);

        // 成型后集中提纯：消耗能量推进进度，不足时暂停不清零（速度升级：消耗率 ×(1+12.5%/级)）
        if (formed && canProcess()) {
            long extract = Math.min((long) (RATE_FORMED * getSpeedMultiplier()), energy.getEnergyStored());
            if (extract > 0) {
                energy.extractEnergy(extract, false);
                progressEnergy += extract;
                if (progressEnergy >= TOTAL_COST) {
                    progressEnergy -= TOTAL_COST;
                    inventory.removeItem(INPUT_SLOT, 1);
                    ItemStack out = inventory.getItem(OUTPUT_SLOT);
                    int amount = outputPerInput();
                    if (out.isEmpty()) {
                        inventory.setItem(OUTPUT_SLOT, new ItemStack(ModItems.akaishiEssence.get(), amount));
                    } else {
                        out.grow(amount);
                    }
                }
                changed = true;
            }
        } else if (formed) {
            // 无有效输入或输出已满：重置进度
            progressEnergy = 0;
        }
        data.set(1, (int) (progressEnergy * 100 / TOTAL_COST));

        if (changed) {
            setChanged();
        }
    }

    /** 墙块判定：矩阵外壳 / 三种端口 / 控制器自身 / 结构玻璃（控制器在墙面上） */
    private boolean isWall(Block b) {
        return b instanceof AkaishiPurifierMatrixCasingBlock
                || b instanceof AkaishiPurifierItemInputPortBlock
                || b instanceof AkaishiPurifierItemOutputPortBlock
                || b instanceof AkaishiPurifierEnergyInputPortBlock
                || b instanceof AkaishiPurifierMatrixControllerBlock
                || b == AkaishiMatrixBlocks.CHISHI_PURIFIER_MATRIX_STRUCTURE_GLASS.get();
    }

    /** 结构检测节流计数（成型后每 10 tick 校验一次） */
    private int structureTick;

    /** 切换结构状态：同步自身 FORMED 标记，并建立/解除端口与控制器的关联 */
    private void setFormed(boolean formed, MatrixStructure.Result scan) {
        level.setBlock(worldPosition, getBlockState().setValue(AkaishiPurifierMatrixControllerBlock.FORMED, formed), 3);
        // 解除关联时遍历上次成型的箱体范围
        BlockPos min = formed ? scan.min : boxMin;
        BlockPos max = formed ? scan.max : boxMax;
        if (min == null || max == null) {
            return;
        }
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(p);
            BlockPos link = formed ? worldPosition : null;
            if (be instanceof AkaishiPurifierItemInputPortBlockEntity in) {
                in.setControllerPos(link);
            } else if (be instanceof AkaishiPurifierItemOutputPortBlockEntity out) {
                out.setControllerPos(link);
            } else if (be instanceof AkaishiPurifierEnergyInputPortBlockEntity e) {
                e.setControllerPos(link);
            }
        }
    }

    /** 是否具备提纯条件：输入有效 + 输出可容纳 */
    private boolean canProcess() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input.isEmpty() || !isValidInput(input)) {
            return false;
        }
        ItemStack out = inventory.getItem(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        return out.is(ModItems.akaishiEssence.get()) && out.getCount() + outputPerInput() <= out.getMaxStackSize();
    }

    /** 有效输入：粗制赤石块 或 赤石水晶块 */
    private boolean isValidInput(ItemStack stack) {
        return stack.is(ModBlocks.RAW_CHISHI_BLOCK.get().asItem())
                || stack.is(AkaishiCrystalBlocks.CHISHI_CRYSTAL_BLOCK.get().asItem());
    }

    /** 单次提纯产出的精华数：赤石水晶块 4 个，粗制赤石块 1 个 */
    private int outputPerInput() {
        return inventory.getItem(INPUT_SLOT).is(AkaishiCrystalBlocks.CHISHI_CRYSTAL_BLOCK.get().asItem()) ? 4 : 1;
    }

    /** 结构是否成型（端口用） */
    public boolean isFormed() {
        return getBlockState().getValue(AkaishiPurifierMatrixControllerBlock.FORMED);
    }

    public Container inventory() {
        return inventory;
    }

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    public ContainerData data() {
        return data;
    }

    // ===== Container =====

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

    // ===== IEnergyProvider：纯消耗型，只接收管道注入 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_purifier_matrix_controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiPurifierMatrixControllerMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
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
        energy.setEnergy(tag.getLong("Energy"));
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
