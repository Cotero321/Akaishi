package com.example.akaishi.item.curio;

import com.example.akaishi.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 赤石采集手环（hands 槽）：玩家挖掘方块时有 5% 概率掉落 1 个赤石晶（以实体形式落在方块位置）。
 * 与狩猎指环共享"赤石晶产出"主题，但来源不同（采集 vs 击杀），概率也更低。
 */
public class AkaishiGatheringBracelet extends AkaishiCurioItem {

    /** 容量：10 万赤能源（备用，当前效果不耗能） */
    private static final long CAPACITY = 100_000;
    /** 掉落概率（百分比） */
    private static final int DROP_CHANCE_PERCENT = 5;

    public AkaishiGatheringBracelet(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public String[] curioSlots() {
        return new String[]{"hands"};
    }

    @Override
    protected String tooltipKey() {
        return "item.akaishi.curio.gathering";
    }

    @Override
    public void onBlockBreak(Player player, ItemStack stack, BlockPos pos, BlockState state) {
        Level level = player.level();
        if (!level.isClientSide && level.getRandom().nextInt(100) < DROP_CHANCE_PERCENT) {
            Block.popResource(level, pos, new ItemStack(ModItems.akaishiCrystal.get()));
        }
    }
}
