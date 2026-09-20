package com.example.akaishi.block.entity;

import com.example.akaishi.config.ModConfig;
import com.example.akaishi.craft.recipe.AkaishiItemProcessRecipe;
import com.example.akaishi.craft.recipe.AkaishiRecipeTypes;
import com.example.akaishi.menu.AkaishiPulverizerMenu;
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
 * 赤石打粉机方块实体：将矿物/赤石/黑曜石打成粉末。
 * 每次消耗 1 个输入产出对应粉末（红石矿石产出原版红石粉）；
 * 速度升级加快打粉、能量升级扩容。
 */
public class AkaishiPulverizerBlockEntity extends AkaishiSingleSlotMachineBlockEntity {

    // 打粉配方已改为数据包配方：data/akaishi/recipes/pulverizing/*.json

    public AkaishiPulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHISHI_PULVERIZER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AkaishiPulverizerBlockEntity be) {
        be.tickServer();
    }

    @Override
    protected RecipeType<AkaishiItemProcessRecipe> recipeType() {
        return AkaishiRecipeTypes.PULVERIZING.get();
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

    @Override
    protected RegistrySupplier<SoundEvent> humSound() {
        return ModSounds.PULVERIZER_HUM;
    }
}
