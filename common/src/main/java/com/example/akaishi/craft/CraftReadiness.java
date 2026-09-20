package com.example.akaishi.craft;

import com.example.akaishi.api.storage.IItemStorageUnit;
import com.example.akaishi.energy.AkaishiEnergyType;
import com.example.akaishi.energy.LifeEnergyType;
import com.example.akaishi.value.ValueCache;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

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
 * 判定走的是与开工完全相同的 {@link VirtualCraftPlanner#plan} → 叶子清单 → 库存核对 +
 * 能量够不够 + 工序供给够不够，因此不会出现"高亮说能做、点下去却说材料/能量/机台不足"的两套口径。
 */
public final class CraftReadiness {

    private static final Logger LOGGER = LogManager.getLogger("akaishi.craft");

    /** 判定结果缓存：键 = 库 + 能量 + 场域工序供给的指纹（配方表变化由 planner 索引自身的指纹兜住） */
    private static long cachedKey = Long.MIN_VALUE;
    private static volatile Set<Item> cached = Set.of();

    private CraftReadiness() {
    }

    /**
     * 当前可直接制作的物品集合（<b>服务端调用</b>：需要真实库内容）。
     *
     * @param candidates 需要判定的候选（即下发给界面的目录）
     * @param units      储存终端的存储单元
     * @param pool       矩阵场域内的能量池（机器能耗来源）；null = 没有能量池 ⇒ 要耗能的都判为"做不出"
     * @param supply     矩阵场域的工序供给（自家机台 + 认可的第三方机器）；null = 没有 ⇒ 要机台的都判为"做不出"
     */
    public static Set<Item> ready(RecipeManager manager, RegistryAccess access,
            List<Item> candidates, List<IItemStorageUnit> units, @Nullable ICraftEnergyPool pool,
            @Nullable IProcessSupply supply) {
        Map<Item, Long> stock = CraftLibrary.stock(units);
        // 场域扫描只做一次：目录有上千条，逐条重扫会被放大上千倍
        ProcessCoverage coverage = supply == null ? ProcessCoverage.EMPTY : supply.processCoverage();
        // 机台光在场不够，还得拿得到能量（真机加工）：拿不到就是"点下去干等到超时"，不能标成可做
        boolean powered = supply != null && supply.canPowerMachines();
        long key = fingerprint(manager, stock, pool, coverage, powered);
        if (key == cachedKey) {
            return cached;
        }
        Set<Item> ready = new HashSet<>();
        long started = System.nanoTime();
        for (Item item : candidates) {
            // 规划带上库存：库里已有的中间产物（如木板/木棍）直接算数，缺的才现做 ⇒
            // "下级材料够"的物品也会被标成可直接制作（与开工时同一套账）
            VirtualCraftPlanner.Plan plan = VirtualCraftPlanner.plan(manager, access, item, 1, stock, coverage);
            if (plan != null && CraftLibrary.hasAll(stock, plan.consumables())
                    && affordableEnergy(pool, plan) && coverage.covers(plan.machineKinds())
                    && (plan.machineKinds().isEmpty() || powered)) {
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

    /**
     * 机器能量够不够（与 {@code VirtualCraftTask.start} 的预检同口径）。
     * <p>不判这一条会漏出"列表高亮说能做、点进去却开不了工"的两套口径 —— 手续费之外，
     * 机器能耗（赤能源 / 生命能量）也是开工的硬门槛。
     */
    private static boolean affordableEnergy(@Nullable ICraftEnergyPool pool, VirtualCraftPlanner.Plan plan) {
        MachineProcessEnergy.Cost cost = plan.machineCost();
        if (cost.isFree()) {
            return true;
        }
        return pool != null
                && pool.availableEnergy(AkaishiEnergyType.INSTANCE) >= cost.chishi()
                && pool.availableEnergy(LifeEnergyType.INSTANCE) >= cost.life();
    }

    /**
     * 库快照 + 能量余额 + 场域工序供给 + <b>配方内容指纹</b>（任一变化即重算）。
     * <p>
     * 配方指纹必须计入：{@code /reload} 改了配方而库存与机台都没变时，真值变了而旧键不变，
     * 于是返回过期的 ready 集合 —— 现象就是"列表亮着、点进去开不了工"。
     */
    private static long fingerprint(RecipeManager manager, Map<Item, Long> stock,
            @Nullable ICraftEnergyPool pool, ProcessCoverage coverage, boolean powered) {
        long hash = 1125899906842597L;
        ValueCache.Fingerprint recipes = ValueCache.fingerprint(manager);
        hash = 31L * hash + recipes.recipeCount();
        hash = 31L * hash + recipes.hash();
        for (Map.Entry<Item, Long> entry : stock.entrySet()) {
            hash = 31L * hash + BuiltInRegistries.ITEM.getKey(entry.getKey()).hashCode();
            hash = 31L * hash + entry.getValue();
        }
        if (pool != null) {
            hash = 31L * hash + pool.availableEnergy(AkaishiEnergyType.INSTANCE);
            hash = 31L * hash + pool.availableEnergy(LifeEnergyType.INSTANCE);
        }
        hash = 31L * hash + coverage.hashCode();
        // 供能资格必须计入：升级件装上/拆下不改变 coverage，但"能不能开工"变了
        hash = 31L * hash + (powered ? 1L : 0L);
        return hash ^ stock.size();
    }
}
