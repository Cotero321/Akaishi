package com.example.akaishi.forge.life;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.effect.ScreenFlashS2C;
import com.example.akaishi.item.curio.AkaishiSocketCurioItem;
import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 「禁忌」四件饰品的侵蚀推进层（D80/D81/D56/D58）。
 *
 * <p>进度本身的累加写在 {@link AkaishiSocketCurioItem#curioTick}（随物走、佩戴才推进），
 * 本类只消费结果：① 阈值低语提示；② 跑满结算——把佩戴者 9 槽全部乱码并重抽数值。</p>
 *
 * <p>幂等（D75/D189）：结算标记写物品 NBT，跑满只结算一次；已乱码的器官不再重抽，
 * 转手给他人佩戴亦不会二次污染（标记随物品走，D190）。</p>
 */
public final class AkaishiForbiddenErosionHandler {

    public static final AkaishiForbiddenErosionHandler INSTANCE = new AkaishiForbiddenErosionHandler();

    /** 已通知到的阈值下标（随物品 NBT 走，跨登录不重复刷屏） */
    private static final String TAG_NOTICE = "ForbiddenErosionNotice";
    /** 跑满结算标记（D189：结算即置位，杜绝重复乱码与重复重抽） */
    private static final String TAG_SETTLED = "ForbiddenErosionSettled";

    /** 兜底低语计时：玩家 → 上次低语 tick（仅内存，重登即失效） */
    private static final Map<Player, Integer> LAST_WHISPER = new WeakHashMap<>();

    /** 检查节流：20 tick 一次（秒级精度足够，避免每 tick 遍历 Curios 容器） */
    private static final int CHECK_INTERVAL = 20;

    private AkaishiForbiddenErosionHandler() {
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) {
            return;
        }
        Player player = event.player;
        if (!ModConfig.erosionEnabled || player.isDeadOrDying() || player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        double highest = 0.0;
        for (ItemStack piece : AkaishiForbiddenCurios.wornPieces(player)) {
            double erosion = AkaishiSocketCurioItem.getErosion(piece);
            if (erosion <= 0.0) {
                continue;
            }
            highest = Math.max(highest, erosion);
            noticeThreshold(player, piece, erosion);
            if (AkaishiSocketCurioItem.isErosionMaxed(piece)) {
                settle(player, piece);
            }
        }
        fallbackWhisper(player, highest);
    }

    // ==================== 阈值提示（D80）====================

    /** 跨越一个或多个必报节点即低语一次（跑满交结算提示，此处不重复） */
    private static void noticeThreshold(Player player, ItemStack piece, double erosion) {
        int[] thresholds = ModConfig.erosionNoticeThresholds;
        if (thresholds == null || thresholds.length == 0) {
            return;
        }
        CompoundTag tag = piece.getOrCreateTag();
        int passed = tag.getInt(TAG_NOTICE);
        int next = passed;
        double percent = erosion * 100.0;
        // 一次跨越多个节点（如配置关闭期累积）只报一次，下标直接推到最新
        while (next < thresholds.length && percent >= thresholds[next]) {
            next++;
        }
        if (next == passed) {
            return;
        }
        tag.putInt(TAG_NOTICE, next);
        if (erosion < 1.0) {
            player.sendSystemMessage(Component.translatable("message.akaishi.forbidden.erosion.notice",
                    percentText(percent)));
        }
    }

    // ==================== 兜底低语（D81）====================

    /** 固定间隔兜底提醒，避免玩家长时间佩戴却毫无反馈 */
    private static void fallbackWhisper(Player player, double highest) {
        int minutes = ModConfig.erosionNoticeIntervalMinutes;
        if (minutes <= 0 || highest <= 0.0) {
            return;
        }
        int now = player.tickCount;
        Integer last = LAST_WHISPER.get(player);
        if (last == null) {
            LAST_WHISPER.put(player, now);
            return;
        }
        if (now - last < minutes * 60 * 20) {
            return;
        }
        LAST_WHISPER.put(player, now);
        player.sendSystemMessage(Component.translatable("message.akaishi.forbidden.erosion.notice",
                percentText(highest * 100.0)));
    }

    // ==================== 跑满结算（D56/D58）====================

    /** 跑满一次：9 槽全部乱码（跳过已乱码，D75）+ 首次乱码时重抽数值（D50/D57） */
    private static void settle(Player player, ItemStack piece) {
        CompoundTag tag = piece.getOrCreateTag();
        if (tag.getBoolean(TAG_SETTLED)) {
            return;
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return;
        }
        tag.putBoolean(TAG_SETTLED, true);
        int corrupted = 0;
        for (BodySlot slot : BodySlot.values()) {
            ItemStack organ = state.getOrgan(slot);
            if (organ.isEmpty()) {
                // 空槽先补一具原生部件再一并乱码，避免留下未污染的缺口（D23/D24/D25）
                if (!state.implantOrgan(slot, AkaishiOrganItem.createNative(slot))) {
                    continue;
                }
                organ = state.getOrgan(slot);
                if (organ.isEmpty()) {
                    continue;
                }
            }
            if (AkaishiOrganItem.isCorrupted(organ)) {
                continue;
            }
            AkaishiOrganItem.setCorrupted(organ);
            rerollStats(organ, player.getRandom());
            corrupted++;
        }
        player.sendSystemMessage(Component.translatable("message.akaishi.forbidden.erosion.maxed", corrupted));
        // 跑满瞬间屏幕边缘泛红（D100/D103/D208）：4.5s 满强度血色，可经配置关闭
        if (ModConfig.erosionScreenFlashEnabled && player instanceof ServerPlayer serverPlayer) {
            ScreenFlashS2C.sendToPlayer(serverPlayer, 90, 1.0f);
        }
    }

    /**
     * 数值重抽：仅波动适配度（± {@code erosionStatRerollPercent}）。
     * 不动完整度——完整度决定品质档位，改动会连锁改变词条容量与倍率，与 D57「只重抽数值」相悖。
     */
    private static void rerollStats(ItemStack organ, RandomSource random) {
        double percent = Math.abs(ModConfig.erosionStatRerollPercent);
        if (percent <= 0.0) {
            return;
        }
        int compat = AkaishiOrganItem.getCompat(organ);
        double delta = compat * percent;
        double rolled = compat + (random.nextDouble() * 2.0 - 1.0) * delta;
        AkaishiOrganItem.setCompat(organ, (int) Math.round(Mth.clamp(rolled, 1.0, AkaishiOrganItem.MAX_COMPAT)));
    }

    // ==================== 会话边界 ====================

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_WHISPER.remove(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LAST_WHISPER.remove(event.getEntity());
    }

    private static String percentText(double percent) {
        return String.valueOf((int) Math.round(percent));
    }
}
