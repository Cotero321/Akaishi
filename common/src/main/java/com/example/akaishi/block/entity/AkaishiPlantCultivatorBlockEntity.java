package com.example.akaishi.block.entity;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.menu.AkaishiPlantCultivatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * 赤石植物培养机方块实体：消耗赤能源培养植物。
 * 种子/茎秆放入输入槽后保留不消耗（{@code consumesInput=false}），持续消耗能量
 * 产出成熟作物（产物不含种子）；速度升级加快培养、能量升级扩容。
 */
public class AkaishiPlantCultivatorBlockEntity extends AkaishiSingleSlotMachineBlockEntity {

    /** 培养配方：作物种子/茎秆 → 成熟作物（输入保留，仅输出产物） */
    public static final Map<Item, MachineRecipe> RECIPES = Map.ofEntries(
            Map.entry(Items.WHEAT_SEEDS, new MachineRecipe(Items.WHEAT_SEEDS, 1, Items.WHEAT, 1)),
            Map.entry(Items.CARROT, new MachineRecipe(Items.CARROT, 1, Items.CARROT, 1)),
            Map.entry(Items.POTATO, new MachineRecipe(Items.POTATO, 1, Items.POTATO, 1)),
            Map.entry(Items.BEETROOT_SEEDS, new MachineRecipe(Items.BEETROOT_SEEDS, 1, Items.BEETROOT, 1)),
            Map.entry(Items.MELON_SEEDS, new MachineRecipe(Items.MELON_SEEDS, 1, Items.MELON, 1)),
            Map.entry(Items.PUMPKIN_SEEDS, new MachineRecipe(Items.PUMPKIN_SEEDS, 1, Items.PUMPKIN, 1)),
            Map.entry(Items.SUGAR_CANE, new MachineRecipe(Items.SUGAR_CANE, 1, Items.SUGAR_CANE, 1)),
            Map.entry(Items.CACTUS, new MachineRecipe(Items.CACTUS, 1, Items.CACTUS, 1)),
            Map.entry(Items.BAMBOO, new MachineRecipe(Items.BAMBOO, 1, Items.BAMBOO, 1)),
            Map.entry(Items.COCOA_BEANS, new MachineRecipe(Items.COCOA_BEANS, 1, Items.COCOA_BEANS, 1)),
            Map.entry(Items.NETHER_WART, new MachineRecipe(Items.NETHER_WART, 1, Items.NETHER_WART, 1)),
            Map.entry(Items.CRIMSON_FUNGUS, new MachineRecipe(Items.CRIMSON_FUNGUS, 1, Items.CRIMSON_FUNGUS, 1)),
            Map.entry(Items.WARPED_FUNGUS, new MachineRecipe(Items.WARPED_FUNGUS, 1, Items.WARPED_FUNGUS, 1)),
            Map.entry(Items.BROWN_MUSHROOM, new MachineRecipe(Items.BROWN_MUSHROOM, 1, Items.BROWN_MUSHROOM, 1)),
            Map.entry(Items.RED_MUSHROOM, new MachineRecipe(Items.RED_MUSHROOM, 1, Items.RED_MUSHROOM, 1))
    );

    public AkaishiPlantCultivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PLANT_CULTIVATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPlantCultivatorBlockEntity be) {
        be.tickServer();
    }

    @Override
    protected Map<Item, MachineRecipe> recipes() {
        return RECIPES;
    }

    @Override
    protected long baseCapacity() {
        return ModConfig.plantCultivatorEnergyCapacity;
    }

    @Override
    protected int ticks() {
        return ModConfig.plantCultivatorTicks;
    }

    @Override
    protected long energyPerTick() {
        return ModConfig.plantCultivatorCostPerTick;
    }

    /** 种子保留不消耗，可持续培养 */
    @Override
    protected boolean consumesInput() {
        return false;
    }

    @Override
    protected AbstractContainerMenu createMenuInstance(int id, Inventory inv) {
        return new AkaishiPlantCultivatorMenu(id, inv, this);
    }

    @Override
    protected String nameKey() {
        return "akaishi.akaishi_plant_cultivator";
    }
}
