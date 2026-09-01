package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;

import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.block.AkaishiReactorFuelRodBlock;
import com.example.akaishi.item.AkaishiHeatSinkItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 散热组件方块实体：单槽散热片位（插入/取出/熔毁损坏）。
 * 只有贴邻燃料棒时该组件才参与散热（结构校验判定），散热效率由散热片品质决定。
 * 反应堆成型且燃烧时每 tick 消耗散热片 1 点耐久，耐久归零破碎消失。
 * NBT 持久化散热片与控制器坐标。
 */
public class AkaishiReactorCoolerBlockEntity extends BlockEntity implements IItemPipeDevice, IDataCarrier {

    public static final int SINK_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    private final SimpleContainer sinkSlot;
    private BlockPos controllerPos;

    public AkaishiReactorCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_REACTOR_COOLER.get(), pos, state);
        this.sinkSlot = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiReactorCoolerBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiReactorCoolerBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 控制器被拆时清除缓存坐标
        if (controllerPos != null && !(level.getBlockEntity(controllerPos) instanceof AkaishiReactorControllerBlockEntity)) {
            controllerPos = null;
            setChanged();
        }
    }

    public void setControllerPos(BlockPos pos) {
        if (!java.util.Objects.equals(pos, controllerPos)) {
            this.controllerPos = pos == null ? null : pos.immutable();
            setChanged();
        }
    }

    /** 该散热组件是否贴邻燃料棒（结构校验用） */
    public static boolean hasAdjacentFuelRod(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).getBlock() instanceof AkaishiReactorFuelRodBlock) {
                return true;
            }
        }
        return false;
    }

    /** 返回槽位中散热片的散热百分比（无散热片返回 0，结构校验用） */
    public static int getQualityAt(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AkaishiReactorCoolerBlockEntity c) {
            ItemStack sink = c.sinkSlot.getItem(SINK_SLOT);
            if (sink.getItem() instanceof AkaishiHeatSinkItem h) {
                return h.getQuality().coolingPercent;
            }
        }
        return 0;
    }

    /** 返回散热片剩余耐久百分比（0-100）；无散热片或不可损坏返回 -1（供控制器 GUI 展示） */
    public static int getDurabilityPercentAt(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AkaishiReactorCoolerBlockEntity c) {
            ItemStack sink = c.sinkSlot.getItem(SINK_SLOT);
            if (!sink.isEmpty() && sink.getMaxDamage() > 0) {
                int remaining = sink.getMaxDamage() - sink.getDamageValue();
                return (int) ((long) remaining * 100 / sink.getMaxDamage());
            }
        }
        return -1;
    }

    /** 插入散热片，返回实际插入数（0 表示槽已占或非散热片） */
    public int insertHeatSink(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof AkaishiHeatSinkItem)) {
            return 0;
        }
        if (!sinkSlot.getItem(SINK_SLOT).isEmpty()) {
            return 0;
        }
        sinkSlot.setItem(SINK_SLOT, stack.copyWithCount(1));
        setChanged();
        return 1;
    }

    /** 取出散热片 */
    public ItemStack takeHeatSink() {
        ItemStack out = sinkSlot.getItem(SINK_SLOT);
        if (out.isEmpty()) {
            return ItemStack.EMPTY;
        }
        sinkSlot.setItem(SINK_SLOT, ItemStack.EMPTY);
        setChanged();
        return out;
    }

    /** 燃烧期间消耗散热片耐久（每 tick 1 点，仅由控制器对有效散热组件调用）；耐久耗尽破碎消失 */
    public void consumeDurability() {
        ItemStack sink = sinkSlot.getItem(SINK_SLOT);
        if (sink.isEmpty()) {
            return;
        }
        sink.setDamageValue(sink.getDamageValue() + 1);
        if (sink.getDamageValue() >= sink.getMaxDamage()) {
            sinkSlot.setItem(SINK_SLOT, ItemStack.EMPTY);
        }
        setChanged();
    }

    /** 熔毁损坏：散热片破碎消失 */
    public void breakHeatSink() {
        if (!sinkSlot.getItem(SINK_SLOT).isEmpty()) {
            sinkSlot.setItem(SINK_SLOT, ItemStack.EMPTY);
            setChanged();
        }
    }

    // ===== IItemPipeDevice：单槽可进可出 =====

    @Override
    public int[] getPipeInputSlots() {
        return new int[]{SINK_SLOT};
    }

    @Override
    public int[] getPipeOutputSlots() {
        return new int[]{SINK_SLOT};
    }

    // ===== Container 委托 =====

    @Override
    public int getContainerSize() {
        return sinkSlot.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return sinkSlot.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return sinkSlot.getItem(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return sinkSlot.removeItem(index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return sinkSlot.removeItemNoUpdate(index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        sinkSlot.setItem(index, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        sinkSlot.clearContent();
    }

    /** 挖掘保留数据：散热片缓冲物品不保留，排除物品与旧控制器关联坐标 */
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
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < SLOT_COUNT; i++) {
            items.set(i, sinkSlot.getItem(i));
        }
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        controllerPos = tag.contains("ControllerPos") ? BlockPos.of(tag.getLong("ControllerPos")) : null;
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < SLOT_COUNT; i++) {
            sinkSlot.setItem(i, items.get(i));
        }
    }
}
