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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import net.minecraft.class_1299;
import net.minecraft.class_1308;
import net.minecraft.class_1799;
import net.minecraft.class_1826;
import net.minecraft.class_2315;
import net.minecraft.class_2342;
import net.minecraft.class_2347;
import net.minecraft.class_2350;
import net.minecraft.class_2357;
import net.minecraft.class_2487;
import net.minecraft.class_3730;
import net.minecraft.class_5712;

public class ArchitecturySpawnEggItem extends class_1826 {
    private static final Logger LOGGER = LogManager.getLogger(ArchitecturySpawnEggItem.class);
    
    private final RegistrySupplier<? extends class_1299<? extends class_1308>> entityType;
    
    protected static class_2357 createDispenseItemBehavior() {
        return new class_2347() {
            @Override
            public class_1799 method_10135(class_2342 source, class_1799 stack) {
                class_2350 direction = source.method_10120().method_11654(class_2315.field_10918);
                class_1299<?> entityType = ((class_1826) stack.method_7909()).method_8015(stack.method_7969());
                
                try {
                    entityType.method_5894(source.method_10207(), stack, null, source.method_10122().method_10093(direction), class_3730.field_16470, direction != class_2350.field_11036, false);
                } catch (Exception var6) {
                    field_34020.error("Error while dispensing spawn egg from dispenser at {}", source.method_10122(), var6);
                    return class_1799.field_8037;
                }
                
                stack.method_7934(1);
                source.method_10207().method_33596(null, class_5712.field_28738, source.method_10122());
                return stack;
            }
        };
    }
    
    public ArchitecturySpawnEggItem(RegistrySupplier<? extends class_1299<? extends class_1308>> entityType, int backgroundColor, int highlightColor, class_1793 properties) {
        this(entityType, backgroundColor, highlightColor, properties, createDispenseItemBehavior());
    }
    
    public ArchitecturySpawnEggItem(RegistrySupplier<? extends class_1299<? extends class_1308>> entityType, int backgroundColor, int highlightColor, class_1793 properties,
                                    @Nullable class_2357 dispenseItemBehavior) {
        super(null, backgroundColor, highlightColor, properties);
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        class_1826.field_8914.remove(null);
        entityType.listen(type -> {
            LOGGER.debug("Registering spawn egg {} for {}", toString(),
                    Objects.toString(type.arch$registryName()));
            class_1826.field_8914.put(type, this);
            this.field_8917 = type;
            
            if (dispenseItemBehavior != null) {
                class_2315.method_10009(this, dispenseItemBehavior);
            }
        });
    }
    
    @Override
    public class_1299<?> method_8015(@Nullable class_2487 compoundTag) {
        class_1299<?> type = super.method_8015(compoundTag);
        return type == null ? entityType.get() : type;
    }
}
