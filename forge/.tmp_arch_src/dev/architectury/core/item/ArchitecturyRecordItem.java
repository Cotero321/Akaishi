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

package dev.architectury.core.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.class_1813;
import net.minecraft.class_3414;

public class ArchitecturyRecordItem extends class_1813 {
    private final RegistrySupplier<class_3414> sound;
    
    public ArchitecturyRecordItem(int analogOutput, RegistrySupplier<class_3414> sound, class_1793 properties, int lengthInSeconds) {
        super(analogOutput, sound.orElse(null), properties, lengthInSeconds);
        this.sound = sound;
        
        if (!sound.isPresent()) {
            class_1813.field_8901.remove(null);

            sound.listen(soundEvent -> {
                class_1813.field_8901.put(soundEvent, this);
            });
        }
    }
    
    @Override
    public class_3414 method_8009() {
        return sound.get();
    }
}
