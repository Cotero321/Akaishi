package com.example.akaishi.api.hud;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一 HUD 元素注册表（对外 API，<b>仅客户端</b>）：本模组与附属的 HUD 元素都从这里登记。
 *
 * <p><b>注册时机</b>：客户端初始化阶段（本模组在 {@code RegisterGuiOverlaysEvent} 内注册）—
 * 即第一帧渲染之前。运行期也可注册/注销，但会带来一次"元素表重建"（见下），不要每帧调。
 *
 * <p><b>线程</b>：本类是线程安全的（{@link CopyOnWriteArrayList} + 原子版本号）；
 * 渲染层按<b>版本号</b>缓存排序后的元素数组，只有版本变化才重建 ⇒ 稳态下渲染路径零分配、零排序。
 *
 * <p><b>id 抢占</b>：注册同 id 的元素会<b>替换</b>旧元素（与项目其它注册表的"id 抢占"口径一致），
 * 便于附属覆盖内置 HUD。
 */
public final class AkaishiHudRegistry {

    private static final List<AkaishiHudElement> ELEMENTS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger VERSION = new AtomicInteger();

    private AkaishiHudRegistry() {
    }

    /** 注册（或按 id 抢占替换）一个 HUD 元素。 */
    public static void register(AkaishiHudElement element) {
        if (element == null || element.id() == null) {
            return;
        }
        ELEMENTS.removeIf(existing -> existing.id().equals(element.id()));
        ELEMENTS.add(element);
        VERSION.incrementAndGet();
    }

    /** 按 id 注销；返回是否真的移除了元素。 */
    public static boolean unregister(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        boolean removed = ELEMENTS.removeIf(existing -> existing.id().equals(id));
        if (removed) {
            VERSION.incrementAndGet();
        }
        return removed;
    }

    /** 当前全部元素的<b>不可变快照</b>（按注册顺序）。 */
    public static List<AkaishiHudElement> snapshot() {
        return List.copyOf(ELEMENTS);
    }

    /** 元素表版本号：每次注册/注销自增，供渲染层做缓存失效判断。 */
    public static int version() {
        return VERSION.get();
    }
}
