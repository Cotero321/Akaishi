package com.example.akaishi.life.body;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * 客户端缓存的玩家躯体状态：由 S2C 同步包（PlayerBodySync）填充，
 * 仅供躯体检查仪界面读取。读取顺序与发送端 BodySlot.values() 一致。
 */
public final class ClientBodyData {

    private static final Map<BodySlot, ItemStack> ORGANS = new EnumMap<>(BodySlot.class);
    private static final Map<BodySlot, Integer> REJECTION = new EnumMap<>(BodySlot.class);

    private ClientBodyData() {
    }

    /** 从同步包读取数据（必须在客户端主线程调用） */
    public static void apply(FriendlyByteBuf buf) {
        ORGANS.clear();
        REJECTION.clear();
        for (BodySlot slot : BodySlot.values()) {
            String id = buf.readUtf();
            boolean hasOrgan = buf.readBoolean();
            ItemStack organ = hasOrgan ? buf.readItem() : ItemStack.EMPTY;
            int rejection = buf.readInt();
            // 顺序解析，id 仅作校验兜底
            if (slot.getId().equals(id)) {
                ORGANS.put(slot, organ);
                REJECTION.put(slot, rejection);
            }
        }
    }

    /** 当前槽位移植的器官（客户端） */
    public static ItemStack getOrgan(BodySlot slot) {
        return ORGANS.getOrDefault(slot, ItemStack.EMPTY);
    }

    public static boolean isOccupied(BodySlot slot) {
        return !getOrgan(slot).isEmpty();
    }

    public static int getRejection(BodySlot slot) {
        return REJECTION.getOrDefault(slot, 0);
    }

    /** 总排斥值（9 槽位求和） */
    public static int getTotalRejection() {
        int total = 0;
        for (BodySlot slot : BodySlot.values()) {
            total += getRejection(slot);
        }
        return total;
    }

    /** 已移植部位数量 */
    public static int getOccupiedCount() {
        int count = 0;
        for (BodySlot slot : BodySlot.values()) {
            if (isOccupied(slot)) {
                count++;
            }
        }
        return count;
    }
}
