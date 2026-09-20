package com.example.akaishi.block.entity;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.AkaishiPlantCultivatorMenu;
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
 * 赤石植物培养机方块实体：消耗赤能源培养植物。
 * 种子/茎秆放入输入槽后保留不消耗（配方 {@code consume_input=false}），持续消耗能量
 * 产出成熟作物（产物不含种子）；速度升级加快培养、能量升级扩容。
 */
public class AkaishiPlantCultivatorBlockEntity extends AkaishiSingleSlotMachineBlockEntity {

    // 培养配方已改为数据包配方：data/akaishi/recipes/plant_cultivating/*.json（consume_input=false 保留种子）

    public AkaishiPlantCultivatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PLANT_CULTIVATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPlantCultivatorBlockEntity be) {
        be.tickServer();
    }

    @Override
    protected RecipeType<AkaishiItemProcessRecipe> recipeType() {
        return AkaishiRecipeTypes.PLANT_CULTIVATING.get();
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

    @Override
    protected AbstractContainerMenu createMenuInstance(int id, Inventory inv) {
        return new AkaishiPlantCultivatorMenu(id, inv, this);
    }

    @Override
    protected String nameKey() {
        return "akaishi.akaishi_plant_cultivator";
    }

    @Override
    protected RegistrySupplier<SoundEvent> humSound() {
        return ModSounds.PLANT_CULTIVATOR_HUM;
    }
}
