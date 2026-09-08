package com.example.akaishi.item;

import com.example.akaishi.block.AkaishiTransgeneBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 烈焰花种：转基因工厂产物。右键灵魂沙顶面种下烈焰花株（第 1 格）；
 * 花株随机刻成株后于上方长出烈焰花冠，盛开花冠可收烈焰凝聚物。
 * 仅能种在灵魂沙上（花株 canSurvive 校验），不满足时不消耗种子。
 */
public class AkaishiBlazeSeedItem extends Item {

    public AkaishiBlazeSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos placePos = context.getClickedPos().above();
        BlockState root = AkaishiTransgeneBlocks.CHISHI_BLAZE_FLOWER_ROOT.get().defaultBlockState();
        // 目标上方位空 + 花株可存活（下方必须为灵魂沙）才允许种植
        if (!level.isEmptyBlock(placePos) || !root.canSurvive(level, placePos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlock(placePos, root, 2);
            ItemStack hand = context.getItemInHand();
            hand.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
