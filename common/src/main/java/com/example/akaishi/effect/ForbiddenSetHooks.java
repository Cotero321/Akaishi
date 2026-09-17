package com.example.akaishi.effect;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 「禁忌」四件套的跨模块钩子（D257 依赖倒置）。
 *
 * <p>common 侧不可见 Curios API，无法自行统计佩戴件数，故此处只定义抽象，
 * 由平台层（forge）在初始化时注入实现。缺省实现全部返回中性值，
 * 保证未注入时行为与既有版本一致。</p>
 */
public final class ForbiddenSetHooks {

    /** 不可名状等级加成：套装集齐后所有施加入口统一 +1（D73/D191） */
    public interface UnnameableBonus {
        int bonusFor(LivingEntity wearer);
    }

    /** 视野扭曲抑制判定：套装集齐则屏蔽视觉扭曲表现（D43，仅客户端） */
    public interface DistortionSuppressor {
        boolean isSuppressed(LocalPlayer player);
    }

    /** 槽位封锁判定：佩戴任意一件禁忌饰品即锁死全部 9 槽，全摘下才解锁（D13/D16/D115） */
    public interface SocketLockQuery {
        boolean isLocked(Player player);
    }

    /**
     * 单件禁忌饰品的展示快照（躯体检查仪「饰品区」，D96）。
     * 顺序固定为 生命之触 / 幼崽之心 / 母神之印 / 孕育之环，与 socket_1..4 一一对应。
     */
    public record CurioState(boolean worn, double erosion) {
    }

    /** 四件禁忌饰品状态查询：始终返回 4 条（未佩戴以 worn=false 占位） */
    public interface CurioStateProvider {
        List<CurioState> states(Player player);
    }

    private static volatile UnnameableBonus unnameableBonus = wearer -> 0;
    private static volatile DistortionSuppressor distortionSuppressor = player -> false;
    private static volatile SocketLockQuery socketLockQuery = player -> false;
    private static volatile CurioStateProvider curioStateProvider = player -> List.of();

    private ForbiddenSetHooks() {
    }

    /** 佩戴者应追加的不可名状等级（未注入或未集齐时为 0） */
    public static int unnameableBonus(LivingEntity wearer) {
        return unnameableBonus.bonusFor(wearer);
    }

    /** 是否应屏蔽本地玩家的视野扭曲表现 */
    public static boolean isDistortionSuppressed(LocalPlayer player) {
        return distortionSuppressor.isSuppressed(player);
    }

    /** 9 槽是否被禁忌饰品锁死（未注入时为 false，即不干预既有改造流程） */
    public static boolean socketsLocked(Player player) {
        return socketLockQuery.isLocked(player);
    }

    public static void setUnnameableBonus(UnnameableBonus impl) {
        unnameableBonus = impl;
    }

    public static void setDistortionSuppressor(DistortionSuppressor impl) {
        distortionSuppressor = impl;
    }

    public static void setSocketLockQuery(SocketLockQuery impl) {
        socketLockQuery = impl;
    }

    /** 四件禁忌饰品当前状态；未注入时返回空列表，GUI 饰品区自动隐藏 */
    public static List<CurioState> curioStates(Player player) {
        return curioStateProvider.states(player);
    }

    public static void setCurioStateProvider(CurioStateProvider impl) {
        curioStateProvider = impl;
    }

    /** 锁槽拒绝反馈：聊天红字 + 失败音效（D95），供各改造入口统一调用 */
    public static void denyLocked(Player player) {
        player.sendSystemMessage(Component.translatable("message.akaishi.forbidden.socket_locked"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(),
                SoundSource.PLAYERS, 0.7F, 0.6F);
    }
}
