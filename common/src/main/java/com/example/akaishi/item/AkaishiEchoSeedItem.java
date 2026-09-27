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
 * 回响花种：转基因工厂产物。右键只能种在幽匿块顶面生成「回响花茎」（第 1 格），
 * 茎随机刻在顶端长出回响花顶花，盛开顶花可收幽匿果。
 * 仅能种在幽匿块上（茎 canSurvive 校验），不满足时不消耗种子。
 */
public class AkaishiEchoSeedItem extends Item {

    public AkaishiEchoSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos placePos = context.getClickedPos().above();
        BlockState stem = AkaishiTransgeneBlocks.CHISHI_ECHO_STEM.get().defaultBlockState();
        // 目标上方位空 + 茎可存活（下方必须为幽匿块）才允许种植
        if (!level.isEmptyBlock(placePos) || !stem.canSurvive(level, placePos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(placePos, stem, 2);
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
