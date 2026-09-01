package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiGenMatrixControllerBlock;
import com.example.akaishi.energy.AkaishiFuels;
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
 * 发生器矩阵燃料输入口：燃料物品输入缓冲（27 格，仅管道/漏斗，无手动界面）。
 * 结构成型后自动将缓冲槽内可燃烧物品分配到控制器燃料槽。
 * 支持本模组物品管道（IItemPipeDevice）与漏斗（Container）。
 */
public class AkaishiGenFuelInputPortBlockEntity extends BlockEntity implements IItemPipeDevice, IDataCarrier {

    public static final int BUFFER_SLOTS = 27;

    private final SimpleContainer buffer;
    private BlockPos controllerPos;

    public AkaishiGenFuelInputPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_GEN_FUEL_INPUT.get(), pos, state);
        this.buffer = new SimpleContainer(BUFFER_SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiGenFuelInputPortBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiGenFuelInputPortBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        BlockEntity at = controllerPos == null ? null : level.getBlockEntity(controllerPos);
        if (controllerPos != null && !(at instanceof AkaishiGenMatrixControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
        AkaishiGenMatrixControllerBlockEntity controller = at instanceof AkaishiGenMatrixControllerBlockEntity c ? c : null;
        if (controller == null || !controller.getBlockState().getValue(AkaishiGenMatrixControllerBlock.FORMED)) {
            return;
        }
        pushFuelToController(controller);
    }

    /** 将缓冲槽中可燃烧物品移到控制器燃料槽（每次 1 个） */
    private void pushFuelToController(AkaishiGenMatrixControllerBlockEntity controller) {
        if (!controller.getItem(AkaishiGenMatrixControllerBlockEntity.FUEL_SLOT).isEmpty()) {
            return;
        }
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            ItemStack stack = buffer.getItem(i);
            if (!stack.isEmpty() && AkaishiFuels.getFuelEnergy(stack) > 0) {
                controller.setItem(AkaishiGenMatrixControllerBlockEntity.FUEL_SLOT, stack.copyWithCount(1));
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

    // ===== IItemPipeDevice：全槽可进（燃料），不可出（产出由管道从输出槽取） =====

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
        // 仅允许可燃烧物品，防止杂物占用缓冲槽卡死物流
        if (stack.isEmpty() || AkaishiFuels.getFuelEnergy(stack) > 0) {
            buffer.setItem(index, stack);
        }
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.isEmpty() || AkaishiFuels.getFuelEnergy(stack) > 0;
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
