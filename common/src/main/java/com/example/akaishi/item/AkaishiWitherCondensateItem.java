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
 * 凋零果（凋零藤产物）：右键使用消耗 1 个，净化自身的凋零与中毒，并给一小段抗性提升。
 * <p>与「凋零玫瑰会施加凋零」形成反向对照：凋零藤把致命的凋零之力转成了可用的解药；
 * <b>不造成伤害、不施加负面状态</b>（与幽匿果 / 末影果同为"安全向"果实）。
 */
public class AkaishiWitherCondensateItem extends Item {

    /** 抗性提升时长（tick）：8 秒 */
    private static final int RESISTANCE_TICKS = 8 * 20;

    public AkaishiWitherCondensateItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            // 净化：凋零与中毒都是"侵蚀生命"的效果，与凋零系意象一致
            player.removeEffect(MobEffects.WITHER);
            player.removeEffect(MobEffects.POISON);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_TICKS, 0));
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 1.0F);
            server.sendParticles(ParticleTypes.SOUL,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 24, 0.4D, 0.6D, 0.4D, 0.02D);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.akaishi.akaishi_wither_condensate.desc"));
    }
}
