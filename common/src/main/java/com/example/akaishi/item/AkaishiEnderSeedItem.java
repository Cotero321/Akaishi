package com.example.akaishi.item;

import com.example.akaishi.block.AkaishiTransgeneBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 末影花种：转基因工厂产物。右键只能种在末地石顶面生成「末影花丛」（单块），
 * 丛随机刻成熟后可收末影果。
 * 仅能种在末地石上（丛 canSurvive 校验），不满足时不消耗种子。
 */
public class AkaishiEnderSeedItem extends Item {

    public AkaishiEnderSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos placePos = context.getClickedPos().above();
        BlockState bush = AkaishiTransgeneBlocks.CHISHI_ENDER_BUSH.get().defaultBlockState();
        // 目标上方位空 + 丛可存活（下方必须为末地石）才允许种植
        if (!level.isEmptyBlock(placePos) || !bush.canSurvive(level, placePos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(placePos, bush, 2);
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
