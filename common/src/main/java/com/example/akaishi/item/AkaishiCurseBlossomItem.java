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
 * 咒怨花（咒怨垂蔓产物）：右键使用消耗 1 个，获得缓降并立即清除已累积的摔落伤害。
 * <p>垂蔓取自恶魂一系（吊挂、飘浮），此效果把"飘浮"变成可控的缓降护体；
 * <b>不造成伤害、不施加负面状态</b>（与幽匿果 / 末影果同为"安全向"果实）。
 */
public class AkaishiCurseBlossomItem extends Item {

    /** 缓降时长（tick）：30 秒 */
    private static final int SLOW_FALLING_TICKS = 30 * 20;

    public AkaishiCurseBlossomItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS, 0));
            player.resetFallDistance(); // 立刻清空已累积的掉落伤害
            server.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GHAST_AMBIENT, SoundSource.PLAYERS, 0.6F, 1.4F);
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 18, 0.4D, 0.6D, 0.4D, 0.01D);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.akaishi.akaishi_curse_blossom.desc"));
    }
}
