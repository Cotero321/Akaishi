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

package dev.architectury.event.events.common;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_7157;

public interface CommandRegistrationEvent {
    /**
     * @see CommandRegistrationEvent#register(CommandDispatcher, class_7157, class_2170.class_5364)
     */
    Event<CommandRegistrationEvent> EVENT = EventFactory.createLoop();
    
    /**
     * This event is invoked after the server registers it's commands.
     * Equivalent to Forge's {@code RegisterCommandsEvent} and Fabric's {@code CommandRegistrationCallback}.
     *
     * @param dispatcher The command dispatcher to register commands to.
     * @param registry   The command registry for building arguments.
     * @param selection  The selection where the command can be executed.
     */
    void register(CommandDispatcher<class_2168> dispatcher, class_7157 registry, class_2170.class_5364 selection);
}
