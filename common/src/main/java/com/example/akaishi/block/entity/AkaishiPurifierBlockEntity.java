package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiCrystalBlocks;
import com.example.akaishi.block.AkaishiPurifierBlock;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiPurifierMenu;
import com.example.akaishi.sound.ModSounds;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
 * 赤石提纯器方块实体（仅服务端驱动逻辑，客户端通过 Menu 数据展示）。
 * 槽位：0=燃料，1=输入（粗制赤石块 / 赤石水晶块），2=输出（赤石精华）。
 * 逻辑：燃烧赤石晶/粗制块产生赤石能量 → 消耗能量提纯输入为赤石精华。
 * 配方：粗制赤石块 → 1 精华；赤石水晶块 → 4 精华。
 */
public class AkaishiPurifierBlockEntity extends BlockEntity implements ExtendedMenuProvider, IEnergyProvider, Container, IDataCarrier, IUpgradeableMachine {

    public static final int FUEL_SLOT = 0;
    public static final int INPUT_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    /** 与 Menu 同步的数据槽数量（0=能量 1=燃烧时间 2=进度 3=燃烧总时间 4=矩阵成型标记） */
    public static final int DATA_SLOTS = 5;
    /** data 索引：提纯矩阵成型标记（1=成型，GUI 据此隐藏燃料槽与火焰） */
    public static final int DATA_FORMED = 4;

    /** 最大能量存储 */
    public static final int MAX_ENERGY = 10000;
    /** 提纯进度百分比满值（GUI 进度条分母） */
    public static final int MAX_PROGRESS = 100;
    /** 单方块每 tick 提纯消耗能量（需求减半后 10→5，燃烧产能 10/tick 可净积累 5/tick） */
    public static final int ENERGY_PER_TICK = 5;
    /** 每 tick 燃烧产能（产能减半后 20→10，与提纯消耗持平，可配合管道外部供能缓冲） */
    private static final int BURN_RATE = 10;
    /** 燃料能量：赤石晶 */
    public static final int FUEL_CRYSTAL = 200;
    /** 燃料能量：粗制赤石块 */
    public static final int FUEL_RAW_BLOCK = 2000;
    /** 单方块完成一次提纯所需总能量 = 100 tick × 5（提纯矩阵与单台一致，速度与耗能同步 30 倍） */
    public static final long TOTAL_COST = 500L;
    /** 提纯矩阵成型后每 tick 消耗 = 普通（5）× 30（耗能率 30 倍，配合 30 倍速度） */
    public static final long RATE_FORMED = 150L;

    private final SimpleContainer inventory;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    /** 与 Menu 同步的数据缓存：0=能量 1=燃烧时间 2=进度百分比 3=燃烧总时间 4=矩阵成型标记 */
    private final SimpleContainerData data;
    private AkaishiEnergyStorage energy;

    private int burnTime;
    private int burnTimeTotal;
    /** 已投入提纯能量（能量池模式，满 {@link #needed()} 完成一次） */
    private long progressEnergy;
    /** 运转音播放冷却（tick） */
    private int humCooldown;
    /** 提纯矩阵成型缓存（每 tick 仅读缓存，仅邻居方块变化时重扫） */
    private boolean matrixFormed;
    /** 矩阵结构待重扫标记（外壳放置/移除、区块加载时置位） */
    private boolean matrixDirty = true;

    public AkaishiPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PURIFIER.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, MAX_ENERGY);
        this.upgradeSlots.setOnChange(this::setChanged);
        // 容器变更时标记方块保存，防止进度/物品丢失
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiPurifierBlockEntity.this.setChanged();
            }
        };
        // 数据缓存：服务端每 tick 写入，客户端经 Menu 同步 set 覆盖，GUI 据此绘制
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    /** 服务端 tick：燃烧产能 + 提纯 */
    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPurifierBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = false;
        // 动态扩容：能量升级组件生效时按倍率提升能量上限
        energy.setMaxEnergy((long) (MAX_ENERGY * getEnergyCapacityMultiplier()));
        // 提纯矩阵成型检测：缓存化 + 事件驱动（仅外壳方块放置/移除时重扫），每 tick 零方块查询
        refreshMatrix();
        boolean matrixFormed = this.matrixFormed;

        // 写入数据缓存：Menu 的 broadcastChanges 每 tick 据此同步到客户端 GUI
        data.set(0, (int) energy.getEnergyStored());
        data.set(1, burnTime);
        data.set(2, (int) (progressEnergy * 100 / needed()));
        data.set(3, burnTimeTotal);
        data.set(4, matrixFormed ? 1 : 0);

        // 1) 燃烧燃料产生赤石能量（矩阵成型后禁用：耗能远超自产，统一由管道外部供能）
        if (!matrixFormed && energy.getEnergyStored() < energy.getMaxEnergy()) {
            if (burnTime <= 0) {
                int fuel = getFuelEnergy(inventory.getItem(FUEL_SLOT));
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
                energy.addEnergy(BURN_RATE, false);
                changed = true;
            }
        }

        // 2) 消耗能量提纯输入（能量不足时进度暂停，不清零）
        //    未成型：每 tick 5，共 500（100 tick/次）；成型：每 tick 150，共 500（3.34 tick/次 = 30 倍，耗能率同步 30 倍）
        //    速度升级：消耗率 ×(1+12.5%/级)，消耗与产出速率同步提升
        if (canProcess()) {
            long extract = Math.min((long) (rate() * getSpeedMultiplier()), energy.getEnergyStored());
            if (extract > 0) {
                energy.extractEnergy(extract, false);
                progressEnergy += extract;
                // 运转声（循环）：每 15 tick 重播短音
                if (--humCooldown <= 0) {
                    level.playSound(null, worldPosition, ModSounds.MACHINE_HUM.get(), SoundSource.BLOCKS, 0.4f, 1.0f);
                    humCooldown = 15;
                }
                if (progressEnergy >= needed()) {
                    progressEnergy -= needed();
                    inventory.removeItem(INPUT_SLOT, 1);
                    ItemStack out = inventory.getItem(OUTPUT_SLOT);
                    if (out.isEmpty()) {
                        inventory.setItem(OUTPUT_SLOT, new ItemStack(ModItems.akaishiEssence.get(), outputPerInput()));
                    } else {
                        out.grow(outputPerInput());
                    }
                }
                changed = true;
            }
        } else {
            // 无有效输入或输出已满：重置进度
            progressEnergy = 0;
        }

        if (changed) {
            setChanged();
        }
    }

    /** 当前模式完成一次提纯所需总能量（矩阵成型与单台一致，均为 500） */
    private long needed() {
        return TOTAL_COST;
    }

    /** 当前模式每 tick 提纯消耗能量（矩阵成型 150，未成型 5） */
    private long rate() {
        return matrixFormed ? RATE_FORMED : ENERGY_PER_TICK;
    }

    /** 当前成型缓存值（供外壳查询，不触发扫描） */
    public boolean isMatrixFormed() {
        return matrixFormed;
    }

    /** 全量扫描 26 个邻居：自身为 3×3×3 中心且周围全部是高级提纯构建方块则成型（仅缓存失效时执行）。
     *  旧提纯矩阵已停用：新式提纯矩阵由 {@link AkaishiPurifierMatrixControllerBlockEntity} 接管，恒不成型。 */
    private boolean scanMatrix() {
        return false;
    }

    /** 结构变化时由外壳方块触发：标记缓存失效，下次 tick 重扫 */
    public void markMatrixDirty() {
        matrixDirty = true;
    }

    /** 通知 3×3×3 范围内的提纯器中心重新校验结构（外壳方块放置/移除时调用） */
    public static void notifyNearbyCenters(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = pos.offset(dx, dy, dz);
                    if (level.getBlockEntity(p) instanceof AkaishiPurifierBlockEntity center) {
                        center.markMatrixDirty();
                    }
                }
            }
        }
    }

    /** 仅当结构缓存失效时重新校验提纯矩阵并同步成型状态（避免每 tick 26 次方块查询） */
    private void refreshMatrix() {
        if (!matrixDirty) {
            return;
        }
        matrixDirty = false;
        boolean formed = scanMatrix();
        if (formed == matrixFormed) {
            return;
        }
        matrixFormed = formed;
        BlockState blockState = level.getBlockState(worldPosition);
        if (formed != blockState.getValue(AkaishiPurifierBlock.FORMED)) {
            level.setBlock(worldPosition, blockState.setValue(AkaishiPurifierBlock.FORMED, formed), 3);
        }
        // 中心成型状态变化会影响所有邻近外壳，通知其重新检测
        AkaishiAdvancedPurifierBlockEntity.notifyNearbyShells(level, worldPosition);
        setChanged();
    }

    /** 是否具备提纯条件：输入有效 + 输出可容纳（能量检查在 tick 内做，不足时暂停而非清零） */
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

    /** 燃料能量值，非燃料返回 0（public 供 Menu 燃料槽放入校验） */
    public static int getFuelEnergy(ItemStack stack) {
        if (stack.is(ModItems.akaishiCrystal.get())) {
            return FUEL_CRYSTAL;
        }
        if (stack.is(ModBlocks.RAW_CHISHI_BLOCK.get().asItem())) {
            return FUEL_RAW_BLOCK;
        }
        return 0;
    }

    public Container inventory() {
        return inventory;
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

    /** 提纯器是纯消耗型机器：只接收管道输入的赤能源，不向外输出 */
    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    /** 允许管道向提纯器注入赤能源（可接入能量网络） */
    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public Component getDisplayName() {
        // 提纯矩阵成型时显示"高级提纯器"，未成型显示普通"赤石提纯器"
        return matrixFormed
                ? Component.translatable("gui.akaishi.purifier.matrix")
                : Component.translatable("block.akaishi.akaishi_purifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiPurifierMenu(id, inv, this);
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
        // 复用既有 energy 实例恢复，避免反复重建 storage 导致外部引用失效、能量不累积
        energy.setEnergy(tag.getLong("Energy"));
        burnTime = tag.getInt("BurnTime");
        burnTimeTotal = tag.getInt("BurnTimeTotal");
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
