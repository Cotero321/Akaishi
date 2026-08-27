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

import com.google.common.base.Suppliers;
import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.RegistrySupplier;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.class_156;
import net.minecraft.class_1792;
import net.minecraft.class_1814;
import net.minecraft.class_1920;
import net.minecraft.class_2338;
import net.minecraft.class_2404;
import net.minecraft.class_2960;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3610;
import net.minecraft.class_3611;
import net.minecraft.class_4538;

public class SimpleArchitecturyFluidAttributes implements ArchitecturyFluidAttributes {
    private final Supplier<? extends class_3611> flowingFluid;
    private final Supplier<? extends class_3611> sourceFluid;
    private boolean canConvertToSource = false;
    private int slopeFindDistance = 4;
    private int dropOff = 1;
    private Supplier<? extends Optional<class_1792>> bucketItem = Optional::empty;
    private int tickDelay = 5;
    private float explosionResistance = 100.0F;
    private Supplier<? extends Optional<? extends class_2404>> block = Optional::empty;
    @Nullable
    private class_2960 sourceTexture;
    @Nullable
    private class_2960 flowingTexture;
    @Nullable
    private class_2960 overlayTexture;
    private int color = 0xffffff;
    private int luminosity = 0;
    private int density = 1000;
    private int temperature = 300;
    private int viscosity = 1000;
    private boolean lighterThanAir = false;
    private class_1814 rarity = class_1814.field_8906;
    @Nullable
    private class_3414 fillSound = class_3417.field_15126;
    @Nullable
    private class_3414 emptySound = class_3417.field_14834;
    private final Supplier<String> defaultTranslationKey = Suppliers.memoize(() -> class_156.method_646("fluid", getSourceFluid().arch$registryName()));
    
    public static SimpleArchitecturyFluidAttributes ofSupplier(Supplier<? extends Supplier<? extends class_3611>> flowingFluid, Supplier<? extends Supplier<? extends class_3611>> sourceFluid) {
        return of(() -> flowingFluid.get().get(), () -> sourceFluid.get().get());
    }
    
    
    public static SimpleArchitecturyFluidAttributes of(Supplier<? extends class_3611> flowingFluid, Supplier<? extends class_3611> sourceFluid) {
        return new SimpleArchitecturyFluidAttributes(flowingFluid, sourceFluid);
    }
    
    protected SimpleArchitecturyFluidAttributes(Supplier<? extends class_3611> flowingFluid, Supplier<? extends class_3611> sourceFluid) {
        this.flowingFluid = flowingFluid;
        this.sourceFluid = sourceFluid;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#canConvertToSource()
     */
    public SimpleArchitecturyFluidAttributes convertToSource(boolean canConvertToSource) {
        this.canConvertToSource = canConvertToSource;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getSlopeFindDistance(class_4538)
     */
    public SimpleArchitecturyFluidAttributes slopeFindDistance(int slopeFindDistance) {
        this.slopeFindDistance = slopeFindDistance;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getDropOff(class_4538)
     */
    public SimpleArchitecturyFluidAttributes dropOff(int dropOff) {
        this.dropOff = dropOff;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getBucketItem()
     */
    public SimpleArchitecturyFluidAttributes bucketItemSupplier(Supplier<RegistrySupplier<class_1792>> bucketItem) {
        return bucketItem(() -> bucketItem.get().toOptional());
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getBucketItem()
     */
    public SimpleArchitecturyFluidAttributes bucketItem(RegistrySupplier<class_1792> bucketItem) {
        return bucketItem(bucketItem::toOptional);
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getBucketItem()
     */
    public SimpleArchitecturyFluidAttributes bucketItem(Supplier<? extends Optional<class_1792>> bucketItem) {
        this.bucketItem = Objects.requireNonNull(bucketItem);
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getTickDelay(class_4538)
     */
    public SimpleArchitecturyFluidAttributes tickDelay(int tickDelay) {
        this.tickDelay = tickDelay;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getExplosionResistance()
     */
    public SimpleArchitecturyFluidAttributes explosionResistance(float explosionResistance) {
        this.explosionResistance = explosionResistance;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getBlock()
     */
    public SimpleArchitecturyFluidAttributes blockSupplier(Supplier<RegistrySupplier<? extends class_2404>> block) {
        return block(() -> block.get().toOptional());
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getBlock()
     */
    public SimpleArchitecturyFluidAttributes block(RegistrySupplier<? extends class_2404> block) {
        return block(block::toOptional);
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getBlock()
     */
    public SimpleArchitecturyFluidAttributes block(Supplier<? extends Optional<? extends class_2404>> block) {
        this.block = Objects.requireNonNull(block);
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getSourceTexture(class_3610, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes sourceTexture(class_2960 sourceTexture) {
        this.sourceTexture = sourceTexture;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getFlowingTexture(class_3610, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes flowingTexture(class_2960 flowingTexture) {
        this.flowingTexture = flowingTexture;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getFlowingTexture(class_3610, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes overlayTexture(class_2960 overlayTexture) {
        this.overlayTexture = overlayTexture;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getColor(class_3610, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes color(int color) {
        this.color = color;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getLuminosity(FluidStack, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes luminosity(int luminosity) {
        this.luminosity = luminosity;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getDensity(FluidStack, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes density(int density) {
        this.density = density;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getTemperature(FluidStack, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes temperature(int temperature) {
        this.temperature = temperature;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getViscosity(FluidStack, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes viscosity(int viscosity) {
        this.viscosity = viscosity;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#isLighterThanAir(FluidStack, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes lighterThanAir(boolean lighterThanAir) {
        this.lighterThanAir = lighterThanAir;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getRarity(FluidStack, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes rarity(class_1814 rarity) {
        this.rarity = rarity;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getFillSound(FluidStack, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes fillSound(class_3414 fillSound) {
        this.fillSound = fillSound;
        return this;
    }
    
    /**
     * @see ArchitecturyFluidAttributes#getEmptySound(FluidStack, class_1920, class_2338)
     */
    public SimpleArchitecturyFluidAttributes emptySound(class_3414 emptySound) {
        this.emptySound = emptySound;
        return this;
    }
    
    @Override
    @Nullable
    public String getTranslationKey(@Nullable FluidStack stack) {
        return defaultTranslationKey.get();
    }
    
    @Override
    public final class_3611 getFlowingFluid() {
        return flowingFluid.get();
    }
    
    @Override
    public final class_3611 getSourceFluid() {
        return sourceFluid.get();
    }
    
    @Override
    public boolean canConvertToSource() {
        return canConvertToSource;
    }
    
    @Override
    public int getSlopeFindDistance(@Nullable class_4538 level) {
        return slopeFindDistance;
    }
    
    @Override
    public int getDropOff(@Nullable class_4538 level) {
        return dropOff;
    }
    
    @Override
    @Nullable
    public class_1792 getBucketItem() {
        return bucketItem.get().orElse(null);
    }
    
    @Override
    public int getTickDelay(@Nullable class_4538 level) {
        return tickDelay;
    }
    
    @Override
    public float getExplosionResistance() {
        return explosionResistance;
    }
    
    @Override
    @Nullable
    public class_2404 getBlock() {
        return block.get().orElse(null);
    }
    
    @Override
    public class_2960 getSourceTexture(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return sourceTexture;
    }
    
    @Override
    public class_2960 getFlowingTexture(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return flowingTexture;
    }
    
    @Override
    public class_2960 getOverlayTexture(@Nullable class_3610 state, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return overlayTexture;
    }
    
    @Override
    public int getColor(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return color;
    }
    
    @Override
    public int getLuminosity(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return luminosity;
    }
    
    @Override
    public int getDensity(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return density;
    }
    
    @Override
    public int getTemperature(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return temperature;
    }
    
    @Override
    public int getViscosity(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return viscosity;
    }
    
    @Override
    public boolean isLighterThanAir(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return lighterThanAir;
    }
    
    @Override
    public class_1814 getRarity(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return rarity;
    }
    
    @Override
    @Nullable
    public class_3414 getFillSound(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return fillSound;
    }
    
    @Override
    @Nullable
    public class_3414 getEmptySound(@Nullable FluidStack stack, @Nullable class_1920 level, @Nullable class_2338 pos) {
        return emptySound;
    }
}
