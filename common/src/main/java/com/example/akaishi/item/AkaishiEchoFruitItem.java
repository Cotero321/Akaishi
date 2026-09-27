package com.example.akaishi.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 幽匿果（回响花产物）：右键使用消耗 1 个，以使用者为中心铺开幽匿块并蔓延脉络。
 * <p>规则参考原版黍菌催化剂绽放与黍菌蔓延：半径内 {@code #sculk_replaceable} 方块转化为幽匿块，
 * 裸露面铺幽匿脉络；播放黍菌绽放/蔓延音效与粒子。<b>不施加任何状态效果</b>。
 */
public class AkaishiEchoFruitItem extends Item {

    /** 待调手感值：幽匿蔓延半径（以使用者所在方块为中心的实心球） */
    private static final int SPREAD_RADIUS = 3;
    /** 待调手感值：单次最多改动方块数（上限防炸地卡顿） */
    private static final int MAX_CHANGES = 128;

    public AkaishiEchoFruitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            spreadSculk(server, player.blockPosition());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 半径内可替换方块转化为幽匿块，再给与幽匿块相邻的裸露空气面铺幽匿脉络 */
    private void spreadSculk(ServerLevel level, BlockPos center) {
        int radius = SPREAD_RADIUS;
        int changes = 0;
        List<BlockPos> exposedAir = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (pos.distSqr(center) > (double) radius * radius) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.SCULK_REPLACEABLE)) {
                if (changes >= MAX_CHANGES) {
                    break;
                }
                level.setBlock(pos, Blocks.SCULK.defaultBlockState(), 2);
                changes++;
            } else if (state.isAir()) {
                exposedAir.add(pos.immutable());
            }
        }
        for (BlockPos pos : exposedAir) {
            if (changes >= MAX_CHANGES) {
                break;
            }
            BlockState vein = Blocks.SCULK_VEIN.defaultBlockState();
            boolean attached = false;
            for (Direction dir : Direction.values()) {
                if (level.getBlockState(pos.relative(dir)).is(Blocks.SCULK)) {
                    vein = vein.setValue(MultifaceBlock.getFaceProperty(dir), true);
                    attached = true;
                }
            }
            if (attached && vein.canSurvive(level, pos)) {
                level.setBlock(pos, vein, 2);
                changes++;
            }
        }
        // 音效与粒子：沿用原版黍菌催化剂绽放 / 黍菌蔓延
        level.playSound(null, center, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, center, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0F, 0.9F);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D, 40, 1.5D, 1.0D, 1.5D, 0.0D);
        level.sendParticles(ParticleTypes.SCULK_SOUL,
                center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D, 20, 1.0D, 0.5D, 1.0D, 0.02D);
    }
}
