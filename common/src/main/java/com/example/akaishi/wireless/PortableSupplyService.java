package com.example.akaishi.wireless;

import com.example.akaishi.api.security.AkaishiSecurityPermission;
import com.example.akaishi.item.AkaishiPortableEnergyCell;
import com.example.akaishi.item.AkaishiWirelessIdentityCardItem;
import com.example.akaishi.item.AkaishiWirelessPortableTerminalItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * 便携终端「随身供能」服务：由平台玩家 tick 驱动，把绑定终端的储能持续注入玩家随身承接物。
 * <p>
 * 解锁前提：终端内腔含 ≥1 便捷传输构架（{@link IWirelessTerminal#hasTransmitFrame()}），
 * 且便携终端物品 NBT 已开启供能开关（{@link #setEnabled(ItemStack, boolean)}）。
 * 承接物分两层：
 * 1) 背包/快捷栏的便捷赤能源单元（本地处理）；
 * 2) 已装备的赤石饰品（依赖 Curios，common 侧不可见，经 {@link PlatformSink} 由平台实现注入）。
 * 手持赤能源工具/盔甲的补充由既有「便携单元 → 装备耐久」逻辑承接：本服务为其上游充能。
 * 单 tick 抽能总量上限 {@link #MAX_SUPPLY_PER_TICK}。
 */
public final class PortableSupplyService {

    /** 单 tick 供能总量上限（10M） */
    public static final long MAX_SUPPLY_PER_TICK = 10_000_000L;
    /** 便携终端物品 NBT 中的供能开关键 */
    private static final String TAG_SUPPLY_ENABLED = "WirelessSupplyEnabled";

    /**
     * 平台供能承接器：common 侧无法访问 Curios API，由平台层（Forge）注册实现。
     */
    public interface PlatformSink {
        /** 平台承接物当前可接受的能量上限（0=无需求或无平台支持） */
        long demand(Player player, long max);

        /** 注入能量，返回实际注入量 */
        long charge(Player player, long amount);
    }

    /** 缺省空承接器（无平台注册时不影响本地承接物供能） */
    private static volatile PlatformSink platformSink = new PlatformSink() {
        @Override
        public long demand(Player player, long max) {
            return 0;
        }

        @Override
        public long charge(Player player, long amount) {
            return 0;
        }
    };

    private PortableSupplyService() {
    }

    /** 注册平台供能承接器（由平台初始化时调用） */
    public static void setPlatformSink(PlatformSink sink) {
        if (sink != null) {
            platformSink = sink;
        }
    }

    /** 便携终端「随身供能」开关是否开启（持久化于物品 NBT） */
    public static boolean isEnabled(ItemStack portable) {
        return portable.hasTag() && portable.getTag().getBoolean(TAG_SUPPLY_ENABLED);
    }

    /** 设置便携终端「随身供能」开关（写入/清除物品 NBT） */
    public static void setEnabled(ItemStack portable, boolean enabled) {
        if (enabled) {
            portable.getOrCreateTag().putBoolean(TAG_SUPPLY_ENABLED, true);
        } else if (portable.hasTag()) {
            portable.getTag().remove(TAG_SUPPLY_ENABLED);
        }
    }

    /**
     * 服务端每 tick 调用：取玩家第一台开启供能的便携终端，从其绑定终端抽能注入承接物。
     * 未开启开关 / 无可用终端（无 CRAFT 权限或离线）/ 未解锁（无便捷传输构架）时静默跳过。
     */
    public static void tick(Player player) {
        if (player.level().isClientSide || player.isDeadOrDying()) {
            return;
        }
        ItemStack portable = findPortable(player);
        if (portable.isEmpty() || !isEnabled(portable)) {
            return;
        }
        IWirelessTerminal terminal = resolveTerminal(player);
        if (terminal == null || !terminal.hasTransmitFrame()) {
            return;
        }
        long demand = Math.min(MAX_SUPPLY_PER_TICK, cellDemand(player) + platformSink.demand(player, MAX_SUPPLY_PER_TICK));
        if (demand <= 0) {
            return; // 承接物已满，不抽能
        }
        long drawn = terminal.extractWireless(demand);
        if (drawn <= 0) {
            return;
        }
        long leftover = chargeCells(player, drawn);
        if (leftover > 0) {
            platformSink.charge(player, leftover);
        }
    }

    /** 玩家背包（含快捷栏/副手等全部格）内第一台无线能源便捷终端；未持有返回空栈 */
    private static ItemStack findPortable(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof AkaishiWirelessPortableTerminalItem) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 反查便携终端可用终端的方块实体：身份取「背包身份卡绑定的玩家」，卡未绑定身份或未持卡则取玩家自己；
     * 终端按安全表与身份反查（需具备 {@link AkaishiSecurityPermission#CRAFT} 权限）。
     * 离线或跨维未加载返回 null。
     * <p>
     * 依赖面收敛到 {@link IWirelessTerminal}：完整终端与<b>微缩后的终端</b>（同一个终端 ID 继续注册）
     * 对便携终端完全同构，坍缩不会让随身供能失效。
     */
    private static IWirelessTerminal resolveTerminal(Player player) {
        UUID identity = null;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof AkaishiWirelessIdentityCardItem) {
                identity = AkaishiWirelessIdentityCardItem.playerOf(s);
                break;
            }
        }
        if (identity == null) {
            identity = player.getUUID(); // 卡未绑定身份 / 未持卡：以本人身份请求
        }
        UUID terminalId = WirelessNetworkManager.findTerminalForIdentity(identity, WirelessFamily.CHISHI,
                AkaishiSecurityPermission.CRAFT);
        if (terminalId == null) {
            return null;
        }
        WirelessNetworkManager.TerminalRef ref = WirelessNetworkManager.terminalOf(terminalId);
        if (ref == null || player.getServer() == null) {
            return null;
        }
        ServerLevel target = player.getServer().getLevel(ref.dimension());
        if (target == null) {
            return null;
        }
        return target.getBlockEntity(ref.pos()) instanceof IWirelessTerminal t && t.isFormed() ? t : null;
    }

    /** 背包便携单元总缺口（容量 - 当前储能之和） */
    private static long cellDemand(Player player) {
        long demand = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof AkaishiPortableEnergyCell cell) {
                demand += cell.getMaxEnergy() - cell.getEnergyStored(s);
            }
        }
        return demand;
    }

    /** 向背包便携单元注入能量，返回未用完的剩余额度 */
    private static long chargeCells(Player player, long amount) {
        long remaining = amount;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof AkaishiPortableEnergyCell cell) {
                remaining -= cell.addEnergy(s, remaining, false);
            }
        }
        return remaining;
    }
}
