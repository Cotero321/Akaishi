package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiPurifierMatrixControllerBlock;
import com.example.akaishi.block.ModBlocks;
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
 * 提纯矩阵物品输入口：提纯原料输入缓冲（27 格，仅管道/漏斗，无手动界面）。
 * 结构成型后自动将缓冲槽内有效原料（粗制赤石块/赤石水晶块）分配到控制器输入槽。
 */
public class AkaishiPurifierItemInputPortBlockEntity extends BlockEntity implements IItemPipeDevice, IDataCarrier {

    public static final int BUFFER_SLOTS = 27;

    private final SimpleContainer buffer;
    private BlockPos controllerPos;

    public AkaishiPurifierItemInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PURIFIER_ITEM_INPUT.get(), pos, state);
        this.buffer = new SimpleContainer(BUFFER_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiPurifierItemInputPortBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPurifierItemInputPortBlockEntity be) {
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
        pushInputToController(controller);
    }

    /** 将缓冲槽中有效原料移到控制器输入槽（每次 1 个） */
    private void pushInputToController(AkaishiPurifierMatrixControllerBlockEntity controller) {
        if (!controller.getItem(AkaishiPurifierMatrixControllerBlockEntity.INPUT_SLOT).isEmpty()) {
            return;
        }
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            ItemStack stack = buffer.getItem(i);
            if (!stack.isEmpty() && isValidInput(stack)) {
                controller.setItem(AkaishiPurifierMatrixControllerBlockEntity.INPUT_SLOT, stack.copyWithCount(1));
                stack.shrink(1);
                if (stack.isEmpty()) {
                    buffer.setItem(i, ItemStack.EMPTY);
                }
                controller.setChanged();
                setChanged();
                return;
            }
        }
    }

    /** 有效输入：粗制赤石块 或 赤石水晶块 */
    private static boolean isValidInput(ItemStack stack) {
        return stack.is(ModBlocks.RAW_CHISHI_BLOCK.get().asItem())
                || stack.is(ModBlocks.CHISHI_CRYSTAL_BLOCK.get().asItem());
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

    // ===== IItemPipeDevice：全槽可进（原料），不可出 =====

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
        return new int[0];
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
        // 仅允许有效提纯原料，防止杂物占用缓冲槽卡死物流
        if (stack.isEmpty() || isValidInput(stack)) {
            buffer.setItem(index, stack);
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.isEmpty() || isValidInput(stack);
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
