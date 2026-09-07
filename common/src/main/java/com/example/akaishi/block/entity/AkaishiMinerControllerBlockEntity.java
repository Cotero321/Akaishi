package com.example.akaishi.block.entity;

import com.example.akaishi.AkaishiMod;
import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.IMinerPortDevice;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.item.IMinerOutputSink;
import com.example.akaishi.api.item.IItemPipeDevice;
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
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 赤石矿机控制器（核心方块，4 级共用）：固定 9×9×5 结构的主方块（控制器位于中间层中心）。
 * 结构成型后消耗赤能源持续挖矿：进度满时按概率表随机产出原矿物，
 * 产物先入暂存槽再推送给结构端口（顶层中心转口 / 立柱物品输出口等 IMinerOutputSink）；
 * 能量由端口（转口/能量输入口）各自限量转发注入；升级模块（速度/时运/储能方块）
 * 安装在升级框架位置上，控制器扫描生效。
 * 数据槽：0=能量 1=容量 2=进度 3=总耗时 4=成型 5=速度升级 6=时运升级 7=储能升级 8=挖矿模式（0 正常 / 1 精准）。
 */
public class AkaishiMinerControllerBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IEnergyProvider, Container, IItemPipeDevice, IDataCarrier {

    /** 产物暂存槽数（等待推送给转口） */
    public static final int OUTPUT_SLOTS = 6;
    public static final int DATA_COUNT = 9;
    public static final int DATA_ENERGY = 0, DATA_CAPACITY = 1, DATA_PROGRESS = 2, DATA_REQUIRED = 3,
            DATA_FORMED = 4, DATA_SPEED = 5, DATA_FORTUNE = 6, DATA_STORAGE = 7, DATA_MODE = 8;
    /** 速度升级：每级 +12.5% 挖矿速率 */
    public static final double SPEED_STEP = 0.125;
    /** 速度升级：每级 +10% 能耗 */
    public static final double COST_STEP = 0.10;
    /** 储能升级：每级 +50% 能量容量 */
    public static final double STORAGE_STEP = 0.50;
    /** 时运升级：每级产物数量 +1 */
    public static final int FORTUNE_STEP = 1;
    /** 物品标签 #akaishi:miner/minerals：矿机可挖矿物的扩展入口（默认十种 + 第三方打标矿物） */
    public static final TagKey<Item> TAG_MINER_MINERALS = TagKey.create(Registries.ITEM,
            new ResourceLocation(AkaishiMod.MOD_ID, "miner/minerals"));

    /**
     * 产出池单条目：正常模式掉落物 drop + 精准模式原矿石方块 silk（可空，空则精准仍给 drop）+ 权重。
     * rare 为 true 时权重随矿机等级倍率放大（钻石/绿宝石/下界合金碎片等）。
     */
    public record OreEntry(Item drop, Item silk, int weight, boolean rare) {
    }

    // 默认产出池（十种原版矿物；权重越高越常见）
    private static final Item[] MINERAL_ITEMS = {
            Items.COAL, Items.RAW_IRON, Items.RAW_COPPER, Items.RAW_GOLD, Items.REDSTONE,
            Items.LAPIS_LAZULI, Items.QUARTZ, Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP
    };
    /** 与 MINERAL_ITEMS 一一对应的原矿石方块（精准模式挖到的矿石本体） */
    private static final Item[] ORE_BLOCK_ITEMS = {
            Blocks.COAL_ORE.asItem(), Blocks.IRON_ORE.asItem(), Blocks.COPPER_ORE.asItem(),
            Blocks.GOLD_ORE.asItem(), Blocks.REDSTONE_ORE.asItem(), Blocks.LAPIS_ORE.asItem(),
            Blocks.NETHER_QUARTZ_ORE.asItem(), Blocks.DIAMOND_ORE.asItem(), Blocks.EMERALD_ORE.asItem(),
            Blocks.ANCIENT_DEBRIS.asItem()
    };
    private static final int[] MINERAL_WEIGHTS = {30, 24, 22, 12, 8, 6, 6, 3, 2, 1};
    private static final int RARE_START = 7;

    /** 静态默认池（运行时把 #akaishi:miner/minerals 标签内多出的第三方矿物追加进实例池） */
    private static final List<OreEntry> DEFAULT_POOL = buildDefaultPool();

    private static List<OreEntry> buildDefaultPool() {
        List<OreEntry> pool = new ArrayList<>(MINERAL_ITEMS.length);
        for (int i = 0; i < MINERAL_ITEMS.length; i++) {
            pool.add(new OreEntry(MINERAL_ITEMS[i], ORE_BLOCK_ITEMS[i], MINERAL_WEIGHTS[i], i >= RARE_START));
        }
        return List.copyOf(pool);
    }

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
    /** 挖矿模式：true=精准（产出原矿石方块、时运折算）；false=正常（产出矿物，时运全额） */
    private boolean preciseMode;
    /** 当前生效产出池（默认池 + 标签扩展矿物） */
    private List<OreEntry> orePool = DEFAULT_POOL;
    /** 产出池刷新节流计数（每 100 tick 重扫一次矿物标签，成本极低） */
    private int orePoolTick;

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
        // 模式槽同步 + 产出池定期重扫（首个 tick 先扫一次，之后每 100 tick；矿物标签增删即时接入）
        data.set(DATA_MODE, preciseMode ? 1 : 0);
        if (orePoolTick++ % 100 == 0) {
            refreshOrePool();
        }

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
                if (level.getBlockEntity(pp) instanceof IMinerPortDevice port) {
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
                // 消耗能量推进挖矿进度（速度升级加速、也提能耗；能量不足则停机）。
                // 配置 [machine]：workSpeed 全局加速，costMultiplier 放大运行耗能
                long cost = (long) (ModConfig.minerCostPerTickBase * (1.0 + COST_STEP * speedCount)
                        * ModConfig.machineCostMultiplier);
                if (energy.getEnergyStored() >= cost) {
                    energy.extractEnergy(cost, false);
                    speedAccum += t.rateMultiplier * (1.0 + SPEED_STEP * speedCount) * ModConfig.machineWorkSpeed;
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

    /**
     * 汇总结构内已安装的升级模块方块。生效上限按矿机等级解锁：
     * 效率 = 该等级 maxSpeedUpgrades（8/16/24/32）；储能与时运 = 效率上限的四分之一（2/4/6/8）。
     */
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
        int speedCap = tier().maxSpeedUpgrades;
        int supportCap = speedCap / 4; // 储能与时运共用该上限
        speedCount = Math.min(s, speedCap);
        fortuneCount = Math.min(f, supportCap);
        storageCount = Math.min(st, supportCap);
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
            if (be instanceof IMinerPortDevice port) {
                port.setControllerPos(formed ? worldPosition : null);
            }
        }
    }

    /**
     * 结算一次采矿产出：按概率表随机抽一个产出条目并放入暂存槽。
     * 正常模式产出矿物掉落物（数量 = 1 + 时运升级数）；
     * 精准模式产出原矿石方块（条目未登记方块则回退掉落物），时运升级按配置除数折算后生效。
     * 优先合并到同种槽，其次空槽；全部放不下（爆仓）返回 false，进度保留等待清仓后再结算。
     */
    private boolean settleOre(AkaishiMinerTier t) {
        OreEntry entry = rollOre(level.random, t);
        boolean precise = preciseMode;
        Item out = precise && entry.silk() != null ? entry.silk() : entry.drop();
        int fortune = precise ? effectiveFortune() : fortuneCount;
        int qty = Math.min(1 + FORTUNE_STEP * fortune, out.getMaxStackSize());
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            ItemStack s = inventory.getItem(i);
            if (s.is(out) && s.getCount() + qty <= s.getMaxStackSize()) {
                s.grow(qty);
                progress = 0;
                speedAccum = 0;
                return true;
            }
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, new ItemStack(out, qty));
                progress = 0;
                speedAccum = 0;
                return true;
            }
        }
        return false;
    }

    /** 精准模式生效的时运升级数 = 时运升级数 ÷ 配置除数（向下取整） */
    private int effectiveFortune() {
        return fortuneCount / Math.max(1, ModConfig.minerPreciseFortuneDivisor);
    }

    /** 从当前产出池按权重随机抽一个条目（稀有条目权重随矿机等级倍率放大） */
    private OreEntry rollOre(RandomSource random, AkaishiMinerTier tier) {
        int total = 0;
        for (OreEntry e : orePool) {
            total += weightOf(e, tier);
        }
        int roll = random.nextInt(Math.max(1, total));
        for (OreEntry e : orePool) {
            roll -= weightOf(e, tier);
            if (roll < 0) {
                return e;
            }
        }
        return DEFAULT_POOL.get(0);
    }

    private static int weightOf(OreEntry e, AkaishiMinerTier tier) {
        int w = e.weight();
        if (e.rare()) {
            w = (int) Math.round(w * tier.rareMultiplier);
        }
        return Math.max(1, w);
    }

    /**
     * 重扫矿物标签：把 #akaishi:miner/minerals 中多出的第三方矿物追加进产出池。
     * 扩展矿物权重取配置（0 = 关闭标签扩展，只挖默认十种）；未登记原矿石方块的条目精准模式仍给掉落物。
     */
    private void refreshOrePool() {
        List<OreEntry> pool = new ArrayList<>(DEFAULT_POOL);
        if (ModConfig.minerExtraOreWeight > 0) {
            Set<Item> known = new HashSet<>();
            for (OreEntry e : DEFAULT_POOL) {
                known.add(e.drop());
            }
            Registry<Item> registry = level.registryAccess().registryOrThrow(Registries.ITEM);
            for (Holder<Item> holder : registry.getTagOrEmpty(TAG_MINER_MINERALS)) {
                Item it = holder.value();
                if (known.add(it)) {
                    pool.add(new OreEntry(it, null, ModConfig.minerExtraOreWeight, false));
                }
            }
        }
        orePool = List.copyOf(pool);
    }

    /** 当前是否精准模式（GUI 高亮当前模式按钮用） */
    public boolean isPreciseMode() {
        return preciseMode;
    }

    /** 服务端模式切换（GUI 按钮 → clickMenuButton → 本方法） */
    public void setPreciseMode(boolean precise) {
        if (level != null && level.isClientSide) {
            return; // 仅服务端可切换，防客户端误调
        }
        if (preciseMode != precise) {
            preciseMode = precise;
            setChanged();
        }
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
                if (be instanceof IMinerOutputSink sink) {
                    stack = sink.receivePartial(stack);
                }
            }
            inventory.setItem(i, stack);
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

    // ===== IItemPipeDevice：产物暂存为仅输出缓冲（第三方物流/自家管道只可抽取） =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[0]; // 不可被第三方插入（仅挖矿产出可写入）
    }

    @Override
    public int[] getPipeOutputSlots() {
        int[] slots = new int[OUTPUT_SLOTS];
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            slots[i] = i;
        }
        return slots;
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
        tag.putBoolean("PreciseMode", preciseMode);
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
        preciseMode = tag.getBoolean("PreciseMode");
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
