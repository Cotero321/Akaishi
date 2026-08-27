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

package dev.architectury.extensions;

import net.minecraft.class_1304;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import org.jetbrains.annotations.Nullable;

/**
 * Extensions to {@link net.minecraft.class_1792}, implement this on to your item.
 */
public interface ItemExtension {
    /**
     * Invoked every tick when this item is equipped.
     *
     * @param stack  the item stack
     * @param player the player wearing the armor
     */
    default void tickArmor(class_1799 stack, class_1657 player) {
    }
    
    /**
     * Returns the {@link class_1304} for {@link class_1799}.
     *
     * @param stack the item stack
     * @return the {@link class_1304}, return {@code null} to default to vanilla's {@link net.minecraft.class_1308#method_32326(class_1799)}
     */
    @Nullable
    default class_1304 getCustomEquipmentSlot(class_1799 stack) {
        return null;
    }
}
