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

package dev.architectury.hooks.client.screen;

import java.util.List;
import net.minecraft.class_339;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_437;
import net.minecraft.class_6379;

public interface ScreenAccess {
    class_437 getScreen();
    
    List<class_6379> getNarratables();
    
    List<class_4068> getRenderables();
    
    <T extends class_339 & class_4068 & class_6379> T addRenderableWidget(T widget);
    
    <T extends class_4068> T addRenderableOnly(T listener);
    
    <T extends class_364 & class_6379> T addWidget(T listener);
}
