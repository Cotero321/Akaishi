package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.energy.IEnergyType;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.body.PlayerBodySync;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.menu.AkaishiSurgeryMenu;
import com.example.akaishi.upgrade.IUpgradeableMachine;
import com.example.akaishi.upgrade.MachineUpgradeSlots;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 手术仓方块实体（仅服务端驱动逻辑）：
 * - 移植：目标槽位须为空，器官放入输入槽，消耗 3 固态 + 20K 生命能量，耗时 4 秒
 * - 摘除：目标槽位须已占用，消耗 1 固态 + 5K 生命能量，耗时 4 秒，器官回背包
 * - 手术期间玩家必须保持界面打开，关闭/离开/资源不足即中断
 * - 摘除承受 {@link PlayerBodyState} 的无视护甲伤害，移植按器官品质写入初始排斥
 * 槽位：0=器官输入，1=固态物。
 */
public class AkaishiSurgeryBlockEntity extends BlockEntity implements
        ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier, IUpgradeableMachine {

    /** 移植手术消耗：3 固态 + 20K 生命能量 */
    public static final int IMPLANT_SOLID_COST = 3;
    public static final long IMPLANT_LIFE_COST = 20_000L;
    /** 摘除手术消耗：1 固态 + 5K 生命能量 */
    public static final int EXTRACT_SOLID_COST = 1;
    public static final long EXTRACT_LIFE_COST = 5_000L;
    /** 生命能量缓冲容量（够 5 次移植） */
    public static final long LIFE_CAPACITY = 100_000L;
    /** 单次手术耗时（tick） */
    public static final int PROGRESS_TICKS = 80;

    public static final int ORGAN_SLOT = 0;
    public static final int SOLID_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    /** Menu 同步数据槽：0/1=生命能量/容量 2=进度% 3=操作类型 4=目标槽位索引 5=固态数量 */
    public static final int DATA_SLOTS = 6;
    public static final int DATA_ENERGY = 0;
    public static final int DATA_CAPACITY = 1;
    public static final int DATA_PROGRESS = 2;
    public static final int DATA_OPERATION = 3;
    public static final int DATA_TARGET = 4;
    public static final int DATA_SOLID = 5;

    /** 操作类型 */
    public static final int OP_NONE = 0;
    public static final int OP_IMPLANT = 1;
    public static final int OP_EXTRACT = 2;

    private final SimpleContainer inventory;
    private final SimpleContainerData data;
    private final AkaishiEnergyStorage life;
    /** 机器升级槽（速度/能量各一格，单格堆叠 8 封顶） */
    private final MachineUpgradeSlots upgradeSlots = new MachineUpgradeSlots();
    private int progress;
    /** 速度升级小数余量（避免 (int) 截断使 1~7 级升级无效） */
    private float speedAccum;
    private int operationType;
    private int targetSlot;
    private UUID operatingPlayer;

    public AkaishiSurgeryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_SURGERY.get(), pos, state);
        this.life = new AkaishiEnergyStorage(LifeEnergyType.INSTANCE, LIFE_CAPACITY);
        this.upgradeSlots.setOnChange(this::setChanged);
        this.inventory = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiSurgeryBlockEntity.this.setChanged();
            }
        };
        this.data = new SimpleContainerData(DATA_SLOTS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiSurgeryBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 动态扩容：能量升级组件生效时按倍率提升生命能量上限
        life.setMaxEnergy((long) (LIFE_CAPACITY * getEnergyCapacityMultiplier()));
        data.set(DATA_ENERGY, (int) life.getEnergyStored());
        data.set(DATA_CAPACITY, (int) life.getMaxEnergy());
        data.set(DATA_SOLID, inventory.getItem(SOLID_SLOT).getCount());

        if (operationType != OP_NONE) {
            Player player = getOperatingPlayer();
            // 可打断：玩家关闭界面 / 死亡 / 资源不足 → 取消手术
            if (player == null || !isPlayerStillInMenu(player)
                    || !hasResources(requiredSolid(), requiredLife())) {
                resetOperation();
            } else {
                // 速度升级：每 tick 进度按倍率累加（小数余量累积避免截断）
                speedAccum += getSpeedMultiplier();
                int delta = (int) speedAccum;
                if (delta > 0) {
                    speedAccum -= delta;
                    progress += delta;
                }
                if (progress >= PROGRESS_TICKS) {
                    completeOperation();
                }
            }
        } else {
            progress = 0;
            speedAccum = 0;
        }
        data.set(DATA_PROGRESS, progress * 100 / PROGRESS_TICKS);
        data.set(DATA_OPERATION, operationType);
        data.set(DATA_TARGET, targetSlot);
        setChanged();
    }

    /** 玩家仍在本手术台的界面中 */
    private boolean isPlayerStillInMenu(Player player) {
        return !player.isDeadOrDying()
                && player.containerMenu instanceof AkaishiSurgeryMenu menu
                && menu.getBlockPos() != null
                && menu.getBlockPos().equals(worldPosition);
    }

    /** 开始手术（C2S 包入口，服务端做全部校验），不满足条件直接拒绝 */
    public void startOperation(Player player, int type, int slotIndex) {
        if (operationType != OP_NONE || level == null || level.isClientSide) {
            return;
        }
        int index = Math.max(0, Math.min(BodySlot.values().length - 1, slotIndex));
        BodySlot target = BodySlot.values()[index];
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return;
        }
        if (type == OP_IMPLANT) {
            ItemStack organ = inventory.getItem(ORGAN_SLOT);
            if (!(organ.getItem() instanceof AkaishiOrganItem item) || item.slot != target) {
                return;
            }
            if (state.isOccupied(target)) {
                return;
            }
            if (!hasResources(IMPLANT_SOLID_COST, IMPLANT_LIFE_COST)) {
                return;
            }
        } else if (type == OP_EXTRACT) {
            if (!state.isOccupied(target)) {
                return;
            }
            if (!hasResources(EXTRACT_SOLID_COST, EXTRACT_LIFE_COST)) {
                return;
            }
        } else {
            return;
        }
        operationType = type;
        targetSlot = index;
        operatingPlayer = player.getUUID();
        progress = 0;
        speedAccum = 0;
        setChanged();
    }

    private void completeOperation() {
        Player player = getOperatingPlayer();
        IPlayerBodyState state = player != null ? PlayerBodyHelper.of(player) : null;
        if (player == null || state == null) {
            resetOperation();
            return;
        }
        BodySlot target = BodySlot.values()[Math.max(0, Math.min(BodySlot.values().length - 1, targetSlot))];
        if (operationType == OP_IMPLANT) {
            ItemStack organ = inventory.getItem(ORGAN_SLOT);
            if (organ.getItem() instanceof AkaishiOrganItem item && item.slot == target
                    && !state.isOccupied(target) && hasResources(IMPLANT_SOLID_COST, IMPLANT_LIFE_COST)) {
                consume(IMPLANT_SOLID_COST, IMPLANT_LIFE_COST);
                state.implantOrgan(target, organ);
                inventory.setItem(ORGAN_SLOT, ItemStack.EMPTY);
            }
        } else if (operationType == OP_EXTRACT) {
            if (state.isOccupied(target) && hasResources(EXTRACT_SOLID_COST, EXTRACT_LIFE_COST)) {
                consume(EXTRACT_SOLID_COST, EXTRACT_LIFE_COST);
                ItemStack removed = state.extractOrgan(player, target);
                if (!removed.isEmpty() && !player.getInventory().add(removed)) {
                    dropItem(removed);
                }
            }
        }
        // 手术结果推送到客户端刷新界面
        if (player instanceof ServerPlayer sp && state != null) {
            PlayerBodySync.sendToPlayer(sp, state);
        }
        resetOperation();
    }

    private void resetOperation() {
        operationType = OP_NONE;
        targetSlot = 0;
        progress = 0;
        speedAccum = 0;
        operatingPlayer = null;
    }

    // ===== 资源 =====

    private int requiredSolid() {
        return operationType == OP_IMPLANT ? IMPLANT_SOLID_COST
                : operationType == OP_EXTRACT ? EXTRACT_SOLID_COST : 0;
    }

    private long requiredLife() {
        return operationType == OP_IMPLANT ? IMPLANT_LIFE_COST
                : operationType == OP_EXTRACT ? EXTRACT_LIFE_COST : 0;
    }

    private boolean hasResources(int solid, long lifeCost) {
        ItemStack solidStack = inventory.getItem(SOLID_SLOT);
        return solidStack.is(ModItems.akaishiLifeEssenceSolid.get())
                && solidStack.getCount() >= solid
                && life.getEnergyStored() >= lifeCost;
    }

    private void consume(int solid, long lifeCost) {
        life.extractEnergy(lifeCost, false);
        inventory.getItem(SOLID_SLOT).shrink(solid);
    }

    /** 手术中玩家 */
    private Player getOperatingPlayer() {
        return operatingPlayer != null && level != null ? level.getPlayerByUUID(operatingPlayer) : null;
    }

    private void dropItem(ItemStack stack) {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemEntity entity = new ItemEntity(level,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                stack);
        level.addFreshEntity(entity);
    }

    // ===== 界面 =====

    public void setTargetSlot(int index) {
        targetSlot = Math.max(0, Math.min(BodySlot.values().length - 1, index));
        data.set(DATA_TARGET, targetSlot);
        setChanged();
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

    // ===== IItemPipeDevice：器官/固态输入，无输出 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{ORGAN_SLOT, SOLID_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[0];
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
        return Component.translatable("block.akaishi.akaishi_surgery");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // 服务端打开界面时，向该玩家推送其躯体状态供界面渲染
        if (player instanceof ServerPlayer sp) {
            IPlayerBodyState state = PlayerBodyHelper.of(sp);
            if (state != null) {
                PlayerBodySync.sendToPlayer(sp, state);
            }
        }
        return new AkaishiSurgeryMenu(id, inv, this);
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
        tag.putInt("OperationType", operationType);
        tag.putInt("TargetSlot", targetSlot);
        tag.put("Upgrades", upgradeSlots.save(new CompoundTag()));
        if (operatingPlayer != null) {
            tag.putUUID("OperatingPlayer", operatingPlayer);
        }
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
        operationType = tag.getInt("OperationType");
        targetSlot = tag.getInt("TargetSlot");
        if (tag.contains("Upgrades")) {
            upgradeSlots.load(tag.getCompound("Upgrades"));
        }
        operatingPlayer = tag.hasUUID("OperatingPlayer") ? tag.getUUID("OperatingPlayer") : null;
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setItem(i, items.get(i));
        }
        // 服务端重启/玩家离线时手术必然中断，置空等待下次校验
        resetOperation();
    }

    @Override
    public MachineUpgradeSlots getUpgradeSlots() {
        return upgradeSlots;
    }
}
