package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.item.AkaishiPlasmaRodItem;
import com.example.akaishi.menu.AkaishiFusionItemPortMenu;
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
 * 聚变物品输入口方块实体：燃料棒缓冲槽（27 格），对接物品管道/手动投放。
 * 结构成型后自动将缓冲槽内的燃料棒分配到控制器空燃料槽。
 * NBT 持久化缓冲槽与控制器坐标。右键打开 27 格缓冲界面。
 */
public class AkaishiFusionItemInputPortBlockEntity extends BlockEntity implements IItemPipeDevice, ExtendedMenuProvider, IDataCarrier {

    public static final int BUFFER_SLOTS = 27;

    private final SimpleContainer buffer;
    private BlockPos controllerPos;

    public AkaishiFusionItemInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUSION_ITEM_INPUT.get(), pos, state);
        this.buffer = new SimpleContainer(BUFFER_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiFusionItemInputPortBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFusionItemInputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        AkaishiFusionControllerBlockEntity controller = getController();
        if (controller != null && controller.isFormed()) {
            pushRods(controller);
        }
    }

    private AkaishiFusionControllerBlockEntity getController() {
        if (controllerPos == null) {
            return null;
        }
        return level.getBlockEntity(controllerPos) instanceof AkaishiFusionControllerBlockEntity c ? c : null;
    }

    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    /** 将缓冲槽中的燃料棒分配到控制器的空燃料槽（仅已解锁槽） */
    private void pushRods(AkaishiFusionControllerBlockEntity controller) {
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            ItemStack rod = buffer.getItem(slot);
            if (!(rod.getItem() instanceof AkaishiPlasmaRodItem)) {
                continue;
            }
            for (int i = 0; i < controller.getFuelSlotCount(); i++) {
                if (controller.fuelSlots().getItem(i).isEmpty()) {
                    controller.fuelSlots().setItem(i, rod.copy());
                    buffer.setItem(slot, ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    /** 插入物品到缓冲槽，返回实际插入数（0 表示缓冲满或非燃料棒） */
    public int insertRod(ItemStack stack) {
        if (!(stack.getItem() instanceof AkaishiPlasmaRodItem)) {
            return 0;
        }
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

    public SimpleContainer buffer() {
        return buffer;
    }

    // ===== ExtendedMenuProvider =====

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.akaishi.akaishi_fusion_item_input");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AkaishiFusionItemPortMenu(id, inv, buffer, AkaishiFusionItemPortMenu.BufferKind.INPUT_RODS);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }

    // ===== IItemPipeDevice =====

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
        return stack.isEmpty() || stack.getItem() instanceof AkaishiPlasmaRodItem;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        // 管道灌入不经过类型校验：输入口只允许燃料棒，防止杂物占用缓冲槽
        if (stack.isEmpty() || stack.getItem() instanceof AkaishiPlasmaRodItem) {
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
