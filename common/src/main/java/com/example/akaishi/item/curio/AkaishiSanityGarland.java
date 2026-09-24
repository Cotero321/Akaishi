package com.example.akaishi.item.curio;

import com.example.akaishi.api.sanity.SanityChangeSource;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.sanity.SanityServiceImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 花环（{@code akaishi:sanity_garland}，charm 槽）：佩戴期间缓慢回稳理智，代价是自身被消耗。
 *
 * <p><b>走"耐久"而不是自建进度字段</b>：规格是"每秒 -1 耐久、上限 720 耐久 = 最长 720 秒"，
 * 这与原版耐久语义完全重合（{@code ItemStack} 的 {@code Damage} 随物品走、进 tooltip 耐久条、
 * 可被修补类逻辑识别），故直接用 {@code Item.Properties#durability(720)}：
 * <ul>
 *   <li>"进度随物品走"免费成立（摘下、转手、放箱子都保留）；</li>
 *   <li>"剩余时间"就是 {@code getMaxDamage() - getDamageValue()}，玩家看耐久条即知，无需自绘；</li>
 *   <li>不新增存档段、不新增同步逻辑（物品 NBT 本来就会随容器同步）。</li>
 * </ul>
 *
 * <p><b>为什么必须"缓冲 + 周期落盘"</b>：{@code curioTick} 每 tick 都被 Curios 调用，
 * 每 tick 改一次 NBT 会持续标记容器脏、把物品栈反复推给客户端同步（项链/花环这类常驻饰品
 * 会把网络与 GC 吃满）。故照 {@link AkaishiSocketCurioItem} 的范式：按"佩戴者弱引用 → 槽位"
 * 暂存未落盘 tick 数，累计满 {@link #FLUSH_TICKS}（= 1 秒）才写一次 {@code Damage}
 * —— 而这恰好就是"每秒 1 点耐久"的节拍，落盘频率与规格完全一致（不存在额外节流损失）。
 *
 * <p><b>每秒补多少 SAN</b>：规格"每分钟 1 点"，即每 {@link #SAN_PERIOD_SECONDS} 秒补
 * {@link #SAN_PER_PERIOD} 点。<b>触发方式刻意选"以耐久值对齐"</b>（{@code damage % 60 == 0}）：
 * 耐久本身就是"已佩戴秒数"的权威记录，这样 SAN 节拍自动随物品走（中途摘下再戴上不会重复领取，
 * 也不会丢失进度），不必再存一个"上次补 SAN 是第几秒"的字段。
 *
 * <p><b>恢复量不乘任何系数</b>（判断依据）：
 * <ul>
 *   <li>{@code SanityCogCurve.naturalRegenMultiplier}（认知值 → 自然恢复倍率）作用于
 *       <b>自然回复速率</b>，即"什么都不做时每秒缓慢回的那一点"，花环是<b>道具恢复</b>，不是自然恢复，
 *       若相乘会变成"高认知戴花环收益翻倍"，把一件饰品的收益绑在认知成长上，与该系数的设计归属不符；</li>
 *   <li>食补效力系数（{@code foodEfficiency}）作用于 {@code SanityFoodRegistry} 的档位链路，花环不经那条链路，
 *       同理不适用。</li>
 * </ul>
 *
 * <p><b>损毁表现</b>：耐久耗尽（{@code Damage} 达到上限）时花环从饰品槽消失，
 * 播放原版 {@link SoundEvents#ITEM_BREAK}（不新增音效资源），并给佩戴者一条 actionbar 提示。
 *
 * <p><b>总开关关闭时</b>：整套暂停（不推进耐久、不补 SAN），与配置项
 * {@code toggles.sanityEnabled} 的"关 = 全停"口径一致（否则关了理智系统还会白掉耐久）。
 *
 * <p>所有数值均为<b>待调手感值</b>。
 */
public class AkaishiSanityGarland extends Item implements ICurioItem {

    /** 允许佩戴的槽位：charm（与既有赤石护符同槽） */
    private static final String SLOT_CHARM = "charm";

    /** 耐久上限 = 最长佩戴秒数：720（720s = 12 分钟；待调手感值） */
    public static final int MAX_DURABILITY = 720;

    /** 每秒消耗的耐久：1 点（待调手感值） */
    private static final int DAMAGE_PER_SECOND = 1;

    /** 每多少秒补 1 点 SAN：60s（规格"每分钟 1 点"，= 1200t；待调手感值） */
    private static final int SAN_PERIOD_SECONDS = 60;

    /** 每次补的 SAN：1 点（待调手感值） */
    private static final float SAN_PER_PERIOD = 1f;

    /** NBT 落盘节流周期（tick）：20 = 1 秒，与"每秒 1 点耐久"同拍（照 {@code AkaishiSocketCurioItem} 的范式） */
    private static final int FLUSH_TICKS = 20;

    /**
     * 未落盘的佩戴 tick 缓冲：佩戴者 → （槽位 → [累计 tick]）。
     *
     * <p>用 {@link WeakHashMap} + 同步包装，佩戴者离线/被回收后条目自动消失，不钉住实体；
     * 桶与 {@code AkaishiSocketCurioItem} 的 {@code PENDING} 同构（[0] 只存亚秒级残值，最大 19）。
     */
    private static final Map<Entity, Map<String, int[]>> PENDING =
            Collections.synchronizedMap(new WeakHashMap<>());

    public AkaishiSanityGarland(Properties properties) {
        super(properties);
    }

    // ==================== 数值查询（tooltip / 调试共用一处口径）====================

    /** 剩余可佩戴秒数（耐久未落的余量） */
    public static int remainingSeconds(ItemStack stack) {
        return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
    }

    // ==================== Curios 契约 ====================

    /** 只允许 charm 槽（与既有一众护符同槽位，玩家可自由取舍） */
    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return SLOT_CHARM.equals(slotContext.identifier());
    }

    /**
     * 佩戴期每 tick 推进（Curios 只在"已佩戴"时调用，天然满足"佩戴才消耗"）：
     * 缓冲满 1 秒 → 写 1 点耐久 → 到 60 秒倍刻 → 补 1 点 SAN；耐久耗尽则损毁。
     */
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        Entity wearer = slotContext.entity();
        if (!(wearer instanceof Player player) || player.level().isClientSide) {
            return;
        }
        String slot = slotContext.identifier();
        if (!ModConfig.sanityEnabled) {
            clearPending(wearer, slot);
            return; // 总开关关闭：整套暂停（见类注释）
        }
        int[] buffer = PENDING.computeIfAbsent(wearer, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(slot, key -> new int[1]);
        int seconds;
        synchronized (buffer) {
            buffer[0] += 1;
            seconds = buffer[0] / FLUSH_TICKS;
            buffer[0] -= seconds * FLUSH_TICKS;
        }
        if (seconds <= 0) {
            return; // 未满 1 秒：不写 NBT（这就是"缓冲"的全部意义）
        }
        int next = stack.getDamageValue() + DAMAGE_PER_SECOND * seconds;
        if (next >= MAX_DURABILITY) {
            // 耗尽：先把最后一分钟的 SAN 结清（720 % 60 == 0，本是该补的一次），再损毁
            grantIfDue(player, MAX_DURABILITY);
            breakGarland(player, stack);
            clearPending(wearer, slot);
            return;
        }
        stack.setDamageValue(next);
        grantIfDue(player, next);
    }

    /** 到整分钟倍刻即补 SAN（倍刻由耐久值本身记录，故进度随物品走、无需额外字段） */
    private static void grantIfDue(Player player, int damageSeconds) {
        if (damageSeconds % SAN_PERIOD_SECONDS == 0) {
            // 标签用 INTERNAL：本条不是环境规则、不是食补档位、也不是 SANC 首用（api.sanity 本轮冻结，不新增枚举值），
            // 与 SanityKillReward 的"模组内部奖励"同类用法一致。
            SanityServiceImpl.instance().addSanInternal(player, SAN_PER_PERIOD, SanityChangeSource.INTERNAL);
        }
    }

    /** 损毁：耐久顶满、从饰品槽移除、播原版物品损坏音、给佩戴者一条提示（不新增音效资源） */
    private static void breakGarland(Player player, ItemStack stack) {
        stack.setDamageValue(MAX_DURABILITY);
        stack.shrink(stack.getCount());
        player.displayClientMessage(Component.translatable("message.akaishi.sanity.garland_broken"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    /** 丢弃某佩戴者某槽位的未落盘缓冲（总开关关闭 / 损毁时调用；亚秒级残值无信息量） */
    private static void clearPending(Entity wearer, String slot) {
        Map<String, int[]> slots = PENDING.get(wearer);
        if (slots != null) {
            slots.remove(slot);
        }
    }

    /** 说明两行：效果 + 剩余可佩戴时长（耐久条之外的直观读数） */
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.akaishi.sanity_garland.tooltip"));
        tooltip.add(Component.translatable("item.akaishi.sanity_garland.remaining",
                Mth.floor(remainingSeconds(stack) / 60f), remainingSeconds(stack) % 60));
    }
}
