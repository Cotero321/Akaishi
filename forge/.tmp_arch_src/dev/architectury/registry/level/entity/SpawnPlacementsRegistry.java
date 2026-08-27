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

package dev.architectury.registry.level.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import java.util.function.Supplier;
import net.minecraft.class_1299;
import net.minecraft.class_1308;
import net.minecraft.class_1317;
import net.minecraft.class_2902;

public final class SpawnPlacementsRegistry {
    /**
     * Registers the spawn placement of an entity.
     *
     * @param type           the type of entity
     * @param spawnPlacement the type of spawn placement
     * @param heightmapType  the type of heightmap
     * @param spawnPredicate the spawn predicate
     * @see net.minecraft.class_1317
     */
    @ExpectPlatform
    public static <T extends class_1308> void register(Supplier<? extends class_1299<T>> type, class_1317.class_1319 spawnPlacement, class_2902.class_2903 heightmapType, class_1317.class_4306<T> spawnPredicate) {
        throw new AssertionError();
    }
}
