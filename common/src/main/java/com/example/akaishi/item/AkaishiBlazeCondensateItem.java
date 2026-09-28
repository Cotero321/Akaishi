package com.example.akaishi.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 烈焰花瓣（烈焰花产物）：右键使用消耗 1 个，扑灭自身火焰并给一小段抗火。
 * <p>烈焰系（只长在灵魂沙上的烈焰花）在此转成"火种"：满开的花冠把烈焰收敛成可控的护火，
 * <b>不点燃任何东西、不造成伤害</b>（与幽匿果 / 末影果同为"安全向"果实）。
 */
public class AkaishiBlazeCondensateItem extends Item {

    /** 抗火时长（tick）：20 秒 */
    private static final int FIRE_RESISTANCE_TICKS = 20 * 20;

    public AkaishiBlazeCondensateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            player.setRemainingFireTicks(0); // 立刻灭火
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_RESISTANCE_TICKS, 0));
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
            server.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.02D);
            server.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.4D, 0.6D, 0.4D, 0.02D);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.akaishi.akaishi_blaze_condensate.desc"));
    }
}
