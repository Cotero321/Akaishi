package com.example.akaishi.menu;

import com.example.akaishi.codex.CodexFamily;
import com.example.akaishi.codex.CodexNode;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 禁忌秘典的<b>专属渲染框架</b>：秘典里的一切绘制都从这里出，屏幕/画布/内容层只负责
 * "数据准备 + 摆位置 + 命中判定"，<b>不再直接写零散 {@code fill}</b>。
 *
 * <p><b>为什么这一层要取消缩放（本轮核心）</b>：上一版把"设计基准 720×400"整体
 * {@code translate + scale} 到实际屏幕，但 MC 的 {@code Screen#width/height} 是<b>已被 GUI 缩放除过</b>
 * 的逻辑空间（GUI 缩放 4 时只有 480×270），于是实际比例落到 0.58 这类<b>非整数</b>，
 * 字体被非整数重采样 —— 观感就是"文字飘忽、发虚"。因此本轮<b>删掉整套视图变换</b>：
 * <ul>
 *   <li>所有几何直接用<b>实际 GUI 坐标</b>算，<b>一律整数</b>（不出现 {@code Math.round(坐标 * scale)}）；</li>
 *   <li>字号与行高保持项目上限（字体 9px、{@link #LINE_H} = 10），<b>文字永远 1:1</b>；</li>
 *   <li>书体尺寸改为<b>按内容定</b>（见 {@link #entryBook}/{@link #chartBook}），
 *       上限 {@link AkaishiCodexMenu#PANEL_W}/{@link AkaishiCodexMenu#PANEL_H}（720×400）；
 *       屏幕装不下时<b>不缩放</b>，而是收窄书体高度、靠页内滚动消化多余内容。</li>
 * </ul>
 * 因此本类里<b>不存在任何 {@code pose().scale(...)}</b>；仅有的 pose 操作是状态暗罩抬 z
 * （{@link AkaishiCodexEmblem} 里的 {@code translate(0,0,300)}），与像素密度无关。
 *
 * <p><b>API 分四组</b>：① {@link #entryBook}/{@link #chartBook} 与
 * {@link #pageArea}/{@link #textArea}/{@link #chartArea}/{@link #tabArea}/{@link #footerBack} 等
 * <b>版面几何唯一真源</b>；② {@link #book}/{@link #sheetHead}/{@link #pageHead}/{@link #prose}/
 * {@link #folio}/{@link #recipePage} 书页排版；③ {@link #tab}/{@link #link}/{@link #tooltip} 控件；
 * ④ {@link #familyColor}/{@link #inked}/{@link #pulse}/{@link #texturePresent} 小工具。
 *
 * <p><b>贴图优先、缺图回落</b>：书底优先读 {@code textures/gui/codex/book_frame.png}，
 * 徽记优先读 {@code textures/gui/codex/emblem/<节点路径>.png}；读不到一律走
 * {@link AkaishiCodexBookArt} 的程序化笔触，<b>绝不 blit 不存在的贴图</b>（那会被画成缺图马赛克）。
 *
 * <p><b>书底改为分段拉伸（本轮核心）</b>：贴图是"跨页整幅"（书脊在正中），整幅拉伸会把内框线
 * 拉到 {@code 书体宽 × 42/512}，与固定页边距对不上 ⇒ 左页文字越框。现在按
 * <b>横向 5 段（左边框 | 左页纸 | 书脊 | 右页纸 | 右边框）/ 纵向 3 段（上边框 | 纸面 | 下边框）</b>
 * 拉伸，边框与书脊宽度固定，<b>只有两页纸面被拉伸</b>；边框厚度由书体宽反算（{@link #borderFor}），
 * 于是<b>框线在屏幕上的位置恒等于 {@code borderFor(书体宽)}</b>，
 * 版心（{@link #textArea}）由它加内边距推出并自检钳制 ⇒ <b>永不越框</b>。
 *
 * <p>本类只管"画"，不读存档、不做条件判定、不管页码 —— 那些是屏幕与内容层的事。
 * 全部尺寸/配色/周期都提为常量并标"待调手感值"。
 */
final class AkaishiCodexRender {

    // ===== 书体尺寸（按内容定；上限 = 菜单里的设计基准；待调手感值） =====

    /** 书体尺寸上限（超上限一律截断并由页内滚动消化） */
    static final int MAX_BOOK_W = AkaishiCodexMenu.PANEL_W;
    static final int MAX_BOOK_H = AkaishiCodexMenu.PANEL_H;
    /** 书体尺寸下限（太短的一段文案也不该得到一本袖珍书） */
    private static final int MIN_BOOK_W = 320;
    private static final int MIN_BOOK_H = 160;
    /**
     * 摊开的双页书体宽（条目页：不随内容变，窄屏才被压窄）。
     *
     * <p>口径自洽：{@code borderFor(356) = round(356×0.06) = 21}；书脊 = {@code round(12×21/43) = 6}；
     * 框线内宽 = 356 − 42 = 314 ⇒ 两页纸各 (314−6)/2 = <b>154</b>；
     * 每页版心 = 154 − 10(外留白+滚动条) − 13(留白+装订余量) = <b>131</b>，可用行宽 121 —— 与上一版 124 同量级。
     */
    private static final int OPEN_BOOK_W = 356;
    /** 书体离屏幕左右边至少留的边距 */
    private static final int EDGE_MARGIN = 8;
    /** 顶部留给标题带的高度 / 底部留给页脚控件带的高度（书体垂直定位用） */
    private static final int TOP_RESERVE = 24;
    private static final int BOTTOM_RESERVE = 28;

    // ===== 双页书几何（全部由"框线"反算；待调手感值） =====

    /** 正文区右侧给滚动条让出的宽（含间隙；滚动条画在版心<b>之外</b>、仍在留白之内） */
    private static final int SCROLL_W = 4;
    private static final int SCROLL_GAP = 2;
    /**
     * 行尾留白：<b>可用行宽 = 版心宽 − 它</b>（"版心宽"与"可用行宽"分开定义）。
     *
     * <p>两件事都要它：① 正文不该顶到版心边界；② 中文"避头尾"允许把行首禁则标点留在上一行
     * （见 {@code AkaishiCodexPages} 的断行），那一行最多再宽出一个汉字（9px）——留白 ≥ 一个汉字宽，
     * 于是"吸标点"后的行也仍在正文区之内，绝不会挤到滚动条或纸缘上。
     */
    static final int LINE_TAIL_PAD = 10;
    /** 版心的最小可用宽（极端窄屏的兜底；再窄也按它算，行会被截断而不是越界） */
    private static final int MIN_LINE_W = 24;
    /** 版心的最小可读宽（{@link #fitInside} 自检用；书体宽 ≥ {@link #MIN_BOOK_W} 时恒可达） */
    private static final int MIN_READABLE_W = 60;

    /**
     * <b>边框系数</b>（待调手感值）：框线在屏幕上距书体边缘的距离，按书体宽取比例再夹上下限。
     *
     * <p>贴图里框线距四边 42px（占宽 8.20%），照搬会让 356 宽的书留下 29px 边框（吃掉版心）；
     * 用户已拍板"边框厚度可按书体尺寸缩一缩（比例更匀）"，故取 6.0% 再夹 [12, 28]。
     * 该值<b>只由书体宽决定</b>（同一尺寸下恒定，也不与"按内容定高"循环依赖），
     * 版心/书体高全部由它反算 ⇒ <b>框线位置在屏幕上恰等于 {@code borderFor(书体宽)}</b>。
     */
    private static final double BORDER_RATIO = 0.06;
    private static final int BORDER_MIN = 12;
    private static final int BORDER_MAX = 28;

    /** 版心到框线内侧的留白（外侧；还要再让出滚动条宽，见 {@link #textArea}） */
    private static final int MARGIN = 4;
    /** 靠书脊一侧在 {@link #MARGIN} 之外<b>另加</b>的装订余量（两页各自独立算；待调手感值） */
    private static final int SPINE_GUTTER = 9;

    /** 页眉基线 / 页眉下规律线 / 正文顶（都相对"框线内"的纸面顶边；待调手感值） */
    private static final int HEAD_INSET = 6;
    private static final int RULE_INSET = 18;
    private static final int HEAD_BAND = 24;
    /** 正文底到框线内下缘的距离（叶号就画在这一带里，故正文永不压叶号） */
    private static final int FOLIO_BAND = 13;
    /** 行高：<b>项目既有上限，永不缩放</b> */
    static final int LINE_H = 10;
    /** 叶号距框线内下缘的距离，以及从纸面外侧向内缩进的量（待调手感值） */
    private static final int FOLIO_BOTTOM = 11;
    private static final int FOLIO_INSET = 10;
    /** 图版（画布态）四边留白与顶部页眉行高 */
    private static final int SHEET_PAD = 10;
    private static final int SHEET_HEAD_H = 20;
    private static final int SHEET_HEAD_TEXT_Y = 5;
    private static final int SHEET_HEAD_RULE_Y = 17;
    /** 书签带：宽 / 高（颜色 = 当前族色），以及它距框线内侧的距离 */
    private static final int RIBBON_W = 8;
    private static final int RIBBON_H = 76;
    private static final int RIBBON_INSET = 6;
    /**
     * 内框线的补画色（半透明墨色；待调手感值）。
     *
     * <p>贴图里那根框线只有 <b>1px</b>，而固定段是 43px 源像素 → 21px 屏幕像素的<b>下采样</b>，
     * 按最近邻采样它<b>可能整根被跳过</b>（实测口径：书体宽 356 时固定段 43→21，
     * 最后一个目标像素取到源像素 41 而非线所在的 42）。故在"框线应在的位置"
     * （= 离书体边缘 {@code borderFor(书体宽)} 处）补画一根同位置的线：
     * 采样到了就重叠（同色系、只是略深），没采样到就补上 ⇒ <b>框线永远可见、位置永远精确</b>。
     */
    private static final int FRAME_LINE = 0x553A2E22;

    // ===== 书页内动作按钮（条件页底部；待调手感值） =====

    static final int ACTION_H = 14;
    private static final int ACTION_GAP = 6;
    /** 动作按钮在正文区里占掉的行数（算书体高度时给条件页让位，故不会被按钮压字） */
    static final int ACTION_ROWS = (ACTION_H + ACTION_GAP + LINE_H - 1) / LINE_H;

    // ===== 书签式分类标签（窄条贴边；用户已定规格） =====

    /** 书签宽 × 高（用户拍板 <b>18×40</b>；只放一枚徽记，族名改走悬停提示） */
    static final int TAB_W = 18;
    static final int TAB_H = 40;
    /** 书签垂直间距（用户拍板 3~4，取 4） */
    static final int TAB_GAP = 4;
    /** 选中那枚向左（书外）突出的距离（用户拍板 4；绘制与命中共用 {@link #tabVisual}） */
    private static final int TAB_LIFT = 4;
    /** 书签内的徽记边长（16×16 居中；缺图回落的物品图标同样是 1:1 的 16×16，不缩放） */
    private static final int TAB_ICON = 16;
    private static final int TAB_EDGE = 0x803A2E22;
    private static final int TAB_EDGE_ON = 0xFF3A2E22;
    /** 未选中标签的压暗系数（保色相，只是"没翻开"） */
    private static final float TAB_DIM_MIX = 0.55f;

    // ===== 页脚控件（书体下方居中；自绘书页风格，故不依赖任何面板底） =====

    private static final int FOOTER_H = 14;
    /** 书体下缘到页脚控件的间隙 */
    private static final int FOOTER_GAP = 6;
    private static final int FOOTER_BACK_W = 64;
    private static final int FOOTER_NAV_W = 16;
    private static final int FOOTER_TEXT_W = 64;
    private static final int FOOTER_ITEM_GAP = 6;
    /** 书页风格按钮：深墨底 + 浅描边 + 浅字（默认态 / 置灰态） */
    private static final int BTN_BASE = 0xF02A2118;
    private static final int BTN_BASE_OFF = 0xB01C1812;
    private static final int BTN_EDGE = 0xFF6E5A3E;
    private static final int BTN_EDGE_OFF = 0xFF4A4034;
    private static final int BTN_TEXT = 0xFFEFE4C8;
    private static final int BTN_TEXT_OFF = 0xFF8C8272;
    /** 书体之外的一行文字（标题 / 画布提示）：压暗背景下必须用浅色，深墨会看不见 */
    static final int OUTSIDE_INK = 0xFFE8DCC2;
    static final int OUTSIDE_INK_DIM = 0xB0C8BCA2;

    // ===== 连线（待调手感值） =====

    /** 拐角圆化半径 */
    private static final int LINK_CORNER = 4;
    /** 挡路连线（粗）与常态连线（细）的线宽 */
    static final int LINK_W_THICK = 2;
    static final int LINK_W_THIN = 1;
    /** 箭头长度与半宽 */
    private static final int ARROW_LEN = 9;
    private static final int ARROW_HALF = 4;
    /** 箭头尾巴离目标徽记中心的距离（≈ 徽记半径 + 3，故不遮图案；与画布的徽记尺寸对齐） */
    private static final int ARROW_GAP = 20;

    // ===== 配方页（待调手感值） =====

    /** 槽位边长与槽距（照原版 18px 槽 + 2px 缝） */
    private static final int SLOT = 18;
    private static final int SLOT_GAP = 2;
    /** 产物槽与网格之间的箭头宽 */
    private static final int CRAFT_ARROW_W = 18;
    /** 网格整体在正文区里的顶部偏移 */
    private static final int CRAFT_TOP = 12;

    // ===== 纸面笔触配色（待调手感值） =====

    private static final int RULE_COLOR = 0x337A6A56;
    private static final int SCROLL_TRACK = 0x30A89A78;
    private static final int SCROLL_HANDLE = 0xB08A7A5C;
    /** 可点击行（关联页 / 可跳转条件）的提示色（提示是深底，故用亮色） */
    private static final int TIP_JUMP = 0xFF9FD0FF;
    private static final int TIP_DIM = 0xFF9A94A8;
    /** 纹章配色里的族色族（族色原色只给标签/亮框/提示；纸面连线一律经 {@link AkaishiCodexBookArt#ink} 压暗） */
    private static final int COLOR_FAMILY_UNKNOWN = 0xFF8A8A95;
    private static final float INK_MIX = 0.62f;
    /** 连线三档透明度：挡路高亮 / 常态 / 被挡节点的其余入线（待调手感值） */
    static final int LINK_ALPHA_HIGHLIGHT = 0xE8;
    static final int LINK_ALPHA_NORMAL = 0xB0;
    static final int LINK_ALPHA_DIM = 0x38;

    // ===== 贴图（建模侧并行产出；本轮只读，不新增资产） =====

    /** 整幅书底贴图（皮革封边 + 纸面 + 内框线；<b>分段拉伸</b>覆盖书体，见 {@link #blitBookTexture}） */
    private static final ResourceLocation TEX_BOOK_FRAME =
            new ResourceLocation("akaishi", "textures/gui/codex/book_frame.png");
    /** 书底贴图原尺寸（<b>实测</b> 512×288 RGBA；切片边界即按这张图的像素坐标给出） */
    private static final int TEX_BOOK_W = 512;
    private static final int TEX_BOOK_H = 288;
    /**
     * <b>切片边界（对 512×288 原图逐行/逐列实测得出，非估算）</b>。
     *
     * <p><b>复现口径</b>：解码 PNG 后取中行 y=144 与中列 x=256 的亮度剖面 ——
     * 皮革段亮度 ≈ 20~70、纸面 ≈ 230、框线是纸面上的一根暗线（≈172）、书脊是纸面上的对称暗带；
     * 逐像素找"亮度突变处"即得下列边界（脚本已按临时文件纪律删除，需要时按此口径重写）。
     * <ul>
     *   <li><b>皮革封边</b>：四边各 20px（x=0..19 / x=492..511 / y=0..19 / y=268..287）；</li>
     *   <li><b>内框线</b>：x=42..469、y=42..245（<b>1px</b> 的线；框线外沿距四边 42px，
     *       即 (512−428)/2 = 42、(288−204)/2 = 42 —— 恰好居中，故左右/上下天然对称）；</li>
     *   <li><b>书脊折痕</b>：x=250..261（最暗在 255/256 ＝图宽正中，两侧渐亮），纵向贯穿纸面。</li>
     * </ul>
     * <p><b>为什么固定段取 {@link #SLICE_EDGE} = 43 而不是 42</b>：42 是框线的<b>外沿</b>，
     * 要连那 1px 线一起"固定不拉伸"，固定段必须吃到 43px，线才会整根落在固定段里
     * （⇒ 屏幕上框线距边恒为 {@code borderFor(书体宽)}，可精确计算）。
     * 校验：43 + 207 + 12 + 207 + 43 = 512；43 + 202 + 43 = 288。
     */
    private static final int SLICE_EDGE = 43;
    private static final int SLICE_SPINE_L = 250;
    private static final int SLICE_SPINE_R = 262;
    /** 书脊折痕带在源图里的宽（固定段：不拉伸，屏幕上按同一边框系数缩放） */
    private static final int SLICE_SPINE_W = SLICE_SPINE_R - SLICE_SPINE_L;
    /** 徽记贴图目录前缀与约定尺寸（32×32，透明底） */
    static final String TEX_EMBLEM_DIR = "textures/gui/codex/emblem/";
    static final int TEX_EMBLEM_SIZE = 32;

    /** 贴图存在性缓存（键 = 贴图 id；资源重载会换掉 ResourceManager 实例 ⇒ 整表失效重探） */
    private static final Map<ResourceLocation, Boolean> PRESENT = new ConcurrentHashMap<>();
    private static ResourceManager cachedManager;

    private AkaishiCodexRender() {
    }

    /** 屏幕坐标（= 实际 GUI 坐标）里的矩形；命中判定与绘制共用同一份几何 */
    record Area(int x, int y, int w, int h) {

        int right() {
            return x + w;
        }

        int bottom() {
            return y + h;
        }

        int cx() {
            return x + w / 2;
        }

        int cy() {
            return y + h / 2;
        }
    }

    /** 命中判定（鼠标是浮点，矩形是整数；一律整数比较，不做任何坐标换算） */
    static boolean in(double mouseX, double mouseY, Area area) {
        return mouseX >= area.x() && mouseX < area.right()
                && mouseY >= area.y() && mouseY < area.bottom();
    }

    /**
     * 按矩形开裁剪。{@code gui.enableScissor} 吃的是<b>屏幕坐标</b>，而本界面所有几何本来就是屏幕坐标
     * （取消视图变换的收益之一），故这里直接透传。
     */
    static void scissor(GuiGraphics gui, Area area) {
        gui.enableScissor(area.x(), area.y(), area.right(), area.bottom());
    }

    static void unscissor(GuiGraphics gui) {
        gui.disableScissor();
    }

    // ===== 书体尺寸（按内容定，最大 720×400） =====

    /**
     * <b>边框厚度</b>（= 框线在屏幕上距书体边缘的距离，见 {@link #BORDER_RATIO}）。
     *
     * <p>只吃书体宽 ⇒ 与"按内容定高"不构成循环；同一书体尺寸下恒定。
     * 版心、书体高、书脊缝宽全部由它反算，故框线位置永远可精确算出（就落在 {@code x + borderFor(w)}）。
     */
    static int borderFor(int bookW) {
        return clampSize((int) Math.round(bookW * BORDER_RATIO), BORDER_MIN, BORDER_MAX);
    }

    /** 书体的横向五段切分结果（边框 / 一页纸宽 / 书脊缝宽） */
    private record Split(int border, int paperW, int spineW) {
    }

    /**
     * 把书体横切成五段：左边框 | 左页纸 | 书脊 | 右页纸 | 右边框。
     *
     * <p>书脊宽按<b>同一边框系数</b>缩放后取整（固定段，不拉伸），再做 0/1px 的奇偶修正，
     * 使两页纸宽<b>严格相等</b>（这是"左右两页必须对称"的落点）。
     */
    private static Split split(Area book) {
        int b = borderFor(book.w());
        int inner = Math.max(2, book.w() - 2 * b);
        int spine = Math.max(2, Math.round(SLICE_SPINE_W * b / (float) SLICE_EDGE));
        if (((inner - spine) & 1) != 0) {
            spine++;
        }
        spine = Math.min(spine, Math.max(2, inner - 2));
        return new Split(b, Math.max(1, (inner - spine) / 2), spine);
    }

    /**
     * 一页正文需要 {@code rows} 行时，书体的高度：两侧边框 + 页眉带 + rows×行高 + 叶号带。
     *
     * <p>这是"书体高度按内容定"的唯一公式；调用方给的是<b>本节点所有可见页的最大需求行数</b>
     * （条件页还要加上 {@link #ACTION_ROWS} 给动作按钮让位），故同一节点翻页时高度恒定。
     * <b>两侧边框也要算进来</b>，否则页眉/叶号会压到框线或皮革上。
     */
    static int entryBookHeight(int bookW, int rows) {
        return 2 * borderFor(bookW) + HEAD_BAND + Math.max(1, rows) * LINE_H + FOLIO_BAND;
    }

    /** 条目页书体宽：<b>常量</b>（不随内容变）；窄屏时会被压到可用宽。
     *  口径自洽校验：{@code borderFor(356) = 21}、书脊 6、两页纸各 154 ⇒ 42 + 6 + 308 = 356。 */
    static int entryBookWidth(int screenW) {
        return clampSize(OPEN_BOOK_W, MIN_BOOK_W, maxBookWidth(screenW));
    }

    /**
     * 给定书体宽时，一页真正能画正文的<b>可用行宽</b>（版心再扣行尾留白 {@link #LINE_TAIL_PAD}）。
     *
     * <p><b>为什么要单独暴露它</b>：内容层要按这个宽做换行，而"书体高度按内容行数定"是反过来的
     * （书体高 ← 行数 ← 换行宽 ← 书体宽）。书体宽只由屏宽决定、与行数无关，
     * 于是调用顺序是"先算宽 → 再按这个宽算行数 → 再算高"，不存在循环。
     * 这里用一根探针书体调 {@link #textArea}，故与真正绘制时<b>同源</b>（不会两处口径漂移）。
     */
    static int pageTextWidth(int bookW) {
        Area probe = new Area(0, 0, bookW, MIN_BOOK_H);
        return Math.max(MIN_LINE_W, textArea(probe, 0).w() - LINE_TAIL_PAD);
    }

    /** 可用行宽（{@link #textArea} 再扣行尾留白）：内容层换行与绘制<b>共用这一口径</b> */
    static int lineWidth(Area book, int side) {
        return Math.max(MIN_LINE_W, textArea(book, side).w() - LINE_TAIL_PAD);
    }

    /** 条目页书体：宽由屏宽定，高按内容行数定 */
    static Area entryBook(int screenW, int screenH, int bookW, int rows) {
        int h = clampSize(entryBookHeight(bookW, rows), MIN_BOOK_H, maxBookHeight(screenH));
        return place(screenW, screenH, bookW, h);
    }

    /**
     * 画布态书体：宽高都按"该族节点包围盒 + 图版留白 + 两侧边框"定（受同一套上下限约束）。
     *
     * <p>边框宽度反过来依赖书体宽，故先按 {@link #BORDER_MIN} 估一版宽、再按真实边框定稿
     * （边框系数很小，估一次就够；定稿后 {@link #chartArea} 的视口宽恰好 = 包围盒宽）。
     */
    static Area chartBook(int screenW, int screenH, int contentW, int contentH) {
        int guess = clampSize(contentW + 2 * (SHEET_PAD + BORDER_MIN),
                MIN_BOOK_W, maxBookWidth(screenW));
        int w = clampSize(contentW + 2 * (SHEET_PAD + borderFor(guess)),
                MIN_BOOK_W, maxBookWidth(screenW));
        int h = clampSize(contentH + SHEET_HEAD_H + SHEET_PAD + 2 * borderFor(w),
                MIN_BOOK_H, maxBookHeight(screenH));
        return place(screenW, screenH, w, h);
    }

    /** 书体可用高（再扣掉标题带与页脚带）与上限取小 */
    private static int maxBookHeight(int screenH) {
        return Math.min(MAX_BOOK_H, screenH - TOP_RESERVE - BOTTOM_RESERVE);
    }

    /** 书体可用宽：书体是水平居中的，故左侧那条书签列要按"两侧都留"来预算（窄屏也不会把书签顶出屏幕） */
    private static int maxBookWidth(int screenW) {
        return Math.min(MAX_BOOK_W, screenW - 2 * (EDGE_MARGIN + TAB_W + TAB_LIFT));
    }

    /** 书签数（= 屏幕里的族数，用于把"最下面一枚书签"也算进垂直预算） */
    private static final int TAB_COUNT = 3;

    /** 书签列总高（从书体顶边向下排满 {@link #TAB_COUNT} 枚） */
    private static int tabColumnHeight() {
        return TAB_COUNT * TAB_H + (TAB_COUNT - 1) * TAB_GAP;
    }

    /** 钳制到 [lo, hi]；窗口小到装不下下限时优先"装得下"（宁可少几行靠滚动，也不越出屏幕） */
    private static int clampSize(int want, int lo, int hi) {
        int low = Math.min(lo, Math.max(1, hi));
        return Math.max(low, Math.min(hi, want));
    }

    /**
     * 书的落位：水平恒居中；垂直"偏上居中"，并同时满足两条预算 ——
     * ① 书体本身要装进"标题带以下、页脚带以上"；② 左侧书签列（比书体高的短书）也要留在屏幕内
     * （书签从书体顶边向下排，书很短时若不额外上收，最下面那枚会掉出屏幕下沿）。
     */
    private static Area place(int screenW, int screenH, int w, int h) {
        int x = (screenW - w) / 2;
        int hi = Math.max(TOP_RESERVE, Math.min(
                screenH - BOTTOM_RESERVE - h,
                screenH - EDGE_MARGIN - tabColumnHeight()));
        int y = Math.max(TOP_RESERVE, Math.min((screenH - h) / 2, hi));
        return new Area(x, y, w, h);
    }

    // ===== 版面几何（屏幕/画布/内容层都从这里取，保证只有一份真源） =====

    /**
     * <b>框线以内的纸面</b>（= 书体四边各内缩 {@link #borderFor}）。
     *
     * <p>它是书页上一切文字/控件的<b>硬边界</b>：页眉、正文、叶号、动作按钮全部由它派生，
     * 故不可能越过贴图里那根内框线（左页曾出现的"越框"正是版心没吃这个边界导致的）。
     */
    static Area frameArea(Area book) {
        Split s = split(book);
        return new Area(book.x() + s.border(), book.y() + s.border(),
                book.w() - 2 * s.border(), book.h() - 2 * s.border());
    }

    /**
     * 一页纸面（side = 0 左页 / 1 右页）：<b>框线之内</b>、两页等宽、中间夹书脊缝。
     *
     * <p>两页宽严格相等（{@link #split} 的奇偶修正保证）；书脊<b>不参与拉伸</b>，
     * 故"跨页整幅"的源图在中间不会变形。
     */
    static Area pageArea(Area book, int side) {
        Split s = split(book);
        int first = book.x() + s.border();
        int x = side == 0 ? first : first + s.paperW() + s.spineW();
        return new Area(x, book.y() + s.border(), s.paperW(), book.h() - 2 * s.border());
    }

    /**
     * 一页的正文区（版心）：<b>由框线反算</b>，左右两页对称。
     *
     * <p>口径：外侧 = 纸面外缘 + {@code MARGIN}（再让出滚动条宽，滚动条画在版心之外）；
     * 靠书脊侧 = 纸面内缘 − {@code MARGIN} − {@code SPINE_GUTTER}（装订余量，两页各自独立算）。
     * 纵向 = 纸面顶 + {@code HEAD_BAND}（页眉带）到 纸面底 − {@code FOLIO_BAND}（叶号带）。
     * 结果一律经 {@link #fitInside} 自检钳进纸面，<b>永不越框</b>。
     */
    static Area textArea(Area book, int side) {
        Area paper = pageArea(book, side);
        int outer = MARGIN + SCROLL_W + SCROLL_GAP;
        int inner = MARGIN + SPINE_GUTTER;
        int left = side == 0 ? paper.x() + outer : paper.x() + inner;
        int right = side == 0 ? paper.right() - inner : paper.right() - outer;
        return fitInside(paper, left, paper.y() + HEAD_BAND, right - left,
                paper.h() - HEAD_BAND - FOLIO_BAND);
    }

    /**
     * <b>版心自检（永不越框的兜底）</b>：把算出的矩形硬钳进 {@code limit}（框线以内的纸面）之内。
     *
     * <p>宽高先收到纸面之内（宽不低于 {@link #MIN_READABLE_W}，高不低于一行），
     * 再把坐标拉回纸面 —— 于是即便以后有人调大 {@link #MARGIN}/换更窄的边框/换更小的字，
     * 也只会看到版心<b>变窄</b>，不会看到文字跑到框线外去。
     * 书体宽 ≥ {@link #MIN_BOOK_W}(320) 时纸面宽恒 ≥ 130 ⇒ 最小可读宽永远可达。
     */
    private static Area fitInside(Area limit, int x, int y, int w, int h) {
        int cw = Math.max(1, Math.min(w, limit.w()));
        if (limit.w() >= MIN_READABLE_W) {
            cw = Math.min(limit.w(), Math.max(MIN_READABLE_W, cw));
        }
        int ch = Math.max(LINE_H, Math.min(h, limit.h()));
        int cx = Math.max(limit.x(), Math.min(x, limit.right() - cw));
        int cy = Math.max(limit.y(), Math.min(y, limit.bottom() - ch));
        return new Area(cx, cy, cw, ch);
    }

    /** 画布（族图）的视口：<b>框线之内</b>再留白，顶部留出一行页眉 */
    static Area chartArea(Area book) {
        Area frame = frameArea(book);
        return new Area(frame.x() + SHEET_PAD, frame.y() + SHEET_HEAD_H,
                frame.w() - 2 * SHEET_PAD, frame.h() - SHEET_HEAD_H - SHEET_PAD);
    }

    /** 书签式分类标签的列：<b>右缘贴住书体左缘（不留缝）</b>，从书体顶边向下排 */
    static Area tabArea(Area book, int index) {
        return new Area(book.x() - TAB_W, book.y() + index * (TAB_H + TAB_GAP), TAB_W, TAB_H);
    }

    /**
     * 书签"画出来 / 可点中"的矩形：选中那枚向书外（左）凸出 {@link #TAB_LIFT}。
     *
     * <p><b>绘制与命中共用这一处</b>，故不会出现"看得到却点不到"（或反之）的错位。
     */
    static Area tabVisual(Area area, boolean selected) {
        return selected
                ? new Area(area.x() - TAB_LIFT, area.y(), area.w() + TAB_LIFT, area.h())
                : area;
    }

    /** 页外侧叶号的基线（相对<b>框线内</b>的纸面底边内缩，故永不压到框线/皮革） */
    static int folioY(Area book) {
        return frameArea(book).bottom() - FOLIO_BOTTOM;
    }

    /**
     * 页内动作按钮（画在条件页底部）。
     *
     * <p><b>宽度钳制（硬要求）</b>：无论窗口多窄、书体被压到多小、文案多短，按钮的左右缘
     * 一律钳在<b>该页纸面（框线内）之内</b>（纸面再内缩 {@link #MARGIN}，与正文同一口径）；
     * 高度也不超过正文区。故绝不会出现"已学完"横跨并越出书体左缘那种反例。
     */
    static Area actionArea(Area book, int side) {
        Area paper = pageArea(book, side);
        Area text = textArea(book, side);
        int w = Math.max(1, Math.min(text.w(), paper.w() - 2 * MARGIN));
        int h = Math.max(1, Math.min(ACTION_H, text.h()));
        int x = Math.max(paper.x() + MARGIN, Math.min(text.x(), paper.right() - MARGIN - w));
        return new Area(x, text.bottom() - h, w, h);
    }

    /** 一页能放几行：条件页底部让出一块给动作按钮，故少 {@link #ACTION_ROWS} 行 */
    static int capacity(Area book, int side, boolean reserveAction) {
        int h = textArea(book, side).h() - (reserveAction ? ACTION_H + ACTION_GAP : 0);
        return Math.max(1, h / LINE_H);
    }

    // ===== 页脚控件几何（书体下方居中） =====

    /** 页脚基线（书体下缘外 {@link #FOOTER_GAP}） */
    private static int footerY(Area book) {
        return book.bottom() + FOOTER_GAP;
    }

    /**
     * 页脚各段的实际几何（{@code [返回键宽, 翻页键宽, 页码文本宽, 间隙]}）。
     *
     * <p><b>为什么要算而不是直接用常量</b>：书体宽会随屏宽被压窄（GUI 缩放档位越高越明显），
     * 而页脚若按常量横铺，窄屏下就会越出书体左右缘。这里按"书体可用宽"（= <b>框线内宽</b>，
     * 故页脚与纸面左右对齐）把各段<b>同比例取整收窄</b>，
     * 于是页脚在任何档位下都落在框线之内（这就是"宽度钳制"的落点）。
     */
    private static int[] footerMetrics(Area book) {
        int back = FOOTER_BACK_W;
        int nav = FOOTER_NAV_W;
        int text = FOOTER_TEXT_W;
        int gap = FOOTER_ITEM_GAP;
        int want = back + 3 * gap + 2 * nav + text;
        int room = book.w() - 2 * borderFor(book.w());
        if (room < want) {
            int r = Math.max(4, room);
            back = Math.max(1, back * r / want);
            nav = Math.max(1, nav * r / want);
            text = Math.max(1, text * r / want);
            gap = Math.max(1, gap * r / want);
        }
        return new int[]{back, nav, text, gap};
    }

    /** 页脚整行的左起点：按书体中心居中，并<b>钳制在框线左右缘之内</b>（窄屏也不越出内框线） */
    private static int footerLeft(Area book) {
        int[] m = footerMetrics(book);
        int pad = borderFor(book.w());
        int total = m[0] + 3 * m[3] + 2 * m[1] + m[2];
        int lo = book.x() + pad;
        int hi = Math.max(lo, book.right() - pad - total);
        return Math.max(lo, Math.min(book.cx() - total / 2, hi));
    }

    static Area footerBack(Area book) {
        return new Area(footerLeft(book), footerY(book), footerMetrics(book)[0], FOOTER_H);
    }

    static Area footerPrev(Area book) {
        int[] m = footerMetrics(book);
        return new Area(footerLeft(book) + m[0] + m[3], footerY(book), m[1], FOOTER_H);
    }

    /** 页码文本的绘制起点（不画底，直接落在压暗的背景上） */
    static int footerTextX(Area book) {
        int[] m = footerMetrics(book);
        return footerLeft(book) + m[0] + m[3] + m[1] + m[3];
    }

    /** 页码文本的实际可用宽（随书体宽收窄，故界面要按它截断与居中） */
    static int footerTextW(Area book) {
        return footerMetrics(book)[2];
    }

    static Area footerNext(Area book) {
        int[] m = footerMetrics(book);
        return new Area(footerTextX(book) + m[2] + m[3], footerY(book), m[1], FOOTER_H);
    }

    /** 页脚文字基线（与按钮内文字同一口径居中） */
    static int footerTextY(Area book) {
        return footerY(book) + (FOOTER_H - 8) / 2;
    }

    /** 书体之外的一行提示（画布态操作提示）的基线 */
    static int outsideHintY(Area book) {
        return footerTextY(book);
    }

    // ===== 书底 =====

    /**
     * 书底：皮革封边 + 纸面 + 内框线 + 书脊折痕 + 书签带。
     *
     * <p><b>贴图存在时走 {@link #blitBookTexture}</b>（横向 5 段 / 纵向 3 段的分段拉伸，
     * 边框与书脊宽度固定、只拉伸两页纸面）；<b>缺图回落</b>到
     * {@link AkaishiCodexBookArt#leather} + {@link AkaishiCodexBookArt#pagePaper}（纸纹/污渍/折角）
     * + {@link AkaishiCodexBookArt#fold}（摊开时）—— 两条路的纸面都是
     * {@link #pageArea}（<b>框线之内</b>），故版心口径一致，缺图也不会让文字跑到纸外。
     *
     * @param open   true = 摊开的双页书（条目页）；false = 单张图版纸（族画布）
     * @param family 当前族（决定书签带颜色；null = 中性暗红）
     */
    static void book(GuiGraphics gui, Area book, int seed, boolean open, @Nullable String family) {
        if (texturePresent(TEX_BOOK_FRAME)) {
            blitBookTexture(gui, book);
        } else {
            AkaishiCodexBookArt.leather(gui, book.x(), book.y(), book.right(), book.bottom());
            if (open) {
                Area left = pageArea(book, 0);
                Area right = pageArea(book, 1);
                AkaishiCodexBookArt.pagePaper(gui, left.x(), left.y(), left.w(), left.h(), seed);
                AkaishiCodexBookArt.pagePaper(gui, right.x(), right.y(), right.w(), right.h(), seed + 1);
                AkaishiCodexBookArt.fold(gui, left.right() + (right.x() - left.right()) / 2,
                        left.y(), left.bottom());
            } else {
                Area sheet = frameArea(book);
                AkaishiCodexBookArt.pagePaper(gui, sheet.x(), sheet.y(), sheet.w(), sheet.h(), seed);
            }
        }
        // 内框线：补画一根（位置 = 离书体边缘 borderFor(书体宽) 处，与贴图那根同位置；见 FRAME_LINE）
        Area frame = frameArea(book);
        AkaishiCodexBookArt.ring(gui, frame.x() - 1, frame.y() - 1, frame.w() + 2, frame.h() + 2, FRAME_LINE);
        // 书签带：族色（不随贴图回落而消失，它是"这一册"的标记）；挂在右页纸面外侧、框线之内
        Area rightPaper = pageArea(book, 1);
        AkaishiCodexBookArt.ribbon(gui, rightPaper.right() - RIBBON_W - RIBBON_INSET, rightPaper.y(),
                RIBBON_W, RIBBON_H, family == null ? 0xFF8C3B34 : familyColor(family));
    }

    /**
     * <b>书底贴图的分段拉伸</b>：横向 5 段 × 纵向 3 段（不是 9 宫格，因为书脊在正中）。
     *
     * <p><b>为什么分段</b>：源图是"跨页整幅"，整幅拉伸会把内框线的位置拉成
     * {@code 书体宽 × 42/512}（≈8.2%）与 {@code 书体高 × 42/288}（≈14.6%），
     * 与代码里的固定页边距对不上，左页文字就会跑到框线外（用户实测的那条 bug）。
     * 分段后：<b>左右边框、上下边框、书脊全部按同一系数缩放且不拉伸</b>（宽度固定），
     * 只有<b>两页纸面中心</b>各自被拉伸，于是框线在屏幕上恒在
     * {@code 书体边缘 + borderFor(书体宽)}，可精确算出。
     *
     * <p>源图分段（实测，见 {@link #SLICE_EDGE}）：
     * 横 {@code [0,43) [43,250) [250,262) [262,469) [469,512)}；
     * 纵 {@code [0,43) [43,245) [245,288)}。目标段与源段一一对应，且各段之和严格等于书体宽/高
     * （整数边界，故不会出现 1px 的接缝或重叠）。
     */
    private static void blitBookTexture(GuiGraphics gui, Area book) {
        Split s = split(book);
        int[] dx = {book.x(), book.x() + s.border(), book.x() + s.border() + s.paperW(),
                book.x() + s.border() + s.paperW() + s.spineW(), book.right() - s.border()};
        int[] dw = {s.border(), s.paperW(), s.spineW(), s.paperW(), s.border()};
        int[] sx = {0, SLICE_EDGE, SLICE_SPINE_L, SLICE_SPINE_R, TEX_BOOK_W - SLICE_EDGE};
        int[] sw = {SLICE_EDGE, SLICE_SPINE_L - SLICE_EDGE, SLICE_SPINE_W,
                (TEX_BOOK_W - SLICE_EDGE) - SLICE_SPINE_R, SLICE_EDGE};
        int[] dy = {book.y(), book.y() + s.border(), book.bottom() - s.border()};
        int[] dh = {s.border(), book.h() - 2 * s.border(), s.border()};
        int[] sy = {0, SLICE_EDGE, TEX_BOOK_H - SLICE_EDGE};
        int[] sh = {SLICE_EDGE, TEX_BOOK_H - 2 * SLICE_EDGE, SLICE_EDGE};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) {
                gui.blit(TEX_BOOK_FRAME, dx[col], dy[row], dw[col], dh[row],
                        sx[col], sy[row], sw[col], sh[row], TEX_BOOK_W, TEX_BOOK_H);
            }
        }
    }

    /** 画布态的纸面页眉：左 = 族名，右 = 本族进度，其下一条手绘规律线（都落在框线之内） */
    static void sheetHead(GuiGraphics gui, Font font, Area book, Component left, Component right, int seed) {
        Area sheet = chartArea(book);
        int headY = sheet.y() - SHEET_HEAD_H;
        String l = font.plainSubstrByWidth(left.getString(), sheet.w() / 2);
        String r = font.plainSubstrByWidth(right.getString(), sheet.w() / 2);
        gui.drawString(font, l, sheet.x(), headY + SHEET_HEAD_TEXT_Y, AkaishiCodexPages.INK, false);
        gui.drawString(font, r, sheet.right() - font.width(r), headY + SHEET_HEAD_TEXT_Y,
                AkaishiCodexPages.INK_DIM, false);
        AkaishiCodexBookArt.rule(gui, sheet.x(), headY + SHEET_HEAD_RULE_Y, sheet.w(), RULE_COLOR, seed);
    }

    // ===== 书签式分类标签 =====

    /**
     * 一个书签式分类标签：<b>窄条贴边</b>（右缘贴住书体左缘），选中那枚向左突出
     * {@link #TAB_LIFT}px（"翻出来"的感觉），未选中则压暗收回（保色相）。
     *
     * <p><b>只放一枚徽记</b>（用户拍板）：{@link #TAB_ICON}×{@link #TAB_ICON} 居中，
     * 优先用该族的徽记贴图（族内第一个节点的
     * {@code textures/gui/codex/emblem/<节点路径>.png}，32→16 为整数倍下采样，不糊）；
     * 缺图回落该节点的物品图标（1:1，不缩放）。<b>族名不再画在书签上</b>——
     * 书签只有 18 宽，字挤在里面既看不清也压图案；族名（与族色）改由悬停提示给出
     * （见 {@code AkaishiCodexScreen#renderTabTooltip}）。
     */
    static void tab(GuiGraphics gui, Area area, String family, @Nullable CodexNode node,
                    ItemStack icon, boolean selected, boolean hovered) {
        int accent = familyColor(family);
        Area tab = tabVisual(area, selected);
        int body = selected ? accent : AkaishiCodexBookArt.ink(accent, TAB_DIM_MIX);
        gui.fill(tab.x(), tab.y(), tab.right(), tab.bottom(), body);
        gui.fill(tab.x(), tab.y(), tab.right(), tab.y() + 1, 0x40000000);
        gui.fill(tab.x(), tab.bottom() - 1, tab.right(), tab.bottom(), 0x70000000);
        gui.fill(tab.x(), tab.y(), tab.x() + 1, tab.bottom(), 0x40000000);
        if (hovered) {
            gui.fill(tab.x() + 1, tab.y() + 1, tab.right() - 1, tab.bottom() - 1, 0x22FFFFFF);
        }
        AkaishiCodexBookArt.ring(gui, tab.x(), tab.y(), tab.w(), tab.h(), selected ? TAB_EDGE_ON : TAB_EDGE);
        drawTabIcon(gui, tab, icon, node);
    }

    /** 书签唯一的图案：族徽记贴图优先（16 居中），缺图回落该节点物品图标（1:1，不缩放） */
    private static void drawTabIcon(GuiGraphics gui, Area tab, ItemStack icon, @Nullable CodexNode node) {
        int x = tab.x() + (tab.w() - TAB_ICON) / 2;
        int y = tab.y() + (tab.h() - TAB_ICON) / 2;
        if (node != null) {
            ResourceLocation tex = AkaishiCodexEmblem.texture(node);
            if (texturePresent(tex)) {
                gui.blit(tex, x, y, TAB_ICON, TAB_ICON,
                        0f, 0f, TEX_EMBLEM_SIZE, TEX_EMBLEM_SIZE, TEX_EMBLEM_SIZE, TEX_EMBLEM_SIZE);
                return;
            }
        }
        if (icon != null && !icon.isEmpty()) {
            gui.renderItem(icon, x, y);
        }
    }

    // ===== 连线（直角折线） =====

    /**
     * 一条前置连线：<b>只走横竖</b>（先横后竖），拐角圆化 {@link #LINK_CORNER} 像素。
     *
     * @param thickness 线宽（挡路的那根用 {@link #LINK_W_THICK}）
     * @param arrow     true = 在 {@code (x1,y1)} 这一端画箭头，<b>箭头指向 (x1,y1)</b>
     *                  （调用方把 (x1,y1) 传成"挡路的前置"，于是箭头就指着挡你的那一个）
     */
    static void link(GuiGraphics gui, int x1, int y1, int x2, int y2, int color,
                     int thickness, boolean arrow) {
        if (x1 == x2 || y1 == y2) {
            segment(gui, x1, y1, x2, y2, color, thickness);
            if (arrow) {
                arrowHead(gui, x1, y1, x2, y2, color);
            }
            return;
        }
        int signX = x2 > x1 ? 1 : -1;
        int signY = y2 > y1 ? 1 : -1;
        int r = Math.min(LINK_CORNER, Math.min(Math.abs(x2 - x1), Math.abs(y2 - y1)) / 2);
        if (r < 1) {
            segment(gui, x1, y1, x2, y1, color, thickness);
            segment(gui, x2, y1, x2, y2, color, thickness);
        } else {
            // 两条腿各缩掉圆角半径，中间补一段四分之一圆（凸角朝外）
            segment(gui, x1, y1, x2 - signX * r, y1, color, thickness);
            segment(gui, x2, y1 + signY * r, x2, y2, color, thickness);
            cornerArc(gui, x2 - signX * r, y1 + signY * r, r, signX, signY, color, thickness);
        }
        if (arrow) {
            // 第一段是"从 (x1,y1) 横向走到拐角"，故箭头方向由拐角指回 (x1,y1)
            arrowHead(gui, x1, y1, x2, y1, color);
        }
    }

    /** 圆角：以 (cx,cy) 为圆心画一段四分之一圆弧（两个端点分别落在两条腿上） */
    private static void cornerArc(GuiGraphics gui, int cx, int cy, int r,
                                  int signX, int signY, int color, int thickness) {
        int steps = Math.max(3, r * 3);
        for (int i = 0; i <= steps; i++) {
            double t = i * (Math.PI / 2) / steps;
            int px = cx + (int) Math.round(signX * r * Math.sin(t));
            int py = cy + (int) Math.round(-signY * r * Math.cos(t));
            dot(gui, px, py, color, thickness);
        }
    }

    /** 直线段（逐像素 fill；厚度按方形块画） */
    private static void segment(GuiGraphics gui, int x1, int y1, int x2, int y2, int color, int thickness) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            dot(gui, x2, y2, color, thickness);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            dot(gui, x1 + (x2 - x1) * i / steps, y1 + (y2 - y1) * i / steps, color, thickness);
        }
    }

    private static void dot(GuiGraphics gui, int x, int y, int color, int thickness) {
        int t = Math.max(1, thickness);
        gui.fill(x, y, x + t, y + t, color);
    }

    /**
     * 箭头：尖端在 {@code (targetX,targetY)} 那一侧，尾巴落在连线上（距目标 {@link #ARROW_GAP} px，
     * 因此不会盖住目标的徽记图案）。
     */
    private static void arrowHead(GuiGraphics gui, int targetX, int targetY, int fromX, int fromY, int color) {
        double dx = targetX - fromX;
        double dy = targetY - fromY;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0D) {
            return;
        }
        double ux = dx / len;
        double uy = dy / len;
        double tailX = targetX - ux * ARROW_GAP;
        double tailY = targetY - uy * ARROW_GAP;
        double px = -uy;
        double py = ux;
        for (int i = 0; i <= ARROW_LEN; i++) {
            int half = ARROW_HALF * (ARROW_LEN - i) / ARROW_LEN;
            int bx = (int) Math.round(tailX + ux * i);
            int by = (int) Math.round(tailY + uy * i);
            for (int j = -half; j <= half; j++) {
                int qx = (int) Math.round(bx + px * j);
                int qy = (int) Math.round(by + py * j);
                gui.fill(qx, qy, qx + 1, qy + 1, color);
            }
        }
    }

    // ===== 悬停提示 =====

    /**
     * 节点悬停提示：名称（按状态上色）→ 族 · 层级[ · 禁忌] → 层级 → 状态 → 一句话描述 →
     * 挡路者（被挡时）或线索（可研究但条件未齐时）。
     */
    static void tooltip(GuiGraphics gui, Font font, int mouseX, int mouseY, CodexNode node,
                        @Nullable AkaishiCodexSync.NodeView view,
                        AkaishiCodexEmblem.Mark mark, @Nullable CodexNode blocker) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(node.nameKey())
                .withStyle(styleOf(AkaishiCodexEmblem.nameColor(mark, node.forbidden()))));
        MutableComponent meta = Component.translatable(CodexFamily.nameKey(node.family()))
                .withStyle(styleOf(familyColor(node.family())))
                .append(Component.literal(" · "))
                .append(Component.translatable(node.tier().nameKey()));
        if (node.forbidden()) {
            meta.append(Component.literal(" · ")).append(Component.translatable("gui.akaishi.codex.tip.forbidden"));
        }
        lines.add(meta);
        lines.add(Component.translatable("gui.akaishi.codex.detail.tier",
                Component.translatable(node.tier().nameKey())).withStyle(styleOf(TIP_DIM)));
        lines.add(Component.translatable(AkaishiCodexEmblem.stateKey(mark)));
        String desc = Component.translatableWithFallback(node.descKey(), "").getString();
        if (!desc.isEmpty()) {
            lines.add(Component.literal(desc).withStyle(styleOf(TIP_DIM)));
        }
        if (blocker != null) {
            lines.add(Component.translatable("gui.akaishi.codex.tip.blocker",
                    Component.translatable(blocker.nameKey())).withStyle(styleOf(TIP_JUMP)));
        } else if (AkaishiCodexPages.needsHint(node, view)) {
            lines.add(Component.translatable("gui.akaishi.codex.page.hint",
                    AkaishiCodexPages.hintText(node)).withStyle(styleOf(TIP_JUMP)));
        }
        gui.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    /** 一行提示（书签悬停 / 页脚按钮 / 可跳转行） */
    static void tip(GuiGraphics gui, Font font, int mouseX, int mouseY, Component text) {
        gui.renderTooltip(font, text, mouseX, mouseY);
    }

    // ===== 书页排版 =====

    /**
     * 页眉（左页写册/篇名，右页写篇名 · 页类型）+ 页眉下的规律线。
     *
     * <p>横向从<b>版心左缘</b>起、按版心宽截断（故页眉既与正文左右对齐，也永不越过框线）；
     * 纵向落在纸面顶边下方的页眉带里（{@link #HEAD_INSET} / {@link #RULE_INSET}）。
     */
    static void pageHead(GuiGraphics gui, Font font, Area book, int side, Component head, int seed) {
        Area paper = pageArea(book, side);
        Area text = textArea(book, side);
        gui.drawString(font, font.plainSubstrByWidth(head.getString(), text.w()),
                text.x(), paper.y() + HEAD_INSET, AkaishiCodexPages.INK_DIM, false);
        AkaishiCodexBookArt.rule(gui, text.x(), paper.y() + RULE_INSET, text.w(), RULE_COLOR, seed);
    }

    /**
     * 正文：只画可见切片（段距占位行不画字；旁注贴页外侧：左页靠左、右页右对齐）。
     *
     * <p><b>本轮取消小字号</b>：旁注原是按 0.78 倍缩放画的，那同样会让字发虚；
     * 现在旁注与正文同一字号（1:1），靠墨色（{@link AkaishiCodexPages#INK_NOTE}）与贴边位置区分。
     *
     * @param side 0 = 左页 / 1 = 右页（决定旁注贴哪一边）
     */
    static void prose(GuiGraphics gui, Font font, Area book, int side, List<AkaishiCodexPages.Line> lines,
                      int scroll, int capacity) {
        Area text = textArea(book, side);
        int shown = Math.max(0, Math.min(lines.size() - scroll, capacity));
        for (int i = 0; i < shown; i++) {
            AkaishiCodexPages.Line line = lines.get(scroll + i);
            if (line.spacer()) {
                continue;
            }
            int y = text.y() + i * LINE_H;
            if (line.note()) {
                int x = side == 0
                        ? text.x() + AkaishiCodexPages.NOTE_OUTER_PAD
                        : text.right() - AkaishiCodexPages.NOTE_OUTER_PAD - font.width(line.text());
                gui.drawString(font, line.text(), x, y, line.color(), false);
            } else {
                gui.drawString(font, line.text(), text.x() + line.indent(), y, line.color(), false);
            }
        }
    }

    /** 正文溢出时的滚动条（只有它是"现代控件"，故压到最淡） */
    static void scrollBar(GuiGraphics gui, Area book, int side, int size, int capacity, int scroll) {
        if (size <= capacity) {
            return;
        }
        Area text = textArea(book, side);
        int trackH = capacity * LINE_H;
        int max = size - capacity;
        int handleH = Math.max(6, trackH * capacity / size);
        int handleY = text.y() + (trackH - handleH) * Math.max(0, Math.min(scroll, max)) / max;
        int barX = text.right() + SCROLL_GAP;
        gui.fill(barX, text.y(), barX + SCROLL_W, text.y() + trackH, SCROLL_TRACK);
        gui.fill(barX, handleY, barX + SCROLL_W, handleY + handleH, SCROLL_HANDLE);
    }

    /** 页外侧叶号：左页左下、右页右下；<b>从框线内的纸面外侧横向内缩 {@link #FOLIO_INSET}</b>，永不压框线 */
    static void folio(GuiGraphics gui, Font font, Area book, int side, int leaf) {
        Area paper = pageArea(book, side);
        AkaishiCodexBookArt.folio(gui, font, leaf,
                side == 0 ? paper.x() + FOLIO_INSET : paper.right() - FOLIO_INSET, folioY(book), side != 0);
    }

    /**
     * 书页风格的按钮：深墨底 + 浅描边 + 浅字（默认态 / 置灰态）。
     *
     * <p><b>为什么自绘</b>：书体之外不再有面板底，页脚控件直接落在"压暗后的游戏世界"上，
     * 原版浅灰按钮会与背景糊在一起；深墨底 + 浅字在任何背景上都读得清，
     * 也与书页（深棕墨色体系）同族。落在纸面上时读作"墨块标签"，同样成立。
     */
    static void button(GuiGraphics gui, Font font, Area area, Component label, boolean enabled) {
        gui.fill(area.x(), area.y(), area.right(), area.bottom(), enabled ? BTN_BASE : BTN_BASE_OFF);
        AkaishiCodexBookArt.ring(gui, area.x(), area.y(), area.w(), area.h(),
                enabled ? BTN_EDGE : BTN_EDGE_OFF);
        String text = font.plainSubstrByWidth(label.getString(), Math.max(0, area.w() - 6));
        gui.drawString(font, text, area.x() + (area.w() - font.width(text)) / 2,
                area.y() + (area.h() - 8) / 2, enabled ? BTN_TEXT : BTN_TEXT_OFF, false);
    }

    /**
     * 配方页：有配方就画合成表（3×3 网格 + 箭头 + 产物槽），没有就画一句占位。
     *
     * <p>配方本体读<b>客户端本地</b>的 {@code RecipeManager}（配方是公共数据，两端同源，
     * 不需要服务端下发）。认不出的配方类型（如熔炉配方）不硬画，改画"找不到了"。
     */
    static void recipePage(GuiGraphics gui, Font font, Area book, int side, List<ResourceLocation> recipes) {
        Area text = textArea(book, side);
        // 占位/找不到两类说明行按"可用行宽"换行（同正文口径，不顶到版心边界）
        int wrapW = Math.max(MIN_LINE_W, text.w() - LINE_TAIL_PAD);
        if (recipes.isEmpty()) {
            wrapped(gui, font, Component.translatable("gui.akaishi.codex.recipe.pending"),
                    text.x(), text.y() + LINE_H, wrapW, AkaishiCodexPages.INK_DIM);
            return;
        }
        ResourceLocation id = recipes.get(0);
        Recipe<?> recipe = findRecipe(id);
        if (!(recipe instanceof CraftingRecipe crafting)) {
            wrapped(gui, font, Component.translatable("gui.akaishi.codex.recipe.missing", id.toString()),
                    text.x(), text.y() + LINE_H, wrapW, AkaishiCodexPages.INK_DIM);
            return;
        }
        drawCrafting(gui, font, text, crafting);
        if (recipes.size() > 1) {
            wrapped(gui, font, Component.translatable("gui.akaishi.codex.recipe.more", recipes.size() - 1),
                    text.x(), text.bottom() - LINE_H, wrapW, AkaishiCodexPages.INK_DIM);
        }
    }

    /** 3×3 网格 + 箭头 + 产物槽（用原版槽位贴图，与项目其它 GUI 同一样式） */
    private static void drawCrafting(GuiGraphics gui, Font font, Area text, CraftingRecipe recipe) {
        int cell = SLOT + SLOT_GAP;
        int gridW = 3 * cell - SLOT_GAP;
        int totalW = gridW + CRAFT_ARROW_W + SLOT;
        int left = text.x() + Math.max(0, (text.w() - totalW) / 2);
        int top = text.y() + CRAFT_TOP;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = left + col * cell;
                int sy = top + row * cell;
                GuiWidgets.slotBox(gui, sx + 1, sy + 1);
                int index = row * 3 + col;
                if (index < ingredients.size()) {
                    ItemStack[] items = ingredients.get(index).getItems();
                    if (items.length > 0) {
                        gui.renderItem(items[0], sx + 1, sy + 1);
                    }
                }
            }
        }
        int arrowX = left + gridW + SLOT_GAP;
        GuiWidgets.progressArrow(gui, arrowX, top + cell, CRAFT_ARROW_W - SLOT_GAP, SLOT, 100f);
        int resultX = arrowX + CRAFT_ARROW_W;
        GuiWidgets.slotBox(gui, resultX + 1, top + cell + 1);
        var level = Minecraft.getInstance().level;
        if (level != null) {
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (!result.isEmpty()) {
                gui.renderItem(result, resultX + 1, top + cell + 1);
                gui.drawString(font,
                        font.plainSubstrByWidth(result.getHoverName().getString(), text.w()),
                        text.x(), top + 3 * cell + 6, AkaishiCodexPages.INK, false);
            }
        }
    }

    @Nullable
    private static Recipe<?> findRecipe(ResourceLocation id) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        return level.getRecipeManager().byKey(id).orElse(null);
    }

    /** 小段自动换行文字（占位文案、结果名之类的"说明行"） */
    private static void wrapped(GuiGraphics gui, Font font, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, Math.max(MIN_LINE_W, width))) {
            gui.drawString(font, line, x, y, color, false);
            y += LINE_H;
        }
    }

    // ===== 徽记的名字（徽记本体见 AkaishiCodexEmblem） =====

    /**
     * 徽记下的节点名（颜色按四档视觉状态）；"可研究"时名字下再补一笔族色线
     * （随圈注一起呼吸：圈注与名字呼应，像"这一条我圈了"）。
     */
    static void label(GuiGraphics gui, Font font, String text, int centerX, int y,
                      String family, AkaishiCodexEmblem.Mark mark, boolean forbidden) {
        int x = centerX - font.width(text) / 2;
        gui.drawString(font, text, x, y, AkaishiCodexEmblem.nameColor(mark, forbidden), false);
        if (mark == AkaishiCodexEmblem.Mark.AVAILABLE) {
            AkaishiCodexBookArt.rule(gui, x, y + 9, font.width(text),
                    AkaishiCodexBookArt.semi(familyColor(family), pulse() * 0x70 / 0xFF + 0x30),
                    AkaishiCodexBookArt.seed("label", family, text));
        }
    }

    // ===== 小工具 =====

    /** 族 → 识别色（未知族回退兜底色，绝不返回 0 导致"看不见的线"） */
    static int familyColor(String family) {
        return switch (family) {
            case CodexFamily.HUSH -> AkaishiCodexEmblem.COLOR_HUSH;
            case CodexFamily.GAZE -> AkaishiCodexEmblem.COLOR_GAZE;
            case CodexFamily.BLOOD -> AkaishiCodexEmblem.COLOR_BLOOD;
            default -> COLOR_FAMILY_UNKNOWN;
        };
    }

    /** 纸面墨化：族色按 {@link #INK_MIX} 压暗（保色相；族色原色只留给标签与提示） */
    static int inked(String family) {
        return AkaishiCodexBookArt.ink(familyColor(family), INK_MIX);
    }

    /** 可研究徽记的呼吸相位（只改 alpha，不改色相；周期 {@link AkaishiCodexEmblem#PULSE_PERIOD_MS}） */
    static int pulse() {
        double phase = (System.currentTimeMillis() % AkaishiCodexEmblem.PULSE_PERIOD_MS)
                / (double) AkaishiCodexEmblem.PULSE_PERIOD_MS;
        return (int) (0x70 + 0x8F * (0.5D + 0.5D * Math.sin(phase * Math.PI * 2D)));
    }

    /**
     * 贴图是否存在（<b>缺图回落的前提</b>）：存在才 blit，否则一律走程序化笔触 ——
     * 直接 blit 不存在的贴图会被原版画成"缺图马赛克"，那是硬性禁止的。
     *
     * <p>结果缓存；资源重载会换掉 {@code ResourceManager} 实例，检测到换实例即整表重探
     * （于是开发期新丢进去的贴图不用重启也能生效）。
     */
    static boolean texturePresent(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        ResourceManager manager = minecraft.getResourceManager();
        if (manager != cachedManager) {
            PRESENT.clear();
            cachedManager = manager;
        }
        return PRESENT.computeIfAbsent(id, key -> manager.getResource(key).isPresent());
    }

    static Style styleOf(int argb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(argb & 0xFFFFFF));
    }
}
