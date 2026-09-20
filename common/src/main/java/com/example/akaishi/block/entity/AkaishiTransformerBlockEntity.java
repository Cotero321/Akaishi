package com.example.akaishi.block.entity;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.AkaishiTransformerMenu;
import com.example.akaishi.sound.ModSounds;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 赤石变化器方块实体：物质 → 基底（青金石粉 → 冷却基底、矿物 → 对应矿石基底）。
 * 每次消耗 1 个输入产出 1 个基底；速度升级加快变化、能量升级扩容。
 */
public class AkaishiTransformerBlockEntity extends AkaishiSingleSlotMachineBlockEntity {

    // 变化配方已改为数据包配方：data/akaishi/recipes/transforming/*.json

    public AkaishiTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_TRANSFORMER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiTransformerBlockEntity be) {
        be.tickServer();
    }

    @Override
    protected RecipeType<AkaishiItemProcessRecipe> recipeType() {
        return AkaishiRecipeTypes.TRANSFORMING.get();
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

    @Override
    protected RegistrySupplier<SoundEvent> humSound() {
        return ModSounds.TRANSFORMER_HUM;
    }
}
