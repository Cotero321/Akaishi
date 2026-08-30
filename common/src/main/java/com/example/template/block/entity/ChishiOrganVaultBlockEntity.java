package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;
import com.example.template.api.energy.IEnergyProvider;
import com.example.template.api.energy.IEnergyStorage;
import com.example.template.api.energy.IEnergyType;
import com.example.template.api.item.IItemPipeDevice;
import com.example.template.api.storage.IStorageVault;
import com.example.template.energy.ChishiEnergyStorage;
import com.example.template.energy.LifeEnergyType;
import com.example.template.life.body.BodySlot;
import com.example.template.life.organ.ChishiOrganItem;
import com.example.template.menu.ChishiOrganVaultMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
 * 器官储藏库（生物冻库）：按躯体 9 槽位分页的大容量器官仓库。
 * - 存储：9 页 × 9 格 = 81 格（按 BodySlot 分页）+ 9 格暂存区（管道输出缓冲），共 90 格
 * - 归类：canPlaceItem 限定器官只能放入所属槽位页——管道/漏斗/手动放置自动"归类"
 * - 维持：每 tick 消耗 1 生命能量维持器官活性（能量耗尽界面警示，不阻塞存取）
 * - 管道：输入需匹配所属页（canPlaceItem 校验），输出从暂存区抽取
 */
public class ChishiOrganVaultBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IStorageVault, IDataCarrier {

    /** 躯体槽位页数（= 器官槽位数） */
    public static final int PAGE_COUNT = BodySlot.values().length;
    /** 每页格数 */
    public static final int PER_PAGE = 9;
    /** 页槽起始（0） */
    public static final int PAGE_START = 0;
    /** 暂存区起始索引 */
    public static final int TEMP_START = PAGE_COUNT * PER_PAGE;
    /** 暂存区大小 */
    public static final int TEMP_SIZE = 9;
    /** 总槽位数（81 + 9 = 90） */
    public static final int SLOT_COUNT = TEMP_START + TEMP_SIZE;

    /** 生命能量缓冲容量 */
    public static final long LIFE_CAPACITY = 100_000L;
    /** 每 tick 活性维持消耗 */
    public static final long KEEP_COST_PER_TICK = 1L;

    /** Menu 同步数据：0/1=生命能量/容量 2=活性状态（1 活性 0 休眠） */
    public static final int DATA_SLOTS = 3;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_ACTIVE = 2;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final ChishiEnergyStorage life;

    public ChishiOrganVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_ORGAN_VAULT.get(), pos, state);
        this.life = new ChishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiOrganVaultBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiOrganVaultBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 活性维持：每 tick 消耗 1 生命能量
        long stored = life.getEnergyStored();
        if (stored > 0) {
            life.extractEnergy(KEEP_COST_PER_TICK, false);
        }
        data.set(DATA_ENERGY, (int) stored);
        data.set(DATA_CAPACITY, (int) life.getMaxEnergy());
        data.set(DATA_ACTIVE, stored > 0 ? 1 : 0);
        setChanged();
    }

    // ===== 归类（放置校验）=====

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (!(stack.getItem() instanceof ChishiOrganItem organ)) {
            return false;
        }
        // 暂存区：任意器官（输出缓冲）
        if (index >= TEMP_START) {
            return true;
        }
        // 页槽：器官必须与所属槽位页匹配（管道/漏斗/手动放置自动归类）
        int page = index / PER_PAGE;
        return organ.slot.ordinal() == page;
    }

    /** 页槽访问（Menu 页映射）：page ∈ [0, PAGE_COUNT)，indexInPage ∈ [0, PER_PAGE) */
    public ItemStack getItemAt(int page, int indexInPage) {
        return inventory.getItem(page * PER_PAGE + indexInPage);
    }

    /** 页槽写入（Menu 页映射） */
    public void setItemAt(int page, int indexInPage, ItemStack stack) {
        inventory.setItem(page * PER_PAGE + indexInPage, stack);
    }

    /** 暂存槽访问 */
    public ItemStack getTempItem(int index) {
        return inventory.getItem(TEMP_START + index);
    }

    public void setTempItem(int index, ItemStack stack) {
        inventory.setItem(TEMP_START + index, stack);
    }

    // ===== 界面 =====

    public ContainerData data() {
        return data;
    }

    // ===== Container：管道 / 漏斗按索引读写 =====

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

    // ===== IItemPipeDevice：输入需匹配所属页（canPlaceItem 保证），输出走暂存区 =====

    @Override
    public int[] getPipeInputSlots() {
        return allPageSlots();
    }

    @Override
    public int[] getPipeOutputSlots() {
        int[] slots = new int[TEMP_SIZE];
        for (int i = 0; i < TEMP_SIZE; i++) {
            slots[i] = TEMP_START + i;
        }
        return slots;
    }

    /** 管道输入槽 = 全部页槽（canPlaceItem 保证只接受匹配槽位的器官） */
    private int[] allPageSlots() {
        int[] slots = new int[TEMP_START];
        for (int i = 0; i < TEMP_START; i++) {
            slots[i] = i;
        }
        return slots;
    }

    // ===== IEnergyProvider：仅生命能量输入（维持活性） =====

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
    public String getVaultNameKey() {
        return "block.template_mod.chishi_organ_vault";
    }

    @Override
    public Container getVaultContainer() {
        return inventory;
    }

    // ===== 界面工厂 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_organ_vault");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiOrganVaultMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== NBT =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("LifeEnergy", life.getEnergyStored());
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
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
    }
}
