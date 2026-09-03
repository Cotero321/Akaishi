package com.example.akaishi.life.body;

import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端缓存的玩家躯体状态：由 S2C 同步包（PlayerBodySync）填充，
 * 供躯体检查仪 / 基因管理器界面读取。读取顺序与发送端一致（先基因强化列表，后槽位）。
 */
public final class ClientBodyData {

    private static final Map<BodySlot, ItemStack> ORGANS = new EnumMap<>(BodySlot.class);
    private static final Map<BodySlot, Integer> REJECTION = new EnumMap<>(BodySlot.class);
    /** 已吸收基因强化：生物来源 → 适配加成（吸收顺序） */
    private static final Map<String, Integer> GENE_BONUSES = new LinkedHashMap<>();
    /** 突破激活（客户端镜像：无激活时 entity 空串、until=-1） */
    private static String btEntity = "";
    private static int btExtra;
    private static int btPct;
    private static long btUntil = -1L;

    private ClientBodyData() {
    }

    /** 从同步包读取数据（必须在客户端主线程调用） */
    public static void apply(FriendlyByteBuf buf) {
        ORGANS.clear();
        REJECTION.clear();
        GENE_BONUSES.clear();
        int geneCount = buf.readVarInt();
        for (int i = 0; i < geneCount; i++) {
            String entityId = buf.readUtf();
            int bonus = buf.readInt();
            if (!entityId.isEmpty()) {
                GENE_BONUSES.put(entityId, bonus);
            }
        }
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
        // 突破激活镜像（顺序与发送端一致：在槽位数据之后）
        boolean hasBt = buf.readBoolean();
        if (hasBt) {
            btEntity = buf.readUtf();
            btExtra = buf.readInt();
            btPct = buf.readInt();
            btUntil = buf.readLong();
        } else {
            btEntity = "";
            btExtra = 0;
            btPct = 0;
            btUntil = -1L;
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

    /** 已移植器官数量（非空槽位） */
    public static int getOccupiedCount() {
        int n = 0;
        for (BodySlot slot : BodySlot.values()) {
            if (isOccupied(slot)) {
                n++;
            }
        }
        return n;
    }

    /** 身体中已移植的同一生物来源非原生器官数（同源套装显示用） */
    public static int sameSourceCount(String entityId) {
        if (entityId == null || entityId.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = getOrgan(slot);
            if (organ.getItem() instanceof AkaishiOrganItem && !AkaishiOrganItem.isNative(organ)
                    && entityId.equals(AkaishiOrganItem.getEntityId(organ))) {
                n++;
            }
        }
        return n;
    }

    /** 已吸收基因强化条目（来源 → 加成，插入序即吸收顺序） */
    public static Map<String, Integer> getGeneBonuses() {
        return Collections.unmodifiableMap(GENE_BONUSES);
    }

    /** 已吸收基因型数量 */
    public static int getGeneCount() {
        return GENE_BONUSES.size();
    }

    public static boolean hasGene(String entityId) {
        return GENE_BONUSES.containsKey(entityId);
    }

    public static int getGeneBonus(String entityId) {
        return GENE_BONUSES.getOrDefault(entityId, 0);
    }

    // ===== 突破激活（客户端镜像） =====

    public static boolean hasActiveBreakthrough() {
        return !btEntity.isEmpty();
    }

    /** 激活突破的来源生物 id（无激活返回空串） */
    public static String getBreakthroughEntity() {
        return btEntity;
    }

    /** 激活中的额外适配加成（2/4/6/8） */
    public static int getBreakthroughExtra() {
        return btExtra;
    }

    /** 激活中的基础数值强化百分比（10/20/30/40） */
    public static int getBreakthroughPct() {
        return btPct;
    }

    /** 激活截止时刻（客户端 gameTime 同步，无激活返回 -1） */
    public static long getBreakthroughUntil() {
        return btUntil;
    }

    /** 剩余生效 tick（<=0 表示已结束，由客户端每帧递减显示） */
    public static long getBreakthroughRemainingTicks(long clientGameTime) {
        if (btUntil < 0L) {
            return 0L;
        }
        return Math.max(0L, btUntil - clientGameTime);
    }
}
