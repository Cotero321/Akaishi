package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.api.item.IItemPipeDevice;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.LifeEnergyType;
import com.example.template.item.ModItems;
import com.example.template.life.organ.ChishiOrganItem;
import com.example.template.life.organ.QualityTier;
import com.example.template.life.sample.ChishiLifeSampleItem;
import com.example.template.menu.ChishiCultivatorMenu;
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
 * 部件培养舱方块实体（仅服务端驱动逻辑）。
 * 双模式（根据输入槽物品自动判定）：
 * - 提纯模式：纯度 <100 的生命样本 + 固态物 + 生命能量 → 纯度 +10（90% 成功，
 *   失败损失材料与能量但样本保留）。耗时受纯度修正（每 25 点纯度 -10% 时间）。
 * - 升级模式：品质 <IV 的器官 + 固态物 + 生命能量 → 品质 +1（100% 成功）。
 * 槽位：0=输入（样本或器官），1=材料（生命能量固态物）。
 */
public class ChishiCultivatorBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier {

    // ===== 提纯参数（按纯度区间 0/25/50/75 分档：越高越难，代价递增）=====
    /** 分档数据：{最低纯度, 成功率x100, 生命能量, 固态物, 耗时tick} */
    private static final int[][] PURIFY_TIERS = {
            {0, 90, 10_000, 1, 300},
            {25, 80, 20_000, 2, 600},
            {50, 70, 40_000, 4, 1200},
            {75, 60, 80_000, 8, 2400}
    };
    /** 单次提纯增加纯度 */
    public static final int PURIFY_GAIN = 10;

    // ===== 升级参数（按品质等级 I→II / II→III / III→IV：成功率下降，代价递增）=====
    /** 分档数据：{成功率x100, 生命能量, 固态物, 耗时tick} */
    private static final int[][] UPGRADE_TIERS = {
            {85, 20_000, 1, 600},
            {75, 80_000, 4, 1200},
            {65, 300_000, 16, 2400}
    };

    /** 生命能量缓冲容量（够 III→IV 一次消耗） */
    public static final long LIFE_CAPACITY = 500_000L;

    public static final int INPUT_SLOT = 0;
    public static final int SOLID_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    /** Menu 同步数据槽：0/1=生命能量/容量 2=进度% 3=模式 */
    public static final int DATA_SLOTS = 4;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_MODE = 3;
    public static final int MODE_PURIFY = 0;
    public static final int MODE_UPGRADE = 1;
    public static final int MODE_IDLE = 2;

    // ===== 分档查询（供 Menu/Screen 显示成功率与消耗） =====

    /** 纯度所在档位索引（0-25 → 0，75-100 → 3） */
    public static int purifyTierIndex(int purity) {
        return purity < 25 ? 0 : purity < 50 ? 1 : purity < 75 ? 2 : 3;
    }

    /** 提纯成功率（百分比） */
    public static int purifyRate(int purity) {
        return PURIFY_TIERS[purifyTierIndex(purity)][1];
    }

    /** 提纯能量消耗 */
    public static long purifyCost(int purity) {
        return PURIFY_TIERS[purifyTierIndex(purity)][2];
    }

    /** 提纯固态物消耗 */
    public static int purifySolid(int purity) {
        return PURIFY_TIERS[purifyTierIndex(purity)][3];
    }

    /** 提纯耗时（tick） */
    public static int purifyTicks(int purity) {
        return PURIFY_TIERS[purifyTierIndex(purity)][4];
    }

    /** 器官升级成功率（百分比，按当前品质） */
    public static int upgradeRate(QualityTier tier) {
        return UPGRADE_TIERS[tier.ordinal()][0];
    }

    /** 器官升级能量消耗 */
    public static long upgradeCost(QualityTier tier) {
        return UPGRADE_TIERS[tier.ordinal()][1];
    }

    /** 器官升级固态物消耗 */
    public static int upgradeSolid(QualityTier tier) {
        return UPGRADE_TIERS[tier.ordinal()][2];
    }

    /** 器官升级耗时（tick） */
    public static int upgradeTicks(QualityTier tier) {
        return UPGRADE_TIERS[tier.ordinal()][3];
    }

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final ChishiEnergyStorage life;
    private int progress;
    private int mode = MODE_IDLE;

    public ChishiCultivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_CULTIVATOR.get(), pos, state);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiCultivatorBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiCultivatorBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(0, (int) life.getEnergyStored());
        data.set(1, (int) life.getMaxEnergy());

        boolean changed = false;
        ItemStack input = inventory.getItem(INPUT_SLOT);

        // 提纯模式：纯度 <100 的样本（按纯度区间分档）
        if (input.getItem() instanceof ChishiLifeSampleItem sampleItem) {
            int purity = ChishiLifeSampleItem.getPurity(input);
            long cost = purifyCost(purity);
            int solid = purifySolid(purity);
            if (purity < 100 && hasSolid(solid) && life.getEnergyStored() >= cost) {
                if (mode != MODE_PURIFY) {
                    mode = MODE_PURIFY;
                    progress = 0;
                }
                progress++;
                if (progress >= purifyTicks(purity)) {
                    progress = 0;
                    // 无论成败都消耗固态物与生命能量；失败不吞样本
                    life.extractEnergy(cost, false);
                    inventory.getItem(SOLID_SLOT).shrink(solid);
                    if (level.random.nextInt(100) < purifyRate(purity)) {
                        ChishiLifeSampleItem.setPurity(input, Math.min(100, purity + PURIFY_GAIN));
                    }
                }
                changed = true;
            }
        }
        // 升级模式：品质 <IV 的器官（按品质等级分档）
        else if (input.getItem() instanceof ChishiOrganItem) {
            QualityTier tier = ChishiOrganItem.getTier(input);
            if (tier != null && tier.next() != null) {
                long cost = upgradeCost(tier);
                int solid = upgradeSolid(tier);
                if (hasSolid(solid) && life.getEnergyStored() >= cost) {
                    if (mode != MODE_UPGRADE) {
                        mode = MODE_UPGRADE;
                        progress = 0;
                    }
                    progress++;
                    if (progress >= upgradeTicks(tier)) {
                        progress = 0;
                        // 无论成败都消耗固态物与生命能量；失败不吞器官
                        life.extractEnergy(cost, false);
                        inventory.getItem(SOLID_SLOT).shrink(solid);
                        if (level.random.nextInt(100) < upgradeRate(tier)) {
                            ChishiOrganItem.setTier(input, tier.next());
                            // 培养升级同时提升适配度 +5（封顶 100），降低后续排异压力
                            ChishiOrganItem.addCompat(input, 5);
                        }
                    }
                    changed = true;
                }
            }
        }

        // 无可执行操作 → 复位
        if (!changed) {
            progress = 0;
            mode = MODE_IDLE;
        }
        data.set(2, progress * 100 / Math.max(1, currentTicks()));
        data.set(3, mode);
    }

    /** 当前模式总耗时（按分档） */
    private int currentTicks() {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input.getItem() instanceof ChishiLifeSampleItem) {
            return purifyTicks(ChishiLifeSampleItem.getPurity(input));
        }
        if (input.getItem() instanceof ChishiOrganItem) {
            QualityTier tier = ChishiOrganItem.getTier(input);
            if (tier != null && tier.next() != null) {
                return upgradeTicks(tier);
            }
        }
        return 1;
    }

    private boolean hasSolid(int count) {
        ItemStack solid = inventory.getItem(SOLID_SLOT);
        return solid.is(ModItems.chishiLifeEssenceSolid.get()) && solid.getCount() >= count;
    }

    public Container inventory() {
        return inventory;
    }

    public ContainerData data() {
        return data;
    }

    // ===== Container：漏斗 / 物品管道直接读写 =====

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
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
    }

    // ===== IItemPipeDevice：输入槽收样本/器官，材料槽收固态物 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{INPUT_SLOT, SOLID_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{INPUT_SLOT};
    }

    // ===== IEnergyProvider：仅生命能量输入 =====

    @Override
    public IEnergyStorage getEnergyStorage() {
        return life;
    }

    @Override
    public IEnergyStorage getEnergyStorage(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE ? life : null;
    }

    @Override
    public boolean canInputEnergy() {
        return true;
    }

    @Override
    public boolean canOutputEnergy() {
        return false;
    }

    @Override
    public boolean canInputEnergy(IEnergyType type) {
        return type == LifeEnergyType.INSTANCE;
    }

    @Override
    public boolean canOutputEnergy(IEnergyType type) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_cultivator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiCultivatorMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("LifeEnergy", life.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("Mode", mode);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, inventory.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        life.setEnergy(tag.getLong("LifeEnergy"));
        progress = tag.getInt("Progress");
        mode = tag.getInt("Mode");
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
