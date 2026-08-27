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

package dev.architectury.impl;

import dev.architectury.hooks.client.screen.ScreenAccess;
import dev.architectury.hooks.client.screen.ScreenHooks;
import java.util.List;
import net.minecraft.class_339;
import net.minecraft.class_364;
import net.minecraft.class_4068;
import net.minecraft.class_437;
import net.minecraft.class_6379;

public class ScreenAccessImpl implements ScreenAccess {
    private class_437 screen;
    
    public ScreenAccessImpl(class_437 screen) {
        this.screen = screen;
    }
    
    public void setScreen(class_437 screen) {
        this.screen = screen;
    }
    
    @Override
    public class_437 getScreen() {
        return screen;
    }
    
    @Override
    public List<class_6379> getNarratables() {
        return ScreenHooks.getNarratables(screen);
    }
    
    @Override
    public List<class_4068> getRenderables() {
        return ScreenHooks.getRenderables(screen);
    }
    
    @Override
    public <T extends class_339 & class_4068 & class_6379> T addRenderableWidget(T widget) {
        return ScreenHooks.addRenderableWidget(screen, widget);
    }
    
    @Override
    public <T extends class_4068> T addRenderableOnly(T listener) {
        return ScreenHooks.addRenderableOnly(screen, listener);
    }
    
    @Override
    public <T extends class_364 & class_6379> T addWidget(T listener) {
        return ScreenHooks.addWidget(screen, listener);
    }
}
