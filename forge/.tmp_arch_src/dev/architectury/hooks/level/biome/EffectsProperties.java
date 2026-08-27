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

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.class_3414;
import net.minecraft.class_4761;
import net.minecraft.class_4763.class_5486;
import net.minecraft.class_4967;
import net.minecraft.class_4968;
import net.minecraft.class_5195;
import net.minecraft.class_6880;

public interface EffectsProperties {
    int getFogColor();
    
    int getWaterColor();
    
    int getWaterFogColor();
    
    int getSkyColor();
    
    OptionalInt getFoliageColorOverride();
    
    OptionalInt getGrassColorOverride();
    
    class_5486 getGrassColorModifier();
    
    Optional<class_4761> getAmbientParticle();
    
    Optional<class_6880<class_3414>> getAmbientLoopSound();
    
    Optional<class_4968> getAmbientMoodSound();
    
    Optional<class_4967> getAmbientAdditionsSound();
    
    Optional<class_5195> getBackgroundMusic();
    
    interface Mutable extends EffectsProperties {
        EffectsProperties.Mutable setFogColor(int color);
        
        EffectsProperties.Mutable setWaterColor(int color);
        
        EffectsProperties.Mutable setWaterFogColor(int color);
        
        EffectsProperties.Mutable setSkyColor(int color);
        
        EffectsProperties.Mutable setFoliageColorOverride(@Nullable Integer colorOverride);
        
        EffectsProperties.Mutable setGrassColorOverride(@Nullable Integer colorOverride);
        
        EffectsProperties.Mutable setGrassColorModifier(class_5486 modifier);
        
        EffectsProperties.Mutable setAmbientParticle(@Nullable class_4761 settings);
        
        EffectsProperties.Mutable setAmbientLoopSound(@Nullable class_6880<class_3414> sound);
        
        EffectsProperties.Mutable setAmbientMoodSound(@Nullable class_4968 settings);
        
        EffectsProperties.Mutable setAmbientAdditionsSound(@Nullable class_4967 settings);
        
        EffectsProperties.Mutable setBackgroundMusic(@Nullable class_5195 music);
    }
}
