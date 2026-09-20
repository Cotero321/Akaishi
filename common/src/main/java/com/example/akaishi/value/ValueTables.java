package com.example.akaishi.value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.example.akaishi.config.ModConfig;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 价值分静态表：标签价 / 品质词加成 / 魔法关键词 / 手动覆盖 / 掉落黑名单。
 *
 * <p>数据来自 {@link ModConfig}，列表为空时回退内置默认（0 = 用内置默认的约定一致）。
 * 构造完成后不可变，可安全跨线程读取。
 *
 * <p>纯计算表，不做任何经济兑换，仅用于统一存储库的排序 / 统计 / 筛选。
 */
public final class ValueTables {

    /** 内置标签价值（键为完整标签 id，含 # 前缀） */
    private static final Map<String, Double> BUILTIN_TAG_VALUES = Map.of(
            "#forge:ores", 12.0,
            "#forge:raw_materials", 8.0,
            "#forge:ingots", 10.0,
            "#forge:gems", 25.0,
            "#forge:dusts", 6.0,
            "#forge:nuggets", 2.0,
            "#forge:storage_blocks", 20.0,
            "#minecraft:coals", 5.0,
            // 本 mod 自有中间品标签：矿石基底不是真矿石，避免污染 #forge:ores 被第三方机器误识别
            "#akaishi:ore_bases", 12.0);

    /** 内置品质词加成（对物品 id 子串匹配，命中多个取最高） */
    private static final Map<String, Double> BUILTIN_TIER_BONUS = Map.of(
            "uncommon", 6.0,
            "rare", 14.0,
            "epic", 30.0,
            "legendary", 60.0,
            "mythic", 90.0,
            "divine", 90.0,
            "supreme", 120.0,
            "ultimate", 120.0);

    /** 内置魔法 / 功能类关键词（按 id 词边界匹配，尾部 * 表示允许词根扩展） */
    private static final Set<String> BUILTIN_MAGIC_KEYWORDS = Set.of(
            "scroll", "focus", "spellbook", "grimoire", "tome", "codex", "rune",
            "talisman", "charm", "amulet", "phylactery", "wand", "staff",
            "scepter", "sceptre", "elixir", "incant*");

    /** 内置魔法 / 功能类标签（软依赖，仅按标签 id 判定，不引用第三方类） */
    private static final Set<String> BUILTIN_MAGIC_TAGS = Set.of(
            "#curios:scroll", "#curios:spellbook", "#curios:spellstone",
            "#irons_spellbooks:school_focus", "#irons_spellbooks:inscribed_rune");

    /**
     * 内置关键词豁免：按 id 通配符匹配，命中则跳过品质词与魔法关键词加成。
     *
     * <p>本 mod 的机器等级后缀（ultimate）与 Curios 槽位名（charm）同品质 / 魔法词撞名，
     * 不豁免会把「散热片终极档」「护符饰品」「坐标绑定棒」误判成高品质或魔法物品。
     * 标签命中不受豁免影响（标签为显式语义声明）。
     */
    private static final List<String> BUILTIN_KEYWORD_EXCLUSIONS = List.of(
            "akaishi:*_ultimate",
            "akaishi:*_charm",
            "akaishi:akaishi_life_energy_wand");

    /** 手动覆盖项：tagKey 非空表示 #标签规则，否则 raw 为精确 id 或含 * 的通配符 */
    public record Override(String raw, TagKey<Item> tagKey, double score) {
        public boolean isTag() {
            return tagKey != null;
        }
    }

    private final Map<String, Double> tagValues;
    private final Map<String, Double> tierBonus;
    private final Set<String> magicKeywords;
    private final List<TagKey<Item>> magicTags;
    private final List<Override> overrides;
    private final List<String> lootBlacklist;
    private final List<String> keywordExclusions;

    private ValueTables(Map<String, Double> tagValues, Map<String, Double> tierBonus,
                        Set<String> magicKeywords, List<TagKey<Item>> magicTags,
                        List<Override> overrides, List<String> lootBlacklist,
                        List<String> keywordExclusions) {
        this.tagValues = tagValues;
        this.tierBonus = tierBonus;
        this.magicKeywords = magicKeywords;
        this.magicTags = magicTags;
        this.overrides = overrides;
        this.lootBlacklist = lootBlacklist;
        this.keywordExclusions = keywordExclusions;
    }

    /** 从配置构建（配置列表为空则用内置默认） */
    public static ValueTables fromConfig() {
        List<TagKey<Item>> magicTags = new ArrayList<>();
        for (String raw : BUILTIN_MAGIC_TAGS) {
            TagKey<Item> tag = itemTag(raw);
            if (tag != null) {
                magicTags.add(tag);
            }
        }
        List<String> exclusions = parseList(ModConfig.valueKeywordExclusions);
        return new ValueTables(
                parseTable(ModConfig.valueTagValues, BUILTIN_TAG_VALUES),
                parseTable(ModConfig.valueTierBonus, BUILTIN_TIER_BONUS),
                new HashSet<>(BUILTIN_MAGIC_KEYWORDS),
                List.copyOf(magicTags),
                parseOverrides(ModConfig.valueOverrides),
                parseList(ModConfig.valueLootBlacklist),
                exclusions.isEmpty() ? BUILTIN_KEYWORD_EXCLUSIONS : exclusions);
    }

    /** 标签价值：取该物品命中标签中的最高分 */
    public double tagValue(Item item) {
        if (tagValues.isEmpty()) {
            return 0.0;
        }
        double best = 0.0;
        for (TagKey<Item> tag : new ItemStack(item).getTags().toList()) {
            Double value = tagValues.get("#" + tag.location());
            if (value != null && value > best) {
                best = value;
            }
        }
        return best;
    }

    /** 品质词加成：物品 id 词边界命中多个词时取最高分；命中豁免模式返回 0 */
    public double tierBonus(String itemId) {
        if (tierBonus.isEmpty() || isKeywordExcluded(itemId)) {
            return 0.0;
        }
        double best = 0.0;
        for (Map.Entry<String, Double> entry : tierBonus.entrySet()) {
            if (entry.getValue() > best && keywordMatch(entry.getKey(), itemId)) {
                best = entry.getValue();
            }
        }
        return best;
    }

    /** 是否魔法 / 功能类物品（关键词按词边界匹配或标签命中；豁免仅作用于关键词） */
    public boolean isMagic(Item item, String itemId) {
        if (!isKeywordExcluded(itemId)) {
            for (String keyword : magicKeywords) {
                if (keywordMatch(keyword, itemId)) {
                    return true;
                }
            }
        }
        if (magicTags.isEmpty()) {
            return false;
        }
        ItemStack stack = new ItemStack(item);
        for (TagKey<Item> tag : magicTags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    /** 是否命中关键词豁免（按 id 通配符匹配） */
    public boolean isKeywordExcluded(String itemId) {
        if (itemId == null || keywordExclusions.isEmpty()) {
            return false;
        }
        for (String pattern : keywordExclusions) {
            if (globMatch(pattern, itemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 关键词匹配：按物品 id 的词边界匹配，避免子串误命中。
     *
     * <p>关键词两侧必须是分隔符（{@code _ - . : / 空格}）或串端点；首尾 {@code *}
     * 表示该侧放宽为任意（如 {@code *charm} 匹配任意命名空间下的 charm 词元，
     * {@code incant*} 匹配 incantation 等词根扩展）。
     */
    public static boolean keywordMatch(String keyword, String itemId) {
        if (keyword == null || itemId == null || keyword.isEmpty()) {
            return false;
        }
        boolean headFree = keyword.charAt(0) == '*';
        boolean tailFree = keyword.length() > 1 && keyword.charAt(keyword.length() - 1) == '*';
        String core = keyword.substring(headFree ? 1 : 0, tailFree ? keyword.length() - 1 : keyword.length());
        if (core.isEmpty()) {
            return false;
        }
        int from = 0;
        while (true) {
            int index = itemId.indexOf(core, from);
            if (index < 0) {
                return false;
            }
            int end = index + core.length();
            boolean leftOk = headFree || index == 0 || isSeparator(itemId.charAt(index - 1));
            boolean rightOk = tailFree || end == itemId.length() || isSeparator(itemId.charAt(end));
            if (leftOk && rightOk) {
                return true;
            }
            from = index + 1;
        }
    }

    private static boolean isSeparator(char c) {
        return c == '_' || c == '-' || c == '.' || c == ':' || c == '/' || c == ' ';
    }

    /**
     * 手动覆盖值：<b>精确 id → 通配符 → #标签</b> 三级优先级（与配置注释一致），同级内按配置顺序取首个。
     *
     * <p><b>不能只按列表顺序扫一遍</b>：那样"先写的 #tag 会盖掉后写的精确 id"，
     * 与文档承诺的优先级相反 —— 精确 id 是最具体、最该生效的那一档。
     *
     * @return 命中返回分值，未命中返回 -1（分值恒非负，可作为哨兵）
     */
    public double overrideValue(String itemId, Item item) {
        if (overrides.isEmpty()) {
            return -1.0;
        }
        for (Override entry : overrides) {
            if (!entry.isTag() && entry.raw().indexOf('*') < 0 && entry.raw().equals(itemId)) {
                return entry.score();
            }
        }
        for (Override entry : overrides) {
            if (!entry.isTag() && entry.raw().indexOf('*') >= 0 && globMatch(entry.raw(), itemId)) {
                return entry.score();
            }
        }
        ItemStack stack = null;
        for (Override entry : overrides) {
            if (entry.isTag()) {
                if (stack == null) {
                    stack = new ItemStack(item);
                }
                if (stack.is(entry.tagKey())) {
                    return entry.score();
                }
            }
        }
        return -1.0;
    }

    /** 是否在掉落来源黑名单内（支持 id / 命名空间 / 通配符） */
    public boolean isLootBlacklisted(String itemId) {
        for (String pattern : lootBlacklist) {
            if (pattern.indexOf('*') >= 0) {
                if (globMatch(pattern, itemId)) {
                    return true;
                }
            } else if (itemId.equals(pattern) || itemId.startsWith(pattern + ":")) {
                return true;
            }
        }
        return false;
    }

    /** 解析 "#namespace:path" 为物品标签，非法返回 null */
    public static TagKey<Item> itemTag(String raw) {
        if (raw == null || raw.length() < 2 || raw.charAt(0) != '#') {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(raw.substring(1));
        return id == null ? null : TagKey.create(Registries.ITEM, id);
    }

    /** 通配符匹配：按 * 分段，逐段按顺序在目标串中定位（首段锚头、末段锚尾） */
    public static boolean globMatch(String pattern, String value) {
        if (pattern == null || value == null) {
            return false;
        }
        if (pattern.indexOf('*') < 0) {
            return pattern.equals(value);
        }
        String[] parts = pattern.split("\\*", -1);
        int cursor = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (i == 0) {
                if (!value.startsWith(part)) {
                    return false;
                }
                cursor = part.length();
            } else if (i == parts.length - 1) {
                int index = value.indexOf(part, cursor);
                return index >= 0 && index + part.length() == value.length();
            } else {
                int index = value.indexOf(part, cursor);
                if (index < 0) {
                    return false;
                }
                cursor = index + part.length();
            }
        }
        return true;
    }

    /** 以 '=' 切分为 [键, 值]，无 '=' 或值为空返回 null */
    public static String[] splitPair(String raw) {
        if (raw == null) {
            return null;
        }
        int index = raw.indexOf('=');
        if (index <= 0 || index >= raw.length() - 1) {
            return null;
        }
        String key = raw.substring(0, index).trim();
        String value = raw.substring(index + 1).trim();
        return key.isEmpty() || value.isEmpty() ? null : new String[]{key, value};
    }

    /** 宽松解析 double，失败返回默认值 */
    public static double parseDouble(String raw, double fallback) {
        try {
            double parsed = Double.parseDouble(raw);
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Map<String, Double> parseTable(String[] configured, Map<String, Double> builtin) {
        if (configured == null || configured.length == 0) {
            return builtin;
        }
        Map<String, Double> parsed = new HashMap<>();
        for (String raw : configured) {
            String[] pair = splitPair(raw);
            if (pair == null) {
                continue;
            }
            parsed.put(pair[0].toLowerCase(Locale.ROOT), Math.max(0.0, parseDouble(pair[1], -1.0)));
        }
        return parsed.isEmpty() ? builtin : Map.copyOf(parsed);
    }

    private static List<Override> parseOverrides(String[] configured) {
        if (configured == null || configured.length == 0) {
            return List.of();
        }
        List<Override> parsed = new ArrayList<>();
        for (String raw : configured) {
            String[] pair = splitPair(raw);
            if (pair == null) {
                continue;
            }
            double score = parseDouble(pair[1], -1.0);
            if (score < 0) {
                continue;
            }
            String key = pair[0].toLowerCase(Locale.ROOT);
            TagKey<Item> tag = itemTag(key);
            if (key.charAt(0) == '#') {
                if (tag != null) {
                    parsed.add(new Override(key, tag, score));
                }
            } else {
                parsed.add(new Override(key, null, score));
            }
        }
        return List.copyOf(parsed);
    }

    private static List<String> parseList(String[] configured) {
        if (configured == null || configured.length == 0) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (String raw : configured) {
            if (raw == null) {
                continue;
            }
            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (!value.isEmpty()) {
                parsed.add(value);
            }
        }
        return List.copyOf(parsed);
    }
}
