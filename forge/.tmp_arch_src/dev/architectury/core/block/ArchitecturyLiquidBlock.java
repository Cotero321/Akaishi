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

package dev.architectury.core.block;

import dev.architectury.platform.Platform;
import java.util.function.Supplier;
import net.minecraft.class_2404;
import net.minecraft.class_3609;

public class ArchitecturyLiquidBlock extends class_2404 {
    public ArchitecturyLiquidBlock(Supplier<? extends class_3609> fluid, class_2251 properties) {
        super(checkPlatform(fluid).get(), properties);
    }
    
    private static <T> T checkPlatform(T obj) {
        if (Platform.isForge()) {
            throw new IllegalStateException("This class should've been replaced on Forge!");
        }
        
        return obj;
    }
}
