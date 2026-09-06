package com.example.akaishi.block.entity;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiEnergyGeneratorBlock;
import com.example.akaishi.block.AkaishiSuperGeneratorCoreBlock;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.AkaishiFuels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 超级发生器架构核心方块实体：已停用为纯材料（发生器矩阵取代），不再形成多方块结构、不再提供界面。
 * 保留能量/燃料存储（NBT 持久化），作为合成材料时无实际功能。
 */
public class AkaishiSuperGeneratorCoreBlockEntity extends BlockEntity implements IEnergyProvider, Container, IItemPipeDevice {

    public static final int FUEL_SLOT = 0;
    public static final int SLOT_COUNT = 1;
    /** 能源产生升级组件装配槽起始（燃料槽之后连续 10 格，最多装 10 个） */
    public static final int UPGRADE_SLOT_START = SLOT_COUNT;
    /** 升级装配槽数量 */
    public static final int UPGRADE_SLOTS = 10;
    /** 容器总槽数 = 燃料 + 升级 */
    public static final int TOTAL_SLOTS = SLOT_COUNT + UPGRADE_SLOTS;

    /** 最大能量存储 = 单台发生机 10 万 × 500 倍 */
    public static final long MAX_ENERGY = 100000L * 500;
    /** 结构激活燃烧速度 = 单台 75 × 200 倍（超级处理速度） */
    public static final int GENERATE_RATE = 15000;
    /**
     * 燃料总能量 = 燃料能量值 × 9 / 4（= ×2.25 = 单台总产能 × 3）。
     * 单台总产能 = 燃料能量/100 × 75 = 燃料能量 × 0.75，×3 后 = 燃料能量 × 2.25。
     * 采用按能量消耗而非整数 tick：每 tick 产出并消耗 min(剩余, 15000)，确保总产能精确为单台 3 倍。
     */
    private static final int TOTAL_ENERGY_NUMERATOR = 9;
    private static final int TOTAL_ENERGY_DENOMINATOR = 4;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private AkaishiEnergyStorage energy;

    private int burnEnergy;
    private int burnEnergyTotal;

    public AkaishiSuperGeneratorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_SUPER_GENERATOR_CORE.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, MAX_ENERGY);
        this.inventory = new SimpleContainer(TOTAL_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiSuperGeneratorCoreBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(5);
    }

    /** 加速倍率：n 个组件 → 1.75^n 倍速度 × (1 - 1%×n) 产出，满配 10 个 ≈ 242 倍 */
    public static double getBoostMultiplier(int upgradeCount) {
        int n = Math.max(0, Math.min(upgradeCount, AkaishiSuperGeneratorCoreBlockEntity.UPGRADE_SLOTS));
        return Math.pow(1.75, n) * (1.0 - 0.01 * n);
    }

    /** 统计装配的能源产生升级组件数量（0-10，最多 10 个） */
    public int getUpgradeCount() {
        int n = 0;
        for (int i = UPGRADE_SLOT_START; i < TOTAL_SLOTS; i++) {
            if (inventory.getItem(i).is(com.example.akaishi.item.ModItems.akaishiSpeedUpgrade.get())) {
                n++;
            }
        }
        return n;
    }

    /** 服务端 tick：验证结构 + 集中产能 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiSuperGeneratorCoreBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = false;
        data.set(0, (int) energy.getEnergyStored());
        data.set(1, burnEnergy);
        data.set(2, burnEnergyTotal);
        int upgrades = getUpgradeCount();
        data.set(4, upgrades);

        boolean formed = getBlockState().getValue(AkaishiSuperGeneratorCoreBlock.FORMED);
        boolean valid = isStructureValid();
        if (formed != valid) {
            setFormed(valid);
            formed = valid;
            changed = true;
        }
        data.set(3, formed ? 1 : 0);

        // 结构完整时以 200 倍速率集中产能，燃料总产能精确为单台的 3 倍
        if (formed && energy.getEnergyStored() < MAX_ENERGY) {
            if (burnEnergy <= 0) {
                int fuel = AkaishiFuels.getFuelEnergy(inventory.getItem(FUEL_SLOT));
                if (fuel > 0) {
                    inventory.removeItem(FUEL_SLOT, 1);
                    // 燃料总能量 = 燃料能量 × 9/4（单台总产能 × 3）
                    burnEnergyTotal = fuel * TOTAL_ENERGY_NUMERATOR / TOTAL_ENERGY_DENOMINATOR;
                    burnEnergy = burnEnergyTotal;
                    changed = true;
                }
            }
            if (burnEnergy > 0) {
                // 每 tick 产出并消耗 min(剩余, 15000)，按能量精确消耗，低档燃料不再被取整吞掉
                int consume = Math.min(GENERATE_RATE, burnEnergy);
                burnEnergy -= consume;
                // 升级组件：每个 ×1.75 倍产出速度、减少 1% 产出（净倍率 1.75^n × (1-0.01n)）
                energy.addEnergy((long) (consume * getBoostMultiplier(upgrades)), false);
                changed = true;
            }
        }

        if (changed) {
            setChanged();
        }
    }

    /** 结构校验：已停用（5×5×5 发生器矩阵取代），恒返回 false 不触发成型 */
    private boolean isStructureValid() {
        return false;
    }

    /** 切换结构状态：同步自身与 124 个外壳发生机的 formed 标记 */
    private void setFormed(boolean formed) {
        level.setBlock(worldPosition, getBlockState().setValue(AkaishiSuperGeneratorCoreBlock.FORMED, formed), 3);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof AkaishiEnergyGeneratorBlock
                            && s.getValue(AkaishiEnergyGeneratorBlock.FORMED) != formed) {
                        level.setBlock(p, s.setValue(AkaishiEnergyGeneratorBlock.FORMED, formed), 3);
                    }
                }
            }
        }
    }

    public Container inventory() {
        return inventory;
    }

    // ===== IItemPipeDevice：燃料槽可入、升级装配槽不向物流开放 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{FUEL_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[0]; // 燃料消耗型，无物品产物
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

    public ContainerData data() {
        return data;
    }

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    /** 结构是能量源：向网络输出赤能源 */
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
