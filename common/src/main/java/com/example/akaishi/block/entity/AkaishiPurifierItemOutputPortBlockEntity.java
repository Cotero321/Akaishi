package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiPurifierMatrixControllerBlock;
import com.example.akaishi.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 提纯矩阵物品输出口：提纯产物输出缓冲（27 格，仅管道/漏斗，无手动界面）。
 * 结构成型后自动将控制器输出槽的精华拉到缓冲槽，供管道/漏斗/物流系统抽取。
 */
public class AkaishiPurifierItemOutputPortBlockEntity extends BlockEntity implements IItemPipeDevice, IDataCarrier {

    public static final int BUFFER_SLOTS = 27;

    private final SimpleContainer buffer;
    private BlockPos controllerPos;

    public AkaishiPurifierItemOutputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PURIFIER_ITEM_OUTPUT.get(), pos, state);
        this.buffer = new SimpleContainer(BUFFER_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiPurifierItemOutputPortBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPurifierItemOutputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof AkaishiPurifierMatrixControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        AkaishiPurifierMatrixControllerBlockEntity controller = at instanceof AkaishiPurifierMatrixControllerBlockEntity c ? c : null;
        if (controller == null || !controller.getBlockState().getValue(AkaishiPurifierMatrixControllerBlock.FORMED)) {
            return;
        }
        pullOutputFromController(controller);
    }

    /** 将控制器输出槽的精华拉到缓冲槽（每次 1 个，缓冲满则跳过） */
    private void pullOutputFromController(AkaishiPurifierMatrixControllerBlockEntity controller) {
        ItemStack out = controller.getItem(AkaishiPurifierMatrixControllerBlockEntity.OUTPUT_SLOT);
        if (out.isEmpty() || !out.is(ModItems.akaishiEssence.get())) {
            return;
        }
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            ItemStack slot = buffer.getItem(i);
            if (slot.isEmpty()) {
                buffer.setItem(i, out.copyWithCount(1));
                out.shrink(1);
                controller.setChanged();
                setChanged();
                return;
            }
            if (slot.is(out.getItem()) && slot.getCount() < slot.getMaxStackSize()) {
                slot.grow(1);
                out.shrink(1);
                controller.setChanged();
                setChanged();
                return;
            }
        }
    }

    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    /** 缓冲容器（供破坏时倒出与管道访问） */
    public SimpleContainer buffer() {
        return buffer;
    }

    // ===== IItemPipeDevice：全槽可出（产物），不可进 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[0];
    }

    @Override
    public int[] getPipeOutputSlots() {
        int[] all = new int[BUFFER_SLOTS];
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            all[i] = i;
        }
        return all;
    }

    // ===== Container（漏斗支持） =====

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
        // 输出口只允许管道抽取，不允许外部塞入
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        buffer.clearContent();
    }

    /** 挖掘保留数据：排除随方块掉落的缓冲物品与旧控制器关联坐标 */
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
