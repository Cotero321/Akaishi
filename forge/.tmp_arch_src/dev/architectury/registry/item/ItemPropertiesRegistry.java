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

package dev.architectury.registry.item;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1935;
import net.minecraft.class_2960;
import net.minecraft.class_6395;

/**
 * Registry for registering item properties used for model predicates.
 *
 * @see net.minecraft.class_5272
 */
@Environment(EnvType.CLIENT)
public final class ItemPropertiesRegistry {
    private ItemPropertiesRegistry() {
    }
    
    /**
     * Registers a generic item property function for all items.
     *
     * @param propertyId the id of the property
     * @param function   the function to be registered
     * @return the function registered
     */
    @ExpectPlatform
    public static class_6395 registerGeneric(class_2960 propertyId, class_6395 function) {
        throw new AssertionError();
    }
    
    /**
     * Registers a generic item property function for a specific item.
     *
     * @param item       the item to be registered for
     * @param propertyId the id of the property
     * @param function   the function to be registered
     * @return the function registered
     */
    @ExpectPlatform
    public static class_6395 register(class_1935 item, class_2960 propertyId, class_6395 function) {
        throw new AssertionError();
    }
}
