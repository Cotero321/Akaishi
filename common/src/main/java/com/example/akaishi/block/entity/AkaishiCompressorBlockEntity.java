package com.example.akaishi.block.entity;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.item.ModItems;
import com.example.akaishi.menu.AkaishiCompressorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * 赤石压缩机方块实体：粉末压缩为对应块、赤石粉压缩为赤石精华。
 * 压缩消耗多个输入（inputCount），进度满一次性扣除并产出 1 个块；
 * 速度升级加快压缩、能量升级扩容。
 */
public class AkaishiCompressorBlockEntity extends AkaishiSingleSlotMachineBlockEntity {

    /** 压缩配方：粉末/精华 → 对应块（9 粉压 1 块；黑曜石 4 粉压 1 块） */
    public static final Map<Item, MachineRecipe> RECIPES = Map.ofEntries(
            Map.entry(ModItems.coalDust.get(), new MachineRecipe(ModItems.coalDust.get(), 9, Items.COAL_BLOCK, 1)),
            Map.entry(ModItems.ironDust.get(), new MachineRecipe(ModItems.ironDust.get(), 9, Items.IRON_BLOCK, 1)),
            Map.entry(ModItems.copperDust.get(), new MachineRecipe(ModItems.copperDust.get(), 9, Items.COPPER_BLOCK, 1)),
            Map.entry(ModItems.goldDust.get(), new MachineRecipe(ModItems.goldDust.get(), 9, Items.GOLD_BLOCK, 1)),
            Map.entry(ModItems.lapisDust.get(), new MachineRecipe(ModItems.lapisDust.get(), 9, Items.LAPIS_BLOCK, 1)),
            Map.entry(ModItems.diamondDust.get(), new MachineRecipe(ModItems.diamondDust.get(), 9, Items.DIAMOND_BLOCK, 1)),
            Map.entry(ModItems.emeraldDust.get(), new MachineRecipe(ModItems.emeraldDust.get(), 9, Items.EMERALD_BLOCK, 1)),
            Map.entry(ModItems.quartzDust.get(), new MachineRecipe(ModItems.quartzDust.get(), 9, Items.QUARTZ_BLOCK, 1)),
            Map.entry(ModItems.netheriteDust.get(), new MachineRecipe(ModItems.netheriteDust.get(), 9, Items.NETHERITE_INGOT, 1)),
            Map.entry(ModItems.obsidianDust.get(), new MachineRecipe(ModItems.obsidianDust.get(), 4, Items.OBSIDIAN, 1)),
            Map.entry(ModItems.akaishiDust.get(), new MachineRecipe(ModItems.akaishiDust.get(), 9, ModItems.akaishiEssence.get(), 1))
    );

    public AkaishiCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_COMPRESSOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiCompressorBlockEntity be) {
        be.tickServer();
    }

    @Override
    protected Map<Item, MachineRecipe> recipes() {
        return RECIPES;
    }

    @Override
    protected long baseCapacity() {
        return ModConfig.compressorEnergyCapacity;
    }

    @Override
    protected int ticks() {
        return ModConfig.compressorTicks;
    }

    @Override
    protected long energyPerTick() {
        return ModConfig.compressorCostPerTick;
    }

    @Override
    protected AbstractContainerMenu createMenuInstance(int id, Inventory inv) {
        return new AkaishiCompressorMenu(id, inv, this);
    }

    @Override
    protected String nameKey() {
        return "akaishi.akaishi_compressor";
    }
}
