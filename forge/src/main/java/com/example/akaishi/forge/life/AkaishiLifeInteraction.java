package com.example.akaishi.forge.life;

import com.example.akaishi.life.sample.AkaishiSampleCollectorItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 生命科技交互事件（Forge）：
 * 手持样本采集器右键生物时执行采集，成功后取消默认交互（避免触发喂食等）。
 * 采集逻辑本体在 common（AkaishiSampleCollectorItem），本类做事件桥接，
 * 并在此维护"单个生物个体最多成功采集 3 份"的上限（持久数据随实体生命周期自动清理）。
 */
public final class AkaishiLifeInteraction {

    public static final AkaishiLifeInteraction INSTANCE = new AkaishiLifeInteraction();

    /** 实体持久数据键：该个体已成功采集的样本份数 */
    private static final String TAG_SAMPLES = "AkaishiSamplesCollected";
    /** 单个生物个体的样本采集上限 */
    private static final int MAX_PER_ENTITY = 3;

    private AkaishiLifeInteraction() {
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack hand = player.getMainHandItem();
        if (!(hand.getItem() instanceof AkaishiSampleCollectorItem)) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        int collected = data.getInt(TAG_SAMPLES);
        // 该个体已采满 3 份：拦截并提示，不再消耗能量
        if (collected >= MAX_PER_ENTITY) {
            player.sendSystemMessage(Component.translatable("message.akaishi.sample.exhausted"));
            event.setCanceled(true);
            return;
        }
        int result = AkaishiSampleCollectorItem.tryCollect(player, target, hand);
        if (result == AkaishiSampleCollectorItem.RESULT_SUCCESS) {
            // 成功份数 +1；采满时补一条提示，让玩家知道该个体已枯竭
            data.putInt(TAG_SAMPLES, collected + 1);
            if (collected + 1 >= MAX_PER_ENTITY) {
                player.sendSystemMessage(Component.translatable("message.akaishi.sample.exhausted"));
            }
            event.setCanceled(true);
        } else if (result == AkaishiSampleCollectorItem.RESULT_FAIL) {
            // 失败（样本流失）：同样消耗本次交互，防止误触发喂食/交易
            event.setCanceled(true);
        }
        // RESULT_NONE（无样本组/能量不足）：放行默认交互
    }
}
