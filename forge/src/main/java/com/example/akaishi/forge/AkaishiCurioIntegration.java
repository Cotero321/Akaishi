package com.example.akaishi.forge;

import com.example.akaishi.item.curio.AkaishiCurioItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Consumer;

/**
 * Forge 平台 Curios 事件接线：
 * - 饰品物品本身实现 ICurioItem，Curios 自动识别（装备/每 tick 由 Curios 驱动），无需手工注册 capability
 * - 本类仅处理三类跨能力事件（击杀/挖掘/受伤），转发到饰品效果钩子
 */
public final class AkaishiCurioIntegration {

    public static final AkaishiCurioIntegration INSTANCE = new AkaishiCurioIntegration();

    private AkaishiCurioIntegration() {
    }

    /** 击杀生物 → 饰品 onKill 回调（狩猎指环掉赤石晶） */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            forEachCurio(player, stack -> {
                if (stack.getItem() instanceof AkaishiCurioItem curio) {
                    curio.onKill(player, stack, event.getEntity());
                }
            });
        }
    }

    /** 挖掘方块 → 饰品 onBlockBreak 回调（采集手环掉赤石晶） */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        forEachCurio(player, stack -> {
            if (stack.getItem() instanceof AkaishiCurioItem curio) {
                curio.onBlockBreak(player, stack, event.getPos(), event.getState());
            }
        });
    }

    /** 玩家受伤 → 饰品 onHurt 回调（防爆护符抵消爆炸伤害） */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player) {
            float[] amount = {event.getAmount()};
            forEachCurio(player, stack -> {
                if (stack.getItem() instanceof AkaishiCurioItem curio) {
                    amount[0] = curio.onHurt(player, stack, amount[0], event.getSource());
                }
            });
            event.setAmount(amount[0]);
        }
    }

    /** 遍历玩家所有已装备的 Curios 饰品，回调每个非空物品 */
    private static void forEachCurio(Player player, Consumer<ItemStack> action) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            var curios = handler.getEquippedCurios(); // IItemHandlerModifiable，非 Iterable，需索引遍历
            for (int i = 0; i < curios.getSlots(); i++) {
                ItemStack stack = curios.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    action.accept(stack);
                }
            }
        });
    }
}
