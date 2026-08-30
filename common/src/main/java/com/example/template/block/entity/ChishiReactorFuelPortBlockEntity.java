package com.example.template.block.entity;

import com.example.template.api.IDataCarrier;

import com.example.template.api.item.IItemPipeDevice;
import com.example.template.fluid.ReactorFuels;
import com.example.template.item.ChishiFuelCellItem;
import com.example.template.menu.ChishiReactorFuelPortMenu;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 燃料投放口方块实体：燃料罐缓冲槽（27 格），对接物品管道/手动投料。
 * 结构成型后自动将缓冲槽内的燃料罐分配到控制器空燃料槽，并回收控制器中的空罐。
 * NBT 持久化缓冲槽与控制器坐标。右键打开 27 格缓冲界面。
 */
public class ChishiReactorFuelPortBlockEntity extends BlockEntity implements IItemPipeDevice, ExtendedMenuProvider, IDataCarrier {

    public static final int BUFFER_SLOTS = 27;

    private final SimpleContainer buffer;
    private BlockPos controllerPos;

    public ChishiReactorFuelPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_REACTOR_FUEL_PORT.get(), pos, state);
        this.buffer = new SimpleContainer(BUFFER_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                ChishiReactorFuelPortBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChishiReactorFuelPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        ChishiReactorControllerBlockEntity controller = getController();
        if (controller != null && controller.isFormed()) {
            pullEmptyCells(controller);
            pushFilledCells(controller);
        }
    }

    private ChishiReactorControllerBlockEntity getController() {
        if (controllerPos == null) {
            return null;
        }
        return level.getBlockEntity(controllerPos) instanceof ChishiReactorControllerBlockEntity c ? c : null;
    }

    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    /** 将控制器的空燃料罐回收进缓冲槽（供管道/玩家取出） */
    private void pullEmptyCells(ChishiReactorControllerBlockEntity controller) {
        for (int i = 0; i < controller.getRodCount(); i++) {
            ItemStack cell = controller.fuelSlots().getItem(i);
            if (cell.getItem() instanceof ChishiFuelCellItem && ChishiFuelCellItem.isEmpty(cell)) {
                if (insertIntoBuffer(cell)) {
                    controller.fuelSlots().setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    /** 将单个物品放入缓冲槽的空位，返回是否成功 */
    private boolean insertIntoBuffer(ItemStack stack) {
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            if (buffer.getItem(i).isEmpty()) {
                buffer.setItem(i, stack.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    /** 将缓冲槽中的燃料罐分配到控制器的空燃料槽（仅已解锁槽） */
    private void pushFilledCells(ChishiReactorControllerBlockEntity controller) {
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            ItemStack cell = buffer.getItem(slot);
            if (!ReactorFuels.isBurnable(cell)) {
                continue;
            }
            for (int i = 0; i < controller.getRodCount(); i++) {
                if (controller.fuelSlots().getItem(i).isEmpty()) {
                    controller.fuelSlots().setItem(i, cell.copy());
                    buffer.setItem(slot, ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    /** 插入燃料罐到缓冲槽，返回实际插入数（0 表示缓冲满） */
    public int insertCell(ItemStack stack) {
        int count = stack.getCount();
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            if (count <= 0) {
                break;
            }
            if (buffer.getItem(i).isEmpty()) {
                buffer.setItem(i, stack.copyWithCount(1));
                count--;
            }
        }
        int inserted = stack.getCount() - count;
        if (inserted > 0) {
            setChanged();
        }
        return inserted;
    }

    /** 取出一罐（优先空罐方便回收），无物品返回空 */
    public ItemStack takeCell() {
        // 优先空罐
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            ItemStack s = buffer.getItem(i);
            if (s.getItem() instanceof ChishiFuelCellItem && ChishiFuelCellItem.isEmpty(s)) {
                return buffer.removeItem(i, 1);
            }
        }
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            if (!buffer.getItem(i).isEmpty()) {
                return buffer.removeItem(i, 1);
            }
        }
        return ItemStack.EMPTY;
    }

    /** 缓冲容器（供菜单添加槽位） */
    public SimpleContainer buffer() {
        return buffer;
    }

    // ===== ExtendedMenuProvider：右键打开 27 格缓冲界面 =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.template_mod.chishi_reactor_fuel_port");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChishiReactorFuelPortMenu(id, inv, buffer);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== IItemPipeDevice：全槽可进（燃料罐）、可出（空罐/多余罐） =====

    @Override
    public int[] getPipeInputSlots() {
        int[] all = new int[BUFFER_SLOTS];
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            all[i] = i;
        }
        return all;
    }

    @Override
    public int[] getPipeOutputSlots() {
        return getPipeInputSlots();
    }

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
    public boolean canPlaceItem(int index, ItemStack stack) {
        // 仅允许空/燃料罐：供管道/漏斗在放入前预判，避免非燃料物品被 setItem 静默吞掉
        return stack.isEmpty() || stack.getItem() instanceof ChishiFuelCellItem;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        // 管道灌入不经过类型校验：燃料口只允许空/燃料罐，防止杂物占用缓冲槽卡死物流
        if (stack.isEmpty() || stack.getItem() instanceof ChishiFuelCellItem) {
            buffer.setItem(index, stack);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        buffer.clearContent();
    }

    /** 挖掘保留数据：反应堆燃料缓冲不保留，排除物品与旧控制器关联坐标 */
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
