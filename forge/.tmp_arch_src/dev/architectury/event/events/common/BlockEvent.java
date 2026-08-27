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

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import dev.architectury.utils.value.IntValue;
import net.minecraft.class_1297;
import net.minecraft.class_1540;
import net.minecraft.class_1937;
import net.minecraft.class_2338;
import net.minecraft.class_2680;
import net.minecraft.class_3222;
import org.jetbrains.annotations.Nullable;

public interface BlockEvent {
    /**
     * @see Break#breakBlock(class_1937, class_2338, class_2680, class_3222, IntValue)
     */
    Event<Break> BREAK = EventFactory.createEventResult();
    /**
     * @see Place#placeBlock(class_1937, class_2338, class_2680, class_1297)
     */
    Event<Place> PLACE = EventFactory.createEventResult();
    /**
     * @see FallingLand#onLand(class_1937, class_2338, class_2680, class_2680, class_1540)
     */
    Event<FallingLand> FALLING_LAND = EventFactory.createLoop();
    
    interface Break {
        /**
         * Invoked when a block is destroyed by a player.
         *
         * @param level  The level the block is in.
         * @param pos    The position of the block.
         * @param state  The current state of the block.
         * @param player The player who is breaking the block.
         * @param xp     The experience that are dropped when the block was destroyed. Always {@code null} on fabric.
         * @return A {@link EventResult} determining the outcome of the event,
         * the execution of the vanilla block breaking may be cancelled by the result.
         */
        EventResult breakBlock(class_1937 level, class_2338 pos, class_2680 state, class_3222 player, @Nullable IntValue xp);
    }
    
    interface Place {
        /**
         * Invoked when a block is placed.
         *
         * @param level  The level the block is in.
         * @param pos    The position of the block.
         * @param state  The future state of the block.
         * @param placer The entity who is placing it. Can be {@code null}, e.g. when a dispenser places something.
         * @return A {@link EventResult} determining the outcome of the event,
         * the execution of the vanilla block placing may be cancelled by the result.
         */
        EventResult placeBlock(class_1937 level, class_2338 pos, class_2680 state, @Nullable class_1297 placer);
    }
    
    interface FallingLand {
        /**
         * Invoked when a falling block is about to land.
         *
         * @param level     The level the block is in.
         * @param pos       The position of the block.
         * @param fallState The current state of the falling block.
         * @param landOn    The current state of the block the falling one is landing on.
         * @param entity    The falling block entity.
         */
        void onLand(class_1937 level, class_2338 pos, class_2680 fallState, class_2680 landOn, class_1540 entity);
    }
}
