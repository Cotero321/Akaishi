package com.example.akaishi.item.curio;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「禁忌」四件 tooltip 的文字动效（D259）。
 *
 * <p>可行的原理：{@code GuiGraphics#renderTooltip} 每帧都会重新调用
 * {@code ItemStack#getTooltipLines}，我们注入的行因此能随时钟变化；且 1.20.1 字体支持
 * 任意 24 位真彩（{@link TextColor#fromRgb}），故可做平滑渐变而非 16 色跳变。
 * 原版 tooltip 不提供位移/缩放/透明度控制，动效只能落在「文字内容与颜色」这一层。</p>
 *
 * <ul>
 *   <li>{@link #whispering}：低语行，暗红↔暗紫色带沿字序流动（色相变化）；</li>
 *   <li>{@link #crawling}：其余说明行，保留 lang/样式自带的颜色语义，仅让明度波沿字序传播，
 *       形成「文字在蠕动」的观感，同时不牺牲可读性。</li>
 * </ul>
 *
 * <p>性能：tooltip 每帧求值，逐字符上色会产生大量短命对象。这里按「量化时间 + 原文」缓存结果，
 * 时间量化到 {@link #TIME_STEP_MS}（10 次/秒已足够顺滑），使绝大多数帧命中同一组件实例。
 * 服务端路径调用同样安全：纯字符处理，无副作用。</p>
 */
public final class AkaishiTooltipFx {

    /** 动效时间量化步长（毫秒） */
    private static final long TIME_STEP_MS = 100L;

    /** 缓存条目上限；文本集合本身有限，超限直接清空即可，无需 LRU */
    private static final int CACHE_LIMIT = 256;

    /** 低语色带阶数 */
    private static final int BAND_SIZE = 64;

    /** 低语色带锚点（首尾同色以保证循环无缝）：暗紫 → 暗酒红 → 深紫 → 暗紫 */
    private static final int[] WHISPER_ANCHORS = {0xAA00AA, 0x8B0A2A, 0x4A0A3A, 0xAA00AA};

    /** 低语色带每 220ms 推进一阶（整条色带循环约 14 秒） */
    private static final int WHISPER_MS_PER_STEP = 220;

    /** 低语色带上每字符滞后的阶数（波长 ≈ 32 字符） */
    private static final int WHISPER_STEPS_PER_CHAR = 2;

    /** 蠕动明度波动范围：下限保证暗色文本仍可辨，上限避免过曝 */
    private static final double CRAWL_MIN = 0.55;
    private static final double CRAWL_MAX = 1.18;

    /** 每字符相位滞后（弧度），决定波在行内的「蠕行」速度 */
    private static final double CRAWL_PHASE_PER_CHAR = 0.55;

    /** 时间→相位换算：每 120ms 推进 1 弧度 */
    private static final double CRAWL_RADIANS_PER_MS = 1.0 / 120.0;

    private static final TextColor FALLBACK = TextColor.fromRgb(0xAAAAAA);

    private static final TextColor[] WHISPER_BAND = buildBand();

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private AkaishiTooltipFx() {
    }

    /** 低语行：暗红↔暗紫色带沿字序流动（斜体由自身样式决定） */
    public static Component whispering(Component source) {
        return render(source, true);
    }

    /** 说明行：保留原色语义，明度波沿字序传播（蠕动感） */
    public static Component crawling(Component source) {
        return render(source, false);
    }

    private static Component render(Component source, boolean whisper) {
        String raw = source.getString();
        if (raw.isEmpty()) {
            return source;
        }
        long stamp = System.currentTimeMillis() / TIME_STEP_MS;
        String key = (whisper ? "w\u0000" : "c\u0000") + raw;
        CacheEntry hit = CACHE.get(key);
        if (hit != null && hit.stamp() == stamp) {
            return hit.text();
        }
        Component built = build(raw, source.getStyle(), whisper, stamp);
        if (CACHE.size() >= CACHE_LIMIT) {
            CACHE.clear();
        }
        CACHE.put(key, new CacheEntry(stamp, built));
        return built;
    }

    /**
     * 逐字符着色：解析 {@code §} 格式码维护当前段颜色与斜体状态，
     * 保证文本宽度与原文一致（格式码不占可见字符），相位按可见字符计数推进。
     */
    private static Component build(String raw, Style baseStyle, boolean whisper, long stamp) {
        TextColor baseColor = baseStyle.getColor() != null ? baseStyle.getColor() : FALLBACK;
        TextColor segment = baseColor;
        boolean italic = baseStyle.isItalic();
        MutableComponent out = Component.empty();
        int index = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == ChatFormatting.PREFIX_CODE && i + 1 < raw.length()) {
                ChatFormatting fmt = ChatFormatting.getByCode(raw.charAt(i + 1));
                if (fmt != null) {
                    if (fmt == ChatFormatting.RESET) {
                        segment = baseColor;
                        italic = baseStyle.isItalic();
                    } else if (fmt == ChatFormatting.ITALIC) {
                        italic = true;
                    } else {
                        Integer rgb = fmt.getColor();
                        if (rgb != null) {
                            segment = TextColor.fromRgb(rgb);
                        }
                    }
                    i++;
                    continue;
                }
            }
            TextColor color = whisper
                    ? WHISPER_BAND[bandIndex(stamp, index)]
                    : pulse(segment, stamp, index);
            boolean italicChar = italic || whisper; // 低语统一斜体，贴合"低语"语义
            out.append(Component.literal(String.valueOf(c))
                    .withStyle(style -> style.withColor(color).withItalic(italicChar)));
            index++;
        }
        return out;
    }

    /** 低语色带索引：整体随钟表推进，行内逐字符滞后，形成流动色带 */
    private static int bandIndex(long stamp, int index) {
        int base = (int) (stamp * TIME_STEP_MS / WHISPER_MS_PER_STEP);
        int idx = (base - index * WHISPER_STEPS_PER_CHAR) % BAND_SIZE;
        return idx < 0 ? idx + BAND_SIZE : idx;
    }

    /** 明度蠕动：以基色为中心做正弦明度缩放，沿字序滞后形成波 */
    private static TextColor pulse(TextColor base, long stamp, int index) {
        double wave = Math.sin(stamp * TIME_STEP_MS * CRAWL_RADIANS_PER_MS - index * CRAWL_PHASE_PER_CHAR);
        double factor = CRAWL_MIN + (wave + 1.0) * 0.5 * (CRAWL_MAX - CRAWL_MIN);
        return TextColor.fromRgb(scale(base.getValue(), factor));
    }

    /** 由锚点均分插值出色带 */
    private static TextColor[] buildBand() {
        TextColor[] band = new TextColor[BAND_SIZE];
        int segments = WHISPER_ANCHORS.length - 1;
        for (int i = 0; i < BAND_SIZE; i++) {
            double pos = (double) i / BAND_SIZE * segments;
            int seg = Math.min((int) pos, segments - 1);
            band[i] = TextColor.fromRgb(lerp(WHISPER_ANCHORS[seg], WHISPER_ANCHORS[seg + 1], pos - seg));
        }
        return band;
    }

    /** 明度缩放（等价于朝黑色插值，自带越界钳制） */
    private static int scale(int rgb, double factor) {
        return lerp(0x000000, rgb, factor);
    }

    /** 通道级线性插值，结果自动钳制到 [0, 255] */
    private static int lerp(int from, int to, double t) {
        int r = channel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = channel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = channel(from & 0xFF, to & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, double t) {
        return Mth.clamp((int) (from + (to - from) * t), 0, 255);
    }

    private record CacheEntry(long stamp, Component text) {
    }
}
