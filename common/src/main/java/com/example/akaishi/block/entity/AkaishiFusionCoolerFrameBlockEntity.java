package com.example.akaishi.block.entity;

import com.example.akaishi.api.IDataCarrier;
import com.example.akaishi.api.item.IItemPipeDevice;
import com.example.akaishi.item.AkaishiFusionHeatSinkItem;
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
 * 聚变散热框架方块实体：单槽散热片位（插入/取出/消耗）。
 * 结构校验统计槽位散热片效率；聚变堆运行或过热宕机降温期间每 100 tick 消耗 1 点耐久。
 * NBT 持久化散热片与控制器坐标。
 */
public class AkaishiFusionCoolerFrameBlockEntity extends BlockEntity implements IItemPipeDevice, IDataCarrier {

    public static final int SINK_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    private final SimpleContainer sinkSlot;
    private BlockPos controllerPos;

    public AkaishiFusionCoolerFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_FUSION_COOLER_FRAME.get(), pos, state);
        this.sinkSlot = new SimpleContainer(SLOT_COUNT) {
            @Override
            public void setChanged() {
                super.setChanged();
                AkaishiFusionCoolerFrameBlockEntity.this.setChanged();
            }
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiFusionCoolerFrameBlockEntity be) {
        be.tickServer();
    }

    private void tickServer() {
        // 控制器被拆时清除缓存坐标
        if (controllerPos != null && !(level.getBlockEntity(controllerPos) instanceof AkaishiFusionControllerBlockEntity)) {
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

    /** 返回槽位散热片效率（%），无散热片返回 0（结构校验用） */
    public static int getQualityAt(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AkaishiFusionCoolerFrameBlockEntity c) {
            ItemStack sink = c.sinkSlot.getItem(SINK_SLOT);
            if (sink.getItem() instanceof AkaishiFusionHeatSinkItem h) {
                return h.getQuality().coolingPercent;
            }
        }
        return 0;
    }

    /** 返回散热片剩余耐久百分比（0-100）；无散热片返回 -1（GUI 展示用） */
    public static int getDurabilityPercentAt(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AkaishiFusionCoolerFrameBlockEntity c) {
            ItemStack sink = c.sinkSlot.getItem(SINK_SLOT);
            if (!sink.isEmpty() && sink.getMaxDamage() > 0) {
                int remaining = sink.getMaxDamage() - sink.getDamageValue();
                return (int) ((long) remaining * 100 / sink.getMaxDamage());
            }
        }
        return -1;
    }

    /** 消耗散热片耐久（每 100 tick 1 点，仅由控制器在运行/宕机降温期间调用）；耐久耗尽破碎消失 */
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

    /** 挖掘保留数据：散热片不保留，排除物品与旧控制器关联坐标 */
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
