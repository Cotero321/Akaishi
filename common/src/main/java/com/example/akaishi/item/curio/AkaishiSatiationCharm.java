package com.example.akaishi.item.curio;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 赤石饱食护符（charm 槽）：当玩家饱和度偏低时，消耗赤能源补充饱和度。
 * 每 tick 最多恢复 0.1 饱和度，消耗 100 赤能源；能量不足时自动停止（由基类从便携单元补充）。
 */
public class AkaishiSatiationCharm extends AkaishiCurioItem {

    /** 容量：50 万赤能源 */
    private static final long CAPACITY = 500_000;
    /** 每次补充的饱和度 */
    private static final float SATURATION_STEP = 0.1f;
    /** 每步消耗能量 */
    private static final long COST_PER_STEP = 100;
    /** 饱和度低于该值时开始补充 */
    private static final float SATURATION_THRESHOLD = 5.0f;

    public AkaishiSatiationCharm(Properties properties) {
        super(properties, CAPACITY);
    }

    @Override
    public String[] curioSlots() {
        return new String[]{"charm"};
    }

    @Override
    protected String tooltipKey() {
        return "item.akaishi.curio.satiation";
    }

    @Override
    public void curioTick(Level level, Player player, ItemStack stack) {
        if (player.isCreative() || level.isClientSide) {
            return;
        }
        FoodData food = player.getFoodData();
        if (food.getSaturationLevel() < SATURATION_THRESHOLD
                && tryConsume(player, stack, COST_PER_STEP) >= COST_PER_STEP) {
            food.setSaturation(food.getSaturationLevel() + SATURATION_STEP);
        }
    }
}
