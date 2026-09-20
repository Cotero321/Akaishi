package com.example.akaishi.craft.thirdparty;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 「第三方认可表」：声明某个配方类型由哪些第三方机器的方块提供，以及跑一道工序的基准成本。
 *
 * <p><b>为什么需要外部声明</b>：第三方机器的执行逻辑在它自己内部，服务端拿不到"这台机器能跑哪些配方"
 * （同 §15.11 的论证）。所以本表是<b>唯一来源</b>，由数据包提供：
 * {@code data/<namespace>/third_party_process/<name>.json}：
 * <pre>
 * {
 *   "process": "mekanism:enriching",
 *   "blocks": ["mekanism:enrichment_chamber", "mekanism:enrichment_factory"],
 *   "energy": 8000,      // 可选，缺省 {@link #DEFAULT_CHISHI}
 *   "life": 0,           // 可选，缺省 0
 *   "ticks": 60          // 可选，缺省 {@link #DEFAULT_TICKS}
 * }
 * </pre>
 *
 * <p><b>两级口径</b>（与用户拍板一致）：
 * <ul>
 *   <li><b>精确</b>：配方类型在本表里声明 ⇒ 准入要求场域内真有声明里那些方块之一（经接入器认可）；</li>
 *   <li><b>粗粒度</b>：未声明的第三方配方类型 ⇒ 只要场域内有<b>任意一台</b>经认可的第三方机器即可跑，
 *       成本取 {@link #DEFAULT_CHISHI} / {@link #DEFAULT_TICKS}。</li>
 * </ul>
 * 不写任何条目时，"未声明的第三方配方"仍走粗粒度（放一台机器就能跑）；要精确控制就写条目。
 *
 * <p>重载整体替换（volatile 换表），读取端无锁。
 */
public final class ThirdPartyProcesses {

    private static final Logger LOGGER = LogManager.getLogger("akaishi.thirdparty");

    /**
     * 未声明时的单道工序基准赤能源消耗：<b>0</b>。
     * <p>刻意不发明数字：第三方机器耗几度电由它自己决定、且随它自己的升级变化，我们无从得知；
     * 更重要的是<b>非零默认会开一个口子</b>——无原料格的第三方配方会因此被当成"能量换物"放行（凭空造物）。
     * 要收费就在数据包里逐条写 {@code energy}。
     */
    public static final long DEFAULT_CHISHI = 0L;
    /** 未声明时的单道工序基准耗时（tick） */
    public static final long DEFAULT_TICKS = 100L;

    /** 一条认可声明（{@code process} 已在载入时解析成注册表里的实际类型） */
    public record Entry(RecipeType<?> process, ResourceLocation processId, Set<Block> blocks, long chishi, long life,
            long ticks) {
    }

    private static volatile Map<RecipeType<?>, Entry> byProcess = Map.of();
    private static volatile Map<Block, Entry> byBlock = Map.of();

    private ThirdPartyProcesses() {
    }

    /** 数据包重载入口（整表替换） */
    public static void apply(Map<ResourceLocation, JsonElement> files) {
        Map<RecipeType<?>, Entry> processes = new HashMap<>();
        Map<Block, Entry> blocks = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : files.entrySet()) {
            Entry entry = parse(file.getKey(), file.getValue());
            if (entry == null) {
                continue;
            }
            Entry previous = processes.put(entry.process(), entry);
            if (previous != null) {
                // 同一配方类型被两个文件声明：遍历顺序不稳定，故明确警告并保留后者
                LOGGER.warn("[akaishi] 第三方认可表重复声明 {}（{} 覆盖 {}）",
                        entry.processId(), file.getKey(), previous.processId());
            }
            for (Block block : entry.blocks()) {
                blocks.put(block, entry);
            }
        }
        byProcess = Map.copyOf(processes);
        byBlock = Map.copyOf(blocks);
        LOGGER.info("[akaishi] 第三方认可表已载入：{} 个配方类型 / {} 个方块", processes.size(), blocks.size());
    }

    /** 该配方类型是否由数据包精确声明过 */
    public static boolean isDeclared(@Nullable RecipeType<?> type) {
        return type != null && byProcess.containsKey(type);
    }

    /** 配方类型 → 声明（未声明返回 null） */
    @Nullable
    public static Entry entryOf(@Nullable RecipeType<?> type) {
        return type == null ? null : byProcess.get(type);
    }

    /** 全量条目快照（只读，顺序不保证）：诊断指令 / 自检用 */
    public static Collection<Entry> entries() {
        return byProcess.values();
    }

    /** 方块 → 它被声明提供的工序（未声明返回 null） */
    @Nullable
    public static Entry entryOfBlock(@Nullable Block block) {
        return block == null ? null : byBlock.get(block);
    }

    /** 解析单个文件：逐字段容错，坏文件不影响整表 */
    @Nullable
    private static Entry parse(ResourceLocation file, JsonElement json) {
        try {
            JsonObject object = json.getAsJsonObject();
            if (!object.has("process")) {
                LOGGER.warn("[akaishi] 第三方认可表 {} 缺少 process 字段，已跳过", file);
                return null;
            }
            ResourceLocation processId = new ResourceLocation(object.get("process").getAsString());
            // 载入时就解析成实际类型：查表变 O(1)，且未知类型能在日志里立刻暴露（而不是运行时静默无效）
            RecipeType<?> process = BuiltInRegistries.RECIPE_TYPE.get(processId);
            if (process == null) {
                LOGGER.warn("[akaishi] 第三方认可表 {} 的配方类型未注册：{}（已跳过该条）", file, processId);
                return null;
            }
            Set<Block> blocks = new LinkedHashSet<>();
            if (object.has("blocks")) {
                for (JsonElement element : object.getAsJsonArray("blocks")) {
                    ResourceLocation id = new ResourceLocation(element.getAsString());
                    Block block = BuiltInRegistries.BLOCK.get(id);
                    if (block == Blocks.AIR) {
                        LOGGER.warn("[akaishi] 第三方认可表 {} 里的方块不存在：{}（已跳过该条）", file, id);
                        continue;
                    }
                    blocks.add(block);
                }
            }
            if (blocks.isEmpty()) {
                LOGGER.warn("[akaishi] 第三方认可表 {} 没有有效的 blocks，已跳过", file);
                return null;
            }
            long chishi = object.has("energy") ? Math.max(0L, object.get("energy").getAsLong()) : DEFAULT_CHISHI;
            long life = object.has("life") ? Math.max(0L, object.get("life").getAsLong()) : 0L;
            long ticks = object.has("ticks") ? Math.max(0L, object.get("ticks").getAsLong()) : DEFAULT_TICKS;
            return new Entry(process, processId, Set.copyOf(blocks), chishi, life, ticks);
        } catch (RuntimeException e) {
            LOGGER.warn("[akaishi] 第三方认可表 {} 解析失败，已跳过：{}", file, e.toString());
            return null;
        }
    }
}
