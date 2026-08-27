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

package dev.architectury.registry.menu;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_2540;
import net.minecraft.class_2561;
import net.minecraft.class_3222;
import net.minecraft.class_3908;
import net.minecraft.class_3917;
import net.minecraft.class_3936;
import net.minecraft.class_437;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A utility class to register {@link class_3917}s and {@link class_437}s for containers
 */
public final class MenuRegistry {
    private MenuRegistry() {
    }
    
    /**
     * Opens the menu.
     *
     * @param player    The player affected
     * @param provider  The {@link class_3908} that provides the menu
     * @param bufWriter That writer that sends extra data for {@link class_3917} created with {@link MenuRegistry#ofExtended(ExtendedMenuTypeFactory)}
     */
    public static void openExtendedMenu(class_3222 player, class_3908 provider, Consumer<class_2540> bufWriter) {
        openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(class_2540 buf) {
                bufWriter.accept(buf);
            }
            
            @Override
            public class_2561 method_5476() {
                return provider.method_5476();
            }
            
            @Nullable
            @Override
            public class_1703 createMenu(int i, class_1661 inventory, class_1657 player) {
                return provider.createMenu(i, inventory, player);
            }
        });
    }
    
    /**
     * Opens the menu.
     *
     * @param player   The player affected
     * @param provider The {@link ExtendedMenuProvider} that provides the menu
     */
    @ExpectPlatform
    public static void openExtendedMenu(class_3222 player, ExtendedMenuProvider provider) {
        throw new AssertionError();
    }
    
    /**
     * Opens the menu.
     *
     * @param player   The player affected
     * @param provider The {@link class_3908} that provides the menu
     */
    public static void openMenu(class_3222 player, class_3908 provider) {
        player.method_17355(provider);
    }
    
    /**
     * Creates a simple {@link class_3917}.
     *
     * @param factory A functional interface to create the {@link class_3917} from an id (Integer) and inventory
     * @param <T>     The type of {@link class_1703} that handles the logic for the {@link class_3917}
     * @return The {@link class_3917} for your {@link class_1703}
     * @deprecated Use the constructor directly.
     */
    @Deprecated(forRemoval = true)
    @ExpectPlatform
    public static <T extends class_1703> class_3917<T> of(SimpleMenuTypeFactory<T> factory) {
        throw new AssertionError();
    }
    
    /**
     * Creates a extended {@link class_3917}.
     *
     * @param factory A functional interface to create the {@link class_3917} from an id (Integer), {@link class_1661}, and {@link class_2540}
     * @param <T>     The type of {@link class_1703} that handles the logic for the {@link class_3917}
     * @return The {@link class_3917} for your {@link class_1703}
     */
    @ExpectPlatform
    public static <T extends class_1703> class_3917<T> ofExtended(ExtendedMenuTypeFactory<T> factory) {
        throw new AssertionError();
    }
    
    /**
     * Registers a Screen Factory on the client to display.
     *
     * @param type    The {@link class_3917} the screen visualizes
     * @param factory A functional interface that is used to create new {@link class_437}s
     * @param <H>     The type of {@link class_1703} for the screen
     * @param <S>     The type for the {@link class_437}
     */
    @Environment(EnvType.CLIENT)
    @ExpectPlatform
    public static <H extends class_1703, S extends class_437 & class_3936<H>> void registerScreenFactory(class_3917<? extends H> type, ScreenFactory<H, S> factory) {
        throw new AssertionError();
    }
    
    /**
     * Creates new screens.
     *
     * @param <H> The type of {@link class_1703} for the screen
     * @param <S> The type for the {@link class_437}
     */
    @Environment(EnvType.CLIENT)
    @FunctionalInterface
    public interface ScreenFactory<H extends class_1703, S extends class_437 & class_3936<H>> {
        /**
         * Creates a new {@link S} that extends {@link class_437}
         *
         * @param containerMenu The {@link class_1703} that controls the game logic for the screen
         * @param inventory     The {@link class_1661} for the screen
         * @param component     The {@link class_2561} for the screen
         * @return A new {@link S} that extends {@link class_437}
         */
        S create(H containerMenu, class_1661 inventory, class_2561 component);
    }
    
    /**
     * Creates simple menus.
     *
     * @param <T> The {@link class_1703} type
     */
    @Deprecated(forRemoval = true)
    @FunctionalInterface
    public interface SimpleMenuTypeFactory<T extends class_1703> {
        /**
         * Creates a new {@link T} that extends {@link class_1703}
         *
         * @param id The id for the menu
         * @return A new {@link T} that extends {@link class_1703}
         */
        T create(int id, class_1661 inventory);
    }
    
    /**
     * Creates extended menus.
     *
     * @param <T> The {@link class_1703} type
     */
    @FunctionalInterface
    public interface ExtendedMenuTypeFactory<T extends class_1703> {
        /**
         * Creates a new {@link T} that extends {@link class_1703}.
         *
         * @param id        The id for the menu
         * @param inventory The {@link class_1661} for the menu
         * @param buf       The {@link class_2540} for the menu to provide extra data
         * @return A new {@link T} that extends {@link class_1703}
         */
        T create(int id, class_1661 inventory, class_2540 buf);
    }
}
