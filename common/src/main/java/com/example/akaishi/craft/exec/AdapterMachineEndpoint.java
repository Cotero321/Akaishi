package com.example.akaishi.craft.exec;

import com.example.akaishi.api.transfer.IItemAccess;
import com.example.akaishi.api.transfer.ItemAccessHolder;
import com.example.akaishi.block.entity.AkaishiWirelessAccessAdapterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 第三方机器端点：经「无线接入器」认可的机器，用标准物品能力（forge {@code ITEM_HANDLER}，
 * 未注册实现时退回原版 {@code Container}）投料与收货。
 *
 * <p><b>三条客观边界（如实对待，不承诺）</b>：
 * <ul>
 *   <li><b>只有一个访问面</b>：第三方没有"输入槽/输出槽"语义，投料与收货都走接入器面对的那一面。
 *       因此收货只按"本节点期望的那件产物 + 期望件数"取 —— 这是不锁机台前提下唯一安全的对账方式；</li>
 *   <li><b>能耗/耗时/并行度全在它自己手里</b>：我们既读不到也管不了，只能等（执行器有超时兜底）；</li>
 *   <li><b>它的能量要玩家自己供</b>：终端只给装了无线接收升级的自研机台直供能量，第三方机器得自己有电，
 *       否则执行器会一直等到超时（失败原因会写明）。</li>
 * </ul>
 */
public final class AdapterMachineEndpoint implements MachineEndpoint {

    private final ServerLevel level;
    private final BlockPos pos;
    private final Direction side;

    private AdapterMachineEndpoint(ServerLevel level, BlockPos pos, Direction side) {
        this.level = level;
        this.pos = pos;
        this.side = side;
    }

    /** 取接入器当前认可的机器；未认可（或机器已被拆）返回 null */
    @Nullable
    public static MachineEndpoint of(ServerLevel level, AkaishiWirelessAccessAdapterBlockEntity adapter) {
        AkaishiWirelessAccessAdapterBlockEntity.Target target = adapter.target();
        if (target == null) {
            return null;
        }
        return new AdapterMachineEndpoint(level, target.pos(), target.side());
    }

    @Override
    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean own() {
        return false;
    }

    @Override
    public boolean hasInputRoom(Item item) {
        return access().acceptable(level, pos, side, new ItemStack(item)) > 0;
    }

    @Override
    public ItemStack insert(ItemStack stack) {
        return access().insert(level, pos, side, stack);
    }

    @Override
    public ItemStack extract(Item item, int max) {
        return access().extract(level, pos, side, max, stack -> stack.is(item));
    }

    /** 第三方读不出"输入槽"这一层语义 ⇒ 撤回与收货同一路：按物品取回同一面上的那件东西 */
    @Override
    public ItemStack reclaim(Item item, int max) {
        return extract(item, max);
    }

    private static IItemAccess access() {
        return ItemAccessHolder.get();
    }
}
