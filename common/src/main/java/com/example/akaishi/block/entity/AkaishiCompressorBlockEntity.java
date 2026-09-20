package com.example.akaishi.block.entity;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.AkaishiCompressorMenu;
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
 * 赤石压缩机方块实体：粉末压缩为对应块、赤石粉压缩为赤石精华。
 * 压缩消耗多个输入（inputCount），进度满一次性扣除并产出 1 个块；
 * 速度升级加快压缩、能量升级扩容。
 */
public class AkaishiCompressorBlockEntity extends AkaishiSingleSlotMachineBlockEntity {

    // 压缩配方已改为数据包配方：data/akaishi/recipes/compressing/*.json

    public AkaishiCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_COMPRESSOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiCompressorBlockEntity be) {
        be.tickServer();
    }

    @Override
    protected RecipeType<AkaishiItemProcessRecipe> recipeType() {
        return AkaishiRecipeTypes.COMPRESSING.get();
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

    @Override
    protected RegistrySupplier<SoundEvent> humSound() {
        return ModSounds.COMPRESSOR_HUM;
    }
}
