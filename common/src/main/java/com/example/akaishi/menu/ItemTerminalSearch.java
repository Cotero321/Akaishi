package com.example.akaishi.menu;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.icu.text.Transliterator;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 库页搜索匹配：本地化名称（含中文）+ <b>拼音首字母</b> + 注册名（含命名空间与路径）。
 * <p>
 * 匹配规则对齐 AE2 的 {@code RepoSearch}：{@code |} 分组（组间 OR），组内空格分词（词间 AND）；
 * 单个词做子串匹配。与 AE2 的差异：AE2 只匹配名称子串，<b>不做拼音</b>，本类额外支持拼音首字母
 * （「钻石」→ {@code zs}）。
 * <p>
 * 拼音用 Minecraft 自带的 ICU4J（{@code com.ibm.icu}）做 汉字→拉丁 转写，再取每个音节的首字母。
 * 转写开销大（每次调用都在做整串转换），因此按「名称串 → 首字母」「物品+名称 → 可搜索串」两级缓存；
 * 缓存按规则用 {@link ConcurrentHashMap}，并设容量上限防长期膨胀。
 */
public final class ItemTerminalSearch {

    private static final Logger LOG = LoggerFactory.getLogger("akaishi-item-terminal-search");
    /** 缓存容量上限：超出即整体清空（条目数有限，正常不会触及） */
    private static final int CACHE_LIMIT = 4096;
    private static final Map<String, String> PINYIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> HAYSTACK_CACHE = new ConcurrentHashMap<>();
    /** ICU 转写器（构建一次复用），不可用时为 null */
    private static volatile Transliterator transliterator;
    private static volatile boolean transliteratorFailed;

    private ItemTerminalSearch() {
    }

    /** 过滤条目表：查询为空时直接返回原表（省一次复制） */
    public static List<AkaishiItemTerminalSync.Entry> filter(
            List<AkaishiItemTerminalSync.Entry> entries, String query) {
        String trimmed = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return entries;
        }
        String[] orGroups = trimmed.split("\\|");
        List<AkaishiItemTerminalSync.Entry> out = new ArrayList<>(entries.size());
        for (AkaishiItemTerminalSync.Entry entry : entries) {
            if (matchesAnyGroup(entry, orGroups)) {
                out.add(entry);
            }
        }
        return out;
    }

    private static boolean matchesAnyGroup(AkaishiItemTerminalSync.Entry entry, String[] orGroups) {
        String haystack = haystack(entry);
        for (String group : orGroups) {
            boolean allTermsHit = true;
            for (String term : group.trim().split("\\s+")) {
                if (!term.isEmpty() && !haystack.contains(term)) {
                    allTermsHit = false;
                    break;
                }
            }
            if (allTermsHit) {
                return true;
            }
        }
        return false;
    }

    /**
     * 条目的可搜索串：名称 + 拼音首字母 + 注册名，三者用分隔符拼成一条，之后每个搜索词只需一次
     * {@code contains}，避免逐词重复做名称解析与转写。
     */
    private static String haystack(AkaishiItemTerminalSync.Entry entry) {
        Item item = entry.display().getItem();
        String name = entry.display().getHoverName().getString();
        String cacheKey = BuiltInRegistries.ITEM.getKey(item) + "\u0000" + name;
        String cached = HAYSTACK_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String built = (name + '\n' + pinyinAid(name) + '\n'
                + BuiltInRegistries.ITEM.getKey(item)).toLowerCase(Locale.ROOT);
        if (HAYSTACK_CACHE.size() >= CACHE_LIMIT) {
            HAYSTACK_CACHE.clear();
        }
        HAYSTACK_CACHE.put(cacheKey, built);
        return built;
    }

    /**
     * 名称 → 拼音检索串（<b>首字母 + 全拼</b>，空格分隔），例如「钻石」→ {@code "zs zuanshi"}。
     * <p>
     * 首字母用于极简输入（{@code zs}），全拼（去声调）用于常规输入（{@code zuan} / {@code zuanshi}）。
     * 非汉字片段原样保留，因此英文名也能吃到同一套（{@code Diamond} → {@code "d diamond"}）。
     * <p>
     * ICU 不可用时返回空串：只损失拼音检索，中文/英文/注册名检索不受影响（不给玩家报错，只记日志）。
     */
    private static String pinyinAid(String name) {
        String cached = PINYIN_CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        String result = "";
        if (!transliteratorFailed) {
            try {
                Transliterator t = transliterator;
                if (t == null) {
                    t = Transliterator.getInstance("Han-Latin");
                    transliterator = t;
                }
                StringBuilder initials = new StringBuilder();
                StringBuilder full = new StringBuilder();
                // Han-Latin 输出以空格分音节（"钻石" → "zuàn shí"），取每音节首字母得首字母串，
                // 去掉声调后拼接得全拼串
                for (String syllable : t.transliterate(name).split("\\s+")) {
                    if (syllable.isEmpty()) {
                        continue;
                    }
                    String plain = stripToneMarks(syllable).toLowerCase(Locale.ROOT);
                    if (plain.isEmpty()) {
                        continue;
                    }
                    initials.append(plain.charAt(0));
                    full.append(plain);
                }
                result = initials + " " + full;
            } catch (Throwable e) {
                // 捕获 Error：类缺失时抛的是 NoClassDefFoundError，不能让搜索把界面带崩
                transliteratorFailed = true;
                LOG.warn("[IT] 拼音转写不可用，退化为名称/注册名匹配", e);
            }
        }
        if (PINYIN_CACHE.size() >= CACHE_LIMIT) {
            PINYIN_CACHE.clear();
        }
        PINYIN_CACHE.put(name, result);
        return result;
    }

    /** 去声调：先做 NFD 分解，再剥掉组合用记号（{@code zuàn} → {@code zuan}） */
    private static String stripToneMarks(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
