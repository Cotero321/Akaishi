package com.example.akaishi.item;

import com.example.akaishi.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 凋零藤种子：转基因工厂产物。右键任意实体方块顶面种下第 1 格（凋零藤根），
 * 之后由根→茎的生长阶段推进；根被挖掘时掉落本种子（1:1 返还，杜绝刷种子）。
 */
public class AkaishiWitherSeedItem extends Item {

    public AkaishiWitherSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos placePos = context.getClickedPos().above();
        BlockState root = ModBlocks.CHISHI_WITHER_ROOT.get().defaultBlockState();
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
