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

package dev.architectury.hooks.item.tool;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import net.minecraft.class_1743;
import net.minecraft.class_2248;
import net.minecraft.class_2465;

public final class AxeItemHooks {
    private AxeItemHooks() {
    }
    
    /**
     * Adds a new stripping (interact with axe) interaction to the game.<p>
     *
     * Note that both the input block and the result block <em>must</em> have the
     * {@link net.minecraft.class_2741#field_12496 AXIS} property,
     * and that the value of this property will be copied from the input block to the result block when the recipe
     * is performed.
     *
     * @param input input block
     * @param result result block
     * @throws IllegalArgumentException if the input or result blocks do not have the
     * {@link net.minecraft.class_2741#field_12496 AXIS} property.
     */
    public static void addStrippable(class_2248 input, class_2248 result) {
        if (!input.method_9564().method_28498(class_2465.field_11459))
            throw new IllegalArgumentException("Input block is missing required 'AXIS' property!");
        if (!result.method_9564().method_28498(class_2465.field_11459))
            throw new IllegalArgumentException("Result block is missing required 'AXIS' property!");
        if (class_1743.field_7898 instanceof ImmutableMap) {
            class_1743.field_7898 = new HashMap<>(class_1743.field_7898);
        }
        class_1743.field_7898.put(input, result);
    }
}
