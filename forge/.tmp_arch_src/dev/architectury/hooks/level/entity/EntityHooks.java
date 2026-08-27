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

package dev.architectury.hooks.level.entity;

import net.minecraft.class_1297;
import net.minecraft.class_3726;
import net.minecraft.class_3727;
import org.jetbrains.annotations.Nullable;

public final class EntityHooks {
    private EntityHooks() {
    }
    
    @Nullable
    public static class_1297 fromCollision(class_3726 ctx) {
        return ctx instanceof class_3727 ? ((class_3727) ctx).method_32480() : null;
    }
}
