package com.example.akaishi.block.entity;

import com.example.akaishi.block.AkaishiOreDef;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiPulverizerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * 赤石打粉机方块实体：将矿物/赤石/黑曜石打成粉末。
 * 每次消耗 1 个输入产出对应粉末（红石矿石产出原版红石粉）；
 * 速度升级加快打粉、能量升级扩容。
 */
public class AkaishiPulverizerBlockEntity extends AkaishiSingleSlotMachineBlockEntity {

    /** 打粉配方：矿物 → 粉末（inputCount=1，outputCount=产量） */
    public static final Map<Item, MachineRecipe> RECIPES = buildRecipes();

    private static Map<Item, MachineRecipe> buildRecipes() {
        Map<Item, MachineRecipe> map = new HashMap<>();
        map.put(Items.COAL_ORE, new MachineRecipe(Items.COAL_ORE, 1, ModItems.coalDust.get(), 2));
        map.put(Items.DEEPSLATE_COAL_ORE, new MachineRecipe(Items.DEEPSLATE_COAL_ORE, 1, ModItems.coalDust.get(), 2));
        map.put(Items.IRON_ORE, new MachineRecipe(Items.IRON_ORE, 1, ModItems.ironDust.get(), 2));
        map.put(Items.DEEPSLATE_IRON_ORE, new MachineRecipe(Items.DEEPSLATE_IRON_ORE, 1, ModItems.ironDust.get(), 2));
        map.put(Items.COPPER_ORE, new MachineRecipe(Items.COPPER_ORE, 1, ModItems.copperDust.get(), 3));
        map.put(Items.DEEPSLATE_COPPER_ORE, new MachineRecipe(Items.DEEPSLATE_COPPER_ORE, 1, ModItems.copperDust.get(), 3));
        map.put(Items.GOLD_ORE, new MachineRecipe(Items.GOLD_ORE, 1, ModItems.goldDust.get(), 2));
        map.put(Items.DEEPSLATE_GOLD_ORE, new MachineRecipe(Items.DEEPSLATE_GOLD_ORE, 1, ModItems.goldDust.get(), 2));
        map.put(Items.NETHER_GOLD_ORE, new MachineRecipe(Items.NETHER_GOLD_ORE, 1, ModItems.goldDust.get(), 2));
        map.put(Items.REDSTONE_ORE, new MachineRecipe(Items.REDSTONE_ORE, 1, Items.REDSTONE, 4));
        map.put(Items.DEEPSLATE_REDSTONE_ORE, new MachineRecipe(Items.DEEPSLATE_REDSTONE_ORE, 1, Items.REDSTONE, 4));
        map.put(Items.LAPIS_ORE, new MachineRecipe(Items.LAPIS_ORE, 1, ModItems.lapisDust.get(), 4));
        map.put(Items.DEEPSLATE_LAPIS_ORE, new MachineRecipe(Items.DEEPSLATE_LAPIS_ORE, 1, ModItems.lapisDust.get(), 4));
        map.put(Items.DIAMOND_ORE, new MachineRecipe(Items.DIAMOND_ORE, 1, ModItems.diamondDust.get(), 2));
        map.put(Items.DEEPSLATE_DIAMOND_ORE, new MachineRecipe(Items.DEEPSLATE_DIAMOND_ORE, 1, ModItems.diamondDust.get(), 2));
        map.put(Items.EMERALD_ORE, new MachineRecipe(Items.EMERALD_ORE, 1, ModItems.emeraldDust.get(), 2));
        map.put(Items.DEEPSLATE_EMERALD_ORE, new MachineRecipe(Items.DEEPSLATE_EMERALD_ORE, 1, ModItems.emeraldDust.get(), 2));
        map.put(Items.NETHER_QUARTZ_ORE, new MachineRecipe(Items.NETHER_QUARTZ_ORE, 1, ModItems.quartzDust.get(), 2));
        map.put(Items.ANCIENT_DEBRIS, new MachineRecipe(Items.ANCIENT_DEBRIS, 1, ModItems.netheriteDust.get(), 1));
        map.put(Items.OBSIDIAN, new MachineRecipe(Items.OBSIDIAN, 1, ModItems.obsidianDust.get(), 1));
        // 16 个赤石矿石 → 赤石粉（浓度/环境无关，统一 2 粉）
        for (AkaishiOreDef def : ModBlocks.ALL_ORES) {
            Item ore = ModBlocks.get(def).asItem();
            map.put(ore, new MachineRecipe(ore, 1, ModItems.akaishiDust.get(), 2));
        }
        return Map.copyOf(map);
    }

    public AkaishiPulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PULVERIZER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPulverizerBlockEntity be) {
        be.tickServer();
    }

    @Override
    protected Map<Item, MachineRecipe> recipes() {
        return RECIPES;
    }

    @Override
    protected long baseCapacity() {
        return ModConfig.pulverizerEnergyCapacity;
    }

    @Override
    protected int ticks() {
        return ModConfig.pulverizerTicks;
    }

    @Override
    protected long energyPerTick() {
        return ModConfig.pulverizerCostPerTick;
    }

    @Override
    protected AbstractContainerMenu createMenuInstance(int id, Inventory inv) {
        return new AkaishiPulverizerMenu(id, inv, this);
    }

    @Override
    protected String nameKey() {
        return "akaishi.akaishi_pulverizer";
    }
}
