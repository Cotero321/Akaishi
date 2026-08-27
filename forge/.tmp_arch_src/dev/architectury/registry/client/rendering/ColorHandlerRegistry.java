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

package dev.architectury.registry.client.rendering;

import I;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1935;
import net.minecraft.class_2248;
import net.minecraft.class_322;
import net.minecraft.class_326;
import java.util.Objects;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public final class ColorHandlerRegistry {
    private ColorHandlerRegistry() {
    }
    
    public static void registerItemColors(class_326 color, class_1935... items) {
        Supplier<class_1935>[] array = new Supplier[items.length];
        for (var i = 0; i < items.length; i++) {
            var item = Objects.requireNonNull(items[i], "items[i] is null!");
            array[i] = () -> item;
        }
        registerItemColors(color, array);
    }
    
    public static void registerBlockColors(class_322 color, class_2248... blocks) {
        Supplier<class_2248>[] array = new Supplier[blocks.length];
        for (var i = 0; i < blocks.length; i++) {
            var block = Objects.requireNonNull(blocks[i], "blocks[i] is null!");
            array[i] = () -> block;
        }
        registerBlockColors(color, array);
    }
    
    @SafeVarargs
    @ExpectPlatform
    public static void registerItemColors(class_326 color, Supplier<? extends class_1935>... items) {
        throw new AssertionError();
    }
    
    @SafeVarargs
    @ExpectPlatform
    public static void registerBlockColors(class_322 color, Supplier<? extends class_2248>... blocks) {
        throw new AssertionError();
    }
}
