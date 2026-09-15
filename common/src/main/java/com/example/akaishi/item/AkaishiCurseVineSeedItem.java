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
 * 咒怨垂蔓种子：转基因工厂产物。右键任意坚固方块底面种下第 1 格（咒怨垂蔓根），
 * 根贴附于方块下方，之后由根→茎向下垂挂生长（最高 5 格）；根被挖掘时掉落本种子（1:1 返还，杜绝刷种子）。
 */
public class AkaishiCurseVineSeedItem extends Item {

    public AkaishiCurseVineSeedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (context.getClickedFace() != Direction.DOWN) {
            return InteractionResult.PASS;
        }
        BlockPos placePos = context.getClickedPos().below();
        BlockState root = ModBlocks.CHISHI_CURSE_VINE_ROOT.get().defaultBlockState();
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
