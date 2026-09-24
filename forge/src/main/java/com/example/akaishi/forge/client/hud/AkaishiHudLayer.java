package com.example.akaishi.forge.client.hud;

import com.example.akaishi.api.hud.AkaishiHudElement;
import com.example.akaishi.api.hud.AkaishiHudRegistry;
import com.example.akaishi.api.hud.HudAnchor;
import com.example.akaishi.api.hud.HudRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 统一 HUD 渲染层（仅客户端，<b>唯一</b>的 HUD 调度器）：把"位置"从各 HUD 元素里抽出来，
 * 由本类统一解算锚点、按序堆叠、与原版 HUD 避让，并把结果经 {@link HudRenderContext} 交给元素绘制。
 *
 * <p><b>为什么需要一个调度器</b>：此前每个 HUD 各自硬编码屏幕坐标、各自开一条 overlay 注册项 ——
 * 新增元素必然与既有元素打架，且"避开原版 HUD"只能靠手工挪常量。集中一处后有两条硬保证：
 * <ol>
 *   <li><b>组内永不重叠</b>：同锚点元素沿堆叠方向<b>单调推进游标</b>（下一个元素的起点 = 上一个元素
 *       实际占位的边缘 + {@link HudAnchor#STACK_GAP}）。因为游标只前进、不回头，两组相邻元素不可能交叠 ——
 *       不需要"逐元素两两求交"的 O(n²) 检查。</li>
 *   <li><b>与原生 HUD 自动避让</b>：占用矩形若与 {@link AkaishiVanillaHudRegions} 的保留区相交，
 *       就沿<b>堆叠方向</b>整体推开（至少 {@link #MIN_SHIFT} 像素）直到不冲突；推开方向恒为"远离组内已放元素"
 *       的方向，故避让不会破坏保证 ①。</li>
 * </ol>
 *
 * <p><b>为什么不会跑出屏幕</b>：避让循环结束后，y 一律再夹到 {@code [0, screenHeight - height]}；
 * 也就是说"屏幕内"优先于"完全避开" —— 若某个锚点被原版元素挤满，元素宁可贴边显示也不越界。
 * 反过来说，本层不保证"任何情况下都完全不与原生 HUD 重叠"，只保证"越界不可能、重叠是最后兜底"。
 *
 * <p><b>性能</b>：稳态（元素表版本未变且屏幕尺寸未变）下本帧只做：一次版本号比较 + 每个元素一次
 * {@code measure} + 常量级避让判定 ⇒ 无排序、无集合/数组分配。排序与元素表重建只在注册/注销时发生。
 *
 * <p><b>异常隔离</b>：单个元素在 {@code measure/render} 里抛错只记录日志并跳过它，不影响其它元素。
 */
public final class AkaishiHudLayer implements IGuiOverlay {

    private static final Logger LOGGER = LogManager.getLogger("akaishi.hud");

    /** 顺延一次的最小步长（逻辑像素，待调手感值）：至少一行字，保证"挪过看得见"。 */
    private static final int MIN_SHIFT = 9;
    /** 单元素最多顺延次数（待调手感值）：防止在密集保留区之间来回弹跳。 */
    private static final int MAX_SHIFT_ATTEMPTS = 4;

    /** 绘制顺序：先按锚点枚举序，再按 priority，最后保持注册顺序（List.sort 稳定）。 */
    private static final Comparator<AkaishiHudElement> ORDER =
            Comparator.comparingInt((AkaishiHudElement element) -> element.anchor().ordinal())
                    .thenComparingInt(AkaishiHudElement::priority);

    /** 每帧复用的上下文（零分配，见 {@link HudRenderContext} 的说明）。 */
    private final HudRenderContext context = new HudRenderContext();

    /** 排序后的元素表（按版本号缓存）。 */
    private AkaishiHudElement[] elements = new AkaishiHudElement[0];
    private int cachedVersion = -1;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        // 与既有各 overlay 同口径：打开界面 / F1 隐藏 HUD 时整层不画
        if (minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        AkaishiHudElement[] all = sortedElements();
        if (all.length == 0) {
            return;
        }
        HudLayoutRect[] reserved = AkaishiVanillaHudRegions.regions(screenWidth, screenHeight);

        HudAnchor groupAnchor = null;
        HudAnchor.StackDirection direction = HudAnchor.StackDirection.DOWN;
        int cursor = 0;
        for (AkaishiHudElement element : all) {
            try {
                if (!element.isVisible()) {
                    continue; // 不可见元素既不绘制也不占位（其余元素自然收拢）
                }
                HudAnchor anchor = element.anchor();
                int available = anchor.availableWidth(screenWidth);
                AkaishiHudElement.HudSize size = element.measure(available);
                if (size == null || size.width() <= 0 || size.height() <= 0) {
                    continue;
                }
                if (anchor != groupAnchor) {
                    groupAnchor = anchor;
                    direction = anchor.stackDirection();
                    cursor = anchor.anchorLineY(screenHeight);
                }
                boolean down = direction == HudAnchor.StackDirection.DOWN;

                int x = anchor.anchorX(screenWidth, size.width()) + element.offsetX();
                int y = down
                        ? cursor + element.offsetY()
                        : cursor - element.offsetY() - size.height();

                // 与原版 HUD 保留区避让：整体沿堆叠方向推开（至少 MIN_SHIFT）
                for (int attempt = 0; attempt < MAX_SHIFT_ATTEMPTS; attempt++) {
                    int shift = requiredShift(x, y, size.width(), size.height(), down, reserved);
                    if (shift == 0) {
                        break;
                    }
                    y += down ? shift : -shift;
                }

                // 始终夹在屏幕内：避让不许把元素推出屏外
                x = Math.max(0, Math.min(screenWidth - size.width(), x));
                y = Math.max(0, Math.min(screenHeight - size.height(), y));

                context.prepare(graphics, minecraft.font, partialTick, screenWidth, screenHeight,
                        anchor, available, x, y, size.width(), size.height());
                element.render(context);

                // 游标推进到本次实际占位的下一行（用避让/夹取后的 y ⇒ 组内仍不重叠）
                cursor = down
                        ? y + size.height() + HudAnchor.STACK_GAP
                        : y - HudAnchor.STACK_GAP;
            } catch (RuntimeException e) {
                // 附属元素抛错不能拖垮整帧 HUD
                LOGGER.error("HUD 元素 [{}] 绘制失败，已跳过本帧", element.id(), e);
            }
        }
    }

    /**
     * 计算"为避开原版保留区需要沿堆叠方向再挪多少像素"，0 = 已无冲突。
     *
     * <p>取所有相交保留区所需位移的<b>最大值</b>：一趟即可越过全部冲突区，
     * 再配合 {@link #MAX_SHIFT_ATTEMPTS} 次外层循环处理"挪过去又撞上另一块"的连锁情况。
     */
    private static int requiredShift(int x, int y, int width, int height,
                                     boolean down, HudLayoutRect[] reserved) {
        int shift = 0;
        for (HudLayoutRect region : reserved) {
            if (!region.intersects(x, y, width, height)) {
                continue;
            }
            int needed = down
                    ? region.bottom() + HudAnchor.STACK_GAP - y
                    : (y + height) - (region.y() - HudAnchor.STACK_GAP);
            shift = Math.max(shift, Math.max(MIN_SHIFT, needed));
        }
        return shift;
    }

    /** 元素表（按锚点/优先级/注册序排序）；版本号未变时复用上一次的数组。 */
    private AkaishiHudElement[] sortedElements() {
        int version = AkaishiHudRegistry.version();
        if (version != cachedVersion) {
            List<AkaishiHudElement> sorted = new ArrayList<>(AkaishiHudRegistry.snapshot());
            sorted.sort(ORDER);
            elements = sorted.toArray(new AkaishiHudElement[0]);
            cachedVersion = version;
        }
        return elements;
    }
}
