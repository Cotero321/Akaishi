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

package dev.architectury.registry.client.gui;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_5632;
import net.minecraft.class_5684;
import java.util.function.Function;

/**
 * Registry for {@link class_5684} factories
 */
@Environment(EnvType.CLIENT)
public final class ClientTooltipComponentRegistry {
    private ClientTooltipComponentRegistry() {
    }
    
    /**
     * Allows users to register custom {@link class_5684}
     * factories for their {@link class_5632} types.
     *
     * @param clazz   class of {@link T}
     * @param factory factory to create instances of {@link class_5684} from {@link T}
     * @param <T>     the type of {@link class_5632} factory
     */
    @ExpectPlatform
    public static <T extends class_5632> void register(Class<T> clazz, Function<? super T, ? extends class_5684> factory) {
        throw new AssertionError();
    }
}
