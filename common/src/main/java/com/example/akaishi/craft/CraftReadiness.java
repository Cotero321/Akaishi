package com.example.akaishi.craft;

import com.example.akaishi.api.storage.IItemStorageUnit;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 「现在就能做」判定：给加工列表排出"可直接制作优先"的顺序，并让界面高亮/压暗。
 * <p>
 * 与 {@link VirtualCraftPlanner#craftableItems} 的分工：那个只看<b>配方树可解</b>（与库无关），
 * 所以列表里既有"能做"也有"缺料"的条目；本类再叠一层<b>库（储存终端）快照</b>核对，
 * 于是高亮出来的必然做得出（口径：可见即可用）。
 * <p>
 * 判定走的是与开工完全相同的 {@link VirtualCraftPlanner#plan} → 叶子清单 → 库存核对，
 * 因此不会出现"高亮说能做、点下去却说材料不足"的两套口径。
 */
public final class CraftReadiness {

    private static final Logger LOGGER = LogManager.getLogger("akaishi.craft");

    /** 判定结果缓存：键 = 库快照指纹（配方表变化由 planner 索引自身的指纹兜住） */
    private static long cachedKey = Long.MIN_VALUE;
    private static volatile Set<Item> cached = Set.of();

    private CraftReadiness() {
    }

    /**
     * 当前可直接制作的物品集合（<b>服务端调用</b>：需要真实库内容）。
     *
     * @param candidates 需要判定的候选（即下发给界面的目录）
     * @param units      储存终端的存储单元
     */
    public static Set<Item> ready(RecipeManager manager, RegistryAccess access,
            List<Item> candidates, List<IItemStorageUnit> units) {
        Map<Item, Long> stock = CraftLibrary.stock(units);
        long key = fingerprint(stock);
        if (key == cachedKey) {
            return cached;
        }
        Set<Item> ready = new HashSet<>();
        long started = System.nanoTime();
        for (Item item : candidates) {
            // 规划带上库存：库里已有的中间产物（如木板/木棍）直接算数，缺的才现做 ⇒
            // "下级材料够"的物品也会被标成可直接制作（与开工时同一套账）
            VirtualCraftPlanner.Plan plan = VirtualCraftPlanner.plan(manager, access, item, 1, stock);
            if (plan != null && CraftLibrary.hasAll(stock, plan.consumables())) {
                ready.add(item);
            }
        }
        long ms = (System.nanoTime() - started) / 1_000_000L;
        // 只在真的慢时打日志：这是服务端线程上的同步工作量，慢了需要改成分片
        if (ms >= 50L) {
            LOGGER.info("[akaishi] 可制作判定：{} / {} 项，用时 {} ms", ready.size(), candidates.size(), ms);
        }
        cachedKey = key;
        cached = ready;
        return ready;
    }

    /** 库快照指纹（物品 + 数量，与遍历顺序无关）：库一变就重算 */
    private static long fingerprint(Map<Item, Long> stock) {
        long hash = 1125899906842597L;
        for (Map.Entry<Item, Long> entry : stock.entrySet()) {
            hash = 31L * hash + BuiltInRegistries.ITEM.getKey(entry.getKey()).hashCode();
            hash = 31L * hash + entry.getValue();
        }
        return hash ^ stock.size();
    }
}
