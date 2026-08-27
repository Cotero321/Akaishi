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

package dev.architectury.registry.registries;

import dev.architectury.utils.OptionalSupplier;
import net.minecraft.class_2378;
import net.minecraft.class_2960;
import net.minecraft.class_5321;

public interface DeferredSupplier<T> extends OptionalSupplier<T> {
    /**
     * @return the identifier of the registry
     */
    class_2960 getRegistryId();
    
    /**
     * @return the identifier of the registry
     */
    default class_5321<class_2378<T>> getRegistryKey() {
        return class_5321.method_29180(getRegistryId());
    }
    
    /**
     * @return the identifier of the entry
     */
    class_2960 getId();
    
    /**
     * Returns the registry key of the creative tab.
     *
     * @return The registry key of the creative tab.
     */
    default class_5321<T> getKey() {
        return class_5321.method_29179(getRegistryKey(), getId());
    }
}
