package com.example.akaishi.item.curio;

import com.example.akaishi.config.ModConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 「禁忌」系扩展槽饰品抽象基类。
 *
 * <p>与 {@link AkaishiCurioItem} 的关键差异（D10）：本系四件为纯被动零消耗、不承载赤能源，
 * 因此不复用赤石饰品的能量体系，也不继承它（避免子类被迫实现无关的能量接口）。</p>
 *
 * <p>共性职责：
 * <ul>
 *   <li>槽位隔离（D38）：每件严格绑定 akaishi_socket_1..4 中唯一一槽，由 {@link #socketId()} 声明；</li>
 *   <li>侵蚀进度（D13/D186）：进度随物品走（存物品 NBT），摘下、转手均保留；</li>
 *   <li>死亡不掉落（D183）：视同绑定物，掉落规则固定 ALWAYS_KEEP；</li>
 *   <li>tooltip 分页（D238 修订）：默认页 = 低语 → 空行 → 自身效果行 → Shift 提示；
 *       第二页（按住 Shift）= 套装说明与状态 → 侵蚀进度 → 跑满警示；
 *       文字动效（D259）统一经 {@link AkaishiTooltipFx}：低语行呼吸流动，其余行明度蠕动。</li>
 * </ul>
 * </p>
 *
 * <p>子类只提供文案键与自身槽位标识；效果的生效实现由平台层（Forge 事件桥）统一接管，
 * 保证「展示与生效同源」。</p>
 */
public abstract class AkaishiSocketCurioItem extends Item implements ICurioItem {

    /** 侵蚀进度 NBT 键（0.0 ~ 1.0） */
    public static final String TAG_EROSION = "ForbiddenErosion";

    /** 侵蚀进度条格数（D98：十格文字条） */
    private static final int EROSION_BAR_CELLS = 10;

    protected AkaishiSocketCurioItem(Properties properties) {
        super(properties);
    }

    /** 本饰品独占的扩展槽标识（akaishi_socket_1..4），仅用于槽位隔离校验 */
    public abstract String socketId();

    /** 世界观低语文案键（B 组，文本自带 §5§o 前缀） */
    protected abstract String whisperKey();

    /** 技术效果行文案键（C 组，按声明顺序渲染） */
    protected abstract String[] effectKeys();

    // ==================== 侵蚀进度（D13/D186：进度随物品 NBT 走）====================

    /** 读取侵蚀进度（0.0 ~ 1.0，缺省 0） */
    public static double getErosion(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? Mth.clamp(tag.getDouble(TAG_EROSION), 0.0, 1.0) : 0.0;
    }

    /** 写入侵蚀进度（自动钳制到 [0, 1]） */
    public static void setErosion(ItemStack stack, double progress) {
        stack.getOrCreateTag().putDouble(TAG_EROSION, Mth.clamp(progress, 0.0, 1.0));
    }

    /** 是否已跑满（D56 污染结算的门槛） */
    public static boolean isErosionMaxed(ItemStack stack) {
        return getErosion(stack) >= 1.0;
    }

    /**
     * 佩戴中的侵蚀累加缓冲（D186）：按「佩戴者弱引用 → 槽位」暂存未落盘进度，
     * 每 {@code erosionNbtFlushTicks} 个 tick 才写一次物品 NBT，
     * 避免每 tick 改动 NBT 触发容器脏标记与同步开销。
     */
    private static final Map<Entity, Map<String, double[]>> PENDING =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Curios 每 tick 驱动（仅佩戴期间被调用，天然满足 D13「佩戴才推进」）：
     * 按 {@code erosionDurationMinutes} 折算单 tick 增量并累加缓冲，到周期即落盘。
     */
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        Entity wearer = slotContext.entity();
        if (wearer == null || wearer.level().isClientSide) {
            return;
        }
        String slot = slotContext.identifier();
        // 关闭侵蚀 / 已跑满：停表并丢弃残留缓冲（跑满后无增量意义）
        if (!ModConfig.erosionEnabled || isErosionMaxed(stack)) {
            clearPending(wearer, slot);
            return;
        }
        double perTick = erosionPerTick();
        if (perTick <= 0.0) {
            return;
        }
        int flushTicks = Math.max(1, ModConfig.erosionNbtFlushTicks);
        double[] buffer = PENDING.computeIfAbsent(wearer, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(slot, key -> new double[2]);
        double pending;
        boolean flushNow;
        synchronized (buffer) {
            buffer[0] += perTick;   // [0] 累计进度
            buffer[1] += 1.0;       // [1] 累计 tick
            pending = buffer[0];
            flushNow = buffer[1] >= flushTicks;
            if (flushNow) {
                buffer[0] = 0.0;
                buffer[1] = 0.0;
            }
        }
        if (flushNow) {
            setErosion(stack, getErosion(stack) + pending);
        }
    }

    /** 单 tick 侵蚀增量（跑满总时长 = erosionDurationMinutes 分钟） */
    private static double erosionPerTick() {
        int minutes = Math.max(1, ModConfig.erosionDurationMinutes);
        return 1.0 / (minutes * 60.0 * 20.0);
    }

    /** 丢弃某佩戴者某槽位的未落盘缓冲（配置关闭 / 跑满时调用） */
    private static void clearPending(Entity wearer, String slot) {
        Map<String, double[]> slots = PENDING.get(wearer);
        if (slots != null) {
            slots.remove(slot);
        }
    }

    // ==================== Curios 契约 ====================

    /** 槽位隔离（D38）：只接受自身声明的扩展槽 */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return socketId().equals(slotContext.identifier());
    }

    /**
     * 死亡不掉落（D183）：禁忌件视同绑定物。
     * 注意（D181）：此处刻意不覆写 canUnequip —— 佩戴中的常规取下始终允许，
     * 「跑满」只封锁器官槽位，不封锁饰品本身。
     */
    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source,
                                       int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    /** 跑满后物品名加 ▓ 包裹（D99），与红字警示行共同构成唯一的满值视觉区分（D212） */
    @Override
    public Component getName(ItemStack stack) {
        Component base = super.getName(stack);
        return isErosionMaxed(stack)
                ? Component.translatable("item.akaishi.curio.erosion.maxed_name", base)
                : base;
    }

    // ==================== tooltip（D238 修订：默认页）====================

    /**
     * 默认页只放本件自带的说明；套装与侵蚀等扩展信息交由平台侧在「按住 Shift」时注入
     * （判定需客户端 Shift 状态，common 侧不可引用，见 {@code AkaishiForbiddenTooltipHandler}）。
     */
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(AkaishiTooltipFx.whispering(Component.translatable(whisperKey())));
        tooltip.add(Component.empty());
        for (String key : effectKeys()) {
            tooltip.add(AkaishiTooltipFx.crawling(Component.translatable(key)));
        }
        // 占位栏位行：直接以本件声明的 socketId 反查 Curios 槽位显示名，避免文案与槽位脱钩
        tooltip.add(AkaishiTooltipFx.crawling(Component.translatable(
                "item.akaishi.curio.forbidden.slot",
                Component.translatable("curios.identifier." + socketId()))));
        tooltip.add(AkaishiTooltipFx.crawling(Component.translatable("item.akaishi.curio.forbidden.shift_hint")));
    }

    /** 第二页：侵蚀进度行 + 跑满警示（供平台侧在按住 Shift 时调用） */
    public static void appendErosionInfo(List<Component> tooltip, ItemStack stack) {
        tooltip.add(AkaishiTooltipFx.crawling(erosionLine(stack)));
        if (isErosionMaxed(stack)) {
            tooltip.add(AkaishiTooltipFx.crawling(Component.translatable("item.akaishi.curio.erosion.maxed")));
        }
    }

    /** 侵蚀进度行（D253：进度为 0 时显示「未启动」，一旦 >0 即切回十格进度条） */
    private static Component erosionLine(ItemStack stack) {
        double progress = getErosion(stack);
        if (progress <= 0.0) {
            return Component.translatable("item.akaishi.curio.erosion.idle");
        }
        return Component.translatable("item.akaishi.curio.erosion.line",
                erosionBar(progress), (int) Math.round(progress * 100));
    }

    /** 十格文字进度条（D98）：已填充 █，未填充 ░，零贴图 */
    private static String erosionBar(double progress) {
        int filled = Mth.clamp((int) (progress * EROSION_BAR_CELLS), 0, EROSION_BAR_CELLS);
        StringBuilder bar = new StringBuilder(EROSION_BAR_CELLS);
        for (int i = 0; i < EROSION_BAR_CELLS; i++) {
            bar.append(i < filled ? '█' : '░');
        }
        return bar.toString();
    }
}
