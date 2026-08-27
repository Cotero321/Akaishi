package com.example.template.block.entity;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.block.ChishiEnergyAssemblyBlock;
import com.example.template.block.ChishiSuperGeneratorCoreBlock;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.ChishiEnergyType;
import com.example.template.energy.ChishiFuels;
import com.example.template.item.ModItems;
import com.example.template.menu.ChishiEnergyGeneratorMenu;
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
 * 赤能源发生机方块实体：燃烧赤石材料产生赤能源（单方块 75/tick）。
 * 作为 3x3 多方块结构外壳时（formed=true）休眠，不再独立燃烧。
 * 数据槽：0=能量，1=燃烧时间，2=燃烧总时间。
 */
public class ChishiEnergyGeneratorBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container {

    public static final int FUEL_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    /** 能源产生升级组件装配槽起始（燃料槽之后连续 10 格，最多装 10 个） */
    public static final int UPGRADE_SLOT_START = SLOT_COUNT;
    /** 升级装配槽数量 */
    public static final int UPGRADE_SLOTS = 10;
    /** 容器总槽数 = 燃料 + 升级 */
    public static final int TOTAL_SLOTS = SLOT_COUNT + UPGRADE_SLOTS;

    /** 最大能量存储 */
    public static final int MAX_ENERGY = 100000;
    /** 单方块燃烧产能速率（产能减半后 150→75） */
    public static final int GENERATE_RATE = 75;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private ChishiEnergyStorage energy;

    private int burnTime;
    private int burnTimeTotal;

    public ChishiEnergyGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ENERGY_GENERATOR.get(), pos, state);
        this.energy = new ChishiEnergyStorage(ChishiEnergyType.INSTANCE, MAX_ENERGY);
        this.inventory = new SimpleContainer(TOTAL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiEnergyGeneratorBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(4);
    }

    /** 加速倍率：n 个组件 → 2^n 倍速度 × (1 - 1%×n) 产出，满配 10 个 ≈ 922 倍 */
    public static double getBoostMultiplier(int upgradeCount) {
        int n = Math.max(0, Math.min(upgradeCount, ChishiEnergyGeneratorBlockEntity.UPGRADE_SLOTS));
        return Math.pow(2, n) * (1.0 - 0.01 * n);
    }

    /** 服务端 tick：燃烧产能；结构外壳时休眠 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiEnergyGeneratorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = false;
        data.set(0, (int) energy.getEnergyStored());
        data.set(1, burnTime);
        data.set(2, burnTimeTotal);
        int upgrades = getUpgradeCount();
        data.set(3, upgrades);

        // formed=true 表示被多方块结构征用为外壳：休眠，不独立燃烧
        boolean formed = getBlockState().getValue(com.example.template.block.ChishiEnergyGeneratorBlock.FORMED);
        if (!formed && energy.getEnergyStored() < MAX_ENERGY) {
            if (burnTime <= 0) {
                int fuel = ChishiFuels.getFuelEnergy(inventory.getItem(FUEL_SLOT));
                if (fuel > 0) {
                    inventory.removeItem(FUEL_SLOT, 1);
                    // 燃烧时长 = 燃料能量 / 100（燃料消耗节奏加快 100 倍）
                    burnTimeTotal = fuel / 100;
                    burnTime = burnTimeTotal;
                    changed = true;
                }
            }
            if (burnTime > 0) {
                burnTime--;
                // 升级组件：每个翻倍产能速度、减少 1% 产出（净倍率 2^n × (1-0.01n)）
                energy.addEnergy((long) (GENERATE_RATE * getBoostMultiplier(upgrades)), false);
                changed = true;
            }
        }

        if (changed) {
            setChanged();
        }
    }

    public Container inventory() {
        return inventory;
    }

    /** 统计装配的能源产生升级组件数量（0-10，最多 10 个） */
    public int getUpgradeCount() {
        int n = 0;
        for (int i = UPGRADE_SLOT_START; i < TOTAL_SLOTS; i++) {
            if (inventory.getItem(i).is(ModItems.chishiSpeedUpgrade.get())) {
                n++;
            }
        }
        return n;
    }

    /** 成型后作为多方块外壳：容器访问动态代理到中心主方块，使 AE2 存储总线 / Mekanism 物流管道能经外壳给结构喂燃料 */
    private Container currentContainer() {
        if (getBlockState().getValue(com.example.template.block.ChishiEnergyGeneratorBlock.FORMED)) {
            ChishiSuperGeneratorCoreBlockEntity superCenter = findSuperCenter();
            if (superCenter != null) {
                return superCenter.inventory();
            }
            ChishiEnergyAssemblyBlockEntity center = findAssemblyCenter();
            if (center != null) {
                return center.inventory();
            }
        }
        return inventory;
    }

    // ===== Container：使 AE2 存储总线 / Mekanism 物流管道能直接读写机器槽位（零硬依赖） =====

    @Override
    public int getContainerSize() {
        return currentContainer().getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return currentContainer().isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return currentContainer().getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return currentContainer().removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return currentContainer().removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        currentContainer().setItem(index, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return currentContainer().stillValid(player);
    }

    @Override
    public void clearContent() {
        currentContainer().clearContent();
    }

    public ContainerData data() {
        return data;
    }

    public ChishiEnergyStorage energy() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        // 成型后作为多方块外壳：代理中心主方块的存储。
        // 中心主方块被外壳完全包围，管道无法直连，必须经外壳才能把结构能量导出网络。
        if (getBlockState().getValue(com.example.template.block.ChishiEnergyGeneratorBlock.FORMED)) {
            ChishiSuperGeneratorCoreBlockEntity superCenter = findSuperCenter();
            if (superCenter != null) {
                return superCenter.energy();
            }
            ChishiEnergyAssemblyBlockEntity center = findAssemblyCenter();
            if (center != null) {
                return center.energy();
            }
        }
        return energy;
    }

    /** 在自身为中心的 5×5×5 范围内查找成型中的超级发生器架构核心 */
    private ChishiSuperGeneratorCoreBlockEntity findSuperCenter() {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof ChishiSuperGeneratorCoreBlock
                            && s.getValue(ChishiSuperGeneratorCoreBlock.FORMED)) {
                        if (level.getBlockEntity(p) instanceof ChishiSuperGeneratorCoreBlockEntity be) {
                            return be;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** 在自身为中心的 3×3×3 范围内查找成型中的组合结构主方块实体 */
    private ChishiEnergyAssemblyBlockEntity findAssemblyCenter() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof ChishiEnergyAssemblyBlock
                            && s.getValue(ChishiEnergyAssemblyBlock.FORMED)) {
                        if (level.getBlockEntity(p) instanceof ChishiEnergyAssemblyBlockEntity be) {
                            return be;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** 发生机是能量源：向网络输出赤能源 */
    @Override
    public boolean canOutputEnergy() {
        return true;
    }

    /** 纯发电：不允许网络反向充能 */
    @Override
    public boolean canInputEnergy() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_energy_generator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiEnergyGeneratorMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnTimeTotal", burnTimeTotal);
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
        burnTime = tag.getInt("BurnTime");
        burnTimeTotal = tag.getInt("BurnTimeTotal");
        NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
