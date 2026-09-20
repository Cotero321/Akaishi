package com.example.akaishi.craft.exec;

import com.example.akaishi.api.item.IItemPipeDevice;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 自研机台端点：按 {@link IItemPipeDevice} 声明的输入/输出槽投料与收货。
 *
 * <p><b>为什么必须分槽</b>：机器把输入槽与输出槽分开（与 GUI、与管道方向语义同源）。
 * 往输出槽塞料会被机器判成"输出已满"而停摆；从输入槽取货则等于偷走还没加工的料。
 *
 * <p><b>写回一律走 {@code setItem}</b>：{@code SimpleContainer#setItem} 会置脏并触发方块实体的
 * {@code setChanged()}，而 {@code removeItem} 不置脏 —— 取货后若机器带着旧内容存盘，玩家读档会看到
 * "产物又回来了"。故这里自己实现"取出并写回余量"。
 */
public final class OwnMachineEndpoint implements MachineEndpoint {

    private final BlockPos pos;
    private final IItemPipeDevice device;

    public OwnMachineEndpoint(BlockPos pos, IItemPipeDevice device) {
        this.pos = pos;
        this.device = device;
    }

    @Override
    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean own() {
        return true;
    }

    @Override
    public boolean hasInputRoom(Item item) {
        for (int slot : device.getPipeInputSlots()) {
            if (roomFor(slot, item) > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        ItemStack left = stack.copy();
        for (int slot : device.getPipeInputSlots()) {
            if (left.isEmpty()) {
                break;
            }
            int room = roomFor(slot, left.getItem());
            if (room <= 0) {
                continue;
            }
            int put = Math.min(room, left.getCount());
            ItemStack current = device.getItem(slot);
            if (current.isEmpty()) {
                // 保留原堆的 NBT（投进去的就是从库里取出的那堆）
                device.setItem(slot, left.copyWithCount(put));
            } else {
                current.grow(put);
                device.setItem(slot, current);
            }
            left.shrink(put);
        }
        return left;
    }

    @Override
    public ItemStack extract(Item item, int max) {
        return pull(device.getPipeOutputSlots(), item, max);
    }

    @Override
    public ItemStack reclaim(Item item, int max) {
        return pull(device.getPipeInputSlots(), item, max);
    }

    /** 该槽还能放几件这种物品（按物品匹配，与账本同为物品级） */
    private int roomFor(int slot, Item item) {
        ItemStack current = device.getItem(slot);
        if (current.isEmpty()) {
            return device.getMaxStackSize();
        }
        if (!current.is(item)) {
            return 0;
        }
        return Math.max(0, current.getMaxStackSize() - current.getCount());
    }

    /** 从给定槽位取回指定物品（取出后把余量写回，保证置脏） */
    private ItemStack pull(int[] slots, Item item, int max) {
        if (max <= 0) {
            return ItemStack.EMPTY;
        }
        for (int slot : slots) {
            ItemStack current = device.getItem(slot);
            if (current.isEmpty() || !current.is(item)) {
                continue;
            }
            int take = Math.min(max, current.getCount());
            ItemStack out = current.copyWithCount(take);
            int rest = current.getCount() - take;
            device.setItem(slot, rest <= 0 ? ItemStack.EMPTY : current.copyWithCount(rest));
            return out;
        }
        return ItemStack.EMPTY;
    }
}
