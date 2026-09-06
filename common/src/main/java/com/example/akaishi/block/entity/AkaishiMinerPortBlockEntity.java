package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.IMinerPortDevice;
import com.example.akaishi.api.energy.IEnergyProvider;
import com.example.akaishi.api.energy.IEnergyStorage;
import com.example.akaishi.api.item.IMinerOutputSink;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiMinerControllerBlock;
import com.example.akaishi.energy.AkaishiEnergyStorage;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.menu.AkaishiMinerPortMenu;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
import org.jetbrains.annotations.NotNull;

/**
 * 矿机转口方块实体：产物缓冲 27 格（物品管道/漏斗抽取）+ 赤能源输入缓冲。
 * 结构成型后每 tick 把缓冲能量转发给控制器，并接收控制器推送的挖矿产物。
 */
public class AkaishiMinerPortBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IEnergyProvider, IItemPipeDevice, IDataCarrier,
        IMinerPortDevice, IMinerOutputSink {

    /** 产物缓冲槽数 */
    public static final int BUFFER_SLOTS = 27;
    /** 能量缓冲容量 */
    public static final long BUFFER_CAPACITY = 10_000_000L;

    public static final int DATA_ENERGY = 0, DATA_CAPACITY = 1, DATA_FORMED = 2;

    private final SimpleContainer buffer = new SimpleContainer(BUFFER_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            AkaishiMinerPortBlockEntity.this.setChanged();
        }
    };
    private final AkaishiEnergyStorage energy = new AkaishiEnergyStorage(AkaishiEnergyType.INSTANCE, BUFFER_CAPACITY);
    private final SimpleContainerData data = new SimpleContainerData(3);
    private BlockPos controllerPos;

    public AkaishiMinerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINER_PORT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiMinerPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        data.set(0, (int) energy.getEnergyStored());
        data.set(1, (int) energy.getMaxEnergy());
        // 控制器被拆/结构解散时清除关联坐标，避免悬空引用
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof AkaishiMinerControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        AkaishiMinerControllerBlockEntity controller = at instanceof AkaishiMinerControllerBlockEntity c ? c : null;
        boolean formed = controller != null
                && controller.getBlockState().getValue(AkaishiMinerControllerBlock.FORMED);
        data.set(2, formed ? 1 : 0);
        if (!formed) {
            return;
        }
        // 能量缓冲 → 控制器（每 tick 满速转发，仅受控制器剩余容量限制）
        long free = controller.getEnergyCapacity() - controller.getEnergyStored();
        long pushed = energy.extractEnergy(free, false);
        if (pushed > 0) {
            controller.addEnergy(pushed);
            controller.setChanged();
            setChanged();
        }
    }

    /** 接收控制器推送的产物（支持部分合并：尽量并入同种槽/空槽，返回未放入的剩余部分） */
    @Override
    public ItemStack receivePartial(ItemStack incoming) {
        if (incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = incoming.copy();
        // 1) 尽量并入已有同种物品槽（允许部分空间）
        for (int i = 0; i < BUFFER_SLOTS && !remaining.isEmpty(); i++) {
            ItemStack s = buffer.getItem(i);
            if (s.is(remaining.getItem()) && s.getCount() < s.getMaxStackSize()) {
                int add = Math.min(remaining.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(add);
                remaining.shrink(add);
            }
        }
        // 2) 剩余放入空槽
        for (int i = 0; i < BUFFER_SLOTS && !remaining.isEmpty(); i++) {
            if (buffer.getItem(i).isEmpty()) {
                int put = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                buffer.setItem(i, remaining.split(put));
            }
        }
        if (remaining.getCount() != incoming.getCount()) {
            setChanged();
        }
        return remaining;
    }

    public SimpleContainer buffer() {
        return buffer;
    }

    public AkaishiEnergyStorage energy() {
        return energy;
    }

    public ContainerData data() {
        return data;
    }

    @Override
    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    // ===== IItemPipeDevice：仅输出槽（产物供管道抽取），不可输入 =====

    @Override
    public int[] getPipeOutputSlots() {
        int[] out = new int[BUFFER_SLOTS];
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            out[i] = i;
        }
        return out;
    }

    // ===== Container（漏斗抽取） =====

    @Override
    public int getContainerSize() {
        return buffer.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return buffer.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return buffer.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return buffer.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        buffer.setItem(index, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return buffer.stillValid(player);
    }

    @Override
    public void clearContent() {
        buffer.clearContent();
    }

    @Override
    public boolean canPlaceItem(int index, @NotNull ItemStack stack) {
        return false; // 产物缓冲只读，仅控制器可写入
    }

    // ===== IEnergyProvider：能量输入口（赤能源管道充能） =====

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
        return Component.translatable("block.akaishi.akaishi_miner_port");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiMinerPortMenu(id, inv, this);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    /** 挖掘保留数据：排除产物缓冲（已随 onRemove 掉落）与旧控制器关联坐标 */
    @Override
    public String[] excludedKeys() {
        return new String[]{"Items", "ControllerPos"};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("Energy", energy.getEnergyStored());
        if (controllerPos != null) {
            tag.putLong("ControllerPos", controllerPos.asLong());
        }
        NonNullList<ItemStack> items = NonNullList.withSize(BUFFER_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            items.set(i, buffer.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energy.setEnergy(tag.getLong("Energy"));
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
        NonNullList<ItemStack> items = NonNullList.withSize(BUFFER_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            buffer.setItem(i, items.get(i));
        }
    }
}
