package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.block.AkaishiMinerControllerBlock;
import com.example.akaishi.block.AkaishiMinerTier;
import com.example.akaishi.block.AkaishiMinerUpgradeBlock;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiMinerControllerMenu;
import com.example.akaishi.multiblock.AkaishiMinerStructure;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 赤石矿机控制器（核心方块，4 级共用）：固定 9×9×3 结构的主方块。
 * 结构成型后消耗赤能源持续挖矿：进度满时按概率表随机产出原版矿物，
 * 产物先入暂存槽再推送给转口；升级模块（速度/时运/储能方块）安装在升级框架位置上，控制器扫描生效。
 * 数据槽：0=能量 1=容量 2=进度 3=总耗时 4=成型 5=速度升级 6=时运升级 7=储能升级。
 */
public class AkaishiMinerControllerBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IEnergyProvider, Container, IDataCarrier {

    /** 产物暂存槽数（等待推送给转口） */
    public static final int OUTPUT_SLOTS = 6;
    public static final int DATA_COUNT = 8;
    public static final int DATA_ENERGY = 0, DATA_CAPACITY = 1, DATA_PROGRESS = 2, DATA_REQUIRED = 3,
            DATA_FORMED = 4, DATA_SPEED = 5, DATA_FORTUNE = 6, DATA_STORAGE = 7;
    /** 每类升级模块生效上限（超出部分不再叠加）：速度 10 / 时运 4 / 储能 10 */
    public static final int SPEED_MAX = 10;
    public static final int FORTUNE_MAX = 4;
    public static final int STORAGE_MAX = 10;
    /** 速度升级：每级 +12.5% 挖矿速率 */
    public static final double SPEED_STEP = 0.125;
    /** 速度升级：每级 +10% 能耗 */
    public static final double COST_STEP = 0.10;
    /** 储能升级：每级 +50% 能量容量 */
    public static final double STORAGE_STEP = 0.50;
    /** 时运升级：每级产物数量 +1 */
    public static final int FORTUNE_STEP = 1;

    // 矿物概率表（权重越高越常见）；索引 >= RARE_START 的稀有矿物受等级倍率影响
    private static final Item[] MINERAL_ITEMS = {
            Items.COAL, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD, Items.REDSTONE,
            Items.LAPIS_LAZULI, Items.QUARTZ, Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP
    };
    private static final int[] MINERAL_WEIGHTS = {30, 24, 22, 12, 8, 6, 6, 3, 2, 1};
    private static final int RARE_START = 7;

    private final SimpleContainer inventory;
    private final SimpleContainerData data = new SimpleContainerData(DATA_COUNT);
    private final AkaishiEnergyStorage energy;
    private int progress;
    private float speedAccum;
    private int structureTick;
    /** 升级统计刷新节流计数（每 20 tick 刷新一次，升级件变化不频繁） */
    private int upgradeTick;
    private List<BlockPos> upgradeFrames = List.of();
    private List<BlockPos> ports = List.of();
    private BlockPos lastMin, lastMax;
    private int speedCount, fortuneCount, storageCount;

    public AkaishiMinerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINER_CONTROLLER.get(), pos, state);
        this.energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, tier().maxEnergy);
        this.inventory = new SimpleContainer(OUTPUT_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiMinerControllerBlockEntity.this.setChanged();
            }
        };
    }

    /** 矿机等级（由核心方块实例决定） */
    public AkaishiMinerTier tier() {
        Block b = getBlockState().getBlock();
        return b instanceof AkaishiMinerControllerBlock c ? c.tier() : AkaishiMinerTier.BASIC;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiMinerControllerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        boolean changed = false;
        AkaishiMinerTier t = tier();
        energy.setMaxEnergy((long) (t.maxEnergy * (1.0 + STORAGE_STEP * storageCount)));
        data.set(DATA_ENERGY, (int) energy.getEnergyStored());
        data.set(DATA_CAPACITY, (int) energy.getMaxEnergy());
        data.set(DATA_PROGRESS, progress);
        data.set(DATA_REQUIRED, ModConfig.minerTicksBase);

        // 结构校验：未成型每 tick，成型后每 10 tick（结构变化不频繁）
        boolean formed = getBlockState().getValue(AkaishiMinerControllerBlock.FORMED);
        boolean checked = !formed || ++structureTick % 10 == 0;
        AkaishiMinerStructure.Result scan = checked ? AkaishiMinerStructure.scan(level, worldPosition) : null;
        boolean valid = checked ? scan != null : formed;
        if (formed != valid) {
            setFormed(valid, scan);
            formed = valid;
            changed = true;
        } else if (checked && valid) {
            // 已成型且本次扫描有效：刷新缓存（BE 重载后 upgradeFrames/ports 为空，
            // 不刷新会导致升级统计归零、产物不推送转口）
            upgradeFrames = scan.upgradeFrames();
            ports = scan.ports();
            refreshUpgrades();
            for (BlockPos pp : ports) {
                if (level.getBlockEntity(pp) instanceof AkaishiMinerPortBlockEntity port) {
                    port.setControllerPos(worldPosition);
                }
            }
        }
        data.set(DATA_FORMED, formed ? 1 : 0);
        if (!formed) {
            // 结构解散：升级统计清零，避免 GUI 残留上一次成型数据
            data.set(DATA_SPEED, 0);
            data.set(DATA_FORTUNE, 0);
            data.set(DATA_STORAGE, 0);
        }

        if (formed) {
            if (++upgradeTick % 20 == 0) {
                refreshUpgrades();
            }
            data.set(DATA_SPEED, speedCount);
            data.set(DATA_FORTUNE, fortuneCount);
            data.set(DATA_STORAGE, storageCount);
            // 先把暂存槽产物推向转口，为下批产出腾空间（爆仓保护依赖此步每 tick 执行）
            pushToPorts();
            // 爆仓保护：进度攒满但暂存/转口均无空位时停机不耗能，待清出空间后再结算产出
            if (progress >= ModConfig.minerTicksBase) {
                // 只有真正结算成功（产物落槽、进度归零）才标脏存档；爆仓未结算无状态变化不标脏
                changed |= settleOre(t);
            } else {
                // 消耗能量推进挖矿进度（速度升级加速、也提能耗；能量不足则停机）
                long cost = (long) (ModConfig.minerCostPerTickBase * (1.0 + COST_STEP * speedCount));
                if (energy.getEnergyStored() >= cost) {
                    energy.extractEnergy(cost, false);
                    speedAccum += t.rateMultiplier * (1.0 + SPEED_STEP * speedCount);
                    int delta = (int) speedAccum;
                    if (delta > 0) {
                        speedAccum -= delta;
                        progress += delta;
                    }
                    changed = true;
                }
            }
        }
        if (changed) {
            setChanged();
        }
    }

    /** 汇总结构内已安装的升级模块方块（每类上限：速度 10 / 时运 4 / 储能 10） */
    private void refreshUpgrades() {
        int s = 0, f = 0, st = 0;
        for (BlockPos p : upgradeFrames) {
            Block b = level.getBlockState(p).getBlock();
            if (b instanceof AkaishiMinerUpgradeBlock up) {
                switch (up.type()) {
                    case SPEED -> s++;
                    case FORTUNE -> f++;
                    case STORAGE -> st++;
                }
            }
        }
        speedCount = Math.min(s, SPEED_MAX);
        fortuneCount = Math.min(f, FORTUNE_MAX);
        storageCount = Math.min(st, STORAGE_MAX);
    }

    /** 切换结构状态：同步自身 FORMED 标记，建立/解除转口关联，刷新升级缓存 */
    private void setFormed(boolean formed, AkaishiMinerStructure.Result scan) {
        level.setBlock(worldPosition, getBlockState().setValue(AkaishiMinerControllerBlock.FORMED, formed), 3);
        BlockPos min, max;
        if (formed) {
            upgradeFrames = scan.upgradeFrames();
            ports = scan.ports();
            lastMin = AkaishiMinerStructure.minPos(worldPosition);
            lastMax = AkaishiMinerStructure.maxPos(worldPosition);
            min = lastMin;
            max = lastMax;
            refreshUpgrades();
        } else {
            upgradeFrames = List.of();
            ports = List.of();
            speedCount = fortuneCount = storageCount = 0;
            min = lastMin;
            max = lastMax;
        }
        if (min == null || max == null) {
            return;
        }
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof AkaishiMinerPortBlockEntity port) {
                port.setControllerPos(formed ? worldPosition : null);
            }
        }
    }

    /**
     * 结算一次采矿产出：按概率表随机产出（数量 = 1 + 时运升级数）并放入暂存槽。
     * 优先合并到同种矿物格，其次空槽；全部放不下（爆仓）返回 false，进度保留等待清仓后再结算。
     */
    private boolean settleOre(AkaishiMinerTier t) {
        Item ore = rollMineral(level.random, t);
        int qty = Math.min(1 + FORTUNE_STEP * fortuneCount, ore.getMaxStackSize());
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack s = inventory.getItem(i);
            if (s.is(ore) && s.getCount() + qty <= s.getMaxStackSize()) {
                s.grow(qty);
                progress = 0;
                speedAccum = 0;
                return true;
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, new ItemStack(ore, qty));
                progress = 0;
                speedAccum = 0;
                return true;
            }
        }
        return false;
    }

    /** 把暂存槽产物推送给转口（部分合并：同种槽塞满为止，剩余留待下 tick） */
    private void pushToPorts() {
        if (ports.isEmpty()) {
            return;
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (BlockPos pp : ports) {
                if (stack.isEmpty()) {
                    break;
                }
                BlockEntity be = level.getBlockEntity(pp);
                if (be instanceof AkaishiMinerPortBlockEntity port) {
                    stack = port.receivePartial(stack);
                }
            }
            inventory.setItem(i, stack);
        }
    }

    /** 随机矿物：稀有矿物（钻石/绿宝石/下界合金碎片）权重按等级倍率放大 */
    public static Item rollMineral(RandomSource random, AkaishiMinerTier tier) {
        int total = 0;
        for (int i = 0; i < MINERAL_ITEMS.length; i++) {
            total += mineralWeight(i, tier);
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < MINERAL_ITEMS.length; i++) {
            roll -= mineralWeight(i, tier);
            if (roll < 0) {
                return MINERAL_ITEMS[i];
            }
        }
        return Items.COAL;
    }

    private static int mineralWeight(int index, AkaishiMinerTier tier) {
        int w = MINERAL_WEIGHTS[index];
        if (index >= RARE_START) {
            w = (int) Math.round(w * tier.rareMultiplier);
        }
        return Math.max(1, w);
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

    public long getEnergyCapacity() {
        return energy.getMaxEnergy();
    }

    public long getEnergyStored() {
        return energy.getEnergyStored();
    }

    public void addEnergy(long amount) {
        energy.addEnergy(amount, false);
    }

    /** 结构是否成型（转口用） */
    public boolean isFormed() {
        return getBlockState().getValue(AkaishiMinerControllerBlock.FORMED);
    }

    public int getSpeedCount() {
        return speedCount;
    }

    public int getFortuneCount() {
        return fortuneCount;
    }

    public int getStorageCount() {
        return storageCount;
    }

    // ===== Container（产物暂存槽） =====

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

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return false; // 产物暂存只读，仅挖矿产出可写入（防漏斗/玩家塞入）
    }

    // ===== IEnergyProvider：只接收赤能源（由转口/能量管道注入） =====

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
        return Component.translatable("block.akaishi.akaishi_miner_controller_" + tier().suffix);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiMinerControllerMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    /** 挖掘保留数据：排除随方块掉落的产物暂存 */
    @Override
    public String[] excludedKeys() {
        return new String[]{"Items"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        NonNullList<ItemStack> items = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        progress = tag.getInt("Progress");
        NonNullList<ItemStack> items = NonNullList.withSize(OUTPUT_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            inventory.setItem(i, items.get(i));
        }
        if (progress >= ModConfig.minerTicksBase) {
            progress = 0;
        }
    }
}
