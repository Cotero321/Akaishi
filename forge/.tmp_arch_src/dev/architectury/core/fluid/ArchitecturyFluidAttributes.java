/*
 * This file is part of architectury.
 * Copyright (C) 2020, 2021, 2022 architectury
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package dev.architectury.core.fluid;

import dev.architectury.fluid.FluidStack;
import net.minecraft.class_1792;
import net.minecraft.class_1814;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_2404;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_3414;
import net.minecraft.class_3610;
import net.minecraft.class_3611;
import net.minecraft.class_4538;
import org.jetbrains.annotations.Nullable;

/**
 * Attributes of a fluid.
 *
 * @see SimpleArchitecturyFluidAttributes
 */
public interface ArchitecturyFluidAttributes {
    /**
     * Returns the translation key of the name of this fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the translation key
     */
    @Nullable
    String getTranslationKey(@Nullable FluidStack stack);
    
    /**
     * Returns the translation key of the name of this fluid.
     *
     * @return the translation key
     */
    @Nullable
    default String getTranslationKey() {
        return getTranslationKey(null);
    }
    
    /**
     * Returns the name of this fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the name
     */
    default class_2561 getName(@Nullable FluidStack stack) {
        return class_2561.method_43471(getTranslationKey(stack));
    }
    
    /**
     * Returns the name of this fluid.
     *
     * @return the name
     */
    default class_2561 getName() {
        return getName(null);
    }
    
    /**
     * Returns the flowing fluid.
     *
     * @return the flowing fluid
     */
    class_3611 getFlowingFluid();
    
    /**
     * Returns the still fluid.
     *
     * @return the still fluid
     */
    class_3611 getSourceFluid();
    
    /**
     * Returns whether this fluid can be converted to a source block when a flowing fluid is adjacent to two source blocks.
     * A fluid that can be converted to a source block means that the fluid can be multiplied infinitely.
     *
     * @return whether this fluid can be converted to a source block
     */
    boolean canConvertToSource();
    
    /**
     * Returns the maximum distance this fluid will consider as a flowable hole candidate.
     *
     * @param level the level, can be {@code null}
     * @return the maximum distance
     * @see net.minecraft.class_3621#method_15733(class_4538)
     * @see net.minecraft.class_3616#method_15733(class_4538)
     */
    int getSlopeFindDistance(@Nullable class_4538 level);
    
    /**
     * Returns the maximum distance this fluid will consider as a flowable hole candidate.
     *
     * @return the maximum distance
     * @see net.minecraft.class_3621#method_15733(class_4538)
     * @see net.minecraft.class_3616#method_15733(class_4538)
     */
    default int getSlopeFindDistance() {
        return getSlopeFindDistance(null);
    }
    
    /**
     * Returns the drop in fluid level per block travelled.
     *
     * @param level the level, can be {@code null}
     * @return the drop in fluid level per block travelled
     * @see net.minecraft.class_3621#method_15739(class_4538)
     * @see net.minecraft.class_3616#method_15739(class_4538)
     */
    int getDropOff(@Nullable class_4538 level);
    
    /**
     * Returns the drop in fluid level per block travelled.
     *
     * @return the drop in fluid level per block travelled
     * @see net.minecraft.class_3621#method_15739(class_4538)
     * @see net.minecraft.class_3616#method_15739(class_4538)
     */
    default int getDropOff() {
        return getDropOff(null);
    }
    
    /**
     * Returns the filled bucket item for this fluid.
     *
     * @return the filled bucket item
     */
    @Nullable
    class_1792 getBucketItem();
    
    /**
     * Returns the tick delay between each flow update.
     *
     * @param level the level, can be {@code null}
     * @return the tick delay
     * @see net.minecraft.class_3621#method_15789(class_4538)
     * @see net.minecraft.class_3616#method_15789(class_4538)
     */
    int getTickDelay(@Nullable class_4538 level);
    
    /**
     * Returns the tick delay between each flow update.
     *
     * @return the tick delay
     * @see net.minecraft.class_3621#method_15789(class_4538)
     * @see net.minecraft.class_3616#method_15789(class_4538)
     */
    default int getTickDelay() {
        return getTickDelay(null);
    }
    
    /**
     * Returns the explosion resistance of this fluid.
     *
     * @return the explosion resistance
     * @see net.minecraft.class_3621#method_15784()
     * @see net.minecraft.class_3616#method_15784()
     */
    float getExplosionResistance();
    
    /**
     * Returns the block form of this fluid.
     *
     * @return the block form
     * @see net.minecraft.class_2246#field_10382
     * @see net.minecraft.class_2246#field_10164
     */
    @Nullable
    class_2404 getBlock();
    
    /**
     * Returns the texture location of this fluid in its source form.
     * <p>
     * The vanilla water location is {@code "block/water_still"}.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the texture location
     * @deprecated Please use and override {@link #getSourceTexture(class_3610, class_1920, class_2338)}
     * or {@link #getSourceTexture(FluidStack)} instead, this method will be removed in a future version.
     */
    @Deprecated(forRemoval = true)
    class_2960 getSourceTexture(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the texture location of this fluid in its source form.
     * <p>
     * The vanilla water location is {@code "block/water_still"}.
     *
     * @param state the fluid state, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the texture location
     */
    default class_2960 getSourceTexture(@Nullable class_3610 state, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return getSourceTexture(state == null ? null : FluidStack.create(state.method_15772(), FluidStack.bucketAmount()), level, pos);
    }
    
    /**
     * Returns the texture location of this fluid in its source form.
     * <p>
     * The vanilla water location is {@code "block/water_still"}.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the texture location
     */
    default class_2960 getSourceTexture(@Nullable FluidStack stack) {
        return getSourceTexture(stack, null, null);
    }
    
    /**
     * Returns the texture location of this fluid in its source form.
     * <p>
     * The vanilla water location is {@code "block/water_still"}.
     *
     * @return the texture location
     */
    default class_2960 getSourceTexture() {
        return getSourceTexture(null);
    }
    
    /**
     * Returns the texture location of this fluid in its flowing form.
     * <p>
     * The vanilla water location is {@code "block/water_flow"}.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the texture location
     * @deprecated Please use and override {@link #getFlowingTexture(class_3610, class_1920, class_2338)}
     * or {@link #getFlowingTexture(FluidStack)} instead, this method will be removed in a future version.
     */
    @Deprecated(forRemoval = true)
    class_2960 getFlowingTexture(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the texture location of this fluid in its flowing form.
     * <p>
     * The vanilla water location is {@code "block/water_flow"}.
     *
     * @param state the fluid state, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the texture location
     */
    default class_2960 getFlowingTexture(@Nullable class_3610 state, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return getFlowingTexture(state == null ? null : FluidStack.create(state.method_15772(), FluidStack.bucketAmount()), level, pos);
    }
    
    /**
     * Returns the texture location of this fluid in its flowing form.
     * <p>
     * The vanilla water location is {@code "block/water_flow"}.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the texture location
     */
    default class_2960 getFlowingTexture(@Nullable FluidStack stack) {
        return getFlowingTexture(stack, null, null);
    }
    
    /**
     * Returns the texture location of this fluid in its flowing form.
     * <p>
     * The vanilla water location is {@code "block/water_flow"}.
     *
     * @return the texture location
     */
    default class_2960 getFlowingTexture() {
        return getFlowingTexture(null);
    }
    
    /**
     * Returns the overlay texture location of this fluid behind transparent blocks.
     * <p>
     * The vanilla water location is {@code "block/water_overlay"}.
     *
     * @param state the fluid state, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the texture location, can be {@code null}
     */
    @Nullable
    default class_2960 getOverlayTexture(@Nullable class_3610 state, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return null;
    }
    
    /**
     * Returns the overlay texture location of this fluid behind transparent blocks.
     * <p>
     * The vanilla water location is {@code "block/water_overlay"}.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the texture location, can be {@code null}
     */
    @Nullable
    default class_2960 getOverlayTexture(@Nullable FluidStack stack) {
        return null;
    }
    
    /**
     * Returns the overlay texture location of this fluid behind transparent blocks.
     * <p>
     * The vanilla water location is {@code "block/water_overlay"}.
     *
     * @return the texture location, can be {@code null}
     */
    @Nullable
    default class_2960 getOverlayTexture() {
        return getOverlayTexture(null);
    }
    
    /**
     * Returns the color of the fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the color
     * @deprecated Please use and override {@link #getColor(class_3610, class_1920, class_2338)}
     * or {@link #getColor(FluidStack)} instead, this method will be removed in a future version.
     */
    @Deprecated(forRemoval = true)
    int getColor(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the color of the fluid.
     *
     * @param state the fluid state, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the color
     */
    default int getColor(@Nullable class_3610 state, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return getColor(state == null ? null : FluidStack.create(state.method_15772(), FluidStack.bucketAmount()), level, pos);
    }
    
    /**
     * Returns the color of the fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the color
     */
    default int getColor(@Nullable FluidStack stack) {
        return getColor(stack, null, null);
    }
    
    /**
     * Returns the color of the fluid.
     *
     * @return the color
     */
    default int getColor() {
        return getColor(null);
    }
    
    /**
     * Returns the luminosity of the fluid, this is between 0 and 15.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the luminosity
     */
    int getLuminosity(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the luminosity of the fluid, this is between 0 and 15.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the luminosity
     */
    default int getLuminosity(@Nullable FluidStack stack) {
        return getLuminosity(stack, null, null);
    }
    
    /**
     * Returns the luminosity of the fluid, this is between 0 and 15.
     *
     * @return the luminosity
     */
    default int getLuminosity() {
        return getLuminosity(null);
    }
    
    /**
     * Returns the density of the fluid, this is 1000 for water and 3000 for lava on forge.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the density
     */
    int getDensity(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the density of the fluid, this is 1000 for water and 3000 for lava on forge.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the density
     */
    default int getDensity(@Nullable FluidStack stack) {
        return getDensity(stack, null, null);
    }
    
    /**
     * Returns the density of the fluid, this is 1000 for water and 3000 for lava on forge.
     *
     * @return the density
     */
    default int getDensity() {
        return getDensity(null);
    }
    
    /**
     * Returns the temperature of the fluid.
     * The temperature is in kelvin, for example, 300 kelvin is equal to room temperature.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the temperature
     */
    int getTemperature(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the temperature of the fluid.
     * The temperature is in kelvin, for example, 300 kelvin is equal to room temperature.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the temperature
     */
    default int getTemperature(@Nullable FluidStack stack) {
        return getTemperature(stack, null, null);
    }
    
    /**
     * Returns the temperature of the fluid.
     * The temperature is in kelvin, for example, 300 kelvin is equal to room temperature.
     *
     * @return the temperature
     */
    default int getTemperature() {
        return getTemperature(null);
    }
    
    /**
     * Returns the viscosity of the fluid. A lower viscosity means that the fluid will flow faster.
     * The default value is 1000 for water.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the viscosity
     */
    int getViscosity(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the viscosity of the fluid. A lower viscosity means that the fluid will flow faster.
     * The default value is 1000 for water.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the viscosity
     */
    default int getViscosity(@Nullable FluidStack stack) {
        return getViscosity(stack, null, null);
    }
    
    /**
     * Returns the viscosity of the fluid. A lower viscosity means that the fluid will flow faster.
     * The default value is 1000 for water.
     *
     * @return the viscosity
     */
    default int getViscosity() {
        return getViscosity(null);
    }
    
    /**
     * Returns whether this fluid is lighter than air. This is used to determine whether the fluid should be rendered
     * upside down like gas.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return {@code true} if the fluid is lighter than air
     */
    boolean isLighterThanAir(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns whether this fluid is lighter than air. This is used to determine whether the fluid should be rendered
     * upside down like gas.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return {@code true} if the fluid is lighter than air
     */
    default boolean isLighterThanAir(@Nullable FluidStack stack) {
        return isLighterThanAir(stack, null, null);
    }
    
    /**
     * Returns whether this fluid is lighter than air. This is used to determine whether the fluid should be rendered
     * upside down like gas.
     *
     * @return {@code true} if the fluid is lighter than air
     */
    default boolean isLighterThanAir() {
        return isLighterThanAir(null);
    }
    
    /**
     * Returns the rarity of the fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the rarity
     */
    class_1814 getRarity(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the rarity of the fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the rarity
     */
    default class_1814 getRarity(@Nullable FluidStack stack) {
        return getRarity(stack, null, null);
    }
    
    /**
     * Returns the rarity of the fluid.
     *
     * @return the rarity
     */
    default class_1814 getRarity() {
        return getRarity(null);
    }
    
    /**
     * Returns the fill sound of the fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the fill sound
     * @see net.minecraft.class_3417#field_15126
     * @see net.minecraft.class_3417#field_15202
     */
    @Nullable
    class_3414 getFillSound(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the fill sound of the fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the fill sound
     * @see net.minecraft.class_3417#field_15126
     * @see net.minecraft.class_3417#field_15202
     */
    @Nullable
    default class_3414 getFillSound(@Nullable FluidStack stack) {
        return getFillSound(stack, null, null);
    }
    
    /**
     * Returns the fill sound of the fluid.
     *
     * @return the fill sound
     * @see net.minecraft.class_3417#field_15126
     * @see net.minecraft.class_3417#field_15202
     */
    @Nullable
    default class_3414 getFillSound() {
        return getFillSound(null);
    }
    
    /**
     * Returns the empty sound of the fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @param level the level, can be {@code null}
     * @param pos   the position, can be {@code null}
     * @return the empty sound
     * @see net.minecraft.class_3417#field_14834
     * @see net.minecraft.class_3417#field_15010
     */
    @Nullable
    class_3414 getEmptySound(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos);
    
    /**
     * Returns the empty sound of the fluid.
     *
     * @param stack the fluid stack, can be {@code null}
     * @return the empty sound
     * @see net.minecraft.class_3417#field_14834
     * @see net.minecraft.class_3417#field_15010
     */
    @Nullable
    default class_3414 getEmptySound(@Nullable FluidStack stack) {
        return getEmptySound(stack, null, null);
    }
    
    /**
     * Returns the empty sound of the fluid.
     *
     * @return the empty sound
     * @see net.minecraft.class_3417#field_14834
     * @see net.minecraft.class_3417#field_15010
     */
    @Nullable
    default class_3414 getEmptySound() {
        return getEmptySound(null);
    }
}
