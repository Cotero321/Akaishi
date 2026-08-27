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

package dev.architectury.registry;

import dev.architectury.extensions.injected.InjectedItemPropertiesExtension;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.registries.DeferredSupplier;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.class_1761;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_5321;
import net.minecraft.class_7699;

/**
 * Registry for creating or modifying creative tabs.
 *
 * @see InjectedItemPropertiesExtension#arch$tab(class_1761) to add an item to a creative tab easily
 */
public final class CreativeTabRegistry {
    private CreativeTabRegistry() {
    }
    
    /**
     * Creates a creative tab, with a custom icon.
     * This has to be registered manually.
     *
     * @param title the title of the creative tab
     * @param icon  the icon of the creative tab
     * @return the creative tab
     */
    public static class_1761 create(class_2561 title, Supplier<class_1799> icon) {
        return create(builder -> {
            builder.method_47321(title);
            builder.method_47320(icon);
        });
    }
    
    
    /**
     * Creates a creative tab, with a configurable builder callback.
     * This has to be registered manually.
     *
     * @param callback the builder callback
     * @return the creative tab
     */
    @ExpectPlatform
    @ApiStatus.Experimental
    public static class_1761 create(Consumer<class_1761.class_7913> callback) {
        throw new AssertionError();
    }
    
    /**
     * Returns a tab supplier for a tab.
     *
     * @param tab the tab
     * @return the tab supplier
     */
    @ExpectPlatform
    @ApiStatus.Experimental
    public static DeferredSupplier<class_1761> ofBuiltin(class_1761 tab) {
        throw new AssertionError();
    }
    
    /**
     * Returns a tab supplier for a tab to be created later.
     *
     * @param name the name of the creative tab
     * @return the tab supplier
     */
    @ExpectPlatform
    @ApiStatus.Experimental
    public static DeferredSupplier<class_1761> defer(class_2960 name) {
        throw new AssertionError();
    }
    
    /**
     * Returns a tab supplier for a tab to be created later.
     *
     * @param name the key of the creative tab
     * @return the tab supplier
     */
    @ApiStatus.Experimental
    public static DeferredSupplier<class_1761> defer(class_5321<class_1761> name) {
        return defer(name.method_29177());
    }
    
    @ApiStatus.Experimental
    public static void modifyBuiltin(class_1761 tab, ModifyTabCallback filler) {
        modify(ofBuiltin(tab), filler);
    }
    
    @ExpectPlatform
    @ApiStatus.Experimental
    public static void modify(DeferredSupplier<class_1761> tab, ModifyTabCallback filler) {
        throw new AssertionError();
    }
    
    @ApiStatus.Experimental
    public static void appendBuiltin(class_1761 tab, class_1935... items) {
        append(ofBuiltin(tab), items);
    }
    
    @ApiStatus.Experimental
    public static <I extends class_1935, T extends Supplier<I>> void appendBuiltin(class_1761 tab, T... items) {
        appendStack(ofBuiltin(tab), Stream.of(items).map(supplier -> () -> new class_1799(supplier.get())));
    }
    
    @ApiStatus.Experimental
    public static void appendBuiltinStack(class_1761 tab, class_1799... items) {
        appendStack(ofBuiltin(tab), items);
    }
    
    @ApiStatus.Experimental
    public static void appendBuiltinStack(class_1761 tab, Supplier<class_1799>... items) {
        appendStack(ofBuiltin(tab), items);
    }
    
    @ApiStatus.Experimental
    public static void append(DeferredSupplier<class_1761> tab, class_1935... items) {
        appendStack(tab, Stream.of(items).map(item -> () -> new class_1799(item)));
    }
    
    @ApiStatus.Experimental
    public static <I extends class_1935, T extends Supplier<I>> void append(DeferredSupplier<class_1761> tab, T... items) {
        appendStack(tab, Stream.of(items).map(supplier -> () -> new class_1799(supplier.get())));
    }
    
    @ApiStatus.Experimental
    public static void appendStack(DeferredSupplier<class_1761> tab, class_1799... items) {
        appendStack(tab, Stream.of(items).map(supplier -> () -> supplier));
    }
    
    @ExpectPlatform
    @ApiStatus.Experimental
    public static void appendStack(DeferredSupplier<class_1761> tab, Supplier<class_1799> item) {
        throw new AssertionError();
    }
    
    @ApiStatus.Experimental
    public static void appendStack(DeferredSupplier<class_1761> tab, Supplier<class_1799>... items) {
        for (Supplier<class_1799> item : items) {
            appendStack(tab, item);
        }
    }
    
    @ApiStatus.Experimental
    public static void appendStack(DeferredSupplier<class_1761> tab, Stream<Supplier<class_1799>> items) {
        items.forEach(item -> appendStack(tab, item));
    }
    
    @ApiStatus.Experimental
    public static void append(class_5321<class_1761> tab, class_1935... items) {
        appendStack(defer(tab), Stream.of(items).map(item -> () -> new class_1799(item)));
    }
    
    @ApiStatus.Experimental
    public static <I extends class_1935, T extends Supplier<I>> void append(class_5321<class_1761> tab, T... items) {
        appendStack(defer(tab), Stream.of(items).map(supplier -> () -> new class_1799(supplier.get())));
    }
    
    @ApiStatus.Experimental
    public static void appendStack(class_5321<class_1761> tab, class_1799... items) {
        appendStack(defer(tab), Stream.of(items).map(supplier -> () -> supplier));
    }
    
    @ApiStatus.Experimental
    public static void appendStack(class_5321<class_1761> tab, Supplier<class_1799> item) {
        appendStack(defer(tab), item);
    }
    
    @ApiStatus.Experimental
    public static void appendStack(class_5321<class_1761> tab, Supplier<class_1799>... items) {
        appendStack(defer(tab), items);
    }
    
    @ApiStatus.Experimental
    public static void appendStack(class_5321<class_1761> tab, Stream<Supplier<class_1799>> items) {
        appendStack(defer(tab), items);
    }
    
    @FunctionalInterface
    public interface ModifyTabCallback {
        void accept(class_7699 flags, CreativeTabOutput output, boolean canUseGameMasterBlocks);
    }
}
