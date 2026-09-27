package com.example.akaishi.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 末影果（末影花产物）：右键使用消耗 1 个，沿视线寻找落点后把使用者传送过去。
 * 行为对齐原版末影珍珠（落点被占据时向上寻找可容纳玩家的空位兜底），
 * 但<b>不造成任何伤害</b>，并清除掉落伤害；播放原版末影传送音效与粒子。
 */
public class AkaishiEnderFruitItem extends Item {

    /** 待调手感值：最大传送距离（原版末影珍珠量级，约 30 格） */
    private static final double MAX_TELEPORT_DISTANCE = 32.0D;

    public AkaishiEnderFruitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            teleport(server, player);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 沿视线射线求落点，再落位传送（不做任何伤害处理） */
    private static void teleport(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(MAX_TELEPORT_DISTANCE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        BlockPos dest = BlockPos.containing(target.x, target.y, target.z);
        // 兜底：落点被方块占据时逐格上移，找到能容纳玩家（两格高）的空位，避免卡进方块
        while (dest.getY() < level.getMaxBuildHeight() - 1 && !hasRoom(level, dest)) {
            dest = dest.above();
        }
        double x = dest.getX() + 0.5D;
        double y = dest.getY();
        double z = dest.getZ() + 0.5D;
        // 传送前：停下载具、清除掉落伤害
        if (player.isPassenger()) {
            player.stopRiding();
        }
        player.resetFallDistance();
        playTeleportEffects(level, player.getX(), player.getY() + 1.0D, player.getZ());
        player.teleportTo(x, y, z);
        player.resetFallDistance();
        playTeleportEffects(level, x, y + 1.0D, z);
    }

    /** 目的地两格（脚 + 头）是否均无碰撞体 */
    private static boolean hasRoom(Level level, BlockPos pos) {
        BlockPos head = pos.above();
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    /** 原版末影传送音效 + 传送门粒子 */
    private static void playTeleportEffects(ServerLevel level, double x, double y, double z) {
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 32, 0.5D, 0.5D, 0.5D, 0.0D);
    }
}
