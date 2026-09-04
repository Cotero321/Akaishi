package com.example.akaishi.life.potion;

import com.example.akaishi.life.body.BodySlot;
import com.example.akaishi.life.body.IPlayerBodyState;
import com.example.akaishi.life.body.PlayerBodyHelper;
import com.example.akaishi.life.organ.AkaishiOrganItem;
import com.example.akaishi.life.organ.OrganEffectResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * 排异中和剂：消耗品，饮用后减轻全身已移植非原生器官的排斥（排斥可逆的唯一通道）。
 * 规则：每器官每次移植限洗 WASH_LIMIT 次（摘除重植重置额度）；每瓶降所有可洗槽
 * 排斥 WASH_REDUCE（下限 0）；两次饮用间隔 COOLDOWN_TICKS（物品冷却，防连喝秒清）。
 * 全程仅服务端结算，前提不满足（无排异/额度用尽/冷却中）一律不消耗。
 */
public class AkaishiRejectionSerumItem extends Item {

    /** 每瓶每个可洗器官的排斥下降量（下限 0） */
    public static final int WASH_REDUCE = 12;
    /** 每个器官每次移植可被中和剂清洗的次数（移植时在 PlayerBodyState 清零） */
    public static final int WASH_LIMIT = 5;
    /** 饮用冷却（tick = 30 秒） */
    public static final int COOLDOWN_TICKS = 600;

    public AkaishiRejectionSerumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // 客户端直接放行（服务端才结算效果与消耗）
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            player.displayClientMessage(Component.translatable("message.akaishi.serum.cooldown"), true);
            return InteractionResultHolder.fail(stack);
        }
        IPlayerBodyState state = PlayerBodyHelper.of(player);
        if (state == null) {
            return InteractionResultHolder.fail(stack);
        }
        int washed = 0;
        boolean quotaExhausted = false;
        boolean conflictLocked = false;
        // 天敌冲突槽的排斥被锁满（每 tick 强制回锁 100），跳过以免浪费清洗额度与药剂
        Set<BodySlot> conflicts = OrganEffectResolver.findConflicts(state);
        for (BodySlot slot : BodySlot.values()) {
            if (conflicts.contains(slot)) {
                conflictLocked = true;
                continue;
            }
            ItemStack organ = state.getOrgan(slot);
            // 只对已移植的非原生器官生效（原生器官无排斥可言）
            if (organ.isEmpty() || !(organ.getItem() instanceof AkaishiOrganItem) || AkaishiOrganItem.isNative(organ)) {
                continue;
            }
            if (state.getRejection(slot) <= 0) {
                continue;
            }
            int used = AkaishiOrganItem.getWashUsed(organ);
            if (used >= WASH_LIMIT) {
                // 该器官本次移植的清洗额度已用完（摘除重植才恢复）
                quotaExhausted = true;
                continue;
            }
            state.addRejection(slot, -WASH_REDUCE);
            AkaishiOrganItem.setWashUsed(organ, used + 1);
            washed++;
        }
        if (washed == 0) {
            // 无排异 / 额度尽 / 存在冲突锁槽：提示原因且不消耗
            String key = conflictLocked ? "message.akaishi.serum.conflict"
                    : quotaExhausted ? "message.akaishi.serum.full_quota" : "message.akaishi.serum.no_effect";
            player.displayClientMessage(Component.translatable(key), true);
            return InteractionResultHolder.fail(stack);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        stack.shrink(1);
        player.displayClientMessage(Component.translatable("message.akaishi.serum.wash", washed), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.akaishi.serum.hint"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
