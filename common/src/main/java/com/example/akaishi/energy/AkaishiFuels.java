package com.example.akaishi.energy;

import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.item.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * 赤能源燃料表：赤石晶 / 粗制赤石块 / 赤石精华 / 浓缩赤石精华 / 浓缩赤石精华块。
 * 供发生机与多方块结构共享，能量档位逐级提升，燃烧时长随能量而定。
 */
public final class AkaishiFuels {

    /** 赤石晶 */
    public static final int FUEL_CRYSTAL = 2000;
    /** 粗制赤石块 */
    public static final int FUEL_RAW_BLOCK = 20000;
    /** 赤石精华 */
    public static final int FUEL_ESSENCE = 80000;
    /** 浓缩赤石精华 */
    public static final int FUEL_COMPRESSED = 800000;
    /** 浓缩赤石精华块 */
    public static final int FUEL_ESSENCE_BLOCK = 7200000;

    private AkaishiFuels() {
    }

    /** 返回物品的赤能源燃料值，非燃料返回 0 */
    public static int getFuelEnergy(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(ModItems.akaishiCrystal.get())) {
            return FUEL_CRYSTAL;
        }
        if (stack.is(ModBlocks.RAW_CHISHI_BLOCK.get().asItem())) {
            return FUEL_RAW_BLOCK;
        }
        if (stack.is(ModItems.akaishiEssence.get())) {
            return FUEL_ESSENCE;
        }
        if (stack.is(ModItems.akaishiEssenceCompressed.get())) {
            return FUEL_COMPRESSED;
        }
        if (stack.is(ModBlocks.CHISHI_ESSENCE_BLOCK.get().asItem())) {
            return FUEL_ESSENCE_BLOCK;
        }
        return 0;
    }
}
