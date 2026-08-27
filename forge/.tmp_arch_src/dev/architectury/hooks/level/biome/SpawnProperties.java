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

import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import net.minecraft.class_1299;
import net.minecraft.class_1311;
import net.minecraft.class_5483;

public interface SpawnProperties {
    float getCreatureProbability();
    
    Map<class_1311, List<class_5483.class_1964>> getSpawners();
    
    Map<class_1299<?>, class_5483.class_5265> getMobSpawnCosts();
    
    interface Mutable extends SpawnProperties {
        Mutable setCreatureProbability(float probability);
        
        Mutable addSpawn(class_1311 category, class_5483.class_1964 data);
        
        boolean removeSpawns(BiPredicate<class_1311, class_5483.class_1964> predicate);
        
        Mutable setSpawnCost(class_1299<?> entityType, class_5483.class_5265 cost);
        
        Mutable setSpawnCost(class_1299<?> entityType, double charge, double energyBudget);
        
        Mutable clearSpawnCost(class_1299<?> entityType);
    }
}
