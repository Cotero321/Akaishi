package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.IMinerPortDevice;
import com.example.akaishi.api.item.IMinerOutputSink;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiMinerControllerBlock;
import com.example.akaishi.menu.AkaishiMinerItemOutputMenu;
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
 * 矿机物品输出口方块实体：安装在矿机边立柱上的产物输出设备。
 * 27 格只读缓冲接收控制器推送的挖矿产物，物品管道/漏斗仅可抽取不可放入；
 * 无能量存储，结构成型状态仅用于界面提示。
 */
public class AkaishiMinerItemOutputBlockEntity extends BlockEntity
        implements ExtendedMenuProvider, IMinerPortDevice, IMinerOutputSink, IItemPipeDevice, IDataCarrier {

    /** 产物缓冲槽数 */
    public static final int BUFFER_SLOTS = 27;
    public static final int DATA_ENERGY = 0, DATA_CAPACITY = 1, DATA_FORMED = 2;

    private final SimpleContainer buffer = new SimpleContainer(BUFFER_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            AkaishiMinerItemOutputBlockEntity.this.setChanged();
        }
    };
    private final SimpleContainerData data = new SimpleContainerData(3);
    private BlockPos controllerPos;

    public AkaishiMinerItemOutputBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_MINER_ITEM_OUTPUT.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiMinerItemOutputBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 控制器被拆/结构解散时清除关联坐标，避免悬空引用
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof AkaishiMinerControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        AkaishiMinerControllerBlockEntity controller = at instanceof AkaishiMinerControllerBlockEntity c ? c : null;
        boolean formed = controller != null
                && controller.getBlockState().getValue(AkaishiMinerControllerBlock.FORMED);
        data.set(DATA_ENERGY, 0);
        data.set(DATA_CAPACITY, 0);
        data.set(DATA_FORMED, formed ? 1 : 0);
    }

    /** 接收控制器推送的产物（合并到同种槽优先，其次空槽；返回未放入的剩余） */
    @Override
    public ItemStack receivePartial(ItemStack incoming) {
        if (incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = incoming.copy();
        for (int i = 0; i < BUFFER_SLOTS && !remaining.isEmpty(); i++) {
            ItemStack s = buffer.getItem(i);
            if (s.is(remaining.getItem()) && s.getCount() < s.getMaxStackSize()) {
                int add = Math.min(remaining.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(add);
                remaining.shrink(add);
            }
        }
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

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_miner_item_output");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiMinerItemOutputMenu(id, inv, this);
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
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
        NonNullList<ItemStack> items = NonNullList.withSize(BUFFER_SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            buffer.setItem(i, items.get(i));
        }
    }
}
