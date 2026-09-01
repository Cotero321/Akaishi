package com.example.akaishi.block.entity;

import com.example.akaishi.block.AkaishiOreDef;
import com.example.akaishi.block.ModBlocks;
import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiTransformerMenu;
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
 * 赤石变化器方块实体：物质 → 基底（青金石粉 → 冷却基底、矿物 → 对应矿石基底）。
 * 每次消耗 1 个输入产出 1 个基底；速度升级加快变化、能量升级扩容。
 */
public class AkaishiTransformerBlockEntity extends AkaishiSingleSlotMachineBlockEntity {

    /** 变化配方：原料 → 基底（inputCount=1，outputCount=1） */
    public static final Map<Item, MachineRecipe> RECIPES = buildRecipes();

    private static Map<Item, MachineRecipe> buildRecipes() {
        Map<Item, MachineRecipe> map = new HashMap<>();
        // 青金石粉 → 冷却基底
        map.put(ModItems.lapisDust.get(), new MachineRecipe(ModItems.lapisDust.get(), 1, ModItems.coolingBase.get(), 1));
        // 矿物 → 对应矿石基底（深层变种与普通矿石产出同种基底）
        map.put(Items.COAL_ORE, new MachineRecipe(Items.COAL_ORE, 1, ModItems.coalOreBase.get(), 1));
        map.put(Items.DEEPSLATE_COAL_ORE, new MachineRecipe(Items.DEEPSLATE_COAL_ORE, 1, ModItems.coalOreBase.get(), 1));
        map.put(Items.IRON_ORE, new MachineRecipe(Items.IRON_ORE, 1, ModItems.ironOreBase.get(), 1));
        map.put(Items.DEEPSLATE_IRON_ORE, new MachineRecipe(Items.DEEPSLATE_IRON_ORE, 1, ModItems.ironOreBase.get(), 1));
        map.put(Items.COPPER_ORE, new MachineRecipe(Items.COPPER_ORE, 1, ModItems.copperOreBase.get(), 1));
        map.put(Items.DEEPSLATE_COPPER_ORE, new MachineRecipe(Items.DEEPSLATE_COPPER_ORE, 1, ModItems.copperOreBase.get(), 1));
        map.put(Items.GOLD_ORE, new MachineRecipe(Items.GOLD_ORE, 1, ModItems.goldOreBase.get(), 1));
        map.put(Items.DEEPSLATE_GOLD_ORE, new MachineRecipe(Items.DEEPSLATE_GOLD_ORE, 1, ModItems.goldOreBase.get(), 1));
        map.put(Items.NETHER_GOLD_ORE, new MachineRecipe(Items.NETHER_GOLD_ORE, 1, ModItems.goldOreBase.get(), 1));
        map.put(Items.REDSTONE_ORE, new MachineRecipe(Items.REDSTONE_ORE, 1, ModItems.redstoneOreBase.get(), 1));
        map.put(Items.DEEPSLATE_REDSTONE_ORE, new MachineRecipe(Items.DEEPSLATE_REDSTONE_ORE, 1, ModItems.redstoneOreBase.get(), 1));
        map.put(Items.LAPIS_ORE, new MachineRecipe(Items.LAPIS_ORE, 1, ModItems.lapisOreBase.get(), 1));
        map.put(Items.DEEPSLATE_LAPIS_ORE, new MachineRecipe(Items.DEEPSLATE_LAPIS_ORE, 1, ModItems.lapisOreBase.get(), 1));
        map.put(Items.DIAMOND_ORE, new MachineRecipe(Items.DIAMOND_ORE, 1, ModItems.diamondOreBase.get(), 1));
        map.put(Items.DEEPSLATE_DIAMOND_ORE, new MachineRecipe(Items.DEEPSLATE_DIAMOND_ORE, 1, ModItems.diamondOreBase.get(), 1));
        map.put(Items.EMERALD_ORE, new MachineRecipe(Items.EMERALD_ORE, 1, ModItems.emeraldOreBase.get(), 1));
        map.put(Items.DEEPSLATE_EMERALD_ORE, new MachineRecipe(Items.DEEPSLATE_EMERALD_ORE, 1, ModItems.emeraldOreBase.get(), 1));
        map.put(Items.NETHER_QUARTZ_ORE, new MachineRecipe(Items.NETHER_QUARTZ_ORE, 1, ModItems.quartzOreBase.get(), 1));
        map.put(Items.ANCIENT_DEBRIS, new MachineRecipe(Items.ANCIENT_DEBRIS, 1, ModItems.netheriteOreBase.get(), 1));
        // 16 个赤石矿石 → 赤石矿石基底
        for (AkaishiOreDef def : ModBlocks.ALL_ORES) {
            Item ore = ModBlocks.get(def).asItem();
            map.put(ore, new MachineRecipe(ore, 1, ModItems.akaishiOreBase.get(), 1));
        }
        return Map.copyOf(map);
    }

    public AkaishiTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_TRANSFORMER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiTransformerBlockEntity be) {
        be.tickServer();
    }

    @Override
    protected Map<Item, MachineRecipe> recipes() {
        return RECIPES;
    }

    @Override
    protected long baseCapacity() {
        return ModConfig.transformerEnergyCapacity;
    }

    @Override
    protected int ticks() {
        return ModConfig.transformerTicks;
    }

    @Override
    protected long energyPerTick() {
        return ModConfig.transformerCostPerTick;
    }

    @Override
    protected AbstractContainerMenu createMenuInstance(int id, Inventory inv) {
        return new AkaishiTransformerMenu(id, inv, this);
    }

    @Override
    protected String nameKey() {
        return "akaishi.akaishi_transformer";
    }
}
