package com.example.template.forge.life;

import com.example.template.life.sample.ChishiSampleCollectorItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 生命科技交互事件（Forge）：
 * 手持样本采集器右键生物时执行采集，成功后取消默认交互（避免触发喂食等）。
 * 采集逻辑本体在 common（ChishiSampleCollectorItem），本类仅做事件桥接。
 */
public final class ChishiLifeInteraction {

    public static final ChishiLifeInteraction INSTANCE = new ChishiLifeInteraction();

    private ChishiLifeInteraction() {
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
        if (!(hand.getItem() instanceof ChishiSampleCollectorItem)) {
            return;
        }
        if (ChishiSampleCollectorItem.tryCollect(player, target, hand)) {
            // 采集成功：取消默认交互（如给动物喂食/与村民交易）
            event.setCanceled(true);
        }
    }
}
