package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiGenEnergyOutputPortBlock;
import com.example.akaishi.block.AkaishiGenFuelInputPortBlock;
import com.example.akaishi.block.AkaishiGenMatrixCasingBlock;
import com.example.akaishi.block.AkaishiGenMatrixControllerBlock;
import com.example.akaishi.block.AkaishiGenMatrixTier;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.AkaishiFuels;
import com.example.akaishi.menu.AkaishiGenMatrixControllerMenu;
import com.example.akaishi.multiblock.MatrixStructure;
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
 * 发生器矩阵控制器：类反应堆式矩阵主方块（低级/高级由方块等级决定）。
 * 结构扫描以自身为中心，半径内（低级 1 / 高级 2）所有位置必须为矩阵外壳或端口；
 * 成型后以对应倍率集中产能，能量经能量输出口对外输出，燃料经燃料输入口注入。
 * 数据槽：0=能量，1=燃烧能量，2=燃料总能量，3=结构状态，4=升级组件数。
 */
public class AkaishiGenMatrixControllerBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container, IDataCarrier {

    public static final int FUEL_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    /** 能源产生升级组件装配槽起始（燃料槽之后连续 10 格，最多装 10 个） */
    public static final int UPGRADE_SLOT_START = SLOT_COUNT;
    public static final int UPGRADE_SLOTS = 10;
    public static final int TOTAL_SLOTS = SLOT_COUNT + UPGRADE_SLOTS;

    private final SimpleContainer inventory;
    private final SimpleContainerData data = new SimpleContainerData(5);
    private final AkaishiEnergyStorage energy;
    private int burnEnergy;
    private int burnEnergyTotal;
    /** 最近一次成型的箱体范围（解除端口关联时使用） */
    private BlockPos boxMin, boxMax;

    public AkaishiGenMatrixControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_GEN_MATRIX_CONTROLLER.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, tier().maxEnergy);
        this.inventory = new SimpleContainer(TOTAL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiGenMatrixControllerBlockEntity.this.setChanged();
            }
        };
    }

    /** 矩阵等级（由方块实例决定，低级/高级） */
    public AkaishiGenMatrixTier tier() {
        Block b = getBlockState().getBlock();
        return b instanceof AkaishiGenMatrixControllerBlock c ? c.tier() : AkaishiGenMatrixTier.BASIC;
    }

    /** 加速倍率：n 个组件 → 1.75^n 倍速度 × (1 - 1%×n) 产出，满配 10 个 ≈ 242 倍 */
    public static double getBoostMultiplier(int upgradeCount) {
        int n = Math.max(0, Math.min(upgradeCount, UPGRADE_SLOTS));
        return Math.pow(1.75, n) * (1.0 - 0.01 * n);
    }

    /** 统计装配的能源产生升级组件数量（0-10） */
    public int getUpgradeCount() {
        int n = 0;
        for (int i = UPGRADE_SLOT_START; i < TOTAL_SLOTS; i++) {
            if (inventory.getItem(i).is(com.example.akaishi.item.ModItems.akaishiSpeedUpgrade.get())) {
                n++;
            }
        }
        return n;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiGenMatrixControllerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = false;
        data.set(0, (int) energy.getEnergyStored());
        data.set(1, burnEnergy);
        data.set(2, burnEnergyTotal);
        int upgrades = getUpgradeCount();
        data.set(4, upgrades);

        boolean formed = getBlockState().getValue(AkaishiGenMatrixControllerBlock.FORMED);
        // 成型后每 10 tick 校验一次即可（结构变化不频繁，减少扫描开销）
        boolean checked = !formed || ++structureTick % 10 == 0;
        MatrixStructure.Result scan = checked
                ? MatrixStructure.scan(level, worldPosition, tier().radius * 2 + 1, this::isWall)
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
        data.set(3, formed ? 1 : 0);

        // 成型后以对应倍率集中产能（低级 45 倍 / 高级 200 倍）
        AkaishiGenMatrixTier t = tier();
        if (formed && energy.getEnergyStored() < t.maxEnergy) {
            if (burnEnergy <= 0) {
                int fuel = AkaishiFuels.getFuelEnergy(inventory.getItem(FUEL_SLOT));
                if (fuel > 0) {
                    inventory.removeItem(FUEL_SLOT, 1);
                    // 燃料总能量按等级系数折算（低级 ×9/8，高级 ×9/4）
                    burnEnergyTotal = fuel * t.fuelNum / t.fuelDen;
                    burnEnergy = burnEnergyTotal;
                    changed = true;
                }
            }
            if (burnEnergy > 0) {
                int consume = Math.min(t.generateRate, burnEnergy);
                burnEnergy -= consume;
                energy.addEnergy((long) (consume * getBoostMultiplier(upgrades)), false);
                changed = true;
            }
        }

        if (changed) {
            setChanged();
        }
    }

    /** 墙块判定：矩阵外壳 / 能量输出口 / 燃料输入口 / 控制器自身（控制器在墙面上） */
    private boolean isWall(Block b) {
        return b instanceof AkaishiGenMatrixCasingBlock
                || b instanceof AkaishiGenEnergyOutputPortBlock
                || b instanceof AkaishiGenFuelInputPortBlock
                || b instanceof AkaishiGenMatrixControllerBlock;
    }

    /** 结构检测节流计数（成型后每 10 tick 校验一次） */
    private int structureTick;

    /** 切换结构状态：同步自身 FORMED 标记，并建立/解除端口与控制器的关联 */
    private void setFormed(boolean formed, MatrixStructure.Result scan) {
        level.setBlock(worldPosition, getBlockState().setValue(AkaishiGenMatrixControllerBlock.FORMED, formed), 3);
        // 解除关联时遍历上次成型的箱体范围
        BlockPos min = formed ? scan.min : boxMin;
        BlockPos max = formed ? scan.max : boxMax;
        if (min == null || max == null) {
            return;
        }
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(p);
            BlockPos link = formed ? worldPosition : null;
            if (be instanceof AkaishiGenEnergyOutputPortBlockEntity o) {
                o.setControllerPos(link);
            } else if (be instanceof AkaishiGenFuelInputPortBlockEntity f) {
                f.setControllerPos(link);
            }
        }
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

    /** 结构是否成型（端口用） */
    public boolean isFormed() {
        return getBlockState().getValue(AkaishiGenMatrixControllerBlock.FORMED);
    }

    // ===== Container：使 AE2 存储总线 / Mekanism 物流管道能直接读写机器槽位（零硬依赖） =====

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

    // ===== IEnergyProvider：结构是能量源，可被能量管道抽取 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public boolean canOutputEnergy() {
        return true;
    }

    @Override
    public boolean canInputEnergy() {
        return false;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_gen_matrix_controller_" + tier().suffix);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiGenMatrixControllerMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    /** 挖掘保留数据：排除随方块掉落的容器物品，防止放置时复制 */
    @Override
    public String[] excludedKeys() {
        return new String[]{"Items"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("BurnEnergy", burnEnergy);
        tag.putInt("BurnEnergyTotal", burnEnergyTotal);
        NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        burnEnergy = tag.getInt("BurnEnergy");
        burnEnergyTotal = tag.getInt("BurnEnergyTotal");
        NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
