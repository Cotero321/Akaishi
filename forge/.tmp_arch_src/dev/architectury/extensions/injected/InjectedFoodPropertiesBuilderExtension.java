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

package dev.architectury.extensions.injected;

import dev.architectury.hooks.item.food.FoodPropertiesHooks;
import java.util.function.Supplier;
import net.minecraft.class_1293;
import net.minecraft.class_4174;

public interface InjectedFoodPropertiesBuilderExtension {
    default class_4174.class_4175 arch$effect(Supplier<? extends class_1293> effectSupplier, float chance) {
        FoodPropertiesHooks.effect((class_4174.class_4175) this, effectSupplier, chance);
        return (class_4174.class_4175) this;
    }
}
