package com.example.akaishi.forge.life;

import com.example.akaishi.item.AkaishiLifeEnergyWandItem;
import com.example.akaishi.life.sample.AkaishiSampleCollectorItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 生命科技交互事件（Forge）：
 * ① 手持样本采集器右键生物时执行采集，成功后取消默认交互（避免触发喂食等）。
 * ② 手持生命能量权杖右键方块时，在方块 use() 之前接管交互（避免发射器抢先打开 GUI）。
 * 逻辑本体在 common（AkaishiSampleCollectorItem / AkaishiLifeEnergyWandItem），本类只做事件桥接。
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

    /**
     * 手持权杖右键方块：原版流程中 BlockState.use() 先于 ItemStack.useOn() 执行，
     * 导致发射器方块会抢先打开 GUI、权杖永远选不中。这里在 RightClickBlock 阶段
     * （早于 block.use）先跑权杖逻辑，成功则取消事件、阻止方块 use()。
     */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Player player = event.getEntity();
        ItemStack hand = player.getItemInHand(event.getHand());
        if (!(hand.getItem() instanceof AkaishiLifeEnergyWandItem wand)) {
            return;
        }
        UseOnContext ctx = new UseOnContext(player, event.getHand(), event.getHitVec());
        InteractionResult result = wand.useOn(ctx);
        if (result.consumesAction()) {
            // 权杖已接管本次交互：取消事件。setCanceled(true) 会连带把 useBlock/useItem 置为 DENY，
            // 从而阻止发射器方块 use() 抢开 GUI 与 ItemStack.useOn() 重复执行；
            // 取消结果设为 SUCCESS，与客户端侧的预测保持一致，避免手臂摆动后无响应。
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
