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

import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import net.minecraft.class_2893;
import net.minecraft.class_2922;
import net.minecraft.class_5321;
import net.minecraft.class_6796;
import net.minecraft.class_6880;

public interface GenerationProperties {
    Iterable<class_6880<class_2922<?>>> getCarvers(class_2893.class_2894 carving);
    
    Iterable<class_6880<class_6796>> getFeatures(class_2893.class_2895 decoration);
    
    List<Iterable<class_6880<class_6796>>> getFeatures();
    
    interface Mutable extends GenerationProperties {
        Mutable addFeature(class_2893.class_2895 decoration, class_6880<class_6796> feature);
        
        @ApiStatus.Experimental
        Mutable addFeature(class_2893.class_2895 decoration, class_5321<class_6796> feature);
        
        Mutable addCarver(class_2893.class_2894 carving, class_6880<class_2922<?>> feature);
        
        @ApiStatus.Experimental
        Mutable addCarver(class_2893.class_2894 carving, class_5321<class_2922<?>> feature);
        
        Mutable removeFeature(class_2893.class_2895 decoration, class_5321<class_6796> feature);
        
        Mutable removeCarver(class_2893.class_2894 carving, class_5321<class_2922<?>> feature);
    }
}
