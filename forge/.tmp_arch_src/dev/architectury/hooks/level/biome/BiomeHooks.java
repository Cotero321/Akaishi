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

package dev.architectury.hooks.level.biome;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.class_1299;
import net.minecraft.class_1311;
import net.minecraft.class_1959;
import net.minecraft.class_2893;
import net.minecraft.class_2922;
import net.minecraft.class_3414;
import net.minecraft.class_4761;
import net.minecraft.class_4763;
import net.minecraft.class_4763.class_5486;
import net.minecraft.class_4967;
import net.minecraft.class_4968;
import net.minecraft.class_5195;
import net.minecraft.class_5483;
import net.minecraft.class_5485;
import net.minecraft.class_6796;
import net.minecraft.class_6880;
import net.minecraft.world.level.biome.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class BiomeHooks {
    public static BiomeProperties getBiomeProperties(class_1959 biome) {
        return new BiomeWrapped(biome);
    }
    
    public static class BiomeWrapped implements BiomeProperties {
        protected final class_1959 biome;
        protected final ClimateProperties climateProperties;
        protected final EffectsProperties effectsProperties;
        protected final GenerationProperties generationProperties;
        protected final SpawnProperties spawnProperties;
        
        public BiomeWrapped(class_1959 biome) {
            this(biome,
                    new ClimateWrapped(biome),
                    new EffectsWrapped(biome),
                    new GenerationSettingsWrapped(biome),
                    new SpawnSettingsWrapped(biome));
        }
        
        public BiomeWrapped(class_1959 biome,
                            ClimateProperties climateProperties,
                            EffectsProperties effectsProperties,
                            GenerationProperties generationProperties,
                            SpawnProperties spawnProperties) {
            this.biome = biome;
            this.climateProperties = climateProperties;
            this.effectsProperties = effectsProperties;
            this.generationProperties = generationProperties;
            this.spawnProperties = spawnProperties;
        }
        
        @Override
        public ClimateProperties getClimateProperties() {
            return climateProperties;
        }
        
        @Override
        public EffectsProperties getEffectsProperties() {
            return effectsProperties;
        }
        
        @Override
        public GenerationProperties getGenerationProperties() {
            return generationProperties;
        }
        
        @Override
        public SpawnProperties getSpawnProperties() {
            return spawnProperties;
        }
    }
    
    public static class MutableBiomeWrapped extends BiomeWrapped implements BiomeProperties.Mutable {
        public MutableBiomeWrapped(class_1959 biome,
                                   GenerationProperties.Mutable generationProperties,
                                   SpawnProperties.Mutable spawnProperties) {
            this(biome,
                    new ClimateWrapped(extractClimateSettings(biome)),
                    new EffectsWrapped(biome.method_24377()),
                    generationProperties,
                    spawnProperties);
        }
        
        public MutableBiomeWrapped(class_1959 biome,
                                   ClimateProperties.Mutable climateProperties,
                                   EffectsProperties.Mutable effectsProperties,
                                   GenerationProperties.Mutable generationProperties,
                                   SpawnProperties.Mutable spawnProperties) {
            super(biome,
                    climateProperties,
                    effectsProperties,
                    generationProperties,
                    spawnProperties);
        }
        
        @Override
        public ClimateProperties.Mutable getClimateProperties() {
            return (ClimateProperties.Mutable) super.getClimateProperties();
        }
        
        @Override
        public EffectsProperties.Mutable getEffectsProperties() {
            return (EffectsProperties.Mutable) super.getEffectsProperties();
        }
        
        @Override
        public GenerationProperties.Mutable getGenerationProperties() {
            return (GenerationProperties.Mutable) super.getGenerationProperties();
        }
        
        @Override
        public SpawnProperties.Mutable getSpawnProperties() {
            return (SpawnProperties.Mutable) super.getSpawnProperties();
        }
    }
    
    @ExpectPlatform
    private static class_1959.class_5482 extractClimateSettings(class_1959 biome) {
        return null;
    }
    
    public static class ClimateWrapped implements ClimateProperties.Mutable {
        
        protected final class_1959.class_5482 climateSettings;
        
        public ClimateWrapped(class_1959 biome) {
            this(extractClimateSettings(biome));
        }
        
        public ClimateWrapped(class_1959.class_5482 climateSettings) {
            this.climateSettings = climateSettings;
        }
        
        @Override
        public Mutable setHasPrecipitation(boolean hasPrecipitation) {
            climateSettings.comp_1187 = hasPrecipitation;
            return this;
        }
        
        @Override
        public Mutable setTemperature(float temperature) {
            climateSettings.comp_844 = temperature;
            return this;
        }
        
        @Override
        public Mutable setTemperatureModifier(class_1959.class_5484 temperatureModifier) {
            climateSettings.comp_845 = temperatureModifier;
            return this;
        }
        
        @Override
        public Mutable setDownfall(float downfall) {
            climateSettings.comp_846 = downfall;
            return this;
        }
        
        @Override
        public boolean hasPrecipitation() {
            return climateSettings.comp_1187;
        }
        
        @Override
        public float getTemperature() {
            return climateSettings.comp_844;
        }
        
        @Override
        public class_1959.class_5484 getTemperatureModifier() {
            return climateSettings.comp_845;
        }
        
        @Override
        public float getDownfall() {
            return climateSettings.comp_846;
        }
    }
    
    public static class EffectsWrapped implements EffectsProperties.Mutable {
        protected final class_4763 effects;
        
        public EffectsWrapped(class_1959 biome) {
            this(biome.method_24377());
        }
        
        public EffectsWrapped(class_4763 effects) {
            this.effects = effects;
        }
        
        @Override
        public EffectsProperties.Mutable setFogColor(int color) {
            effects.field_22067 = color;
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setWaterColor(int color) {
            effects.field_22068 = color;
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setWaterFogColor(int color) {
            effects.field_22069 = color;
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setSkyColor(int color) {
            effects.field_26418 = color;
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setFoliageColorOverride(@Nullable Integer colorOverride) {
            effects.field_26419 = Optional.ofNullable(colorOverride);
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setGrassColorOverride(@Nullable Integer colorOverride) {
            effects.field_26420 = Optional.ofNullable(colorOverride);
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setGrassColorModifier(class_5486 modifier) {
            effects.field_26421 = modifier;
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setAmbientParticle(@Nullable class_4761 settings) {
            effects.field_22070 = Optional.ofNullable(settings);
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setAmbientLoopSound(@Nullable class_6880<class_3414> sound) {
            effects.field_22491 = Optional.ofNullable(sound);
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setAmbientMoodSound(@Nullable class_4968 settings) {
            effects.field_22492 = Optional.ofNullable(settings);
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setAmbientAdditionsSound(@Nullable class_4967 settings) {
            effects.field_22493 = Optional.ofNullable(settings);
            return this;
        }
        
        @Override
        public EffectsProperties.Mutable setBackgroundMusic(@Nullable class_5195 music) {
            effects.field_24113 = Optional.ofNullable(music);
            return this;
        }
        
        @Override
        public int getFogColor() {
            return effects.field_22067;
        }
        
        @Override
        public int getWaterColor() {
            return effects.field_22068;
        }
        
        @Override
        public int getWaterFogColor() {
            return effects.field_22069;
        }
        
        @Override
        public int getSkyColor() {
            return effects.field_26418;
        }
        
        @Override
        public OptionalInt getFoliageColorOverride() {
            return effects.field_26419.map(OptionalInt::of).orElseGet(OptionalInt::empty);
        }
        
        @Override
        public OptionalInt getGrassColorOverride() {
            return effects.field_26420.map(OptionalInt::of).orElseGet(OptionalInt::empty);
        }
        
        @Override
        public class_5486 getGrassColorModifier() {
            return effects.field_26421;
        }
        
        @Override
        public Optional<class_4761> getAmbientParticle() {
            return effects.field_22070;
        }
        
        @Override
        public Optional<class_6880<class_3414>> getAmbientLoopSound() {
            return effects.field_22491;
        }
        
        @Override
        public Optional<class_4968> getAmbientMoodSound() {
            return effects.field_22492;
        }
        
        @Override
        public Optional<class_4967> getAmbientAdditionsSound() {
            return effects.field_22493;
        }
        
        @Override
        public Optional<class_5195> getBackgroundMusic() {
            return effects.field_24113;
        }
    }
    
    public static class GenerationSettingsWrapped implements GenerationProperties {
        protected final class_5485 settings;
        
        public GenerationSettingsWrapped(class_1959 biome) {
            this(biome.method_30970());
        }
        
        public GenerationSettingsWrapped(class_5485 settings) {
            this.settings = settings;
        }
        
        @Override
        public Iterable<class_6880<class_2922<?>>> getCarvers(class_2893.class_2894 carving) {
            return settings.method_30976(carving);
        }
        
        @Override
        public Iterable<class_6880<class_6796>> getFeatures(class_2893.class_2895 decoration) {
            if (decoration.ordinal() >= settings.method_30983().size()) {
                return Collections.emptyList();
            }
            return settings.method_30983().get(decoration.ordinal());
        }
        
        @Override
        public List<Iterable<class_6880<class_6796>>> getFeatures() {
            return (List<Iterable<class_6880<class_6796>>>) (List<?>) settings.method_30983();
        }
    }
    
    public static class SpawnSettingsWrapped implements SpawnProperties {
        protected final class_5483 settings;
        
        public SpawnSettingsWrapped(class_1959 biome) {
            this(biome.method_30966());
        }
        
        public SpawnSettingsWrapped(class_5483 settings) {
            this.settings = settings;
        }
        
        @Override
        public float getCreatureProbability() {
            return this.settings.method_31002();
        }
        
        @Override
        public Map<class_1311, List<class_5483.class_1964>> getSpawners() {
            return null;
        }
        
        @Override
        public Map<class_1299<?>, class_5483.class_5265> getMobSpawnCosts() {
            return null;
        }
    }
}
