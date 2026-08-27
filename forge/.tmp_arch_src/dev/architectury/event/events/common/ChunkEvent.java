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
import net.minecraft.class_2487;
import net.minecraft.class_2791;
import net.minecraft.class_3218;
import org.jetbrains.annotations.Nullable;

public interface ChunkEvent {
    /**
     * @see SaveData#save(class_2791, class_3218, class_2487)
     */
    Event<SaveData> SAVE_DATA = EventFactory.createLoop();
    /**
     * @see LoadData#load(class_2791, class_3218, class_2487)
     */
    Event<LoadData> LOAD_DATA = EventFactory.createLoop();
    
    interface SaveData {
        /**
         * Invoked when a chunk's data is saved, just before the data is written.
         * Add your own data to the {@link class_2487} parameter to get your data saved as well.
         * Equivalent to Forge's {@code ChunkDataEvent.Save}.
         *
         * @param chunk The chunk that is saved.
         * @param level The level the chunk is in.
         * @param nbt   The chunk data that is written to the save file.
         */
        void save(class_2791 chunk, class_3218 level, class_2487 nbt);
    }
    
    interface LoadData {
        /**
         * Invoked just before a chunk's data is fully read.
         * You can read out your own data from the {@link class_2487} parameter, when you have saved one before.
         * Equivalent to Forge's {@code ChunkDataEvent.Load}.
         *
         * @param chunk The chunk that is loaded.
         * @param level The level the chunk is in, may be {@code null}.
         * @param nbt   The chunk data that was read from the save file.
         */
        void load(class_2791 chunk, @Nullable class_3218 level, class_2487 nbt);
    }
}
